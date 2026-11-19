------------------------------------------------------------
-- MATERIAL INPUT MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_MaterialInput
classID: #(0xaf609d3L, 0x65f31f9aL)
name: ~MTLINPUT_NAME~
toolTip: ~MTLINPUT_TOOLTIP~
useGamma: true
(
	-- Because of a scope issue we need to keep the UI controls a local variables to be able to manipulate them in CB 
	local callbackItem = undefined
	local localmaterialMapsCB = undefined
	local localObjectMaterialText = undefined
	local localObjectMaterialChangedText = undefined 
	local localCbToTexmap = #()
	
	parameters main rollout:params
	(
		materialSlotIndex type:#integer default:1
	)
	
	fn GetSubTexmapIndexFromComboBox cbIndex =
	(
		if (localCbToTexmap!= undefined) do(
			if( cbIndex > 0 and cbIndex <= localCbToTexmap.count ) do
			(
				return localCbToTexmap[cbIndex]
			)
		)
		return -1
	)
	
	fn UpdateMaterialInfo modifiedNode resetUI = 
	(
		if( modifiedNode.material == undefined ) then
		(
			localObjectMaterialText.text = ~MTLINPUT_NO_MTL~
			localmaterialMapsCB.visible = false
		)
		else 
		(
			typeString = (classof modifiedNode.material ) as string 
			fullText = ~MTLINPUT_MTL_NAME~ + modifiedNode.material.name + "( " + typeString + " )"
			localObjectMaterialText.text = fullText
				
			itemsName = #()
			localCbToTexmap = #()
			currentSlotIndexCBIndex = 1
			for i = 1 to (getNumSubTexmaps modifiedNode.material ) do 
			(
				m_name = getSubTexmapSlotName modifiedNode.material i
				if(m_name != "") then 
				(
					append itemsName m_name
					append localCbToTexmap i
				)
				if( i == materialSlotIndex ) then
				(
					currentSlotIndexCBIndex = localCbToTexmap.count
				)
			)
			localmaterialMapsCB.items = itemsName
			localmaterialMapsCB.visible = true
			-- this index is 0-based
			if(currentSlotIndexCBIndex < 1) then currentSlotIndexCBIndex = 1
			localmaterialmapsCB.currentindex = currentSlotIndexCBIndex - 1
				
			if(resetUI) then
			(
				localObjectMaterialChangedText.visible=true
			)
		)
	)		
	
	
	fn MaterialChangedCB ev nd = 
	(
		for i = 1 to nd.count do 
		(
			modifiedNode = GetAnimByHandle nd[i]
			
			if(modifiedNode != undefined and modifiedNode == bttmap.sceneobject ) then
			(
				newMaterial = modifiedNode.material != bttmap.sceneObject.material	
				UpdateMaterialInfo modifiedNode newMaterial
				return()
			)
		)
	)
	
	local minimumW = 50* GetUIScaleFactor()
	rollout params ~MTLINPUT_PARAMS~ extendedUI:true
	(
		GridLayout gridLayout columnStretch:#(0,1,0,0,1) /* <- add spacer between the cols*/ horzSpacing:6 
		Label objectMaterialText "" visible:true row:0 column:0 minimumWidth:minimumW
		DropDownList materialMapsCB ~MTLINPUT_INPUT~ row:0 column:2 items:#()
		Label objectMaterialChangedText ~MTLINPUT_MTL_CHANGED~ visible:false row:1 column:0
		
		on params open do
		(
			localmaterialMapsCB = materialMapsCB
			localObjectMaterialText = objectMaterialText
			localObjectMaterialChangedText = objectMaterialChangedText
			UpdateMaterialInfo bttMap.sceneObject false
			
			-- Make sure the callback is released 
			callbackItem = undefined
			gc light:true
			callbackItem = NodeEventCallback mouseUp:true delay:1000 materialStructured:this.MaterialChangedCB
		)
		
		on materialMapsCB selected selIndex do
		(
			objectMaterialChangedText.visible=false
			-- Update Material Slot Index , selIndex is 1 based as well as the slot index
			materialSlotIndex = getSubTexmapIndexFromComboBox selIndex
		)
		
		on params close do
		(
			-- This is the way of releasing the callback
			callbackItem = undefined
			gc light:true
		)
	)
)


