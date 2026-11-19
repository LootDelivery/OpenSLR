-- by Miran (Miran4@op.pl)
-- 17.01.2011

if SLRR_Native_Objects_buttons_count == undefined then
(
	global SLRR_Native_Objects_buttons_count = 10
	global SLRR_Native_Objects_buttons = openBitmap ((GetDir #scripts)+"\\SLR_Tools\\SnapToGround.bmp")
	global SLRR_Native_Objects_buttonsA = openBitmap ((GetDir #scripts)+"\\SLR_Tools\\SnapToGround_A.bmp")

	global SLRR_Native_Objects_rndPosX = 10.0
	global SLRR_Native_Objects_rndPosY = 10.0
	global SLRR_Native_Objects_rndPosZ = 0.0

	global SLRR_Native_Objects_rndOriX = 2.0
	global SLRR_Native_Objects_rndOriY = 2.0
	global SLRR_Native_Objects_rndOriZ = 360.0

	global SLRR_Native_Objects_groundNodes = #()
	global SLRR_Native_Objects_snapMode = 2
	global SLRR_Native_Objects_snapOffset = -2.0
)

-- native object properties dialog
if SLRR_Native_Objects_properties == undefined then
(
	rollout SLRR_Native_Objects_properties "SLRR Native Object properties"
	(
		label name_lbl pos:[10,10]  align:#left
		
		label lib_name_lbl  "RPK patch and name:" pos:[10,30] align:#left
		label lib_id_lbl "Object ID:" pos:[230,30] align:#left
		
		edittext lib_name_edt fieldWidth:215 pos:[5,45]
		edittext lib_id_edt fieldWidth:60 pos:[225,45]
		
		button addRemData_btn "Add native object info" pos:[10,75] width:170
		
		function update =
		(
			if selection.count > 0 then
			(
				name_lbl.text = "Object: " + selection[1].name
			
				--local lib = getUserProp selection[1] "SLRR_native_object_RPK"
				--local id = getUserProp selection[1] "SLRR_native_object_ID"
				
				local lib = getUserProp selection[1] "SLRR_native_object_RPK"
				local id = getUserProp selection[1] "SLRR_native_object_ID"
				
				if lib == undefined or lib == "" or id == undefined or id == "" then
				(
					lib_name_lbl.enabled = false
					lib_id_lbl.enabled = false
					lib_name_edt.enabled = false
					lib_name_edt.text = ""
					lib_id_edt.enabled = false
					lib_id_edt.text = ""
					addRemData_btn.enabled =true
					addRemData_btn.text = "Add native object info"
				)
				else
				(
					lib_name_lbl.enabled = true
					lib_id_lbl.enabled = true
					lib_name_edt.enabled = true
					lib_name_edt.text = substring lib 2 (lib.count-2)
					lib_id_edt.enabled = true
					lib_id_edt.text = substring id 2 (id.count-2)
					addRemData_btn.enabled = true
					addRemData_btn.text = "Remove native object info"
				)
				
				if SLRR_Native_Objects != undefined then SLRR_Native_Objects.update()
			)
			else
			(
				name_lbl.text = "Object: [NOT SELECTED]"
				lib_name_lbl.enabled = false
				lib_id_lbl.enabled = false
				lib_name_edt.enabled = false
				lib_name_edt.text = ""
				lib_id_edt.enabled = false
				lib_id_edt.text = ""
				addRemData_btn.enabled = false
			)
		)
		
		on lib_name_edt changed arg do setUserProp selection[1] "SLRR_native_object_RPK" ("\""+arg+"\"")
		
		on lib_id_edt changed arg do setUserProp selection[1] "SLRR_native_object_ID" ("\""+arg+"\"")
		
		on addRemData_btn pressed do
		(
			local lib = getUserProp selection[1] "SLRR_native_object_RPK"
			local id = getUserProp selection[1] "SLRR_native_object_ID"
				
			if lib == undefined or lib == "" or id == undefined or id == "" then
			(
				setUserProp selection[1] "SLRR_native_object_RPK" "\"maps\\example_objects\\\""
				setUserProp selection[1] "SLRR_native_object_ID" "\"02E1\""
			)
			else
			(
				if queryBox "Do you really want to remove?\n    " title:(selection[1].name+" native object info         ") beep:true then
				(
					setUserProp selection[1] "SLRR_native_object_RPK" ""
					setUserProp selection[1] "SLRR_native_object_ID" ""
				)
			)
			
			update()
		)
		
		on SLRR_Native_Objects_properties open do
			update()
	)
	-- register event callbacks
	callbacks.removeScripts #selectionSetChanged id:#SLRR_Native_Objects_properties_sel_upd
	callbacks.addScript #selectionSetChanged "SLRR_Native_Objects_properties.update()" id:#SLRR_Native_Objects_properties_sel_upd
)

-- native objects dialog
if SLRR_Native_Objects == undefined then
(
	rollout SLRR_Native_Objects "SLRR Native Objects"
	(
		GroupBox grp1 "Randomize" pos:[5,2] width:330 height:72

		button rndPos_btn "Position ±" pos:[15,18] width:100 height:16
		spinner rndPosX_spn "x:" pos:[120,18] fieldwidth:45 type:#worldunits range:[0,1000,0] scale:5
		spinner rndPosY_spn "y:" pos:[190,18] fieldwidth:45 type:#worldunits range:[0,1000,0] scale:5
		spinner rndPosZ_spn "z:" pos:[260,18] fieldwidth:45 type:#worldunits range:[0,1000,0] scale:5

		button rndOri_btn "Rotation ±" pos:[15,48] width:100 height:16
		spinner rndOriX_spn "x:" pos:[120,48] fieldwidth:45 type:#float range:[0,360,0] scale:10
		spinner rndOriY_spn "y:" pos:[190,48] fieldwidth:45 type:#float range:[0,360,0] scale:10
		spinner rndOriZ_spn "z:" pos:[260,48] fieldwidth:45 type:#float range:[0,360,0] scale:10

		
		GroupBox grp2 "Snap to ground" pos:[5,76] width:330 height:107
		button gndPick_btn "Take selection as ground" pos:[15,92] width:186 height:20
		button gndShow_btn "Ground nodes: 0" pos:[210,92] width:115 height:20

		label placement_lbl "Object place by:" pos:[15,120] width:80
		radiobuttons placement_rbut pos:[100,120] labels:#("Pivot ", "Bottom")
		spinner placement_spn "offset:" pos:[239,121] fieldwidth:45 type:#worldunits range:[-1000,1000,-2] scale:1

		label snapTo_lbl "Snap to ground:" pos:[15,152] width:80
		button snapTo1_btn pos:[95,145] width:42 height:28 images:#( SLRR_Native_Objects_buttons, SLRR_Native_Objects_buttonsA, SLRR_Native_Objects_buttons_count, 1, 1, 2, 2 )
		button snapTo2_btn pos:[142,145] width:42 height:28 images:#( SLRR_Native_Objects_buttons, SLRR_Native_Objects_buttonsA, SLRR_Native_Objects_buttons_count, 3, 3, 4, 4 )
		button snapTo3_btn pos:[188,145] width:42 height:28 images:#( SLRR_Native_Objects_buttons, SLRR_Native_Objects_buttonsA, SLRR_Native_Objects_buttons_count, 5, 5, 6, 6 )
		button snapTo4_btn pos:[236,145] width:42 height:28 images:#( SLRR_Native_Objects_buttons, SLRR_Native_Objects_buttonsA, SLRR_Native_Objects_buttons_count, 7, 7, 8, 8 )
		button snapTo5_btn pos:[283,145] width:42 height:28 images:#( SLRR_Native_Objects_buttons, SLRR_Native_Objects_buttonsA, SLRR_Native_Objects_buttons_count, 9, 9, 10, 10 )

		button objExport_btn "EXPORT NATIVE OBJECTS [0]" pos:[5,190] width:271 height:20
		button objEdit_btn "EDIT" pos:[285,190] width:50 height:20

		function formatFloatString f =
		(
			f *= 10000.0
			f = floor f
			f /= 10000.0

			f = (f as string)
			for i=1 to (4-(f.count-(findString f "."))) do -- fill missing zeros
				append f "0"

			return f
		)

		function GetSnapHeight object point = 
		( 
			local testRay = ray point [0,0,-1] 
			testRay = intersectRay object testRay
			
			if( testRay != undefined ) then
				return #( testRay.pos, testRay.dir )
			else 
				return undefined
		) 

		function snapToGround normalMultiplier:1.0 =
		(
			if (SLRR_Native_Objects_groundNodes.count > 0 ) and (selection.count > 0) then
			(
				undo "snap to ground" on
				(
					local posObj, posSnap, posTop, rotTop
					for obj in selection do
					(
						posObj = obj.pos
						posObj.z = obj.max.z

						posTop = undefined
						for gnd in SLRR_Native_Objects_groundNodes where isValidNode(maxOps.getNodeByHandle gnd) do
						(
							posSnap = GetSnapHeight (maxOps.getNodeByHandle gnd) posObj

							if posSnap != undefined then
							(
								if (posTop == undefined) or (posTop[1].z < posSnap[1].z) then
									posTop = posSnap
							)
						)

						if posTop != undefined then
						(
							obj.pos = posTop[1] -- snap to ground
							
							if SLRR_Native_Objects_snapMode == 2 then -- snap object to bottom
								move obj (point3 0 0 (obj.pos.z-obj.min.z))

							move obj (point3 0 0 SLRR_Native_Objects_snapOffset) -- snap offset

							if normalMultiplier > 0.0 then -- rotate
							(
								rotTop = (inverse (MatrixFromNormal posTop[2])) as EulerAngles
								rotTop.x *= normalMultiplier
								rotTop.y *= normalMultiplier
								rotTop.z = 0

								in coordsys (transMatrix posTop[1]) rotate obj (rotTop as Quat)
							)
						)
					)
				)
			)
		)

		on rndPos_btn pressed do
		(
			undo "randomize position" on
			(
				local offset
				for obj in selection do
				(
					offset = (point3 0 0 0)
					offset.x = random (-SLRR_Native_Objects_rndPosX) SLRR_Native_Objects_rndPosX
					offset.y = random (-SLRR_Native_Objects_rndPosY) SLRR_Native_Objects_rndPosY
					offset.z = random (-SLRR_Native_Objects_rndPosZ) SLRR_Native_Objects_rndPosZ

					move obj offset
				)
			)
		)

		on rndPosX_spn changed val do SLRR_Native_Objects_rndPosX = val
		on rndPosY_spn changed val do SLRR_Native_Objects_rndPosY = val
		on rndPosZ_spn changed val do SLRR_Native_Objects_rndPosZ = val

		on rndOri_btn pressed do
		(
			undo "randomize rotation" on
			(
				local offset
				for obj in selection do
				(
					offset = (eulerAngles 0 0 0)
					offset.x = random (-SLRR_Native_Objects_rndOriX) SLRR_Native_Objects_rndOriX
					offset.y = random (-SLRR_Native_Objects_rndOriY) SLRR_Native_Objects_rndOriY
					offset.z = random (-SLRR_Native_Objects_rndOriZ) SLRR_Native_Objects_rndOriZ

					rotate obj offset
				)
			)
		)

		on rndOriX_spn changed val do SLRR_Native_Objects_rndOriX = val
		on rndOriY_spn changed val do SLRR_Native_Objects_rndOriY = val
		on rndOriZ_spn changed val do SLRR_Native_Objects_rndOriZ = val

		on gndPick_btn pressed do
		(
			if selection.count > 0 then
			(
				SLRR_Native_Objects_groundNodes = #()
				for obj in selection do
					append SLRR_Native_Objects_groundNodes obj.handle

				SLRR_Native_Objects.update()
			)
		)

		on gndShow_btn pressed do
		(
			undo "select ground objects" on
			(
				if SLRR_Native_Objects_groundNodes.count > 0 then
				(
					local groundObj = #()
					for h in SLRR_Native_Objects_groundNodes where isValidNode(maxOps.getNodeByHandle h) do
						append groundObj (maxOps.getNodeByHandle h)
					
					select groundObj
				)
			)
		)

		on placement_rbut changed arg do  SLRR_Native_Objects_snapMode = arg
		on placement_spn changed val do SLRR_Native_Objects_snapOffset = val

		on snapTo1_btn pressed do snapToGround normalMultiplier:0.00
		on snapTo2_btn pressed do snapToGround normalMultiplier:0.25
		on snapTo3_btn pressed do snapToGround normalMultiplier:0.50
		on snapTo4_btn pressed do snapToGround normalMultiplier:0.75
		on snapTo5_btn pressed do snapToGround normalMultiplier:1.00

		on objExport_btn pressed do
		(
			local file_name = getSaveFileName caption:"SLRR Export native objects wizard file" types:"Track Wizard Natives file (*.twn)|*.twn"

			if( file_name != undefined ) then
			(
				file = openFile file_name mode:"w" -- write, destroy existing

				if( file != undefined ) then
				(
				-- collect natives from current selection
					local natives = #()
					local lib, id
					
					for obj in selection do
					(
						lib = getUserProp obj "SLRR_native_object_RPK"
						id = getUserProp obj "SLRR_native_object_ID"
						
						if not(lib == undefined or lib == "" or id == undefined or id == "") then
							append natives obj
					)
					
				-- group natives by libraries
					local libs = #()
					local lib_natives = #()
					local idx
					
					for obj in natives do
					(
						lib = getUserProp obj "SLRR_native_object_RPK"
						
						idx = findItem libs lib -- check is this lib already added
						if idx == 0 then -- not collected yet
						(
							append libs lib
							append lib_natives #(obj)
						)
						else
							append lib_natives[idx] obj
					)
					
					format "Track Wizard 2.0 Natives\n" to:file -- write header
					format "%\n" libs.count to:file -- write libs count
					
					local pos, ori
					for i=1 to libs.count do
					(
						format "%\n" (substring libs[i] 2 (libs[i].count-2)) to:file -- write lib name (remowe quotes)
						format "%\n" lib_natives[i].count to:file -- write objects count
						
						for obj in lib_natives[i] do -- write info about each object in this lib
						(
							id = getUserProp obj "SLRR_native_object_ID"
							id = substring id 2 (id.count-2) -- remove quotes
							
						-- object ID
							if id.count > 4 then id = substring id (id.count-3) 4 -- get only last 4 characters
							while id.count < 4 do id = "0" + id -- fill begin with zeros, to make it 4 char length
							
							format "%\n" id to:file -- write object id
							
						-- object properties
							pos = obj.pos/100.0
						
							--ori = quatToEuler (inverse obj.rotation) order:5
							ori = quatToEuler obj.rotation order:5

							ori = point3 ori.x (360-ori.y) ori.z
							for i=1 to 3 do
							(
								ori[i] += 0.000001 -- make sure the output text will no contains special float values
								ori[i] = mod ori[i] 360 -- normalize
								ori[i] = degToRad ori[i] -- deegres to radians
							)
							
							format "%,%,%,%,%,%\n" \
							(formatFloatString(pos.x)) \
							(formatFloatString(pos.z)) \
							(formatFloatString(-pos.y)) \
							(formatFloatString(-ori.x)) \
							(formatFloatString(ori.y)) \
							(formatFloatString(ori.z)) to:file -- write object pos and rotation
							
						--	(formatFloatString(-ori.z)) \
						--	(formatFloatString(-ori.x)) \
						--	(formatFloatString(ori.y)) to:file -- write object pos and rotation
						)
					)
				)
				else
					messagebox ( "Can't save to " + (filenameFromPath file_name) + " file  ") title:"Error" beep:true
				
				close file
			)
		)

		on objEdit_btn pressed do
		(
			DestroyDialog SLRR_Native_Objects -- close

			DestroyDialog SLRR_Native_Objects_properties -- recreate if exsists
			CreateDialog SLRR_Native_Objects_properties 300 108 style:#( #style_titlebar, #style_sysmenu, #style_minimizebox )
		)
		
		function update =
		(
			local selected = selection.count > 0

			rndPos_btn.enabled = selected
			rndPosX_spn.value = SLRR_Native_Objects_rndPosX
			rndPosY_spn.value = SLRR_Native_Objects_rndPosY
			rndPosZ_spn.value = SLRR_Native_Objects_rndPosZ

			rndOri_btn.enabled = selected
			rndOriX_spn.value = SLRR_Native_Objects_rndOriX
			rndOriY_spn.value = SLRR_Native_Objects_rndOriY
			rndOriZ_spn.value = SLRR_Native_Objects_rndOriZ

			gndPick_btn.enabled = selected
			gndShow_btn.enabled = SLRR_Native_Objects_groundNodes.count > 0
			gndShow_btn.text = "Ground objects: " + (SLRR_Native_Objects_groundNodes.count as string)

			placement_rbut.state = SLRR_Native_Objects_snapMode
			placement_spn.value = SLRR_Native_Objects_snapOffset

			snapTo1_btn.enabled = snapTo2_btn.enabled = snapTo3_btn.enabled = snapTo4_btn.enabled = snapTo5_btn.enabled = selected and (SLRR_Native_Objects_groundNodes.count > 0)

			local lib, id, nCount = 0
			for obj in selection do
			(
				lib = getUserProp obj "SLRR_native_object_RPK"
				id = getUserProp obj "SLRR_native_object_ID"
				
				if not(lib == undefined or lib == "" or id == undefined or id == "") then
					nCount += 1
			)
				
			
			objExport_btn.enabled = nCount > 0
			
			objExport_btn.text = "EXPORT NATIVE OBJECTS [" + (nCount as string) + "]"
		)
		
		on SLRR_Native_Objects open do update()
	)

	-- register event callbacks
	callbacks.removeScripts #selectionSetChanged id:#SLRR_Native_Objects_sel_upd
	callbacks.addScript #selectionSetChanged "SLRR_Native_Objects.update()" id:#SLRR_Native_Objects_sel_upd
)

DestroyDialog SLRR_Native_Objects -- recreate if exsists
CreateDialog SLRR_Native_Objects 340 215 style:#( #style_titlebar, #style_sysmenu, #style_minimizebox )
