------------------------------------------------------------
-- Material ID MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_MaterialID
classID: #(0x8224ba7L, 0x2ac80f05L)
name: ~MATERIALID_NAME~
toolTip: ~MATERIALID_TOOLTIP~
(
)

plugin CommonBakeMapOperator ScanlineMaterialIDMap
classID:#(0x37ed7c34L, 0x2b6b2014L)
commonMapClassID: CommonMap_MaterialID.classid
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
		
		mId = OSLMap()
		mId.OSLpath = pathConfig.GetDir #maxroot + "OSL\GetMtlID.osl"

		--set the value on the bake element
		bakeElem.setParamFPValue 1 mId
		
		-- set the temporary material 
		m = MetaSLProxyMaterial()
		m.shader = mId		
		bttmap.sceneObject.material = m		
	)
	
	on postBake do
	(
		bttmap.sceneObject.material = initialMaterial		
	)
)

plugin commonBakeMapOperator ArnoldMaterialIDMap
classID:#(0x648b563fL, 0x4c5b43cdL)
commonMapClassID: CommonMap_MaterialID.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(1236154164L, 1814527600L) --arnold_map_override bake element
(
	on requireSceneChange do
	(
		return false
	)
	on preBake bakeElem do
	(
		mId = OSLMap()
		mId.OSLpath = pathConfig.GetDir #maxroot + "OSL\GetMtlID.osl"

		--set the value on the bake element
		bakeElem.setParamFPValue 1 mId				
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgBDa0TeGo0QuaGhIwHvDNYXNFePz6BsLFZz0
-- c3OXlTzfN/sg/DI+/bfuIRW8veRzYfuKBHH7xNwMSRKlfftz4IF4XtzIFBbryn2r
-- ZqoEwGTOkn1gkKch8PWHIM82aRUkKOdeyGRjRiS2Yu/qGqJ04IAoHuDxRI2shlws
-- XN65w+8HGJ1BRL/W18uip4lqW1Vf8cFzdJiTOy9zsbwRTqLKMpuxXXwuSGVMzOOy
-- b4yKuGAJQUbIGeYXYcg2Oc2elPVrl1j/JPNFgK+BEOJqemgt1tbJu7F7jsoDjYaG
-- fDT8rJRfwsDbQ8ZYb/LPZ7HWqGrmEDmQQa47hDCT2tiqss9QHdwl/7oqvE9omj5S
-- rzPIx7lE2+jABOTu/01d6QRht35bPEFX0pp2v9Jfz/m+Xl0++F8xZyfG+O3tL3XE
-- 7JiLLhUapOgCMla4znNaiBLqoYk5Pj5F+TGRHCsS3tnSW9sVT4v3SF2fk14ZyIl0
-- Z7hN6sZeu/nA9B0q5cTaiGTxY9SQ0w==
-- -----END-SIGNATURE-----