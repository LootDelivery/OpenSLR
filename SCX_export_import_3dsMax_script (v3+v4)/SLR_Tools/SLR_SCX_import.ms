-- ==========================================================================
--  SCX Importer for Street Legal Racing: Redline (Invictus "INVO" v3 and v4)
--  Reads the mesh, both diffuse layers, all UV channels, vertex colors and
--  the material properties back into 3ds Max.
-- ==========================================================================

if( SLR_SCX_IMPORT_WELD == undefined ) then -- keep chosen options between runs
(
	global SLR_SCX_IMPORT_WELD = true
	global SLR_SCX_IMPORT_WELD_TRESH = 0.001
	global SLR_SCX_IMPORT_SMOOTH = true
	global SLR_SCX_IMPORT_SMOOTH_ANGLE = 45.0
	global SLR_SCX_IMPORT_SEPARATE = false
	global SLR_SCX_IMPORT_SLRRMAT = true
	global SLR_SCX_IMPORT_RPK = true
	global SLR_SCX_IMPORT_SCALE = 1.0
	global SLR_SCX_IMPORT_TEXDIR = ""
	global SLR_SCX_IMPORT_LAST_DIR = ""
)

global SLR_SCX_IMPORT_VERSION = 6 -- other tools reload an older copy of this file
global SLR_SCX_TEX_KNOWN = false -- the texture list of the mesh being imported is known (rpk or .tex)

struct SLR_SCX_MapDef -- one texture slot of a material
(
	index = -1, -- texture index (position in the .tex file / RenderRef list)
	channel = 1, -- max map channel (1-3), 0 = environment mapping
	tileU = 1.0, tileV = 1.0, offU = 0.0, offV = 0.0,
	amount = 1.0
)

struct SLR_SCX_Mesh -- one mesh of the file (material + geometry)
(
	name = "",
	flags = 0,
	alpha = false, -- material is transparent
	diffuse, specular, illum,
	opacity = 1.0, gloss = 0.0, shading = 1.0,
	diff1, diff2, bump, illumMap, refl, -- SLR_SCX_MapDef
	blend1 = false, -- v3 flag 0x100: the diffuse color shows through the alpha of layer 1
	verts, normals, colors, uv1, uv2, uv3, faces
)

-- --------------------------------------------------------------- reading
function SLR_SCX_readString f size = -- fixed size zero padded string
(
	local endPos = (ftell f) + size
	local str = "", c, done = false

	for i=1 to size do
	(
		c = ReadByte f #unsigned

		if( c == 0 ) then done = true
		if( not done ) then str += (bit.intAsChar c)
	)

	fseek f endPos #seek_set
	str -- return
)

function SLR_SCX_newMesh = -- mesh with its own (not shared) arrays
(
	local m = SLR_SCX_Mesh()

	m.diffuse = (color 128 128 128)
	m.specular = (color 0 0 0)
	m.illum = (color 0 0 0)
	m.verts = #(); m.normals = #(); m.faces = #()

	m -- return
)

