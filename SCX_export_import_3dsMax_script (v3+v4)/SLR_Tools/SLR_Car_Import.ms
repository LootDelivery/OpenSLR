-- ==========================================================================
--  SCX Car Import v2.2 for Street Legal Racing: Redline
--  Rebuilds a car from its rpk the way the game puts it together:
--    gametype (toe 8)  "native car|part <file>.cfg"
--    every .cfg has slots; "attach <part> <slot>" and "compatible <part> <slot>"
--    say which slot of which part a slot connects to, so a part sits at
--        inverse(its own slot) * slot of the parent * parent
--    slots with slottype 1xx-3xx are relative to their wheel, 10-99 to the
--    steering; "stockpart" lists what a car or a part comes with (the door
--    brings the window and the mirror)
--    renders right after "wheel"/"steering" lines are relative to them,
--    "body ... <file.scx>" lines are the collision (phys) meshes
--  References with a high word live in other rpks (vanilla parts, default
--  wheels of racers.rpk, parts.rpk...) and are followed through the ext table.
--  Positions are in meters, meshes in centimeters, angles in radians.
-- ==========================================================================
if( (SLR_RPK_read == undefined) or (SLR_SCX_IMPORT_VERSION == undefined) or (SLR_SCX_IMPORT_VERSION < 6) ) then
	fileIn ((GetDir #scripts) + "\\SLR_Tools\\SLR_SCX_import.ms") -- mesh importer holds the readers

global SLR_Car_Import

if( SLR_CAR_MODE == undefined ) then -- keep chosen options between runs
(
	global SLR_CAR_MODE = 1 -- 1 assembled car, 2 everything in the rpk
	global SLR_CAR_PHYS = true -- collision meshes of the "body" lines
	global SLR_CAR_LIGHTS = false -- lit lamp meshes (renders with flags)
	global SLR_CAR_LAYERS = true
	global SLR_CAR_INSTANCE = true
	global SLR_CAR_PREFIX = false
	global SLR_CAR_HIERARCHY = 1 -- 1 part it sits on, 2 chassis, 3 no links
	global SLR_CAR_SCALE = 100.0 -- the cfg is in meters, the meshes in centimeters
	global SLR_CAR_LAST = ""
)

if( SLR_CAR_GROUPS == undefined ) then
	global SLR_CAR_GROUPS = 2 -- 1 no groups, 2 phys/lights/wheels in groups, 3 every part with its phys

-- state of the import running now
global SLR_CAR_ROOT = ""
global SLR_CAR_RPKS = #( #(), #() )
global SLR_CAR_PKEYS = #()
global SLR_CAR_PARTS = #()

struct SLR_CAR_Slot ( id, pos, rot, wblock, stype, attach, compat, ids, wants )
struct SLR_CAR_Render ( rref, pos, rot, flags, lod, ctx, ctxIdx )
struct SLR_CAR_Body ( kind, pos, rot, shp, size, rref, nocoll )
struct SLR_CAR_Part ( pkey, rpk, tid, kind, cfg, name, renders, deformRef, meshRef, bodies, wheels, wheelR, steering, slots, stock )
struct SLR_CAR_Node ( part, tm, used, par, how, slot, obj, idx )

-- --------------------------------------------------------------- helpers
function SLR_CAR_float str =
(
	local v = str as float
	if( v == undefined ) then 0.0 else v
)

function SLR_CAR_rel root file_name = -- path relative to the game folder, lower case
(
	local f = toLower (substituteString file_name "/" "\\")
	local r = toLower (substituteString root "/" "\\")

	if( (substring f 1 r.count) == r ) then (substring f (r.count+1) -1) else f
)

function SLR_CAR_rpk rel = -- #( ext names, entries ) of a rpk, read once
(
	SLR_RPK_readCached (SLR_CAR_ROOT + rel) SLR_CAR_RPKS
)

function SLR_CAR_keyOf rel tid =
(
	rel + "|" + (tid as string)
)

function SLR_CAR_resolve rel ref = -- #( rpk, typeID ) of a reference made inside rpk rel
(
	if( ref == undefined ) then return undefined

	local extIdx = bit.shift ref -16
	local tid = bit.and ref 0xFFFF

	if( extIdx == 0 ) then return #( rel, tid )

	local rpk = SLR_CAR_rpk rel

	if( (rpk == undefined) or (extIdx > rpk[1].count) or (rpk[1][extIdx] == undefined) ) then return undefined

	#( (toLower (substituteString rpk[1][extIdx] "/" "\\")), tid ) -- return
)

function SLR_CAR_nativeCfg e = -- #( "car" or "part", cfg ) of a gametype entry
(
	local tok, res

	for l in e.lines while (res == undefined) do
	(
		tok = filterString l " \t"

		if( (tok.count >= 3) and ((toLower tok[1]) == "native") and ((toLower (getFilenameType tok[tok.count])) == ".cfg") ) then
			res = #( (toLower tok[2]), tok[tok.count] )
	)

	res -- return
)

function SLR_CAR_wheelName i =
(
	local names = #("FL", "FR", "RL", "RR")

	if( (i >= 1) and (i <= 4) ) then names[i] else ("W" + (i as string))
)

-- --------------------------------------------------------------- reading
function SLR_CAR_addBody part tok cref = -- "body|steering x y z yaw pitch roll mass <shape> [size]"
(
	if( tok.count < 8 ) then return undefined

	local b = SLR_CAR_Body kind:(toLower tok[1]) \
		pos:[ (SLR_CAR_float tok[2]), (SLR_CAR_float tok[3]), (SLR_CAR_float tok[4]) ] \
		rot:[ (SLR_CAR_float tok[5]), (SLR_CAR_float tok[6]), (SLR_CAR_float tok[7]) ] \
		shp:"" size:0.0 rref:cref nocoll:false

	if( tok.count >= 9 ) then b.shp = tok[9]
	if( tok.count >= 10 ) then b.size = SLR_CAR_float tok[10]

	append part.bodies b
	b -- return
)

function SLR_CAR_readCfg file_name = -- everything of one .cfg that places meshes
(
	local part = SLR_CAR_Part cfg:file_name renders:#() bodies:#() wheels:#() wheelR:#() slots:#() stock:#()

	if( not (doesFileExist file_name) ) then return part

	local f = openFile file_name

	if( f == undefined ) then return part

	-- lines that keep a render attached to the wheel/steering above it
	local keepCtx = #("render", "flags", "colors", "noclick", "nocollision", "lod", "lods", "buoy", "bone", "wheelbones", "notify")
	local ctx = 0, ctxIdx = 0 -- 0 car, 1 wheel, 2 steering
	local line, tok, key, i, j, c, cref, slot, lastRender, lastType, lastBody, wblock = 0

	while( not (eof f) ) do
	(
		line = readLine f
		cref = undefined

		i = findString line ";" -- the wizard writes the rpk id of a phys mesh as a comment
		if( i != undefined ) then
		(
			c = toLower (substring line (i+1) -1)
			j = findString c "0x"
			if( j != undefined ) then cref = SLR_RPK_hexToInt (substring c j 10)
		)

		for cm in #("//", ";", "#") do
		(
			i = findString line cm
			if( i != undefined ) then line = substring line 1 (i-1)
		)

		tok = filterString line " \t"

		if( tok.count > 0 ) then
		(
			key = toLower tok[1]
			if( key == "stcokpart" ) then key = "stockpart" -- typos found in some mods
			if( key == "nocollisio" ) then key = "nocollision"

			if( ((findItem keepCtx key) == 0) and (key != "wheel") and (key != "steering") ) then
			(
				ctx = 0
				ctxIdx = 0
			)

			case key of
			(
				"render":
				(
					if( tok.count >= 2 ) then
					(
						lastRender = SLR_CAR_Render rref:(SLR_RPK_hexToInt tok[2]) rot:[0,0,0] ctx:ctx ctxIdx:ctxIdx

						if( tok.count >= 5 ) then lastRender.pos = [ (SLR_CAR_float tok[3]), (SLR_CAR_float tok[4]), (SLR_CAR_float tok[5]) ]
						if( tok.count >= 8 ) then lastRender.rot = [ (SLR_CAR_float tok[6]), (SLR_CAR_float tok[7]), (SLR_CAR_float tok[8]) ]

						append part.renders lastRender
					)
				)
				"flags": if( (lastRender != undefined) and (tok.count >= 2) ) then lastRender.flags = (SLR_CAR_float tok[2]) as integer
				"lod": if( (lastRender != undefined) and (tok.count >= 2) ) then lastRender.lod = (SLR_CAR_float tok[2]) as integer
				"body":
				(
					lastBody = SLR_CAR_addBody part tok cref
					lastRender = undefined
				)
				"nocollision": if( lastBody != undefined ) then lastBody.nocoll = true -- water/buoyancy boxes, not collision
				"buoy": if( lastBody != undefined ) then lastBody.nocoll = true
				"steering":
				(
					local b = SLR_CAR_addBody part tok cref
					lastBody = b

					if( b != undefined ) then
					(
						part.steering = b
						ctx = 2
						ctxIdx = 0
					)

					lastRender = undefined
				)
				"type": if( (tok.count >= 4) and ((toLower tok[3]) == "sphere") ) then lastType = SLR_CAR_float tok[4]
				"deformable": if( (tok.count >= 2) and (part.deformRef == undefined) ) then part.deformRef = SLR_RPK_hexToInt tok[2]
				"mesh": if( (tok.count >= 2) and (part.meshRef == undefined) ) then part.meshRef = SLR_RPK_hexToInt tok[2]
				"wheel":
				(
					if( tok.count >= 7 ) then
					(
						append part.wheels #( [ (SLR_CAR_float tok[2]), (SLR_CAR_float tok[3]), (SLR_CAR_float tok[4]) ], \
							[ (SLR_CAR_float tok[5]), (SLR_CAR_float tok[6]), (SLR_CAR_float tok[7]) ] )
						append part.wheelR (if lastType == undefined then 0.0 else lastType)

						wblock = part.wheels.count
						ctx = 1
						ctxIdx = wblock
					)

					lastRender = undefined
				)
				"slot":
				(
					if( tok.count >= 8 ) then
					(
						slot = SLR_CAR_Slot id:((SLR_CAR_float tok[8]) as integer) \
							pos:[ (SLR_CAR_float tok[2]), (SLR_CAR_float tok[3]), (SLR_CAR_float tok[4]) ] \
							rot:[ (SLR_CAR_float tok[5]), (SLR_CAR_float tok[6]), (SLR_CAR_float tok[7]) ] \
							wblock:wblock attach:#() compat:#() ids:#() wants:#()

						append part.slots slot
					)

					lastRender = undefined
				)
				"attach": if( (slot != undefined) and (tok.count >= 3) ) then append slot.attach #( (SLR_RPK_hexToInt tok[2]), ((SLR_CAR_float tok[3]) as integer) )
				"compatible": if( (slot != undefined) and (tok.count >= 3) ) then append slot.compat #( (SLR_RPK_hexToInt tok[2]), ((SLR_CAR_float tok[3]) as integer) )
				"slottype": if( (slot != undefined) and (tok.count >= 2) ) then slot.stype = (SLR_CAR_float tok[2]) as integer
				"stockpart": if( tok.count >= 2 ) then append part.stock (SLR_RPK_hexToInt tok[2])
				default: ()
			)
		)
	)

	close f

	-- level of detail: keep only the best one
	local top = -1

	for r in part.renders where (r.lod != undefined) do
		if( r.lod > top ) then top = r.lod

	if( top >= 0 ) then
		part.renders = for r in part.renders where ((r.lod == undefined) or (r.lod == top)) collect r

	part -- return
)

function SLR_CAR_getPart rel tid = -- the part of a gametype, read once (undefined when it has no cfg)
(
	local k = SLR_CAR_keyOf rel tid
	local idx = findItem SLR_CAR_PKEYS k

	if( idx != 0 ) then return SLR_CAR_PARTS[idx]

	append SLR_CAR_PKEYS k
	append SLR_CAR_PARTS undefined
	idx = SLR_CAR_PKEYS.count

	local rpk = SLR_CAR_rpk rel
	if( rpk == undefined ) then return undefined

	local e = SLR_RPK_findEntry rpk[2] 8 tid
	if( e == undefined ) then return undefined

	local info = SLR_CAR_nativeCfg e
	if( info == undefined ) then return undefined
	if( not (doesFileExist (SLR_CAR_ROOT + info[2])) ) then return undefined

	local part = SLR_CAR_readCfg (SLR_CAR_ROOT + info[2])

	part.pkey = k
	part.rpk = rel
	part.tid = tid
	part.kind = info[1]
	part.cfg = info[2]
	part.name = getFilenameFile info[2]

	local r

	for s in part.slots do
	(
		s.ids = #( (k + "|" + (s.id as string)) )

		for c in s.compat do
		(
			r = SLR_CAR_resolve rel c[1]
			if( r != undefined ) then append s.ids ((SLR_CAR_keyOf r[1] r[2]) + "|" + (c[2] as string))
		)

		for a in s.attach do
		(
			r = SLR_CAR_resolve rel a[1]
			if( r != undefined ) then append s.wants #( ((SLR_CAR_keyOf r[1] r[2]) + "|" + (a[2] as string)), r[1], r[2], a[2] )
		)
	)

	SLR_CAR_PARTS[idx] = part
	part -- return
)

function SLR_CAR_chassisList rel = -- the "native car" gametypes of a rpk
(
	local rpk = SLR_CAR_rpk rel
	local out = #()

	if( rpk == undefined ) then return out

	for e in rpk[2] where (e.toe == 8) do
	(
		local p = SLR_CAR_getPart rel e.typeID
		if( (p != undefined) and (p.kind == "car") ) then append out p
	)

	out -- return
)

-- -------------------------------------------------------------- assembly
function SLR_CAR_matrix pos rot scale = -- one cfg transform (meters + radians) in max space
(
	local tm = rotateZMatrix (radToDeg rot.x) -- yaw, around the up axis of the game

	if( rot.y != 0.0 ) then tm = (rotateXMatrix (radToDeg rot.y)) * tm -- pitch
	if( rot.z != 0.0 ) then tm = (rotateYMatrix (radToDeg -rot.z)) * tm -- roll

	tm.row4 = [ pos.x*scale, -pos.z*scale, pos.y*scale ]
	tm -- return
)

function SLR_CAR_slotTM part s scale = -- slot transform inside its part
(
	local tm = SLR_CAR_matrix s.pos s.rot scale

	if( (s.stype != undefined) and (s.stype >= 100) and (s.wblock > 0) and (s.wblock <= part.wheels.count) ) then
		tm = tm * (SLR_CAR_matrix part.wheels[s.wblock][1] part.wheels[s.wblock][2] scale) -- on the wheel
	else
	if( (s.stype != undefined) and (s.stype >= 10) and (s.stype < 100) and (part.steering != undefined) ) then
		tm = tm * (SLR_CAR_matrix part.steering.pos part.steering.rot scale) -- on the steering

	tm -- return
)

function SLR_CAR_fits c q = -- 1: slot c attaches to q, 2: q accepts c, 0: no
(
	local res = 0

	for w in c.wants while (res == 0) do
		if( (findItem q.ids w[1]) != 0 ) then res = 1

	for w in q.wants while (res == 0) do
		if( (findItem c.ids w[1]) != 0 ) then res = 2

	res -- return
)

function SLR_CAR_fitsLoose c q qPart = -- chassis variant without "compatible" lines: same slot id of another chassis
(
	if( qPart.kind != "car" ) then return false

	local fitsHere = false

	for w in c.wants while (not fitsHere) do
	(
		if( (w[4] == q.id) and (w[2] == qPart.rpk) and (w[3] != qPart.tid) ) then
		(
			local t = SLR_CAR_getPart w[2] w[3]
			if( (t != undefined) and (t.kind == "car") ) then fitsHere = true
		)
	)

	fitsHere -- return
)

function SLR_CAR_findSlot child nodes occupancy = -- #( node, slot index, its slot, slot of the child, how ) or undefined
(
	local found, n, qp, q, how

	for pass=1 to 2 while (found == undefined) do
	(
		for ni=1 to nodes.count while (found == undefined) do
		(
			n = nodes[ni]
			qp = n.part

			for qi=1 to qp.slots.count while (found == undefined) do
			(
				if( (not occupancy) or ((findItem n.used qi) == 0) ) then
				(
					q = qp.slots[qi]

					for c in child.slots while (found == undefined) do
					(
						if( pass == 1 ) then
						(
							how = SLR_CAR_fits c q
							if( how != 0 ) then found = #( n, qi, q, c, (if how == 1 then "attach" else "accept") )
						)
						else
						if( SLR_CAR_fitsLoose c q qp ) then
							found = #( n, qi, q, c, "loose" )
					)
				)
			)
		)
	)

	found -- return
)

function SLR_CAR_place child found scale = -- node of a part sitting on the slot found for it
(
	local n = found[1]
	local tm = (inverse (SLR_CAR_matrix found[4].pos found[4].rot scale)) * (SLR_CAR_slotTM n.part found[3] scale) * n.tm

	SLR_CAR_Node part:child tm:tm used:#() par:n how:found[5] slot:found[3] -- return
)

function SLR_CAR_natural part scale depth = -- #( transform, parent part ) from the attach lines alone
(
	if( part.kind == "car" ) then return #( (matrix3 1), undefined )
	if( depth > 12 ) then return undefined

	local res, t, wt

	for c in part.slots while (res == undefined) do
	(
		for w in c.wants while (res == undefined) do
		(
			t = SLR_CAR_getPart w[2] w[3]

			if( (t != undefined) and (t.pkey != part.pkey) ) then
			(
				for q in t.slots while (res == undefined) do
				(
					if( q.id == w[4] ) then
					(
						wt = SLR_CAR_natural t scale (depth+1)

						if( wt != undefined ) then
							res = #( ((inverse (SLR_CAR_matrix c.pos c.rot scale)) * (SLR_CAR_slotTM t q scale) * wt[1]), t )
					)
				)
			)
		)
	)

	res -- return
)

function SLR_CAR_assemble chassis scale report = -- #( nodes, listed parts that found no free slot )
(
	local nodes = #( (SLR_CAR_Node part:chassis tm:(matrix3 1) used:#() how:"root") )
	local pending = #(), keys = #( chassis.pkey )
	local i = 0, n, p, r, child, order, found
	local again = true

	while again do
	(
		while( i < nodes.count ) do
		(
			i += 1
			n = nodes[i]
			p = n.part

			for ref in p.stock do
			(
				r = SLR_CAR_resolve p.rpk ref
				child = if( r != undefined ) then (SLR_CAR_getPart r[1] r[2]) else undefined

				if( child == undefined ) then
					append report ("part 0x" + (bit.intAsHex ref) + " listed by " + p.name + " is not in the game files")
				else
				(
					-- the part that lists it first, then everything already in the car
					order = #( n )
					for k=1 to nodes.count where (k != i) do append order nodes[k]

					found = SLR_CAR_findSlot child order true

					if( found != undefined ) then
					(
						append found[1].used found[2]
						append nodes (SLR_CAR_place child found scale)

						if( (findItem keys child.pkey) == 0 ) then append keys child.pkey
					)
					else
						append pending child
				)
			)
		)

		-- try again once the rest is in: a headlight listed before the grill it sits on
		again = false
		local still = #()

		for c in pending where ((findItem keys c.pkey) == 0) do -- listed twice (chassis and door): no room for the copy, fine
		(
			found = SLR_CAR_findSlot c nodes true

			if( found != undefined ) then
			(
				append found[1].used found[2]
				append nodes (SLR_CAR_place c found scale)
				append keys c.pkey
				again = true
			)
			else
				append still c
		)

		pending = still
	)

	local misfits = #(), missKeys = #()

	for c in pending where ((findItem missKeys c.pkey) == 0) do
	(
		append missKeys c.pkey
		append misfits c
	)

	#( nodes, misfits ) -- return
)

function SLR_CAR_everything rel scale report = -- #( nodes, keys of the stock parts )
(
	local rpk = SLR_CAR_rpk rel
	local chassis = #(), others = #()

	for e in rpk[2] where (e.toe == 8) do
	(
		local p = SLR_CAR_getPart rel e.typeID

		if( p != undefined ) then
		(
			if( p.kind == "car" ) then append chassis p else append others p
		)
	)

	local nodes = for c in chassis collect (SLR_CAR_Node part:c tm:(matrix3 1) used:#() how:"root")
	local stockKeys = #()

	-- what each chassis comes with; parts of other rpks (default wheels...) only come this way
	for ci=1 to chassis.count do
	(
		local asm = SLR_CAR_assemble chassis[ci] scale #()

		for k=2 to asm[1].count do
		(
			local n = asm[1][k]

			if( (findItem stockKeys n.part.pkey) == 0 ) then append stockKeys n.part.pkey

			if( n.part.rpk != rel ) then
			(
				local dup = false

				for m in nodes where ((m.part.pkey == n.part.pkey) and ((distance m.tm.row4 n.tm.row4) < 1.0)) do dup = true

				if( not dup ) then
					append nodes (SLR_CAR_Node part:n.part tm:n.tm used:#() par:nodes[ci] how:n.how slot:n.slot)
			)
		)
	)

	-- every part of the rpk once, stock parts first; parts on parts need their parent placed
	local pending = #()

	for p in others where ((findItem stockKeys p.pkey) != 0) do append pending p
	for p in others where ((findItem stockKeys p.pkey) == 0) do append pending p

	local progress = true

	while( (pending.count > 0) and progress ) do
	(
		progress = false
		local rest = #()

		for p in pending do
		(
			local found = SLR_CAR_findSlot p nodes false

			if( found != undefined ) then
			(
				append nodes (SLR_CAR_place p found scale)
				progress = true
			)
			else
				append rest p
		)

		pending = rest
	)

	for p in pending do
	(
		local nat = SLR_CAR_natural p scale 0
		local n = SLR_CAR_Node part:p tm:(matrix3 1) used:#() par:(if nodes.count > 0 then nodes[1] else undefined) how:"free"

		if( nat != undefined ) then
		(
			n.tm = nat[1]
			n.how = "natural"
		)
		else
			append report ("no slot found for " + p.name + ", left where its mesh was modeled")

		append nodes n
	)

	#( nodes, stockKeys ) -- return
)

-- ---------------------------------------------------------------- meshes
function SLR_CAR_meshFile part src = -- full name of a mesh, also looked for next to the cfg
(
	local full = SLR_CAR_ROOT + src

	if( doesFileExist full ) then return full

	if( part.cfg != undefined ) then
	(
		local dataDir = getFilenamePath (trimRight (getFilenamePath (SLR_CAR_ROOT + part.cfg)) "\\/")
		full = dataDir + "meshes\\" + (filenameFromPath src)

		if( doesFileExist full ) then return full
	)

	undefined -- return
)

function SLR_CAR_renderInfo part ref = -- #( scx file, rpk, renderref entry, key ) of a render reference
(
	local r = SLR_CAR_resolve part.rpk ref
	if( r == undefined ) then return undefined

	local rpk = SLR_CAR_rpk r[1]
	if( rpk == undefined ) then return undefined

	local rr = SLR_RPK_findEntry rpk[2] 14 r[2]
	if( rr == undefined ) then return undefined

	local mr = SLR_RPK_values rr "mesh"
	if( mr.count == 0 ) then return undefined -- lights, particles: no mesh

	local src = SLR_RPK_refSource rpk[1] rpk[2] (SLR_RPK_hexToInt (SLR_RPK_firstToken mr[1])) 5 SLR_CAR_ROOT SLR_CAR_RPKS
	if( src == undefined ) then return undefined

	local file = SLR_CAR_meshFile part src
	if( file == undefined ) then return undefined

	#( file, rpk, rr, (SLR_CAR_keyOf r[1] r[2]) ) -- return
)

function SLR_CAR_meshInfo part ref = -- same for a "mesh 0x..." line (mesh entry, textures from a renderref using it)
(
	local r = SLR_CAR_resolve part.rpk ref
	if( r == undefined ) then return undefined

	local rpk = SLR_CAR_rpk r[1]
	if( rpk == undefined ) then return undefined

	local me = SLR_RPK_findEntry rpk[2] 5 r[2]
	if( me == undefined ) then return undefined

	local src = SLR_RPK_values me "sourcefile"
	if( src.count == 0 ) then return undefined

	local file = SLR_CAR_meshFile part src[1]
	if( file == undefined ) then return undefined

	local rr, mv

	for e in rpk[2] where ((e.toe == 14) and (rr == undefined)) do
	(
		mv = SLR_RPK_values e "mesh"
		if( (mv.count > 0) and ((bit.and (SLR_RPK_hexToInt (SLR_RPK_firstToken mv[1])) 0xFFFF) == r[2]) ) then rr = e
	)

	#( file, rpk, rr, ("mesh|" + (SLR_CAR_keyOf r[1] r[2])) ) -- return
)

function SLR_CAR_visuals part lights scale = -- #( #( info, transform in the part, role, wheel ) )
(
	local out = #(), seen = #()
	local info, tm, sig, role, dup

	for r in part.renders do
	(
		if( (r.flags == undefined) or lights ) then
		(
			info = SLR_CAR_renderInfo part r.rref

			if( info != undefined ) then
			(
				tm = SLR_CAR_matrix (if r.pos == undefined then [0,0,0] else r.pos) r.rot scale

				if( (r.ctx == 1) and (r.ctxIdx >= 1) and (r.ctxIdx <= part.wheels.count) ) then
					tm = tm * (SLR_CAR_matrix part.wheels[r.ctxIdx][1] part.wheels[r.ctxIdx][2] scale)
				else
				if( (r.ctx == 2) and (part.steering != undefined) ) then
					tm = tm * (SLR_CAR_matrix part.steering.pos part.steering.rot scale)

				sig = (toLower info[1]) + "@" + (tm.row4 as string) + (tm.row1 as string)

				if( (findItem seen sig) == 0 ) then -- the same mesh written again for each lod
				(
					append seen sig
					role = if( r.flags != undefined ) then "light" else if( r.ctx == 1 ) then "wheel" else if( r.ctx == 2 ) then "steering" else "main"
					append out #( info, tm, role, r.ctxIdx )
				)
			)
		)
	)

	if( part.deformRef != undefined ) then -- chassis: its body is the deformable render
	(
		info = SLR_CAR_renderInfo part part.deformRef

		if( info != undefined ) then
		(
			dup = false
			for o in out where ((toLower o[1][1]) == (toLower info[1])) do dup = true
			if( not dup ) then append out #( info, (matrix3 1), "main", 0 )
		)
	)

	if( part.meshRef != undefined ) then
	(
		dup = false
		for o in out where (o[3] == "main") do dup = true

		if( not dup ) then
		(
			info = SLR_CAR_meshInfo part part.meshRef
			if( info != undefined ) then append out #( info, (matrix3 1), "main", 0 )
		)
	)

	out -- return
)

function SLR_CAR_findPhys part b = -- phys mesh: path of the cfg, rpk entry named in the comment, or next to the cfg
(
	local full = SLR_CAR_ROOT + b.shp

	if( doesFileExist full ) then return full

	if( b.rref != undefined ) then
	(
		local rpk = SLR_CAR_rpk part.rpk

		if( rpk != undefined ) then
		(
			local e = SLR_RPK_findEntry rpk[2] 9 (bit.and b.rref 0xFFFF)

			if( e != undefined ) then
			(
				local v = SLR_RPK_values e "shape"

				if( v.count > 0 ) then
				(
					full = SLR_CAR_ROOT + (SLR_RPK_firstToken v[1])
					if( doesFileExist full ) then return full
				)
			)
		)
	)

	SLR_CAR_meshFile part b.shp -- return
)

function SLR_CAR_physList part scale = -- #( #( scx file or undefined, sphere radius, transform in the part, name ) )
(
	local out = #()
	local shp

	for b in part.bodies where (b.kind == "body") do
	(
		shp = toLower b.shp

		if( (getFilenameType shp) == ".scx" ) then
		(
			local file = SLR_CAR_findPhys part b
			if( file != undefined ) then append out #( file, 0.0, (SLR_CAR_matrix b.pos b.rot scale), (getFilenameFile b.shp), b.nocoll )
		)
		else
		if( (shp == "sphere") and (b.size >= 0.01) ) then -- skip the dummy spheres of 1 mm
			append out #( undefined, b.size, (SLR_CAR_matrix b.pos b.rot scale), (part.name + "_phys_sphere"), b.nocoll )
	)

	for i=1 to part.wheels.count where (part.wheelR[i] > 0.0) do -- "type 10.0 sphere 0.315" of each wheel
		append out #( undefined, part.wheelR[i], (SLR_CAR_matrix part.wheels[i][1] part.wheels[i][2] scale), ("_phys_wheel_" + (SLR_CAR_wheelName i)), false )

	out -- return
)

-- ----------------------------------------------------------------- scene
function SLR_CAR_status txt =
(
	if( (SLR_Car_Import != undefined) and SLR_Car_Import.open ) then
	(
		SLR_Car_Import.done_lbl.text = txt
		windows.processPostedMessages()
	)
)

function SLR_CAR_layer layName hidden = -- existing layer or a new one
(
	local lay = LayerManager.getLayerFromName layName

	if( lay == undefined ) then
	(
		lay = LayerManager.newLayerFromName layName

		if( hidden and (lay != undefined) ) then
			try( lay.isHidden = true ) catch( try( lay.on = false ) catch() )
	)

	lay -- return
)

function SLR_CAR_newObject info tm objName noMat cacheKeys cacheObjs = -- imports a mesh, or instances it when already in
(
	local key = (toLower info[1]) + "|" + info[4]
	local idx = if SLR_CAR_INSTANCE then (findItem cacheKeys key) else 0
	local o

	if( idx != 0 ) then
		o = instance cacheObjs[idx]
	else
	(
		local tex = if noMat then #() else undefined

		if( (not noMat) and (info[3] != undefined) ) then
			tex = SLR_RPK_renderTextures info[2][1] info[2][2] info[3] SLR_CAR_ROOT SLR_CAR_RPKS

		local objs = ImportSCXfile info[1] texList:tex objName:objName quiet:true sceneRedraw:false noMaterial:noMat

		if( (objs == undefined) or (objs.count == 0) ) then return undefined

		o = objs[1]
		for k=2 to objs.count do delete objs[k]

		append cacheKeys key
		append cacheObjs o
	)

	o.name = objName
	o.transform = tm
	o -- return
)

function SLR_CAR_wheelOf n = -- wheel block of a part on a wheel slot (rim, brake...), or on a part that is (tyre on the rim); 0 if none
(
	local w = 0

	while( (n != undefined) and (w == 0) ) do
	(
		if( (n.slot != undefined) and (n.slot.stype != undefined) and (n.slot.stype >= 100) and (n.slot.wblock > 0) ) then w = n.slot.wblock
		n = n.par
	)

	w -- return
)

function SLR_CAR_rootIdx nodes i = -- index of the chassis a node hangs from
(
	local j = i

	while( (nodes[j].par != undefined) and (nodes[j].par.idx != undefined) ) do j = nodes[j].par.idx

	j -- return
)

function SLR_CAR_nearest nodes handles i = -- object or group of node i, or of the nearest part below it that has one
(
	local j = i

	while( (j != undefined) and (handles[j] == undefined) ) do
		j = if( nodes[j].par != undefined ) then nodes[j].par.idx else undefined

	if( j == undefined ) then undefined else handles[j] -- return
)

function SLR_CAR_makeGroup objs grpName = -- max group of the objects, on the layer of the first one
(
	local g = group objs name:grpName select:false

	if( g != undefined ) then
	(
		try( objs[1].layer.addNode g ) catch()

		local allHidden = true
		for o in objs where (not o.isHidden) do allHidden = false
		if( allHidden ) then g.isHidden = true

		setUserProp g "slrr_role" "group"
	)

	g -- return
)

function ImportCar rpk_name variant mode:SLR_CAR_MODE quiet:false =
(
	-- returns #( created objects, report lines ) or undefined
	local t0 = timeStamp()
	local root = SLR_gameRoot (getFilenamePath rpk_name)

	if( root == undefined ) then
	(
		if( not quiet ) then messageBox "Game folder not found (no sl.rpk/cars.rpk above the rpk)" title:"SCX Car Import" beep:true
		return undefined
	)

	SLR_CAR_ROOT = root
	SLR_CAR_RPKS = #( #(), #() )
	SLR_CAR_PKEYS = #()
	SLR_CAR_PARTS = #()

	local rel = SLR_CAR_rel root rpk_name
	local carName = getFilenameFile rpk_name
	local scale = SLR_CAR_SCALE
	local report = #()

	if( (SLR_CAR_rpk rel) == undefined ) then
	(
		if( not quiet ) then messageBox ((filenameFromPath rpk_name) + " is not a rpk") title:"SCX Car Import" beep:true
		return undefined
	)

	local chassis = SLR_CAR_chassisList rel

	if( chassis.count == 0 ) then
	(
		if( not quiet ) then messageBox ((filenameFromPath rpk_name) + " has no car chassis inside") title:"SCX Car Import" beep:true
		return undefined
	)

	SLR_CAR_status "putting the car together..."

	local nodes, misfits = #(), stockKeys = #(), grp

	if( mode == 1 ) then
	(
		local ch = chassis[ (if (variant >= 1) and (variant <= chassis.count) then variant else 1) ]
		local asm = SLR_CAR_assemble ch scale report

		nodes = asm[1]
		misfits = asm[2]
		grp = carName + " " + ch.name

		for m in misfits do -- listed, but no free slot: brought hidden, where its attach lines put it
		(
			local nat = SLR_CAR_natural m scale 0
			append nodes (SLR_CAR_Node part:m tm:(if nat != undefined then nat[1] else (matrix3 1)) used:#() par:nodes[1] how:"misfit")
			append report ("listed by the car but no free slot for it: " + m.name + " (hidden layer)")
		)
	)
	else
	(
		local every = SLR_CAR_everything rel scale report

		nodes = every[1]
		stockKeys = every[2]
		grp = carName
	)

	-- the mesh importer settings that would get in the way
	local oldSeparate = SLR_SCX_IMPORT_SEPARATE
	local oldScale = SLR_SCX_IMPORT_SCALE
	SLR_SCX_IMPORT_SEPARATE = false
	SLR_SCX_IMPORT_SCALE = scale/100.0

	local created = #(), owners = #(), roles = #(), heads = #()
	local cacheKeys = #(), cacheObjs = #()
	local physMat, nPhys = 0, nLights = 0, nParts = 0
	local pre = if SLR_CAR_PREFIX then (carName + "_") else ""
	local failed

	for i=1 to nodes.count do nodes[i].idx = i

	disableSceneRedraw()

	try
	(
		with undo off
		(
			for ni=1 to nodes.count do
			(
				local n = nodes[ni]
				local p = n.part
				local base = pre + p.name

				if( (ni == 1) or ((mod ni 5) == 0) ) then
					SLR_CAR_status ("importing " + (ni as string) + "/" + (nodes.count as string) + ": " + p.name)

				-- parts of a wheel (default wheels, hubs, the tyre on the rim...) get the wheel in the name
				local wb = SLR_CAR_wheelOf n
				if( wb > 0 ) then base += "_" + (SLR_CAR_wheelName wb)

				-- layer of the part
				local layName = grp

				if( n.how == "misfit" ) then layName = grp + " (no free slot)"
				else
				if( mode == 2 ) then
				(
					if( n.how == "root" ) then layName = carName + " chassis"
					else
					if( (findItem stockKeys p.pkey) != 0 ) then layName = carName + " parts"
					else layName = carName + " extra parts"
				)

				local mainCount = 0, steerCount = 0
				local vis = SLR_CAR_visuals p SLR_CAR_LIGHTS scale

				for v in vis do
				(
					local objName

					case v[3] of
					(
						"main":
						(
							mainCount += 1
							objName = if( mainCount == 1 ) then base else (base + "_" + (mainCount as string))
						)
						"steering":
						(
							steerCount += 1
							objName = base + "_steering" + (if steerCount == 1 then "" else ("_" + (steerCount as string)))
						)
						"wheel": objName = base + "_wheel_" + (SLR_CAR_wheelName v[4])
						default: objName = pre + (getFilenameFile v[1][1])
					)

					local o = SLR_CAR_newObject v[1] (v[2] * n.tm) objName false cacheKeys cacheObjs

					if( o != undefined ) then
					(
						setUserProp o "slrr_part" p.name
						setUserProp o "slrr_cfg" p.cfg
						setUserProp o "slrr_rpk" p.rpk
						setUserProp o "slrr_role" v[3]

						if( v[3] == "light" ) then nLights += 1

						if( SLR_CAR_LAYERS ) then
						(
							local lay = SLR_CAR_layer (if (v[3] == "light") and (n.how != "misfit") then (grp + " lights") else layName) (n.how == "misfit")
							if( lay != undefined ) then lay.addNode o
						)
						else
						if( n.how == "misfit" ) then o.isHidden = true

						if( (v[3] == "main") and (n.obj == undefined) ) then n.obj = o

						append created o
						append owners n
						append roles v[3]
					)
				)

				if( vis.count > 0 ) then nParts += 1

				if( SLR_CAR_PHYS ) then
				(
					for ph in (SLR_CAR_physList p scale) do
					(
						local o
						local objName = pre + ph[4]

						if( ph[1] != undefined ) then
							o = SLR_CAR_newObject #( ph[1], undefined, undefined, "phys" ) (ph[3] * n.tm) objName true cacheKeys cacheObjs
						else
						(
							o = Sphere radius:(ph[2]*scale) segs:16 name:objName
							o.transform = ph[3] * n.tm
						)

						if( o != undefined ) then
						(
							if( physMat == undefined ) then
							(
								physMat = Standard name:"SLRR phys"
								physMat.diffuse = color 70 190 90
								physMat.opacity = 40.0
							)

							o.material = physMat
							o.wirecolor = color 70 190 90
							o.renderable = false
							o.xray = true

							setUserProp o "slrr_part" p.name
							setUserProp o "slrr_role" "phys"

							if( SLR_CAR_LAYERS ) then
							(
								local lay = SLR_CAR_layer (grp + " phys") false
								if( lay != undefined ) then lay.addNode o
							)

							if( (n.how == "misfit") or (ph[5] == true) ) then o.isHidden = true -- "nocollision" bodies (water box) come hidden

							nPhys += 1
							append created o
							append owners n
							append roles "phys"
						)
					)
				)
			)

			-- groups
			local inGroup = #(), partHead = #(), headOwner = #()

			for k=1 to created.count do append inGroup false
			for i=1 to nodes.count do append partHead undefined

			if( SLR_CAR_GROUPS == 2 ) then -- phys, lights, wheels and the parts with no free slot, a group each
			(
				local setNames = #( (grp + " phys"), (grp + " lights"), (grp + " wheels"), (grp + " (no free slot)") )
				local sets = #( #(), #(), #(), #() )

				for k=1 to created.count do
				(
					local n = owners[k]
					local which = 0

					if( n.how == "misfit" ) then which = 4
					else
					if( roles[k] == "phys" ) then which = 1
					else
					if( roles[k] == "light" ) then which = 2
					else
					if( (roles[k] == "wheel") or ((SLR_CAR_wheelOf n) > 0) ) then which = 3

					if( which > 0 ) then append sets[which] k
				)

				for w=1 to 4 where (sets[w].count > 0) do
				(
					local members = #()
					for k in sets[w] do append members created[k] -- plain loops: collect here trips a maxscript closure bug

					local g = SLR_CAR_makeGroup members setNames[w]

					if( g != undefined ) then
					(
						for k in sets[w] do inGroup[k] = true
						append heads g
						append headOwner (SLR_CAR_rootIdx nodes owners[sets[w][1]].idx)
					)
				)
			)
			else
			if( SLR_CAR_GROUPS == 3 ) then -- every part in a group with its phys and lights
			(
				local byNode = #()

				for i=1 to nodes.count do append byNode #()
				for k=1 to created.count do append byNode[owners[k].idx] k

				for i=1 to nodes.count where (byNode[i].count >= 2) do
				(
					local nm = (if nodes[i].obj != undefined then nodes[i].obj.name else (pre + nodes[i].part.name)) + "_group"
					local members = #()
					for k in byNode[i] do append members created[k]

					local g = SLR_CAR_makeGroup members nm

					if( g != undefined ) then
					(
						for k in byNode[i] do inGroup[k] = true
						partHead[i] = g
					)
				)
			)

			-- hierarchy: a part is represented by its group, or by its main object
			local handles = #()

			for i=1 to nodes.count do
				append handles (if partHead[i] != undefined then partHead[i] else nodes[i].obj)

			if( SLR_CAR_HIERARCHY != 3 ) then
			(
				for k=1 to created.count where (not inGroup[k]) do
				(
					local o = created[k]
					local i = owners[k].idx
					local target

					if( SLR_CAR_HIERARCHY == 2 ) then
						target = handles[SLR_CAR_rootIdx nodes i]
					else
					if( o == nodes[i].obj ) then
						target = if( nodes[i].par != undefined ) then (SLR_CAR_nearest nodes handles nodes[i].par.idx) else undefined
					else
						target = SLR_CAR_nearest nodes handles i

					if( (target != undefined) and (target != o) ) then o.parent = target
				)

				for i=1 to nodes.count where ((partHead[i] != undefined) and (nodes[i].par != undefined)) do
				(
					local target = if( SLR_CAR_HIERARCHY == 2 ) then handles[SLR_CAR_rootIdx nodes i] else (SLR_CAR_nearest nodes handles nodes[i].par.idx)

					if( (target != undefined) and (target != partHead[i]) ) then partHead[i].parent = target
				)

				for h=1 to heads.count do
				(
					local target = handles[headOwner[h]]

					if( (target != undefined) and (target != heads[h]) ) then heads[h].parent = target
				)
			)

			for g in partHead where (g != undefined) do append heads g
			join created heads
		)
	)
	catch
	(
		failed = getCurrentException()
	)

	SLR_SCX_IMPORT_SEPARATE = oldSeparate
	SLR_SCX_IMPORT_SCALE = oldScale

	enableSceneRedraw()
	redrawViews()

	if( failed != undefined ) then
	(
		format "SCX Car Import failed: %\n" failed
		append report ("import failed: " + failed)
		if( not quiet ) then messageBox ("Import failed:\n" + failed) title:"SCX Car Import" beep:true
		return #( created, report )
	)

	local mats = #(), gone = #()

	for o in created where (o.material != undefined) do appendIfUnique mats o.material

	for mt in mats do
	(
		for b in (getClassInstances BitmapTexture target:mt) do
		(
			if( (b.filename != "") and (not (doesFileExist b.filename)) ) then appendIfUnique gone (filenameFromPath b.filename)
		)
	)

	if( gone.count > 0 ) then
	(
		local txt = ""
		for g in gone do txt += (if txt == "" then "" else ", ") + g
		append report ("the rpk points to texture files that are not in the game folder: " + txt)
	)

	format "SCX Car Import: % - % part(s) with meshes, % phys, % light(s), % group(s), % object(s) in %s\n" \
		grp nParts nPhys nLights heads.count created.count (((timeStamp()-t0)/1000.0) as string)

	for l in report do format "   %\n" l

	#( created, report ) -- return
)

-- ------------------------------------------------------------------- UI
if( SLR_Car_Import != undefined ) then -- recreate if window already exists
	DestroyDialog SLR_Car_Import

rollout SLR_Car_Import "SCX Car Import v2.2"
(
	local carRpk = ""

	button rpk_btn "Open car RPK..." pos:[8,8] width:110 height:22
	label car_lbl "no car loaded" pos:[126,12] width:190
	label info_lbl "" pos:[8,36] width:308

	groupBox what_grp "What to import" pos:[6,54] width:312 height:92
	radioButtons mode_rdo "" pos:[14,72] labels:#("Assembled car: chassis + the parts it comes with", "Everything: all chassis and all parts of the rpk") columns:1
	label var_lbl "Chassis:" pos:[14,118]
	dropdownList var_ddl "" pos:[64,114] width:246 items:#()

	groupBox opt_grp "Options" pos:[6,152] width:312 height:194
	checkbox phys_chck "Phys meshes (collision bodies, not rendered)" pos:[14,170]
	checkbox lights_chck "Light meshes (lamps lit, brake, reverse)" pos:[14,190]
	checkbox layers_chck "Layers for parts, phys and lights" pos:[14,210]
	checkbox inst_chck "Instance repeated meshes" pos:[14,230]
	checkbox prefix_chck "Car name in front of the object names" pos:[14,250]
	label hier_lbl "Links:" pos:[14,276]
	dropdownList hier_ddl "" pos:[64,272] width:246 items:#("Each part linked to the part it sits on", "Every part linked to the chassis", "No links")
	label grp_lbl "Groups:" pos:[14,300]
	dropdownList grp_ddl "" pos:[64,296] width:246 items:#("No groups", "Phys, lights and wheels in groups", "Each part grouped with its phys and lights")
	label scale_lbl "Scale:" pos:[14,324]
	spinner scale_spn "" pos:[64,323] fieldwidth:60 type:#float range:[0.001,10000.0,100.0] scale:1.0

	button imp_btn "IMPORT CAR" pos:[8,354] width:308 height:28 enabled:false
	label done_lbl "" pos:[8,388] width:308 height:30

	function lockUI state = -- no clicks while a car is read or imported: the status line lets the window take them
	(
		for c in #(rpk_btn, mode_rdo, phys_chck, lights_chck, layers_chck, inst_chck, prefix_chck, hier_ddl, grp_ddl, scale_spn) do
			c.enabled = not state

		var_ddl.enabled = (not state) and (SLR_CAR_MODE == 1)
		imp_btn.enabled = (not state) and (var_ddl.items.count > 0)
	)

	function loadCar file_name =
	(
		carRpk = file_name
		car_lbl.text = filenameFromPath file_name
		done_lbl.text = "reading..."
		var_ddl.items = #()
		windows.processPostedMessages()

		local root = SLR_gameRoot (getFilenamePath file_name)

		if( root == undefined ) then
		(
			info_lbl.text = ""
			done_lbl.text = "game folder not found above this rpk"
			return false
		)

		SLR_CAR_ROOT = root
		SLR_CAR_RPKS = #( #(), #() )
		SLR_CAR_PKEYS = #()
		SLR_CAR_PARTS = #()

		local rel = SLR_CAR_rel root file_name
		local rpk = SLR_CAR_rpk rel

		if( rpk == undefined ) then
		(
			info_lbl.text = ""
			done_lbl.text = "not a rpk"
			return false
		)

		local chassis = #(), nParts = 0

		for e in rpk[2] where (e.toe == 8) do
		(
			local p = SLR_CAR_getPart rel e.typeID

			if( p != undefined ) then
			(
				if( p.kind == "car" ) then append chassis p else nParts += 1
			)
		)

		var_ddl.items = for c in chassis collect c.name

		info_lbl.text = (chassis.count as string) + " chassis, " + (nParts as string) + " part(s) in this rpk"
		done_lbl.text = if( chassis.count == 0 ) then "no car chassis in this rpk" else ""

		true
	)

	on rpk_btn pressed do
	(
		local file_name = getOpenFileName caption:"Open the rpk of a car" \
			types:"SLRR resource (*.rpk)|*.rpk|All files (*.*)|*.*" filename:SLR_CAR_LAST

		if( file_name != undefined ) then
		(
			SLR_CAR_LAST = file_name
			lockUI true

			try( loadCar file_name )
			catch
			(
				lockUI false
				throw()
			)

			lockUI false
		)
	)

	on mode_rdo changed arg do
	(
		SLR_CAR_MODE = arg
		var_ddl.enabled = (arg == 1)
	)

	on phys_chck changed arg do SLR_CAR_PHYS = arg
	on lights_chck changed arg do SLR_CAR_LIGHTS = arg
	on layers_chck changed arg do SLR_CAR_LAYERS = arg
	on inst_chck changed arg do SLR_CAR_INSTANCE = arg
	on prefix_chck changed arg do SLR_CAR_PREFIX = arg
	on hier_ddl selected arg do SLR_CAR_HIERARCHY = arg
	on grp_ddl selected arg do SLR_CAR_GROUPS = arg
	on scale_spn changed arg do SLR_CAR_SCALE = arg

	on imp_btn pressed do
	(
		if( carRpk != "" ) then
		(
			local t0 = timeStamp()
			local res

			lockUI true

			try( res = ImportCar carRpk var_ddl.selection mode:SLR_CAR_MODE )
			catch
			(
				if SLR_Car_Import.open then lockUI false
				throw()
			)

			if( not SLR_Car_Import.open ) then return false -- window closed while importing

			lockUI false

			if( res == undefined ) then
				done_lbl.text = "nothing imported"
			else
			(
				local txt = (res[1].count as string) + " object(s) in " + (((timeStamp()-t0)/1000.0) as string) + "s"
				if( res[2].count > 0 ) then txt += ", " + (res[2].count as string) + " note(s) in the MAXScript listener"
				done_lbl.text = txt

				if( res[1].count > 0 ) then select (for o in res[1] where (o.parent == undefined) collect o)
			)
		)
	)

	on SLR_Car_Import open do
	(
		mode_rdo.state = SLR_CAR_MODE
		var_ddl.enabled = (SLR_CAR_MODE == 1)
		phys_chck.checked = SLR_CAR_PHYS
		lights_chck.checked = SLR_CAR_LIGHTS
		layers_chck.checked = SLR_CAR_LAYERS
		inst_chck.checked = SLR_CAR_INSTANCE
		prefix_chck.checked = SLR_CAR_PREFIX
		hier_ddl.selection = SLR_CAR_HIERARCHY
		grp_ddl.selection = SLR_CAR_GROUPS
		scale_spn.value = SLR_CAR_SCALE
	)
)

CreateDialog SLR_Car_Import width:324 height:422
