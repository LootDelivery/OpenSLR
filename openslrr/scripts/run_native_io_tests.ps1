param(
  [string]$BuildDir = "build",
  [string]$Test = "test_save_io",
  [string]$BaselineRef = "",
  [string]$BaselineSources = "src/natives/natives_boot,src/natives/natives_io,src/resources/part_runtime"
)
# Standalone native-IO/native-state regression runners. The tests link the
# slrr_host object files (minus main.obj), so no second copy of the engine is
# compiled: build slrr_host Release first.
#
#   powershell -ExecutionPolicy Bypass -File scripts/run_native_io_tests.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/run_native_io_tests.ps1 -Test test_save_persistence
#   powershell -ExecutionPolicy Bypass -File scripts/run_native_io_tests.ps1 -BaselineRef 947d11bf
#
# -BaselineRef compiles the listed sources from another revision in place of
# the built objects, which is how the before/after tables in
# docs/UPSTREAM_INTEGRATION.md were produced.
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
Push-Location $repoRoot
try {
  $buildRoot = (Resolve-Path $BuildDir).Path
  $outputRoot = Join-Path $buildRoot "native-io-tests"
  New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
  $vswhere = Join-Path ${env:ProgramFiles(x86)} "Microsoft Visual Studio/Installer/vswhere.exe"
  $installation = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
  if (-not $installation) { throw "Visual Studio C++ x64 tools not found" }
  $toolVersion = (Get-Content (Join-Path $installation "VC/Auxiliary/Build/Microsoft.VCToolsVersion.default.txt")).Trim()
  $toolRoot = Join-Path $installation "VC/Tools/MSVC/$toolVersion"
  $sdkRoot = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows Kits\Installed Roots').KitsRoot10
  $sdkVersion = Get-ChildItem (Join-Path $sdkRoot 'Include') -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName 'um/Windows.h') } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1 -ExpandProperty Name
  $env:INCLUDE = @("$toolRoot/include", "$sdkRoot/Include/$sdkVersion/ucrt", "$sdkRoot/Include/$sdkVersion/shared", "$sdkRoot/Include/$sdkVersion/um") -join ';'
  $env:LIB = @("$toolRoot/lib/x64", "$sdkRoot/Lib/$sdkVersion/ucrt/x64", "$sdkRoot/Lib/$sdkVersion/um/x64") -join ';'
  $compiler = Join-Path $toolRoot 'bin/Hostx64/x64/cl.exe'
  [xml]$project = Get-Content (Join-Path $buildRoot 'slrr_host.vcxproj')
  $dependencies = $project.SelectSingleNode("//*[local-name()='ItemDefinitionGroup' and contains(@Condition,'Release|x64')]/*[local-name()='Link']/*[local-name()='AdditionalDependencies']").InnerText
  $libraries = foreach ($entry in $dependencies.Split(';')) {
    if (-not $entry -or $entry.StartsWith('%(')) { continue }
    $entry = $entry.Replace('$(Configuration)', 'Release')
    if ([IO.Path]::IsPathRooted($entry) -or $entry -notmatch '[/\\]') { $entry }
    else { Join-Path $buildRoot $entry }
  }
  $objects = (Get-ChildItem (Join-Path $buildRoot 'slrr_host.dir/Release') -Recurse -Filter '*.obj' |
    Where-Object { $_.Name -ne 'main.obj' }).FullName
  if (-not $objects) { throw "Build slrr_host Release first" }
  $label = 'after'
  if ($BaselineRef) {
    $label = 'before'
    $baselineRoot = Join-Path $outputRoot 'baseline'
    New-Item -ItemType Directory -Path $baselineRoot -Force | Out-Null
    foreach ($relative in $BaselineSources.Split(',')) {
      $relative = $relative.Trim()
      if (-not $relative) { continue }
      $name = [IO.Path]::GetFileNameWithoutExtension($relative)
      $source = Join-Path $baselineRoot "$name.cpp"
      $bytes = & git -c "safe.directory=$($repoRoot.Replace('\','/'))" show "${BaselineRef}:./$relative.cpp"
      if ($LASTEXITCODE -ne 0) { throw "Could not read $BaselineRef $relative.cpp" }
      [IO.File]::WriteAllText($source, ($bytes -join "`n"), [Text.UTF8Encoding]::new($false))
      $object = Join-Path $baselineRoot "$name.obj"
      & $compiler /nologo /std:c++20 /EHsc /MD /O2 /utf-8 /D_CRT_SECURE_NO_WARNINGS /Iinclude /Isrc/natives /c $source "/Fo:$object"
      if ($LASTEXITCODE -ne 0) { throw "Baseline compilation failed for $name" }
      $objects = @($objects | Where-Object { [IO.Path]::GetFileName($_) -ne "$name.obj" }) + @($object)
    }
  }
  $executable = Join-Path $outputRoot "$Test-$label.exe"
  $testObject = Join-Path $outputRoot "$Test-$label.obj"
  & $compiler /nologo /std:c++20 /EHsc /MD /O2 /utf-8 /D_CRT_SECURE_NO_WARNINGS /Iinclude /Isrc/natives "tests/$Test.cpp" "/Fe:$executable" "/Fo:$testObject" @objects @libraries
  if ($LASTEXITCODE -ne 0) { throw "$Test link failed" }
  $log = Join-Path $outputRoot "$Test-$label.log"
  # Redirect inside cmd: the host writes diagnostics to stderr, and a native
  # command that touches stderr must not abort the script.
  & cmd /c "`"$executable`" > `"$log`" 2>&1"
  $testExit = $LASTEXITCODE
  Get-Content $log
  Write-Output "$Test exit code: $testExit (log: $log)"
  exit $testExit
} finally {
  Pop-Location
}
