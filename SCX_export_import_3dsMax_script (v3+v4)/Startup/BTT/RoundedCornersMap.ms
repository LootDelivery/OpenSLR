------------------------------------------------------------
-- ROUNDED CORNERS MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_RoundedCorners
classID: #(0x4d30da7L, 0x488e665eL)
name: ~ROUNDEDCORNER_NAME~
toolTip: ~ROUNDEDCORNER_TOOLTIP~
backgroundColor: [127,127,255] --AColor(0.5f, 0.5f, 1.0f, 1.0f);
(	
	parameters main rollout:params
	(
		CornerRad type:#float default:1 ui:radiusSpinBox
		Samples type:#integer default:6 ui:sampleSpinBox

		MapSpace type:#integer default:3 ui:mapSpaceCombo
		MikkTForTangent type:#boolean default:true ui: mikkTForTangentCheck
		BitangentPerPixel type:#boolean default:true ui: bitangentPerPixelCheck

		SyncWith type:#integer default:0 ui: syncWithCombo
		XRed type:#integer default:1 ui: xRedCombo
		YGreen type:#integer default:1 ui: yGreenCombo
		
		OutputToNormalBump type:#boolean default:true ui: outputToNormalBumpCheck
				
	)--end params
	
	
	rollout params "Rounded Corners Parameters" extendedUI:true
	(
		-- NormalMapParameters::ORIENTATION_X, NormalMapParameters::ORIENTATION_Y, TSTR
		local ORIENTATION_X_LEFT = 1
		local ORIENTATION_X_RIGHT = 2
		local ORIENTATION_Y_UP = 1
		local ORIENTATION_Y_DOWN = 2
		local syncPresets = #(#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN),	--3dsMax
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP),	--Maya
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP),	--Unity
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN),	--Unreal
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN),	--Cry Engine
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN),	--Lumberyard
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP),	--Blender
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP),	--Marmoset
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP))	--Custom
		local TANGENT_ITEM_INDEX = 4
		local CUSTOM_SYNC_ITEM_INDEX = syncPresets.count


		GridLayout gridLayout columnStretch:#(0,0,1,0,0,1,0,0,3) /* <- add spacer between the cols*/ horzSpacing:6 

		Spinner radiusSpinBox ~ROUNDEDCORNER_RADIUS~ maximum:100000 row:0 column:0 
		Spinner sampleSpinBox ~ROUNDEDCORNER_SAMPLES~ type:#integer maximum:100000 row:1 column:0 
				
		DropDownList mapSpaceCombo ~ROUNDEDCORNER_NORMAL_MAP_SPACE~ row:0 column:3 items:#(~ROUNDEDCORNER_WORLD~, ~ROUNDEDCORNER_SCREEN~, ~ROUNDEDCORNER_OBJECT_LOCAL~, ~ROUNDEDCORNER_TANGENT~)
		DropDownList syncWithCombo ~ROUNDEDCORNER_SYNC_WITH~ row:0 column:6 items:#(~ROUNDEDCORNER_3DS_MAX~, ~ROUNDEDCORNER_MAYA~, ~ROUNDEDCORNER_UNITY~, ~ROUNDEDCORNER_UNREAL~, ~ROUNDEDCORNER_CRY_ENGINE~, ~ROUNDEDCORNER_LUMBERYARD~, ~ROUNDEDCORNER_BLENDER~, ~ROUNDEDCORNER_MARMOSET~, ~ROUNDEDCORNER_CUSTOM~)
		
		CheckBox outputToNormalBumpCheck ~ROUNDEDCORNER_OUTPUT_TO_NORMAL_BUMP~ row:3 column:6
		CheckBox mikkTForTangentCheck ~ROUNDEDCORNER_MIKKT_FOR_TANGENT~ row:1 column:3 tooltip:~ROUNDEDCORNER_MIKK_FOR_TANGENT_TOOLTIP~
		DropDownList xRedCombo ~ROUNDEDCORNER_X_RED~ row:1 column:6 items:#(~ROUNDEDCORNER_LEFT~, ~ROUNDEDCORNER_RIGHT~) 
		CheckBox bitangentPerPixelCheck ~ROUNDEDCORNER_BITANGENT_PER_PIXEL~ row:2 column:3
		DropDownList yGreenCombo ~ROUNDEDCORNER_Y_GREEN~ row:2 column:6 items:#(~ROUNDEDCORNER_UP~, ~ROUNDEDCORNER_DOWN~)
		
		on mapSpaceCombo selected selIndex do
		(
			-- Enable Tangent Calculation options
			mikkTForTangentCheck.enabled = (selIndex == TANGENT_ITEM_INDEX)
			bitangentPerPixelCheck.enabled = (selIndex == TANGENT_ITEM_INDEX and mikkTForTangentCheck.checked)
		)
		
		on mikkTForTangentCheck toggled isChecked do
		(
			bitangentPerPixelCheck.enabled = isChecked
		)
		
		on syncWithCombo selected selIndex do
		(
			-- Enable X and Y combo boxes only for "Custom"
			xRedCombo.enabled = (selIndex == CUSTOM_SYNC_ITEM_INDEX)
			yGreenCombo.enabled = (selIndex == CUSTOM_SYNC_ITEM_INDEX)
			
			local preset = syncPresets[selIndex] 
			xRedCombo.selection = preset[1]
			yGreenCombo.selection = preset[2]
		)
		
		on params open do
		(
			-- Init controls
			-- Enable Tangent Calculation options
			local selMapSpaceComboIndex = mapSpaceCombo.selection
			mikkTForTangentCheck.enabled = (selMapSpaceComboIndex == TANGENT_ITEM_INDEX)
			bitangentPerPixelCheck.enabled = (selMapSpaceComboIndex == TANGENT_ITEM_INDEX and mikkTForTangentCheck.checked)
			-- Enable X and Y combo boxes only for "Custom"
			local selSyncComboIndex = syncWithCombo.selection
			xRedCombo.enabled = (selSyncComboIndex == CUSTOM_SYNC_ITEM_INDEX)
			yGreenCombo.enabled = (selSyncComboIndex == CUSTOM_SYNC_ITEM_INDEX)
		)
	)
)

