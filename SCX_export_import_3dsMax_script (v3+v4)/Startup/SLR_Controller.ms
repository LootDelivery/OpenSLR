utility SLR_generator "SLR Tools v1.0"
(
		group "General"
		(
			button script01 "SCX Importer" width:140 align:#center
			button script02 "CFG Generator" width:140 align:#center
		)
		
		group "Tracks"
		(
			button script04 "SPL Tool" width:140 align:#center
		)
		
		group "Helpers"
		(
			button script03 "Position Helper" width:140 align:#center
		)
		
		on script01 pressed do
		(
			try ( fileIn (scriptspath+"\\SLR_Tools\\SLR_SCX_import.ms") quiet:true   )
			catch messagebox "Please re-install SLR Tools!"
		)
		
		on script02 pressed do
		(
			try ( fileIn (scriptspath+"\\SLR_Tools\\SLR_CFG_generator.ms") quiet:true   )
			catch messagebox "Please re-install SLR Tools!"
		)
		
		on script03 pressed do
		(
			try ( fileIn (scriptspath+"\\SLR_Tools\\SLR_position_helper.ms") quiet:true   )
			catch messagebox "Please re-install SLR Tools!"
		)
		
		on script04 pressed do
		(
			try ( fileIn (scriptspath+"\\SLR_Tools\\SLR_SPL_Tool.ms") quiet:true   )
			catch messagebox "Please re-install SLR Tools!"
		)
)