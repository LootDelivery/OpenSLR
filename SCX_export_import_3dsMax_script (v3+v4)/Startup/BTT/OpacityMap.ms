
------------------------------------------------------------
-- Opacity MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_Opacity
classID: #(0x61170b53L, 0x7aa3231L)
name: ~OPACITY_NAME~
toolTip: ~OPACITY_TOOLTIP~
(
	parameters main rollout:params
	(
		PartialOpacity type:#integer default:0 ui:partialOpacityCombo
		Cutout type:#boolean default:false ui: cutoutCheck
		InvertOpacity type:#boolean default:false ui: invertOpacityCheck
	)
	
	rollout params "Opacity Parameters" extendedUI:true
	(
		GridLayout gridLayout columnStretch:#(1,0,2,0,2,1) /* <- add spacer between the cols*/ horzSpacing:6 
		CheckBox cutoutCheck ~OPACITY_CUTOUT~ row:0 column:0
		DropDownList partialOpacityCombo ~OPACITY_PARTIAL_OPACITY~ row:0 column:2 items:#(~OPACITY_OPAQUE~, ~OPACITY_TRANSPARENT~, ~OPACITY_STEP_AT_FIFTY~)
		CheckBox invertOpacityCheck ~OPACITY_INVERT~ row:0 column:4		
							
		on cutoutCheck toggled isChecked do
		(
			partialOpacityCombo.enabled = isChecked;
		)
					
		on params open do
		(
			-- Init controls
			-- Enable partial opacity depending on the cutout
			partialOpacityCombo.enabled = cutoutCheck.checked
			
		)
	)
)

plugin CommonBakeMapOperator ScanlineOpacityMap
classID:#(0x223740b0, 0x465a7ed1)
commonMapClassID: CommonMap_Opacity.classid
rendererClassID: #(1, 0)
bakeElementClassID: #(1177994923L, 3734390495L) --override bake element
(
	on requireSceneChange do
	(
		return true
	)
	local initialMaterial = undefined
	on preBake bakeElem do
	(
		local maxDir = pathConfig.GetDir #maxroot
		initialMaterial = mtl = bttMap.sceneObject.material
		-- No processing if scene object doesnt have a material
		if (mtl == undefined) then 
			return()
		local partialOpacity = 1
		local cutout = true
		local invertOpacity = false
		local opacity_map = undefined
		
		for i = 1 to bttMap.getOptionsCount() do
		(
			local paramName = bttMap.getOptionName(i)
			case paramName of
			(
				"PartialOpacity":
				(
					partialOpacity = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName partialOpacity
				)
				"Cutout": 
				(
					cutout = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName cutout
				)
				"InvertOpacity": 
				(
					invertOpacity = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName invertOpacity
				)
			)
		)	
		
		-- Determine Opacity Map
		if(hasproperty mtl "opacity_map") then 
		(			
			--format "Material has opacity map: '%'" opacity_map
			if(mtl.opacity_map == undefined) then
			(
				if(hasproperty mtl "opacity") then 
				(
					opacity_value_map = oslmap() 
					-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
					opacity_value_map.OSL_Path = maxDir + "\OSL\\" + "SetFloat.osl"
					opacity_value_map.in = mtl.opacity
					--format "Transparency Value:'%'\n" (opacity_value_map.in)							
					opacity_map = opacity_value_map 
				)
				--else ... nothing to show
			)
			else
			(
				opacity_map = mtl.opacity_map
			)	
		)
		else 
		(
			if(hasproperty mtl "cutout_map") then 
			(
				if(mtl.cutout_map == undefined) then
				(
					if(hasproperty mtl "transparency") then 
					(
						--Procedure for creating the map with the opacity value
						opacity_value_map = oslmap() 
						-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
						opacity_value_map.OSL_Path = maxDir + "\OSL\\" + "SetFloat.osl"
						-- Opacity is the inverse of transparency
						opacity_value_map.in = 1 - mtl.transparency
												
						opacity_map = opacity_value_map 
					)
				)
				else
				(
					--Create a OSL map to wrap transparency value
					opacity_value_map = oslmap() 
					-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
					opacity_value_map.OSL_Path = maxDir + "\OSL\\" + "SetFloat.osl"
					-- Opacity is the inverse of transparency
					opacity_value_map.in = 1 - mtl.transparency	
					
					-- Create a map to multiply transparency with cutout map
					object_opacity_map = oslmap() 
					-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
					object_opacity_map.OSL_Path = maxDir + "\OSL\\" + "ColorMul.osl"
					object_opacity_map.A_map = opacity_value_map
					object_opacity_map.B_map = mtl.cutout_map	

					-- Set the map to be processed for options:
					opacity_map = object_opacity_map
				)
			)
		)
		-- Process opacity options
		osl_map = oslmap() 
		-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
		osl_map.OSL_Path = maxDir + "\OSL\\" + "ProcessOpacity.osl"
			
		-- Setup Process opacity options
		osl_map.opacityMap_map = opacity_map						
		osl_map.PartialOpacity = partialOpacity
		osl_map.Cutout = cutout
		osl_map.MapInvert = invertOpacity

		-- set the final output for 3ds Max 			
		opacity_output_map = MultiOutputChannelTexmapToTexmap() 
		opacity_output_map.sourceMap = osl_map 
		opacity_output_map.outputChannelIndex = 1
		
		--set the value on the bake element
		bakeElem.setParamFPValue 1 opacity_output_map
		
		-- set the temporary material for override  
		m = MetaSLProxyMaterial()
		m.shader = opacity_output_map		
		bttMap.sceneObject.material = m					
	)
	
	on postBake do
	(
		-- Revert the object's material
		bttMap.sceneObject.material = initialMaterial		
	)
)

