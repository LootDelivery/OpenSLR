------------------------------------------------------------
-- NORMAL MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_Normal
classID: #(0x7051119, 0x6c820a87)
name: ~NORMAL_NAME~
toolTip: ~NORMAL_TOOLTIP~
backgroundColor: [127,127,255] --AColor(0.5f, 0.5f, 1.0f, 1.0f);
(	
	parameters main rollout:params
	(
		MapSpace type:#integer default:3 ui:mapSpaceCombo
		MikkTForTangent type:#boolean default:true ui: mikkTForTangentCheck
		BitangentPerPixel type:#boolean default:true ui: bitangentPerPixelCheck

		SyncWith type:#integer default:0 ui: syncWithCombo
		XRed type:#integer default:1 ui: xRedCombo
		YGreen type:#integer default:1 ui: yGreenCombo
		
		OutputToNormalBump type:#boolean default:true ui: outputToNormalBumpCheck
				
	)--end params
	
	
	rollout params "Normal Parameters" extendedUI:true
	(
		-- NormalMapParameters::ORIENTATION_X, NormalMapParameters::ORIENTATION_Y, TSTR
		local ORIENTATION_X_LEFT = 1
		local ORIENTATION_X_RIGHT = 2
		local ORIENTATION_Y_UP = 1
		local ORIENTATION_Y_DOWN = 2
		local syncPresets = #(#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN, "3ds Max"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP, "Maya"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP, "Unity"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN, "Unreal"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN, "Cry Engine"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_DOWN, "Lumberyard"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP, "Blender"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP, "Marmoset"),
								#(ORIENTATION_X_RIGHT, ORIENTATION_Y_UP, "Custom"))
		local TANGENT_ITEM_INDEX = 4
		local CUSTOM_SYNC_ITEM_INDEX = syncPresets.count
				
		GridLayout gridLayout columnStretch:#(0,0,1,0,0,1,0,0,3) /* <- add spacer between the cols*/ horzSpacing:6 
		DropDownList mapSpaceCombo ~NORMAL_NORMAL_MAP_SPACE~ row:0 column:0 items:#(~NORMAL_WORLD~, ~NORMAL_SCREEN~, ~NORMAL_OBJECT_LOCAL~, ~NORMAL_TANGENT~)
		DropDownList syncWithCombo ~NORMAL_SYNC_WITH~ row:0 column:3 items:#(~NORMAL_3DS_MAX~, ~NORMAL_MAYA~, ~NORMAL_UNITY~, ~NORMAL_UNREAL~, ~NORMAL_CRY_ENGINE~, ~NORMAL_LUMBERYARD~, ~NORMAL_BLENDER~, ~NORMAL_MARMOSET~, ~NORMAL_CUSTOM~)
		
		CheckBox outputToNormalBumpCheck ~NORMAL_OUTPUT_TO_NORMAL_BUMP~ row:0 column:6
		CheckBox mikkTForTangentCheck ~NORMAL_MIKKT_FOR_TANGENT~ row:1 column:0 tooltip:~NORMAL_MIKKT_FOR_TANGENT_TOOLTIP~
		DropDownList xRedCombo ~NORMAL_X_RED~ row:1 column:3 items:#(~NORMAL_LEFT~, ~NORMAL_RIGHT~) 
		CheckBox bitangentPerPixelCheck ~NORMAL_BITANGENT_PER_PIXEL~ row:2 column:0
		DropDownList yGreenCombo ~NORMAL_Y_GREEN~ row:2 column:3 items:#(~NORMAL_UP~, ~NORMAL_DOWN~)
		
		on mapSpaceCombo selected selIndex do
		(
			--format "You selected '%'!\n" mapSpaceCombo.items[selIndex]
			-- Enable Tangent Calculation options
			mikkTForTangentCheck.enabled = (selIndex == TANGENT_ITEM_INDEX)
			bitangentPerPixelCheck.enabled = (selIndex == TANGENT_ITEM_INDEX and mikkTForTangentCheck.checked)
		)
		
		on mikkTForTangentCheck toggled isChecked do
		(
			bitangentPerPixelCheck.enabled = isChecked;
		)
		
		on syncWithCombo selected selIndex do
		(
			--format "You selected '%'!\n" syncWithCombo.items[selIndex]
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

plugin CommonBakeMapOperator ScanlineNormalMap
classID:#(0x74c431ad, 0x58566b1)
commonMapClassID: CommonMap_Normal.classid
rendererClassID: #(1, 0)
bakeElementClassID: #(0x00010007, 0x00000000) -- Normal Bake Element class id
(
	on requireSceneChange do
	(
		return true
	)
	local initialNormalSpace = #tangent
	local initialTangentXDir = #X_Right
	local initialTangentYDir = #Y_Down
	local initialCalcBitangetPerPixel = false
	local initialUseMikkTForTangent = false
	on preBake bakeElem do
	(
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
				"MapSpace":
				(
					normalSpace = case bttMap.getOptionValue(i) of --ToDo: better enum mapping
					(
						0: #world
						1: #screen
						2: #local
						3: #tangent
					)
					--format "MapOption: % -> '%'\n" paramName normalSpace
				)
				"XRed":
				(
					tangentXDir = case bttMap.getOptionValue(i) of
					(
						0: #X_Left
						1: #X_Right
					)
					--format "MapOption: % -> '%'\n" paramName tangentXDir
				)
				"YGreen":
				(
					tangentYDir = case bttMap.getOptionValue(i) of
					(
						0: #Y_Up
						1: #Y_Down
					)
					--format "MapOption: % -> '%'\n" paramName tangentYDir
				)
				"MikkTForTangent":
				(
					useMikkTForTangent = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName useMikkTForTangent
				)
				"BitangentPerPixel": 
				(
					calcBitangetPerPixel = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName calcBitangetPerPixel
				)
				"BitangentPerPixel": 
				(
					calcBitangetPerPixel = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName calcBitangetPerPixel
				)
				"OutputToNormalBump": 
				(
					mapOutputToNormalBump = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName mapOutputToNormalBump
				)
			)
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
		
	)
)

plugin CommonBakeMapOperator ArnoldNormalMap
classID:#(0x3a3d2672, 0x5654344)
commonMapClassID: CommonMap_Normal.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(0x00010007, 0x00000000) -- Normal Bake Element class id
(
	on requireSceneChange do
	(
		return true
	)
	
	local initialNormalSpace = #tangent
	local initialTangentXDir = #X_Right
	local initialTangentYDir = #Y_Down
	local initialCalcBitangetPerPixel = false
	local initialUseMikkTForTangent = false
	on preBake bakeElem do
	(
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
				"MapSpace":
				(
					normalSpace = case bttMap.getOptionValue(i) of --ToDo: better enum mapping
					(
						0: #world
						1: #screen
						2: #local
						3: #tangent
					)
					--format "MapOption: % -> '%'\n" paramName normalSpace
				)
				"XRed":
				(
					tangentXDir = case bttMap.getOptionValue(i) of
					(
						0: #X_Left
						1: #X_Right
					)
					--format "MapOption: % -> '%'\n" paramName tangentXDir
				)
				"YGreen":
				(
					tangentYDir = case bttMap.getOptionValue(i) of
					(
						0: #Y_Up
						1: #Y_Down
					)
					--format "MapOption: % -> '%'\n" paramName tangentYDir
				)
				"MikkTForTangent":
				(
					useMikkTForTangent = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName useMikkTForTangent
				)
				"BitangentPerPixel": 
				(
					calcBitangetPerPixel = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName calcBitangetPerPixel
				)
				"BitangentPerPixel": 
				(
					calcBitangetPerPixel = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName calcBitangetPerPixel
				)
				"OutputToNormalBump": 
				(
					mapOutputToNormalBump = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName mapOutputToNormalBump
				)
			)
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgHtGvrHiuWcUMwkeg+DdLP0nqX4AeQlrdjRD
-- AY3dsEMJwyW5L8JACR4KT8oPIBoKBJAaHTCaVdwwiFXmoUqmr/WCdvqN1/U1jRT1
-- zhrFVMgJyjPUrqDJpuvVmgskjNnoXAHPw/LXWwOueOru9yDGa22igAYPj2FhBrXe
-- aizvL3c5xVvLF5MgZm62JSsMLCX0T4XBp7hpQVr31U+C9kGvxb//eolMUqR1hm/m
-- 35DzQ2AB08dmxRsETIcmBj6CGc1pRr8yX/ovcfAScIJLKTG4HwZ+HAfLbhxtRalc
-- +52zdtp82V34hHSuwjS07hR7DYeJ2GABi4U8TZZAUgY5xNxPDg0KHqUvjOI5qFUd
-- 1NNBpCspmthnhmp1ZQGCaoFysnYDozytUKZHbDtDwlbYdorUJFAXhFZtjVh10IJy
-- i+umMFbGlSAaJYLI2ZlKhDBiJQy/w2sH7X1ZyU9+BaP1g/qxR769KOrgjd7RzIt7
-- bXip8IijcJxnN068/bwE0thhQfdAYg==
-- -----END-SIGNATURE-----