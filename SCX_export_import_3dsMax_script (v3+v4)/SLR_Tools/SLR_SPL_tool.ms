fn shapefilter line = 
(
	superClassOf line == Shape
)

rollout rollTool "SPL Tool"
(
	group "Import"
	(
		button imp "Import SPL" width:140
	)
	
	on imp pressed do
	(			
	)
)

fn GetVec s k d =
(
	if (d==1) then
		return getOutVec $ s k
	else
		return getInVec $ s k
)

fn outputKnot s k d =
(
	p = getKnotPoint $ s k
	v = GetVec s k d
	p /= 100.0
	v /= 100.0
	v -= p
	p = [p[1], p[3], -p[2]]
	v = [v[1], v[3], -v[2]]
	v /= length v
	format "% % %\t% % %\t%\n" p[1] p[2] p[3] v[1] v[2] v[3] 2.0 to:txt
)

rollout exportSPL "Export SPL"
(
	label l1 "select line" pos:[81,33] width:0 height:13
	checkbox invDir "Inverse Direction"	
	button exp "Export SPL" width:140
	
	on selObj picked obj do
	(
		selObj.Text = obj.name
	)
	
	on selObjByName pressed do
	(
		obj.name = selObj.Text
		
		print obj.name
		--print selObj.Text
	)
	
	on exp pressed do
	(		
		if selObj.Text != "Select Line" then
		(		
			txt = newScript()
			
			select $line01
			
			if invDir.checked == false then
			(			
				for s = numSplines $ to 1 by -1 do
				(
					for k = numKnots $ s to 1 by -1 do
					(
						outputKnot s k -1
					)
				
					outputKnot (numSplines $) (numKnots $ (numSplines $)) -1
				)
			)
			else
			(				
				for s = 1 to numSplines $ do
				(
					for k = 1 to numKnots $ s do
					(
						outputKnot s k 1
					)
					
					outputKnot 1 1 1
				)
			)
		)
		else
			messagebox "Please select line"
	)
)

rollout aboutRoll "About" width:162 height:175 rolledup:true
(
	group ""
	(	
		label l1 "SLR Tools v1.0" pos:[44,26] width:74 height:13
		label l2 " " pos:[80,44] width:3 height:13
		label l3 "Created by:" pos:[54,62] width:54 height:13
		label l4 "Bartosz 'Wichur' Bieszka" pos:[23,80] width:116 height:13
		label l5 "All Rights Reserved" pos:[35,98] width:93 height:13
		label l6 " " pos:[80,116] width:3 height:13
		label l7 "wichur@msn.com" pos:[39,134] width:84 height:13
		label l8 "25 Nov 2009" pos:[50,152] width:62 height:13
	)
)

if( undefined != SLR_SPL_TOOL ) then (closeRolloutFloater SLR_SPL_TOOL; gc())
SLR_SPL_TOOL = newRolloutFloater "SPL Tool" 180 500
addRollout rollTool SLR_SPL_TOOL
addRollout exportSPL SLR_SPL_TOOL
addRollout aboutRoll SLR_SPL_TOOL