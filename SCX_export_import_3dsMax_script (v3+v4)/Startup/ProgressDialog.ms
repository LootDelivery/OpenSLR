-- Progress Dialog script for 3d max studio
-- Author: Miran (Miran4@op.pl)
-- You can use it on GNU license terms.

-- some variables for this dialog
PD_name = "Working..."
PD_currName = "Total:"
PD_type = 1
PD_pCurrent = 0
PD_pTotal = 0
PD_typeOffset = 0
PD_height = 0
PD_cancelable = false
PD_canceled = false
PD_lTime = 0 -- last time
PD_lDate = "" -- last date
PD_eTime = 0 -- elapsed time
PD_finalize = true
PD_refresher_delay = 250 -- dialog update time 

function getTimeString time =
(
	time = time/1000 -- time seconds now

	local timeText = ""
	local days = 0
	local hours = 0
	local minutes = 0
	local seconds = 0

	if( time >= (60*60*24) ) then -- days
	(
		days = floor (time/(60*60*24)) -- only whole days
		time = time - (days*(60*60*24))
	)

	if( time >= (60*60) ) then -- hours
	(
		hours = floor (time/(60*60))
		time = time - (hours*(60*60))
	)

	if( time >= 60 ) then -- minutes
	(
		minutes = floor (time/60)
		time = time - (minutes*60)
	)

	seconds = time -- seconds

	if( days>0 ) then
		 timeText = timeText + " " + ((days as integer) as string) + "d"
	
	if( (hours>0) OR (days>0) ) then
		 timeText = timeText + " " + ((hours as integer) as string) + "h"

	if( (minutes>0) OR (hours>0) OR (days>0) ) then
		 timeText = timeText + " " + ((minutes as integer) as string) + "m"

	timeText = timeText + " " + ((seconds as integer) as string) + "s" -- always show seconds

	return timeText
)

