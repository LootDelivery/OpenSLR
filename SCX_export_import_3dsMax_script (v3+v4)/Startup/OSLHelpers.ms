--
-- Contains helper functions for the OSL maps actionButton actionID's
--
struct OSLHelpers (
	--  Get the current map, in whichever material editor it happens to be....
	fn CurrentMap = 
	(
		if mateditor.mode == #basic then
			medit.GetCurMtl()
		else
			sme.getmtlinparameditor()
	),
	--- Completely generic layering. 
	--- Will search for //====LAYER-BEGIN==== and //====LAYER-END==== in the OSL code
	--- and will add the LAYER(n) macro as many times as the Layers parameter in the OSL Shader
	--- says. It is up to the OSL code to #define, use, and #undef the macro before / after each
	--- section to do whatever is needed to make the dynamic shader work	
	fn UpdateGenericLayering up = 
	(
		local o = CurrentMap()
		
		if (up ==  1 and o.layers < 99) do o.Layers = o.Layers + 1
		if (up == -1 and o.layers >  1) do o.Layers = o.Layers - 1

		-- Since we are editing the source we need to turn off AutoUpdate
		o.OSLAutoUpdate = false
		local s = o.OSLCode
		--- The things searched for in the OSL code
		local startsearch = "//====LAYER-BEGIN===="
		local endsearch   = "//====LAYER-END===="

		-- The part of the string we have already processed
		local processed = ""
		local start = 0
		local end   = 0
	
		start = findString s startsearch
		end  = findString s endsearch
	
		while (start != undefined and end != undefined) do (			
			start = start + startsearch.count			
			before = (substring s 1 start)			
				
			local i = 0
			for i = 1 to o.Layers do
			(
				before = before + "\tLAYER(" + (i as string)  + ")\n" 
			)
			
			-- tuck away whatever we processed in the processed varuable
			processed = processed + before			
			processed = processed + (substring s (end-1) (endsearch.count+1))
			
			-- Set s to rest of string
			s = (substring s (end + endsearch.count) -1)		
			
			-- Search again
			start = findString s startsearch
			end = findString s endsearch
		)		
		-- Add whatever is left in s to processed
		processed = processed + s		
		-- Set the OSL Code to 'processed'
		o.OSLCode = processed
	),	
	--- Gradient need some special sauce
	fn UpdateGradientShader up = 
	(
		local alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" 
		local o = CurrentMap()
		
		if up == 1  and o.layers < 26 do o.layers = o.layers + 1
		if up == -1 and o.layers > 2 do o.layers = o.layers - 1
		
		o.OSLAutoUpdate = false
		local s = o.OSLCode
		local search = "//====PAR-BEGIN===="
		local start = findString s search
		local end   = findString s "//====PAR-END===="
		if (start > 0) then (
			start = start + search.count
			before = (substring s 1 start)
			local i = 0
			for i = 1 to o.Layers do
			(
				local fraction = (i-1.0) / (o.Layers-1.0)
				local letter = substring alphabet i 1
				before = before + "\tPAR(" + letter + ", " + (fraction as string) + ",  color(" + (abs(cos(i * 15)) as string) + ", " + (abs(sin(i *20)) as string) + ", " + (abs(sin(i*10)) as string) +  "))\n" 
			)
			
			before = before + (substring s (end-1) -1)		
			
			s = before
		)

		search = "//====INT-BEGIN===="
		start = findString s search
		end   = findString s "//====INT-END===="
		if (start > 0) then (
			start = start + search.count
			before = (substring s 1 start)
			local i = 0
			for i = 1 to o.Layers-1 do
			(
				local letter = substring alphabet i 1
				local letter2 = substring alphabet (i+1) 1
				before = before + "\tINT(" + letter + ", " + letter2 + ")\n" 
			)
			
			before = before + (substring s (end-1) -1)		
			
			s = before
		)
		
		o.OSLCode = s
	),
	---- Composite needs some seriously special sauce :)
	fn UpdateCompositeShader up = 
	(
		local o = CurrentMap()
		
		if (up == 1  and o.layers < 50) do o.layers = o.layers + 1
		if (up == -1 and o.layers > 2)  do o.layers = o.layers - 1
		
		o.OSLAutoUpdate = false
		local s = o.OSLCode
		local search = "//====PAR-BEGIN===="
		local start = findString s search
		local end   = findString s "//====PAR-END===="
		if (start > 0) then (
			start = start + search.count
			before = (substring s 1 start)
			local i = 0
			for i = 1 to o.Layers do
			(
				before = before + "\tPAR(" + ((o.Layers - i + 1) as string)  + ")\n" 
			)
			
			before = before + (substring s (end-1) -1)		
			
			s = before
		)

		search = "//====USE-BEGIN===="
		start = findString s search
		end   = findString s "//====USE-END===="
		if (start > 0) then (
			start = start + search.count
			before = (substring s 1 start)
			local i = 0
			for i = 1 to o.Layers do
			(
				before = before + "\tUSE(" + (i as string)  + ")\n" 
			)
			
			before = before + (substring s (end-1) -1)		
			
			s = before
		)
		
		o.OSLCode = s
	),
	--- Helper to swapping stuff in Composite map
	fn SwapProperty o F T = 
	(
		local temp = getProperty o T
		setProperty o T (getProperty o F)
		setProperty o F temp
	),
	--- Swap rows in Composite map
	fn RearrangeCompositeShader n dir = 
	(
		local o = CurrentMap()
		local layers = o.layers	
		local dest = n + dir
		
		if (dest >= 0 and dest <= layers) do
		(
			-- from
			local F = n as string
			-- to
			local T = dest as string 
				
			SwapProperty o ("RGB" + F) ("RGB" + T)
			SwapProperty o ("RGB" + F + "_map") ("RGB" + T + "_map")
			SwapProperty o ("A" + F) ("A" + T)
			SwapProperty o ("A" + F + "_map") ("A" + T + "_map")
			SwapProperty o ("O" + F) ("O" + T)
			SwapProperty o ("O" + F + "_map") ("O" + T + "_map")
			SwapProperty o ("Enable" + F) ("Enable" + T)
			SwapProperty o ("BlendMode" + F) ("BlendMode" + T)
			SwapProperty o ("Premult" + F) ("Premult" + T)
		)	
	),
	fn removePointController p = 
	(
		local o = CurrentMap()
		
		local s = p as string
		-- Point property name
		local pprop = "LightPos" + s
		-- Button property name
		local nprop = "LightHlp" + s

		-- Get the name in the button
		local n = getProperty o nprop	
		
		local ctrl = getPropertyController o pprop

		-- Remember current position
		local pos  = getProperty o pprop
		
		local myHelper = undefined

		try (
			-- Try to get the node from the controller, if possibru
			myHelper = ctrl.getNode("Obj")
		)
		catch
		(
			-- See if this is a node
			myHelper = getNodeByName n
		)
		
		if (myHelper != undefined) do 
		(
			-- If so, delete it
			delete myHelper 
		)

		-- Clear the button
		setProperty o nprop "Create point helper"
		
		ctrl = bezier_point3()
		setPropertyController o pprop ctrl		
		-- Restore position
		setProperty o pprop pos
	),
	--- HDRI Lights special magic
	fn pointHelper p = 
	(
		local o = CurrentMap()
		
		local s = p as string
		-- Point property name
		local pprop = "LightPos" + s
		-- Button property name
		local nprop = "LightHlp" + s

		-- Get the name in the button
		local n = getProperty o nprop
		
		local ctrl = getPropertyController o pprop

		if (n != undefined) do
		(
			local myHelper = undefined

			try (
				-- Try to get the node from the controller, if possibru
				myHelper = ctrl.getNode("Obj")
			)
			catch
			(
				-- See if this is a node
				myHelper = getNodeByName n
			)
			
			if (myHelper != undefined) do 
			(
				-- Set the button text to the name of the thing
				setProperty o nprop myHelper.name

				-- If so, select it, and exit
				select myHelper 
				return 0
			)
		)

		-- Get the current position
		local p = getProperty o pprop
		-- Create a point helper there
		local help = Point pos:p
		-- Wire the position to the property

		ctrl = point3_script()
		setPropertyController o pprop ctrl
		
		ctrl.addNode "Obj" help
		ctrl.script = "if isValidNode Obj then (coordsys local Obj.transform.row4) else [100,100,100]"
		
		-- Set th button text to the name of the thing
		setProperty o nprop help.name
		
		-- paramWire.connect help.transform.controller[#Position]  o[pprop] "Position"
		-- Make our point helper selected, so you can start doing "Place Highlight" on it immediately
		select help
	),
	
	--- OBJECT SELECTION STUFF
	
	---
	--- Generic object selection. Populates parameters w. the matrix stuff
	---
	
	fn selectObject msg = 
	(
		local o = CurrentMap()
		o.INode= msg
		local obj = pickObject message:msg
		if (obj != undefined) do (
			o.INode = obj.name
			o.XAxis.controller =  point3_script()
			o.XAxis.controller.addNode "Obj" obj
			o.XAxis.controller.script = "if isValidNode Obj then (coordsys local Obj.transform.row1) else [0,0,0]"
			o.YAxis.controller = point3_script()
			o.YAxis.controller.addNode "Obj" obj
			o.YAxis.controller.script = "if isValidNode Obj then (coordsys local Obj.transform.row2) else [0,0,0]"
			o.ZAxis.controller = point3_script()
			o.ZAxis.controller.addNode "Obj" obj
			o.ZAxis.controller.script = "if isValidNode Obj then (coordsys local Obj.transform.row3) else [0,0,0]"
			o.Position.controller = point3_script()
			o.Position.controller.addNode "Obj" obj
			o.Position.controller.script = "if isValidNode Obj then (coordsys local Obj.transform.row4) else [0,0,0]"
		)
	),
	--- Slightly more specific Camera selection
	fn selectCamera =
	(
		local o = CurrentMap()
		
		o.INode= "Pick a Camera"
		fn camFilt o = (superClassOf o == Camera)
		local obj = pickObject filter:camFilt message:"Pick a Camera"
		if (obj != undefined) do (
			o.INode = obj.name
			o.Camera.controller = point3_script() 
			o.Camera.controller.addNode "Obj" obj
			o.Camera.controller.script = "if isValidNode Obj then Obj.Position else [0,0,0]"
			o.Target.controller = point3_script()
			o.Target.controller.addNode "Obj" obj
			o.Target.controller.script = "if isValidNode Obj then (Obj.Position - Obj.dir) else [0,0,0]"
			o.Right.controller = point3_script()
			o.Right.controller.addNode "Obj" obj
			o.Right.controller.script = "if isValidNode Obj then Obj.transform.Row1 else [0,0,0]"
			local q = float_expression()
			q.addScalarTarget "FOV" obj.baseobject[#fov]
			q.setExpression "FOV"
			o.FOV.controller = q
		)
	)
	,

	fn Reset =  
	(
		local o = CurrentMap()
		OSL.Reset o
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgDI2lB6gSg+dUH7AGKfTSog7ZwmuV9+7Mf/o
-- mUxJpcr+l/hx1tH4jama2a6ApVgJto+2o9NDCl5rcXVs68O0HIgQSRO9MXQgOnx+
-- /Eeovp/kU5LiO8fgggzDorVFoSo/2p89CusEq7fmyrz7SYx9qc8GnAh4DibfBq//
-- bxYg40VMuNLf8YwfvxNqGVYXU12RbS/QoOWYjdrMNd5+vgBy1R1gPa+U9lo7bTvD
-- LFg2RzyeIRfIECJIlaQ4c9R/ZqCIjekL9VXMxobPCZOqiIJCV/0OnemSTHa/7k+2
-- aUMRBqEnRODEhsIi17tF9C+eSuBLUUxtj6wOg4ZinMRrP64id//Y//Cbc6jP5uWk
-- vRk6rn/kS7JuSBcBXx4xxyJ7tnghOOcmGY2mMPXntMXk1fUlBff3iAij1jdVHRPS
-- MNLPrfipx6VxhIoBXeF5/RGY7tHUjRnFLlBcoyUM8398y9RwsfjOO6IwjdTGbam0
-- dkZA7jGC4IID+DUFnPlKkFy80SLN+A==
-- -----END-SIGNATURE-----