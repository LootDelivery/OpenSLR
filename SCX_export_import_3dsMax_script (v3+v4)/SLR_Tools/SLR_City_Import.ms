-- ==========================================================================
--  SCX City Import for Street Legal Racing: Redline
--  The city maps keep their layout in the AI files of maps\<city>\ai\ :
--    .trc  - binary, the road graph (lanes, race nodes and intersections)
--    .walk - text, the areas where pedestrians can walk (sidewalks)
--    .rest - text, spatial index over the .trc
--  This tool rebuilds that layout in 3ds Max as splines and boxes, which is
--  the only geometry of the city that does not live in the (often missing)
--  area_*.scx sector meshes.
-- ==========================================================================
if( SLR_SCX_readFile == undefined ) then -- mesh importer holds the material and rpk readers
	fileIn ((GetDir #scripts) + "\\SLR_Tools\\SLR_SCX_import.ms")

if( SLR_CITY_IMPORT_LANES == undefined ) then -- keep chosen options between runs
(
	global SLR_CITY_IMPORT_LANES = true
	global SLR_CITY_IMPORT_RACE = true
	global SLR_CITY_IMPORT_CROSS = true
	global SLR_CITY_IMPORT_WALK = false
	global SLR_CITY_IMPORT_REST = false
	global SLR_CITY_IMPORT_MATS = true
	global SLR_CITY_IMPORT_MESH = true
	global SLR_CITY_IMPORT_SCALE = 100.0 -- the .trc is in meters, the meshes in centimeters
	global SLR_CITY_IMPORT_LAST = ""
)

-- --------------------------------------------------------------- reading
function SLR_TRC_readSegment f = -- one 56 bytes spline segment (14 floats)
(
	local tx = ReadFloat f, ty = ReadFloat f, tz = ReadFloat f -- target (end)
	local sx = ReadFloat f, sy = ReadFloat f, sz = ReadFloat f -- source (start)
	local ax = ReadFloat f, ay = ReadFloat f, az = ReadFloat f -- target control point
	local ux = ReadFloat f, uy = ReadFloat f, uz = ReadFloat f -- source control point, negated ("by" is reserved in maxscript)
	ReadFloat f; ReadFloat f -- length at target / at source

	#( [tx,ty,tz], [sx,sy,sz], [ax,ay,az], [ux,uy,uz] ) -- return
)

function SLR_TRC_read file_name = -- returns #( crossing curves, race nodes, lanes, lane speeds )
(
	local f = fopen file_name "rb"

	if( f == undefined ) then
	(
		messageBox ("Could not open " + (filenameFromPath file_name)) title:"SCX City Import" beep:true
		return undefined
	)

	fseek f 0 #seek_end
	local fileSize = ftell f
	fseek f 0 #seek_set

	local version = ReadLong f #signed

	if( version != 1 ) then
	(
		fclose f
		messageBox ((filenameFromPath file_name) + " is not a version 1 .trc file") title:"SCX City Import" beep:true
		return undefined
	)

	local nCrossA = ReadLong f #signed
	local nCrossB = ReadLong f #signed
	local nRace = ReadLong f #signed
	local nLanes = ReadLong f #signed

	local cross = #(), race = #(), lanes = #(), speeds = #()
	local nLinks, nPaths, nSeg, chain

	-- intersections: the turn curves, one segment each
	for i=1 to (nCrossA+nCrossB) do
	(
		nLinks = ReadLong f #signed
		fseek f (nLinks*5) #seek_cur

		nPaths = ReadLong f #signed

		for k=1 to nPaths do
		(
			fseek f 12 #seek_cur -- from/to link, priority, unused, crossing type
			append cross #( SLR_TRC_readSegment f )
			fseek f 1 #seek_cur -- separator
		)

		fseek f 16 #seek_cur -- center + flags
	)

	-- race nodes: the axis of a street between two intersections
	for i=1 to nRace do
	(
		fseek f 40 #seek_cur -- lane list head, both intersections, both ends, zero
		nSeg = ReadLong f #signed
		chain = #()

		for k=1 to nSeg do
		(
			append chain (SLR_TRC_readSegment f)
			fseek f 1 #seek_cur
		)

		append race chain
	)

	-- lanes: what the traffic cars actually drive on
	for i=1 to nLanes do
	(
		fseek f 73 #seek_cur -- race node, links, both ends, indices...
		append speeds (ReadFloat f) -- max speed in m/s
		fseek f 8 #seek_cur -- generation priority, -1
		nSeg = ReadLong f #signed
		chain = #()

		for k=1 to nSeg do
		(
			append chain (SLR_TRC_readSegment f)
			fseek f 1 #seek_cur
		)

		append lanes chain
	)

	local left = fileSize - (ftell f)
	fclose f

	if( left != 0 ) then
		format "SCX City Import: warning, % byte(s) left after the end of the .trc\n" left

	#( cross, race, lanes, speeds ) -- return
)

function SLR_AI_readBoxes file_name = -- .walk / .rest: "minX,minY,minZ,maxX,maxY,maxZ payload"
(
	local boxes = #()

	if( not (doesFileExist file_name) ) then return boxes

	local f = openFile file_name
	local line, tok

	while( (f != undefined) and (not (eof f)) ) do
	(
		line = readLine f

		if( line != "" ) then
		(
			tok = filterString line ", \t"

			if( tok.count >= 6 ) then
			(
				append boxes #( [ (tok[1] as float), (tok[2] as float), (tok[3] as float) ], \
					[ (tok[4] as float), (tok[5] as float), (tok[6] as float) ] )
			)
		)
	)

	if( f != undefined ) then close f

	boxes -- return
)

-- -------------------------------------------------------------- building
function SLR_CITY_point p scale = -- game axes (X, Y up, Z lenght) to max
(
	[ p.x*scale, -p.z*scale, p.y*scale ]
)

function SLR_CITY_buildSplines chains name scale color = 
(
	local sh = SplineShape pos:[0,0,0] name:name wirecolor:color
	local spl, seg, pEnd, pStart, vEnd, vStart

	for c=1 to chains.count do
	(
		if( chains[c].count > 0 ) then
		(
			spl = addNewSpline sh

			for k=1 to chains[c].count do
			(
				seg = chains[c][k]
				pEnd = SLR_CITY_point seg[1] scale -- target: where this piece ends
				pStart = SLR_CITY_point seg[2] scale -- source: where it starts
				vEnd = (SLR_CITY_point seg[3] scale)/3.0 -- hermite tangent to bezier handle
				vStart = (SLR_CITY_point seg[4] scale)/3.0 -- this one is stored negated

				-- the chain walks target1 -> source1(=target2) -> source2(=target3) ...
				-- so every knot but the first is shared with the next piece
				if( k == 1 ) then
					addKnot sh spl #bezierCorner #curve pEnd (pEnd + vEnd) (pEnd - vEnd)
				else
					setOutVec sh spl (numKnots sh spl) (pEnd - vEnd)

				addKnot sh spl #bezierCorner #curve pStart (pStart - vStart) (pStart + vStart)
			)
		)
	)

	updateShape sh
	sh -- return
)

function SLR_CITY_buildBoxes boxes name scale color =
(
	local verts = #(), faces = #()
	local a, b, base

	verts[boxes.count*8] = [0,0,0]
	faces[boxes.count*12] = [1,1,1]

	for i=1 to boxes.count do
	(
		a = SLR_CITY_point boxes[i][1] scale
		b = SLR_CITY_point boxes[i][2] scale
		base = (i-1)*8

		verts[base+1] = [a.x,a.y,a.z]; verts[base+2] = [b.x,a.y,a.z]
		verts[base+3] = [b.x,b.y,a.z]; verts[base+4] = [a.x,b.y,a.z]
		verts[base+5] = [a.x,a.y,b.z]; verts[base+6] = [b.x,a.y,b.z]
		verts[base+7] = [b.x,b.y,b.z]; verts[base+8] = [a.x,b.y,b.z]

		local fb = (i-1)*12
		faces[fb+1] = [base+1,base+2,base+3]; faces[fb+2] = [base+1,base+3,base+4]
		faces[fb+3] = [base+5,base+7,base+6]; faces[fb+4] = [base+5,base+8,base+7]
		faces[fb+5] = [base+1,base+6,base+2]; faces[fb+6] = [base+1,base+5,base+6]
		faces[fb+7] = [base+2,base+7,base+3]; faces[fb+8] = [base+2,base+6,base+7]
		faces[fb+9] = [base+3,base+8,base+4]; faces[fb+10] = [base+3,base+7,base+8]
		faces[fb+11] = [base+4,base+5,base+1]; faces[fb+12] = [base+4,base+8,base+5]
	)

	local obj = mesh vertices:verts faces:faces name:name
	obj.wirecolor = color
	obj -- return
)

function SLR_CITY_importMaterials scy_name = -- the .scy of a city map is a material list with no geometry
(
	if( not (doesFileExist scy_name) ) then return undefined

	local res = SLR_SCX_readFile scy_name
	local mats = res[2]

	if( mats.count == 0 ) then return undefined

	local texNames = SLR_SCX_texFromRPK scy_name -- texture list of the map rpk
	local dirs = #( (getFilenamePath scy_name), SLR_SCX_IMPORT_TEXDIR )
	local mm = multiMaterial numsubs:mats.count name:((getFilenameFile scy_name) + "_materials")

	for i=1 to mats.count do
	(
		mm[i] = SLR_SCX_makeMaterial mats[i] texNames dirs SLR_SCX_IMPORT_SLRRMAT
		mm.names[i] = mats[i].name
	)

	meditMaterials[1] = mm -- drop it in the first slot of the material editor

	format "SCX City Import: % material(s) from % (material editor slot 1)
" mats.count (filenameFromPath scy_name)
	mm -- return
)

function ImportCityAI trc_name = -- reads a .trc (and the .walk/.rest beside it)
(
	local t0 = timeStamp()
	local data = SLR_TRC_read trc_name

	if( data == undefined ) then return undefined

	local dir = getFilenamePath trc_name
	local base = getFilenameFile trc_name
	local scale = SLR_CITY_IMPORT_SCALE
	local created = #()

	format "SCX City Import: % - % intersection curves, % race nodes, % lanes\n" \
		(filenameFromPath trc_name) data[1].count data[2].count data[3].count

	disableSceneRedraw()

	try
	(
		if( SLR_CITY_IMPORT_LANES and (data[3].count > 0) ) then
			append created (SLR_CITY_buildSplines data[3] (base + "_lanes") scale (color 255 200 0))

		if( SLR_CITY_IMPORT_RACE and (data[2].count > 0) ) then
			append created (SLR_CITY_buildSplines data[2] (base + "_racenodes") scale (color 0 160 255))

		if( SLR_CITY_IMPORT_CROSS and (data[1].count > 0) ) then
			append created (SLR_CITY_buildSplines data[1] (base + "_crossings") scale (color 255 80 80))

		if( SLR_CITY_IMPORT_WALK ) then
		(
			local boxes = SLR_AI_readBoxes (dir + base + ".walk")

			if( boxes.count > 0 ) then
			(
				format "SCX City Import: % pedestrian areas\n" boxes.count
				append created (SLR_CITY_buildBoxes boxes (base + "_walk_areas") scale (color 120 255 120))
			)
		)

		local scyMats -- material list of the map, shared with the baked geometry

		if( SLR_CITY_IMPORT_MATS or SLR_CITY_IMPORT_MESH ) then -- materials of the sector file, textures resolved by the rpk
		(
			local scy = (getFilenamePath (trimRight dir "\/")) + "meshes\\" + base + ".scy"

			if( not (doesFileExist scy) ) then scy = (getFilenamePath (trimRight dir "\/")) + "meshes\\" + base + ".SCY"

			scyMats = SLR_CITY_importMaterials scy
		)

		if( SLR_CITY_IMPORT_MESH ) then -- the city meshes are baked inside the map rpk
		(
			-- maps\\city\\ai\\city.trc  ->  maps\\city.rpk
			local mapDir = getFilenamePath (trimRight dir "\\/")
			local mapName = filenameFromPath (trimRight mapDir "\\/")
			local rpkFile = (getFilenamePath (trimRight mapDir "\\/")) + mapName + ".rpk"

			if( not (doesFileExist rpkFile) ) then
				format "SCX City Import: rpk of the map not found (%)\n" rpkFile
			else
			(
				local blocks = SLR_RPK_polyBlocks rpkFile

				if( blocks.count == 0 ) then
					format "SCX City Import: no baked geometry inside %\n" (filenameFromPath rpkFile)
				else
				(
					local bv = 0, bt = 0

					for b in blocks do ( bv += b[3]; bt += b[4] )

					format "SCX City Import: % baked block(s) in %, % verts, % tris\n" blocks.count (filenameFromPath rpkFile) bv bt

					local o = SLR_RPK_buildPoly rpkFile blocks (base + "_meshes") scale #() scyMats

					if( o != undefined ) then append created o
				)
			)
		)

		if( SLR_CITY_IMPORT_REST ) then
		(
			local boxes = SLR_AI_readBoxes (dir + base + ".rest")

			if( boxes.count > 0 ) then
			(
				format "SCX City Import: % index boxes\n" boxes.count
				append created (SLR_CITY_buildBoxes boxes (base + "_rest_boxes") scale (color 160 160 160))
			)
		)
	)
	catch
	(
		enableSceneRedraw()
		format "SCX City Import failed: %\n" (getCurrentException())
		messageBox ("Import failed:\n" + (getCurrentException())) title:"SCX City Import" beep:true
		return undefined
	)

	enableSceneRedraw()
	redrawViews()

	format "SCX City Import: % object(s) in %s\n" created.count (((timeStamp()-t0)/1000.0) as string)

	if( created.count > 0 ) then select created

	created -- return
)

-- ------------------------------------------------------------------- UI
if( SLR_City_Import != undefined ) then -- recreate if window already exists
	DestroyDialog SLR_City_Import

rollout SLR_City_Import "SCX City Import v1.0"
(
	label info1_lbl "Rebuilds the layout of a city map from" pos:[10,8]
	label info2_lbl "its AI files (maps\\<city>\\ai\\*.trc)" pos:[10,24]

	checkbox lanes_chck "Traffic lanes" pos:[10,48] tooltip:"1572 lanes in the city, the roads the traffic drives on"
	checkbox race_chck "Street axis (race nodes)" pos:[10,68]
	checkbox cross_chck "Intersection turns" pos:[10,88]
	checkbox walk_chck "Pedestrian areas (.walk)" pos:[10,108]
	checkbox rest_chck "AI index boxes (.rest)" pos:[10,128]
	checkbox mesh_chck "City meshes baked in the rpk" pos:[10,148]
	checkbox mats_chck "Materials of the sector file (.scy)" pos:[10,168]

	label scale_lbl "Scale:" pos:[10,194]
	spinner scale_spn "" pos:[100,192] fieldwidth:60 type:#float range:[0.001,10000.0,100.0] scale:1.0 \
		tooltip:"the .trc is in meters and the meshes in centimeters, so 100 keeps both together"

	imgTag separator pos:[10,218] width:190 height:1 style:#bmp_stretch bitmap:(bitmap 1 1 color:(color 110 110 110))

	button imp_btn "IMPORT CITY..." pos:[10,226] width:190 height:24
	label done_lbl "" pos:[10,256]

	function update =
	(
		lanes_chck.checked = SLR_CITY_IMPORT_LANES
		race_chck.checked = SLR_CITY_IMPORT_RACE
		cross_chck.checked = SLR_CITY_IMPORT_CROSS
		walk_chck.checked = SLR_CITY_IMPORT_WALK
		rest_chck.checked = SLR_CITY_IMPORT_REST
		mats_chck.checked = SLR_CITY_IMPORT_MATS
		mesh_chck.checked = SLR_CITY_IMPORT_MESH
		scale_spn.value = SLR_CITY_IMPORT_SCALE
	)

	on lanes_chck changed arg do SLR_CITY_IMPORT_LANES = arg
	on race_chck changed arg do SLR_CITY_IMPORT_RACE = arg
	on cross_chck changed arg do SLR_CITY_IMPORT_CROSS = arg
	on walk_chck changed arg do SLR_CITY_IMPORT_WALK = arg
	on rest_chck changed arg do SLR_CITY_IMPORT_REST = arg
	on mats_chck changed arg do SLR_CITY_IMPORT_MATS = arg
	on mesh_chck changed arg do SLR_CITY_IMPORT_MESH = arg
	on scale_spn changed arg do SLR_CITY_IMPORT_SCALE = arg

	on imp_btn pressed do
	(
		local file_name = getOpenFileName caption:"Open the road graph of a map (.trc)" \
			types:"SLRR road graph (*.trc)|*.trc|All files (*.*)|*.*" \
			filename:SLR_CITY_IMPORT_LAST

		if( file_name != undefined ) then
		(
			SLR_CITY_IMPORT_LAST = file_name
			done_lbl.text = "reading..."
			windows.processPostedMessages()

			local t0 = timeStamp()
			local created = ImportCityAI file_name

			if( created == undefined ) then done_lbl.text = ""
			else done_lbl.text = (created.count as string) + " object(s) in " + (((timeStamp()-t0)/1000.0) as string) + "s"
		)
	)

	on SLR_City_Import open do update()
)

CreateDialog SLR_City_Import width:230 height:280