plugin CommonBakeMapOperator ArnoldMaterialInputMap
classID: #(0x2ef32c81L, 0x62184e3dL)
commonMapClassID: CommonMap_MaterialInput.classid
rendererClassID: #(2980329694L, 2688902778L) --arnold.classid
bakeElementClassID: #(1236154164L, 1814527600L) --arnold_map_override bake element
(
	on requireSceneChange do
	(
		return false
	)
	on preBake bakeElem do
	(
		if bakeElem == undefined do
		(
			return()
		)
		
		mtl = bttMap.sceneObject.material
		
		-- No processing if scene object doesnt have a material
		if (mtl == undefined) then 
			return()
		mapIndex = 0
		for i = 1 to bttMap.getOptionsCount() do
		(
			local paramName = bttMap.getOptionName(i)
			case paramName of
			(
				"materialSlotIndex":
				(
					mapIndex = bttMap.getOptionValue(i)				
				)
			)
		)
		numTexmaps = getNumSubTexmaps mtl 
		if(mapIndex >=1 and mapIndex <= numTexmaps) then
		(
			input_texmap = getSubTexmap mtl mapIndex
			if(input_texmap != undefined) then
			(
				bakeElem.setParamFPValue 1 input_texmap	
			)
		)
	)
)


plugin CommonBakeMapOperator ScanlineMaterialInput
classID: #(0x296102cL, 0x4eaa67dfL)
commonMapClassID: CommonMap_MaterialInput.classid
rendererClassID: #(1, 0)
bakeElementClassID: #(1177994923L, 3734390495L) -- override class id
(
	local initialMaterial = undefined
	on requireSceneChange do
	(
		return true
	)
	on preBake bakeElem do
	(
		if bakeElem == undefined do
		(
			return()
		)
		
		initialMaterial = mtl = bttMap.sceneObject.material
		
		-- No processing if scene object doesnt have a material
		if (mtl == undefined) then 
			return()
		mapIndex = 0
		for i = 1 to bttMap.getOptionsCount() do
		(
			local paramName = bttMap.getOptionName(i)
			case paramName of
			(
				"materialSlotIndex":
				(
					mapIndex = bttMap.getOptionValue(i)			
				)
			)
		)
		numTexmaps = getNumSubTexmaps mtl 
		if(mapIndex >= 1 and mapIndex <= numTexmaps) then
		(
			input_texmap = getSubTexmap mtl mapIndex
			if(input_texmap != undefined) then
			(
				bakeElem.setParamFPValue 1 input_texmap	
				m = MetaSLProxyMaterial()
				m.shader = input_texmap		
				$.material = m
			)
		)
	)

	on postBake do
	(
		bttMap.sceneObject.material = initialMaterial
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgCBVrG7v0QBc1c2hzR/rqmIfNvxETEzAHeWT
-- VcUJtNvhZCJjXhEe1k0LOs+oAwVjwLBl6sjW4lEbns9IIQpGDot6M37/f3PxICWw
-- HvCRGthQoRLcssgVx1voOaB6HfzEZ7Lks1tsbU9M2yBODNZwwwUIbzqr3PxM90OS
-- yYvSMRXzwYfoIWjVRcdEuXzBNfnJZI9c1x4ByJfkLgs9/679+1pZA5Za33vpS/Xv
-- XOu88SQ86POShhEGqbgrWwijTlmyEciO0c/CTsHlJfaqpcYD/0xZzuPiCbfuc4/b
-- xq7qauFSaBnQKkz3XCle+ImcYMB654vcuPvq+wWEM2klMfS6w0slTfovcYcC24RN
-- wqh9s0aKYfYNM1MnMcIgByuew+LwPETcv9KZOXIgpAz4innwcXPwJpdZdqgFn1JK
-- jwBbhv1ww8UplxeDDx3W1dKH2k8tAnYn1wlx0L/j5MiIG4xXbtKmRdcyGUWNMduL
-- EPNHBAuc2Da+osq8AIfg8KQeZi4DLg==
-- -----END-SIGNATURE-----