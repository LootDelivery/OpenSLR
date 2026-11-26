package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

// F8 camera panel: main chase camera only (cockpit keeps the original cfg,
// secondary cameras are untouched). It is a plain modal dialog on the game's
// own OSD (frame, backshield, sliders), never the ImGui debug overlay.
// Values are applied live by the host natives, no pause, and persist in
// save/game/camera_settings. Implemented by slrr_host only: the class is
// loaded from the patch tree, so the original exe never sees it.
public class CameraDialog extends Dialog
{
	static final int CMD_FOV = 500;
	static final int CMD_DIST = 501;
	static final int CMD_LIFT = 502;
	static final int CMD_TILT = 503;
	static final int CMD_RET = 504;
	static final int CMD_CLOSE = 505;

	public static native float getFov();
	public static native void setFov(float value);
	public static native float getDistance();
	public static native void setDistance(float value);
	public static native float getLift();
	public static native void setLift(float value);
	public static native float getTilt();
	public static native void setTilt(float value);
	public static native float getReturnSpeed();
	public static native void setReturnSpeed(float value);

	public CameraDialog()
	{
		super(GameLogic.player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "CAMERA", null);
		// Rows are built once here: the host keeps one dialog object and
		// toggles it with F8, so show() must not append rows again.
		Style row = new Style(0.45, 0.1, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
		Style sld = new Style(0.45, 0.06, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_SLD_BACK));
		Style knob = new Style(0.04, 0.05, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB));
		Menu menu = osd.createMenu(row, 0.0, (-(0.35)), 0);
		menu.setSliderStyle(sld, knob);
		menu.addItem("FOV", CameraDialog.CMD_FOV, CameraDialog.getFov(), 20.0, 90.0, 0, null);
		menu.addItem("DISTANCE", CameraDialog.CMD_DIST, CameraDialog.getDistance(), 1.0, 40.0, 0, null); 
		menu.addItem("TILT", CameraDialog.CMD_TILT, CameraDialog.getLift(), 0.0, 5.0, 0, null);
		menu.addItem("RETURN SPEED", CameraDialog.CMD_RET, CameraDialog.getReturnSpeed(), 0.2, 3.0, 0, null);
		menu.addSeparator();
		menu.addItem("CLOSE", CameraDialog.CMD_CLOSE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, CameraDialog.CMD_CLOSE, this);
		osd.createText("Main camera only, cockpit stays on cfg", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, (-(0.5)));
	}

	public void show()
	{
		super.show();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == CameraDialog.CMD_FOV))
		{
			CameraDialog.setFov(osd.sliderValue);
			return;
		}
		if ((arg0 == CameraDialog.CMD_DIST))
		{
			CameraDialog.setDistance(osd.sliderValue);
			return;
		}
 		if ((arg0 == CameraDialog.CMD_TILT))
		{
			CameraDialog.setLift(osd.sliderValue);
			return;
		}
		if ((arg0 == CameraDialog.CMD_RET))
		{
			CameraDialog.setReturnSpeed(osd.sliderValue);
			return;
		}
		if ((arg0 == CameraDialog.CMD_CLOSE))
		{
			hide();
			return;
		}
		super.osdCommand(arg0);
	}
}
