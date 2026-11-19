-- A scripted implementation of a PBR material frontend in the Specular/Gloss mode
-- Material_PBRSpecGloss.ms, by Zap Andersson and Juan Martinez
-- Modified: 2022-12-14
-- Copyright 2022 Autodesk Inc, All rights reserved. This file is licensed under Apache 2.0 license
-- You are free to modify this file to make derivative versions to match your workflow.
-- Remember to change the ClassID if you do...
--    https://github.com/ADN-DevTech/3dsMax-OSL-Shaders/blob/license/LICENSE.txt

plugin material PBRSpecGloss
	name:~PBR_MATERIAL_SPGL~
	nonLocalizedName:"PBR Material (Spec/Gloss)"
	classID:#(0xd00f1e00L, 0x01dbad33L)
	extends:PhysicalMaterial replaceUI:true version:2019.1215
(
	local params
	local settings
	
	fn setGamma1 map = 
	(
		if (classof map == bitmapTex) do
		(
			-- Accessing .bitmap of a missing map yeilds an exception
			try (				
				if (map.bitmap != undefined) do 
				(
					if (map.bitmap.gamma != 1.0) do 
					(
						local gamma1bmp = openBitmap map.bitmap.filename gamma:1.0 colorspace: ColorPipelineMgr.DataColorSpace
						if (gamma1bmp != undefined) do map.bitmap = gamma1bmp
					)					
				)				
			)	
			catch() -- do-nothing catch
		)
		if (classOf map == OSLMap) do 
		(
			if (hasProperty map "autoGamma") do 
				map.autogamma = false
				
			if (hasProperty map "manualGamma") do 
				map.manualGamma = 1.0
			
			-- Set any parameter ending in "_ColorSpace" to "" (Raw)
			local props = getPropNames map
			for prop in props do if ((findString (prop as string) "_ColorSpace") != undefined) do setProperty map (prop as string) ""
		)
		if (classOf map == MultiOutputChannelTexmapToTexmap) do
		(
			setGamma1(map.sourceMap)
		)
	)
	
	fn transmap = (
		-- Is the map there, and already connected to the OSL map - use it
		if (delegate.roughness_map != undefined) do
		(
			if (delegate.roughness_map.sourceMap != undefined) do
				return delegate.roughness_map.sourceMap
		)
		-- If not - create it

		local code = 
		"shader SpecGlossToPhysical
		(
			color        In     = 0.02,
			float        Gloss  = 1.0,
			float        AO     = 1.0,
			int          Invert = 1,
			output float Reflectivity = 0.0,
			output color ReflColor = 0.0,
			output float Roughness = 0.0
		)
		{
			Roughness = Invert?1.0 - Gloss:Gloss;
	
			Reflectivity = max(max(In[0],In[1]),In[2]);
			if (Reflectivity > 0.0)
				ReflColor = In / Reflectivity;

			float fres   = pow(1-abs(dot(I, N)),5.0) * (1.0-Roughness);
			Reflectivity = mix(Reflectivity, 1.0, fres);
			ReflColor    = mix(ReflColor, 1.0, fres) * AO;
		}"

		local tm = OSLmap()
		tm.OSLCode = code			
		
		local mul = MultiOutputChannelTexmapToTexmap()
		
		mul.sourceMap = tm
		mul.outputChannelIndex = 1
		delegate.reflectivity_map = mul

		mul = MultiOutputChannelTexmapToTexmap()
		mul.sourceMap = tm
		mul.outputChannelIndex = 2
		delegate.refl_color_map = mul

		mul = MultiOutputChannelTexmapToTexmap()
		mul.sourceMap = tm
		mul.outputChannelIndex = 3
		delegate.roughness_map = mul
		return tm
	)

	parameters settings rollout:settings
	(
		ao_affects_diffuse     type:#boolean ui:aoDiffuse default:true
		ao_affects_reflection type:#boolean ui:aoReflect default:true
		normal_flip_red         type:#boolean ui:normFlipR default:false
		normal_flip_green      type:#boolean ui:normFlipG default:false
	)
	
	
    parameters main rollout:params 
    (
        basecolor type:#frgba default:white ui:cpCol 
        base_color_map type:#TextureMap ui:mapBaseColor localizedname:~PBR_MATERIAL_BASECOL_MAP~
        specular type:#frgba default:[0.04,0.04,0.04,1.0] ui:cpSpecular
        specular_map type:#TextureMap ui:mapSpecular localizedname:~PBR_MATERIAL_SPECULAR_MAP~
        glossiness type:#float default:0.0 ui:spnGlossiness
        glossiness_map type:#TextureMap ui:mapGlossiness localizedname:~PBR_MATERIAL_GLOSSINESS_MAP~
        useGlossiness type:#integer default:1
        ao_map type:#TextureMap ui:mapAmbientOcclusion localizedname:~PBR_MATERIAL_AO_MAP~
        bump_map_amt type:#float default:1.0 ui:spnBumpAmount
        norm_map type:#TextureMap ui:mapNormal localizedname:~PBR_MATERIAL_NORMAL_MAP~ nonlocalizedname:"Normal Map"
        emit_color type:#frgba default:black ui:cpEmCol
        emit_color_map type:#TextureMap ui:mapEmitColor localizedname:~PBR_MATERIAL_EMISSION_MAP~ nonlocalizedname:"Emission Map"
        opacity_map type:#TextureMap ui:mapOpacity localizedname:~PBR_MATERIAL_OPACITY_MAP~
        displacement_amt type:#float default:1.0 ui:spnDispAmount
        displacement_map type:#TextureMap ui:mapDisplace localizedname:~PBR_MATERIAL_DISPLACE_MAP~

        on basecolor set val do (
			delegate.base_color = val
			
			if (basecolor != white) then
			(
				local mul = OSLMap()
				mul.oslCode = "shader mul (color a = 1, color b = 1, output color out = 1) { out = a * b; }"
				mul.a = basecolor
				mul.b_map = base_color_map
				delegate.base_color_map = mul
			)
			else
			(
				delegate.base_color_map = base_color_map
			)
		)
        on base_color_map set val do (
			if (basecolor != white) then
			(
				local mul = OSLMap()
				mul.oslCode = "shader mul (color a = 1, color b = 1, output color out = 1) { out = a * b; }"
				mul.a = basecolor
				mul.b_map = base_color_map
				delegate.base_color_map = mul
			)
			else
			(
				delegate.base_color_map = base_color_map
			)
		)
        on specular set val do (
			local x = transmap()
			x.In = val
		)
        on specular_map set val do (
			local x = transmap()
			x.In_map = val
		)
        on glossiness set val do 
		(
			local x = transmap()
			x.Gloss = val
		)
        on glossiness_map set val do (
			setGamma1(val)
			local x = transmap()
			x.Gloss_map = val
		)
		
        on useGlossiness set val do (
			local x = transmap()
			x.Invert = (val == 1)
		)
		
        on ao_map set val do (
			setGamma1(val)
			
			if (ao_affects_diffuse) then
				delegate.base_weight_map = val		
			else
				delegate.base_weight_map = undefined
			
			local x = transmap()
			
			if (ao_affects_reflection) then
				x.AO_map = val
			else
				x.AO_map = undefined
		)
        on opacity_map set val do (
			setGamma1(val)
			delegate.cutout_map = val
		)
        on displacement_map set val do (
			setGamma1(val)
			delegate.displacement_map = val
		)
        on bump_map_amt set val do delegate.bump_map_amt = val
		on displacement_amt set val do delegate.displacement_map_amt = val
        on norm_map set val do (
			if (val != undefined) then (
				setGamma1(val)
				local bm = Gnormal()
				bm.normal_map = val
				bm.flipred    = normal_flip_red
				bm.flipgreen = normal_flip_green
				delegate.bump_map = bm
			)
			else
			(
				delegate.bump_map = undefined
			)
		)
        on emit_color set val do delegate.emit_color = val
        on emit_color_map set val do delegate.emit_color_map = val
    )
	
    rollout params ~PBR_MATERIAL_PARAMSPEC~
    (
		edittext lvl readOnly:true text:~PBR_MATERIAL_INFO~ height:60
		group ~PBR_MATERIAL_MTLPARS~ (	
			colorpicker cpCol ~PBR_MATERIAL_BASECOL~ color:white across:2 fieldWidth:45 offset:[-45,6] align:#right
			mapbutton mapBaseColor "" align:#right width:190 offset:[-5,5]

			colorpicker cpSpecular ~PBR_MATERIAL_SPECULAR~ color:white across:2 fieldWidth:45 offset:[-45,3] align:#right
			mapbutton mapSpecular "" align:#right width:190 offset:[-5,2]

			spinner spnGlossiness ~PBR_MATERIAL_GLOSSINESS~ range:[0.0,1.0,0.0] scale:0.01 across:2 fieldWidth:35 offset:[-45,5] align:#right
			mapbutton mapGlossiness "" align:#right width:190 offset:[-5,2]

			label lblAO ~PBR_MATERIAL_AO~ across:2 align:#right offset:[-45,5]
			mapbutton mapAmbientOcclusion "" align:#right width:190 offset:[-5,2]

			spinner spnBumpAmount ~PBR_MATERIAL_NORMAL~ range:[-5.0,5.0,1.0] scale:0.01 fieldwidth:35 across:2 offset:[-45,5] align:#right
			mapbutton mapNormal "" align:#right width:190 offset:[-5,2]

			colorpicker cpEmCol ~PBR_MATERIAL_EMISSION~ color:black across:2 fieldWidth:45 offset:[-45,3] align:#right
			mapbutton mapEmitColor "" align:#right width:190 offset:[-5,2]

			spinner spnDispAmount ~PBR_MATERIAL_DISPLACE~ range:[0.0,999.0,1.0] scale:0.01 fieldwidth:35 across:2 offset:[-45,5] align:#right
			mapbutton mapDisplace "" align:#right width:190 offset:[-5,2]
			
			label lblOpac ~PBR_MATERIAL_OPACITY~ across:2 across:2 align:#right offset:[-45,5] height:28
			mapbutton mapOpacity "" align:#right width:190 offset:[-5,2]
	
		)
    )
	
	rollout settings ~PBR_MATERIAL_SETTINGS~
	(
		label lblSurface ~PBR_MATERIAL_SURFSMOOTH~ across:2 align:#left
		radiobuttons rdoGlossiness labels:#(~PBR_MATERIAL_GLOSSINESS2~, ~PBR_MATERIAL_ROUGHNESS2~) default:1 align:#left
		checkbox aoDiffuse ~PBR_MATERIAL_AO_AFFECTS_DIFFUSE~ offset:[20,0] across:2
		checkBox normFlipR ~PBR_MATERIAL_FLIP_R~ align:#left 
		checkbox aoReflect ~PBR_MATERIAL_AO_AFFECTS_REFLECT~ offset:[20,0] across:2
		checkBox normFlipG ~PBR_MATERIAL_FLIP_G~ align:#left 		
			label lblSpc "" height:2	
		
        fn setUIText =
        (
            if useGlossiness == 1 then
            (
                params.spnGlossiness.caption = ~PBR_MATERIAL_GLOSSINESS~
            )
            else
            (
                params.spnGlossiness.caption = ~PBR_MATERIAL_ROUGHNESS~
            )
        )
		
        on settings open do
        (
            rdoGlossiness.state = useGlossiness
            setUIText()
        )

        on rdoGlossiness changed state do
        (
            useGlossiness = state
            setUIText()
        )
		
		on aoDiffuse changed state do ao_map = ao_map
		on aoReflect changed state do ao_map = ao_map
		on normFlipR changed state do norm_map = norm_map			
		on normFlipG changed state do norm_map = norm_map			
	)

    on create do
    (
		-- Switch to Advanced material mode
	    enableHardwareMaterial delegate true
		showHWTextureMap delegate true
		-- Switch to "High Quality" mode
		--actionMan.executeAction -844228238 "12"

        delegate.base_color = white
        delegate.metalness = 0.0
		delegate.trans_ior = 50.0
        delegate.roughness = 0.0
        delegate.bump_map_amt = 1.0
        delegate.roughness_inv = false
        delegate.reflectivity = 1.0
        delegate.emit_color = black
        delegate.emit_color_map_on = true
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgANdVpT/scrQT25Oaw+nZblmniCLSWXMatkh
-- ejZhf1DuEnqzBXPQwwf0BE6G2ijjYebQH665+s3Gbv5nVv8W6XOGMpD16gAunaWx
-- PX2+4Vkym2Mkpyr30BGUDal30X9YSYbXLRnTPL0EIKWqrJXFp3578USffWgdZZz7
-- J9YAtqwMm+4CMScKO6vAMTQO8yV+UrYWAWtSRlyWvvqwqAafYLx50p/sJOQ5q69R
-- l6Bi+amHdwO3bOqk/J2O63flstjfeD6C/XSgZnxhJhF+TBZKiQlNTgCmh3R5uvrk
-- pP9XlmgdAWADPl5EguAHaxHPbMn+EFQagHb9vZBf9sph16uj9ifvv/uvuhAMtl2D
-- zgEejSh9nb7bVVqgDMUC3MF4a3yCMQ3dmY5LObwAfvx4G2duA4OXpdKRDVA+obgr
-- 5IeczVqaILOqTLCZ1RA849fZg/dzkJ9yVPFnlWRaZDy7CVnOq9ner292NeQmf4Rq
-- GeIGsUro3TJFkx8GmkNyDR6zijcTKw==
-- -----END-SIGNATURE-----