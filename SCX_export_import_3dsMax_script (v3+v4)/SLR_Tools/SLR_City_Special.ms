-- ==========================================================================
--  SCX City Import SPECIAL for Street Legal Racing: Redline
--  Brings a whole city in one go:
--    - the baked meshes of city 1 and city 2 (POLY blocks inside the rpk)
--    - the material list of each one (.scy) with the textures of the rpk
--    - the native objects (trees, lamps, signs...) placed as instances
--    - optionally the road graph of the .trc
--  Every native is a "gametype 0x... / params x,y,z,yaw,pitch,roll" record
--  stored in the spatial tree of the map rpk.
-- ==========================================================================
if( SLR_RPK_read == undefined ) then -- mesh importer holds the readers
	fileIn ((GetDir #scripts) + "\\SLR_Tools\\SLR_SCX_import.ms")

if( SLR_CITYSP_CITY1 == undefined ) then -- keep chosen options between runs
(
	global SLR_CITYSP_CITY1 = true
	global SLR_CITYSP_CITY2 = true
	global SLR_CITYSP_MESH = true
	global SLR_CITYSP_NATIVES = true
	global SLR_CITYSP_ROADS = false
	global SLR_CITYSP_LIMIT = 0 -- instances per type, 0 = all
	global SLR_CITYSP_SCALE = 100.0 -- the rpk is in meters, the object meshes in centimeters
	global SLR_CITYSP_GAME = ""
)

struct SLR_CITY_Native ( ref, pos, yaw, pitch, roll )

function SLR_CITY_nativeList rpk_name = -- every native placed in a map rpk
(
	local rsds = #()
	local polys = #()
	local rpk = SLR_RPK_read rpk_name

	if( rpk == undefined ) then return #()

	local f = fopen rpk_name "rb"

	if( f == undefined ) then return #()

	for e in rpk[2] where ((e.toe == 1) and (e.rsdLen > 0)) do
		SLR_RPK_walkRegion f e.rsdOff e.rsdLen polys rsds

	local out = #()
	local txt, lines, l, ref, vals, nat

	for blk in rsds do
	(
		fseek f (blk[1]+8) #seek_set
		txt = SLR_SCX_readString f (blk[2]-8)
		lines = filterString txt "\r\n"

		ref = undefined; vals = undefined

		for i=1 to lines.count do
		(
			l = lines[i]

			if( (substring l 1 9) == "gametype " ) then
				ref = SLR_RPK_hexToInt (SLR_RPK_firstToken (substring l 10 -1))
			else
			if( (substring l 1 7) == "params " ) then
				vals = filterString (substring l 8 -1) ","
		)

		if( (ref != undefined) and (vals != undefined) and (vals.count >= 6) ) then
		(
			nat = SLR_CITY_Native ref:ref pos:[ (vals[1] as float), (vals[2] as float), (vals[3] as float) ] \
				yaw:(vals[4] as float) pitch:(vals[5] as float) roll:(vals[6] as float)

			append out nat
		)
	)

	fclose f
	out -- return
)

function SLR_CITY_placeNatives rpk_name natives root scale limit layerName =
(
	-- imports one model per type and puts an instance at every placement
	local cache = #( #(), #() )
	local types = #(), counts = #()
	local idx, created = #(), t0 = timeStamp()

	for n in natives do
	(
		idx = findItem types n.ref

		if( idx == 0 ) then ( append types n.ref; append counts 1 )
		else counts[idx] += 1
	)

	format "SCX City Special: % native(s) of % type(s)\n" natives.count types.count

	local protos = #(); protos[types.count] = undefined
	local model, objs, lay

	if( layerName != undefined ) then
	(
		lay = LayerManager.getLayerFromName layerName

		if( lay == undefined ) then lay = LayerManager.newLayerFromName layerName
	)

	-- one model per type
	for i=1 to types.count do
	(
		model = SLR_CITY_nativeModel rpk_name types[i] root cache

		if( model != undefined ) then
		(
			objs = ImportSCXfile model[1] texList:model[2] objName:(getFilenameFile model[1]) quiet:true

			if( (objs != undefined) and (objs.count > 0) ) then
			(
				protos[i] = #( objs[1], model[3], model[4] )

				for k=2 to objs.count do delete objs[k] -- keep one node per type
			)
		)
		else
			format "SCX City Special: type 0x%08X has no model (sound/ambience?)\n" types[i]
	)

	-- one instance per placement
	local used = #(); used[types.count] = 0
	local proto, inst, tm, lp

	for n in natives do
	(
		idx = findItem types n.ref
		proto = protos[idx]

		if( proto != undefined ) then
		(
			if( (limit == 0) or (used[idx] == undefined) or (used[idx] < limit) ) then
			(
				if( used[idx] == undefined ) then used[idx] = 0
				used[idx] += 1

				-- placement: rotation around the game up axis (max Z), then position
				tm = rotateZMatrix (radToDeg n.yaw)

				if( n.pitch != 0.0 ) then tm = (rotateXMatrix (radToDeg n.pitch)) * tm
				if( n.roll != 0.0 ) then tm = (rotateYMatrix (radToDeg -n.roll)) * tm

				tm.row4 = [ n.pos.x*scale, -n.pos.z*scale, n.pos.y*scale ]

				lp = proto[2] -- offset of the model inside the object
				local ltm = transMatrix [ lp.x*scale, -lp.z*scale, lp.y*scale ]

				inst = instance proto[1]
				inst.transform = ltm * tm
				inst.name = proto[1].name

				append created inst

				if( lay != undefined ) then lay.addNode inst
			)
		)
	)

	-- the prototypes stay as the first instance of their type
	for p in protos where (p != undefined) do
	(
		if( lay != undefined ) then lay.addNode p[1]
		p[1].isHidden = true
	)

	format "SCX City Special: % instance(s) placed in %s\n" created.count (((timeStamp()-t0)/1000.0) as string)
	created -- return
)

function ImportCitySpecial gameDir =
(
	local root = gameDir

	if( (root == undefined) or (root == "") ) then return undefined
	if( (root[root.count] != "\\") and (root[root.count] != "/") ) then root += "\\"

	local scale = SLR_CITYSP_SCALE
	local created = #()
	local t0 = timeStamp()
	local maps = #()

	if( SLR_CITYSP_CITY1 ) then append maps #( (root + "maps\\city.rpk"), (root + "maps\\city\\meshes\\city.SCY"), "city" )
	if( SLR_CITYSP_CITY2 ) then append maps #( (root + "maps\\city2.rpk"), (root + "maps\\city2\\meshes\\city2.SCY"), "city2" )

	disableSceneRedraw()

	try
	(
		for mp in maps do
		(
			if( not (doesFileExist mp[1]) ) then
				format "SCX City Special: % not found\n" mp[1]
			else
			(
				local matlib

				if( SLR_CITYSP_MESH ) then
				(
					matlib = SLR_SCX_materialLibrary mp[2]

					local blocks = SLR_RPK_polyBlocks mp[1]

					format "SCX City Special: % has % baked block(s)\n" (filenameFromPath mp[1]) blocks.count

					if( blocks.count > 0 ) then
					(
						local o = SLR_RPK_buildPoly mp[1] blocks (mp[3] + "_meshes") scale #() matlib

						if( o != undefined ) then
						(
							append created o

							local lay = LayerManager.getLayerFromName mp[3]
							if( lay == undefined ) then lay = LayerManager.newLayerFromName mp[3]
							lay.addNode o
						)
					)
				)

				if( SLR_CITYSP_NATIVES ) then
				(
					local nat = SLR_CITY_nativeList mp[1]

					if( nat.count > 0 ) then
						join created (SLR_CITY_placeNatives mp[1] nat root scale SLR_CITYSP_LIMIT (mp[3] + "_natives"))
				)
			)
		)

		if( SLR_CITYSP_ROADS ) then
		(
			local trc = root + "maps\\city\\ai\\city.trc"

			if( doesFileExist trc ) then
			(
				if( SLR_City_Import == undefined ) then
					fileIn ((GetDir #scripts) + "\\SLR_Tools\\SLR_City_Import.ms")

				SLR_CITY_IMPORT_MESH = false -- already imported here
				SLR_CITY_IMPORT_MATS = false
				SLR_CITY_IMPORT_SCALE = scale

				local roads = ImportCityAI trc

				if( roads != undefined ) then join created roads
			)
		)
	)
	catch
	(
		enableSceneRedraw()
		format "SCX City Special failed: %\n" (getCurrentException())
		messageBox ("Import failed:\n" + (getCurrentException())) title:"SCX City Import Special" beep:true
		return created
	)

	enableSceneRedraw()
	redrawViews()

	format "SCX City Special: % object(s) in %s\n" created.count (((timeStamp()-t0)/1000.0) as string)
	created -- return
)

-- ------------------------------------------------------------------- UI
if( SLR_City_Special != undefined ) then -- recreate if window already exists
	DestroyDialog SLR_City_Special

rollout SLR_City_Special "SCX City Import Special v1.0"
(
	label t1_lbl "Imports the whole city: baked meshes," pos:[10,8]
	label t2_lbl "materials and the objects placed on it." pos:[10,24]

	checkbox city1_chck "City 1  (maps\\city.rpk)" pos:[10,48]
	checkbox city2_chck "City 2  (maps\\city2.rpk)" pos:[10,68]

	checkbox mesh_chck "Baked meshes + materials" pos:[10,94]
	checkbox nat_chck "Native objects (trees, lamps, signs)" pos:[10,114]
	checkbox road_chck "Road graph of the .trc" pos:[10,134]

	label lim_lbl "Max instances peer type:" pos:[10,160]
	spinner lim_spn "" pos:[168,158] fieldwidth:50 type:#integer range:[0,100000,0] \
		tooltip:"0 imports every placement; a small number is handy for a quick look"

	label scale_lbl "Scale:" pos:[10,182]
	spinner scale_spn "" pos:[168,180] fieldwidth:50 type:#float range:[0.001,10000.0,100.0] scale:1.0

	label game_lbl "Game folder:" pos:[10,204]
	editText game_edt "" pos:[6,220] width:190 height:18
	button game_btn "..." pos:[198,220] width:24 height:19

	imgTag separator pos:[10,246] width:212 height:1 style:#bmp_stretch bitmap:(bitmap 1 1 color:(color 110 110 110))

	button imp_btn "IMPORT THE CITY" pos:[10,254] width:212 height:26
	label done_lbl "" pos:[10,286]

	function guessGame =
	(
		if( SLR_CITYSP_GAME != "" ) then return SLR_CITYSP_GAME

		-- the game folder of the last imported mesh, if any
		local d = SLR_SCX_IMPORT_LAST_DIR

		if( d != "" ) then
		(
			local r = SLR_gameRoot d
			if( r != undefined ) then return r
		)

		""
	)

	function update =
	(
		city1_chck.checked = SLR_CITYSP_CITY1
		city2_chck.checked = SLR_CITYSP_CITY2
		mesh_chck.checked = SLR_CITYSP_MESH
		nat_chck.checked = SLR_CITYSP_NATIVES
		road_chck.checked = SLR_CITYSP_ROADS
		lim_spn.value = SLR_CITYSP_LIMIT
		scale_spn.value = SLR_CITYSP_SCALE
		game_edt.text = guessGame()
	)

	on city1_chck changed arg do SLR_CITYSP_CITY1 = arg
	on city2_chck changed arg do SLR_CITYSP_CITY2 = arg
	on mesh_chck changed arg do SLR_CITYSP_MESH = arg
	on nat_chck changed arg do SLR_CITYSP_NATIVES = arg
	on road_chck changed arg do SLR_CITYSP_ROADS = arg
	on lim_spn changed arg do SLR_CITYSP_LIMIT = arg
	on scale_spn changed arg do SLR_CITYSP_SCALE = arg
	on game_edt changed arg do SLR_CITYSP_GAME = arg

	on game_btn pressed do
	(
		local dir = getSavePath caption:"Folder of the game (the one with sl.rpk)" initialDir:game_edt.text

		if( dir != undefined ) then
		(
			if( (dir[dir.count] != "\\") and (dir[dir.count] != "/") ) then dir += "\\"

			SLR_CITYSP_GAME = dir
			game_edt.text = dir
		)
	)

	on imp_btn pressed do
	(
		local g = game_edt.text

		if( (g == "") or (not (doesFileExist (g + "maps\\city.rpk"))) ) then
			messageBox "Point the game folder to the one that holds maps\\city.rpk" title:"SCX City Import Special" beep:true
		else
		(
			SLR_CITYSP_GAME = g
			done_lbl.text = "importing..."
			windows.processPostedMessages()

			local t0 = timeStamp()
			local created = ImportCitySpecial g

			done_lbl.text = ((if created == undefined then 0 else created.count) as string) + " object(s) in " + (((timeStamp()-t0)/1000.0) as string) + "s"
		)
	)

	on SLR_City_Special open do update()
)

CreateDialog SLR_City_Special width:232 height:312
