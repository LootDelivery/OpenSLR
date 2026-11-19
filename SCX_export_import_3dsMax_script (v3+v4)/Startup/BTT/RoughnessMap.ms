plugin CommonBakeMap CommonMap_Roughness
classID: #(0x382e2f56L, 0x42916439L)
name: ~ROUGHNESS_NAME~
toolTip: ~ROUGHNESS_TOOLTIP~
(
	parameters main rollout:params
	(
		BakeGlossiness type:#boolean default:false ui: bakeGlossinessCheckBox
	)
	
	rollout params "Roughness Parameters" extendedUI:true
	(			
		GridLayout gridLayout columnStretch:#(0,0,1) /* <- add spacer between the cols*/ horzSpacing:6 
		CheckBox bakeGlossinessCheckBox ~ROUGHNESS_GLOSSINESS~ row:0 column:0	
	)
)

plugin commonBakeMapOperator ArnoldRoughnessMap
classID:#(0x798e05f3L, 0x51855600L)
commonMapClassID: CommonMap_Roughness.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(1236154164L, 2569562704L) --arnold_advanced_map bake element
(

	on requireSceneChange do
	(
		return false
	)

	local initialCompatibilityMode = undefined
	on preBake bakeElem do
	(
		initialCompatibilityMode = renderers.current.compatibility_mode
		
		-- Set current legacy 3ds max map support to true to support ColorMap()
		renderers.current.compatibility_mode = 1
		
        bakeGlossiness = bttMap.getOptionValue(1)
        if( bakeGlossiness ==true ) then(
			bakeElem.setParamFPValue 1 "glossiness"
		)
		else (
			bakeElem.setParamFPValue 1 "specular_roughness"
		)
		bakeElem.setParamFPValue 2 "standard_surface"
	)
	
	on postBake do
	(
		-- Set the original value of the compatibility_mode
		renderers.current.compatibility_mode = initialCompatibilityMode 
	)
)

						

plugin CommonBakeMapOperator ScanlineRoughnessMap
classID:#(0x57e222b7L, 0x5bbb7e95L)
commonMapClassID: CommonMap_Roughness.classid
rendererClassID: #(1, 0)
bakeElementClassID: #(1177994923L, 3734390495L) --override bake element
(
	on requireSceneChange do
	(
		return true
	)
	local initialMaterial = undefined
	local bakeGlossiness = undefined

	on preBake bakeElem do
	(
		bakeGlossiness = bttMap.getOptionValue(1)
			
		initialMaterial = mtl = bttmap.sceneObject.material
		materialClass = classof initialMaterial
		materialClass = materialClass as string

		isGlossiness = false
		map = undefined
		value = undefined

		if (mtl == undefined) then 
			return()
			

		if ( materialClass == "ai_standard_surface" ) then 
		(
			map = mtl.specular_roughness_shader
			value = mtl.specular_roughness
			isGlossiness = false 	-- Standard surface only has setting for roughness
		)
		else if ( materialClass == "Standardmaterial" ) then 
		(
			map = mtl.glossinessMap
			value = mtl.glossiness/100.0  	-- Scanline legacy. Glossiness in % (have to divide by 100 to get value between 0 and 1)
			isGlossiness = true 	 -- Standard surface only has setting for glossiness
		)
		else if ( materialClass == "PhysicalMaterial" ) then
		(
			map = mtl.roughness_map
			value = mtl.roughness
			isGlossiness = mtl.roughness_inv  	-- if the roughness in inverted, then it is a glossiness value or map
		)
		else if (materialClass == "PBRSpecGloss" ) then 
		(
			map = mtl.glossiness_map
			value = mtl.glossiness
			isGlossiness = (mtl.UseGlossiness == 1) 	 -- 1->glossiness, 2->roughness
		)
		else if( materialClass == "PBRMetalRough") then
		(
			map = mtl.roughness_map
			value = mtl.roughness
			isGlossiness = (mtl.UseGlossiness == 1) 	-- 1->glossiness, 2->roughness
		)

		-- Should be inverted if : bakeGlossiness XOR isGlossiness
		-- We want to invert the map only if:
		-- we have a roughness input and the user ask for a glossiness output
		-- or
		-- we have a glossiness input and the user ask for a rougnhess output
		shouldBeInverted = (bakeGlossiness and not isGlossiness) or (not bakeGlossiness and isGlossiness)

		-- Bake the value if the map is not set but the value is
		if((map == undefined) and (value != undefined)) then
		(
			-- Use OSL Map
			c = colorMap()
			c.gamma = 1.0

			 -- Invert the value of the map if required
			if(shouldBeInverted) then
			(
				value = 1 - value
			)
			
			-- Create grey value based on the value
			glossiness_c = value*255
			glossiness_as_a_color = color glossiness_c glossiness_c glossiness_c 0.0
			c.solidcolor = glossiness_as_a_color	
			
			--set the value on the bake element
			bakeElem.setParamFPValue 1 c
			m = MetaSLProxyMaterial()
			m.shader = c
			bttmap.sceneObject.material = m
		)
		esle if(map!= undefined) then
		(
			 -- Invert the value of the map if required
			 tempMap = map
			if(shouldBeInverted) then
			(
				inv = oslmap()
				inv.OSL_Path = pathConfig.GetDir #maxroot + "OSL\ColorComp.osl"
				inv.Input_map = map
				tempMap = inv
			)
			
			--set the value on the bake element
			bakeElem.setParamFPValue 1 tempMap
			m = MetaSLProxyMaterial()	
			m.shader = tempMap
			bttmap.sceneObject.material = m
		)
	)
	
	on postBake do
	(
		--restore the original material of the object
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgJuT0wLUFsH5cQ4eQBmo8H79lu+n5z8ep3Py
-- CEHvNrbQVwSN9sxJuO3vIwYR5eG3NPQ4aCgv8eP4MyAiMpjYicDN3OVlTuMLcL57
-- 0PWOqg47FFFqkvhBteFAle3uKYSgwwMc6aGhfdOfZa6okBFAJyXTIXiNDX6Z+FvQ
-- 0CZiRWC/Lt9c6KgwAkjSc9Jl89vudFImg8txF0j5DeG/Q8leFhl2pEdhnS3iuMBM
-- xLsEBs3Az/Vak+059x8iJ8COM6Ojii/hJE3uq7LMYOTxiJI6yqoa/x+uDkVMGft3
-- l8jeyviQZ1clPcsXcPXMdkSloV36i04iGKFRSl52TI6ExvRYRoKqrnADIuKjWSN+
-- gn/73e9ql0qt7/9Xlbjec5g3OoVU+cybh3wje1PDfr8OzoHU7U/2h7JeGN61uBIq
-- eQGdA7fRdtZisKZbOmuXzVwkskyHGh6t+VjnSodA1mc9wFNlg5YkpUAOwDzYdVFD
-- w/xqU9ETl7PhHfs+Bz70Q8Y8+DPXQA==
-- -----END-SIGNATURE-----