function SLR_SCX_readMaterialV4 f blockEnd m = -- material entries of a v4 block
(
	local flags = ReadLong f #unsigned
	local alpha = ReadLong f #unsigned -- 0 = opaque, float 2.0 = transparent
	local count = ReadLong f #unsigned

	m.flags = flags
	m.alpha = (alpha != 0)

	local slot, dType, r, g, b, a, val, map

	for i=1 to count while ((ftell f) + 4 <= blockEnd) do
	(
		slot = ReadShort f #unsigned
		dType = (ReadShort f #unsigned) / 256 -- type id is stored byte swapped

		case dType of
		(
			0: -- BGRA color
			(
				b = ReadByte f #unsigned; g = ReadByte f #unsigned
				r = ReadByte f #unsigned; a = ReadByte f #unsigned

				case slot of
				(
					0: ( m.diffuse = (color r g b); m.opacity = a/255.0 )
					1: m.specular = (color r g b)
					2: m.illum = (color r g b)
				)
			)
			1: -- float amount
			(
				val = ReadFloat f

				case slot of
				(
					0: m.gloss = val -- glossiness
					1: if( m.refl != undefined ) then m.refl.amount = val -- reflection amount
					2: if( m.bump != undefined ) then m.bump.amount = val -- bump amount
				)
			)
			5: ReadLong f #signed -- integer value, not used
			6: -- map definition
			(
				map = SLR_SCX_MapDef()
				map.index = ReadLong f #signed
				map.channel = (ReadLong f #signed) + 1 -- game counts UV channels from 0
				ReadLong f #signed -- tilling flags
				map.tileU = ReadFloat f
				map.tileV = ReadFloat f
				map.offU = ReadFloat f
				map.offV = -(ReadFloat f) -- V axis is flipped

				if( map.channel < 1 ) then map.channel = 0 -- environment mapping

				case slot of
				(
					0: m.diff1 = map
					1: m.diff2 = map
					2: m.bump = map
					3: m.refl = map
					4: m.illumMap = map
				)
			)
			8: m.name = SLR_SCX_readString f 32 -- material name
			default: -- unknown entry, no way to know its size
			(
				format "SCX: unknown material entry type % (slot %)\n" dType slot
				fseek f blockEnd #seek_set
			)
		)
	)

	fseek f blockEnd #seek_set
)

function SLR_SCX_readVertices f count vType stride m = -- v4 vertex block
(
	local hasPos = (bit.and vType 0x00001) != 0
	local w1 = (bit.and vType 0x00004) != 0
	local w2 = (bit.and vType 0x00008) != 0
	local w3 = (bit.and vType 0x00010) != 0
	local hasBone = (bit.and vType 0x00020) != 0
	local hasNormal = (bit.and vType 0x00040) != 0
	local hasIllum = (bit.and vType 0x00080) != 0
	local hasColor = (bit.and vType 0x00100) != 0
	local hasUV1 = (bit.and vType 0x00200) != 0
	local hasUV2 = (bit.and vType 0x00400) != 0
	local hasUV3 = (bit.and vType 0x00800) != 0
	local hasBumpN = (bit.and vType 0x40000) != 0

	local known = 0
	if hasPos then known += 12
	if w1 then known += 4
	if w2 then known += 8
	if w3 then known += 12
	if hasBone then known += 4
	if hasNormal then known += 12
	if hasIllum then known += 4
	if hasColor then known += 4
	if hasUV1 then known += 8
	if hasUV2 then known += 8
	if hasUV3 then known += 8
	if hasBumpN then known += 12

	local skip = stride - known -- unknown fields at the end of every vertex

	m.verts = #(); m.verts[count] = [0,0,0]
	if hasNormal then ( m.normals = #(); m.normals[count] = [0,0,0] )
	if hasColor then ( m.colors = #(); m.colors[count] = [1,1,1] )
	if hasUV1 then ( m.uv1 = #(); m.uv1[count] = [0,0,0] )
	if hasUV2 then ( m.uv2 = #(); m.uv2[count] = [0,0,0] )
	if hasUV3 then ( m.uv3 = #(); m.uv3[count] = [0,0,0] )

	local x, y, z, r, g, b

	for i=1 to count do
	(
		if hasPos then
		(
			x = ReadFloat f; y = ReadFloat f; z = ReadFloat f
			m.verts[i] = [x, -z, y] -- game axes: X width, Y up, Z lenght
		)

		if w1 then ReadFloat f
		if w2 then ( ReadFloat f; ReadFloat f )
		if w3 then ( ReadFloat f; ReadFloat f; ReadFloat f )
		if hasBone then ( ReadLong f #unsigned )

		if hasNormal then
		(
			x = ReadFloat f; y = ReadFloat f; z = ReadFloat f
			m.normals[i] = [x, -z, y]
		)

		if hasIllum then ReadLong f #unsigned -- vertex illumination, not imported

		if hasColor then
		(
			b = ReadByte f #unsigned; g = ReadByte f #unsigned
			r = ReadByte f #unsigned; ReadByte f #unsigned
			m.colors[i] = [r/255.0, g/255.0, b/255.0]
		)

		if hasUV1 then ( x = ReadFloat f; y = ReadFloat f; m.uv1[i] = [x, -y, 0] )
		if hasUV2 then ( x = ReadFloat f; y = ReadFloat f; m.uv2[i] = [x, -y, 0] )
		if hasUV3 then ( x = ReadFloat f; y = ReadFloat f; m.uv3[i] = [x, -y, 0] )

		if hasBumpN then ( ReadFloat f; ReadFloat f; ReadFloat f )

		if( skip > 0 ) then fseek f skip #seek_cur
	)
)

function SLR_SCX_readV4 f fileSize = -- returns array of SLR_SCX_Mesh
(
	local count = ReadLong f #unsigned
	local entryType = #(), entryOffset = #()

	for i=1 to count do
	(
		entryType[i] = ReadLong f #unsigned
		entryOffset[i] = ReadLong f #unsigned
	)

	local meshes = #()
	local m, bType, bSize, blockEnd, nv, vType, stride, ns, a, b, c

	for i=1 to count do
	(
		if( (entryOffset[i] < 12) or ((entryOffset[i]+8) > fileSize) ) then
		(
			format "SCX: block % has an invalid offset (%)\n" i entryOffset[i]
			exit
		)

		fseek f entryOffset[i] #seek_set
		bType = ReadLong f #unsigned
		bSize = ReadLong f #unsigned
		blockEnd = entryOffset[i] + bSize

		case bType of
		(
			0: -- material, starts a new mesh
			(
				m = SLR_SCX_newMesh()
				append meshes m
				SLR_SCX_readMaterialV4 f blockEnd m
			)
			1: () -- hard surface definition, nothing to read
			2: () -- bone list, skinning is not imported
			3: ()
			4: -- vertices
			(
				if( m == undefined ) then ( m = SLR_SCX_newMesh(); append meshes m ) -- mesh without material

				(
					nv = ReadLong f #unsigned
					vType = ReadLong f #unsigned

					if( nv > 0 ) then
					(
						stride = (bSize - 16) / nv
						SLR_SCX_readVertices f nv vType stride m
					)
				)
			)
			5: -- faces
			(
				if( m == undefined ) then ( m = SLR_SCX_newMesh(); append meshes m )

				(
					ns = ReadLong f #unsigned
					m.faces = #(); m.faces[ns/3] = [1,1,1]

					for j=1 to (ns/3) do
					(
						a = (ReadShort f #unsigned)+1
						b = (ReadShort f #unsigned)+1
						c = (ReadShort f #unsigned)+1
						m.faces[j] = [a,b,c]
					)
				)
			)
			default: format "SCX: unknown block type % at %\n" bType entryOffset[i]
		)
	)

	meshes -- return
)

function SLR_SCX_readV3 f fileSize = -- returns array of SLR_SCX_Mesh
(
	local meshes = #()
	local m, matStart, matSize, oneVertexSize, nv, nf, map
	local mapIdx = #(), mapMode = #(), chans = #()
	local x, y, z, r, g, b, a, specAvg = 0.0, con = true

	while( con and (((ftell f) + 140) <= fileSize) ) do
	(
		matStart = ftell f
		matSize = ReadLong f #unsigned

		if( (matSize < 68) or (matSize > 1024) or ((matStart+matSize) > fileSize) ) then
		(
			con = false -- not a material, end of file data
		)
		else
		(
			m = SLR_SCX_newMesh()

			r = ReadFloat f; g = ReadFloat f; b = ReadFloat f
			m.diffuse = (color (r*255) (g*255) (b*255))
			m.opacity = ReadFloat f

			r = ReadFloat f; g = ReadFloat f; b = ReadFloat f
			specAvg = (r + g + b)/3.0 -- v3 keeps the reflection amount in the specular color
			m.specular = (color (r*255) (g*255) (b*255))
			a = ReadFloat f -- specular level
			m.specular = m.specular * a
			m.gloss = ReadFloat f

			m.flags = ReadLong f #unsigned
			m.alpha = ((bit.and m.flags 0x00000001) != 0)
			m.blend1 = ((bit.and m.flags 0x00000100) != 0)

			for i=1 to 8 do -- map slots: diffuse 1, bump, specular, reflection, diffuse 2, unknown, light, unknown
			(
				mapIdx[i] = ReadByte f #unsigned
				mapMode[i] = ReadByte f #unsigned
			)

			oneVertexSize = ReadLong f #unsigned

			for i=1 to 3 do ( ReadByte f #unsigned; ReadByte f #unsigned ) -- unknown maps

			for i=1 to 5 do chans[i] = ReadByte f #unsigned -- diffuse 1, unknown, diffuse 2, layer 3, bump
			for i=1 to 5 do ReadByte f #unsigned -- unknown

			ReadLong f #unsigned; ReadLong f #unsigned; ReadLong f #unsigned -- unknown

			r = ReadFloat f; g = ReadFloat f; b = ReadFloat f
			m.illum = (color (r*255) (g*255) (b*255))

			m.name = SLR_SCX_readString f 32

			fseek f (matStart + matSize) #seek_set -- skip anything left of the material

			if( mapIdx[1] != 0xFF ) then ( map = SLR_SCX_MapDef(); map.index = mapIdx[1]; map.channel = chans[1]+1; m.diff1 = map )
			if( mapIdx[5] != 0xFF ) then ( map = SLR_SCX_MapDef(); map.index = mapIdx[5]; map.channel = chans[3]+1; m.diff2 = map )
			if( mapIdx[2] != 0xFF ) then ( map = SLR_SCX_MapDef(); map.index = mapIdx[2]; map.channel = chans[5]+1; m.bump = map )
			if( mapIdx[7] != 0xFF ) then ( map = SLR_SCX_MapDef(); map.index = mapIdx[7]; map.channel = 2; m.illumMap = map )
			if( mapIdx[4] != 0xFF ) then ( map = SLR_SCX_MapDef(); map.index = mapIdx[4]; map.channel = 0; map.amount = (amin specAvg 1.0); m.refl = map )

			if( (oneVertexSize < 12) or (oneVertexSize > 256) ) then
				con = false -- broken material, stop here
			else
			(
				nv = ReadLong f #unsigned

				if( nv == 0 ) then
				(
					ReadLong f #unsigned -- faces count, always 0 too
					append meshes m -- marker material of a sector file (.scy), kept just to be counted
				)
				else
				if( (nv < 0) or (((ftell f) + nv*oneVertexSize) > fileSize) ) then
					con = false
				else
				(
				-- vertices, fields are always in the same order, oneVertexSize says how many exist
					m.verts = #(); m.verts[nv] = [0,0,0]
					if( oneVertexSize >= 24 ) then ( m.normals = #(); m.normals[nv] = [0,0,0] )
					if( oneVertexSize >= 32 ) then ( m.uv1 = #(); m.uv1[nv] = [0,0,0] )
					if( oneVertexSize >= 40 ) then ( m.uv2 = #(); m.uv2[nv] = [0,0,0] )
					if( oneVertexSize >= 44 ) then ( m.colors = #(); m.colors[nv] = [1,1,1] )
					if( oneVertexSize >= 60 ) then ( m.uv3 = #(); m.uv3[nv] = [0,0,0] )

					local vertEnd
					for i=1 to nv do
					(
						vertEnd = (ftell f) + oneVertexSize

						x = ReadFloat f; y = ReadFloat f; z = ReadFloat f
						m.verts[i] = [x, -z, y]

						if( oneVertexSize >= 24 ) then
						(
							x = ReadFloat f; y = ReadFloat f; z = ReadFloat f
							m.normals[i] = [x, -z, y]
						)

						if( oneVertexSize >= 32 ) then ( x = ReadFloat f; y = ReadFloat f; m.uv1[i] = [x, -y, 0] )
						if( oneVertexSize >= 40 ) then ( x = ReadFloat f; y = ReadFloat f; m.uv2[i] = [x, -y, 0] )

						if( oneVertexSize >= 44 ) then
						(
							b = ReadByte f #unsigned; g = ReadByte f #unsigned
							r = ReadByte f #unsigned; ReadByte f #unsigned
							m.colors[i] = [r/255.0, g/255.0, b/255.0]
						)

						if( oneVertexSize >= 60 ) then
						(
							ReadLong f #unsigned; ReadLong f #unsigned -- unknown
							x = ReadFloat f; y = ReadFloat f; m.uv3[i] = [x, -y, 0]
						)

						fseek f vertEnd #seek_set
					)

					nf = ReadLong f #unsigned

					if( (nf < 0) or (((ftell f) + nf*12) > fileSize) ) then
						con = false
					else
					(
						m.faces = #(); m.faces[nf] = [1,1,1]

						for i=1 to nf do
							m.faces[i] = [ ((ReadLong f #unsigned)+1), ((ReadLong f #unsigned)+1), ((ReadLong f #unsigned)+1) ]

						append meshes m
					)
				)
			)
		)
	)

	meshes -- return
)

function SLR_SCX_readFile file_name = -- returns #( version, #(meshes) )
(
	local f = fopen file_name "rb"

	if( f == undefined ) then
	(
		messageBox ("Could not open " + (filenameFromPath file_name)) title:"Error" beep:true
		return #(0, #())
	)

	fseek f 0 #seek_end
	local fileSize = ftell f
	fseek f 0 #seek_set

	local sign = SLR_SCX_readString f 4
	local version = 0
	local meshes = #()

	if( sign != "INVO" ) then
		messageBox ((filenameFromPath file_name) + " is not an Invictus SCX mesh") title:"Error" beep:true
	else
	(
		version = ReadLong f #unsigned

		case version of
		(
			3: meshes = SLR_SCX_readV3 f fileSize
			4: meshes = SLR_SCX_readV4 f fileSize
			default: messageBox ("SCX version " + (version as string) + " is not supported") title:"Error" beep:true
		)
	)

	fclose f

	#(version, meshes) -- return
)

-- ==========================================================================
--  RPK reader - only what is needed to get the texture list of a mesh
--  chain: mesh (typeOfEntry 5) -> RenderRef (14) "texture 0x..." -> texture (7)
-- ==========================================================================
struct SLR_RPK_Entry ( typeID, toe, alias, lines, rsdOff, rsdLen )

function SLR_RPK_hexToInt str = -- "0x0000AA01" or "12" to integer
(
	local s = toLower (trimLeft (trimRight str))
	local v = 0, c, con = true

	if( (substring s 1 2) == "0x" ) then s = substring s 3 -1

	for i=1 to s.count while con do
	(
		c = bit.charAsInt s[i]

		if( (c >= 48) and (c <= 57) ) then v = v*16 + (c-48)
		else if( (c >= 97) and (c <= 102) ) then v = v*16 + (c-87)
		else con = false
	)

	v -- return
)

function SLR_RPK_firstToken str = -- text up to the first space or tab
(
	local i = 1

	while( (i <= str.count) and (str[i] != " ") and (str[i] != "\t") ) do i += 1

	substring str 1 (i-1) -- return
)

function SLR_RPK_readLines f count = -- reads count bytes of the RSD block as text lines
(
	local lines = #(), cur = "", c

	for i=1 to count do
	(
		c = ReadByte f #unsigned

		if( c == undefined ) then exit

		if( (c == 13) or (c == 10) or (c == 0) ) then
		(
			if( cur != "" ) then append lines cur
			cur = ""
		)
		else
			cur += (bit.intAsChar c)
	)

	if( cur != "" ) then append lines cur

	lines -- return
)

function SLR_RPK_values entry key = -- values of every "<key> <value>" line, in file order
(
	local out = #(), l, c

	for i=1 to entry.lines.count do
	(
		l = entry.lines[i]

		if( ((substring l 1 key.count) == key) and (l.count > key.count) ) then
		(
			c = l[key.count+1]

			if( (c == " ") or (c == "\t") ) then
				append out (trimLeft (trimRight (substring l (key.count+1) -1)))
		)
	)

	out -- return
)

function SLR_RPK_read file_name = -- returns #( external rpk names, entries ) or undefined
(
	local f = fopen file_name "rb"

	if( f == undefined ) then return undefined

	local ok = true
	local ext = #(), entries = #()

	try
	(
		if( (SLR_SCX_readString f 4) != "RPAK" ) then
			ok = false
		else
		(
			ReadLong f #unsigned -- version
			local extCount = ReadLong f #unsigned
			ReadLong f #unsigned -- zero

			local idx
			for i=1 to extCount do
			(
				ReadShort f #unsigned -- unknown
				idx = ReadShort f #unsigned
				local nm = SLR_SCX_readString f 60

				if( idx > 0 ) then ext[idx] = nm
			)

			ReadLong f #unsigned -- entries block size
			local entriesCount = ReadLong f #unsigned
			ReadLong f #unsigned; ReadLong f #unsigned

			local toe, addType, rsdOff, rsdLen, aliasLen, alias, typeID, pos, e

			for i=1 to entriesCount do
			(
				ReadLong f #unsigned -- superID
				typeID = ReadLong f #unsigned
				toe = ReadByte f #unsigned
				addType = ReadByte f #unsigned
				ReadFloat f -- isParentCompatible
				rsdOff = ReadLong f #unsigned
				rsdLen = ReadLong f #unsigned
				aliasLen = ReadByte f #unsigned
				alias = SLR_SCX_readString f aliasLen

				if( (bit.and addType 1) != 0 ) then fseek f 48 #seek_cur -- bounding box

				if( (toe == 1) or (toe == 5) or (toe == 7) or (toe == 8) or (toe == 9) or (toe == 14) ) then -- spatial root, mesh, texture, gametype, collision, renderref
				(
					e = SLR_RPK_Entry typeID:typeID toe:toe alias:alias lines:#() rsdOff:rsdOff rsdLen:rsdLen

					if( rsdLen > 0 ) then
					(
						pos = ftell f
						fseek f rsdOff #seek_set
						e.lines = SLR_RPK_readLines f rsdLen
						fseek f pos #seek_set
					)

					append entries e
				)
			)
		)
	)
	catch
	(
		ok = false
	)

	fclose f

	if( ok ) then #(ext, entries) else undefined -- return
)

function SLR_RPK_readCached file_name cache = -- cache = #( #(names), #(rpks) )
(
	local key = toLower file_name
	local idx = findItem cache[1] key

	if( idx != 0 ) then return cache[2][idx]

	local rpk = SLR_RPK_read file_name

	append cache[1] key
	append cache[2] (if rpk == undefined then 0 else rpk) -- 0 marks a rpk that failed to load

	if( rpk == undefined ) then undefined else rpk -- return
)

function SLR_RPK_findEntry entries toe typeID =
(
	for e in entries where ((e.toe == toe) and (e.typeID == typeID)) do
		return e

	undefined -- return
)

function SLR_pathTail p count = -- last components of a path, lower case
(
	local parts = filterString (toLower p) "\\/"
	local out = ""
	local first = parts.count - count + 1

	if( first < 1 ) then first = 1

	for i=first to parts.count do
	(
		if( out != "" ) then out += "\\"
		out += parts[i]
	)

	out -- return
)

function SLR_gameRoot dir = -- folder of the game (the one holding sl.rpk / cars.rpk)
(
	local d = dir, up

	while( (d != undefined) and (d != "") ) do
	(
		if( (doesFileExist (d + "sl.rpk")) or (doesFileExist (d + "cars.rpk")) or (doesFileExist (d + "system.rpk")) ) then
			return d

		up = getFilenamePath (trimRight d "\\/")

		if( (up == d) or (up == "") ) then exit

		d = up
	)

	undefined -- return
)

function SLR_RPK_refSource ext entries ref toe root cache = -- "sourcefile" of a referenced entry
(
	if( ref == 0 ) then return undefined -- slot replaced by the game (car paint)

	local extIdx = bit.shift ref -16
	local localID = bit.and ref 0xFFFF
	local target = entries

	if( extIdx != 0 ) then -- the entry lives in another rpk
	(
		if( (extIdx > ext.count) or (ext[extIdx] == undefined) ) then return undefined

		local sub = SLR_RPK_readCached (root + ext[extIdx]) cache

		if( sub == undefined ) then return undefined

		target = sub[2]
	)

	local e = SLR_RPK_findEntry target toe localID

	if( e == undefined ) then return undefined

	local src = SLR_RPK_values e "sourcefile"

	if( src.count == 0 ) then undefined else src[1] -- return (path relative to the game folder)
)

function SLR_RPK_resolveTexture ext entries ref root cache = -- texture file of a "texture 0x..." reference
(
	local src = SLR_RPK_refSource ext entries ref 7 root cache

	if( src == undefined ) then return undefined

	local full = root + src

	if( doesFileExist full ) then return full

	local base = (getFilenamePath full) + (getFilenameFile full) -- same name, other image type (.dds turned .png)

	for x in #(".dds", ".png", ".tga", ".bmp", ".jpg", ".jpeg") where (doesFileExist (base + x)) do
		return (base + x)

	filenameFromPath src -- return: the file of the game is missing, keep its name
)

function SLR_RPK_renderTextures ext entries entry root cache = -- ordered texture list of a render definition
(
	-- every position is kept, the materials of the mesh count on it:
	--   path or name   texture of the rpk (only the name when the file is missing)
	--   "?name"        null or broken reference whose comment is just a label: used if such a file exists
	--   ""             null reference without a comment: a slot the game fills (reflection of the car)
	local refs = SLR_RPK_values entry "texture"
	local names = #(), file, token, rest, found, b

	for i=1 to refs.count do
	(
		token = SLR_RPK_firstToken refs[i]
		file = SLR_RPK_resolveTexture ext entries (SLR_RPK_hexToInt token) root cache

		if( file == undefined ) then
		(
			rest = substring refs[i] (token.count+1) -1 -- the name written as comment
			rest = trimLeft (trimRight rest " \t\"'") " \t\"'"
			file = ""

			if( rest != "" ) then
			(
				file = "?" + rest

				if( (getFilenameType rest) == "" ) then -- a bare name: "general" is general_env_map.dds of the game
				(
					found = getFiles (root + "cars\\textures\\" + rest + "*.*")

					for f in found while ((file[1] == "?")) do
					(
						b = toLower (getFilenameFile f)
						if( (b == (toLower rest)) or ((substring b 1 (rest.count+1)) == ((toLower rest) + "_")) ) then file = f
					)
				)
			)
		)

		append names file
	)

	names -- return
)

function SLR_RPK_textures rpk_name scx_name root cache = -- ordered texture list of a mesh, or undefined
(
	local rpk = SLR_RPK_readCached rpk_name cache

	if( rpk == undefined ) then return undefined

	local ext = rpk[1], entries = rpk[2]
	local tail = SLR_pathTail scx_name 3
	local meshID, src

	for e in entries where (e.toe == 5) do -- the mesh entry of this scx file
	(
		src = SLR_RPK_values e "sourcefile"

		if( (src.count > 0) and ((SLR_pathTail src[1] 3) == tail) ) then
		(
			meshID = e.typeID
			exit
		)
	)

	if( meshID == undefined ) then return undefined

	local refs

	for e in entries where (e.toe == 14) do -- render definition that uses this mesh
	(
		refs = SLR_RPK_values e "mesh"

		if( (refs.count > 0) and ((SLR_RPK_hexToInt (SLR_RPK_firstToken refs[1])) == meshID) ) then
			return (SLR_RPK_renderTextures ext entries e root cache)
	)

	undefined -- return
)

function SLR_SCX_texFromRPK scx_name = -- texture names of a mesh, taken from the rpk that uses it
(
	local dir = getFilenamePath scx_name
	local root = SLR_gameRoot dir

	if( root == undefined ) then
	(
		format "SCX Import: game folder not found (no sl.rpk/cars.rpk above the mesh)\n"
		return #()
	)

	local parent = getFilenamePath (trimRight dir "\\/") -- ..\Taxi_data\
	local grand = getFilenamePath (trimRight parent "\\/") -- ..\traffic\
	local great = getFilenamePath (trimRight grand "\\/")
	local name = filenameFromPath (trimRight parent "\\/") -- Taxi_data
	local base = name

	if( (name.count > 5) and ((toLower (substring name (name.count-4) 5)) == "_data") ) then
		base = substring name 1 (name.count-5) -- Taxi_data -> Taxi

	local list = #()
	append list (grand + base + ".rpk") -- the usual name
	append list (grand + name + ".rpk")
	if( grand != "" ) then join list (getFiles (grand + "*.rpk"))
	if( great != "" ) then join list (getFiles (great + "*.rpk"))

	local cache = #( #(), #() )
	local tried = #(), key, names

	for i=1 to list.count while (tried.count < 80) do
	(
		key = toLower list[i]

		if( ((findItem tried key) == 0) and (doesFileExist list[i]) ) then
		(
			append tried key
			names = SLR_RPK_textures list[i] scx_name root cache

			if( names != undefined ) then
			(
				SLR_SCX_TEX_KNOWN = true -- even an empty list is the real one
				format "SCX Import: % texture(s) found in %\n" names.count (filenameFromPath list[i])
				return names
			)
		)
	)

	format "SCX Import: no render definition found for % in the rpk files around it\n" (filenameFromPath scx_name)
	#() -- return
)

-- ==========================================================================
--  Geometry baked inside a rpk (the "POLY" blocks of the volume hierarchy)
--  Maps like the city keep their meshes here instead of loose .scx files:
--  ResEntry(typeOfEntry 1) -> NamedDataArray -> ... -> POLY -> meshes
--  Every mesh has a material index into the material list of the map .scy
-- ==========================================================================
function SLR_RPK_walkRegion f regionPos size polys rsds = -- collects the POLY (and RSD) blocks of a region
(
	local endPos = regionPos + size
	local p = regionPos
	local sig, blk, nMesh, nv, nt, q, count, nameLen, subOff, subSize, guard = 0

	while( (p < endPos) and (guard < 200000) ) do
	(
		guard += 1
		fseek f p #seek_set
		sig = SLR_SCX_readString f 4

		if( sig == "POLY" ) then
		(
			blk = (ReadLong f #signed) + 8
			ReadLong f #signed
			nMesh = ReadLong f #signed

			q = p + 16
			nv = 0; nt = 0

			for i=1 to nMesh do
			(
				fseek f (q+4) #seek_set
				nv += (ReadLong f #signed)
				nt += (ReadLong f #signed)
				fseek f q #seek_set
				ReadLong f #signed -- material index
				local mv = ReadLong f #signed
				local mt = ReadLong f #signed
				q += 12 + mv*56 + mt*12
			)

			append polys #(p, nMesh, nv, nt)
			p += blk
		)
		else
		if( sig == "RSD" ) then -- text block: the native objects live in these
		(
			blk = (ReadLong f #signed) + 8
			append rsds #(p, blk)
			p += blk
		)
		else
		if( (sig == "PHYS") or (sig == "EXTP") ) then
		(
			blk = (ReadLong f #signed) + 8
			p += blk
		)
		else
		if( sig == "" ) then -- NamedDataArray: signature is four zero bytes
		(
			count = ReadLong f #signed

			if( (count < 0) or (count > 200000) ) then
				p = endPos -- not an array, stop here
			else
			(
				q = p + 8

				for i=1 to count do
				(
					fseek f (q+14) #seek_set
					subOff = ReadLong f #signed
					subSize = ReadLong f #signed
					nameLen = ReadByte f #unsigned

					q += 23 + nameLen + 48 -- record + 12 floats of bounding box

					if( (subSize > 0) and (subOff > 0) ) then
						SLR_RPK_walkRegion f subOff subSize polys rsds
				)

				p = q
			)
		)
		else
			p = endPos -- unknown block, stop
	)
)

function SLR_RPK_polyBlocks file_name = -- every POLY block baked in a rpk
(
	local rpk = SLR_RPK_read file_name

	if( rpk == undefined ) then return #()

	local f = fopen file_name "rb"

	if( f == undefined ) then return #()

	local polys = #()

	for e in rpk[2] where ((e.toe == 1) and (e.rsdLen > 0)) do
		SLR_RPK_walkRegion f e.rsdOff e.rsdLen polys #()

	fclose f
	polys -- return
)

-- -------------------------------------------------------------- textures
function SLR_SCX_readTexNames file_name = -- names stored in the .tex file written by the exporter
(
	local names = #()
	local tex_name = (getFilenamePath file_name) + (getFilenameFile file_name) + ".tex"

	if( doesFileExist tex_name ) then
	(
		local f = openFile tex_name
		local line

		while( (f != undefined) and (not (eof f)) ) do
		(
			line = readLine f
			if( line != "" ) then append names line
		)

		if( f != undefined ) then close f
	)

	names -- return
)

function SLR_SCX_texFile map texNames dirs = -- texture file of a map (path when found on disk), undefined for an empty slot
(
	if( (map == undefined) or (map.index < 0) ) then return undefined

	local name, label = false

	if( (map.index+1) <= texNames.count ) then
		name = texNames[map.index+1] -- real name from the .tex file or from the rpk
	else
	if( SLR_SCX_TEX_KNOWN ) then
		return undefined -- the texture list is known and this index is not in it: nothing there in game either
	else
		name = ("texture_" + (map.index as string) + ".dds") -- unknown list: placeholder keeps the texture order

	if( name == "" ) then return undefined -- slot the game fills itself

	if( name[1] == "?" ) then -- only a label in the rpk
	(
		name = substring name 2 -1
		label = true
	)

	if( (getFilenameType name) == "" ) then name += ".dds" -- max needs a file type

	if( doesFileExist name ) then return name -- full path (resolved through the rpk)

	local short = filenameFromPath name
	local base = getFilenameFile short

	for d in dirs where (d != "") do
	(
		if( doesFileExist (d + short) ) then return (d + short)

		for x in #(".dds", ".png", ".tga", ".bmp", ".jpg", ".jpeg") where (doesFileExist (d + base + x)) do
			return (d + base + x)
	)

	if( label ) then undefined else short -- return: a label without file is an empty slot, a real name is kept
)

function SLR_SCX_envMap dirs = -- environment map the game puts in the empty reflection slots
(
	local root

	for d in dirs where ((d != "") and (root == undefined)) do root = SLR_gameRoot d

	if( root == undefined ) then return undefined

	local f = root + "cars\\textures\\general_env_map.dds"

	if( doesFileExist f ) then f else undefined -- return
)

function SLR_SCX_makeBitmap file channel =
(
	local bmp = bitmaptexture fileName:file monoOutput:1 alphasource:2
	bmp.coords.mapChannel = (if channel < 1 then 1 else channel)
	bmp
)

function SLR_SCX_makeMaterial m texNames dirs useSLRR =
(
	local mat
	local diff1_file = SLR_SCX_texFile m.diff1 texNames dirs
	local diff2_file = SLR_SCX_texFile m.diff2 texNames dirs
	local bump_file = SLR_SCX_texFile m.bump texNames dirs
	local illum_file = SLR_SCX_texFile m.illumMap texNames dirs
	local refl_file = SLR_SCX_texFile m.refl texNames dirs

	if( (refl_file == undefined) and (m.refl != undefined) and (m.refl.index >= 0) ) then
		refl_file = SLR_SCX_envMap dirs -- empty reflection slot: the game puts its environment map there

	local specLevel = (((m.specular.r + m.specular.g + m.specular.b)/3.0)/255.0)*100.0
	local specColor = m.specular
	if( specLevel > 0.01 ) then specColor = (m.specular * (100.0/specLevel))
	local specMax = amax m.specular.r m.specular.g m.specular.b

	if( useSLRR and (SLRR_Material != undefined) ) then
	(
		mat = SLRR_Material()
		mat.name = m.name

		mat.diff_col = m.diffuse
		-- the SLRR material keeps the specular level inside the color, but the plugin gives up
		-- (level 0) when a channel goes over 255: give it the color at level 100 and set the level
		if( specMax > 255.0 ) then
		(
			mat.spec_col = m.specular * (255.0/specMax)
			mat.delegate.specularLevel = (specMax/255.0)*100.0
		)
		else
			mat.spec_col = m.specular
		mat.gloss = (m.gloss/54.0)*100.0
		mat.opac = m.opacity*100.0
		mat.illum_col = m.illum
		mat.shad = m.shading

		-- the bitmaps have to be set before the P_ parameters below: the handlers
		-- of the plugin read them (and fail on undefined)
		if( diff1_file != undefined ) then
			mat.delegate.diffuseMap.map1.map2 = SLR_SCX_makeBitmap diff1_file m.diff1.channel

		if( diff2_file != undefined ) then
		(
			mat.delegate.diffuseMap.map2 = SLR_SCX_makeBitmap diff2_file m.diff2.channel

			-- the second layer blends by its own alpha, without the mask it does not show at all
			mat.delegate.diffuseMap.mask = copy mat.delegate.diffuseMap.map2
			mat.delegate.diffuseMap.mask.alphasource = 0
		)

		if( m.blend1 and (diff1_file != undefined) ) then
			mat.P_diff_mix = 2 -- the diffuse color (car paint) shows through the alpha of layer 1

		if( illum_file != undefined ) then
		(
			mat.delegate.selfIllumMap.map1 = SLR_SCX_makeBitmap illum_file m.illumMap.channel
			mat.delegate.selfIllumMap.map2 = copy mat.delegate.diffuseMap -- light map times the diffuse, as the plugin does
			mat.delegate.selfIllumMapEnable = true
		)

		if( refl_file != undefined ) then
		(
			mat.delegate.reflectionMap.map1 = SLR_SCX_makeBitmap refl_file 1
			mat.delegate.reflectionMap.map1.coords.mappingType = 1 -- environment
			mat.delegate.reflectionMap.map1.coords.mapping = 1 -- cylindrical
		)

		if( m.refl != undefined ) then mat.P_reflect_am = m.refl.amount*100.0

		if( m.diff1 != undefined ) then mat.P_diff_ch = m.diff1.channel
		if( m.diff2 != undefined ) then mat.P_diff2_ch = m.diff2.channel
		if( m.illumMap != undefined ) then mat.P_illum_ch = m.illumMap.channel

		if( m.alpha and (m.opacity > 0.99) ) then -- transparent by the alpha of a texture, not by material opacity
		(
			if( diff1_file != undefined ) then
				mat.P_diff_opac = true
			else
			(
				-- the plugin copies that alpha from the first diffuse layer and fails without one:
				-- keep the flag for the exporter (handler skipped) and show the alpha of the second layer
				mat.l_sysCommand = true
				mat.P_diff_opac = true

				if( isKindOf mat.delegate.diffuseMap.map2 bitmaptexture ) then
				(
					mat.delegate.opacityMap = copy mat.delegate.diffuseMap.map2
					mat.delegate.opacityMap.alphasource = 0
				)
			)
		)
	)
	else
	(
		mat = Standard name:m.name

		mat.diffuse = m.diffuse
		mat.specular = specColor
		mat.specularLevel = specLevel
		mat.glossiness = (m.gloss/54.0)*100.0
		mat.opacity = m.opacity*100.0

		if( (m.illum.r + m.illum.g + m.illum.b) > 0 ) then
		(
			mat.useSelfIllumColor = true
			mat.selfIllumColor = m.illum
		)

		if( diff1_file != undefined ) then mat.diffuseMap = SLR_SCX_makeBitmap diff1_file m.diff1.channel
		if( illum_file != undefined ) then mat.selfIllumMap = SLR_SCX_makeBitmap illum_file m.illumMap.channel

		if( bump_file != undefined ) then
		(
			mat.bumpMap = SLR_SCX_makeBitmap bump_file m.bump.channel
			mat.bumpMapAmount = m.bump.amount*100.0
		)

		if( refl_file != undefined ) then
		(
			mat.reflectionMap = SLR_SCX_makeBitmap refl_file 1
			mat.reflectionMap.coords.mappingType = 1 -- environment
			mat.reflectionMapAmount = m.refl.amount*100.0
		)
	)

	if( ((classOf mat) == Standard) and (mat.diffuseMap != undefined) ) then
		showTextureMap mat mat.diffuseMap true

	mat -- return
)

-- ------------------------------------------------------------- geometry
function SLR_SCX_shadingAmount m = -- the game stores the vertex shading in the normal lenght
(
	if( (m.normals == undefined) or (m.normals.count == 0) ) then return 1.0

	local sum = 0.0
	local step = 1 + (m.normals.count/500) -- sample big meshes

	local n = 0
	for i=1 to m.normals.count by step do
	(
		sum += (length m.normals[i])
		n += 1
	)

	if( n > 0 ) then (sum/n) else 1.0
)

function SLR_SCX_setMapChannel obj channel mapVerts faces =
(
	if( (mapVerts == undefined) or (mapVerts.count == 0) ) then return false

	if( (meshop.getNumMaps obj) <= channel ) then
		meshop.setNumMaps obj (channel+1) keep:true

	meshop.setMapSupport obj channel true
	meshop.setNumMapVerts obj channel mapVerts.count
	meshop.setNumMapFaces obj channel faces.count

	for i=1 to mapVerts.count do
		meshop.setMapVert obj channel i mapVerts[i]

	for i=1 to faces.count do
		meshop.setMapFace obj channel i faces[i]

	true -- return
)

function SLR_SCX_buildObject meshes name mats scale =
(
	-- merge the given meshes into one editable mesh, one material ID peer mesh
	local allVerts = #(), allFaces = #(), allIDs = #()
	local uv1 = #(), uv2 = #(), uv3 = #(), colors = #()
	local useUV1 = false, useUV2 = false, useUV3 = false, useColor = false
	local vCount = 0, fCount = 0

	for m in meshes do
	(
		vCount += m.verts.count
		fCount += m.faces.count

		if( m.uv1 != undefined ) then useUV1 = true
		if( m.uv2 != undefined ) then useUV2 = true
		if( m.uv3 != undefined ) then useUV3 = true
		if( m.colors != undefined ) then useColor = true
	)

	allVerts[vCount] = [0,0,0]; allFaces[fCount] = [1,1,1]; allIDs[fCount] = 1
	if useUV1 then uv1[vCount] = [0,0,0]
	if useUV2 then uv2[vCount] = [0,0,0]
	if useUV3 then uv3[vCount] = [0,0,0]
	if useColor then colors[vCount] = [1,1,1]

	local vOff = 0, fOff = 0, f

	for mi=1 to meshes.count do
	(
		local m = meshes[mi]

		for i=1 to m.verts.count do
		(
			allVerts[vOff+i] = m.verts[i]*scale

			if useUV1 then uv1[vOff+i] = (if m.uv1 != undefined then m.uv1[i] else [0,0,0])
			if useUV2 then uv2[vOff+i] = (if m.uv2 != undefined then m.uv2[i] else [0,0,0])
			if useUV3 then uv3[vOff+i] = (if m.uv3 != undefined then m.uv3[i] else [0,0,0])
			if useColor then colors[vOff+i] = (if m.colors != undefined then m.colors[i] else [1,1,1])
		)

		for i=1 to m.faces.count do
		(
			f = m.faces[i]
			allFaces[fOff+i] = [f.x+vOff, f.y+vOff, f.z+vOff]
			allIDs[fOff+i] = mi
		)

		vOff += m.verts.count
		fOff += m.faces.count
	)

	local obj = mesh vertices:allVerts faces:allFaces materialIDs:allIDs name:name

	if useUV1 then SLR_SCX_setMapChannel obj 1 uv1 allFaces
	if useUV2 then SLR_SCX_setMapChannel obj 2 uv2 allFaces
	if useUV3 then SLR_SCX_setMapChannel obj 3 uv3 allFaces
	if useColor then SLR_SCX_setMapChannel obj 0 colors allFaces

	if( mats != undefined ) then obj.material = mats

	update obj
	obj -- return
)

function SLR_SCX_materialLibrary scy_name = -- the .scy of a map is its material list
(
	if( not (doesFileExist scy_name) ) then return undefined

	local res = SLR_SCX_readFile scy_name
	local mats = res[2]

	if( mats.count == 0 ) then return undefined

	SLR_SCX_TEX_KNOWN = false
	local texNames = SLR_SCX_texFromRPK scy_name -- texture list of the map rpk
	local dirs = #( (getFilenamePath scy_name), SLR_SCX_IMPORT_TEXDIR )
	local mm = multiMaterial numsubs:mats.count name:((getFilenameFile scy_name) + "_materials")

	for i=1 to mats.count do
	(
		mm[i] = SLR_SCX_makeMaterial mats[i] texNames dirs SLR_SCX_IMPORT_SLRRMAT
		mm.names[i] = mats[i].name
	)

	format "SCX Import: % material(s) read from %\n" mats.count (filenameFromPath scy_name)
	mm -- return
)

function SLR_RPK_buildPoly file_name polys name scale texNames matlib =
(
	-- builds one editable mesh with every baked mesh of the rpk;
	-- the material index of each mesh becomes its material ID
	local f = fopen file_name "rb"

	if( f == undefined ) then return undefined

	local nv = 0, nf = 0

	for b in polys do ( nv += b[3]; nf += b[4] )

	if( (nv == 0) or (nf == 0) ) then ( fclose f; return undefined )

	local verts = #(); verts[nv] = [0,0,0]
	local uvs = #(); uvs[nv] = [0,0,0]
	local cols = #(); cols[nv] = [1,1,1]
	local faces = #(); faces[nf] = [1,1,1]
	local ids = #(); ids[nf] = 1

	local vBase = 0, fBase = 0
	local q, mIdx, mv, mt, x, y, z, r, g, b, maxID = 0

	for blk in polys do
	(
		q = blk[1] + 16

		for m=1 to blk[2] do
		(
			fseek f q #seek_set
			mIdx = ReadLong f #signed
			mv = ReadLong f #signed
			mt = ReadLong f #signed

			if( mIdx < 0 ) then mIdx = 0
			if( (mIdx+1) > maxID ) then maxID = mIdx+1

			for i=1 to mv do
			(
				fseek f (q + 12 + (i-1)*56) #seek_set

				x = ReadFloat f; y = ReadFloat f; z = ReadFloat f
				verts[vBase+i] = [x*scale, -z*scale, y*scale] -- game axes to max

				ReadFloat f; ReadFloat f; ReadFloat f -- normal, rebuilt by smoothing

				b = ReadByte f #unsigned; g = ReadByte f #unsigned
				r = ReadByte f #unsigned; ReadByte f #unsigned
				cols[vBase+i] = [r/255.0, g/255.0, b/255.0]

				ReadLong f #unsigned -- vertex illumination

				x = ReadFloat f; y = ReadFloat f
				uvs[vBase+i] = [x, -y, 0]
			)

			fseek f (q + 12 + mv*56) #seek_set

			for i=1 to mt do
			(
				faces[fBase+i] = [ (ReadLong f #signed)+1+vBase, (ReadLong f #signed)+1+vBase, (ReadLong f #signed)+1+vBase ]
				ids[fBase+i] = mIdx+1
			)

			vBase += mv
			fBase += mt
			q += 12 + mv*56 + mt*12
		)
	)

	fclose f

	local obj = mesh vertices:verts faces:faces materialIDs:ids name:name

	SLR_SCX_setMapChannel obj 1 uvs faces
	SLR_SCX_setMapChannel obj 0 cols faces

	if( matlib != undefined ) then obj.material = matlib

	update obj
	obj -- return
)

function SLR_CITY_nativeModel rpk_name ref root cache =
(
	-- gametype -> "render 0x..." -> renderref -> mesh + textures
	-- returns #( mesh file, texture names, local position, local rotation ) or undefined
	local rpk = SLR_RPK_readCached rpk_name cache

	if( rpk == undefined ) then return undefined

	local ext = rpk[1], entries = rpk[2]
	local extIdx = bit.shift ref -16
	local localID = bit.and ref 0xFFFF
	local target = entries, targetExt = ext, subName = rpk_name

	if( extIdx != 0 ) then -- the object lives in another rpk (usually objects.rpk)
	(
		if( (extIdx > ext.count) or (ext[extIdx] == undefined) ) then return undefined

		subName = root + ext[extIdx]
		local sub = SLR_RPK_readCached subName cache

		if( sub == undefined ) then return undefined

		target = sub[2]; targetExt = sub[1]
	)

	local gt = SLR_RPK_findEntry target 8 localID -- gametype

	if( gt == undefined ) then return undefined

	local rend = SLR_RPK_values gt "render"

	if( rend.count == 0 ) then return undefined -- sound or ambience object, no model

	local tok = filterString rend[1] " \t"

	if( tok.count < 1 ) then return undefined

	local rref = SLR_RPK_hexToInt tok[1]
	local lPos = [0,0,0], lRot = [0,0,0]

	if( tok.count >= 7 ) then
	(
		lPos = [ (tok[2] as float), (tok[3] as float), (tok[4] as float) ]
		lRot = [ (tok[5] as float), (tok[6] as float), (tok[7] as float) ]
	)

	-- the render reference can point to yet another rpk
	local rExt = bit.shift rref -16
	local rLocal = bit.and rref 0xFFFF
	local rTarget = target, rTargetExt = targetExt

	if( rExt != 0 ) then
	(
		if( (rExt > targetExt.count) or (targetExt[rExt] == undefined) ) then return undefined

		local sub2 = SLR_RPK_readCached (root + targetExt[rExt]) cache

		if( sub2 == undefined ) then return undefined

		rTarget = sub2[2]; rTargetExt = sub2[1]
	)

	local rr = SLR_RPK_findEntry rTarget 14 rLocal

	if( rr == undefined ) then return undefined

	local mr = SLR_RPK_values rr "mesh"

	if( mr.count == 0 ) then return undefined

	local src = SLR_RPK_refSource rTargetExt rTarget (SLR_RPK_hexToInt (SLR_RPK_firstToken mr[1])) 5 root cache

	if( src == undefined ) then return undefined

	local full = root + src

	if( not (doesFileExist full) ) then return undefined

	#( full, (SLR_RPK_renderTextures rTargetExt rTarget rr root cache), lPos, lRot ) -- return
)

function SLR_SCX_weldByNormal obj meshes tresh = -- welds the vertices that share position AND normal
(
	-- the two sides of a double sided part (hood, roof...) sit on the same spot with
	-- opposite normals; welding them breaks the smoothing. Max only tells which vertices
	-- meet: it welds a helper mesh with one face per vertex (the other two corners far
	-- away, so no face collapses; the vertex number rides in the smoothing group).
	-- Every group is split by normal and the faces are pointed to the vertex that stays,
	-- so no vertex moves.
	local k = 0, n
	local nrm = #()
	nrm[obj.numverts] = [0,0,0]

	for m in meshes do
	(
		for i=1 to m.verts.count do
		(
			k += 1
			n = if( (m.normals != undefined) and (i <= m.normals.count) ) then m.normals[i] else [0,0,0]
			if( k <= nrm.count ) then nrm[k] = if( (length n) > 0.000001 ) then (normalize n) else [0,0,0]
		)
	)

	if( k != obj.numverts ) then -- should not happen: plain weld
	(
		meshop.weldVertsByThreshold obj obj.verts tresh
		return false
	)

	local tmp = TriMesh()
	local p, far = 0.0

	setNumVerts tmp (k+2)
	setNumFaces tmp k

	for i=1 to k do
	(
		p = getVert obj i
		setVert tmp i p
		far = amax far (abs p.x) (abs p.y) (abs p.z)
	)

	local away = 1000.0 + 10.0*tresh
	setVert tmp (k+1) ([1,1,1]*(2.0*far + away))
	setVert tmp (k+2) ([1,1,1]*(2.0*far + away) + [away,0,0])

	for i=1 to k do
	(
		setFace tmp i [i, k+1, k+2]
		setFaceSmoothGroup tmp i i
	)

	meshop.weldVertsByThreshold tmp #{1..k} tresh

	local spot = #() -- vertex of the helper each vertex went to
	spot[k] = 0
	local src

	for j=1 to tmp.numfaces do
	(
		src = getFaceSmoothGroup tmp j
		if( (src >= 1) and (src <= k) ) then spot[src] = (getFace tmp j).x as integer
	)

	local groups = #(), rep = #() -- vertex that stays, for every vertex
	rep[k] = k
	local s, g, found, nz = [0,0,0]

	for o=1 to k do
	(
		s = spot[o]
		g = if( (s != undefined) and (s > 0) ) then groups[s] else undefined

		if( g == undefined ) then
		(
			if( (s != undefined) and (s > 0) ) then groups[s] = #(o)
			rep[o] = o
		)
		else
		(
			found = 0

			for r in g while (found == 0) do -- normals less than ~2 degrees apart weld
				if( (nrm[o] == nz) or (nrm[r] == nz) or ((dot nrm[o] nrm[r]) > 0.9994) ) then found = r

			if( found == 0 ) then ( append g o; rep[o] = o ) else rep[o] = found
		)
	)

	local flat = #{} -- faces left without area
	local f, a, b, c

	for i=1 to obj.numfaces do
	(
		f = getFace obj i
		a = rep[f.x as integer]; b = rep[f.y as integer]; c = rep[f.z as integer]

		if( (a == b) or (b == c) or (a == c) ) then flat[i] = true
		else setFace obj i [a, b, c]
	)

	if( not flat.isEmpty ) then meshop.deleteFaces obj flat delIsoVerts:false
	meshop.deleteIsoVerts obj

	true -- return
)

function ImportSCXfile file_name texList:undefined objName:undefined quiet:false sceneRedraw:true noMaterial:false =
(
	local t = timeStamp()
	local res = SLR_SCX_readFile file_name
	local version = res[1]
	local meshes = #()

	for m in res[2] where ((m.verts.count > 0) and (m.faces.count > 0)) do
		append meshes m -- drop empty meshes (marker materials of some track files)

	if( meshes.count == 0 ) then
	(
		if( version != 0 ) then
		(
			if( res[2].count > 0 ) then
			( -- sector files (.scy) only carry the material list of a map
				format "SCX Import: % has % material(s) and no geometry (sector file)\n" (filenameFromPath file_name) res[2].count

				if( not quiet ) then
					messageBox ((filenameFromPath file_name) + " has " + (res[2].count as string) + " materials and no geometry.\n\nSector files (.scy) only hold the material list of a map.\nThe meshes are the .scx files listed in the map rpk - use SCX Map Import on it.") title:"SCX Import" beep:true
			)
			else
				if( not quiet ) then
					messageBox ((filenameFromPath file_name) + " has no mesh inside") title:"SCX Import" beep:true
		)

		return undefined
	)

	local texNames = texList -- the caller may already know the list (map import)

	SLR_SCX_TEX_KNOWN = (texList != undefined)

	if( texNames == undefined ) then
	(
		texNames = SLR_SCX_readTexNames file_name -- .tex file written by the exporter
		if( texNames.count > 0 ) then SLR_SCX_TEX_KNOWN = true

		if( (texNames.count == 0) and SLR_SCX_IMPORT_RPK ) then
			texNames = SLR_SCX_texFromRPK file_name -- game mesh: names live in the rpk
	)

	local dir = getFilenamePath file_name
	local dirs = #( dir, ((getFilenamePath (trimRight dir "\\/")) + "textures\\"), SLR_SCX_IMPORT_TEXDIR )
	local base = if( objName != undefined ) then objName else (getFilenameFile file_name)
	local created = #()

	for m in meshes do m.shading = SLR_SCX_shadingAmount m

	if sceneRedraw then disableSceneRedraw()

	try
	(
		if( SLR_SCX_IMPORT_SEPARATE ) then
		(
			for i=1 to meshes.count do
			(
				local mat = if noMaterial then undefined else (SLR_SCX_makeMaterial meshes[i] texNames dirs SLR_SCX_IMPORT_SLRRMAT)
				local nm = base
				if( meshes.count > 1 ) then nm = (base + "_" + (i as string))

				append created (SLR_SCX_buildObject #(meshes[i]) nm mat SLR_SCX_IMPORT_SCALE)
			)
		)
		else
		(
			local mats

			if noMaterial then
				mats = undefined
			else
			if( meshes.count > 1 ) then
			(
				mats = multiMaterial numsubs:meshes.count name:base

				for i=1 to meshes.count do
					mats[i] = SLR_SCX_makeMaterial meshes[i] texNames dirs SLR_SCX_IMPORT_SLRRMAT
			)
			else
				mats = SLR_SCX_makeMaterial meshes[1] texNames dirs SLR_SCX_IMPORT_SLRRMAT

			append created (SLR_SCX_buildObject meshes base mats SLR_SCX_IMPORT_SCALE)
		)

		for ci=1 to created.count do
		(
			local obj = created[ci]

			if( SLR_SCX_IMPORT_WELD ) then
				SLR_SCX_weldByNormal obj (if SLR_SCX_IMPORT_SEPARATE then #(meshes[ci]) else meshes) SLR_SCX_IMPORT_WELD_TRESH

			if( SLR_SCX_IMPORT_SMOOTH and (obj.numfaces > 0) ) then
				meshop.autoSmooth obj #{1..obj.numfaces} SLR_SCX_IMPORT_SMOOTH_ANGLE -- threshold is in degrees

			update obj
		)
	)
	catch
	(
		if sceneRedraw then enableSceneRedraw()
		format "SCX Import failed on %:\n%\n" (filenameFromPath file_name) (getCurrentException())
		if( not quiet ) then
			messageBox ("Import failed:\n" + (getCurrentException())) title:"SCX Import" beep:true
		return undefined
	)

	if sceneRedraw then
	(
		enableSceneRedraw()
		redrawViews()
	)

	local nv = 0, nf = 0
	for obj in created do ( nv += obj.numverts; nf += obj.numfaces )

	format "SCX Import: % (v%) - % mesh(es), % verts, % faces, % texture(s) in %s\n" \
		(filenameFromPath file_name) version meshes.count nv nf texNames.count (((timeStamp()-t)/1000.0) as string)

	if sceneRedraw then select created
	created -- return
)

-- ------------------------------------------------------------------- UI
if( SLR_SCX_Import != undefined ) then -- recreate if window already exists
	DestroyDialog SLR_SCX_Import

rollout SLR_SCX_Import "SCX Import v1.0"
(
	checkbox weld_chck "Weld vertices" pos:[10,8]
	spinner weld_spn "" pos:[100,8] fieldwidth:52 type:#float range:[0.0,10.0,0.01] scale:0.01

	checkbox smooth_chck "Auto smooth" pos:[10,30]
	spinner smooth_spn "" pos:[100,30] fieldwidth:52 type:#float range:[0.0,180.0,45.0] scale:1.0

	checkbox separate_chck "One object peer material" pos:[10,52]
	checkbox slrrmat_chck "SLRR materials" pos:[10,74]
	checkbox rpk_chck "Textures from the RPK" pos:[10,96] toolTip:"Reads the texture list of the model from the rpk file of the car/part/track"

	label scale_lbl "Scale:" pos:[10,120]
	spinner scale_spn "" pos:[100,118] fieldwidth:52 type:#float range:[0.001,1000.0,1.0] scale:0.1

	label tex_lbl "Textures folder:" pos:[10,142]
	editText texdir_edt "" pos:[6,158] width:120 height:18
	button texdir_btn "..." pos:[128,158] width:24 height:19

	imgTag separator pos:[10,184] width:141 height:1 style:#bmp_stretch bitmap:(bitmap 1 1 color:(color 110 110 110)) -- separate horizontal line

	button import_btn "IMPORT SCX..." pos:[10,192] width:141 height:20

	function update =
	(
		weld_chck.checked = SLR_SCX_IMPORT_WELD
		weld_spn.enabled = SLR_SCX_IMPORT_WELD
		weld_spn.value = SLR_SCX_IMPORT_WELD_TRESH
		smooth_chck.checked = SLR_SCX_IMPORT_SMOOTH
		smooth_spn.enabled = SLR_SCX_IMPORT_SMOOTH
		smooth_spn.value = SLR_SCX_IMPORT_SMOOTH_ANGLE
		separate_chck.checked = SLR_SCX_IMPORT_SEPARATE
		slrrmat_chck.checked = SLR_SCX_IMPORT_SLRRMAT
		slrrmat_chck.enabled = (SLRR_Material != undefined)
		rpk_chck.checked = SLR_SCX_IMPORT_RPK
		scale_spn.value = SLR_SCX_IMPORT_SCALE
		texdir_edt.text = SLR_SCX_IMPORT_TEXDIR
	)

	on weld_chck changed arg do ( SLR_SCX_IMPORT_WELD = arg; weld_spn.enabled = arg )
	on weld_spn changed arg do SLR_SCX_IMPORT_WELD_TRESH = arg
	on smooth_chck changed arg do ( SLR_SCX_IMPORT_SMOOTH = arg; smooth_spn.enabled = arg )
	on smooth_spn changed arg do SLR_SCX_IMPORT_SMOOTH_ANGLE = arg
	on separate_chck changed arg do SLR_SCX_IMPORT_SEPARATE = arg
	on slrrmat_chck changed arg do SLR_SCX_IMPORT_SLRRMAT = arg
	on rpk_chck changed arg do SLR_SCX_IMPORT_RPK = arg
	on scale_spn changed arg do SLR_SCX_IMPORT_SCALE = arg
	on texdir_edt changed arg do SLR_SCX_IMPORT_TEXDIR = arg

	on texdir_btn pressed do
	(
		local dir = getSavePath caption:"Folder with the textures" initialDir:SLR_SCX_IMPORT_TEXDIR

		if( dir != undefined ) then
		(
			if( (dir[dir.count] != "\\") and (dir[dir.count] != "/") ) then dir += "\\"

			SLR_SCX_IMPORT_TEXDIR = dir
			texdir_edt.text = dir
		)
	)

	on import_btn pressed do
	(
		local file_name = getOpenFileName caption:"Import SCX mesh" \
			types:"Invictus meshes (*.scx;*.scy)|*.scx;*.scy|All files (*.*)|*.*" \
			filename:SLR_SCX_IMPORT_LAST_DIR

		if( file_name != undefined ) then
		(
			SLR_SCX_IMPORT_LAST_DIR = getFilenamePath file_name
			ImportSCXfile file_name
		)
	)

	on SLR_SCX_Import open do update()
)

CreateDialog SLR_SCX_Import width:161 height:222