plugin CommonBakeMapOperator ArnoldRoundedCornersMap
classID:#(0x509073bbL, 0x74c65775L)
commonMapClassID: CommonMap_RoundedCorners.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(0x00010007, 0x00000000) -- Normal Bake Element class id
(
	local initialMaterial = undefined
	local projObject = undefined
	local initialNormalSpace = #tangent
	local initialTangentXDir = #X_Right
	local initialTangentYDir = #Y_Down
	local initialCalcBitangetPerPixel = false
	local initialUseMikkTForTangent = false
	
	on requireSceneChange do
	(
		return true
	)
	on preBake bakeElem do
	(
		local radius = 1.0
		local samples = 6
		local normalSpace = #tangent
		local tangentXDir = #X_Right
		local tangentYDir = #Y_Down
		local calcBitangetPerPixel = false
		local useMikkTForTangent = false
		local mapOutputToNormalBump = false
		
		for i = 1 to bttMap.getOptionsCount() do
		(
			local paramName = bttMap.getOptionName(i)
			case paramName of
			(
				"CornerRad":
				(
					radius = bttMap.getOptionValue(i)
				)
				"Samples":
				(
					samples = bttMap.getOptionValue(i)
				)
				"MapSpace":
				(
					normalSpace = case bttMap.getOptionValue(i) of --ToDo: better enum mapping
					(
						0: #world
						1: #screen
						2: #local
						3: #tangent
					)
				)
				"XRed":
				(
					tangentXDir = case bttMap.getOptionValue(i) of
					(
						0: #X_Left
						1: #X_Right
					)
				)
				"YGreen":
				(
					tangentYDir = case bttMap.getOptionValue(i) of
					(
						0: #Y_Up
						1: #Y_Down
					)
				)
				"MikkTForTangent":
				(
					useMikkTForTangent = bttMap.getOptionValue(i)
				)
				"BitangentPerPixel": 
				(
					calcBitangetPerPixel = bttMap.getOptionValue(i)
				)
				"BitangentPerPixel": 
				(
					calcBitangetPerPixel = bttMap.getOptionValue(i)
				)
				"OutputToNormalBump": 
				(
					mapOutputToNormalBump = bttMap.getOptionValue(i)
				)
			)
		)
		
		initialMaterial = bttMap.sceneObject.material
		
		-- Create a standard surface with rounder corners
		standardSurface = ai_standard_surface()
		roundCornerMap = ai_round_corners()

		roundCornerMap.radius = radius
		roundCornerMap.samples = samples

		standardSurface.normal_shader = roundCornerMap

		-- Get the index of the projection modifier
		projModifierIndex = bttmap.projectionModifierIndex

		-- If the projection is not set to self, get the projection object from the projection modifier
		if(projModifierIndex != 0) then
		(
			modifiers = bttMap.sceneObject.modifiers
			if(modifiers.count >= projModifierIndex) then
			(
				if((ClassOf modifiers[projModifierIndex]) == projection) then
				(
					projObjects = (modifiers[projModifierIndex]).geomNodes

					if(projObjects.count > 0) then
					(
						-- Replace the material of the projObject with the standard surface with rounded corners
						projObject = projObjects[1]

						initialMaterial = projObject.material
						projObject.material = standardSurface
					)
				)
			)
		)
		else
		(
			-- If the projection is set to self, replace the material of the object with the standard surface with rounded corners
			initialMaterial = bttMap.sceneObject.material
			bttMap.sceneObject.material = standardSurface
		)


		bakeProjProps =  bttMap.sceneObject.INodeBakeProjProperties
		
		--{#world|#screen|#local|#tangent}
		initialNormalSpace = bakeProjProps.normalSpace
		bakeProjProps.normalSpace = normalSpace
		
		-- Syn Preset is not a property, but the preset x/Y values drive those:
		--{#X_Right|#X_Left}
		initialTangentXDir = bakeProjProps.tangentXDir
		bakeProjProps.tangentXDir = tangentXDir
		
		--{#Y_Up|#Y_Down}
		initialTangentYDir = bakeProjProps.tangentYDir
		bakeProjProps.tangentYDir = tangentYDir

		if normalSpace == #tangent do
		(
			-- The "Mikkt for Tangent" will force the tangent mode to Mikkt:
			initialUseMikkTForTangent = NormalMappingManager.NormalBumpMode
			if useMikkTForTangent do
			(
				NormalMappingManager.NormalBumpMode = #mikkt
			)
			--Mikkt settings are "global setting"
			initialCalcBitangetPerPixel = NormalMappingManager.calculateBitangentPerPixel
			NormalMappingManager.calculateBitangentPerPixel = calcBitangetPerPixel
		)
		
		bttMap.outputtonormalbump = mapOutputToNormalBump			
	)
	
	on postBake do
	(
		bakeProjProps =  bttMap.sceneObject.INodeBakeProjProperties
		--{#world|#screen|#local|#tangent}
		bakeProjProps.normalSpace = initialNormalSpace
		
		--{#X_Right|#X_Left}
		bakeProjProps.tangentXDir = initialTangentXDir
		
		--{#Y_Up|#Y_Down}
		bakeProjProps.tangentYDir = initialTangentYDir

		if normalSpace == #tangent do
		(
			-- The "Mikkt for Tangent" will force the tangent mode to Mikkt:
			if useMikkTForTangent do
			(
				NormalMappingManager.NormalBumpMode = initialUseMikkTForTangent
			)
			--Mikkt settings are "global setting"
			NormalMappingManager.calculateBitangentPerPixel = initialCalcBitangetPerPixel
		)
		
		-- Reassign the itnitial material to the object
		if(projObject != undefined) then 
		(
			projObject.material = initialMaterial
		)
		else
		(
			bttMap.sceneObject.material = initialMaterial
		)
	)
)

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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgCCEY8B29NA/dMPRsxXx1EDs4SCaeqP793f+
-- YQc3SAS28BJQr/0HorsODPPf8RcWL0js+N6o0Gl8qHTgRYfWUxhv856LndM8Zv3R
-- O1pogImJ0DoK90SKRb7okbrc6prDxQYszA9ldmV4q7qLnM4GODWn9Mi44a+7CNPR
-- gvcT3U714Lqz88roLyeh0U4AGmLBSlVEd4ZuGC6u09zlgb43gm967M9SNBvBT33R
-- wwkJLN7aBkHFDUe0y+qYZ0NvYKXASk5JkXwExLoYkXYMinWH9P6yw8VkSWOz754/
-- +Vgh1V7ay5TxkC2gj6DuJX6qZa7yK50NA2L7kR2XyzGRDngea/KWvwtDfzNXHXXI
-- kB9yBx8oovqGg/MyQCEhOSAqO8/tClr4eWdO8eTpWVM8W7cpkU1OBz2x6yL98+mf
-- VjWArcere9Ge1A5WpO2Q0i7a1ARe7lC+yJ/i4JX6t9RxDNxFBIkoSTYZcR8xfaot
-- fiw/CwUMuIVNoUINtpm7L2F9Zhay2g==
-- -----END-SIGNATURE-----