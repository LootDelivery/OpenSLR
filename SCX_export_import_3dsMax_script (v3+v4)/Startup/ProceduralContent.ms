
-- This structure wraps operations specific to the Procedural Content feature
struct ProceduralContentOps
(
	private
	--  Loads the assemblies (DLLs) needed by the procedural content code
	fn LoadAssemblies = 
	(
		-- Load the PC-3dsMax bridge assembly
		local bridgePath = (symbolicPaths.getPathValue "$max") + @"\Viper3dsMaxBridge.dll"
		dotNet.loadAssembly bridgePath returnPassFail:true
		
		-- Load the MaxPlus assembly
		local maxPlusPath = (symbolicPaths.getPathValue "$max") + @"\MaxPlusDotNet.dll"
		dotNet.loadAssembly maxPlusPath returnPassFail:true
	),
	
	-- Creates the user profile folder if not existent and copies the CSS file
	fn SetupUserProfileFolder =
	(
		local bridge = dotNetClass "Viper3dsMaxBridge.Main"
		try
		(
			-- This function can throw a .NET exception if folders cannot be created.
			-- This will happen when 3dsMax runs in low integrity mode because folders
			-- are not created in %USER PROFILE%\AppData\LocalLow.
			bridge.CopyProceduralContentToUserProfile()
		)
		catch()
	),
	
	-- Compiles and registers the standard (built-in) operators, all compounds, and all tools
	-- to make them available to the current user
	fn ReloadOperatorsAndCompileTools =
	(
		local bridge = dotNetClass "Viper3dsMaxBridge.Main"    
		--  Loads all compounds from the users profile folder and exports the operators.
		bridge.ReloadOperators()
		-- Compile all graphs from all the registered folders
		bridge.CompileAllToolsAndPackages()
	),

	-- Adds directories from the specified section of the ini file to the list of directory
	fn AddDirs AddDirFunc iniFile iniSection = 
	(
		if (hasINISetting iniFile iniSection) then
		(
			local iniKeys = getINISetting iniFile iniSection
			for key in iniKeys do
			(
				local dir = getINISetting iniFile iniSection key
				AddDirFunc dir
			)
		)
	),

	-- Registers with Viper the custom graph directories specified in the startup ini file
	fn RegisterCustomGraphPaths =
	(
		local iniFile = getMAXIniFile()
		local settings = dotNetClass "Viper3dsMaxBridge.Settings"
		
		-- MCG directory evaluation order: Max, user, custom directories if any
		AddDirs settings.AddCompoundDirectory iniFile "MCG Compound Directories"
		AddDirs settings.AddToolDirectory iniFile "MCG Tools Directories"
		AddDirs settings.AddPackageDirectory iniFile "MCG Package Directories"
		AddDirs settings.AddAssemblyDirectory iniFile "MCG Assembly Directories"
	),
	
	-- Register a call back on file open 
	fn RegisterForPreLoadMaxFile = 
	(
		callbacks.removeScripts id:#pcPreFileOpen
		callbacks.addScript #filePreOpenProcess "ProceduralContentOps.OnPreFileOpen()" id:#pcPreFileOpen	
	),

	public
	
	-- Call back registered for file opens  
	fn OnPreFileOpen = 
	(
		-- This callback is effective only when Max is in network rendering mode
		if (IsNetServer()) then
		(
			local np = callbacks.notificationParam()
			--format "CompileProceduralContentGraphs: param1=%, param2=%\n" (np[1]) (np[2])
			local path = (getFilenamePath np[2])
			local bridge = dotNetClass "Viper3dsMaxBridge.Main"
			local replaceExistentCompounds = true
			-- Load the compounds from the folder where the Max file is loaded from
			-- Any new compounds that have the same name as already registered ones will overwrite them
			bridge.LoadViperCompounds path replaceExistentCompounds
			-- Load and compile the tools from the folder where the Max file is loaded from
			local loadToolsFromSubFolders = true
			local PrintToConsole = true
			bridge.CompileGraphsByFolder path
		)
	),

	-- This sets up the user currently logged in and running 3ds Max to use
	-- the Procedural Content features. It registers built in operators, 
	-- loads and registers compounds and tools 
	fn SetUpCurrentUser = 
	(
		LoadAssemblies()
		SetupUserProfileFolder()
		RegisterCustomGraphPaths()
		ReloadOperatorsAndCompileTools()
		RegisterForPreLoadMaxFile()
	)
)

-- A helper function for generating a MaxPlus INode from a MAXScript node
fn MaxPlusINodeFromNode n = 
(
	if not (IsValidNode n) do return undefined
	local maxPlusINodeClass = dotNetClass "Autodesk.Max.MaxPlus.INode"    
	if (maxPlusINodeClass == undefined) then (
		format "Error: failed to load Autodesk.Max.MaxPlus.INode\n"
		return undefined
	)
	return maxPlusINodeClass._CreateWrapper n
)