rollout ProgressDialog PD_name
(
	-- additional elements of dialog type 2
	label current_label "Current:" pos:[10,5] align:#left
	label currentP_label "" pos:[490,5] align:#right
	progressBar current_pBar "" pos:[10,20] width:480

	-- standard dialog elements
	label total_label "Total:" pos:[10,5+PD_typeOffset] align:#left
	label totalP_label "" pos:[490,5+PD_typeOffset] align:#right
	progressBar total_pBar "" pos:[10,20+PD_typeOffset] width:480

	label PD_eTime_label "" pos:[10,40+PD_typeOffset] align:#left
	label rtime_label "" pos:[490,40+PD_typeOffset] align:#right

	button cancel_button "Cancel" width:150 align:#center

	on cancel_button pressed do
	(
		cancel_button.enabled = false

		if( queryBox "Do you really want to cancel?" title:PD_name ) then
		(
			cancel_button.caption = "Canceling..."
			PD_canceled = true
		)
		else
			cancel_button.enabled = true
	)

	local tmpDate = ""
	function update = 
	(
		if( PD_cancelable ) then
		(
			if( (progressUpdate PD_pTotal) == false ) then
			(
				PD_canceled = true
				cancel_button.enabled = false
				cancel_button.caption = "Canceling..."
			)
		)

		if( PD_type == 1 ) then
		(
			ProgressDialog.total_label.caption = PD_currName
		)
		else
		if( PD_type == 2 ) then
		(
			ProgressDialog.currentP_label.caption = (PD_pCurrent as string) + "%"
			ProgressDialog.current_pBar.value = PD_pCurrent

			ProgressDialog.current_label.caption = PD_currName
			ProgressDialog.total_label.caption = "Total:"
		)

		ProgressDialog.totalP_label.caption = (PD_pTotal as string) + "%"
		ProgressDialog.total_pBar.value = PD_pTotal

		tmpDate = (filterString localTime " ")[1] -- get current date
		if( tmpDate != PD_lDate ) then -- crossed midnight, timeStamp() need correction
		(
			PD_eTime = PD_eTime + ((1000*60*60*24)-PD_lTime) + timeStamp()
			PD_lDate = tmpDate
		)
		else
		(
			PD_eTime = PD_eTime + (timeStamp()-PD_lTime)
		)
		PD_lTime = timeStamp()
		ProgressDialog.PD_eTime_label.caption = "Elapsed time:" + (getTimeString PD_eTime)
		
		if( PD_pTotal > 0.001 AND PD_eTime > 10 ) then
			ProgressDialog.rtime_label.caption = ( "Approximate remaining time:" + (getTimeString ((PD_eTime/(PD_pTotal/100))-PD_eTime)) )
		else
			ProgressDialog.rtime_label.caption = "Approximate remaining time: N/A"

		--SetDialogPos ProgressDialog (GetDialogPos ProgressDialog) -- redraw dialog
	)


	function setProgressBars curr:undefined total:undefined = 
	(
		if( curr != undefined ) then
		(
			if( curr == 0 ) then
				PD_pCurrent == 0
			else
			(
				curr = floor curr -- make whole number
				curr /= 10.0

				PD_pCurrent = curr
			)
		)

		if( total != undefined ) then
		(
			if( total == 0 ) then
				PD_pTotal == 0
			else
			(
				total = floor total -- make whole number
				total /= 10.0

				PD_pTotal = total
			)
		)
		
		if( (timeStamp() - PD_lTime) > PD_refresher_delay ) then
			ProgressDialog.update()
	)

	function setTotalProgress val =
	(
		if( PD_type == 1 ) then -- only one progress bar
		(
			setProgressBars curr:val total:val
		)
		else
			setProgressBars total:val
	)

	function setCurrProgress val =
	(
		if( PD_type == 1 ) then -- only one progress bar
		(
			setProgressBars curr:val total:val
		)
		else
			setProgressBars curr:val
	)
	
	function setProgress curr total:undefined = 
	(
		if( PD_type == 1 ) then -- only one progress bar
		(
			setProgressBars curr:curr total:curr
		)
		else
			setProgressBars curr:curr total:total
	)

	-- set operation name texts
	function setCurOperationName name =
	(
		PD_currName = name
		ProgressDialog.update()
	)

	function setTotalOperationName name =
	(
		if( PD_type == 1 ) then
		(
			PD_currName = name
			ProgressDialog.update()
		)
		else
		(
			total_label.text = name
		)
	)

	function setOperationName curr total:undefined =
	(
		if( PD_type != 1 ) then
		(
			if( total != undefined ) then
				total_label.text = total
		)
		
		PD_currName = curr
		ProgressDialog.update()
	)


	function canceled = -- check is canceled
	(
		local canc = false

		if( PD_cancelable ) then
		(
			canc = PD_canceled
		)

		return canc
	)

	function finished close:true =
	(
		ProgressDialog.update()

		if( PD_cancelable ) then progressEnd() -- close additional PD_cancelable progress display

		if( close == false ) then
		(
			if( PD_cancelable and PD_canceled ) then
			(
				ProgressDialog.rtime_label.caption = "Canceled"
				cancel_button.caption = "Cancel"
			)
			else
			(
				ProgressDialog.currentP_label.caption = "100.0%"
				ProgressDialog.current_pBar.value = 100

				ProgressDialog.totalP_label.caption = "100.0%"
				ProgressDialog.total_pBar.value = 100

				ProgressDialog.rtime_label.caption = "Completed"
			)

			PD_finalize = true -- user can close dialog
			cancel_button.enabled = false
		)
		else
		(	
			PD_finalize = true
			DestroyDialog ProgressDialog -- close dialog
		)

		redrawViews() -- redraw viewports
	)

	on ProgressDialog open do
	(	
		if( PD_type == 2 ) then
		(
			ProgressDialog.current_label.visible = true
			ProgressDialog.currentP_label.visible = true
			ProgressDialog.current_pBar.visible = true
		)
		else
		(
			ProgressDialog.current_label.visible = false
			ProgressDialog.currentP_label.visible = false
			ProgressDialog.current_pBar.visible = false
		)

		if( PD_cancelable ) then
			ProgressDialog.cancel_button.visible = true
		else
			ProgressDialog.cancel_button.visible = false

		ProgressDialog.update() -- reset bars and set labels
	)

	on ProgressDialog oktoclose do -- user can't close before complete progress
	(
		return PD_finalize
	)
)

function CreateProgressDialog name type:1 center:true cancel:false refresh_freq:6 =
(
	PD_finalize = true
	DestroyDialog ProgressDialog -- close already existing

	PD_name = name
	PD_type = type
	PD_refresher_delay = 1000.0/refresh_freq -- update time
	PD_pCurrent = 0
	PD_pTotal = 0
	PD_canceled = false
	PD_cancelable = cancel
	PD_finalize = false -- user can not close dialog
	PD_height = 58

	if( type == 1 ) then -- simple
	(
		PD_typeOffset = 0
		PD_currName = "Progress:"
	)
	else
	if( type == 2 ) then -- extra bar for current operation
	(
		PD_typeOffset = 35
		PD_currName = "Current:"
	)

	PD_height = PD_height + PD_typeOffset -- additional items will enlarge dialog size

	if( PD_cancelable ) then PD_height = PD_height + 27 -- cancel button also need some extra space

	PD_eTime = 0
	PD_lDate = (filterString localTime " ")[1] -- store current date, without day time
	PD_lTime = timeStamp() -- store today's time in miliseconds

	if( PD_cancelable ) then progressStart PD_name -- add standard PD_cancelable progress display	
	CreateDialog ProgressDialog width:500 height:PD_height style:#(#style_titlebar, #style_border) -- create custom progress dialog

	if( center == false ) then SetDialogPos ProgressDialog (point2 (sysInfo.DesktopSize.x/2-250) 5)
	ProgressDialog.update()
)
