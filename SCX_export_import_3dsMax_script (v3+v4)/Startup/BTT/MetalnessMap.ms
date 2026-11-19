------------------------------------------------------------
-- METALNESS MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_Metalness
classID: #(0x702d46afL, 0x79b855a0L)
name: ~METALNESS_NAME~
toolTip: ~METALNESS_TOOLTIP~
(
)


plugin CommonBakeMapOperator ArnoldMetalnessMap
classID: #(0x2c890367L, 0x798c120bL)
commonMapClassID: CommonMap_Metalness.classid
rendererClassID: #(2980329694L, 2688902778L) --arnold.classid
bakeElementClassID: #(0x49ae3734L, 0x35c70b72L)
(
	on requireSceneChange do
	(
		return false
	)
)


plugin CommonBakeMapOperator ScanlineMetalnessMap
classID: #(0x79d072dL, 0x3c7436e7L)
commonMapClassID: CommonMap_Metalness.classid
rendererClassID: #(1, 0)
bakeElementClassID: #(1177994923L, 3734390495L)
(
	on requireSceneChange do
	(
		return true
	)
	local initialMaterial = undefined
	on preBake bakeElem do
	(
		--print "On Pre Bake ScanlineMetalnessMap!"
		if bakeElem == undefined do
		(
			print "Warning: bakeElem undefined!"
		)
		
		initialMaterial = mtl = bttMap.sceneObject.material
		
		-- No processing if scene object doesnt have a material
		if (mtl == undefined) then 
			return()

		-- if object has a physical material
		if( hasproperty mtl "metalness_map" ) then 
		(
			if( mtl.metalness_map == undefined ) then 
			(
				--set the color map to the value of the parameter on the material
				if( hasproperty mtl "metalness") then
				(
					c = colorMap()
					col = mtl.metalness*255.0
					c.solidcolor = (color col col col)
					m = MetaSLProxyMaterial()
					m.shader = c
					bttMap.sceneObject.material = m
					bakeElem.setParamFPValue 0 c
				)
			)
			else
			(
				bakeElem.setParamFPValue 0 mtl.metalness_map	
				m = MetaSLProxyMaterial()
				m.shader = mtl.metalness_map		
				$.material = m
			)
		)
		else 
		(
			c = colorMap()
			c.solidcolor = (color 0 0 0)
			m = MetaSLProxyMaterial()
			m.shader = c
			bttMap.sceneObject.material = m
			bakeElem.setParamFPValue 0 c
		)
	)

	on postBake do
	(
		--print "On Post Bake ScanlineMetalnessMap!"
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgAJ2LDVtqMJEA7ekKV7DfqHIKHNlxABwGyMC
-- zHb2J8QIY9FF/VG9ho7lmyyBoO+Bm+BJk1CCahsYci3ltZfDSqa5KOQI0x204CWk
-- o0vvulUQ4wHI2qkSOXXCKTdBujdGzOCNs9HBLYBRfBZuh5obW1fvIAWGqijPv8JT
-- xn2xYETdYL5MubMJzy918xywS6CO62ibJKz1j35ia8AZJ/r0GUIMdk90GKJGXnbU
-- lr82c9b+pGYbgPDXtebusTT5KYtYYvdjxgEmwpGi7uFj8V0PIHwk5MgpXeBQJ2bl
-- c0JJ2mUQzx4kthLtnSSB5E5qtyvO4ZAAa/USRBMVwuQGXOS50WcsdVFphuRJctA4
-- jGQ3aG1HepZ3DFOO0USd7Rh6pvpheT6AmJiSagp0rzvPU5S1LUcfDV6MwV78MlFa
-- ulkkquV1708aD1X2AX/pnK1reWk1e5AGnvGd+ew9c8sPUMdLp0MAb/uPikp5R3l0
-- e8p0VOKSjAimsirw/P8XCjMI51toHg==
-- -----END-SIGNATURE-----