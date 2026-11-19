------------------------------------------------------------
-- VertexColor MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_VertexColor
classID: #(0x3e5c6fe3L, 0xc1618d2L)
name: ~VERTEXCOLOR_NAME~
toolTip: ~VERTEXCOLOR_TOOLTIP~
useGamma: true
(
	parameters main rollout:params
	(
		subChannel type:#integer default:0 ui:subChannelCombo
	)
	
	rollout params "Vertex Color Parameters" extendedUI:true
	(
		GridLayout gridLayout columnStretch:#(0,0,1) /* <- add spacer between the cols*/ horzSpacing:6

		DropDownList subChannelCombo ~VERTEXCOLOR_SUB_CHANNELS~ row:0 column:0 items:#(~VERTEXCOLOR_ALL~, ~VERTEXCOLOR_RED~, ~VERTEXCOLOR_GREEN~,~VERTEXCOLOR_BLUE~)
		Label warningLabel ~VERTEXCOLOR_WARNING~ visible:false row:1 column:0 columnSpan:3
			
		on params open do
		(		
			-- Display warning message if there is no vertex with vertex color
			try
			(
				warningLabel.visible = not (meshop.getNumMapVerts bttMap.sceneObject.mesh 0 != 0)
			)
			catch
			(
				warningLabel.visible = true
			)	
		)
	)
)

plugin CommonBakeMapOperator ScanlineVertexColorMap
classID:#(0x6bd8281dL, 0x5ce572bL)
commonMapClassID: CommonMap_VertexColor.classid
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
		initialMaterial = mtl = bttmap.sceneObject.material
		
		local subChannel = bttmap.getOptionValue(1)
		
		v = VertexColor()
		v.map = 0
		v.subid = subChannel

		--set the value on the bake element
		bakeElem.setParamFPValue 1 v
		
		-- set the temporary material 
		m = MetaSLProxyMaterial()
		m.shader = v		
		bttmap.sceneObject.material = m		
	)
	
	on postBake do
	(
		bttmap.sceneObject.material = initialMaterial		
	)
)

plugin commonBakeMapOperator ArnoldVertexColorMap
classID:#(0x478a072fL, 0x41d1486cL)
commonMapClassID: CommonMap_VertexColor.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(1236154164L, 1814527600L) --arnold_map_override bake element
(
	on requireSceneChange do
	(
		return true
	)
	local initialCompatibilityMode = undefined
	on preBake bakeElem do
	(
		initialCompatibilityMode = renderers.current.compatibility_mode
		renderers.current.compatibility_mode = 1
		
		local subChannel = bttmap.getOptionValue(1)

		v = VertexColor()
		v.map = 0
		v.subid = subChannel

		--set the value on the bake element
		bakeElem.setParamFPValue 1 v				
	)

	on postBake do
	(
		renderers.current.compatibility_mode = initialCompatibilityMode        
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgH/UJVt7HgYRyi+7l9W1BM3yaiRRzs6WJiwf
-- EqfIC9CJO9EraT2zCuUdTIiz3OL9n/vDaoAe3+qfDSTw+Ho/BWATFGk07h5D6UcA
-- abTYgeOmiPnWH146TCZwY6M9u2f50yf2N5BVY7GhupQNiXqDWv4IF5r2sWnD21Fx
-- ezNdEihPmr9ls09g6G4s3OHH7kOfoQOmwW1dIHqDlALGfx5jn3D8t8+X946U8eEA
-- cotTJyyiwp5o6NynzfsmAZu2LqI0/olyZ6EiFT/Qwkrt7crOGVw0HsBgHPddkf0O
-- VT9b1wMPuYEYRxS80hPPANMOde7snVCyXswJpFBRr1HMbM2sWhDlkLCcHgesN8AD
-- 9qZyoOtB541VJ+MIo2KTgcMtKQMH48GHk5I9B6fqzF0DMs8USyePnF/FtQzVAYXd
-- F2Rlm0JYOaQPp4rJ6LPn3Edr7SkjXwkWQi7PQs7zN2BXJhakbSQH0avqbY8rW3T3
-- pLzPebq7W5+gMP7kMOBBXZZBIO8IOg==
-- -----END-SIGNATURE-----