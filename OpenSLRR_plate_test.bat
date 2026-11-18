@echo off
rem Diagnostic: paints a white square at kPlateView to check that the view
rem draws at all. Remove this file once the plate routing is settled.
set "SLRR_PLATE_TEST=1"
start "" "E:\OpenSLR\openslrr\build\Release\slrr_host.exe" --game --window
