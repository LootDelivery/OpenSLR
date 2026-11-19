------------------------------------------------------------
-- Color MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_Color
classID: #(0x4d80f394L, 0x2b3b2564L)
name: ~COLOR_NAME~
toolTip: ~COLOR_TOOLTIP~
useGamma: true
(
)

plugin CommonBakeMapOperator ArnoldColorMap
classID: #(0x4da5ebe9L, 0x249e87b5L)
commonMapClassID: CommonMap_Color.classid
rendererClassID: #(2980329694L, 2688902778L) --arnold.classid
bakeElementClassID: #(1236154164L, 2569562704L) --Arnold_advanced_map.classid
(
	on requireSceneChange do
	(
		return false
	)
	on preBake bakeElem do
	(
		-- setup parameters for Arnold_advanced_map BakeElement
		local paramName = "base_color"
		local shaderName = "standard_surface"
		local objMtlType = classof bttMap.sceneObject.material
		case objMtlType of
		(
			ai_car_paint: 
				(
					shaderName = "car_paint"
				)
			ai_lambert:
				(
					paramName = "Kd_color"
					shaderName = "lambert"
				)
			ai_standard_hair:
				(
					shaderName = "standard_hair"
				)
		)

		bakeElem.setParamFPValue 1 paramName
		bakeElem.setParamFPValue 2 shaderName
	)
)

plugin CommonBakeMapOperator ScanlineColorMap
classID: #(0x162b2177L, 0x6195ce24L)
commonMapClassID: CommonMap_Color.classid
rendererClassID: #(1L, 0L)
bakeElementClassID: #(1177994923L, 3734390495L) --"Override" bake element
(
	on requireSceneChange do
	(
		return true
	)
	local initMtl = undefined
	
	fn getPropFromMtl mtl prop =
	(
		local val = undefined		
		if hasproperty mtl prop then
		(
			val = getProperty mtl prop
		)
		return val
	)
	
	fn getColorFromNode node =
	(
		local col = undefined
		
		mtl = node.material		
		if mtl == undefined then
		(
			-- use wire color for dummy material
			return node.wirecolor
		)
		
		-- add more "color" property to the query list over time
		color_prop_query_list = #(
			-- firstly, color map
			"base_color_map",
			"diffuseMap",
			"paint_map",
			"base_color_shader",
			"Kd_color_shader",
			"color_shader",
			
			-- then, color
			"base_color",
			"basecolor",
			"diffuse",
			"k_d",
			"paint_color",
			"Kd_color",
			"color"
		)

		for prop in color_prop_query_list do
		(
			col = getPropFromMtl mtl prop
			
			if col != undefined then 
			(
				return col
			)
		)
		
		print "Warning: color property was not found from the node!"
		return col
	)	
	
	on preBake bakeElem do
	(
		local inputColorMap = undefined

		-- get the color property from the node
		initMtl = bttMap.sceneObject.material
		col = getColorFromNode bttMap.sceneObject		
		if isKindOf col texturemap then
		(
			inputColorMap = col
		)
		else
		(
			if col == undefined then
			(
				-- continue to bake a black map if no color was found
				col = black
			)
			inputColorMap = colorMap()
			inputColorMap.solidcolor = col
		)

		-- setup the "Override" bake element
		-- todo: move the code below to "Override" bake element and call setParamFPValue directly
		m = MetaSLProxyMaterial()
		m.shader = inputColorMap
		bttMap.sceneObject.material = m
		--bakeElem.setParamFPValue 1 inColorMap
	)
	
	on postBake do
	(
		bttMap.sceneObject.material = initMtl
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgIs95ctBKq0cLOTEX8yaqpaWJJ4iCpzMHSoe
-- Cr/27Jep/Z+Ufm4RB1guw0Z4MPpWsnZl/U+TNr2uPZh4+nvYcVdZ5ZX6L8Heop6V
-- MCciO0wbKjbAqwNYnUgWiqwtNUf3zEBEWypeOYIYh223CJDNYTtNjb0tr+6+N/23
-- pJSxvqA5hUkg5eYs66Pq9NBD+pnMXfcOJPvY6REigyX9KVMvGatzB3obwu05Mz8j
-- 8cqfMOC02C2Y/RLiN5iR7+8yBa/wOOvt8G3W+oVyaFpLoaE0XdfrFiGl0vlE/uHB
-- QGEa7dRgWnzy7RcQgSX3Y9ccu0vMAD64jcJvPXjg6C4+nEJKHEoLOXTHTlD93qAj
-- pqy5NYlthS5zploimOj0M7EsL8AEBk+0e/erD9/vM1BE6GbzB7wBDZ8Y4zYPAliP
-- fsI3Ivxlg4tbJoLSvcylJw4ywWO+3JdHI+gP4UUGDPdxY0+GxFaifst5g3Q3LDIn
-- VEPMZKBkAAFGq2jtcwzSqDZIkZwtNA==
-- -----END-SIGNATURE-----