-- Turns on a log file that profiles mesh updating in modifiers and geometry. 
fn TurnOnMCGProfiling =
(
	local profiler = dotNetClass "Viper3dsMaxBridge.Profiler"    
	profiler.TurnOnProfiling()
)

-- Turns off the profiler 
fn TurnOffMCGProfiling = 
(
	local profiler = dotNetClass "Viper3dsMaxBridge.Profiler"    
	profiler.TurnOffProfiling()
)

-- Create the procedural content structure 
thePcOps = ProceduralContentOps()

-- The main entry function 
thePcOps.SetUpCurrentUser()

fn GetFileNameForMCGUserTool theToolName = (
	-- @todo: there should be symbolic paths for MCG directories
	viperMaxBridge = dotNetClass "Viper3dsMaxBridge.Main"
	local toolPath = viperMaxBridge.UserToolsDirectory 
	append toolpath ("\\" + theToolName + ".maxtool")
)

fn GenerateMeshGraphCompoundForSelection addUV compoundName: = 
(
	local res = false
	if (selection.count == 1) then (
		createAction = dotNetClass "ProceduralContentActions.PCActionCreateCompoundFromMesh"
		if (createAction != undefined) do (
			if (compoundName == unsupplied) do
				compoundName = selection[1].name
			res = createAction.GenerateMeshGraphCompoundForSelection addUV compoundName
		)
	)
	res
)

-- Generate a new tool file from a tool template file
-- Optionally compile it and open it in the graph editor
fn GenerateMCGToolFromTemplate templateFilename toolName compileIt openIt =
(
	local res = false
	local templateStream = openFile templateFilename
	if (templateStream != undefined) then (
		-- read the content of the template file to a string
		local template = readChars templateStream -1 errorAtEOF:false
		close templateStream 

		local guidMarker1 = "uuid=\""
		local guidMarker2 = "guid=\""
		local identifierMarker = "<identifier>"
		local displayNameMarker = "<displayName>"
		local guidPattern = "????????-????-????-????-????????????"
		-- Replace the 2 existing guids in the template file with new ones, dynamically created
		local ndx = findString template guidMarker1
		if (ndx != undefined) do (
			local newGuid = genGuid noBrackets:true
			template = replace template (ndx + guidMarker1.count) guidPattern.count newGuid 
			ndx = findString template guidMarker2
			if (ndx != undefined) do (
				newGuid = genGuid noBrackets:true
				template = replace template (ndx + guidMarker2.count) guidPattern.count newGuid
				ndx = findString template identifierMarker
				if (ndx != undefined) do (
					local identifier = toolName
					template = replace template (ndx + identifierMarker.count) 0 identifier
					ndx = findString template displayNameMarker
					if (ndx != undefined) do (
						local displayName = toolName
						template = replace template (ndx + displayNameMarker.count) 0 displayName
						
						-- Now save that string to <toolName>.maxtool in the user MCG Tools folder
						local toolPath = GetFileNameForMCGUserTool toolName
						local toolStream = openFile toolpath mode:"w"
						if (toolStream != undefined) do (
							format "%" template to:toolStream
							close toolStream

							res = true
							if (compileIt) do 
							(
								-- compile this tool, and then create a selected instance of the tool object
								local viperMaxBridge = dotNetClass "Viper3dsMaxBridge.Main"
								res = viperMaxBridge.CompileGraph toolpath
							)

							if (openIt) do
							(
								-- Open the MCG Editor with that template graph
								geBridge = dotNetClass "ProceduralContentActions.PCGraphEditorBridge"
								geBridge.LoadGraph toolpath
							)
						)
					)	
				)
			)
		)
	)
	res
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
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgIZWI3PcW2rmIj+D7LaaG7/eh9VcwPUDbKAr
-- cNlFrrpt7n9Oy3wKjnDdj4hVNRWbf1ewDgXZAV+5AE+WV/eFN9NMjCcmM4cMNNeC
-- 7bhaxa51w8dRYLQcntOPYjGNHQGJNcN7qfng8AMkomxeZh6RTVh2bYM9yGeOw/tq
-- hRONhF5Vxm7Ws4V85ahC7uv2XAc0ID0x4zQTc6sBaM/gFx6Cv5y/+PsWk5QVDOc9
-- NrNTUvFQ5ZEgbo0LE+KFQRlE3QquTsKBDohz7oKlWWfKU9ekRKYTpjorTiJUFasO
-- Cvlzyrvc306iLwr7yr1urZYZ1jtuE5L9/oY0f9r+Zidl0gz5HiPjuVui0NWaCf5C
-- JZOBPkv9AJuLPz7BclV2rbRXgeQLIu8q86LBYIt1MqtxJP7lRYmoRC7iR3jwB4c4
-- uyea7+piTKkcv6990mUYtuPKVZB0fmgaTx2GcUuyvQIzz9m2Ltyk1DOQDDaovwLL
-- Ikgh/9JonbjVbX/1saRPI+Swo4xiVA==
-- -----END-SIGNATURE-----