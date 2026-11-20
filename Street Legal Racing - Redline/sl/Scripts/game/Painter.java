package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class Painter extends GameType implements GameState
{
	static final int RID_PAINT_SPRAY = frontend:0x130r;
	static final int RID_PAINT_PART = frontend:0x131r;
	static final int RID_PAINT_DECAL = frontend:0x132r;
	static final int RID_PAINT_LOAD = frontend:0x11r;
	static final int RID_PAINT_SAVE = frontend:0x72r;
	static final int RID_BACKGROUND = frontend:0xc0r;
	static final float BACKGROUND_HEIGHT = 0.4;
	static final float PARTS_VP_TOP = 0.815;
	static final float PARTS_VP_LEFT = 0.226;
	static final float PARTS_VP_WIDTH = 0.696;
	static final float PARTS_VP_HEIGHT = 0.178;
	static final int CMD_SCROLL_UP = 0;
	static final int CMD_SCROLL_DOWN = 1;
	static final int CMD_SPRAY = 2;
	static final int CMD_PART = 3;
	static final int CMD_DECALS = 4;
	static final ResourceRef PAINTBRUSH = new ResourceRef(misc.garage:0x102r);
	static final SfxRef SfxSPRAY = new SfxRef(GameLogic.SFX_SPRAY);
	static final SfxRef SfxDECAL = new SfxRef(GameLogic.SFX_DECAL);
	static final SfxRef SfxWARN = new SfxRef(frontend:0x81r);
	static final int RID_button_decal = misc.garage:0x8cr;
	static final int CMD_DECALBUTTONS = 64;
	static final int CMD_NONE = 100;
	static final int CMD_BRUSHINC = 102;
	static final int CMD_BRUSHDEC = 103;
	static final int CMD_BRUSHRCW = 104;
	static final int CMD_BRUSHRCCW = 105;
	static final int CMD_BRUSHFLIP = 106;
	static final int CMD_LOADSKIN = 107;
	static final int CMD_SAVESKIN = 108;
	static final int CMD_COPYSKIN = 109;
	static final int CMD_C_COPY = 110;
	static final int CMD_C_PAINT = 111;
	static final int MODE_NONE = 0;
	static final int MODE_PAINTCOLOR = 1;
	static final int MODE_PAINTPART = 2;
	static final int MODE_PAINTDECAL = 3;
	static final int MODE_COPY = 4;
	static final int PF_REDUCED_FUNCTIONALITY = 1;
	ControlSetState css;
	PaintInventory paintCans;
	int overVehicle;
	int mode;
	int lastPaintMode = Painter.MODE_PAINTPART;
	GameRef paintCursor;
	ResourceRef paintBrush;
	int brushColor = -1;
	float decalRotation;
	float decalSize = 0.7;
	int decalFlip;
	GameRef[] decalButtons;
	RenderRef[] decalButtonTypes = new RenderRef[5];
	RenderRef[] decalButtonInstances = new RenderRef[5];
	ResourceRef[] decalButtonTextures = new ResourceRef[5];
	int showDecals;
	int spraySfxID;
	int spraySfxOn;
	int handleControls;
	Osd osd;
	int copyGroup;
	int paintGroup;
	int colorsGroup;
	int decalsGroup;
	int brushGroup;
	int sprayGroup;
	int osdGroup;
	Text moneyTxt;
	Text infoline;
	Text invLineTxt;
	Player player;
	Gadget red0;
	Gadget red1;
	int flags;
	Rectangle copy_prev;
	int copy_texture;
	int copy_status;
	Button copy_button;
	Button paint_button;

	public Painter(Player arg0, Osd arg1, Text arg2, Text arg3, int arg4)
	{
		super();
		createNativeInstance();
		player = arg0;
		osd = arg1;
		moneyTxt = arg2;
		infoline = arg3;
		handleControls = arg4;
		osd.globalHandler = this;
		Style local6 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWUP)
		);
		osd.createButton(local6, 0.94000006, 0.68, Painter.CMD_SCROLL_UP, "Scroll up");
		Style local7 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWDN)
		);
		osd.createButton(local7, 0.94000006, 0.92, Painter.CMD_SCROLL_DOWN, "Scroll down");
		invLineTxt = osd.createText("1", Frontend.largeFont, Text.ALIGN_CENTER, 0.95000005, 0.76);
		osd.createRectangle(
			0.0,
			(1.0 - (Painter.BACKGROUND_HEIGHT / 2)),
			2.0,
			Painter.BACKGROUND_HEIGHT,
			(-(1)),
			new ResourceRef(Painter.RID_BACKGROUND)
		);
		osd.hideGroup((osdGroup = osd.endGroup()));
		Style local8 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		Menu local9 = osd.createMenu(local8, (-(0.98)), 0.71999997, 0.120000005, Osd.MD_HORIZONTAL);
		red0 = local9.addItem(new ResourceRef(Painter.RID_PAINT_SPRAY), Painter.CMD_SPRAY, "Spray paint");
		local9.addItem(new ResourceRef(Painter.RID_PAINT_PART), Painter.CMD_PART, "Paint whole parts");
		red1 = local9.addItem(new ResourceRef(Painter.RID_PAINT_DECAL), Painter.CMD_DECALS, "Apply decals");
		local9 = osd.createMenu(local8, (-(0.98)), 0.88, 0.120000005, Osd.MD_HORIZONTAL);
		local9.addItem(new ResourceRef(Painter.RID_PAINT_LOAD), Painter.CMD_LOADSKIN, "Load paint job");
		local9.addItem(new ResourceRef(Painter.RID_PAINT_SAVE), Painter.CMD_SAVESKIN, "Save paint job");
		local9.addItem(new ResourceRef(MW_Mod:0x98r), Painter.CMD_COPYSKIN, "Copy part paint");
		osd.hideGroup((paintGroup = osd.endGroup()));
		osd.hideGroup((colorsGroup = osd.endGroup()));
		decalButtons = new GameRef[5];
		float local10 = (-(0.425));
		float local11 = 0.285;
		decalButtons[0] = osd.createButton(
			Painter.RID_button_decal,
			local10,
			0.8,
			null,
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			this,
			Painter.CMD_DECALBUTTONS
		);
		local10 += local11;
		decalButtons[1] = osd.createButton(
			Painter.RID_button_decal,
			local10,
			0.8,
			null,
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			this,
			(Painter.CMD_DECALBUTTONS + 1)
		);
		local10 += local11;
		decalButtons[2] = osd.createButton(
			Painter.RID_button_decal,
			local10,
			0.8,
			null,
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			this,
			(Painter.CMD_DECALBUTTONS + 2)
		);
		local10 += local11;
		decalButtons[3] = osd.createButton(
			Painter.RID_button_decal,
			local10,
			0.8,
			null,
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			this,
			(Painter.CMD_DECALBUTTONS + 3)
		);
		local10 += local11;
		decalButtons[4] = osd.createButton(
			Painter.RID_button_decal,
			local10,
			0.8,
			null,
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			this,
			(Painter.CMD_DECALBUTTONS + 4)
		);
		local10 += local11;
		osd.hideGroup((decalsGroup = osd.endGroup()));
		local8 = new Style(0.3, 0.1, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Mechanic.RID_BTN_L));
		local9 = osd.createMenu(local8, (-(1.0)), 0.15, 0);
		local9.addItem("FLIP", Painter.CMD_BRUSHFLIP, "TAB or NUM 5 key");
		local9.addItem("R. LEFT", Painter.CMD_BRUSHRCCW, "Q or NUM 4 key");
		local9.addItem("R. RIGHT", Painter.CMD_BRUSHRCW, "W or NUM 6 key");
		local9.addItem("BIGGER", Painter.CMD_BRUSHINC, "A or NUM 8 key");
		local9.addItem("SMALLER", Painter.CMD_BRUSHDEC, "S or NUM 2 key");
		osd.createHotkey(Input.RCDIK_TAB, Input.KEY, Painter.CMD_BRUSHFLIP, this);
		osd.createHotkey(Input.RCDIK_Q, Input.KEY, Painter.CMD_BRUSHRCCW, this);
		osd.createHotkey(Input.RCDIK_W, Input.KEY, Painter.CMD_BRUSHRCW, this);
		osd.createHotkey(Input.RCDIK_A, Input.KEY, Painter.CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_S, Input.KEY, Painter.CMD_BRUSHDEC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD4, Input.KEY, Painter.CMD_BRUSHRCCW, this);
		osd.createHotkey(Input.RCDIK_NUMPAD6, Input.KEY, Painter.CMD_BRUSHRCW, this);
		osd.createHotkey(Input.RCDIK_NUMPAD8, Input.KEY, Painter.CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD2, Input.KEY, Painter.CMD_BRUSHDEC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD5, Input.KEY, Painter.CMD_BRUSHFLIP, this);
		osd.hideGroup((brushGroup = osd.endGroup()));
		local9 = osd.createMenu(local8, (-(1.0)), 0.45, 0);
		local9.addItem("BIGGER", Painter.CMD_BRUSHINC, "A or NUM 8 key");
		local9.addItem("SMALLER", Painter.CMD_BRUSHDEC, "S or NUM 2 key");
		osd.createHotkey(Input.RCDIK_A, Input.KEY, Painter.CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_S, Input.KEY, Painter.CMD_BRUSHDEC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD8, Input.KEY, Painter.CMD_BRUSHINC, this);
		osd.createHotkey(Input.RCDIK_NUMPAD2, Input.KEY, Painter.CMD_BRUSHDEC, this);
		osd.hideGroup((sprayGroup = osd.endGroup()));
		osd.createRectangle((-(0.8)), 0.25, 0.4, 0.7, (-(1)), new ResourceRef(MW_Mod:0x34r));
		copy_prev = osd.createRectangle((-(0.8)), 0.16, 0.35000002, 0.45, 0, new ResourceRef(frontend:0x13cr));
		local8 = new Style(0.2, 0.089999996, Frontend.largeFont, Text.ALIGN_CENTER, null);
		local9 = osd.createMenu(local8, (-(0.8)), 0.45, 0);
		copy_button = local9.addItem("COPY", Painter.CMD_C_COPY);
		paint_button = local9.addItem("PAINT", Painter.CMD_C_PAINT);
		osd.hideGroup((copyGroup = osd.endGroup()));
		Part local12 = new Part();
		paintCans = new PaintInventory(
			player,
			Painter.PARTS_VP_LEFT,
			Painter.PARTS_VP_TOP,
			Painter.PARTS_VP_WIDTH,
			Painter.PARTS_VP_HEIGHT
		);
		paintCans.addItem(new PaintCan(16119285, 1.0));
		paintCans.addItem(new PaintCan(12171705, 1.0));
		paintCans.addItem(new PaintCan(8355711, 1.0));
		paintCans.addItem(new PaintCan(5263440, 1.0));
		paintCans.addItem(new PaintCan(1907997, 1.0));
		paintCans.addItem(new PaintCan(14424346, 1.0));
		paintCans.addItem(new PaintCan(11408661, 1.0));
		paintCans.addItem(new PaintCan(8392463, 1.0));
		paintCans.addItem(new PaintCan(5442058, 1.0));
		paintCans.addItem(new PaintCan(2426117, 1.0));
		paintCans.addItem(new PaintCan(16092680, 1.0));
		paintCans.addItem(new PaintCan(13005576, 1.0));
		paintCans.addItem(new PaintCan(9392902, 1.0));
		paintCans.addItem(new PaintCan(6240004, 1.0));
		paintCans.addItem(new PaintCan(2430209, 1.0));
		paintCans.addItem(new PaintCan(15517203, 1.0));
		paintCans.addItem(new PaintCan(12624143, 1.0));
		paintCans.addItem(new PaintCan(9533707, 1.0));
		paintCans.addItem(new PaintCan(6377737, 1.0));
		paintCans.addItem(new PaintCan(3221764, 1.0));
		paintCans.addItem(new PaintCan(14543122, 1.0));
		paintCans.addItem(new PaintCan(12042510, 1.0));
		paintCans.addItem(new PaintCan(9278730, 1.0));
		paintCans.addItem(new PaintCan(6580743, 1.0));
		paintCans.addItem(new PaintCan(3027203, 1.0));
		paintCans.addItem(new PaintCan(2353166, 1.0));
		paintCans.addItem(new PaintCan(1816333, 1.0));
		paintCans.addItem(new PaintCan(1411593, 1.0));
		paintCans.addItem(new PaintCan(940806, 1.0));
		paintCans.addItem(new PaintCan(537347, 1.0));
		paintCans.addItem(new PaintCan(1239491, 1.0));
		paintCans.addItem(new PaintCan(966560, 1.0));
		paintCans.addItem(new PaintCan(692088, 1.0));
		paintCans.addItem(new PaintCan(416588, 1.0));
		paintCans.addItem(new PaintCan(208936, 1.0));
		paintCans.addItem(new PaintCan(999149, 1.0));
		paintCans.addItem(new PaintCan(864956, 1.0));
		paintCans.addItem(new PaintCan(665489, 1.0));
		paintCans.addItem(new PaintCan(465765, 1.0));
		paintCans.addItem(new PaintCan(133934, 1.0));
		paintCans.addItem(new PaintCan(11144427, 1.0));
		paintCans.addItem(new PaintCan(9374406, 1.0));
		paintCans.addItem(new PaintCan(6883474, 1.0));
		paintCans.addItem(new PaintCan(4457823, 1.0));
		paintCans.addItem(new PaintCan(2097709, 1.0));
		paintCans.addItem(new PaintCan(15703513, 1.0));
		paintCans.addItem(new PaintCan(13392301, 1.0));
		paintCans.addItem(new PaintCan(10694019, 1.0));
		paintCans.addItem(new PaintCan(7804509, 1.0));
		paintCans.addItem(new PaintCan(4655158, 1.0));
		paintCans.addItem(new PaintCan(12240865, 1.0));
		paintCans.addItem(new PaintCan(9609905, 1.0));
		paintCans.addItem(new PaintCan(7042688, 1.0));
		paintCans.addItem(new PaintCan(4607316, 1.0));
		paintCans.addItem(new PaintCan(2040357, 1.0));
		paintCans.addItem(new PaintCan(15644430, 1.0));
		paintCans.addItem(new PaintCan(12052350, 1.0));
		paintCans.addItem(new PaintCan(11790034, 1.0));
		paintCans.addItem(new PaintCan(11249285, 1.0));
		paintCans.addItem(new PaintCan(9940887, 1.0));
		paintCans.addItem(new PaintCan(15644193, 1.0));
		paintCans.addItem(new PaintCan(14935011, 1.0));
		paintCans.addItem(new PaintCan(13831180, 1.0));
		paintCans.addItem(new PaintCan(7603204, 1.0));
		paintCans.addItem(new PaintCan(3803708, 1.0));
		paintCans.addItem(new PaintCan(15174406, 1.0));
		paintCans.addItem(new PaintCan(2184452, 1.0));
		paintCans.addItem(new PaintCan(9408399, 1.0));
		paintCans.addItem(new PaintCan(5263440, 1.0));
		paintCans.addItem(new PaintCan(2039583, 1.0));
		paintCans.addItem(new PaintCan(13267725, 1.0));
		paintCans.addItem(new PaintCan(1095127, 1.0));
		paintCans.addItem(new PaintCan(20320, 1.0));
		paintCans.addItem(new PaintCan(5975304, 1.0));
		paintCans.addItem(new PaintCan(269403, 1.0));
	}

	public void show()
	{
		if ((player.decals.size() <= showDecals))
		{
			showDecals = 0;
		}
		if ((flags & Painter.PF_REDUCED_FUNCTIONALITY))
		{
			red0.disable();
			red1.disable();
			lastPaintMode = Painter.MODE_PAINTPART;
		} else
		{
			red0.enable();
			red1.enable();
		}
		osd.showGroup(osdGroup);
		changeMode(lastPaintMode);
		paintCans.paintColor = paintCans.getCanbyIndex(paintCans.lastCanId).color;
		Input.cursor.addHandler(this);
		setEventMask(GameRef.EVENT_CURSOR);
		if (handleControls)
		{
			Input.cursor.setPointer(Frontend.pointers, "D");
			Input.cursor.enable(1);
			Input.getAxis(1, (-(1)));
			css = player.controller.reset();
			player.controller.activateState(ControlSet.CAMTURNSET);
		}
	}

	public void hide()
	{
		clearEventMask(GameRef.EVENT_ANY);
		Input.cursor.remHandler(this);
		if (handleControls)
		{
			Input.cursor.setPointer(Frontend.pointers, "J");
			Input.cursor.enable(0);
			player.controller.reset(css);
		}
		changeMode(Painter.MODE_NONE);
		osd.hideGroup(osdGroup);
		infoline.changeText(null);
	}

	public void changeMode(int arg0)
	{
		if ((mode != arg0))
		{
			if ((mode == Painter.MODE_PAINTCOLOR))
			{
				paintCans.hide();
				osd.hideGroup(paintGroup);
				osd.hideGroup(colorsGroup);
				osd.hideGroup(sprayGroup);
				lastPaintMode = mode;
			} else
			{
				if ((mode == Painter.MODE_PAINTPART))
				{
					paintCans.hide();
					osd.hideGroup(paintGroup);
					osd.hideGroup(colorsGroup);
					lastPaintMode = mode;
				} else
				{
					if ((mode == Painter.MODE_PAINTDECAL))
					{
						clearDecalButtons();
						osd.hideGroup(paintGroup);
						osd.hideGroup(decalsGroup);
						osd.hideGroup(brushGroup);
						lastPaintMode = mode;
					} else
					{
						if ((mode == Painter.MODE_COPY))
						{
							osd.hideGroup(copyGroup);
							lastPaintMode = mode;
						}
					}
				}
			}
			mode = arg0;
			if ((mode == Painter.MODE_PAINTCOLOR))
			{
				paintCans.show();
				invLineTxt.changeText(Integer.toString((paintCans.currentLine() + 1)));
				osd.showGroup(paintGroup);
				osd.showGroup(colorsGroup);
				osd.showGroup(sprayGroup);
				paintBrush = Painter.PAINTBRUSH;
			} else
			{
				if ((mode == Painter.MODE_PAINTPART))
				{
					paintCans.show();
					invLineTxt.changeText(Integer.toString((paintCans.currentLine() + 1)));
					osd.showGroup(paintGroup);
					osd.showGroup(colorsGroup);
				} else
				{
					if ((mode == Painter.MODE_PAINTDECAL))
					{
						setDecalButtons();
						if ((!(player.decals.isEmpty())))
						{
							paintBrush = player.decals.elementAt(showDecals);
						} else
						{
							paintBrush = null;
						}
						osd.showGroup(paintGroup);
						osd.showGroup(decalsGroup);
						osd.showGroup(brushGroup);
					} else
					{
						if ((mode == Painter.MODE_COPY))
						{
							osd.showGroup(paintGroup);
							osd.showGroup(copyGroup);
							if (copy_texture)
							{
								copy_prev.changeTexture(new ResourceRef(copy_texture));
							} else
							{
								copy_prev.changeTexture(new ResourceRef(frontend:0x13cr));
							}
							Input.cursor.setPointer(Frontend.pointers, "J");
							copy_status = 0;
							copy_button.disable();
							copy_button.label.txt.changeColor(-13568);
							paint_button.enable();
						}
					}
				}
			}
		}
	}

	native void doPaint(GameRef arg0, int arg1, int arg2, int arg3, float arg4, float arg5, int arg6);

	native void paintPart(GameRef arg0, int arg1);

	native void xPaint(GameRef arg0);

	public void beginPaint(int arg0)
	{
		paintCursor = new GameRef(arg0);
		enableControlHook();
	}

	public void endPaint()
	{
		disableControlHook();
		paintCursor = null;
	}

	public void setDecalButtons()
	{
		int local1 = player.decals.size();
		clearDecalButtons();
		int local2 = 0;
		while ((local2 < decalButtons.length))
		{
			RenderRef local3 = new RenderRef(misc.garage:0x83r);
			if ((local1 > (showDecals + local2)))
			{
				decalButtonTypes[local2] = new RenderRef();
				decalButtonTypes[local2].duplicate(local3);
				ResourceRef local4 = new ResourceRef(misc.garage:0x103r);
				ResourceRef local5 = player.decals.elementAt((showDecals + local2));
				decalButtonTypes[local2].changeResource(local4, local5);
				decalButtonTextures[local2] = local5;
				decalButtonInstances[local2] = new RenderRef(
					decalButtons[local2],
					decalButtonTypes[local2].id(),
					"decal button"
				);
				decalButtons[local2].queueEvent(
					null,
					GameType.EVENT_COMMAND,
					("render " + decalButtonInstances[local2].id())
				);
			} else
			{
				decalButtonTypes[local2] = null;
			}
			local2++;
		}
		invLineTxt.changeText(Integer.toString((((showDecals + 1) / decalButtons.length) + 1)));
	}

	public void clearDecalButtons()
	{
		int local1 = 0;
		while ((local1 < decalButtons.length))
		{
			if (decalButtonInstances[local1])
			{
				decalButtonInstances[local1].destroy();
				decalButtonInstances[local1] = null;
			}
			if (decalButtonTypes[local1])
			{
				decalButtonTypes[local1].destroy();
				decalButtonTypes[local1] = null;
			}
			if (decalButtonTextures[local1])
			{
				decalButtonTextures[local1].unload();
				decalButtonTextures[local1] = null;
			}
			local1++;
		}
	}

	public void handleEvent(GameRef arg0, int arg1, String arg2)
	{
		int local4 = (-(1));
		if ((arg1 == GameRef.EVENT_CURSOR))
		{
			int local5 = arg2.token(++local4).intValue();
			int local6 = arg2.token(++local4).intValue();
			if ((local5 == GameType.EC_LCLICK))
			{
				int local7 = arg2.token(++local4).intValue();
				Vector3 local8 = new Vector3(
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue()
				);
				Vector3 local9 = new Vector3(
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue()
				);
				GameRef local10 = arg0;
				int local11 = local10.getInfo(GameType.GII_CATEGORY);
				if ((mode == Painter.MODE_PAINTPART))
				{
					if (((local11 == GameType.GIR_CAT_PART) || (local11 == GameType.GIR_CAT_VEHICLE)))
					{
						SfxSPRAY.play(local9, 0.0, 1.0, 1.0, 0);
						paintPart(new GameRef(local6), (paintCans.paintColor | -16777216));
					}
				} else
				{
					if ((mode == Painter.MODE_PAINTDECAL))
					{
						if (((local11 == GameType.GIR_CAT_PART) || (local11 == GameType.GIR_CAT_VEHICLE)))
						{
							if (paintBrush)
							{
								SfxDECAL.play(local9, 0.0, 1.0, 1.0, 0);
								doPaint(
									new GameRef(local6),
									brushColor,
									paintBrush.id(),
									0,
									decalRotation,
									decalSize,
									decalFlip
								);
								if ((!(--((Decal) paintBrush).stickies)))
								{
									player.decals.removeElement(paintBrush);
									paintBrush = null;
									int local12 = player.decals.size();
									if (((showDecals >= decalButtons.length) && (showDecals >= local12)))
									{
										showDecals -= decalButtons.length;
									}
									setDecalButtons();
									infoline.changeText(null);
								} else
								{
									infoline.changeText((((Decal) paintBrush).stickies + " stickies left"));
								}
							}
						}
					} else
					{
						if ((mode == Painter.MODE_COPY))
						{
							if ((copy_status == 0))
							{
								Object local12_2 = arg0.getScriptInstance();
								if (((local12_2 instanceof Part) || (local12_2 instanceof Chassis)))
								{
									copy_texture = local12_2.getTexture();
									if (copy_texture)
									{
										copy_prev.changeTexture(new ResourceRef(copy_texture));
									} else
									{
										SfxWARN.play();
										copy_prev.changeTexture(new ResourceRef(frontend:0x13cr));
									}
								}
							} else
							{
								Object local12_3 = arg0.getScriptInstance();
								if (((local12_3 instanceof Part) || (local12_3 instanceof Chassis)))
								{
									int local13 = local12_3.getTexture();
									if ((copy_texture && local13))
									{
										SfxDECAL.play(local9, 0.0, 1.0, 1.0, 0);
										ResourceRef local14 = new ResourceRef();
										local14.duplicate(new ResourceRef(copy_texture));
										local12_3.setTexture(local14.id());
									} else
									{
										SfxWARN.play();
									}
								}
							}
						}
					}
				}
			} else
			{
				if ((local5 == GameType.EC_LDOWN))
				{
					if (overVehicle)
					{
						if ((mode == Painter.MODE_PAINTCOLOR))
						{
							Vector3 local7_2 = Input.cursor.getPickedPos();
							spraySfxID = SfxSPRAY.play(
								local7_2,
								0.0,
								1.0,
								1.0,
								(SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP),
								Input.cursor.id()
							);
							spraySfxOn = 1;
							beginPaint(local6);
						}
					}
				} else
				{
					if ((local5 == GameType.EC_LUP))
					{
						if ((mode == Painter.MODE_PAINTCOLOR))
						{
							if (spraySfxOn)
							{
								SfxSPRAY.stop(spraySfxID);
								spraySfxOn = 0;
							}
							endPaint();
						}
					} else
					{
						if ((local5 == GameType.EC_HOVER))
						{
							GameRef local7_3;
							GameRef local8_2 = new GameRef(arg2.token(++local4).intValue());
							int local9_2;
							Object local10_2 = local8_2.getScriptInstance();
							if ((local10_2 instanceof Part))
							{
								if ((!(overVehicle)))
								{
									overVehicle = 1;
									local9_2 = 1;
								}
							} else
							{
								if (overVehicle)
								{
									overVehicle = 0;
									local9_2 = 1;
								}
							}
							if ((mode == Painter.MODE_PAINTDECAL))
							{
								if (local9_2)
								{
									if (overVehicle)
									{
										beginPaint(local6);
									} else
									{
										endPaint();
										if (player.car)
										{
											xPaint(player.car);
										}
									}
								}
							}
						} else
						{
							if ((local5 == GameType.EC_RDRAGBEGIN))
							{
								player.controller.user_Del(Input.AXIS_CURSOR_X, ControlSet.MOUSE, 0);
								player.controller.user_Del(Input.AXIS_CURSOR_Y, ControlSet.MOUSE, 1);
								Input.cursor.cursor.queueEvent(null, GameType.EVENT_COMMAND, "lock");
							} else
							{
								if ((local5 == GameType.EC_RDRAGEND))
								{
									player.controller.user_Add(
										Input.AXIS_CURSOR_X,
										ControlSet.MOUSE,
										0,
										(-(1.0)),
										1.0,
										(-(1.0)),
										1.0
									);
									player.controller.user_Add(
										Input.AXIS_CURSOR_Y,
										ControlSet.MOUSE,
										1,
										(-(1.0)),
										1.0,
										(-(1.0)),
										1.0
									);
									Input.cursor.cursor.queueEvent(null, GameType.EVENT_COMMAND, "unlock");
								}
							}
						}
					}
				}
			}
		}
	}

	public void control(float arg0)
	{
		if ((mode == Painter.MODE_PAINTDECAL))
		{
			if (paintBrush)
			{
				doPaint(paintCursor, brushColor, paintBrush.id(), 1, decalRotation, decalSize, decalFlip);
			}
		} else
		{
			if ((mode == Painter.MODE_PAINTCOLOR))
			{
				Vector3 local2 = Input.cursor.getPickedPos();
				spraySfxID = SfxSPRAY.play(
					local2,
					0.0,
					1.0,
					1.0,
					(SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP),
					Input.cursor.id()
				);
				doPaint(
					paintCursor,
					(paintCans.paintColor | 1711276032),
					paintBrush.id(),
					0,
					decalRotation,
					decalSize,
					decalFlip
				);
			}
		}
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == Painter.CMD_LOADSKIN))
		{
			VehicleSkinFileReqDialog local2 = new VehicleSkinFileReqDialog(
				player.controller,
				((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
				"LOAD PAINT JOB",
				"LOAD",
				GameLogic.skinSaveDir,
				"*"
			);
			if ((local2.display() == 0))
			{
				String local3 = (GameLogic.skinSaveDir + local2.fileName);
				player.car.loadSkin(local3);
			}
		} else
		{
			if ((arg0 == Painter.CMD_SAVESKIN))
			{
				VehicleSkinFileReqDialog local2_2 = new VehicleSkinFileReqDialog(
					player.controller,
					(((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG) | Dialog.FRF_SAVE),
					"SAVE PAINT JOB",
					"SAVE",
					GameLogic.skinSaveDir,
					"*"
				);
				if ((local2_2.display() == 0))
				{
					String local3_2 = (GameLogic.skinSaveDir + local2_2.fileName);
					if (File.exists(local3_2))
					{
						File.delete(local3_2);
						File.delete((local3_2 + ".*"));
					}
					player.car.saveSkin(local3_2);
					new WarningDialog(
						player.controller,
						((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
						"SAVE PAINT JOB",
						"Paint job has been saved.").display(
					);
				}
			} else
			{
				if ((arg0 == Painter.CMD_SCROLL_UP))
				{
					if (((mode == Painter.MODE_PAINTCOLOR) || (mode == Painter.MODE_PAINTPART)))
					{
						paintCans.upScroll();
						invLineTxt.changeText(Integer.toString((paintCans.currentLine() + 1)));
					} else
					{
						if ((mode == Painter.MODE_PAINTDECAL))
						{
							int local2_3 = player.decals.size();
							if ((local2_3 > decalButtons.length))
							{
								if ((showDecals >= decalButtons.length))
								{
									showDecals -= decalButtons.length;
								} else
								{
									showDecals = ((local2_3 - 1) - ((local2_3 - 1) % decalButtons.length));
								}
								setDecalButtons();
							}
						}
					}
				} else
				{
					if ((arg0 == Painter.CMD_SCROLL_DOWN))
					{
						if (((mode == Painter.MODE_PAINTCOLOR) || (mode == Painter.MODE_PAINTPART)))
						{
							paintCans.downScroll();
							invLineTxt.changeText(Integer.toString((paintCans.currentLine() + 1)));
						} else
						{
							if ((mode == Painter.MODE_PAINTDECAL))
							{
								int local2_4 = player.decals.size();
								if ((local2_4 > decalButtons.length))
								{
									if (((showDecals + decalButtons.length) < local2_4))
									{
										showDecals += decalButtons.length;
									} else
									{
										showDecals = 0;
									}
									setDecalButtons();
								}
							}
						}
					} else
					{
						if ((arg0 == Painter.CMD_C_COPY))
						{
							copy_button.disable();
							copy_button.label.txt.changeColor(-13568);
							paint_button.enable();
							copy_status = 0;
							Input.cursor.setPointer(Frontend.pointers, "J");
						} else
						{
							if ((arg0 == Painter.CMD_C_PAINT))
							{
								copy_button.enable();
								paint_button.disable();
								paint_button.label.txt.changeColor(-13568);
								copy_status = 1;
								Input.cursor.setPointer(Frontend.pointers, "D");
							} else
							{
								if ((arg0 == Painter.CMD_COPYSKIN))
								{
									changeMode(Painter.MODE_COPY);
								}
							}
						}
					}
				}
			}
		}
		if ((arg0 == Painter.CMD_SPRAY))
		{
			changeMode(Painter.MODE_PAINTCOLOR);
		} else
		{
			if ((arg0 == Painter.CMD_PART))
			{
				changeMode(Painter.MODE_PAINTPART);
			} else
			{
				if ((arg0 == Painter.CMD_DECALS))
				{
					changeMode(Painter.MODE_PAINTDECAL);
				} else
				{
					if ((arg0 == Painter.CMD_BRUSHRCCW))
					{
						decalRotation -= 0.024999999;
					} else
					{
						if ((arg0 == Painter.CMD_BRUSHRCW))
						{
							decalRotation += 0.024999999;
						} else
						{
							if ((arg0 == Painter.CMD_BRUSHINC))
							{
								if ((decalSize < 4.2))
								{
									decalSize += 0.024999999;
								}
							} else
							{
								if ((arg0 == Painter.CMD_BRUSHDEC))
								{
									if ((decalSize > 0.049999997))
									{
										decalSize -= 0.024999999;
									}
								} else
								{
									if ((arg0 == Painter.CMD_BRUSHFLIP))
									{
										decalFlip = (1 - decalFlip);
									} else
									{
										if (((arg0 >= Painter.CMD_DECALBUTTONS) && (arg0 <= (Painter.CMD_DECALBUTTONS + decalButtons.length))))
										{
											int local2_5 = ((showDecals + arg0) - Painter.CMD_DECALBUTTONS);
											if ((player.decals.size() > local2_5))
											{
												paintBrush = player.decals.elementAt(local2_5);
												infoline.changeText((((Decal) paintBrush).stickies + " stickies left"));
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}

public class PaintInventory extends VisualInventory
{
	int paintColor;
	int lastCanId;

	public PaintInventory(Player arg0, float arg1, float arg2, float arg3, float arg4)
	{
		super(arg0, arg1, arg2, arg3, arg4, 1);
	}

	public void panelLeftClick(int arg0)
	{
		arg0 += (currentLine() * partsPerLine);
		if ((arg0 < items.size()))
		{
			lastCanId = arg0;
			PaintCan local2 = getCanbyIndex(arg0);
			paintColor = local2.color;
			new SfxRef(Frontend.SFX_MENU_SELECT).play();
		}
	}

	public void panelSwap(int arg0, Gadget arg1)
	{
	}

	public void panelDragNDrop(int arg0, GameRef arg1)
	{
	}

	public int addItem(PaintCan arg0)
	{
		if ((!(items.size())))
		{
			paintColor = arg0.color;
		}
		return super.addItem(arg0);
	}

}

public class VehicleSkinFileReqDialog extends FileRequesterDialog
{
	public VehicleSkinFileReqDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, String arg5)
	{
		super(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	public int validator(String arg0)
	{
		return Vehicle.fileCheck_Skin(arg0);
	}

}