plugin commonBakeMapOperator ArnoldOpacityMap
classID:#(0x354b4b49, 0x13ba651b) 
commonMapClassID: CommonMap_Opacity.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(1236154164L, 1814527600L) --arnold_map_override bake element
(
	on requireSceneChange do
	(
		return true
	)
	on preBake bakeElem do
	(
		local maxDir = pathConfig.GetDir #maxroot
		mtl = bttMap.sceneObject.material
		-- Nothing to process if object doesnt have a material
		if (mtl == undefined) then 
			return()
		local partialOpacity = 1
		local cutout = true
		local invertOpacity = false
		
		for i = 1 to bttMap.getOptionsCount() do
		(
			local paramName = bttMap.getOptionName(i)
			case paramName of
			(
				"PartialOpacity":
				(
					partialOpacity = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName partialOpacity
				)
				"Cutout": 
				(
					cutout = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName cutout
				)
				"InvertOpacity": 
				(
					invertOpacity = bttMap.getOptionValue(i)
					--format "MapOption: % -> '%'\n" paramName invertOpacity
				)
			)
		)
		
		--Determine what will be the opacity map of the object
		opacity_map = undefined
		
		-- Standard material has an opacity map and opacity value, 
		-- PBR only has an opacity map, so nothing to show if map is undefined
		if(hasproperty mtl "opacity_map") then 
		(
			--format "Material has opacity map: '%'" opacity_map
			if(mtl.opacity_map == undefined) then
			(
				if(hasproperty mtl "opacity") then 
				(
					opacity_value_map = oslmap() 
					-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
					opacity_value_map.OSL_Path = maxDir + "\OSL\\" + "SetFloat.osl"
					opacity_value_map.in = mtl.opacity
					-- format "Transparency Value:'%'\n" (opacity_value_map.in)							
					opacity_map = opacity_value_map 
				)
				--else ... nothing to show
			)
			else
			(
				opacity_map = mtl.opacity_map
			)			
			
		)
		-- Arnold Standard Surface
		else if(hasproperty mtl "opacity_shader") then 
		(
			
			if(mtl.opacity_shader == undefined) then
			(
				--Procedure for creating the color map using the opacity 
				color_value_map = oslmap() 
				-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
				color_value_map.OSL_Path = maxDir + "\OSL\\" + "SetColor.osl"
				color_value_map.in = mtl.opacity_color						
				opacity_map = color_value_map 
			)
			else
			(
				opacity_map = mtl.opacity_shader
			)
		)
		-- Physical Material
		else if(hasproperty mtl "cutout_map") then 
		(
			if(mtl.cutout_map == undefined) then
			(
				if(hasproperty mtl "transparency") then 
				(
					--Procedure for creating the map with the opacity value
					opacity_value_map = oslmap() 
					-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
					opacity_value_map.OSL_Path = maxDir + "\OSL\\" + "SetFloat.osl"
					-- Opacity is the inverse of transparency
					opacity_value_map.in = 1 - mtl.transparency
					--format "Transparency Value:'%'\n" (transparency_value_map.in)
						
					opacity_map = opacity_value_map 
				)
			)
			else
			(
				--Create a OSL map to wrap transparency value
				opacity_value_map = oslmap() 
				-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
				opacity_value_map.OSL_Path = maxDir + "\OSL\\" + "SetFloat.osl"
				-- Opacity is the inverse of transparency
				opacity_value_map.in = 1 - mtl.transparency	
					
				-- Create a map to multiply opacity with cutout map
				object_opacity_map = oslmap() 
				-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
				object_opacity_map.OSL_Path = maxDir + "\OSL\\" + "ColorMul.osl"
				-- Is it the transparency value that should be used ?
				object_opacity_map.A_map = opacity_value_map
				object_opacity_map.B_map = mtl.cutout_map	

				-- Set the map to be processed for options:
				opacity_map = object_opacity_map
			)			
		)
		-- Process opacity options
		osl_map = oslmap() 
		-- Note: Setting the osl file automatically "compiles" the code and make the parameters inside it available
		osl_map.OSL_Path = maxDir + "\OSL\\" + "ProcessOpacity.osl"
			
		-- Setup Process opacity options
		osl_map.opacityMap_map = opacity_map						
		osl_map.PartialOpacity = partialOpacity
		osl_map.Cutout = cutout
		osl_map.MapInvert = invertOpacity

		-- set the final output for 3ds Max 			
		opacity_output_map = MultiOutputChannelTexmapToTexmap() 
		opacity_output_map.sourceMap = osl_map 
		opacity_output_map.outputChannelIndex = 1
		
		-- Set the value on the bake element
		bakeElem.setParamFPValue 1 opacity_output_map
		
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgEDnAvN1kXNgfZbvZ4lCysTgbM3o+KMOj/4z
-- iDAvag06ngtVujro0LLNmqaSIPcFKb9wH9yhnEruX3QMtVe0sK38xN7wSFLKtsuS
-- JGbisPNMLgsejM11p9hwETZcp1BB7FpTG8lvyEjYJcS+YfBh4qd7PyUswttJQ3lw
-- lEdHpLf+y5EnuN2KKIOWau+lMJGAj7YIDVjUoTjt8+QmyN/shsY3n4+ouMFfQ7wY
-- AdPUfwfaIgIBD6XKmOLESZlKNqRssKoxGUJWfijxn0Aaf8N5jztwOXQMXExBbif6
-- +kKgsMXjA7TjDulysKGCYIyyJFSbxJC7ZkmBkNqIxJNQylYXDBUrBzqph3mlAeeW
-- EyfpjzfeDKIZflB8amo32fMSYxJ4eL2BXqg8AbaPWPACzaIfPnYI4aBE3sdEyVv8
-- 0W+NH9/pfb3PgLReTIv/NQWYMyl7/9/BT55kS32iPfx4bLuBPgmOEoorlVbZHhGy
-- Y3CDngo6QWJQ8pqsQrYDKu//IQHtZg==
-- -----END-SIGNATURE-----