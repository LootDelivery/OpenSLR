/*
Name: Custom_Attributes_Presets

Originally created by: Paul Neale as PEN_Attribute_Holder plugin modifier
Company: PEN Productions Inc. http://paulneale.com
Purpose: For storing custom attributes and saving presets for them.

**************************************************************************
Adapted for inclusion in 3ds Max by Autodesk Inc.
Copyright (c) 2015 Autodesk, Inc.
All rights reserved.

These coded instructions, statements, and computer programs contain
unpublished proprietary information written by Autodesk, Inc., and are
protected by Federal copyright law. They may not be disclosed to third
parties or copied or duplicated in any form, in whole or in part, without
the prior written consent of Autodesk, Inc.
**************************************************************************
*/

global PENCopyPaste_vr = #()

CustomAttributesPresets = attributes Custom_Attributes_Presets
	attribID:#(0x1f4433d4, 0x363425fa)
	version:3
	initialRollupState:1
(
	--Locals
	local Names = #(), Values = #(), updatePresets_fn, presets_R
	
	parameters control_P 
	(
		presetName_str type:#stringTab tabSize:0 tabSizeVariable:true
		preset_str type:#stringTab tabSize:0 tabSizeVariable:true
	)
	parameters optionsP rollout:presets_R
	(
		rampOn type:#boolean ui:rampOn_Cb default:false animatable:false
	)
	
	----------------------------------------------Get the Ca definitions
	Fn getCaAttributes_fn =
	(
		caDefs = #()
		caSubs = #()

		local owner = (custAttributes.getOwner this)
		--Get the Defs
		caNum = (custAttributes.count owner)
		for x = 1 to caNum do
		(
			append caDefs (custAttributes.get owner x)
		)
		
		--Get the Ca's in the Defs
		for x = 1 to cadefs.count do
		(
			for y = 1 to caDefs[x].numsubs do
			(
				append caSubs caDefs[x][y]
			)
		)
		caSubs
	)
		
	-------------------------------------------------------adds a preset to the menu
	FN addPreset_fn =
	(
		local presetName = presets_R.presetName_et.text
		local ss = stringStream ""
		
		if presetName != "" do
		(
			ca = getCaAttributes_fn()
			format "%" "#(" to:ss
			if (ca.count == 0) then
			(
				format "%" ")" to:ss
			)
			else
			(
				for x = 1 to ca.count do
				(
					format "%" ca[x].value to:ss
					format "%" (if x == ca.count then (")")else (",")) to:ss
				)
			)
			append presetName_str presetName 
			append preset_str (ss as string)
		)
		presets_R.presetName_et.text = ""
		updatePresets_fn()
	)-- end addPreset_fn
	
	-----------------------------------------------------deletes a preset
	FN deletePreset_fn delNum =
	(
		local owner = (custAttributes.getOwner this)
		CNTobj = (refs.dependents owner)[1]
		
		deleteItem presetName_str delNum
		deleteItem preset_str delNum
		
		updatePresets_fn()
	)--end deletePreset_fn

	------------------------------------------------------saves presets to file
	FN savePre_fn =
	(
		local owner = (custAttributes.getOwner this)
		fileName = getsavefileName caption:~CUSTSTRING_PRESET_SAVE_PRESETS~ filename:(this.name + ".cps")\
			types:~CUSTSTRING_PRESET_CHARACTER_PRESETS_STORE~
	
		if fileName != undefined do							--Check for path
		(
			savedFile = createFile fileName					--Create the file
			close savedFile
			openedFile = openFile fileName mode:"a"
			
			for x = 1 to presetName_str.count do						--Format data to file
			(
				-- convert " to ' to prevent user from getting themselves into trouble on load
				-- note: DO NOT LOCALIZE Name/Value; these are part of the file format!
				format "%" "**Name> " to:openedFile
				format "%%%\n" "\"" (substituteString presetName_str[x] "\"" "'") "\"" to:openedFile
				format "%" "**Value> " to:openedFile
				format "%%%\n" "\"" (substituteString preset_str[x] "\"" "'") "\"" to:openedFile
			)
			close openedFile
		)
	)-- end savePre_fn

	------------------------------------------------Check for duplicate names without updating list names
	FN dupNameCheck_fn inStr =
	(
		isDup = false
		inStrUpper = (toUpper inStr)
		for x in presetName_str while not isDup do
		(
			if (toUpper x) == inStrUpper do
			(
				isDup = true
			)
		)

		return isDup
	)-- end dupNameCheck_fn

	------------------------------------------------------process presets file (load and merge)
	FN readPresetsFile_fn inFileName =
	(
		if inFileName != undefined do
		(
			-- to log parsing and duplicated names issues
			isWarnParsingIssues = false
			isWarnDuplicatedNames = false
			lineNbrsWithParsingIssues = #()
			duplicatedNames = #()
			lineNbr = 0

			-- to not process value for duplicated names
			isDuplicatedNameEncountered = false

			-- to use preset name and value as a pair if preset name cannot be added
			isPresetNameReady = false
			filteredName = ""

			openedFile = openFile inFileName mode:"r"
			if openedFile != undefined then
			(
				while (not (eof openedFile)) do
				(
					str = (readLine openedFile)
					lineNbr += 1

					-- skip empty white space lines
					if (filterString str " \t") != undefined and (filterString str " \t").count > 0 do
					(
						if (filterString str ">")[1] == "**Name" then
						(
							isPresetNameReady = false
							filteredName = (filterString str "> \"")[2]
							if filteredName != undefined then
							(
								isDuplicatedNameEncountered = dupNameCheck_fn filteredName
								if isDuplicatedNameEncountered then
								(
									isWarnDuplicatedNames = true
									append duplicatedNames filteredName
								)
								else
								(
									-- preset name will be added when next valid line-value valid will be found
									isPresetNameReady = true
								)
							)
							else
							(
								append lineNbrsWithParsingIssues lineNbr
								isWarnParsingIssues = true
							)
						)
						else if (filterString str ">")[1] == "**Value" then
						(
							if isDuplicatedNameEncountered then
							(
								-- skip this vlaue and reset our flag
								isDuplicatedNameEncountered = false
							)
							else
							(
								if isPresetNameReady then
								(
									filteredValue = (filterString str "> \"")[2]
									if filteredValue != undefined then
									(
										if isPresetNameReady do
										(
											append presetName_str filteredName
											append preset_str filteredValue
										)
									)
									else
									(
										append lineNbrsWithParsingIssues lineNbr
										isWarnParsingIssues = true
									)
								)
								else
								(
									append lineNbrsWithParsingIssues lineNbr
									isWarnParsingIssues = true
								)
							)

							isPresetNameReady = false --reset flag
						)
						else
						(
							append lineNbrsWithParsingIssues lineNbr
							isWarnParsingIssues = true
						)
					)
				)
				close openedFile
			)

			if isWarnParsingIssues do
			(
				messageBox (~CUSTSTRING_PRESET_WARNING_PARSING_PRESETS_FILE~ + lineNbrsWithParsingIssues as string) Title:~CUSTSTRING_PRESET_WARNING~
			)
			if isWarnDuplicatedNames do
			(
				messageBox (~CUSTSTRING_PRESET_WARNING_DUPLICATED_PRESETS_FROM_FILE~ + duplicatedNames as string) Title:~CUSTSTRING_PRESET_WARNING~
			)
		)
	)-- end readPresetsFile_fn

	-----------------------------------------------------load presets
	FN loadPre_fn =
	(
		presetName_str = #()
		preset_str = #()

		fileName = getopenfileName caption:~CUSTSTRING_PRESET_LOAD_PRESETS~ \
			types:~CUSTSTRING_PRESET_CHARACTER_PRESETS_STORE~
		
		if fileName != undefined do
		(
			readPresetsFile_fn fileName
			
			updatePresets_fn()
		)
	)-- end loadPre_fn

	--------------------------------------------------------merge presets
	FN mergePre_fn =
	(
		fileName = getopenfileName caption:~CUSTSTRING_PRESET_MERGE_PRESETS~ \
			types:~CUSTSTRING_PRESET_CHARACTER_PRESETS_STORE~

		if fileName != undefined do
		(
			readPresetsFile_fn fileName
			
			updatePresets_fn()
		)
	)--end mergePre_fn

	------------------------------------------------Check for duplicate names
	Fn addPresetNoDuplication_fn inStr =
	(
		isDup = dupNameCheck_fn inStr

		if isDup == false then
		(
			addPreset_fn()
		)else
		(
			messageBox ~CUSTSTRING_PRESET_DUPLICATE_NAME_ENTERED~ Title:~CUSTSTRING_PRESET_ERROR~
		)
	)--end addPresetNoDuplication_fn

	-------------------------------------------------------- updates preset info
	FN updatePresets_fn =									
	(
		presetName_ar = #()
		for x in presetName_str do (append presetName_ar x)

		presets_R.presets_ddl.items = presetName_ar
		presets_R.presets_ddl.selection=1
	)--end updatePresets_fn 
	
	--Menus
	fn menus =
	(
		rcMenu menuBar
		(
			local modInst --Local instance of the scripted modifier
			menuItem addPreset ~CUSTSTRING_PRESET_ADD_PRESET~
			menuItem deletePreset ~CUSTSTRING_PRESET_DELETE_SELECTED_PRESET~
			menuItem clearPresets ~CUSTSTRING_PRESET_DELETE_ALL_PRESETS~
			menuItem savePreset ~CUSTSTRING_PRESET_SAVE_PRESETS~
			menuItem loadPreset ~CUSTSTRING_PRESET_LOAD_PRESETS~
			menuItem mergePreset ~CUSTSTRING_PRESET_MERGE_PRESETS~
			
			on addPreset picked do
			(
				modInst.addPresetNoDuplication_fn modInst.presets_R.presetName_et.text
			)
			on deletePreset picked do
			(
				if queryBox ~CUSTSTRING_PRESET_QUERY_DELETE_SELECTED~ then
				(
					delNum = modInst.presets_R.presets_ddl.selection
					with MXSCallstackCaptureEnabled off try(modInst.deletePreset_fn delNum)catch()
				)
			)
			on clearPresets picked do
			(
				if queryBox ~CUSTSTRING_PRESET_QUERY_DELETE_ALL~ then
				(
					modInst.presetName_str = #()
					modInst.preset_str = #()
					modInst.presets_R.presets_ddl.items = #()
				)
			)
			on savePreset picked do
			(
				modInst.savePre_fn()
			)
			on loadPreset picked do
			(
				modInst.loadPre_fn()
			)
			on mergePreset picked do
			(
				modInst.mergePre_fn()
			)
			
			on menuBar open do
			(
				modInst=this
			)
		)
		menuBar --Return the menu
	)
		
	rollout presets_R ~CUSTSTRING_PRESET_ROLLOUT_CAPTION~
	(
		group ~CUSTSTRING_PRESET_MANAGE_PRESETS_CAPTION~
		(
			edittext presetName_et ~CUSTSTRING_PRESET_NAME_CAPTION~ fieldWidth:~CUSTSTRING_PRESET_NAME_WIDTH~ align:#left
			button optionsBt ~CUSTSTRING_PRESET_NAME_OPTIONS_CAPTION~ width:~CUSTSTRING_PRESET_NAME_OPTIONS_WIDTH~ pos:~CUSTSTRING_PRESET_NAME_OPTIONS_POSITION~ toolTip:~CUSTSTRING_PRESET_NAME_OPTIONS_TOOLTIP~
		)

		dropDownList presets_ddl ~CUSTSTRING_PRESET_DROPDOWN_CAPTION~
		slider rampCa_Sl ~CUSTSTRING_PRESET_RAMP_CAPTION~ range:[0,1,0] scale:.01 width:~CUSTSTRING_PRESET_RAMP_WIDTH~ --across:2
		checkButton rampOn_Cb "" highLightColor:[0,255,0] width:~CUSTSTRING_PRESET_RAMPON_WIDTH~ height:~CUSTSTRING_PRESET_RAMPON_HEIGHT~ offset:~CUSTSTRING_PRESET_RAMPON_OFFSET~ toolTip:~CUSTSTRING_PRESET_RAMPON_TOOLTIP~
		
		button keyAll_bt ~CUSTSTRING_PRESET_KEYALL_CAPTION~ width:~CUSTSTRING_PRESET_KEYALL_WIDTH~ height:~CUSTSTRING_PRESET_KEYALL_HEIGHT~ across:2 toolTip:~CUSTSTRING_PRESET_KEYALL_TOOLTIP~
		button resetall_bt ~CUSTSTRING_PRESET_RESETALL_CAPTION~ width:~CUSTSTRING_PRESET_RESETALL_WIDTH~ height:~CUSTSTRING_PRESET_RESETALL_HEIGHT~ toolTip:~CUSTSTRING_PRESET_RESETALL_TOOLTIP~
		
		button copyPose_bt ~CUSTSTRING_PRESET_COPY_CAPTION~ width:~CUSTSTRING_PRESET_COPY_WIDTH~ height:~CUSTSTRING_PRESET_COPY_HEIGHT~ across:2 toolTip:~CUSTSTRING_PRESET_COPY_TOOLTIP~
		button pastePose_bt ~CUSTSTRING_PRESET_PASTE_CAPTION~ width:~CUSTSTRING_PRESET_PASTE_WIDTH~ height:~CUSTSTRING_PRESET_PASTE_HEIGHT~ toolTip:~CUSTSTRING_PRESET_PASTE_TOOLTIP~
		
		on optionsBt pressed do
		(
			menuSys=menus()
			popUpMenu menuSys rollout:presets_R
		)

		on copyPose_bt pressed do
		(
			PENCopyPaste_vr = #()
			ca = getCaAttributes_fn()
			for x = 1 to ca.count do
			(
				with MXSCallstackCaptureEnabled off try(append PENCopyPaste_vr ca[x].value)catch()
			)
		)
		
		on pastePose_bt pressed do
		(
 			ca = getCaAttributes_fn()
			for x = 1 to ca.count do
			(
				with MXSCallstackCaptureEnabled off try(ca[x].value = PENCopyPaste_vr[x])catch()
			)
		)
		
		on keyAll_bt pressed do
		(
			ca = getCaAttributes_fn()
			for x = 1 to ca.count do
			(
				with MXSCallstackCaptureEnabled off try(ca[x].value = ca[x].value)catch()
			)
		)
		
		on resetAll_bt pressed do
		(
			ca = getCaAttributes_fn()
			for x = 1 to ca.count do
			(
				with MXSCallstackCaptureEnabled off try(ca[x].value = 0)catch()
			)
		)
		
		on presets_ddl selected i do
		(
			if rampOn_Cb.checked == false then
			(
				ca = getCaAttributes_fn()
				-- format "executing: %\n" preset_str[i]
				if safeExecute != undefined then
					vals = (safeExecute preset_str[i])
				else
					vals = (execute preset_str[i])
				for x = 1 to ca.count do
				(
					with MXSCallstackCaptureEnabled off try(ca[x].value = vals[x])catch()
				)
			)			
		)
		
		local curVals = #()
		on rampCa_Sl buttonDown do
		(
			ca = getCaAttributes_fn()
			for x in ca do
			(
				append curVals x.value
			)
		)
		on rampCa_Sl changed val do
		(
			ca = getCaAttributes_fn()
			vals = 
				if presets_ddl.items.count > 0 then 
				(
					-- format "executing: %\n" preset_str[presets_ddl.selection]
					if safeExecute != undefined then
						safeExecute preset_str[presets_ddl.selection]
					else
						execute preset_str[presets_ddl.selection]
				)
				else
				(
					""
				)
			for x = 1 to ca.count do
			(
				with MXSCallstackCaptureEnabled off try(ca[x].value = curVals[x] + ((vals[x] - curVals[x]) * val))catch()--
			)
		)
		on rampCa_Sl buttonUp do
		(
			rampCa_Sl.value = 0
			curVals = #()
		)
		
----------------------------------------------------------Save presets	
		on addPre_bt pressed do 		
		(
			addPresetNoDuplication_fn presetName_et.text
		)
		on deletePre_bt pressed do
		(
			delNum = presets_ddl.selection
			with MXSCallstackCaptureEnabled off try(deletePreset_fn delNum)catch()
		)
		
		on savePre_bt pressed do
		(
			savePre_fn()
		)
		
		on loadPre_bt pressed do
		(
			loadPre_fn()
		)
		
		on mergePre_bt pressed do
		(
			mergePre_fn()
		)
		
		on presets_R open do
		(
			updatePresets_fn()
		)--end on open
		
	)-- end presets_R


	on update do
	(
		if version == 1 then
		(
			print ~CUSTSTRING_PRESET_VERSION_1_UPDATED~
		)
		else if version == 2 then
		(
			print ~CUSTSTRING_PRESET_VERSION_2_UPDATED~
		)
		else if version == 3 then
		(
			print ~CUSTSTRING_PRESET_VERSION_3_UPDATED~
		)
	)--end on update	

	fn rollouts = (#(presets_R))
)--end attributes

-------BEGIN-SIGNATURE-----
-- cgkAADCCCW4GCSqGSIb3DQEHAqCCCV8wgglbAgEBMQ8wDQYJKoZIhvcNAQELBQAw
-- CwYJKoZIhvcNAQcBoIIHCjCCBwYwggTuoAMCAQICEAPoXQ8zvyfgRZ+u8YJGR4Uw
-- DQYJKoZIhvcNAQELBQAwaTELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDkRpZ2lDZXJ0
-- LCBJbmMuMUEwPwYDVQQDEzhEaWdpQ2VydCBUcnVzdGVkIEc0IENvZGUgU2lnbmlu
-- ZyBSU0E0MDk2IFNIQTM4NCAyMDIxIENBMTAeFw0yMjA3MjcwMDAwMDBaFw0yMzA4
-- MTkyMzU5NTlaMIGKMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTET
-- MBEGA1UEBxMKU2FuIFJhZmFlbDEXMBUGA1UEChMOQXV0b2Rlc2ssIEluYy4xHzAd
-- BgNVBAsTFkRlc2lnbiBTb2x1dGlvbnMgR3JvdXAxFzAVBgNVBAMTDkF1dG9kZXNr
-- LCBJbmMuMIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAwDa9P4A2kzCY
-- kkQvhanUbJJF3K3fWIBjDvAcs/TqTAzYxqHbIW9Oi57R+NLNnB0m+GKwL7Vya0wb
-- UhZvEC8VN5Nw8ZjEDcevdrcv399QQeh5OQaVURgr5JsaSP8u2mdCfvrcnIDBntrf
-- hqGcQT2nioZ1H/PqUUcbOfEXlYMJa+SKm3yWvHkQwihiGWSP13RDDgSJlU+DebsT
-- zv+C9IpPLad8nSpuSA1vCywhzgzok+hDF9H/WJAl3Fv6WQvmj/vH6/8Nb+v5IE/K
-- JvAn+Er5un7fdAXhB+GXHH/bp4jXzq7wUxSxbamDDvpfS3OEN4BFoWgQ5t9eILSp
-- LecHsx9cFyLSbRP1kHBpsUib0lSItjgOAK3tFoE94DBnCWsfUsn8kYqWfcSlAV3l
-- G/T3OhPzHvPGAR2a84Mz71ywuP1j47RsPq2YndMlrxDZV+bGHefT717hDNc1lG+p
-- /Xnr1tdiqw+FHFI7aWIozEJ1zJB6wiXx7mgrQdB1LtNJk7pPOpYdAgMBAAGjggIG
-- MIICAjAfBgNVHSMEGDAWgBRoN+Drtjv4XxGG+/5hewiIZfROQjAdBgNVHQ4EFgQU
-- EF/sqJMBw1FZMj7ZWWrfrXY80GYwDgYDVR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoG
-- CCsGAQUFBwMDMIG1BgNVHR8Ega0wgaowU6BRoE+GTWh0dHA6Ly9jcmwzLmRpZ2lj
-- ZXJ0LmNvbS9EaWdpQ2VydFRydXN0ZWRHNENvZGVTaWduaW5nUlNBNDA5NlNIQTM4
-- NDIwMjFDQTEuY3JsMFOgUaBPhk1odHRwOi8vY3JsNC5kaWdpY2VydC5jb20vRGln
-- aUNlcnRUcnVzdGVkRzRDb2RlU2lnbmluZ1JTQTQwOTZTSEEzODQyMDIxQ0ExLmNy
-- bDA+BgNVHSAENzA1MDMGBmeBDAEEATApMCcGCCsGAQUFBwIBFhtodHRwOi8vd3d3
-- LmRpZ2ljZXJ0LmNvbS9DUFMwgZQGCCsGAQUFBwEBBIGHMIGEMCQGCCsGAQUFBzAB
-- hhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wXAYIKwYBBQUHMAKGUGh0dHA6Ly9j
-- YWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydFRydXN0ZWRHNENvZGVTaWduaW5n
-- UlNBNDA5NlNIQTM4NDIwMjFDQTEuY3J0MAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcN
-- AQELBQADggIBALEgKMMNJ9H5FKWijL5VSvloTQT20Cnln2tk7X3Q8o9b/Ba4qHBf
-- TAPfbF/OaW7k4SZNjHYKnZ3LnBCJFBZTegWigmRsKYwBD92Ot/x/nh2ZgZdwkI0k
-- UbONNkMx3Fo2OR/nxfM8eaevKj+P+QQEtabtDUXAD5e3tspdvCrZWdUk019N8D1c
-- /vLqKIXlPAM8a+1NfKNksZBgr9b3HRr9issohBniAJdE97OgF+2xBnGp72WEe9Uj
-- ZGiGOkGczbCDACjPcBrJqlWrOqJzSFgIzBjcTwBozRsKmGGme5lZ9I8LShBfyz3b
-- XMWL6lo0V5KAZilCLUiFYO3fFcElWLwL8csSD5oZn1zJZs4ZeFzuv1NiS/q7/UlP
-- W4KgAFrnJfRZYJ6OfTAhh4WNmgRXV88L8Fn54huczOnndBePsBE+IkVBT+pe5QwI
-- Dlt0K3EHlsYTE9YBcdXk7+JpWMm3q8etEICpI8UYGUbxhlbWpvb7kIAOyPXj62xG
-- uXSznSL/CSaN/3LoAOWeFnPdRiT1pHx6TXf66XnAtldtAfiW2MjRVhd8QGMYddoA
-- zFPOznYjiijwo+Dnckt0imDhziF6UlpLsY9qn/ZiHihWlJs/FfjWln3iCpG8UJzc
-- ALUxiX1dd/EIj5TuREOmVS4yvHrFhdJnOrsboWb54ZCzFu/23FK43IN1MYICKDCC
-- AiQCAQEwfTBpMQswCQYDVQQGEwJVUzEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4x
-- QTA/BgNVBAMTOERpZ2lDZXJ0IFRydXN0ZWQgRzQgQ29kZSBTaWduaW5nIFJTQTQw
-- OTYgU0hBMzg0IDIwMjEgQ0ExAhAD6F0PM78n4EWfrvGCRkeFMA0GCSqGSIb3DQEB
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgBKJKKXP+Faj5UWC/T5YwXRePTTXR4sChusX
-- 6dBLzVBX7MohBsULo/2/e4ej22DaJZiBeOj2yYcjk6xOlC3ed2VCieWxwgXrZ8SD
-- dy0Y1vNFPCWZPgHL7GZJWhKOwNI90Si54RNMdVIYhiiJZIbXfhSlWjmAX2a2jMHS
-- 3vQMcKmAV20NI1gOyOcqWw64vdwvg5dSZ6m8wqjoezCwf+VMpEe60SiI8FwJ9YCt
-- 1gqtmv7EMWym49LwRdJhHySX0omxA6DOStjjGG68QFtjo0NOX4B1ZslVuRf+BGQI
-- wKQa/5wIsz3f9viQp/rxCZFCZPl8gtp8hCjs8ytp6yDll97zvNP4Fh/A7sB/bVKS
-- WprTUHkcZgI02TjYivyV58SyGzOXasTU00vyNmQ6o4IDcr0qtg8i9oYaSx6fJbeZ
-- NCzYJFHCxVL3edMYPqab4TYOeP9Xtwc2G1F/yvSh/8ljxaNmxMtDn/8ghzttUHdT
-- WGqnmMFZ3U85BL8O1YL0qh/Ksjnfng==
-- -----END-SIGNATURE-----