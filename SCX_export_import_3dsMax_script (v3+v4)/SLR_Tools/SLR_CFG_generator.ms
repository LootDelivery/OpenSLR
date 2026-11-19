if( undefined != SLR_CFGG ) then (closeRolloutFloater SLR_CFGG; gc())
SLR_CFGG = newRolloutFloater "CFG Generator" 180 500
addRollout aboutRoll SLR_CFGG

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
