@echo off
rem Diagnostic: legacy path, no HDR target and no ACES tonemapper. If the UI
rem backgrounds match the original here but not in the normal run, the
rem darkening comes from the HDR path. Remove once that is settled.
set "SLRR_NO_HDR=1"
start "" "E:\OpenSLR\openslrr\build\Release\slrr_host.exe" --game --window
