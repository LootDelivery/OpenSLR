-- load buttons images
SLRR_buttons_count = 13

if( SLRR_buttons != undefined ) then close SLRR_buttons
SLRR_buttons = openBitmap ((GetDir #scripts)+"\\SLR_Tools\\Buttons.bmp")

if( SLRR_buttons_a != undefined ) then close SLRR_buttons_a
SLRR_buttons_a = openBitmap ((GetDir #scripts)+"\\SLR_Tools\\ButtonsA.bmp")

-- store properties after close dialog
if( SLR_Object_Info_parent == undefined ) then
(
	global SLR_Object_Info_mode = 1
	global SLR_Object_Info_parent = undefined
)

function formatFloatStringC f = -- coarse
(
	f *= 100.0
	f = floor f
	f /= 100.0
	
	f = (f as string)
	for i=1 to (2-(f.count-(findString f "."))) do -- fill missing zeros
		append f "0"

	return f
)

function formatFloatString f =
(
	f *= 1000.0
	f = floor f
	f /= 1000.0
	
	f = (f as string)
	for i=1 to (3-(f.count-(findString f "."))) do -- fill missing zeros
		append f "0"

	return f
)

function formatFloatStringF f = -- fine
(
	f *= 10000.0
	f = floor f
	f /= 10000.0

	f = (f as string)
	for i=1 to (4-(f.count-(findString f "."))) do -- fill missing zeros
		append f "0"

	return f
)

if( SLR_Object_Info != undefined ) then -- recreate if window already existing
	DestroyDialog SLR_Object_Info

rollout SLR_Object_Info "SLR object info"
(
	local handler_object, handler_parent -- update handlers
	function isGeometry obj = isKindOf obj GeometryClass

	label selection_lbl "Object:" pos:[7,11]
	pickbutton selection_pbtn pos:[46,8] width:242 height:20 filter:isGeometry
	button sel_list_btn pos:[292,8] width:20 height:20 images:#( SLRR_buttons, SLRR_buttons_a, SLRR_buttons_count, 9, 9, 9, 9 ) tooltip:"Choose from list"

	label parent_lbl "Parent:" pos:[7,39]
	checkbutton parent_link_chbtn pos:[46,34] width:20 height:20 images:#( SLRR_buttons, SLRR_buttons_a, SLRR_buttons_count, 12, 12, 13, 13 ) tooltip:"Automatic from linking"
	pickbutton parent_pbtn pos:[70,34] width:194 height:20 filter:isGeometry
	button parent_root_btn pos:[292,34] width:20 height:20 images:#( SLRR_buttons, SLRR_buttons_a, SLRR_buttons_count, 5, 5, 6, 6 ) tooltip:"World root as parent"
	button parent_list_btn pos:[268,34] width:20 height:20 images:#( SLRR_buttons, SLRR_buttons_a, SLRR_buttons_count, 9, 9, 9, 9 ) tooltip:"Choose from list"

	imgTag separator "" pos:[8,60] width:304 height:1 style:#bmp_stretch bitmap:(bitmap 1 1 color:(color 110 110 110)) -- separate horizontal line

	-- mode buttons
	checkbutton mode_1_chbtn "CFG Pos&Ori" pos:[8,67] width:73 height:18 tooltip:"Position and orientation for cfg files"
	checkbutton mode_2_chbtn "RPK Pos&Ori" pos:[85,67] width:73 height:18 tooltip:"Position and orientation for rpk files"
	checkbutton mode_3_chbtn "Bounds" pos:[162,67] width:73 height:18 tooltip:"Bounding box for native objects"
	checkbutton mode_4_chbtn "Physic" pos:[239,67] width:73 height:18 tooltip:"Physics for cfg objects"

	edittext output_txt pos:[4,91] fieldWidth:280 height:20
	button copy_btn  pos:[292,91] width:20 height:20 images:#( SLRR_buttons, SLRR_buttons_a, SLRR_buttons_count, 10, 10, 11, 11 ) tooltip:"Copy to clipboard"


	on selection_pbtn picked obj do 
	(
		select #(obj)
		SLR_Object_Info.update()
	)

	on sel_list_btn pressed do (max tool hlist) -- select object by list

	on parent_link_chbtn changed state do SLR_Object_Info.update()

	on parent_pbtn picked obj do 
	(
		parent_link_chbtn.checked = false
		SLR_Object_Info_parent = obj
		SLR_Object_Info.update()
	)

	on parent_root_btn pressed do
	(
		parent_link_chbtn.checked = false
		SLR_Object_Info_parent = undefined
		SLR_Object_Info.update()
	)

	on mode_1_chbtn changed state do
	(
		SLR_Object_Info_mode = 1
		SLR_Object_Info.updateRollout()
	)

	on mode_2_chbtn changed state do
	(
		SLR_Object_Info_mode = 2
		SLR_Object_Info.updateRollout()
	)

	on mode_3_chbtn changed state do
	(
		SLR_Object_Info_mode = 3
		SLR_Object_Info.updateRollout()
	)

	on mode_4_chbtn changed state do
	(
		SLR_Object_Info_mode = 4
		SLR_Object_Info.updateRollout()
	)

	on copy_btn pressed do
	(
		ShellLaunch ((GetDir #scripts)+"\\SLR_Tools\\Data\\Copy_to_clipboard.exe") ("\"" + output_txt.text + "\"")
	)

	local updateTime = 0
	function updateOut = 
	(
		if selection.count > 0  then 
		(
			if (updateTime+60) < timeStamp() then -- min update time is 60ms
			(
				updateTime = timeStamp()

				local pos, ori, ppos, pori
				if( selection.count == 1 ) then
				(
					pos = selection[1].pos
					ori = selection[1].rotation
				)
				else
				(
					pos = selection.center
					ori = quat 0 0 0 0
				)

				if( SLR_Object_Info_parent != undefined ) then
				(
					ppos = SLR_Object_Info_parent.pos
					pori = SLR_Object_Info_parent.rotation
				)
				else
				(
					ppos = (point3 0 0 0)
					pori = (quat 0 0 0 0)
				)

				pos -= ppos -- parent offset
				pos /= 100.0 -- slr scale

				ori = quatToEuler ori order:5
				pori = quatToEuler pori order:5
				ori = point3 (ori.x-pori.x) (360-(ori.y-pori.y)) (ori.z-pori.z)
				for i=1 to 3 do
				(
					ori[i] += 0.000001 -- make sure the output text will no contains special float values
					ori[i] = mod ori[i] 360 -- normalize
					ori[i] = degToRad ori[i] -- deegres to radians
				)		

				case SLR_Object_Info_mode of 
				( 
					1: output_txt.text = (	(formatFloatString pos.x) + " " + 
								(formatFloatString pos.z) + " " + 
								(formatFloatString (-pos.y)) + " \t" +
								(formatFloatStringF ori.x) + " " + 
								(formatFloatStringF ori.y) + " " +
								(formatFloatStringF ori.z) )
					
					2: output_txt.text = (	(formatFloatString pos.x) + "," + 
								(formatFloatString pos.z) + "," + 
								(formatFloatString (-pos.y)) + "," +
								(formatFloatStringF ori.x) + "," + 
								(formatFloatStringF ori.y) + "," +
								(formatFloatStringF ori.z) )

					3: 
					(
						local bMax, bMin, ppos
							
						if( SLR_Object_Info_parent != undefined ) then
							ppos = SLR_Object_Info_parent.pos
						else
							ppos = (point3 0 0 0)

						/*bMax = (selection.max - ppos) / 100.0 -- slr scale
						bMin = (selection.min - ppos) / 100.0//*/

						bMax = selection.max / 100.0 -- slr scale
						bMin = selection.min / 100.0

						output_txt.text = ( "bounds\t" + (formatFloatStringC bMin.x) + " " +
										  (formatFloatStringC bMin.z) + " " +
										  (formatFloatStringC bMin.y) + " " +
										  (formatFloatStringC bMax.x) + " " +
										  (formatFloatStringC bMax.z) + " " +
										  (formatFloatStringC bMax.y) )
					)

					4: 
					(
						output_txt.text = ( "body\t\t" + (formatFloatString pos.x) + " " + 
										(formatFloatString pos.z) + " " + 
										(formatFloatString (-pos.y)) + " \t" +
										(formatFloatStringF ori.x) + " " + 
										(formatFloatStringF ori.y) + " " +
										(formatFloatStringF ori.z) + "\t" +
										"0.0" + "\t")

						if selection.count == 1 then
						(
							case ClassOf selection[1] of
							(
								Sphere: output_txt.text += ("sphere " + (formatFloatString selection[1].radius/100))

								Cylinder: output_txt.text += ("rody " +	(formatFloatString (selection[1].height/200)) + " " + 
													(formatFloatString (selection[1].radius/100)) )

								Box: output_txt.text += ("box " +	(formatFloatString (selection[1].width/200)) + " " +
													(formatFloatString (selection[1].length/200)) + " " +
													(formatFloatString (selection[1].height/200)) )
								
								default: output_txt.text += ("\\Meshes\\" + selection[1].name + ".scx")
							)
						)
					)
				)
			)
		)
		else
			output_txt.text = ""
	)

	function updateRollout = 
	(
		if( selection.count > 1 ) then
		(
			selection_pbtn.text = "[MULTI SELECTION]"

			output_txt.enabled = true
			copy_btn.enabled = true
		)
		else
		if( selection.count == 1 ) then
		(
			selection_pbtn.text = selection[1].name

			output_txt.enabled = true
			copy_btn.enabled = true
		)
		else
		(
			selection_pbtn.text = "[NO SELECTION]"

			output_txt.text = ""
			output_txt.enabled = false
			copy_btn.enabled = false
		)


		if( SLR_Object_Info_parent != undefined ) then
		(
			parent_pbtn.text = SLR_Object_Info_parent.name
			parent_root_btn.enabled = true
		)
		else
		(
			parent_pbtn.text = "[WORLD ROOT]"
			parent_root_btn.enabled = false
		)

		mode_1_chbtn.checked = false
		mode_2_chbtn.checked = false
		mode_3_chbtn.checked = false
		mode_4_chbtn.checked = false
		case SLR_Object_Info_mode of 
		( 
			1: mode_1_chbtn.checked = true
			2: mode_2_chbtn.checked = true
			3: mode_3_chbtn.checked = true
			4: mode_4_chbtn.checked = true
		)

		updateOut()
	)

	function update =
	(
	-- object support
		if( handler_object != undefined ) then 
			deletechangehandler handler_object

		if( selection.count > 0 ) then
			handler_object = when transform selection[1] changes handleAt:#redrawViews do updateOut()

	-- parent support
		if parent_link_chbtn.checked then
		(
			if selection.count == 1 then
				SLR_Object_Info_parent = selection[1].parent
			else
			(
			)
		)

		if( handler_parent != undefined ) then 
			deletechangehandler handler_parent

		if( SLR_Object_Info_parent != undefined ) then
			handler_parent = when transform SLR_Object_Info_parent changes do updateOut()

		updateRollout()
	)

	on SLR_Object_Info close do
	(
		if( handler_object != undefined ) then deletechangehandler handler_object
		if( handler_parent != undefined ) then deletechangehandler handler_parent
		callbacks.removeScripts #selectionSetChanged id:#SLR_Object_Info_sel_upd
	)
)

SLR_Object_Info_dlg = CreateDialog SLR_Object_Info width:320 height:119 style:#( #style_sysmenu, #style_toolwindow )
SLR_Object_Info.update()

-- register event callbacks
callbacks.addScript #selectionSetChanged "SLR_Object_Info.update()" id:#SLR_Object_Info_sel_upd
