-- Export tool for Richard's Burn Rally game
-- by Miran (Miran4@op.pl)
-- 17.01.2011

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

utility SLRR_Native_Objects "SLRR Native Objects"
(
	button export_btn "EXPORT  SELECTED" pos:[5,10] width:150 height:20
	button edit_btn "Edit object properties" pos:[5,40] width:150 height:20

	function update =
	(
		local lib, id, nCount = 0
		for obj in selection do
		(
			lib = getUserProp obj "SLRR_native_object_RPK"
			id = getUserProp obj "SLRR_native_object_ID"
			
			if not(lib == undefined or lib == "" or id == undefined or id == "") then
				nCount += 1
		)
			
		export_btn.enabled = nCount > 0
		--if nCount > 0 then export_btn.enabled = true
		--else export_btn.enabled = false
		
		export_btn.text = "EXPORT  SELECTED [" + (nCount as string) + "]"
	)
	
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

	on export_btn pressed do
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
	
	on edit_btn pressed do
		CreateDialog SLRR_Native_Objects_properties 300 108 style:#( #style_titlebar, #style_sysmenu, #style_minimizebox )

	on SLRR_Native_Objects open do 
	(
		update()	
	)
)

-- register event callbacks
callbacks.removeScripts #selectionSetChanged id:#SLRR_Native_Objects_sel_upd
callbacks.addScript #selectionSetChanged "SLRR_Native_Objects.update()" id:#SLRR_Native_Objects_sel_upd
