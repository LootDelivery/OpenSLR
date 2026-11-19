------------------------------------------------------------
-- EMISSIVE MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_Emissive
classID: #(0x3f364fd4L, 0x42193d63L)
name: ~EMISSIVE_NAME~
toolTip: ~EMISSIVE_TOOLTIP~
useGamma: true
(
)

plugin CommonBakeMapOperator ArnoldEmissiveMap
classID: #(0x6a267c01L, 0x6d2b49aaL)
commonMapClassID: CommonMap_Emissive.classid
rendererClassID: #(2980329694L, 2688902778L) --arnold.classid
bakeElementClassID: #(0x49ae3734L, 0x40bc0416L)
(
	on requireSceneChange do
	(
		return false
	)
)

plugin CommonBakeMapOperator ScanlineEmissiveMap
classID: #(0x68a42961L, 0x3d8e15c8L)
commonMapClassID: CommonMap_Emissive.classid
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
		initialMaterial = mtl = bttmap.sceneObject.material
		materialClass = classof initialMaterial
		materialClass = materialClass as string
		thisObject = bttmap.sceneObject
		
		if ( materialClass == "Standardmaterial" ) then 
		(
			if ( initialMaterial.useSelfIllumColor == false ) then 
			(
				c = colorMap()
				col = ( initialMaterial.selfIllumAmount * 255.0 ) / 100.0
				c.solidcolor = ( color col col col )
				m = MetaSLProxyMaterial()
				m.shader = c
				thisObject.material = m
				bakeElem.setParamFPValue 0 c
			)
			else
			(
				if ( initialMaterial.selfIllumMap == undefined ) then
				(
					c = colorMap()
					initialMaterial.selfIllumColor
					c.solidcolor = initialMaterial.selfIllumColor
					m = MetaSLProxyMaterial()
					m.shader = c
					thisObject.material = m
					bakeElem.setParamFPValue 0 c
				)
				else
				(
					m = MetaSLProxyMaterial()
					m.shader = initialMaterial.selfIllumMap
					thisObject.material = m
					bakeElem.setParamFPValue 0 initialMaterial.selfIllumMap
				)
			)
		)
		
		-- Algorithm given by Zap. Need color correction to have the same result as Arnold.
		-- Multiply Emisison Color with Emisison with Effective Luminance.
		if ( materialClass == "PhysicalMaterial" ) then
		(
			multiplyNode = OSLMap()
			multiplyNode.OSLpath = pathConfig.GetDir #maxroot + "OSL\ColorMul.osl"
			multiplyNode2 = OSLMap()
			multiplyNode2.OSLpath = pathConfig.GetDir #maxroot + "OSL\ColorMul.osl"

			-- Multiply Emission with Emisison Color
			--Emission:
			if ( initialMaterial.emissionMap != undefined) then
			(
				multiplyNode.A_map = initialMaterial.emissionMap
			)
			else
			(
				c = colorMap()
				col = 255 * initialMaterial.emission
				c.solidColor = ( color col col col )
				multiplyNode.A_map = c
			)

			--Emission Color:
			if ( initialMaterial.emissionColorMap != undefined) then 
			(
				multiplyNode.B_map = initialMaterial.emissionColorMap
			)
			else
			(
				c = colorMap()
				c.solidColor = initialMaterial.emissionColor
				multiplyNode.B_map = c
			)
			
			-- Multiply (Emission and Emission Color) with Effective Luminance
			-- Effective Luminance:
			b = colorMap()
			colb = initialMaterial.EffectiveLuminance
			b.solidColor = colb
			multiplyNode2.A_map = b

			multiplyNode2.B_map = multiplyNode
			

			m = MetaSLProxyMaterial()
			m.shader = multiplyNode2
			thisObject.material = m
			bakeElem.setParamFPValue 1 multiplyNode2
		)
		
		-- Added color correction to match arnold output as much as possible.
		if (materialClass == "PBRSpecGloss" or materialClass == "PBRMetalRough" ) then 
		(
			if ( initialMaterial.emit_color_map == undefined ) then 
			(
				c = colorMap()
				colr = (sqrt (initialMaterial.emit_color.red))*30
				colg = (sqrt (initialMaterial.emit_color.green))*30
				colb = (sqrt (initialMaterial.emit_color.blue))*30
				c.solidColor = ( color colr colg colb )
						
				m = MetaSLProxyMaterial()
				m.shader = c
				thisObject.material = m
				bakeElem.setParamFPValue 0 c
			)
			else
			(
				divideMode = OSLMap()
				divideMode.OSLpath = pathConfig.GetDir #maxroot + "OSL\ColorDiv.osl"
				
				tempC = colorMap()
				tempC.solidColor = (color 155 155 155)
				
				divideMode.A_map = initialMaterial.emit_color_map
				divideMode.B_map = tempC
				
				m = MetaSLProxyMaterial()
				m.shader = divideMode
				thisObject.material = m
				bakeElem.setParamFPValue 0 divideMode
			)
		)
	)

	on postBake do
	(
		bttmap.sceneObject.material = initialMaterial
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgIb1VjCccFV5EvMWQslbdGEOYdL8NSskN/Aw
-- VS/y5CGbUMMw948ygA2jMpKX81m/uk2++xYsD+4CAzKu46hns4uOmO3z2oK2frYJ
-- aqmWa5AxeZzM8PfhHzOtrTm7j9+AcSCTkm/gBenaV4j0E/ltvvcCZyFc0E9ncYh5
-- nMHcFUrgx5KDioH+3HOxQz2vobFCVM1yxVjfq3GkzQ96yqpEDef5ic2RDYThZzsK
-- aq2x1z3ZnEWd4WkvfQ8BAMYtkfZVBBZDfhX6HNHEtc6EBQE00Lp/X4u8J5njddwv
-- Ss3fb5e9NekNWugODG6UPFRcW6LXflKZrho3/3N0dwiEugyro98ukv3D9omSiQ75
-- ac9Brn1E/tkGZ/8Db7U61Q38rU/mwEmFjP9zI9YFzPAl3zG0q3DpJxW4TGUvBvSf
-- t6XZOQgAT/JukMyaTjtAukOJdn0NWPNiUFw/pHBh5SpcBrEL86e/JRvkGQeYzdLr
-- cOs5vlalfriIN9D05Pk8hdMfXW9dOA==
-- -----END-SIGNATURE-----