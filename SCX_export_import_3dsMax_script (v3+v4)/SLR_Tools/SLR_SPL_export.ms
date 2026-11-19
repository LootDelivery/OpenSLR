/* ************************************************************************************ */
/* Developed by Invictus Games Ltd Hungary, (c) 2003-2004 Invictus Games                */
/* ************************************************************************************ */

clearListener()

convertToSplineShape $

txt = newScript()

function GetVec s k d =
(
	if (d==1) then
		return getOutVec $ s k
	else
		return getInVec $ s k
)

function outputKnot s k d =
(
	p = getKnotPoint $ s k
	v = GetVec s k d
	p /= 100.0
	v /= 100.0
	v -= p
	p = [p[1], p[3], -p[2]]
	v = [v[1], v[3], -v[2]]
	v /= length v
	format "% % %\t% % %\t%\n" p[1] p[2] p[3] v[1] v[2] v[3] 2.0 to:txt
)

if queryBox "Do you want to export in forward direction?\nYes=forward\nNo=backward" then
(
	for s = numSplines $ to 1 by -1 do
		for k = numKnots $ s to 1 by -1 do
			outputKnot s k -1
	outputKnot (numSplines $) (numKnots $ (numSplines $)) -1
)
else
(
	for s = 1 to numSplines $ do
		for k = 1 to numKnots $ s do
			outputKnot s k 1
	outputKnot 1 1 1
)
