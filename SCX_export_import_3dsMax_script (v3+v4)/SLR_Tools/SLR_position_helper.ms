rollout helperRoll "SLRR Position Helper"
(
	label lab00 "SLRR Position Helper" pos:[30,7] width:103 height:13

	group "Settings"
	(
		checkbox invert "Invert Z position" pos:[13,44] width:100 height:15 checked:false
		checkbox linked "Object is linked" pos:[13,64] width:96 height:15 checked:false
		label null00 "" pos:[81,84] width:0 height:13
		button mov "Reset chassis position" pos:[13,90] width:137 height:28
		label null01 "" pos:[81,128] width:0 height:13		
		pickbutton pick "Pick object" pos:[11,146] width:139 height:39
		label null02 "" pos:[81,172] width:0 height:13
	)
	
	on mov pressed do	
	(
		if $chassis == null then
			messagebox "Can't find chassis in scene"
		else
			$chassis.position = [0, 0, 0]
	)	
	on pick picked obj do
	(	
		--if $chassis.position == [0, 0, 0] then
		--(
			if (linked.checked == false) then
			(
				sys_x = (obj.pos.x/100) as string
				sys_y = (obj.pos.z/100) as string
		
				if invert.checked == false then
					sys_z = (-obj.pos.y/100) as string
				else
				(
					sys_z = (obj.pos.y/100) as string
		
					sys_all = obj.name+ ": " +sys_x+ " " +sys_y+ " " +sys_z
					print sys_all
				)
			)
			else
			(
				if obj.parent != null then
				(
					-- create file to save
					
					sys2_x = ((obj.pos.x-obj.parent.pos.x)/100) as string
					sys2_y = ((obj.pos.z-obj.parent.pos.z)/100) as string
			
					if invert.checked == false then
						sys2_z = ((-obj.pos.y+(-obj.parent.pos.y))/100) as string
					else
					(
						sys2_z = ((obj.pos.y-obj.parent.pos.y)/100) as string
				
						sys_all_2 = obj.parent.name+ "-" +obj.name+ ": " +sys2_x+ " " +sys2_y+ " " +sys2_z
						print sys_all_2
					)
				)
				else
					messagebox "Object are not linked!"
			)		
		--)
	)
)
	
rollout aboutRoll "About" rolledup:true
(
	group ""
	(
		label l1 "SLR Tools v1.0"
		label l2 " "
		label l3 "Created by:"
		label l4 "Bartosz 'Wichur' Bieszka"
		label l5 "All Rights Reserved"
		label l6 " "
		label l7 "wichur@msn.com"
		label l8 "25 Nov 2009"
	)
)

if( undefined != SLR_POSHELPER ) then (closeRolloutFloater SLR_POSHELPER; gc())
SLR_POSHELPER = newRolloutFloater "Position Helper" 180 500
addRollout helperRoll SLR_POSHELPER
addRollout aboutRoll SLR_POSHELPER