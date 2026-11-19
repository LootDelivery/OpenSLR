-- ==========================================================================
--  SCX Map Import for Street Legal Racing: Redline
--  Reads .rpk files (map, object library, car, part) and imports every mesh
--  they render, with the textures the rpk assigns to each one.
--  A map is usually split in several rpk files (the map one plus Objects.rpk,
--  Sectors\*.rpk and so on), so it can also read a whole map folder at once.
--  Uses the mesh reader and the options of SLR_SCX_import.ms
-- ==========================================================================
if( SLR_RPK_read == undefined ) then -- mesh importer holds the readers
	fileIn ((GetDir #scripts) + "\\SLR_Tools\\SLR_SCX_import.ms")

if( SLR_MAP_IMPORT_PHYS == undefined ) then -- keep chosen options between runs
(
	global SLR_MAP_IMPORT_PHYS = false
	global SLR_MAP_IMPORT_LAYER = true
	global SLR_MAP_IMPORT_LAST = ""
	global SLR_MAP_IMPORT_LAST_DIR = ""
)

struct SLR_MAP_Row ( kind, alias, file, exists, size, tex, rpk )

function SLR_MAP_scan rpk_name includePhys cache:undefined = -- every mesh used by one rpk
(
	if( cache == undefined ) then cache = #( #(), #() )

	local rpk = SLR_RPK_readCached rpk_name cache

	if( rpk == undefined ) then
	(
		format "SCX Map Import: % is not a rpk file\n" (filenameFromPath rpk_name)
		return #()
	)

	local root = SLR_gameRoot (getFilenamePath rpk_name)

	if( root == undefined ) then
	(
		format "SCX Map Import: game folder not found (no sl.rpk/cars.rpk above the rpk)\n"
		return #()
	)

	local ext = rpk[1], entries = rpk[2]
	local short = filenameFromPath rpk_name
	local rows = #()
	local refs, src, full, row

	for e in entries where (e.toe == 14) do -- render definitions
	(
		refs = SLR_RPK_values e "mesh"

		if( refs.count > 0 ) then
		(
			src = SLR_RPK_refSource ext entries (SLR_RPK_hexToInt (SLR_RPK_firstToken refs[1])) 5 root cache

			if( src != undefined ) then
			(
				full = root + src
				row = SLR_MAP_Row kind:"mesh" alias:e.alias file:full exists:(doesFileExist full) size:0 \
					tex:(SLR_RPK_renderTextures ext entries e root cache) rpk:short

				if( row.exists ) then row.size = getFileSize full

				append rows row

				-- a sector file (.scy) is only the material table of the map: the real
				-- geometry is the .scx with the same name beside it, or the .scx files
				-- inside a folder with that name
				if( (toLower (getFilenameType full)) == ".scy" ) then
				(
					local mdir = getFilenamePath full
					local mbase = getFilenameFile full
					local extra = getFiles (mdir + mbase + ".scx")
					join extra (getFiles (mdir + mbase + "\\*.scx"))

					for g in extra do
						append rows (SLR_MAP_Row kind:"mesh" alias:e.alias file:g exists:true size:(getFileSize g) tex:row.tex rpk:short)
				)
			)
		)
	)

	if( includePhys ) then
	(
		for e in entries where (e.toe == 9) do -- collision shapes
		(
			refs = SLR_RPK_values e "shape"

			if( refs.count > 0 ) then
			(
				full = root + refs[1]
				row = SLR_MAP_Row kind:"phys" alias:e.alias file:full exists:(doesFileExist full) size:0 tex:#() rpk:short

				if( row.exists ) then row.size = getFileSize full

				append rows row
			)
		)
	)

	rows -- return
)

function SLR_MAP_rpkList dir = -- every rpk of a map: the ones inside the folder and the one beside it
(
	local clean = trimRight dir "\\/"
	local parent = getFilenamePath clean
	local name = filenameFromPath clean
	local list = #()

	append list (parent + name + ".rpk") -- maps\Addon_Maps\shibuya.rpk
	join list (getFiles (clean + "\\*.rpk"))

	for d1 in (getDirectories (clean + "\\*")) do -- Objects\, Sectors\, Meshes\ ...
	(
		join list (getFiles (d1 + "*.rpk"))

		for d2 in (getDirectories (d1 + "*")) do
			join list (getFiles (d2 + "*.rpk"))
	)

	local out = #(), seen = #()

	for f in list where ((doesFileExist f) and ((findItem seen (toLower f)) == 0)) do
	(
		append seen (toLower f)
		append out f
	)

	out -- return
)

function SLR_MAP_scanFolder dir includePhys = -- merges the meshes of every rpk of a map
(
	local files = SLR_MAP_rpkList dir
	local cache = #( #(), #() ) -- shared, the rpk files reference each other
	local seen = Dictionary #string
	local rows = #(), key

	for f in files do
	(
		format "SCX Map Import: reading %\n" (filenameFromPath f)

		for r in (SLR_MAP_scan f includePhys cache:cache) do
		(
			key = toLower (r.kind + "|" + r.alias + "|" + r.file)

			if( not (hasDictValue seen key) ) then -- the same mesh is often listed by several rpk
			(
				seen[key] = true
				append rows r
			)
		)
	)

	-- geometry baked inside the rpk itself (city, test_track, ROC...)
	for f in files do
	(
		local blocks = SLR_RPK_polyBlocks f

		if( blocks.count > 0 ) then
		(
			local bv = 0, bt = 0

			for b in blocks do ( bv += b[3]; bt += b[4] )

			append rows (SLR_MAP_Row kind:"poly" alias:((getFilenameFile f) + " (baked)") file:f exists:true size:(bv*56 + bt*12) tex:#() rpk:(filenameFromPath f))

			format "SCX Map Import: % has % baked block(s), % verts, % tris\n" (filenameFromPath f) blocks.count bv bt
		)
	)

	-- loose .scx files of the map folder that no rpk points to
	local known = Dictionary #string

	for r in rows do known[(toLower r.file)] = true

	local clean = trimRight dir "\\/"
	local loose = #()
	join loose (getFiles (clean + "\\*.scx"))

	for d1 in (getDirectories (clean + "\\*")) do
	(
		join loose (getFiles (d1 + "*.scx"))

		for d2 in (getDirectories (d1 + "*")) do
		(
			join loose (getFiles (d2 + "*.scx"))

			for d3 in (getDirectories (d2 + "*")) do
				join loose (getFiles (d3 + "*.scx"))
		)
	)

	for g in loose where (not (hasDictValue known (toLower g))) do
	(
		known[(toLower g)] = true
		append rows (SLR_MAP_Row kind:"loose" alias:(getFilenameFile g) file:g exists:true size:(getFileSize g) tex:#() rpk:"(not in a rpk)")
	)

	rows -- return
)

function SLR_MAP_sizeStr bytes =
(
	if( bytes >= 1048576 ) then ((((bytes/104857.6) as integer)/10.0) as string) + " MB"
	else if( bytes >= 1024 ) then (((bytes/1024) as integer) as string) + " KB"
	else if( bytes > 0 ) then ((bytes as integer) as string) + " B"
	else "0 KB"
)

function SLR_MAP_label row multi = -- one line of the list
(
	local s = row.alias

	while( s.count < 24 ) do s += " "
	s += (filenameFromPath row.file)
	while( s.count < 56 ) do s += " "

	if( row.exists ) then s += (SLR_MAP_sizeStr row.size) else s += "MISSING"

	if( row.kind == "phys" ) then s += "   [collision]"
	else if( row.kind == "loose" ) then s += "   [not in the rpk]"
	else if( row.kind == "poly" ) then s += "   [geometry baked in the rpk]"
	else if( row.tex.count > 0 ) then s += ("   " + (row.tex.count as string) + " tex")

	if( (toLower (getFilenameType row.file)) == ".scy" ) then
		s += "   [material list, no geometry]"

	if( multi ) then s += ("   - " + row.rpk)

	s -- return
)

function SLR_MAP_importRows rows sel layerName = -- imports the given rows, returns the objects
(
	local created = #(), objs, lay
	local t0 = timeStamp()

	if( layerName != undefined ) then
	(
		lay = LayerManager.getLayerFromName layerName

		if( lay == undefined ) then lay = LayerManager.newLayerFromName layerName
	)

	for k=1 to sel.count do
	(
		local row = rows[sel[k]]

		if( not row.exists ) then
			format "SCX Map Import: % is missing on disk (%)\n" row.alias row.file
		else
		if( row.kind == "poly" ) then -- geometry baked inside the rpk
		(
			local blocks = SLR_RPK_polyBlocks row.file
			local matlib

			-- the material list of a baked map is the .scy the rpk points to
			for other in rows where (other.exists and ((toLower (getFilenameType other.file)) == ".scy")) do
			(
				matlib = SLR_SCX_materialLibrary other.file
				exit
			)

			local o = SLR_RPK_buildPoly row.file blocks (getFilenameFile row.file) 100.0 #() matlib

			if( o != undefined ) then
			(
				append created o

				if( lay != undefined ) then lay.addNode o
			)
		)
		else
		(
			local tl = if( row.tex.count > 0 ) then row.tex else undefined -- let the importer look for the rpk

			objs = ImportSCXfile row.file texList:tl objName:row.alias quiet:true

			if( objs != undefined ) then
			(
				join created objs

				if( lay != undefined ) then
					for o in objs do lay.addNode o
			)
		)
	)

	format "SCX Map Import: % object(s) in %s\n" created.count (((timeStamp()-t0)/1000.0) as string)
	created -- return
)

-- ------------------------------------------------------------------- UI
if( SLR_Map_Import != undefined ) then -- recreate if window already exists
	DestroyDialog SLR_Map_Import

rollout SLR_Map_Import "SCX Map Import v1.1"
(
	local rows = #() -- every mesh found
	local view = #() -- rows currently listed (filter)
	local mapName = ""
	local multi = false -- more than one rpk was read

	button rpk_btn "Open RPK..." pos:[8,8] width:92 height:22
	button dir_btn "Open map folder..." pos:[104,8] width:120 height:22
	label rpk_lbl "nothing loaded" pos:[232,14]
	label flt_lbl "filter:" pos:[352,14]
	editText filter_edt "" pos:[386,9] width:126 height:20

	multiListBox mesh_lst "" pos:[8,36] width:504 height:18

	button all_btn "All" pos:[8,308] width:56 height:20
	button none_btn "None" pos:[68,308] width:56 height:20
	button found_btn "Only found" pos:[128,308] width:76 height:20
	checkbox phys_chck "List collision meshes" pos:[214,311]

	checkbox layer_chck "Put the objects in a layer named after the map" pos:[8,336]
	label info_lbl "" pos:[8,358]

	button imp_btn "IMPORT SELECTED" pos:[8,378] width:504 height:24

	function fill =
	(
		local items = #()
		local flt = toLower (trimLeft (trimRight filter_edt.text))

		view = #()

		for i=1 to rows.count do
		(
			if( (flt == "") or ((findString (toLower (rows[i].alias + " " + (filenameFromPath rows[i].file))) flt) != undefined) ) then
			(
				append view i
				append items (SLR_MAP_label rows[i] multi)
			)
		)

		mesh_lst.items = items
		mesh_lst.selection = #{}

		local found = 0, bytes = 0

		for r in rows where r.exists do ( found += 1; bytes += r.size )

		info_lbl.text = (view.count as string) + " listed of " + (rows.count as string) + " mesh(es), " + (found as string) + " on disk, " + (SLR_MAP_sizeStr bytes)
		imp_btn.enabled = (view.count > 0)
	)

	function loadRPK file_name =
	(
		mapName = getFilenameFile file_name
		multi = false
		rpk_lbl.text = (filenameFromPath file_name)
		info_lbl.text = "reading..."

		windows.processPostedMessages()

		rows = SLR_MAP_scan file_name SLR_MAP_IMPORT_PHYS
		SLR_MAP_IMPORT_LAST = file_name

		fill()
	)

	function loadFolder dir =
	(
		local clean = trimRight dir "\\/"

		mapName = filenameFromPath clean
		multi = true
		rpk_lbl.text = (mapName + "  (all rpk)")
		info_lbl.text = "reading the rpk files..."

		windows.processPostedMessages()

		rows = SLR_MAP_scanFolder clean SLR_MAP_IMPORT_PHYS
		SLR_MAP_IMPORT_LAST_DIR = clean + "\\"

		fill()
	)

	on rpk_btn pressed do
	(
		local file_name = getOpenFileName caption:"Open a SLRR resource file" \
			types:"SLRR resource (*.rpk)|*.rpk|All files (*.*)|*.*" \
			filename:SLR_MAP_IMPORT_LAST

		if( file_name != undefined ) then loadRPK file_name
	)

	on dir_btn pressed do
	(
		local dir = getSavePath caption:"Pick the folder of the map (every rpk inside it is read)" \
			initialDir:SLR_MAP_IMPORT_LAST_DIR

		if( dir != undefined ) then loadFolder dir
	)

	on all_btn pressed do if( view.count > 0 ) then mesh_lst.selection = #{1..view.count}
	on none_btn pressed do mesh_lst.selection = #{}

	on found_btn pressed do
	(
		local sel = #{}

		for i=1 to view.count where rows[view[i]].exists do sel[i] = true

		mesh_lst.selection = sel
	)

	on phys_chck changed arg do
	(
		SLR_MAP_IMPORT_PHYS = arg

		if( multi ) then
		(
			if( SLR_MAP_IMPORT_LAST_DIR != "" ) then loadFolder SLR_MAP_IMPORT_LAST_DIR
		)
		else
			if( SLR_MAP_IMPORT_LAST != "" ) then loadRPK SLR_MAP_IMPORT_LAST
	)

	on layer_chck changed arg do SLR_MAP_IMPORT_LAYER = arg
	on filter_edt changed arg do fill()

	on imp_btn pressed do
	(
		local sel = mesh_lst.selection as array

		if( sel.count == 0 ) then
			messageBox "Select the meshes to import first.\n\nUse \"Only found\" to take everything that exists on disk." title:"SCX Map Import" beep:false
		else
		(
			local real = for i in sel collect view[i] -- list index to row index
			local bytes = 0, missing = 0

			for i in real do
			(
				bytes += rows[i].size
				if( not rows[i].exists ) then missing += 1
			)

			local go = true

			if( missing == real.count ) then
			(
				messageBox ("None of the " + (missing as string) + " selected meshes exists on disk.\n\nThis map only has the rpk descriptions here,\nthe .scx files were not installed.") title:"SCX Map Import" beep:true
				go = false
			)
			else
			if( bytes > 50000000 ) then
				go = queryBox ("The selected meshes have " + (SLR_MAP_sizeStr bytes) + " of geometry.\nImporting can take several minutes. Continue?") title:"SCX Map Import"

			if( go ) then
			(
				local layerName = if( layer_chck.checked ) then mapName else undefined

				info_lbl.text = "importing..."
				windows.processPostedMessages()

				local t0 = timeStamp()
				local created = SLR_MAP_importRows rows real layerName

				info_lbl.text = (created.count as string) + " object(s) imported in " + (((timeStamp()-t0)/1000.0) as string) + "s"

				if( created.count > 0 ) then select created

				redrawViews()
			)
		)
	)

	on SLR_Map_Import open do
	(
		phys_chck.checked = SLR_MAP_IMPORT_PHYS
		layer_chck.checked = SLR_MAP_IMPORT_LAYER
		imp_btn.enabled = false
	)
)

CreateDialog SLR_Map_Import width:520 height:412
