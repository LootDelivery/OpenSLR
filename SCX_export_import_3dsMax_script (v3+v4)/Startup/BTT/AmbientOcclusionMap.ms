------------------------------------------------------------
-- AMBIENT OCCLUSION MAP 
------------------------------------------------------------
plugin CommonBakeMap CommonMap_AmbientOcclusion
classID: #(0x685b703cL, 0x55a14bc7L)
name: ~AMBIENTOCCLUSION_NAME~
toolTip: ~AMBIENTOCCLUSION_TOOLTIP~
useGamma: true
(
	parameters main rollout:params
	(
		darkColor type:#color default:black ui:darkColorPicker
		brightColor type:#color default:white ui:brightColorPicker
		
		samples type:#integer default:16 ui:samplesSpinBox
		nearClip type:#float default:0 ui:nearClipSpinBox
		farClip type:#float default:100 ui:farClipSpinBox
		
		spread type:#float default:0.8 ui:spreadSpinBox
		falloff1 type:#float default:1 ui:falloff1SpinBox
	)
	
	rollout params "Ambient Occlusion Parameters" extendedUI:true
	(
		GridLayout gridLayout columnStretch:#(0,0,1,0,0,1,0,0,1) /* <- add spacer between the cols*/ horzSpacing:6 
		
		ColorPicker darkColorPicker ~AMBIENTOCCLUSION_DARK_COLOR~ row:0 column:0
		ColorPicker brightColorPicker ~AMBIENTOCCLUSION_BRIGHT_COLOR~ row:1 column:0
		
		Spinner samplesSpinBox ~AMBIENTOCCLUSION_SAMPLES~ type:#integer range: [0,100000,0] row:0 column:3
		Spinner nearClipSpinBox ~AMBIENTOCCLUSION_NEAR_CLIP~ type:#float range: [0,100000,0] row:1 column:3
		Spinner farClipSpinBox ~AMBIENTOCCLUSION_FAR_CLIP~ type:#float range: [0,100000,0] row:2 column:3
		
		Spinner spreadSpinBox ~AMBIENTOCCLUSION_SPREAD~ range: [-100000,100000,0.8] row:0 column:6
		Spinner falloff1SpinBox ~AMBIENTOCCLUSION_FALLOFF~ range: [-100000,100000,1] row:1 column:6
		
	
		on params open do
		(
			darkColorPicker.minimumWidth = darkColorPicker.height*3
		)
	)
)

plugin commonBakeMapOperator ArnoldAmbientOcclusionMap
classID:#(0x260626edL, 0x27ff60a1L)
commonMapClassID: CommonMap_AmbientOcclusion.classid
rendererClassID: #(2980329694L, 2688902778L)
bakeElementClassID: #(1236154164L, 1814527600L) --arnold_map_override bake element
(
	on requireSceneChange do
	(
		return false
	)
	local initialMaterial = undefined
	on preBake bakeElem do
	(
		local darkColor = bttmap.getOptionValue(1)
		local brightColor = bttmap.getOptionValue(2)
		
		local samples = bttmap.getOptionValue(3)
		local nearClip = bttmap.getOptionValue(4)
		local farClip = bttmap.getOptionValue(5)
		
		local spread = bttmap.getOptionValue(6)
		local falloff1 = bttmap.getOptionValue(7)

		ao = ai_ambient_occlusion()
		
		ao.white = brightColor
		ao.black = darkColor
		
		ao.samples = sqrt(samples)
		ao.far_clip = farClip
		ao.near_clip = nearClip
		
		ao.spread = spread
		ao.falloff = falloff1
		
		ao.normal = (point3 0 0 0)
		ao.invert_normals = false
		ao.trace_set = ""
		ao.inclusive = true
		ao.self_only = false

		--set the value on the bake element
		bakeElem.setParamFPValue 1 ao			
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgE+hxdj2a/pUJo8Jj8uac0+PeF2K4cHZtbX4
-- uCz1crdLfQVEKAWwduR6F1XboH+Q3oNOvO8G1ETLiN5mIZ2YqR3AzK5j6uCKf1JO
-- eO2CIR/t0DFuFmOAKk7xQN2YYsgayzqRRkX3cIdyKOP9har/IyU1GqkrGYiplPze
-- WV4xhj8jCrWwFlY88FY49B++6bHOiJiUm94Wna1ICJ+sQj081F1DH4cjy2p/+y/3
-- E5ShSIoZeoVMraKVlTUCR7JBO/z1GXrQjszNUXdlWmqTplSQ6NglNL2uld2JfRTl
-- 963ujsOK9AQcc1h9JCweaWYKkfuppiQInd9JHG2pYuC168n8oVX+Ep9EJ3oHdXaK
-- Y7Tyiwy6c/s8bF7i8A+BUw3khM4d+48l0fDEuVc0/b71LyeSdLzMBaOXxDmenHxh
-- cdawoNgnq+OaG2tMD83j2r+krpIQlk+QNrgFsEvDASmpD1GYLeiPBdRmQ1IUItGn
-- 2RUEj3TBreCPwAOcmq4XCL6v4q3mbw==
-- -----END-SIGNATURE-----