fileIn( (GetDir #scripts) + "\\ProgressDialog.ms" ) -- include progress dialog support

-- store chosen options after close export dialog
if( SLR_SCX_EXPORT_VERSION == undefined ) then
(
	global SLR_SCX_EXPORT_VERSION = 3
	global SLR_SCX_EXPORT_SHADING = 2
	global SLR_SCX_EXPORT_OPTIMIZE = true
	global SLR_SCX_EXPORT_OPTIMIZE_TYPE = 2
	global SLR_SCX_EXPORT_PHYSIC = false
	global SLR_SCX_EXPORT_PIVOT = false
	global SLR_SCX_EXPORT_SEPARATE = false
	global SLR_SCX_EXPORT_PASSWORD_ENABLED = false
	global SLR_SCX_EXPORT_PASSWORD = "Your password"
)

pCurr_range -- progress range of current operation
progress_curr_prefix = "" -- prefix for name of current operation
progress_step = 0

function progress_curr progress =
(
	--SLR_SCX_Export.progress_bar.value = progress * 100.0 / progress_curr_range
	ProgressDialog.setCurrProgress (progress * 1000.0 / pCurr_range)
)

function progress_curr_range range =
(
	ProgressDialog.setCurrProgress 0
	pCurr_range = range
)

function progress_curr_name name = 
(
	ProgressDialog.setCurOperationName (progress_curr_prefix + name)
)

function MaxRedraw enabled =
(
	if( enabled == 1 ) then
		enableSceneRedraw()
	else 
		disableSceneRedraw()	
)

-- some functions
function avg3 point3array =
(
	local average = point3 0.0 0.0 0.0

	if( point3array.count > 0 ) then
	(
		for i=1 to point3array.count do
		(
			average += point3array[i]
		)

		average /= point3array.count
	)

	average -- return
)

function collectSelection = 
(
	objects = #()

	for obj in selection where (isKindOf obj GeometryClass) do
	(
		append objects obj
	)

	objects -- return
)

function addMapName m_name array = 
(
	local idx = findItem array (filenameFromPath m_name)

	if( idx == 0 ) then
	(
		append array (filenameFromPath m_name)
		(array.count-1) -- return
	)
	else
		(idx-1) -- return
)

function getFacesByVert object vert = 
(	
	local faces_val = meshOp.getFacesUsingVert object #{vert}
	local faces = #()

	for i=1 to faces_val.count do
	(
		if( faces_val[i] ) then
			append faces i
	)

	faces -- return
)

function getFacesByVerts object verts = 
(	
	local faces_val = meshOp.getFacesUsingVert object verts
	local faces = #()

	for i=1 to faces_val.count do
	(
		if( faces_val[i] ) then
			append faces i
	)

	faces -- return
)

function getVertsByFaces object faces = 
(
	local face
	local verts = #()

	for i=1 to faces.count do
	(
		face = getFace object faces[i]

		for j=1 to 3 do
		(
			if( finditem verts face[j] == 0 ) then
				append verts face[j]
		)
	)

	verts -- return
)

function getMapVerts object channel = -- return array of map IDs matched to mesh verts 
(
	if( meshOp.getMapSupport object channel ) then
	(
		-- prepare array for each verteice
		local mapVerts = for i=1 to object.numverts collect #()
		
		local mapFace, meshFace, mVert
		for i=1 to (meshOp.getNumMapFaces object channel) do 
		(
			mapFace = meshOp.getMapFace object channel i
			mapFace = #( (mapFace.x as integer), (mapFace.y as integer), (mapFace.z as integer) )

			meshFace = getFace object i
			meshFace = #( (meshFace.x as integer), (meshFace.y as integer), (meshFace.z as integer) )
			
			for j=1 to 3 do
			(
				if( (findItem mapVerts[meshFace[j]] mapFace[j]) == 0 ) do -- isn't this map vert already added?
				(
					append mapVerts[meshFace[j]] mapFace[j] -- store index
					--append mapVerts[meshFace[j]] (meshop.getMapVert object channel mapFace[j]) -- vertex, index need less memory than map vertex
				)
			)
		)
		
		mapVerts -- return
	)
	else
		undefined -- return
)

function getMapUV object channel indices averaged:false = -- returns arrays of UV vertices for given groups of map IDs 
(
	if( (indices != undefined) and (indices.count > 0) ) then
	(
		UV_Verts = #(); UV_Verts[indices.count] = #() -- init know size array

		progress_curr_range indices.count -- progress dialog
		for i=1 to indices.count do
		(
			if( (indices[i] != undefined) and (indices[i].count > 0) ) then
			(
				UV_Verts[i] = #(); UV_Verts[i].count = indices[i].count -- init know size array

				for j=1 to UV_Verts[i].count do
				(	
					UV_Verts[i][j] = meshop.getMapVert object channel indices[i][j]
				)

				if averaged then 
					UV_Verts[i] = (avg3 UV_Verts[i])
			)
			else
			(
				if averaged then
					UV_Verts[i] = [0,0,0]
				else
					UV_Verts[i] = #([0,0,0])
			)

			progress_curr i -- progress dialog
		)

		UV_Verts -- return
	)
	else
		undefined -- return
)

function getAverageMapVert object channel indices = -- returns average UV of given map indices array
(
	if( (indices != undefined) and (indices.count > 0) ) then
	(
		local mapVerts = #(); mapVerts[indices.count] = [0.0,0.0,0.0] -- init known size array

		for i=1 to indices.count do
		(
			mapVerts[i] = meshop.getMapVert object channel indices[i]
		)

		(avg3 mapVerts) -- return average of map vertices
	)
	else
		[0,0,0] -- return
)

function getColorValueVerts object channel = -- return array of vertex color value matched to mesh verts (0.0-1.0)
(
	if( meshOp.getMapSupport object channel ) then
	(
		local mapVerts = getMapVerts object channel -- get colors from object

		-- prepare array for each verteice
		local valueVerts = #(); valueVerts[object.numverts] = 0 -- init known size array

		local val
		for i=1 to mapVerts.count do
		(
			val = getAverageMapVert object channel mapVerts[i]
			val = ((val[1]+val[2]+val[3])as float)/3.0
			valueVerts[i] = val
		)

		valueVerts -- return
	)
	else
		undefined -- return
)

function getVertNormals object =
(
	local normals = for i=1 to object.numverts collect #()
	local face, faceNormals

	for i=1 to object.numfaces do
	(
		face = getFace object i
		faceNormals = meshOp.getFaceRNormals object i

		for j=1 to 3 do
		(
			if( (findItem normals[face[j]] faceNormals[j]) == 0 ) then -- this normal isn't already added to this vert
				append normals[face[j]] faceNormals[j]
		)
	)

	normals -- return
)

function removeWeldedVerts verts welded =
(
	if( (welded.count > 0) and (verts.count > 1) ) then -- weld needs least two vertices
	(
		if( verts[verts.count] > welded[1] ) then
		(
			if( verts[1] < welded[welded.count] ) then
			(
				local i = welded.count
				local wVerts = welded.count
				
				for j=verts.count to 1 by -1 where (i>0 and verts[j]>welded[1]) do
				(
					while( (i > 0) and (verts[j] < welded[i]) ) do i -= 1

					if( (i > 0) and (verts[j] == welded[i]) ) then
						verts[j] = unsupplied
					else
						verts[j] -= i
				)
			)
			else
			(
				for i=1 to verts.count do
					verts[i] -= welded.count
			)
		)
	)
)

function absP3 point = -- absolute of point3
(
	[abs point.x, abs point.y, abs point.z]
)

function prepareVerts object type:2 = -- returns optimized mesh type:1-good 2:-best
(
	progress_curr_name "Optimizing: Starting..."

	local normalTreshold = 0.001, vcTreshold = 0.1, uvTreshold = 0.0004 -- optimize tresholds

	local con, con2, con3 -- continue, use to break loops (replace exit function)

	-- work on copy, convert to mesh
	local mesh = snapshotAsMesh object

-- step 1 - divide vertices which contains multiplied peer-vertice information (game no supports it)
	meshOp.weldVertsByThreshold mesh mesh.verts 0.0001

	progress_curr_name "Optimizing: Collecting Normals data..."
	gc() -- free memory
	--local VertNormals = getVertNormals object
	local VertNormals = getVertNormals mesh
	
	-- collect maping
	local MapVerts = #()
	
	progress_curr_name "Optimizing: Collecting Vertex-Colors data..."
	append MapVerts (getMapUV mesh 0 (getMapVerts mesh 0))

	progress_curr_name "Optimizing: Collecting UV-Mapping channel 1 data..."
	append MapVerts (getMapUV mesh 1 (getMapVerts mesh 1))

	progress_curr_name "Optimizing: Collecting UV-Mapping channel 2 data..."
	append MapVerts (getMapUV mesh 2 (getMapVerts mesh 2))

	progress_curr_name "Optimizing: Collecting UV-Mapping channel 3 data..."
	append MapVerts (getMapUV mesh 3 (getMapVerts mesh 3))


	progress_curr_name "Optimizing: Analysing vertices division..."
	local multiVert, complexVerts = #() -- array of verts that will be break
	for i=1 to mesh.numverts do
	(
		multiVert = false

		if( VertNormals[i].count > 1 ) then -- faces with different smooth groups based on this vert
		(
			con = true
			for j=1 to (VertNormals[i].count-1) where con do -- compare each verts, and look for any pair which is out of treshold
			(
				for k=(j+1) to VertNormals[i].count where con do
				(
					--if( (distance VertNormals[i][j] VertNormals[i][k]) > normalTreshold ) then
					if( (distance (absP3 VertNormals[i][j]) (absP3 VertNormals[i][k])) > normalTreshold ) then
					(
						multiVert = true
						con = false -- break BOTH loops
					)
				)
			)
		)--*/

		if( not multiVert ) then
		(
			con = true
			for ch=1 to 4 where con do -- check for multi map verts peer this mesh vert
			(
				if( MapVerts[ch] != undefined ) then -- is this channel supported?
				(
					if( (MapVerts[ch][i].count) > 1 ) then -- there is more than one uv peer vertex
					(
						for j=1 to (MapVerts[ch][i].count-1) where con do -- compare each verts, and look for any pair which is out of treshold
						(
							for k=(j+1) to MapVerts[ch][i].count do
							(
								if( ch == 1 ) then -- vertex color
								(
									if( (distance MapVerts[ch][i][j] MapVerts[ch][i][k]) > vcTreshold ) then
									(
										multiVert = true
										con = false -- break BOTH loops
									)
								)
								else -- uv map
								(
									if( (distance MapVerts[ch][i][j] MapVerts[ch][i][k]) > uvTreshold ) then
									(
										multiVert = true
										con = false -- break BOTH loops
									)
								)
							)
						)
					)
				)
			)
		)--*/

		if( multiVert ) then 
			append complexVerts i
	)
	
	progress_curr_name "Optimizing: Breaking vertices..."
	local faces_indices = getFacesByVerts mesh complexVerts
	local faces = for i=1 to faces_indices.count collect (getFace mesh faces_indices[i])
	breakGroups = for i=1 to mesh.numverts collect #(i)
	
	meshop.breakVerts mesh complexVerts -- break
	
	local face, idx
	for i=1 to faces.count do
	(
		face = getFace mesh faces_indices[i] -- get coresponding face
		
		for j=1 to 3 do 
		(	
			idx = findItem breakGroups[faces[i][j]] face[j] -- check this verticew isn't already added
			
			if( idx == 0 ) then
				append breakGroups[faces[i][j]] face[j]
		)
	)

	-- delete one element arrays
	idx = 0
	while( idx < breakGroups.count ) do
	(
		idx += 1

		if( breakGroups[idx].count <= 1 ) then
		(
			deleteItem breakGroups idx
			idx -= 1
		)
	)

-- step 2 - stick similar verts (optimize)
	if type == 2 then -- best optimization
	(
		progress_curr_name "Optimizing: Collecting normals data..."
		VertNormals = undefined; -- flush old
		VertNormals = getMapUV mesh ((meshop.getNumMaps mesh)-1) (getMapVerts mesh ((meshop.getNumMaps mesh)-1)) averaged:true -- get normals saved in map channel

		MapVerts = #() -- flush old
		progress_curr_name "Optimizing: Collecting vertex colors data..."
		append MapVerts (getMapUV mesh 0 (getMapVerts mesh 0))
		
		progress_curr_name "Optimizing: Collecting UV-mapping (channel 1) data..."
		append MapVerts (getMapUV mesh 1 (getMapVerts mesh 1))

		progress_curr_name "Optimizing: Collecting UV-mapping (channel 2) data..."
		append MapVerts (getMapUV mesh 2 (getMapVerts mesh 2))

		progress_curr_name "Optimizing: Collecting UV-mapping (channel 3) data..."
		append MapVerts (getMapUV mesh 3 (getMapVerts mesh 3))

		progress_curr_name "Optimizing: Analysing vertices unification..."
		local weldGroups = #(); -- groups of verts that will be weld
		local wGroup, wg_idx, optimize
		for i=1 to breakGroups.count do
		(
			wGroup = #()

			for j=1 to (breakGroups[i].count-1) do
			(
				-- check is this vert already assigned to any weld group
				wg_idx = 0; con = true
				for k=1 to wGroup.count where con do
				(
					wg_idx = (findItem wGroup[k] breakGroups[i][j])

					if( wg_idx > 0 ) then -- found group with this vert
					(
						wg_idx = k
						con = false -- break loop
					)
				)

				if( wg_idx == 0 ) then -- there is no group with this vert
				(
					append wGroup #(breakGroups[i][j]) -- add new group with this vert
					wg_idx = wGroup.count 
				)

				for k=(j+1) to breakGroups[i].count do -- compare this faces with rest
				(
					if( (findItem wGroup[wg_idx] breakGroups[i][k]) == 0 ) then -- check aren't these vertices already welded
					(
						optimize = true

						-- check normals
						if( (distance VertNormals[breakGroups[i][j]] VertNormals[breakGroups[i][k]]) > normalTreshold ) then
						(
							optimize = false
						)

						-- check v.colors and UV maping
						if( optimize ) then -- still possible to optimize
						(
							con = true
							for ch=1 to 4 where con do -- check vertex colors and uv maping on all channels
							(
								if( MapVerts[ch] != undefined ) then -- is this channel supported?
								(
									if( ch == 1 ) then -- vertex color chennel
									(
										if( (distance MapVerts[ch][breakGroups[i][j]][1] MapVerts[ch][breakGroups[i][k]][1]) >= vcTreshold ) then
										(
											optimize = false
											con = false -- break loop
										)
									)
									else -- texture maping channel
									(
										if( (distance MapVerts[ch][breakGroups[i][j]][1] MapVerts[ch][breakGroups[i][k]][1]) >= uvTreshold ) then
										(
											optimize = false
											con = false -- break loop
										)
									)
								)
							)
						)

						if( optimize ) then
							append wGroup[wg_idx] breakGroups[i][k]
					)
				)
			)

			for j=1 to wGroup.count do
				append weldGroups wGroup[j] -- collect results
		)
		
		progress_curr_name "Optimizing: Unifing vertices..."
		local welded = #(), lastWelded, group, groupOrg
		progress_curr_range weldGroups.count -- progress dialog
		for i=1 to weldGroups.count where weldGroups[i].count>1 do
		(
			sort weldGroups[i]
			lastWelded = #(); join lastWelded weldGroups[i] -- copy the group

			removeWeldedVerts weldGroups[i] welded -- update indices

			-- vertices removed by a previous weld are marked unsupplied, drop them
			group = #(); groupOrg = #()
			for k=1 to weldGroups[i].count where (weldGroups[i][k] != unsupplied) do
			(
				append group weldGroups[i][k]
				append groupOrg lastWelded[k]
			)

			if( group.count > 1 ) then
			(
				meshOp.weldVertSet mesh group -- weld the group

				deleteItem groupOrg 1 -- delete first vert (it wasn't deleted when weld)
				join welded groupOrg -- add last welded to welded group
				sort welded
			)

			progress_curr i -- progress dialog
		)
	)

	mesh -- return optimization result mesh
)

-- ==========================================================================
--  SCX v4 support (block table format)
-- ==========================================================================
-- v4 stores face indices as 16bit values, but the game loads them with a sign
-- extension (MOVSX), so one mesh block has to stay below 32768 vertices.
global SLR_SCX_V4_VERT_LIMIT = 32000

function writeFixedString file str size = -- write string padded with zeros to fixed size
(
	local s = substring str 1 size

	WriteString file s
	fseek file -1 #seek_cur -- back to string terminating 0 char

	for i=s.count to (size-1) do
		WriteByte file 0x00 #unsigned
)

function clampByte val = -- float to 0-255 byte
(
	local v = (val as integer)

	if( v < 0 ) then v = 0
	if( v > 255 ) then v = 255

	v -- return
)

function hasVertexColors mesh = -- true only when the vertex color channel really holds colors
(
	if( not (meshOp.getMapSupport mesh 0) ) then
		return false

	local count = meshop.getNumMapVerts mesh 0
	local val

	for i=1 to count do
	(
		val = meshop.getMapVert mesh 0 i

		if( (val.x < 0.999) or (val.y < 0.999) or (val.z < 0.999) ) then
			return true -- something else than plain white
	)

	false -- return
)

function v4EntryHead file slot dataType = -- material entry header: slot index + byte swapped data type
(
	WriteShort file slot #unsigned
	WriteShort file (dataType*256) #unsigned -- 0-color, 1-float, 5-int, 6-map, 8-string
)

function v4WriteColor file slot col alpha = -- data type 0 - BGRA color
(
	v4EntryHead file slot 0

	WriteByte file (clampByte col.b) #unsigned
	WriteByte file (clampByte col.g) #unsigned
	WriteByte file (clampByte col.r) #unsigned
	WriteByte file (clampByte alpha) #unsigned
)

function v4WriteAmount file slot val = -- data type 1 - float
(
	v4EntryHead file slot 1
	WriteFloat file val
)

function v4WriteName file slot name = -- data type 8 - 32 chars string
(
	v4EntryHead file slot 8
	writeFixedString file name 32
)

function v4WriteMap file slot bmp map_names environment:false = -- data type 6 - map definition
(
	local channel = 0

	if( environment ) then
		channel = -2 -- reflection mapping, no UV channel used
	else
	(
		channel = bmp.coords.mapChannel

		if( channel < 1 ) then channel = 1
		if( channel > 3 ) then channel = 3

		channel -= 1 -- the game counts UV channels from 0
	)

	v4EntryHead file slot 6

	WriteLong file (addMapName bmp.filename map_names) #signed -- texture index (order of .tex file)
	WriteLong file channel #signed
	WriteLong file 3 #signed -- tilling flags: 3 = tile U + tile V
	WriteFloat file bmp.coords.U_Tiling
	WriteFloat file bmp.coords.V_Tiling
	local offV = -bmp.coords.V_Offset -- V axis is flipped on export
	if( offV == 0.0 ) then offV = 0.0 -- avoid writing -0.0

	WriteFloat file bmp.coords.U_Offset
	WriteFloat file offV

	if( environment ) then 0 else (channel+1) -- return used max map channel
)

function v4MaterialFlags hasDiff1 hasDiff2 hasSpec hasRefl = -- combinations taken from the game meshes
(
	local flags = 0

	if( hasDiff1 or hasDiff2 ) then -- textured material
	(
		if( hasDiff2 ) then flags += 0x01
		if( hasDiff2 and (not hasDiff1) ) then flags += 0x02 -- second layer is the base one
		if( hasSpec ) then flags += 0x04
		if( hasRefl ) then flags += 0x08
	)
	else -- material without any diffuse texture
	(
		flags = 0x30

		if( hasSpec ) then flags += 0x01
		if( hasRefl ) then flags += 0x02
	)

	flags -- return
)

function pickExportMaterial object matID physic = -- returns max material of given material group
(
	local mesh_mat

	if( physic ) then
	( -- material for physic
		mesh_mat = Standard()
		mesh_mat.name = ""
		mesh_mat.diffuse = color 255 0 128 -- pink
		mesh_mat.specular = color 255 255 255
		mesh_mat.specularLevel = 50
		mesh_mat.selfIllumAmount = 50
		mesh_mat.opacity = 50
	)
	else
	if( (matID != 0) and (isKindOf object.material multiSubMaterial) ) then
	(
		local idx = finditem object.material.materialIDList matID

		if( idx != 0 ) then
		(
			if( ((classOf object.material.materialList[idx]) == Standard) or ((classOf object.material.materialList[idx]) == SLRR_Material) ) then
				mesh_mat = object.material.materialList[idx]
			else
			(
				if( (classOf object.material.materialList[idx]) == multiSubMaterial ) then
					messageBox( "MultiSub material in MultiSub material is no suported!\nExported as blank material" ) title:"Error" beep:true
				else
					messageBox( "Material \"" + ((ClassOf object.material.materialList[idx]) as string) + "\" is no supported!\nExported as blank material" ) title:"Error" beep:true

				mesh_mat = SLRR_Material() -- empty material
				mesh_mat.name = "unsuported material"
			)
		)
		else
		(
			messageBox( "Missing material!\nExported as blank material" ) title:"Error" beep:true
			mesh_mat = SLRR_Material() -- empty material
			mesh_mat.name = "missing material"
		)
	)
	else -- single material
	(
		if( ((classOf object.material) == Standard) or ((classOf object.material) == SLRR_Material) ) then
			mesh_mat = object.material
		else
		(
			messageBox( "Material \"" + ((ClassOf object.material) as string) + "\" is no supported!\nExported as blank material" ) title:"Error" beep:true
			mesh_mat = SLRR_Material() -- empty material
			mesh_mat.name = "unsuported material"
		)
	)

	mesh_mat -- return
)

function writeV4Material file mesh_mat map_names physic =
(
	-- writes one v4 material block, returns #( highest used UV channel, shading multiplier )
	local maxUV = 0, ch
	local normals_mul = 1.0

	local diff1_map, diff2_map, illum_map, refl_map, bump_map
	local diff_col = (color 128 128 128), spec_col = (color 0 0 0), illum_col = (color 0 0 0)
	local opacity = 1.0, gloss = 0.0, refl_am = 0.0, bump_am = 0.0
	local use_spec = false, use_illum = false, use_alpha = false
	local m_name = ""

	if( not physic ) then
	(
		-- material name (32 chars max), "_R"/"_P" prefix marks track render/physic only material
		if( (isKindOf mesh_mat SLRR_Material) and (mesh_mat.track_render != mesh_mat.track_physic) ) then
		(
			if( mesh_mat.track_render ) then
				m_name = "_R" -- name prefix for render only material
			else
				m_name = "_P" -- name prefix for physic only material

			m_name += (substring mesh_mat.name 1 30)
		)
		else
			m_name = (substring mesh_mat.name 1 32)

		if( isKindOf mesh_mat SLRR_Material ) then
		(
			normals_mul = mesh_mat.shad -- store shading amount

			diff_col = mesh_mat.delegate.diffuse
			opacity = mesh_mat.delegate.opacity/100.0
			use_alpha = mesh_mat.P_diff_opac

			-- SLRR material keeps the specular level inside the specular color
			spec_col = (mesh_mat.delegate.specular * (mesh_mat.delegate.specularLevel/100.0))
			gloss = (mesh_mat.delegate.glossiness/100.0)*54.0
			use_spec = (mesh_mat.delegate.specularLevel > 0.0)

			if( isKindOf mesh_mat.delegate.diffuseMap.map1.map2 bitmaptexture ) then
				diff1_map = mesh_mat.delegate.diffuseMap.map1.map2

			if( isKindOf mesh_mat.delegate.diffuseMap.map2 bitmaptexture ) then
				diff2_map = mesh_mat.delegate.diffuseMap.map2

			if( isKindOf mesh_mat.delegate.selfIllumMap.map1 bitmaptexture ) then
				illum_map = mesh_mat.delegate.selfIllumMap.map1

			if( isKindOf mesh_mat.delegate.reflectionMap.map1 bitmaptexture ) then
			(
				refl_map = mesh_mat.delegate.reflectionMap.map1
				refl_am = mesh_mat.P_reflect_am/100.0 -- v4 has its own reflection amount, no specular tricks needed
			)

			if( (illum_map == undefined) and mesh_mat.delegate.useSelfIllumColor ) then
			(
				illum_col = mesh_mat.delegate.selfIllumColor
				use_illum = ((illum_col.r + illum_col.g + illum_col.b) > 0.5) -- black illumination is the same as none
			)
		)
		else -- standard material
		(
			diff_col = mesh_mat.diffuse
			opacity = mesh_mat.opacity/100.0
			use_alpha = (mesh_mat.opacityMap != undefined)

			spec_col = (mesh_mat.specular * (mesh_mat.specularLevel/100.0))
			gloss = (mesh_mat.glossiness/100.0)*54.0
			use_spec = (mesh_mat.specularLevel > 0.0)

			if( isKindOf mesh_mat.diffuseMap bitmaptexture ) then
				diff1_map = mesh_mat.diffuseMap

			if( isKindOf mesh_mat.selfIllumMap bitmaptexture ) then
				illum_map = mesh_mat.selfIllumMap

			if( isKindOf mesh_mat.bumpMap bitmaptexture ) then
			(
				bump_map = mesh_mat.bumpMap
				bump_am = mesh_mat.bumpMapAmount/100.0
			)

			if( isKindOf mesh_mat.reflectionMap bitmaptexture ) then
			(
				refl_map = mesh_mat.reflectionMap
				refl_am = mesh_mat.reflectionMapAmount/100.0
			)

			if( (illum_map == undefined) and mesh_mat.useSelfIllumColor ) then
			(
				illum_col = mesh_mat.selfIllumColor
				use_illum = ((illum_col.r + illum_col.g + illum_col.b) > 0.5) -- black illumination is the same as none
			)
		)
	)
	else -- physic material, the game uses an almost empty one
		diff_col = (color 255 0 128) -- pink

-- count entries and block size
	local entries = 2, size = 20 + (4+32) + (4+4) -- name + diffuse color

	if( use_illum ) then ( entries += 1; size += (4+4) )
	if( use_spec ) then ( entries += 2; size += (4+4)+(4+4) )
	if( diff1_map != undefined ) then ( entries += 1; size += (4+28) )
	if( diff2_map != undefined ) then ( entries += 1; size += (4+28) )
	if( bump_map != undefined ) then ( entries += 2; size += (4+28)+(4+4) )
	if( illum_map != undefined ) then ( entries += 1; size += (4+28) )
	if( refl_map != undefined ) then ( entries += 2; size += (4+28)+(4+4) )

-- block header
	WriteLong file 0 #unsigned -- block type: material
	WriteLong file size #unsigned
	WriteLong file (v4MaterialFlags (diff1_map != undefined) (diff2_map != undefined) use_spec (refl_map != undefined)) #unsigned

	if( use_alpha or (opacity < 1.0) ) then
		WriteFloat file 2.0 -- transparency value used by all alpha materials of the game
	else
		WriteLong file 0 #unsigned

	WriteLong file entries #unsigned

-- entries (order taken from the game meshes)
	v4WriteName file 0 m_name
	v4WriteColor file 0 diff_col (opacity*255.0)

	if( use_illum ) then
		v4WriteColor file 2 illum_col 255 -- illumination color

	if( use_spec ) then
	(
		v4WriteAmount file 0 gloss -- glossiness
		v4WriteColor file 1 spec_col 255 -- specular color
	)

	if( diff1_map != undefined ) then
	(
		ch = v4WriteMap file 0 diff1_map map_names -- diffuse layer 1
		if( ch > maxUV ) then maxUV = ch
	)

	if( diff2_map != undefined ) then
	(
		ch = v4WriteMap file 1 diff2_map map_names -- diffuse layer 2
		if( ch > maxUV ) then maxUV = ch
	)

	if( bump_map != undefined ) then
	(
		ch = v4WriteMap file 2 bump_map map_names -- bump map
		if( ch > maxUV ) then maxUV = ch

		v4WriteAmount file 2 bump_am -- bump amount
	)

	if( illum_map != undefined ) then
	(
		ch = v4WriteMap file 4 illum_map map_names -- light map
		if( ch > maxUV ) then maxUV = ch
	)

	if( refl_map != undefined ) then
	(
		v4WriteMap file 3 refl_map map_names environment:true -- reflection map
		v4WriteAmount file 1 refl_am -- reflection amount
	)

	#(maxUV, normals_mul) -- return
)

function exportSCX obj file_name version:4 optimize:true shading:1 physic:false pivot:false = 
(
	undo off
	(
		MaxRedraw 0 -- disable interface redraw

		file_scx = fopen file_name "wb" -- write binnary destroy existing

		if( file_scx != undefined ) then
		(
			print( "Export start: " )
			local t = timeStamp()
			progress_curr_name "Starting..."

			local flags
			local map_names = #() -- tex file
			
			WriteString file_scx "INVO"
			fseek file_scx -1 #seek_cur -- back to string terminating 0 char

		-- Prepare mesh to export, collect some mesh data
			progress_curr_name "Preparing mesh..."
			local object = copy obj -- keeps modifiers

			if( pivot ) then -- export in pivot space (position and rotation zeroed, scale kept)
				object.transform = (scaleMatrix object.scale)
			
			--if( ((classOf object) != Editable_mesh) and ((classOf object) != PolyMeshObject) ) then
				convertToMesh object

			if( physic ) then -- remove atributes not needed for physic
			(
				for ch=0 to 3 where (ch < (meshop.getNumMaps object)) do
					meshOp.setMapSupport object ch false -- vertex color + UV channels

				for i=1 to object.numfaces do
				(
					setFaceMatID object i 1
					setFaceSmoothGroup object i 1
				)
			)

			meshOp.autoedge object.mesh object.mesh.Edges 0 -- make all faces triangles
			--meshOp.removeDegenerateFaces object.mesh -- removes UV maping 
			meshOp.removeIllegalFaces object.mesh
			meshOps.removeIsolatedVerts object.mesh

			-- save current normals in map channel
			progress_curr_name "Transforming Normals data..."
			local norChannel
			if( (meshop.getNumMaps object) < 100 ) then
			(
				meshop.setNumMaps object ((meshop.getNumMaps object)+1) keep:true -- add new map channel
				norChannel = (meshop.getNumMaps object)-1
			)
			else
				norChannel = 99

			meshop.freeMapChannel object norChannel -- enable added channel
			meshop.setMapSupport object norChannel true
			meshop.setNumMapVerts object norChannel (3*object.numfaces)
			meshop.setNumMapFaces object norChannel object.numfaces

			local normal
			progress_curr_range object.numfaces -- progress dialog
			for i=1 to object.numfaces do
			(
				normal = meshOp.getFaceRNormals object i

				meshop.setMapVert object norChannel ((i*3)-2) normal[1]
				meshop.setMapVert object norChannel ((i*3)-1) normal[2]
				meshop.setMapVert object norChannel (i*3) normal[3]

				meshop.setMapFace object norChannel i [(i*3)-2, (i*3)-1, i*3]

				progress_curr i -- progress dialog
			)
				
			-- create optimized or not mesh for export
			local meshObj -- will store optimized mesh
			if( optimize != 0 ) then
			(
				meshObj = prepareVerts object type:optimize -- break only these vertices which really need it
			)
			else
			(
				-- "Preparing mesh..." already displayed
				meshObj = snapshotAsMesh object
				meshOp.breakVerts meshObj meshObj.verts -- just break all vertices
			)
				
			-- get normals
			progress_curr_name "Collecting Normals data..."
			local mesh_normals = getMapUV meshObj ((meshop.getNumMaps meshObj)-1) (getMapVerts meshObj ((meshop.getNumMaps meshObj)-1)) averaged:true -- get normals saved in map channel
			print( "Get normals: " + (((timeStamp()-t)/1000.0) as string) + "s" ); t = timeStamp()
				
			-- get vertex colors and UV maping indices
			local UV_channels = #(1,2,3) -- offseted channels
			local mesh_UVmaps = #()
			progress_curr_name "Collecting Vertex-Colors data..."
			append mesh_UVmaps (getMapVerts meshObj 0)

			progress_curr_name "Collecting UV-Mapping (channel 1) data..."
			append mesh_UVmaps (getMapVerts meshObj 1)

			progress_curr_name "Collecting UV-Mapping (channel 2) data..."
			append mesh_UVmaps (getMapVerts meshObj 2)
			
			progress_curr_name "Collecting UV-Mapping (channel 3) data..."
			append mesh_UVmaps (getMapVerts meshObj 3)

			progress_curr_name "Collecting Vertex-Shading data..."
			append mesh_UVmaps (getColorValueVerts meshObj -1) -- vertex illumination (shadows)

			print( "Get mapping: " + (((timeStamp()-t)/1000.0) as string) + "s" ); t = timeStamp()

			-- collect faces by material id
			progress_curr_name "Collecting material groups..."
			local MaterialID = #() -- id of each collected face and vertice groups
			local MatFace = #() -- groups of faces
			local MatVert = #() -- groups of vertices
			if( (not physic) and (isKindOf object.material multiSubMaterial) and (object.material.numsubs > 1) ) then
			(
				-- collect faces
				progress_curr_name "Collecting material groups (faces)..."
				local ID, idx
				progress_curr_range meshObj.numfaces -- progress dialog
				for i=1 to meshObj.numfaces do
				(
					ID = getFaceMatID meshObj i
					idx = finditem MaterialID ID

					if( idx != 0 ) then
						append MatFace[idx] i
					else
					(
						append MaterialID ID
						append MatFace #(i)
					)

					progress_curr i -- progress dialog
				)
					
				-- collect verts
				progress_curr_name "Collecting material groups (vertices)..."
				progress_curr_range MaterialID.count -- progress dialog
				for i=1 to MaterialID.count do
				(
					MatVert[i] = getVertsByFaces meshObj MatFace[i]
					progress_curr i -- progress dialog
				)

				-- if mat ID is bigger than sub-mat then roll the ID
				for i=1 to MaterialID.count do
					MaterialID[i] = 1+(mod (MaterialID[i]-1) object.material.numsubs) -- roll to if is out of range
			)
			else
			(
				progress_curr_name "Analysing material groups..."
				progress_curr_range 2 -- progress dialog

				append MaterialID 0

				MatVert = #(#())
				MatVert[1] = (for i=1 to meshObj.numverts collect i)
				progress_curr 1 -- progress dialog

				MatFace = #(#())
				MatFace[1] = (for i=1 to meshObj.numfaces collect i)
				progress_curr 2 -- progress dialog
			)
			--messageBox( "IDs: " + (MaterialID as string) + "\nFaces: " + (MatFace as string) + "\nVertices: " + (MatVert as string) )

			if( version == 3 ) then -- scx version v3
			(
				WriteLong file_scx 3 #unsigned -- file version

				local mesh_mat, idx, normals_mul, progress_mat_count
				for matID=1 to MaterialID.count do
				(
					print( "Material start" )
					t = timeStamp()
					progress_mat_count = ("Material " + (matID as string) + "/" + (MaterialID.count as string) + " - ")
					progress_curr_name (progress_mat_count + "Saving material properties...")

					normals_mul = 1.0

					mesh_mat = pickExportMaterial object MaterialID[matID] physic

					if( not physic ) then
					(
						local diff1_ch=0, diff2_ch=0, layer3_ch=0, illum_ch=0, bump_ch=0 -- UV channels

						WriteLong file_scx 0x88 #unsigned -- extended material size

						if( isKindOf mesh_mat SLRR_Material ) then
						(
							normals_mul = mesh_mat.shad -- store shading amount

							WriteFloat file_scx (mesh_mat.delegate.diffuse.r/255.0) -- diffuse color red
							WriteFloat file_scx (mesh_mat.delegate.diffuse.g/255.0) -- diffuse color green
							WriteFloat file_scx (mesh_mat.delegate.diffuse.b/255.0) -- diffuse color blue

							WriteFloat file_scx (mesh_mat.delegate.opacity/100.0) -- opacity

							-- Specular color and specular amount are associated with reflection amount. Need special calculations:
							local s_color, s_level
							--if( isKindOf mesh_mat.reflectionMap bitmaptexture ) then
							if( (isKindOf mesh_mat.delegate.reflectionMap.map1 bitmaptexture) and (mesh_mat.P_reflect_am > 0.09) ) then
							(
								local scol_val = ( mesh_mat.P_reflect_am / (( mesh_mat.delegate.specular.r + mesh_mat.delegate.specular.g + mesh_mat.delegate.specular.b )/(3.0*255.0) * 100) ) -- calculate total color correction value
								
								s_color = (mesh_mat.delegate.specular * scol_val)
								s_level = (mesh_mat.delegate.specularLevel * (1/scol_val))
							)
							else
							(
								s_color = mesh_mat.delegate.specular
								s_level = mesh_mat.delegate.specularLevel
							)

							WriteFloat file_scx (s_color.r/255.0) -- specular color red
							WriteFloat file_scx (s_color.g/255.0) -- specular color green
							WriteFloat file_scx (s_color.b/255.0) -- specular color blue

							WriteFloat file_scx (s_level/100.0) -- specular level
							WriteFloat file_scx ((mesh_mat.delegate.glossiness/100.0)*54.0) -- glossiness amount
						)
						else -- standard material
						(
							WriteFloat file_scx (mesh_mat.diffuse.r/255.0) -- diffuse color red
							WriteFloat file_scx (mesh_mat.diffuse.g/255.0) -- diffuse color green
							WriteFloat file_scx (mesh_mat.diffuse.b/255.0) -- diffuse color blue

							WriteFloat file_scx (mesh_mat.opacity/100.0) -- opacity

							WriteFloat file_scx (mesh_mat.specular.r/255.0) -- specular color red
							WriteFloat file_scx (mesh_mat.specular.g/255.0) -- specular color green
							WriteFloat file_scx (mesh_mat.specular.b/255.0) -- specular color blue
							WriteFloat file_scx (mesh_mat.specularLevel/100.0) -- specular amount
							WriteFloat file_scx ((mesh_mat.glossiness/100.0)*54.0) -- glossiness amount
						)

					-- properties flags
						flags = 0
						-- alpha opacity flag
						if( isKindOf mesh_mat SLRR_Material ) then
						(
							if( mesh_mat.P_diff_opac ) then
								flags += 0x00000001
						)
						else -- standard material
						(
							if( mesh_mat.opacityMap != undefined ) then
								flags += 0x00000001
						)

						-- layer1 blend flags
						if( (isKindOf mesh_mat SLRR_Material) and (mesh_mat.P_diff_mix != 1) ) then
						(
							flags += 0x00000100 -- blend enable (diffuse color)

							if( mesh_mat.P_diff_mix == 3 ) then
								flags += 0x00000002 -- blend to vertex color
						)

						-- layer2 mix flags
						if( (isKindOf mesh_mat SLRR_Material) and (mesh_mat.P_diff2_mix == 2) ) then
						( -- mix by vertex color
							flags += 0x00001000
						)
						else
							flags += 0x00000040 -- mix by alpha

						WriteLong file_scx flags #unsigned -- write flags

				-- textures info
					-- diffuse layer 1
						if( isKindOf mesh_mat SLRR_Material ) then
						(
							if( isKindOf mesh_mat.delegate.diffuseMap.map1.map2 bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.delegate.diffuseMap.map1.map2.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode

								diff1_ch = mesh_mat.delegate.diffuseMap.map1.map2.coords.mapChannel
							)
							else
							(
								diff1_ch = 0 -- no diff 1 map 

								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode
							)
						)
						else -- standard material
						(
							if( isKindOf mesh_mat.diffuseMap bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.diffuseMap.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode

								diff1_ch = mesh_mat.diffuseMap.coords.mapChannel
							)
							else
							(
								diff1_ch = 0 -- no diff 1 map 

								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode
							)
						)

					-- bump map
						WriteByte file_scx 0xFF #unsigned -- no map
						WriteByte file_scx 0xFF #unsigned -- bump map mode

					-- specular map
						WriteByte file_scx 0xFF #unsigned -- no map
						WriteByte file_scx 0xFF #unsigned -- specular map mode

					-- reflection map
						if( isKindOf mesh_mat SLRR_Material ) then
						(
							if( isKindOf mesh_mat.delegate.reflectionMap.map1 bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.delegate.reflectionMap.map1.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode
							)
							else
							(
								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode
							)
						)
						else -- standard material
						(
							if( isKindOf mesh_mat.reflectionMap bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.reflectionMap.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode
							)
							else
							(
								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode
							)
						)

					-- diffuse layer 2
						if( isKindOf mesh_mat SLRR_Material ) then
						(
							if( isKindOf mesh_mat.delegate.diffuseMap.map2 bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.delegate.diffuseMap.map2.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode

								diff2_ch = mesh_mat.delegate.diffuseMap.map2.coords.mapChannel
							)
							else
							(
								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode

								diff2_ch = 0 -- no diff 2 map
							)
						)
						else
						(
							WriteByte file_scx 0xFF #unsigned -- no map
							WriteByte file_scx 0xFF #unsigned -- map mode
						)

					-- unknown map
						WriteByte file_scx 0xFF #unsigned -- no map
						WriteByte file_scx 0xFF #unsigned -- unknown map mode
						
					-- light map
						if( isKindOf mesh_mat SLRR_Material ) then
						(
							if( isKindOf mesh_mat.delegate.selfIllumMap.map1 bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.delegate.selfIllumMap.map1.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode

								illum_ch = mesh_mat.delegate.selfIllumMap.map1.coords.mapChannel
							)
							else
							(
								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode

								illum_ch = 0
							)
						)
						else -- standard material
						(
							if( isKindOf mesh_mat.selfIllumMap bitmaptexture ) then
							(
								local m_idx = (addMapName mesh_mat.selfIllumMap.filename map_names)
								WriteByte file_scx m_idx #unsigned
								WriteByte file_scx 0x00 #unsigned -- map mode

								illum_ch = mesh_mat.selfIllumMap.coords.mapChannel
							)
							else
							(
								WriteByte file_scx 0xFF #unsigned -- no map
								WriteByte file_scx 0xFF #unsigned -- map mode

								illum_ch = 0
							)
						)

					-- unknown map 2
						WriteByte file_scx 0x00 #unsigned -- no map
						WriteByte file_scx 0x00 #unsigned -- unknown map 2 mode

					-- unknown flags 2
						WriteLong file_scx 0x00000040 #unsigned -- write flags

					-- unknown map 3
						WriteByte file_scx 0xFF #unsigned -- no map
						WriteByte file_scx 0xFF #unsigned -- unknown map mode
						
					-- unknown map 4
						WriteByte file_scx 0xFF #unsigned -- no map
						WriteByte file_scx 0xFF #unsigned -- unknown map mode
						
					-- unknown map 5
						WriteByte file_scx 0xFF #unsigned -- no map
						WriteByte file_scx 0xFF #unsigned -- unknown map mode

					-- uv channels
						-- calculate channels offset (light map have to be on 2nd channel)
						if( illum_ch > 0 ) then
						(
							UV_channels = #() -- offseted channels
							UV_channels[1] = (1 + (mod (illum_ch+1) 3)) as integer
							UV_channels[2] = illum_ch
							UV_channels[3] = (1 + (mod (illum_ch+3) 3)) as integer
						)
						else
							UV_channels = #(1,2,3)
						--messageBox ( "Illum channel: " + (illum_ch as string) + "\nUV channels:\n" + (UV_channels as string) )

						if( diff1_ch > 0 ) then WriteByte file_scx ((finditem UV_channels diff1_ch)-1) -- diffuse 1 UV channel
						else WriteByte file_scx 0x00

						WriteByte file_scx 0x00 #unsigned -- unknown

						if( diff2_ch > 0 ) then WriteByte file_scx ((finditem UV_channels diff2_ch)-1) -- diffuse 2 UV channel
						else WriteByte file_scx 0x00

						if( layer3_ch > 0 ) then WriteByte file_scx ((finditem UV_channels layer3_ch)-1) -- layer 3 UV channel
						else WriteByte file_scx 0x00

						if( bump_ch > 0 ) then WriteByte file_scx ((finditem UV_channels bump_ch)-1) -- bump UV channel
						else WriteByte file_scx 0x00
						
						WriteByte file_scx 0x00 #unsigned -- unknown
						WriteByte file_scx 0x00 #unsigned -- unknown
						WriteByte file_scx 0x00 #unsigned -- unknown
						WriteByte file_scx 0xFF #unsigned -- unknown
						WriteByte file_scx 0xFF #unsigned -- unknown

						WriteLong file_scx 0x00000000 #unsigned -- unknown
						WriteLong file_scx 0x00000000 #unsigned -- unknown
						WriteLong file_scx 0x00000000 #unsigned -- unknown
							
					-- illumination color
						if( isKindOf mesh_mat SLRR_Material ) then
						(
							if( not (isKindOf mesh_mat.delegate.selfIllumMap.map1 bitmaptexture) ) then
							(
								if( mesh_mat.delegate.useSelfIllumColor ) then
								(
									WriteFloat file_scx (mesh_mat.delegate.selfIllumColor.r/255.0) -- illumination r
									WriteFloat file_scx (mesh_mat.delegate.selfIllumColor.g/255.0) -- illumination g
									WriteFloat file_scx (mesh_mat.delegate.selfIllumColor.b/255.0) -- illumination b
								)
								else
								(
									WriteFloat file_scx (mesh_mat.delegate.selfIllumAmount/100.0) -- illumination r
									WriteFloat file_scx (mesh_mat.delegate.selfIllumAmount/100.0) -- illumination g
									WriteFloat file_scx (mesh_mat.delegate.selfIllumAmount/100.0) -- illumination b
								)
							)
							else
							(
								WriteLong file_scx 0x00000000 -- illumination r
								WriteLong file_scx 0x00000000 -- illumination g
								WriteLong file_scx 0x00000000 -- illumination b
							)
						)
						else -- standard material
						(
							if( not (isKindOf mesh_mat.selfIllumMap bitmaptexture) ) then
							(
								if( mesh_mat.useSelfIllumColor ) then
								(
									WriteFloat file_scx (mesh_mat.selfIllumColor.r/255.0) -- illumination r
									WriteFloat file_scx (mesh_mat.selfIllumColor.g/255.0) -- illumination g
									WriteFloat file_scx (mesh_mat.selfIllumColor.b/255.0) -- illumination b
								)
								else
								(
									WriteFloat file_scx (mesh_mat.selfIllumAmount/100.0) -- illumination r
									WriteFloat file_scx (mesh_mat.selfIllumAmount/100.0) -- illumination g
									WriteFloat file_scx (mesh_mat.selfIllumAmount/100.0) -- illumination b
								)
							)
							else
							(
								WriteLong file_scx 0x00000000 -- illumination r
								WriteLong file_scx 0x00000000 -- illumination g
								WriteLong file_scx 0x00000000 -- illumination b
							)
						)
						
					-- material name (always takes 32 chars)
						local m_name
						if( (isKindOf mesh_mat SLRR_Material) and (mesh_mat.track_render != mesh_mat.track_physic) ) then
						(
							if( mesh_mat.track_render ) then
								m_name = "_R" -- name prefix for render only material
							else
								m_name = "_P" -- name prefix for physic only material

							m_name += (substring mesh_mat.name 1 30) -- append material name
						)
						else
							m_name = (substring mesh_mat.name 1 32) -- get max 32 chars from name
							
						WriteString file_scx m_name
						fseek file_scx -1 #seek_cur -- back to string terminating 0 char

						for i=m_name.count to 32-1 do
							WriteByte file_scx 0x00 #unsigned
					)
					else -- physic, the game uses a blank material (same 136 bytes layout)
					(
						WriteLong file_scx 0x88 #unsigned -- extended material size

						WriteFloat file_scx 1.0 -- diffuse color red
						WriteFloat file_scx 1.0 -- diffuse color green
						WriteFloat file_scx 1.0 -- diffuse color blue
						WriteFloat file_scx 1.0 -- opacity
						WriteFloat file_scx 1.0 -- specular color red
						WriteFloat file_scx 1.0 -- specular color green
						WriteFloat file_scx 1.0 -- specular color blue
						WriteFloat file_scx 0.0 -- specular level
						WriteFloat file_scx 0.0 -- glossiness amount

						WriteLong file_scx 0x00000000 #unsigned -- flags

						for i=1 to 7 do -- diffuse 1, bump, specular, reflection, diffuse 2, unknown, light map
						(
							WriteByte file_scx 0xFF #unsigned -- no map
							WriteByte file_scx 0xFF #unsigned -- map mode
						)

						WriteByte file_scx 0x00 #unsigned -- unknown map 2
						WriteByte file_scx 0x00 #unsigned -- unknown map 2 mode

						WriteLong file_scx 0x00000040 #unsigned -- one vertex size

						for i=1 to 3 do -- unknown maps 3, 4, 5
						(
							WriteByte file_scx 0xFF #unsigned -- no map
							WriteByte file_scx 0xFF #unsigned -- map mode
						)

						for i=1 to 8 do
							WriteByte file_scx 0x00 #unsigned -- uv channels + unknown

						WriteByte file_scx 0xFF #unsigned -- unknown
						WriteByte file_scx 0xFF #unsigned -- unknown

						WriteLong file_scx 0x00000000 #unsigned -- unknown
						WriteLong file_scx 0x00000000 #unsigned -- unknown
						WriteLong file_scx 0x00000000 #unsigned -- unknown

						WriteLong file_scx 0x00000000 -- illumination r
						WriteLong file_scx 0x00000000 -- illumination g
						WriteLong file_scx 0x00000000 -- illumination b

						for i=1 to 32 do
							WriteByte file_scx 0x00 #unsigned -- empty material name
					)

					print( "Material done: " + (((timeStamp()-t)/1000.0) as string) + "s" )
					t = timeStamp()
						
				-- W R I T E  M E S H --
				-- vertices
					progress_curr_name (progress_mat_count + "Saving vertices...")

					WriteLong file_scx MatVert[matID].count #unsigned -- vertices count

					local val
					progress_curr_range MatVert[matID].count -- progress dialog
					for i=1 to MatVert[matID].count do
					(
					-- vertex cordinates
						val = getVert meshObj (MatVert[matID][i])
						WriteFloat file_scx val.x
						WriteFloat file_scx val.z
						WriteFloat file_scx (-val.y)
							
					-- normal
						val = mesh_normals[MatVert[matID][i]]

						if shading == 4  then -- sprite type shading
						(
							val  *= 0.5
							val.z = 1.0
						)

						if shading == 3  then -- track type shading
						(
							val  *= 0.5
							val.z += 0.5
						)

						val *= normals_mul -- material shading multiplier

						if( shading > 1 and (mesh_UVmaps[5] != undefined) ) then -- ambient occlusion or track shadows (vertex illumination channel)
							val *= mesh_UVmaps[5][MatVert[matID][i]]

						WriteFloat file_scx val.x
						WriteFloat file_scx val.z
						WriteFloat file_scx (-val.y)
							
					-- UV channel 1
						if( mesh_UVmaps[UV_channels[1]+1] != undefined ) then -- is this channel supported?
						(
							val = getAverageMapVert meshObj UV_channels[1] mesh_UVmaps[UV_channels[1]+1][MatVert[matID][i]]
							--val = (avg3 mesh_UVmaps[UV_channels[1]+1][MatVert[matID][i]])
							WriteFloat file_scx val.x
							WriteFloat file_scx (-val.y)
						)
						else
						(
							WriteFloat file_scx 0 -- no UV
							WriteFloat file_scx 0 -- no UV
						)
							
					-- UV channel 2
						if( mesh_UVmaps[UV_channels[2]+1] != undefined ) then -- is this channel supported?
						(
							val = getAverageMapVert meshObj UV_channels[2] mesh_UVmaps[UV_channels[2]+1][MatVert[matID][i]]
							--val = (avg3 mesh_UVmaps[UV_channels[2]+1][MatVert[matID][i]])
							WriteFloat file_scx val.x
							WriteFloat file_scx (-val.y)
						)
						else
						(
							WriteFloat file_scx 0 -- no UV
							WriteFloat file_scx 0 -- no UV
						)
							
					-- Vertex color
						if( mesh_UVmaps[1] != undefined ) then -- is v.color supported?
						(
							val = getAverageMapVert meshObj 0 mesh_UVmaps[1][MatVert[matID][i]]
							--val = (avg3 mesh_UVmaps[1][MatVert[matID][i]])
							WriteByte file_scx (val.z*255.0) -- blue
							WriteByte file_scx (val.y*255.0) -- green
							WriteByte file_scx (val.x*255.0) -- red
							WriteByte file_scx 0xFF -- alpha (unused)
						)
						else
						(
							-- white
							WriteByte file_scx 0xFF -- blue
							WriteByte file_scx 0xFF -- green
							WriteByte file_scx 0xFF -- red
							WriteByte file_scx 0xFF -- alpha (unused)
						)
						
					-- 2x unknown
						WriteLong file_scx 0
						WriteLong file_scx 0

					-- UV channel 3
						if( mesh_UVmaps[UV_channels[3]+1] != undefined ) then -- is this channel supported?
						(
							val = getAverageMapVert meshObj UV_channels[3] mesh_UVmaps[UV_channels[3]+1][MatVert[matID][i]]
							--val = (avg3 mesh_UVmaps[UV_channels[3]+1][MatVert[matID][i]])
							WriteFloat file_scx val.x
							WriteFloat file_scx (-val.y)
						)
						else
						(
							WriteFloat file_scx 0 -- no UV
							WriteFloat file_scx 0 -- no UV
						)
							
					
						WriteLong file_scx 0 -- unknown

						--total_vert_count += 1
					-- vertex end
						
						progress_curr i -- progress dialog
					)

					print( "Verts done: " + (((timeStamp()-t)/1000.0) as string) + "s" )
					t = timeStamp()

				-- faces
					progress_curr_name (progress_mat_count + "Saving faces...")
					WriteLong file_scx MatFace[matID].count #unsigned -- faces count
						

					-- mesh vertice index to material group index (way faster than finditem)
					local vLocal = #()
					if( meshObj.numverts > 0 ) then vLocal[meshObj.numverts] = 0
					for i=1 to MatVert[matID].count do vLocal[MatVert[matID][i]] = i

					progress_curr_range MatFace[matID].count -- progress dialog
					for i=1 to MatFace[matID].count do
					(
						val = getFace meshObj (MatFace[matID][i])
						WriteLong file_scx (vLocal[(val.x as integer)]-1) #unsigned
						WriteLong file_scx (vLocal[(val.y as integer)]-1) #unsigned
						WriteLong file_scx (vLocal[(val.z as integer)]-1) #unsigned

						progress_curr i -- progress dialog
					)

					print( "Faces done: " + (((timeStamp()-t)/1000.0) as string) + "s" )
					t = timeStamp()
				) -- main for end
						
				WriteLong file_scx 0 -- file end

				/*debug_mesh = Box name:"SCX export optimize debug"
				convertToMesh debug_mesh
				setMesh debug_mesh (copy mesh)
				
				for i=1 to debug_mesh.numverts do
					setNormal debug_mesh i mesh_normals[i]--*/

				delete meshObj -- delete copy of exported object
			)
			else
			if( version == 4 ) then -- scx version v4
			(
				WriteLong file_scx 4 #unsigned -- file version

			-- split material groups into blocks small enough for 16bit face indices
				progress_curr_name "Preparing mesh blocks..."

				local blockMat = #() -- material group of every block
				local blockVert = #() -- mesh vertices of every block
				local blockFace = #() -- mesh faces of every block

				local vBlock = #() -- block of every mesh vertice
				local vLocal = #() -- index of vertice inside its block

				if( meshObj.numverts > 0 ) then -- init known size arrays
				(
					vBlock[meshObj.numverts] = 0
					vLocal[meshObj.numverts] = 0
				)

				local blockID = 0
				local face, fv, newVerts, curVerts, curFaces

				for matID=1 to MaterialID.count do
				(
					blockID += 1
					curVerts = #(); curFaces = #()

					for i=1 to MatFace[matID].count do
					(
						face = getFace meshObj MatFace[matID][i]
						fv = #( (face.x as integer), (face.y as integer), (face.z as integer) )

						-- how many vertices would this face add to current block?
						newVerts = 0
						for j=1 to 3 where ((vBlock[fv[j]] != blockID) and ((findItem fv fv[j]) == j)) do
							newVerts += 1

						if( ((curVerts.count + newVerts) > SLR_SCX_V4_VERT_LIMIT) and (curFaces.count > 0) ) then
						( -- close current block and start a new one
							append blockMat matID
							append blockVert curVerts
							append blockFace curFaces

							blockID += 1
							curVerts = #(); curFaces = #()
						)

						for j=1 to 3 where (vBlock[fv[j]] != blockID) do
						(
							vBlock[fv[j]] = blockID
							append curVerts fv[j]
						)

						append curFaces MatFace[matID][i]
					)

					if( curFaces.count > 0 ) then
					(
						append blockMat matID
						append blockVert curVerts
						append blockFace curFaces
					)
				)

			-- file header: table of 4 entries (material, hard surface, vertices, faces) peer block
				WriteLong file_scx (blockMat.count*4) #unsigned -- entries count

				local tablePos = ftell file_scx
				for i=1 to (blockMat.count*4) do -- placeholder, offsets are patched after writing blocks
				(
					WriteLong file_scx 0 #unsigned
					WriteLong file_scx 0 #unsigned
				)

				local blockOffset = #() -- file offset of every written entry
				local hasVColor = (not physic) and (mesh_UVmaps[1] != undefined) and (hasVertexColors meshObj)

				local matInfo, maxUV, normals_mul, vType, vSize, verts, faces, val
				local mesh_mat, progress_mat_count

				for b=1 to blockMat.count do
				(
					t = timeStamp()
					verts = blockVert[b]
					faces = blockFace[b]
					progress_mat_count = ("Block " + (b as string) + "/" + (blockMat.count as string) + " - ")

					for i=1 to verts.count do
						vLocal[verts[i]] = i -- local indices of this block

				-- material
					progress_curr_name (progress_mat_count + "Saving material properties...")

					mesh_mat = pickExportMaterial object MaterialID[blockMat[b]] physic

					append blockOffset (ftell file_scx)
					matInfo = writeV4Material file_scx mesh_mat map_names physic
					maxUV = matInfo[1]
					normals_mul = matInfo[2]

				-- keep the UV channels that hold coordinates for this block even when no
				-- material map uses them, so nothing is lost (v3 meshes carry up to 3)
					if( not physic ) then
					(
						local blockMaxUV = 0, con

						for ch=3 to 1 by -1 while (ch > blockMaxUV) do
						(
							if( (ch < norChannel) and (mesh_UVmaps[ch+1] != undefined) ) then
							(
								con = true

								for i=1 to verts.count while con do
								(
									val = getAverageMapVert meshObj ch mesh_UVmaps[ch+1][verts[i]]

									if( (val.x != 0.0) or (val.y != 0.0) ) then
									(
										blockMaxUV = ch
										con = false
									)
								)
							)
						)

						if( maxUV < blockMaxUV ) then maxUV = blockMaxUV
					)

				-- hard surface definition (mesh without bones)
					append blockOffset (ftell file_scx)
					WriteLong file_scx 1 #unsigned -- block type
					WriteLong file_scx 12 #unsigned -- block size
					WriteLong file_scx 0 #unsigned

					print( "Material done: " + (((timeStamp()-t)/1000.0) as string) + "s" )
					t = timeStamp()

				-- W R I T E  M E S H --
				-- vertices
					progress_curr_name (progress_mat_count + "Saving vertices...")

					vType = 0x00001 + 0x00040 -- position + normal
					vSize = 12 + 12

					if( hasVColor ) then ( vType += 0x00100; vSize += 4 ) -- vertex color
					if( maxUV >= 1 ) then ( vType += 0x00200; vSize += 8 ) -- UV channel 1
					if( maxUV >= 2 ) then ( vType += 0x00400; vSize += 8 ) -- UV channel 2
					if( maxUV >= 3 ) then ( vType += 0x00800; vSize += 8 ) -- UV channel 3

					append blockOffset (ftell file_scx)
					WriteLong file_scx 4 #unsigned -- block type
					WriteLong file_scx (16 + verts.count*vSize) #unsigned -- block size
					WriteLong file_scx verts.count #unsigned -- vertices count
					WriteLong file_scx vType #unsigned -- fields stored in every vertice

					progress_curr_range verts.count -- progress dialog
					for i=1 to verts.count do
					(
					-- vertex cordinates
						val = getVert meshObj verts[i]
						WriteFloat file_scx val.x
						WriteFloat file_scx val.z
						WriteFloat file_scx (-val.y)

					-- normal (its length is the vertex shading)
						val = mesh_normals[verts[i]]

						if shading == 4  then -- sprite type shading
						(
							val  *= 0.5
							val.z = 1.0
						)

						if shading == 3  then -- track type shading
						(
							val  *= 0.5
							val.z += 0.5
						)

						val *= normals_mul -- material shading multiplier

						if( shading > 1 and (mesh_UVmaps[5] != undefined) ) then -- ambient occlusion or track shadows (vertex illumination channel)
							val *= mesh_UVmaps[5][verts[i]]

						WriteFloat file_scx val.x
						WriteFloat file_scx val.z
						WriteFloat file_scx (-val.y)

					-- vertex color
						if( hasVColor ) then
						(
							val = getAverageMapVert meshObj 0 mesh_UVmaps[1][verts[i]]
							WriteByte file_scx (clampByte (val.z*255.0)) #unsigned -- blue
							WriteByte file_scx (clampByte (val.y*255.0)) #unsigned -- green
							WriteByte file_scx (clampByte (val.x*255.0)) #unsigned -- red
							WriteByte file_scx 0xFF #unsigned -- alpha (unused)
						)

					-- UV channels
						for ch=1 to maxUV do
						(
							if( mesh_UVmaps[ch+1] != undefined ) then -- is this channel supported?
							(
								val = getAverageMapVert meshObj ch mesh_UVmaps[ch+1][verts[i]]
								WriteFloat file_scx val.x
								WriteFloat file_scx (-val.y)
							)
							else
							(
								WriteFloat file_scx 0 -- no UV
								WriteFloat file_scx 0 -- no UV
							)
						)

						progress_curr i -- progress dialog
					)

					print( "Verts done: " + (((timeStamp()-t)/1000.0) as string) + "s" )
					t = timeStamp()

				-- faces
					progress_curr_name (progress_mat_count + "Saving faces...")

					append blockOffset (ftell file_scx)
					WriteLong file_scx 5 #unsigned -- block type
					WriteLong file_scx (12 + faces.count*6) #unsigned -- block size
					WriteLong file_scx (faces.count*3) #unsigned -- indices count

					progress_curr_range faces.count -- progress dialog
					for i=1 to faces.count do
					(
						val = getFace meshObj faces[i]
						WriteShort file_scx (vLocal[(val.x as integer)]-1) #unsigned
						WriteShort file_scx (vLocal[(val.y as integer)]-1) #unsigned
						WriteShort file_scx (vLocal[(val.z as integer)]-1) #unsigned

						progress_curr i -- progress dialog
					)

					print( "Faces done: " + (((timeStamp()-t)/1000.0) as string) + "s" )
				) -- main for end

			-- patch the header table with real block offsets
				local endPos = ftell file_scx
				fseek file_scx tablePos #seek_set

				for b=1 to blockMat.count do
				(
					WriteLong file_scx 0 #unsigned; WriteLong file_scx blockOffset[(b-1)*4+1] #unsigned -- material
					WriteLong file_scx 1 #unsigned; WriteLong file_scx blockOffset[(b-1)*4+2] #unsigned -- hard surface
					WriteLong file_scx 4 #unsigned; WriteLong file_scx blockOffset[(b-1)*4+3] #unsigned -- vertices
					WriteLong file_scx 5 #unsigned; WriteLong file_scx blockOffset[(b-1)*4+4] #unsigned -- faces
				)

				fseek file_scx endPos #seek_set

				delete meshObj -- delete copy of exported object
			)
			else
			(
				messageBox( "Export SCX version " + (version as string) + " is no supported" ) title:"Error" beep:true
			)

			delete object -- delete copy of exported object

			-- create tex file with textures order
			file_tex = fopen (replace file_name (file_name.count-2) 3 "tex") "wb" -- write binnary destroy existing
			if( file_tex != undefined ) then
			(
				for i=1 to map_names.count do
				(
					WriteString file_tex map_names[i]
					fseek file_tex -1 #seek_cur -- back to string terminating 0 char

					-- jump to next line
					WriteByte file_tex 0x0D
					WriteByte file_tex 0x0A
				)

				fclose file_tex
			)
			else
				messagebox ("Could save to " + (filenameFromPath (replace file_name (file_name.count-2) 3 "tex")) + " file  ") title:"Error" beep:true
		)
		else
			messagebox ("Could save to " + (filenameFromPath file_name) + " file  ") title:"Error" beep:true

		fclose file_scx
		MaxRedraw 1 -- enable interface redraw
	) -- end undo off

	gc(); -- clean rubish
)

if( SLR_SCX_Export != undefined) then -- recreate if window already exists
	DestroyDialog SLR_SCX_Export

rollout SLR_SCX_Export "SCX Export v0.6"
(
	local update_enabled = true

	label ver_lbl "SCX version:" pos:[9,8]
	radiobuttons ver_rbut pos:[80,7] labels:#("v3  ", "v4")

	checkbox optimize_chck "Optimization" pos:[10,33]
	dropdownList optimize_ddl pos:[92,30] width:60 items:#("Good", "Best")

	checkbox pivot_chck "Center by pivot" pos:[10,57]

	checkbox physic_chck "Physic mesh" pos:[10,75]
	checkbox separate_chck "Separate objects" pos:[10,93]

	label shading_lbl "Shading" pos:[10,117]
	dropdownList shading_ddl pos:[55,114] width:97 items:#("Standard", "Shadows, A.Occluson", "Track+Shadows", "Sprite+Shadows")

	--checkbox password_chck "Lock by password:" pos:[10,141]
	--editText password_btn_edt pos:[6,158] width:145 height:15

	imgTag separator pos:[10,141] width:141 height:1 style:#bmp_stretch bitmap:(bitmap 1 1 color:(color 110 110 110)) -- separate horizontal line

	button export_btn "EXPORT SELECTED" pos:[10,148] width:141 height:20 enabled:false
	progressBar progress_bar "" pos:[10,190] width:141 height:20 visible:false

	function update = 
	(
		if( update_enabled ) then
		(
			ver_rbut.state = SLR_SCX_EXPORT_VERSION - 2
			shading_ddl.selection = SLR_SCX_EXPORT_SHADING
			optimize_chck.checked = SLR_SCX_EXPORT_OPTIMIZE
			optimize_ddl.enabled = SLR_SCX_EXPORT_OPTIMIZE
			optimize_ddl.selection = SLR_SCX_EXPORT_OPTIMIZE_TYPE
			physic_chck.checked = SLR_SCX_EXPORT_PHYSIC
			pivot_chck.checked = SLR_SCX_EXPORT_PIVOT
			separate_chck.checked = SLR_SCX_EXPORT_SEPARATE
			--password_chck.checked = SLR_SCX_EXPORT_PASSWORD_ENABLED
			--password_btn_edt.enabled = SLR_SCX_EXPORT_PASSWORD_ENABLED
			--password_btn_edt.text = SLR_SCX_EXPORT_PASSWORD

			objects = collectSelection()

			if( objects.count > 0 ) then
			(
				export_btn.enabled = true

				if( objects.count > 1 ) then
					separate_chck.enabled = true
				else
					separate_chck.enabled = false
			)
			else
			(
				export_btn.enabled = false
				separate_chck.enabled = false
			)
		)
	)

	function disable val = 
	(
		if( val == true )then
		(
			update_enabled = false

			ver_lbl.enabled = false
			ver_rbut.enabled = false
			optimize_chck.enabled = false
			shading_ddl.enabled = false
			physic_chck.enabled = false
			pivot_chck.enabled = false
			separate_chck.enabled = false
			--password_chck.enabled = false
			--password_btn_edt.enabled = false
			export_btn.visible = false
			progress_bar.visible = true
			
			--SetDialogPos SLR_SCX_Export (GetDialogPos SLR_SCX_Export) -- redraw dialog
		)
		else
		(
			ver_lbl.enabled = true
			ver_rbut.enabled = true
			optimize_chck.enabled = true
			shading_ddl.enabled = true
			physic_chck.enabled = true
			pivot_chck.enabled = true
			--password_chck.enabled = true
			--password_btn_edt.enabled = SLR_SCX_EXPORT_PASSWORD_ENABLED
			export_btn.visible = true
			progress_bar.visible = false

			update_enabled = true
			update() -- rest of buttons
		)
	)

	-- controls events
	on ver_rbut changed arg do SLR_SCX_EXPORT_VERSION = arg+2
	on shading_ddl selected  arg do SLR_SCX_EXPORT_SHADING = arg
	on optimize_ddl selected  arg do SLR_SCX_EXPORT_OPTIMIZE_TYPE = arg
	on physic_chck changed arg do SLR_SCX_EXPORT_PHYSIC = arg
	on pivot_chck changed arg do SLR_SCX_EXPORT_PIVOT = arg
	on separate_chck changed arg do SLR_SCX_EXPORT_SEPARATE = arg

	on optimize_chck changed arg do 
	(
		SLR_SCX_EXPORT_OPTIMIZE = arg
		optimize_ddl.enabled = arg
	)
	
	on password_chck changed arg do
	(
		SLR_SCX_EXPORT_PASSWORD_ENABLED = arg
		password_btn_edt.enabled = arg
	)
	
	on password_btn_edt changed arg do 
	(
		if( arg.count > 32 ) then
		(
			SLR_SCX_EXPORT_PASSWORD = (substring arg 1 32) -- get 32 chars from entered password
			password_btn_edt.text = SLR_SCX_EXPORT_PASSWORD
			messageBox "Password can be max 32 characters length!" title:"Warning" beep:true
		)
		else
			SLR_SCX_EXPORT_PASSWORD = arg
	)

	on export_btn pressed do
	(
		local file_name = (getSaveFileName caption:"Save mesh" types:"Invictus meshes (*.scx)|*.scx")

		if( file_name != undefined ) then
		(
			local t = timeStamp()

			undo off
			(
				MaxRedraw 0 -- disable interface redraw
				disable true -- disable export dialog
				objects = collectSelection()
				CreateProgressDialog "SCX Export" type:2 refresh_freq:25 -- create progress dialog with two bars
				
				local optType = if SLR_SCX_EXPORT_OPTIMIZE then SLR_SCX_EXPORT_OPTIMIZE_TYPE else 0

				if( objects.count > 1 ) then
				(
					if( separate_chck.checked == true ) then
					(
						local obj_name, tokens, path = getFilenamePath file_name

						for i=1 to objects.count do
						(
							tokens = filterString objects[i].name " \\ \/ : * ? \" < > |" -- frobiden characters + space too
							obj_name = ""
							for j=1 to tokens.count do -- stick name back together
							(
								obj_name +=  tokens[j]

								if( j < tokens.count ) then
									obj_name += "_" -- replace frobiden chars with _
							)

							exportSCX objects[i] (path+obj_name+".scx") version:SLR_SCX_EXPORT_VERSION optimize:optType shading:SLR_SCX_EXPORT_SHADING physic:SLR_SCX_EXPORT_PHYSIC pivot:SLR_SCX_EXPORT_PIVOT
						)
					)
					else
					(
						-- group all selected objects into one
						local object, union = copy objects[1]
						convertToMesh union

						for i=2 to objects.count do
						(
							object = copy objects[i]
							attach union object -- attach keeps the world position of both objects
						)

						exportSCX union file_name version:SLR_SCX_EXPORT_VERSION optimize:optType shading:SLR_SCX_EXPORT_SHADING physic:SLR_SCX_EXPORT_PHYSIC pivot:SLR_SCX_EXPORT_PIVOT

						delete union
					)
				)
				else
				if( objects.count == 1 ) then
					exportSCX objects[1] file_name version:SLR_SCX_EXPORT_VERSION optimize:optType shading:SLR_SCX_EXPORT_SHADING physic:SLR_SCX_EXPORT_PHYSIC pivot:SLR_SCX_EXPORT_PIVOT

				ProgressDialog.finished()
				select( objects ) -- restore old selection (geometry only)
				disable false -- enable export dialog
				MaxRedraw 1 -- enable interface redraw
			) -- undo off end

			print( "Export time: " + (((timeStamp()-t)/1000.0) as string) + "s" )
		) 
	)
)

CreateDialog SLR_SCX_Export width:161 height:178
SLR_SCX_Export.update()

-- register event callbacks
callbacks.removeScripts #selectionSetChanged id:#SLR_SCX_Export_sel_upd
callbacks.addScript #selectionSetChanged "SLR_SCX_Export.update()" id:#SLR_SCX_Export_sel_upd
