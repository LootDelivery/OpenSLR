package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class OptionsDialog extends Dialog
{
	static final ResourceRef RID_GENERALBG = new ResourceRef(frontend:0x16r);
	static final int CMD_OPTIONS = 0;
	static final int CMD_VIDEO_OPTIONS = 1;
	static final int CMD_CONTROL_OPTIONS = 2;
	static final int CMD_SOUND_OPTIONS = 3;
	static final int CMD_REDEFINE_CONTROLS = 4;
	static final int CMD_MAIN = 5;
	static final int CMD_REDEFINE_DONE = 6;
	static final int CMD_RESET_CONTROLS = 7;
	static final int CMD_LOAD_CONTROLS = 8;
	static final int CMD_SAVE_CONTROLS = 9;
	static final int CMD_RESOLUTION = 10;
	static final int CMD_VIDEO_OPTIONS_DONE = 11;
	static final int CMD_TEXTURE_DETAIL = 12;
	static final int CMD_SHADOW_DETAIL = 13;
	static final int CMD_VIEW_RANGE = 14;
	static final int CMD_EFFECTS_VOL = 15;
	static final int CMD_MUSIC_VOL = 16;
	static final int CMD_ENGINE_VOL = 17;
	static final int CMD_GAME_OPTIONS = 18;
	static final int CMD_DIFFICULTY = 19;
	static final int CMD_TRAFFICDENSITY = 20;
	static final int CMD_PEDESTRIANDENSITY = 21;
	static final int CMD_CLUTCH = 22;
	static final int CMD_STEERHELP = 23;
	static final int CMD_GAME_OPTIONS_DONE = 24;
	static final int CMD_MOUSE_SENS = 25;
	static final int CMD_REDEFINE_CONTROLS2 = 26;
	static final int CMD_REDEFINE_CONTROLS3 = 27;
	static final int CMD_METRIC = 28;
	static final int CMD_GAMMA = 29;
	static final int CMD_OBJECT_DETAIL = 30;
	static final int CMD_LOD_DETAIL = 31;
	static final int CMD_ABS_SLIDER = 32;
	static final int CMD_ASR_SLIDER = 33;
	static final int CMD_GPSMODE = 34;
	static final int CMD_AXISCHECK = 35;
	static final int CMD_AXISCHECK_DONE = 36;
	static final int CMD_UNUSED = 37;
	static final int CMD_SOUND_3D = 38;
	static final int CMD_SOUND_HW = 39;
	static final int CMD_GAME_OPTIONS2 = 40;
	static final int CMD_HMF_1 = 41;
	static final int CMD_HMF_2 = 42;
	static final int CMD_HMF_3 = 43;
	static final int CMD_PARTICLE = 44;
	static final int CMD_HEADLIGHTS = 45;
	static final int CMD_FLARES = 46;
	static final int CMD_SKIDMARK = 402;
	static final int CMD_SHADOW_RANGE = 403;
	static final int CMD_REFRESH_TEXT = 404;
	static final int CMD_MIRROR_DISTANCE = 405;
	static final int CMD_DEPTH = 406;
	static final int CMD_MODE = 407;
	static final int CMD_UNRES = 408;
	static final int CMD_MIRROR = 409;
	static final int CMD_BGSCENE = 410;
	static final int CMD_M_GROUP = 411;
	static final int CMD_LP = 412;
	static final int CMD_RED_SUSP = 413;
	static final int CMD_GETKEY = 100;
	static final int CMD_DEAD_ZONE = 200;
	static final int CMD_POWER = 300;
	static final int CMD_FFB = 400;
	static final int CMD_COMPILE = 401;
	static final int NCONTROLS = ControlSet.NCONTROLS;
	static String[] soundHWText;
	static String[] TextureSizeText;
	static String[] ShadowSizeText;
	static String[] LP_TexList;
	static String[] detailTextOO;
	static String[] metricSystemText;
	static String[] gpsText = new String[4];
	static int[] ResX_list;
	static int[] ResY_list;
	static final float aspectX;
	static final float aspectY;
	String Res_Aspect;
	int Current_Resolution;
	int aspect;
	int color_depth;
	int windowed;
	int loading_primary;
	int tmp_lp;
	Button Resolution_button;
	Button MirrorDist_Text;
	Button Dist_Text;
	Button ShadowDist_Text;
	Button Particle_Text;
	Button SkidMark_Text;
	Button Depth_button;
	Button windowed_button;
	Player player;
	int getKey = (-(1));
	int getName = (-(1));
	int getKeyStat = 0;
	Text[] keyText = new Text[OptionsDialog.NCONTROLS];
	String[] clutchText = new String[4];
	int[] clutchTrf;
	int[] clutchTrfInv;
	Text accelerateText;
	Text brakeText;
	Text steeringText;
	int axisCheckCallerGroup;
	int mainGroup;
	int optionsGroup;
	int videoGroup;
	int soundGroup;
	int controlGroup;
	int gameGroup;
	int game2Group;
	int MirrorGroup;
	int redefineSuspGroup;
	int redefineGroup;
	int redefine2Group;
	int redefine3Group;
	int axisCheckGroup;
	int actGroup;
	int textureDetail;
	int shadow_size;
	int shadow_distance;
	MultiChoice soundHWMulti;
	Slider[] powerSlider;
	Slider[] dzSlider;
	Slider[] ffbSlider;
	Slider acGasSlider;
	Slider acBreakSlider;
	Slider acSteerSlider;
	Slider acClutchSlider;
	Slider TextureSlider;
	ControlSet controlSet;
	Style butt0 = new Style(0.45, 0.120000005, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
	Style butt1 = new Style(0.45, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	Style butt2 = new Style(0.45, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
	Style buttX = new Style(0.1, 0.1, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
	Style sld1 = new Style(0.45, 0.06, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_SLD_BACK));
	Style sldk = new Style(
		0.04,
		0.049999997,
		Frontend.mediumFont,
		Text.ALIGN_RIGHT,
		new ResourceRef(Osd.RID_SLD_KNOB)
	);
	Style sld2 = new Style(
		0.8,
		0.06,
		Frontend.mediumFont,
		Text.ALIGN_CENTER,
		new ResourceRef(Osd.RID_SLD_BACK)
	);
	Style Menu_Left = new Style(0.6, 0.1, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
	Style Menu_Right = new Style(0.6, 0.1, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
	Style Menu_Center = new Style(0.6, 0.1, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	Style Menu_Left_Small = new Style(0.6, 0.1, Frontend.smallFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
	Style Menu_Right_Small = new Style(0.6, 0.1, Frontend.smallFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
	Style Menu_Center_Small = new Style(0.6, 0.1, Frontend.smallFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
	GameRef activeCamera;

	public OptionsDialog(int arg0)
	{
		super(GameLogic.player.controller, ((arg0 | Dialog.DF_FULLSCREEN) | Dialog.DF_LOWPRI), null, null);
		float local2 = (Math.random() * 100);
		if ((MainMenu.Check(local2) != (Math.sqrt(local2) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		player = GameLogic.player;
		osd.globalHandler = this;
		actGroup = (-(1));
		OptionsDialog.ResX_list = new int[20];
		OptionsDialog.ResX_list[0] = 160;
		OptionsDialog.ResX_list[1] = 213;
		OptionsDialog.ResX_list[2] = 320;
		OptionsDialog.ResX_list[3] = 320;
		OptionsDialog.ResX_list[4] = 400;
		OptionsDialog.ResX_list[5] = 480;
		OptionsDialog.ResX_list[6] = 512;
		OptionsDialog.ResX_list[7] = 640;
		OptionsDialog.ResX_list[8] = 640;
		OptionsDialog.ResX_list[9] = 720;
		OptionsDialog.ResX_list[10] = 720;
		OptionsDialog.ResX_list[11] = 800;
		OptionsDialog.ResX_list[12] = 848;
		OptionsDialog.ResX_list[13] = 960;
		OptionsDialog.ResX_list[14] = 1024;
		OptionsDialog.ResX_list[15] = 1152;
		OptionsDialog.ResX_list[16] = 1280;
		OptionsDialog.ResX_list[17] = 1280;
		OptionsDialog.ResX_list[18] = 1280;
		OptionsDialog.ResX_list[19] = 1280;
		OptionsDialog.ResY_list = new int[20];
		OptionsDialog.ResY_list[0] = 120;
		OptionsDialog.ResY_list[1] = 160;
		OptionsDialog.ResY_list[2] = 200;
		OptionsDialog.ResY_list[3] = 240;
		OptionsDialog.ResY_list[4] = 300;
		OptionsDialog.ResY_list[5] = 360;
		OptionsDialog.ResY_list[6] = 384;
		OptionsDialog.ResY_list[7] = 400;
		OptionsDialog.ResY_list[8] = 480;
		OptionsDialog.ResY_list[9] = 480;
		OptionsDialog.ResY_list[10] = 576;
		OptionsDialog.ResY_list[11] = 600;
		OptionsDialog.ResY_list[12] = 480;
		OptionsDialog.ResY_list[13] = 720;
		OptionsDialog.ResY_list[14] = 768;
		OptionsDialog.ResY_list[15] = 864;
		OptionsDialog.ResY_list[16] = 720;
		OptionsDialog.ResY_list[17] = 768;
		OptionsDialog.ResY_list[18] = 960;
		OptionsDialog.ResY_list[19] = 1024;
		OptionsDialog.detailTextOO = new String[2];
		OptionsDialog.detailTextOO[0] = "OFF";
		OptionsDialog.detailTextOO[1] = "ON";
		OptionsDialog.LP_TexList = new String[3];
		OptionsDialog.LP_TexList[0] = "LOW (Best)";
		OptionsDialog.LP_TexList[1] = "MEDIUM";
		OptionsDialog.LP_TexList[2] = "DEFAULT";
		OptionsDialog.TextureSizeText = new String[6];
		OptionsDialog.TextureSizeText[0] = "32 PTX";
		OptionsDialog.TextureSizeText[1] = "64 PTX";
		OptionsDialog.TextureSizeText[2] = "128 PTX";
		OptionsDialog.TextureSizeText[3] = "256 PTX";
		OptionsDialog.TextureSizeText[4] = "512 PTX";
		OptionsDialog.TextureSizeText[5] = "1024 PTX";
		OptionsDialog.ShadowSizeText = new String[6];
		OptionsDialog.ShadowSizeText[0] = "OFF";
		OptionsDialog.ShadowSizeText[1] = "64 PTX";
		OptionsDialog.ShadowSizeText[2] = "128 PTX";
		OptionsDialog.ShadowSizeText[3] = "256 PTX";
		OptionsDialog.ShadowSizeText[4] = "512 PTX";
		OptionsDialog.ShadowSizeText[5] = "1024 PTX";
		OptionsDialog.soundHWText = new String[4];
		OptionsDialog.soundHWText[0] = "SOFTWARE (SLOW!)";
		OptionsDialog.soundHWText[1] = "HARDWARE (2D ONLY)";
		OptionsDialog.soundHWText[2] = "HARDWARE (FULL 3D)";
		OptionsDialog.soundHWText[3] = "AUTO";
		clutchText[0] = "Auto+Autoclutch";
		clutchText[1] = "Semiauto+Autoclutch";
		clutchText[2] = "Manual+Autoclutch";
		clutchText[3] = "Manual+Clutch";
		clutchTrf = new int[6];
		clutchTrf[0] = 2;
		clutchTrf[1] = 0;
		clutchTrf[2] = 3;
		clutchTrf[3] = (-(1));
		clutchTrf[4] = (-(1));
		clutchTrf[5] = 1;
		clutchTrfInv = new int[4];
		clutchTrfInv[0] = 1;
		clutchTrfInv[1] = 5;
		clutchTrfInv[2] = 0;
		clutchTrfInv[3] = 2;
		OptionsDialog.metricSystemText = new String[2];
		OptionsDialog.metricSystemText[0] = "Miles";
		OptionsDialog.metricSystemText[1] = "Kilometres";
		OptionsDialog.gpsText[0] = "FOLLOW CAR, MAP RELATIVE";
		OptionsDialog.gpsText[1] = "FOLLOW CAR, CAR RELATIVE";
		OptionsDialog.gpsText[2] = "FOLLOW CAMERA, MAP RELATIVE";
		OptionsDialog.gpsText[3] = "FOLLOW CAMERA, CAMERA RELATIVE";
	}

	public void show()
	{
		float local1 = (Math.random() * 100);
		if ((MainMenu.Check(local1) != (Math.sqrt(local1) + 3.75)))
		{
			new WarningDialog(
				GameLogic.player.controller,
				(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
				"PROTECTION ERROR",
				"Miran Wichur Mod protection error: \n \n CORRUPTED FILES \n \n Sorry game will be close. \n Please reinstall Miran Wichur Mod.").display(
			);
			GameLogic.changeActiveSection(null);
		}
		OptionsDialog.aspectX = Config.video_x;
		OptionsDialog.aspectY = Config.video_y;
		color_depth = Config.video_depth;
		windowed = Config.video_windowed;
		int local2 = 0;
		while ((local2 <= (ResX_list.length - 1)))
		{
			if ((OptionsDialog.aspectX == OptionsDialog.ResX_list[local2]))
			{
				Current_Resolution = local2;
				if ((OptionsDialog.aspectX == OptionsDialog.ResX_list[local2]))
				{
					Current_Resolution = local2;
					local2 += ResX_list.length;
				}
			}
			local2++;
		}
		textureDetail = (5 - Config.texture_size);
		if ((Config.resource_refreshrate == 2))
		{
			loading_primary = 0;
		} else
		{
			if ((Config.resource_refreshrate == 512))
			{
				loading_primary = 1;
			} else
			{
				if ((Config.resource_refreshrate == 2048))
				{
					loading_primary = 2;
				}
			}
		}
		tmp_lp = loading_primary;
		if ((Config.shadow_size == 1024))
		{
			shadow_size = 5;
		} else
		{
			if ((Config.shadow_size == 512))
			{
				shadow_size = 4;
			} else
			{
				if ((Config.shadow_size == 256))
				{
					shadow_size = 3;
				} else
				{
					if ((Config.shadow_size == 128))
					{
						shadow_size = 2;
					} else
					{
						if ((Config.shadow_size == 64))
						{
							shadow_size = 1;
						} else
						{
							if ((Config.shadow_size < 64))
							{
								shadow_size = 0;
							}
						}
					}
				}
			}
		}
		float local2_2;
		float local3;
		float local4;
		float local5 = (-(1.0));
		float local6 = (-(0.1));
		addCustomGroups();
		Menu local7;
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("OPTIONS");
		local7 = osd.createMenu(butt1, 0.0, (-(0.6)), 0);
		local7.addItem("VIDEO OPTIONS", OptionsDialog.CMD_VIDEO_OPTIONS);
		local7.addItem("SOUND OPTIONS", OptionsDialog.CMD_SOUND_OPTIONS);
		local7.addItem("CONTROL OPTIONS", OptionsDialog.CMD_CONTROL_OPTIONS);
		local7.addItem("GAME OPTIONS", OptionsDialog.CMD_GAME_OPTIONS);
		local7.addItem("ADVANCED GAME OPTIONS", OptionsDialog.CMD_GAME_OPTIONS2);
		local7 = osd.createMenu(butt0, (-(0.98)), 0.85, 0);
		local7.addItem("DEBUG COMPILE ALL JAVAs", OptionsDialog.CMD_COMPILE);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_MAIN);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_MAIN, this);
		osd.hideGroup((optionsGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("VIDEO OPTIONS");
		local7 = osd.createMenu(Menu_Left, (-(0.95000005)), (-(0.6)), 0);
		Resolution_button = local7.addItem(
			"SCREEN RESOLUTION: --- X --- (---)",
			OptionsDialog.CMD_RESOLUTION,
			"Get next standard resolution"
		);
		local7 = osd.createMenu(Menu_Center, (-(0.5)), (-(0.5)), 0);
		Depth_button = local7.addItem("COLOR DEPTH: ---", OptionsDialog.CMD_DEPTH, null);
		local7 = osd.createMenu(Menu_Left, (-(0.4)), (-(0.4)), 0);
		local7.addItem(
			"TEXTURE DETAIL",
			OptionsDialog.CMD_TEXTURE_DETAIL,
			textureDetail,
			OptionsDialog.TextureSizeText,
			null
		);
		local7 = osd.createMenu(Menu_Center_Small, (-(0.5)), (-(0.2)), 0);
		Dist_Text = local7.addItem(
			("VIEW DISTANCE: " + Float.toString(Config.camera_ext_viewrange, "%.0f m")),
			OptionsDialog.CMD_UNUSED,
			null
		);
		local7 = osd.createMenu(Menu_Left_Small, (-(0.5)), (-(0.15)), 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(null, OptionsDialog.CMD_VIEW_RANGE, Config.camera_ext_viewrange, 50, 2050, 81, null);
		local7 = osd.createMenu(Menu_Center_Small, (-(0.5)), (-(0.049999997)), 0);
		local7.addItem("OBJECT DETAIL", OptionsDialog.CMD_UNUSED, "How much details is visable");
		local7 = osd.createMenu(Menu_Left_Small, (-(0.5)), 0.0, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_OBJECT_DETAIL,
			Config.object_detail,
			0.074,
			0.01,
			11,
			"How much details is visable"
		);
		local7 = osd.createMenu(Menu_Center_Small, (-(0.5)), 0.1, 0);
		local7.addItem("LOD DETAIL", OptionsDialog.CMD_UNUSED, "Distance of objects simplification");
		local7 = osd.createMenu(Menu_Left_Small, (-(0.5)), 0.15, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_LOD_DETAIL,
			Config.object_detail_amp,
			35.0,
			4.0,
			11,
			"Distance of objects simplification"
		);
		local7 = osd.createMenu(Menu_Left, (-(0.4)), 0.28, 0);
		local7.addItem(
			"LOADING PRIMARY:",
			OptionsDialog.CMD_LP,
			loading_primary,
			OptionsDialog.LP_TexList,
			"Use LOW to fast game working"
		);
		local7 = osd.createMenu(Menu_Left, (-(0.5)), 0.5, 0);
		local7.addItem("FLARES:", OptionsDialog.CMD_FLARES, Config.flares, OptionsDialog.detailTextOO, null);
		local7 = osd.createMenu(Menu_Center, 0.5, (-(0.6)), 0);
		local7.addItem("UNCOMMON RESOLUTION", OptionsDialog.CMD_UNRES, "Set uncommon resolution");
		windowed_button = local7.addItem("DISPLAY MODE: ---", OptionsDialog.CMD_MODE, null);
		local7 = osd.createMenu(Menu_Left, 0.3, (-(0.4)), 0);
		local7.setSliderStyle(sld1, sldk);
		local7.addItem(
			"GAMMA",
			OptionsDialog.CMD_GAMMA,
			Config.video_gamma,
			0.4,
			4.0,
			0,
			"Max shadow texture size").printValue("%.2f"
		);
		local7 = osd.createMenu(Menu_Left, 0.6, (-(0.18)), 0);
		local7.addItem(
			"SHADOW DETAIL",
			OptionsDialog.CMD_SHADOW_DETAIL,
			shadow_size,
			OptionsDialog.ShadowSizeText,
			"Max shadow texture size"
		);
		local7 = osd.createMenu(Menu_Center_Small, 0.5, (-(0.049999997)), 0);
		ShadowDist_Text = local7.addItem(
			("SHADOW VIEW DISTANCE: " + Float.toString(((1 / Config.shadow_detail) * 10), "%.0f m")),
			OptionsDialog.CMD_UNUSED,
			"Max shadow view distance"
		);
		local7 = osd.createMenu(Menu_Left_Small, 0.5, 0.0, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_SHADOW_RANGE,
			((1 / Config.shadow_detail) * 10),
			10,
			210,
			21,
			"Max shadow view distance"
		);
		local7 = osd.createMenu(Menu_Center_Small, 0.5, 0.1, 0);
		Particle_Text = local7.addItem(
			(("PARTICLE DENSITY: " + Float.toString((Config.particle_density * 100), "%.0f")) + "%"),
			OptionsDialog.CMD_UNUSED,
			"Density of tyres smoke, sparks etc."
		);
		local7 = osd.createMenu(Menu_Left_Small, 0.5, 0.15, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_PARTICLE,
			Config.particle_density,
			0.0,
			2.0,
			9,
			"Density of tyres smoke, sparks etc."
		);
		local7 = osd.createMenu(Menu_Center_Small, 0.5, 0.25, 0);
		SkidMark_Text = local7.addItem(
			(("SKIDMARKS LENGTH: " + (Config.skidmark_max / 4)) + " m"),
			OptionsDialog.CMD_UNUSED,
			"Max length of skidmarks"
		);
		local7 = osd.createMenu(Menu_Left_Small, 0.5, 0.3, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_SKIDMARK,
			(Config.skidmark_max / 4),
			50,
			2050,
			21,
			"Max length of skidmarks"
		);
		local7 = osd.createMenu(Menu_Left, 0.65000004, 0.5, 0);
		local7.addItem(
			"HEADLIGHT RAYS:",
			OptionsDialog.CMD_HEADLIGHTS,
			Config.headlight_rays,
			OptionsDialog.detailTextOO,
			null
		);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_VIDEO_OPTIONS_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_VIDEO_OPTIONS_DONE, this);
		osd.hideGroup((videoGroup = osd.endGroup()));
		osd.createHeader("MIRROR OPTIONS");
		local7 = osd.createMenu(Menu_Left, (-(0.5)), 0.5, 0);
		local7.addItem("MIRROR:", OptionsDialog.CMD_MIRROR, Config.mirror, OptionsDialog.detailTextOO, null);
		local7 = osd.createMenu(Menu_Center_Small, (-(0.5)), (-(0.049999997)), 0);
		MirrorDist_Text = local7.addItem(
			("VIEW DISTANCE: " + Float.toString(Config.camera_mirror_viewrange, "%.0f m")),
			OptionsDialog.CMD_UNUSED,
			null
		);
		local7 = osd.createMenu(Menu_Left_Small, (-(0.5)), 0.0, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_MIRROR_DISTANCE,
			Config.camera_mirror_viewrange,
			5,
			105,
			41,
			null
		);
		local7 = osd.createMenu(Menu_Center_Small, (-(0.5)), 0.1, 0);
		local7.addItem("OBJECT DETAIL", OptionsDialog.CMD_UNUSED, "How much details is visable");
		local7 = osd.createMenu(Menu_Left_Small, (-(0.5)), 0.15, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_OBJECT_DETAIL,
			Config.object_detail,
			0.074,
			0.01,
			11,
			"How much details is visable"
		);
		local7 = osd.createMenu(Menu_Center_Small, (-(0.5)), 0.25, 0);
		local7.addItem("LOD DETAIL", OptionsDialog.CMD_UNUSED, "Distance of objects simplification");
		local7 = osd.createMenu(Menu_Left_Small, (-(0.5)), 0.3, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_LOD_DETAIL,
			Config.object_detail_amp,
			35.0,
			4.0,
			11,
			"Distance of objects simplification"
		);
		local7 = osd.createMenu(Menu_Left, 0.6, (-(0.04)), 0);
		local7.addItem(
			"SHADOW DETAIL",
			OptionsDialog.CMD_SHADOW_DETAIL,
			shadow_size,
			OptionsDialog.ShadowSizeText,
			"Max shadow texture size"
		);
		local7 = osd.createMenu(Menu_Center_Small, 0.5, 0.1, 0);
		local7.addItem(
			("SHADOW VIEW DISTANCE: " + Float.toString(((1 / Config.shadow_detail) * 10), "%.0f m")),
			OptionsDialog.CMD_UNUSED,
			"Max shadow view distance"
		);
		local7 = osd.createMenu(Menu_Left_Small, 0.5, 0.15, 0);
		local7.setSliderStyle(sld2, sldk);
		local7.addItem(
			null,
			OptionsDialog.CMD_SHADOW_RANGE,
			((1 / Config.shadow_detail) * 10),
			10,
			210,
			21,
			"Max shadow view distance"
		);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_VIDEO_OPTIONS);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_VIDEO_OPTIONS, this);
		osd.hideGroup((MirrorGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("SOUND OPTIONS");
		local7 = osd.createMenu(butt0, 0.0, (-(0.6)), 0);
		local7.setSliderStyle(sld1, sldk);
		local7.addItem(
			"EFFECTS VOLUME",
			OptionsDialog.CMD_EFFECTS_VOL,
			Sound.getVolume(Sound.CHANNEL_EFFECTS),
			null
		);
		local7.addItem("MUSIC VOLUME", OptionsDialog.CMD_MUSIC_VOL, Sound.getVolume(Sound.CHANNEL_MUSIC), null);
		local7.addItem(
			"ENGINE VOLUME",
			OptionsDialog.CMD_ENGINE_VOL,
			Sound.getVolume(Sound.CHANNEL_ENGINE),
			null
		);
		local7.addSeparator();
		soundHWMulti = local7.addItem("MIXING", OptionsDialog.CMD_SOUND_HW, 0, OptionsDialog.soundHWText, null);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_OPTIONS);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_OPTIONS, this);
		osd.hideGroup((soundGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("CONTROL OPTIONS");
		local7 = osd.createMenu(butt1, 0.0, (-(0.6)), 0);
		local7.addItem("REDEFINE CAR CONTROLS", OptionsDialog.CMD_REDEFINE_CONTROLS);
		local7.addItem("REDEFINE GAME CONTROLS", OptionsDialog.CMD_REDEFINE_CONTROLS2);
		local7.addItem("ADVANCED SETTINGS", OptionsDialog.CMD_REDEFINE_CONTROLS3);
		local7.addItem("RESET TO DEFAULTS", OptionsDialog.CMD_RESET_CONTROLS);
		local7.addItem("LOAD CONTROLS", OptionsDialog.CMD_LOAD_CONTROLS);
		local7.addItem("SAVE CONTROLS", OptionsDialog.CMD_SAVE_CONTROLS);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_OPTIONS);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_OPTIONS, this);
		osd.hideGroup((controlGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("REDEFINE CAR CONTROLS - DRIVING");
		local7 = osd.createMenu(Menu_Left, (-(0.98)), (-(0.83)), 0, Osd.MD_HORIZONTAL);
		local7.addItem("DRIVING", OptionsDialog.CMD_REDEFINE_CONTROLS);
		local7.addItem("SUSPENSION", OptionsDialog.CMD_RED_SUSP);
		osd.createText("    PRIMARY CONTROLS", Frontend.mediumFont, Text.ALIGN_LEFT, local5, (-(0.7)));
		local7 = osd.createMenu(butt0, (-(0.95000005)), (-(0.45)), 0);
		local7.addItem("ACCELERATE", (OptionsDialog.CMD_GETKEY + 0));
		local7.addItem("BRAKE", (OptionsDialog.CMD_GETKEY + 1));
		local7.addItem("TURN LEFT", (OptionsDialog.CMD_GETKEY + 2));
		local7.addItem("TURN RIGHT", (OptionsDialog.CMD_GETKEY + 3));
		local7.addItem("HAND BRAKE", (OptionsDialog.CMD_GETKEY + 4));
		local7.addItem("SHIFT UP", (OptionsDialog.CMD_GETKEY + 5));
		local7.addItem("SHIFT DOWN", (OptionsDialog.CMD_GETKEY + 6));
		local7.addItem("CLUTCH", (OptionsDialog.CMD_GETKEY + 7));
		local7.addItem("ENGAGE NOS", (OptionsDialog.CMD_GETKEY + 8));
		local7.addItem("HONK HORN", (OptionsDialog.CMD_GETKEY + 9));
		osd.createText(" SECONDARY CONTROLS", Frontend.mediumFont, Text.ALIGN_LEFT, local6, (-(0.7)));
		local7 = osd.createMenu(butt0, (-(0.1)), (-(0.45)), 0);
		local7.addItem("ACCELERATE", (OptionsDialog.CMD_GETKEY + 15));
		local7.addItem("BRAKE", (OptionsDialog.CMD_GETKEY + 16));
		local7.addItem("TURN LEFT", (OptionsDialog.CMD_GETKEY + 17));
		local7.addItem("TURN RIGHT", (OptionsDialog.CMD_GETKEY + 18));
		local7.addItem("HAND BRAKE", (OptionsDialog.CMD_GETKEY + 19));
		local7.addItem("SHIFT UP", (OptionsDialog.CMD_GETKEY + 20));
		local7.addItem("SHIFT DOWN", (OptionsDialog.CMD_GETKEY + 21));
		local7.addItem("CLUTCH", (OptionsDialog.CMD_GETKEY + 22));
		local7.addItem("ENGAGE NOS", (OptionsDialog.CMD_GETKEY + 23));
		local7.addItem("HONK HORN", (OptionsDialog.CMD_GETKEY + 24));
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0, Osd.MD_HORIZONTAL);
		local7.addItem("BACK", OptionsDialog.CMD_REDEFINE_DONE);
		local7.addItem("CHECK AXES", OptionsDialog.CMD_AXISCHECK);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_REDEFINE_DONE, this);
		local2_2 = (-(0.4));
		local3 = (-(0.48000002));
		int local8 = 0;
		while ((local8 < 25))
		{
			if ((local8 == 10))
			{
				local3 += 0.6;
			}
			if ((local8 == 15))
			{
				local2_2 = 0.5;
				local3 = (-(0.48000002));
			}
			if (((local8 < 10) || (local8 > 14)))
			{
				keyText[local8] = osd.createText(
					"Undefined",
					Frontend.mediumFont,
					Text.ALIGN_LEFT,
					local2_2,
					local3
				);
			}
			local3 += 0.120000005;
			local8++;
		}
		osd.hideGroup((redefineGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("REDEFINE CAR CONTROLS - SUSPENSION");
		osd.createRectangle(0.0, 0.0, 1.65, 1.15, (-(1)), new ResourceRef(MW_Mod:0x97r));
		local7 = osd.createMenu(Menu_Left, (-(0.98)), (-(0.83)), 0, Osd.MD_HORIZONTAL);
		local7.addItem("DRIVING", OptionsDialog.CMD_REDEFINE_CONTROLS);
		local7.addItem("SUSPENSION", OptionsDialog.CMD_RED_SUSP);
		osd.createText(
			"Redefine pneumatic suspension controls:",
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			0.0,
			(-(0.7))
		);
		local7 = osd.createMenu(Menu_Center, 0.54, (-(0.38)), 0);
		local7.addItem("FL", (OptionsDialog.CMD_GETKEY + 65));
		keyText[65] = osd.createText(
			"Undefined",
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			0.54,
			(-(0.32000002))
		);
		local7 = osd.createMenu(Menu_Center, 0.54, (-(0.03)), 0);
		local7.addItem("Front", (OptionsDialog.CMD_GETKEY + 66));
		keyText[66] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.54, 0.03);
		local7 = osd.createMenu(Menu_Center, 0.54, 0.32000002, 0);
		local7.addItem("FR", (OptionsDialog.CMD_GETKEY + 67));
		keyText[67] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.54, 0.38);
		local7 = osd.createMenu(Menu_Center, 0.0, (-(0.38)), 0);
		local7.addItem("Left", (OptionsDialog.CMD_GETKEY + 68));
		keyText[68] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, (-(0.32000002)));
		local7 = osd.createMenu(Menu_Center, 0.0, (-(0.03)), 0);
		local7.addItem("All", (OptionsDialog.CMD_GETKEY + 69));
		keyText[69] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.03);
		local7 = osd.createMenu(Menu_Center, 0.0, 0.32000002, 0);
		local7.addItem("Right", (OptionsDialog.CMD_GETKEY + 70));
		keyText[70] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.38);
		local7 = osd.createMenu(Menu_Center, (-(0.54)), (-(0.4)), 0);
		local7.addItem("RL", (OptionsDialog.CMD_GETKEY + 71));
		keyText[71] = osd.createText(
			"Undefined",
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			(-(0.54)),
			(-(0.32000002))
		);
		local7 = osd.createMenu(Menu_Center, (-(0.54)), (-(0.03)), 0);
		local7.addItem("Rear", (OptionsDialog.CMD_GETKEY + 72));
		keyText[72] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, (-(0.54)), 0.03);
		local7 = osd.createMenu(Menu_Center, (-(0.54)), 0.32000002, 0);
		local7.addItem("RR", (OptionsDialog.CMD_GETKEY + 73));
		keyText[73] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, (-(0.54)), 0.38);
		local7 = osd.createMenu(Menu_Right, 0.2, 0.7, 0);
		local7.addItem("Change Mode (Up/Down)", (OptionsDialog.CMD_GETKEY + 74));
		keyText[74] = osd.createText("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, 0.22, 0.66);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_REDEFINE_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_REDEFINE_DONE, this);
		osd.hideGroup((redefineSuspGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("REDEFINE GAME CONTROLS");
		local7 = osd.createMenu(butt0, (-(0.4)), (-(0.6)), 0);
		local7.addItem("NEXT RADIO STATION", (OptionsDialog.CMD_GETKEY + 11));
		local7.addItem("PREV RADIO STATION", (OptionsDialog.CMD_GETKEY + 12));
		local7.addItem("VOLUME UP", (OptionsDialog.CMD_GETKEY + 13));
		local7.addItem("VOLUME DOWN", (OptionsDialog.CMD_GETKEY + 14));
		local7.addSeparator();
		local7.addItem("CRUISE CONTROL", (OptionsDialog.CMD_GETKEY + 75));
		local7.addItem("LOOK BACK", (OptionsDialog.CMD_GETKEY + 76));
		local7.addItem("VIEW MAP", (OptionsDialog.CMD_GETKEY + 77));
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_REDEFINE_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_REDEFINE_DONE, this);
		local2_2 = 0.3;
		local3 = (-(0.63));
		int local8_2 = 11;
		while ((local8_2 <= 14))
		{
			keyText[local8_2] = osd.createText(
				"Undefined",
				Frontend.mediumFont,
				Text.ALIGN_LEFT,
				local2_2,
				local3
			);
			local3 += 0.120000005;
			local8_2++;
		}
		local3 = (-(0.089999996));
		int local8_3 = 75;
		while ((local8_3 <= 77))
		{
			keyText[local8_3] = osd.createText(
				"Undefined",
				Frontend.mediumFont,
				Text.ALIGN_LEFT,
				local2_2,
				local3
			);
			local3 += 0.120000005;
			local8_3++;
		}
		osd.hideGroup((redefine2Group = osd.endGroup()));
		powerSlider = new Slider[4];
		dzSlider = new Slider[4];
		ffbSlider = new Slider[2];
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("ADVANCED CONTROL SETTINGS");
		local7 = osd.createMenu(butt1, 0.0, (-(0.6)), 0);
		local7.setSliderStyle(sld1, sldk);
		powerSlider[0] = local7.addItem("ACCEL. GAMMA", (OptionsDialog.CMD_POWER + 0), 0.0, null);
		powerSlider[1] = local7.addItem("BRAKE GAMMA", (OptionsDialog.CMD_POWER + 1), 0.0, null);
		powerSlider[2] = local7.addItem("STEERING GAMMA", (OptionsDialog.CMD_POWER + 2), 0.0, null);
		powerSlider[3] = local7.addItem("CLUTCH GAMMA", (OptionsDialog.CMD_POWER + 3), 0.0, null);
		local7.addItem("MOUSE SENSITIVITY", OptionsDialog.CMD_MOUSE_SENS, Config.mouseSensitivity, null);
		dzSlider[0] = local7.addItem("ACCELERATE DEAD ZONE", (OptionsDialog.CMD_DEAD_ZONE + 0), 0.0, null);
		dzSlider[1] = local7.addItem("BRAKE DEAD ZONE", (OptionsDialog.CMD_DEAD_ZONE + 1), 0.0, null);
		dzSlider[2] = local7.addItem("STEERING DEAD ZONE", (OptionsDialog.CMD_DEAD_ZONE + 2), 0.0, null);
		dzSlider[3] = local7.addItem("CLUTCH DEAD ZONE", (OptionsDialog.CMD_DEAD_ZONE + 3), 0.0, null);
		local7.addSeparator();
		ffbSlider[0] = local7.addItem(
			"REAL FFB STRENGTH",
			(OptionsDialog.CMD_FFB + 0),
			Config.FFB_strength,
			0.0,
			1.0,
			0,
			null
		);
		ffbSlider[1] = local7.addItem(
			"EMULATED FFB STRENGTH",
			(OptionsDialog.CMD_FFB + 1),
			Config.FFB_strength_emulated,
			0.0,
			0.1,
			0,
			null
		);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0, Osd.MD_HORIZONTAL);
		local7.addItem("BACK", OptionsDialog.CMD_REDEFINE_DONE);
		local7.addItem("CHECK AXES", OptionsDialog.CMD_AXISCHECK);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_REDEFINE_DONE, this);
		osd.hideGroup((redefine3Group = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("VIEW CONTROL AXES");
		osd.createText(
			"Please review how the axes respond to the car controls:",
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			(-(0.95000005)),
			(-(0.7))
		);
		Style local8_4 = new Style(
			0.85,
			0.06,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_SLD_BACK)
		);
		local7 = osd.createMenu(butt1, 0.0, (-(0.45)), 0);
		local7.setSliderStyle(local8_4, sldk);
		acGasSlider = local7.addItem("THROTTLE", OptionsDialog.CMD_UNUSED, 0.0, 0.0, 1.0, 0, null);
		acGasSlider.nofocus = 1;
		local7.addSeparator();
		acBreakSlider = local7.addItem("BRAKE", OptionsDialog.CMD_UNUSED, 0.0, 0.0, 1.0, 0, null);
		acBreakSlider.nofocus = 1;
		local7.addSeparator();
		acSteerSlider = local7.addItem("STEER", OptionsDialog.CMD_UNUSED, 0.5, 0.0, 1.0, 0, null);
		acSteerSlider.nofocus = 1;
		local7.addSeparator();
		acClutchSlider = local7.addItem("CLUTCH", OptionsDialog.CMD_UNUSED, 0.5, 0.0, 1.0, 0, null);
		acClutchSlider.nofocus = 1;
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_AXISCHECK_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_AXISCHECK_DONE, this);
		osd.hideGroup((axisCheckGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("GAME OPTIONS");
		local7 = osd.createMenu(butt0, 0.0, (-(0.6)), 0);
		local7.setSliderStyle(sld1, sldk);
		local7.addItem(
			"TRANSMISSION",
			OptionsDialog.CMD_CLUTCH,
			clutchTrf[Config.player_transmission],
			clutchText,
			null
		);
		local7.addItem(
			"METRIC SYSTEM",
			OptionsDialog.CMD_METRIC,
			Config.metricSystem,
			OptionsDialog.metricSystemText,
			null
		);
		local7.addItem("GPS MODE", OptionsDialog.CMD_GPSMODE, Config.gpsMode, OptionsDialog.gpsText, null);
		local7.addItem("DIFFICULTY LEVEL", OptionsDialog.CMD_DIFFICULTY, Config.deformation, 0.5, 1.5, 0, null);
		local7.addSeparator();
		local7.addItem("STEERING HELP", OptionsDialog.CMD_STEERHELP, Config.player_steeringhelp, null);
		local7.addItem("ABS", OptionsDialog.CMD_ABS_SLIDER, Config.player_abs, null);
		local7.addItem("ASR", OptionsDialog.CMD_ASR_SLIDER, Config.player_asr, null);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_GAME_OPTIONS_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_GAME_OPTIONS_DONE, this);
		osd.hideGroup((gameGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("ADVANCED GAME OPTIONS");
		local7 = osd.createMenu(butt0, 0.0, (-(0.6)), 0);
		local7.setSliderStyle(sld1, sldk);
		local7.addItem(
			"DRIVER MOVEMENT/STEER",
			OptionsDialog.CMD_HMF_1,
			Config.head_move_steer,
			0.0,
			2.0,
			0,
			"HOW THE STEERING WHEELS POSITION AFFECTS THE DRIVERS MOVEMENT"
		);
		local7.addItem(
			"DRIVER MOVEMENT/VELOCITY",
			OptionsDialog.CMD_HMF_2,
			Config.head_move_vel,
			0.0,
			2.0,
			0,
			"HOW THE CARS VELOCITY AFFECTS THE DRIVERS MOVEMENT"
		);
		local7.addItem(
			"DRIVER MOVEMENT/ACCELERATION",
			OptionsDialog.CMD_HMF_3,
			Config.head_move_acc,
			0.0,
			2.0,
			0,
			"HOW THE CARS ACCELERATION AFFECTS THE DRIVERS MOVEMENT"
		);
		local7.addSeparator();
		local7.addItem(
			"TRAFFIC DENSITY",
			OptionsDialog.CMD_TRAFFICDENSITY,
			Config.trafficDensity,
			0.0,
			1.5,
			0,
			null
		);
		local7.addItem(
			"PEDESTRIAN DENSITY",
			OptionsDialog.CMD_PEDESTRIANDENSITY,
			Config.pedestrianDensity,
			0.0,
			1.5,
			0,
			null
		);
		local7 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local7.addItem("BACK", OptionsDialog.CMD_GAME_OPTIONS_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_GAME_OPTIONS_DONE, this);
		osd.hideGroup((game2Group = osd.endGroup()));
		super.show();
		enableAnimateHook();
	}

	public void hide()
	{
		disableAnimateHook();
		super.hide();
	}

	public void addCustomGroups()
	{
	}

	public void setDetailText(Text arg0, int arg1)
	{
		if ((arg1 == Config.OFF))
		{
			arg0.changeText("Off");
		} else
		{
			if ((arg1 == Config.LOW))
			{
				arg0.changeText("Low");
			} else
			{
				if ((arg1 == Config.MID))
				{
					arg0.changeText("Mid");
				} else
				{
					if ((arg1 == Config.HIGH))
					{
						arg0.changeText("High");
					} else
					{
						if ((arg1 == Config.SHIGH))
						{
							arg0.changeText("Super High");
						}
					}
				}
			}
		}
	}

	public void setEnabledText(Text arg0, int arg1)
	{
		if ((arg1 == 0))
		{
			arg0.changeText("Disabled");
		} else
		{
			arg0.changeText("Enabled");
		}
	}

	public int ChangeDetail(int arg0, int arg1, int arg2, int arg3)
	{
		arg0 += arg1;
		if ((arg0 < arg2))
		{
			arg0 = arg3;
		}
		if ((arg0 > arg3))
		{
			arg0 = arg2;
		}
		return arg0;
	}

	public int fequal(float arg0, float arg1)
	{
		float local3 = (arg0 - arg1);
		if ((local3 < 0))
		{
			local3 = (-(local3));
		}
		if ((local3 < 0.0009999999))
		{
			return 1;
		}
		return 0;
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 < 0))
		{
			return;
		} else
		{
			if ((arg0 == OptionsDialog.CMD_COMPILE))
			{
				System.compileAll(".");
				new SfxRef(DestructionTools:0x39r).play();
			}
		}
		if ((arg0 == OptionsDialog.CMD_AXISCHECK))
		{
			axisCheckCallerGroup = actGroup;
			changeMode(axisCheckGroup);
			player.controller.setcontrol(controlSet);
		} else
		{
			if ((arg0 == OptionsDialog.CMD_AXISCHECK_DONE))
			{
				changeMode(axisCheckCallerGroup);
			} else
			{
				if ((arg0 == OptionsDialog.CMD_EFFECTS_VOL))
				{
					Sound.setVolume(Sound.CHANNEL_EFFECTS, osd.sliderValue);
				} else
				{
					if ((arg0 == OptionsDialog.CMD_MUSIC_VOL))
					{
						Sound.setVolume(Sound.CHANNEL_MUSIC, osd.sliderValue);
					} else
					{
						if ((arg0 == OptionsDialog.CMD_ENGINE_VOL))
						{
							Sound.setVolume(Sound.CHANNEL_ENGINE, osd.sliderValue);
						} else
						{
							if ((arg0 == OptionsDialog.CMD_OPTIONS))
							{
								changeMode(optionsGroup);
							} else
							{
								if ((arg0 == OptionsDialog.CMD_VIDEO_OPTIONS))
								{
									changeMode(videoGroup);
								} else
								{
									if ((arg0 == OptionsDialog.CMD_VIDEO_OPTIONS_DONE))
									{
										System.getConfigOptions();
										if (((((Config.video_x != OptionsDialog.aspectX) || (Config.video_y != OptionsDialog.aspectY)) || (Config.video_depth != color_depth)) || (Config.video_windowed != windowed)))
										{
											int local2 = 0;
											if ((Config.video_windowed == 1))
											{
												int local3 = (0 == new NoYesDialog(GameLogic.player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "WARNING", "Game restart is requiring to apply changes. \n Do you want make this now? \n \n Note that your current career will be saved.").display());
												Config.video_windowed = windowed;
												Config.video_x = OptionsDialog.aspectX;
												Config.video_y = OptionsDialog.aspectY;
												Config.video_depth = color_depth;
												if ((local3 == 1))
												{
													GameLogic.autoSaveQuiet();
													GameLogic.changeActiveSection(null);
												} else
												{
													changeMode(optionsGroup);
												}
											} else
											{
												if ((Config.video_windowed != windowed))
												{
													int local3_2 = (0 == new NoYesDialog(GameLogic.player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "WARNING", "Game restart is requiring to apply changes. \n Do you want make this now? \n \n Note that your current career will be saved.").display());
													Config.video_windowed = windowed;
													Config.video_x = OptionsDialog.aspectX;
													Config.video_y = OptionsDialog.aspectY;
													Config.video_depth = color_depth;
													if ((local3_2 == 1))
													{
														GameLogic.autoSaveQuiet();
														GameLogic.changeActiveSection(null);
													} else
													{
														changeMode(optionsGroup);
													}
												} else
												{
													int local3_3 = 2;
													while ((local3_3 <= (ResX_list.length - 1)))
													{
														if (((OptionsDialog.aspectX == OptionsDialog.ResX_list[local3_3]) && (OptionsDialog.aspectY == OptionsDialog.ResY_list[local3_3])))
														{
															local2 = 1;
															local3_3 += ResX_list.length;
														}
														local3_3++;
													}
													if (local2)
													{
														Config.video_windowed = windowed;
														Config.video_x = OptionsDialog.aspectX;
														Config.video_y = OptionsDialog.aspectY;
														Config.video_depth = color_depth;
														GfxEngine.changeVideoMode(320, 240, 16);
														GfxEngine.changeVideoMode(640, 480, 32);
														GfxEngine.changeVideoMode(
															Config.video_x,
															Config.video_y,
															Config.video_depth
														);
														Frontend.setFonts();
														changeMode(optionsGroup);
													} else
													{
														new WarningDialog(
															GameLogic.player.controller,
															(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
															"WARNING",
															"This resolution can't be use at full screen mode! \n \n Please change resolution or video mode.").display(
														);
													}
												}
											}
										} else
										{
											changeMode(optionsGroup);
											if ((tmp_lp != loading_primary))
											{
												if ((0 == new NoYesDialog(GameLogic.player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "WARNING", "Game restart is requiring to apply changes of Loading Primary. \n Do you want make this now? \n \n Note that your current career will be saved.").display()))
												{
													GameLogic.autoSaveQuiet();
													GameLogic.changeActiveSection(null);
												}
											}
										}
									} else
									{
										if ((arg0 == OptionsDialog.CMD_M_GROUP))
										{
											changeMode(MirrorGroup);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if ((arg0 == OptionsDialog.CMD_RESOLUTION))
		{
			if ((Current_Resolution < (ResX_list.length - 1)))
			{
				Current_Resolution += 1;
			} else
			{
				Current_Resolution = 0;
			}
			OptionsDialog.aspectX = OptionsDialog.ResX_list[Current_Resolution];
			OptionsDialog.aspectY = OptionsDialog.ResY_list[Current_Resolution];
			osdCommand(OptionsDialog.CMD_REFRESH_TEXT);
		} else
		{
			if ((arg0 == OptionsDialog.CMD_REFRESH_TEXT))
			{
				if ((!(OptionsDialog.aspectX)))
				{
					OptionsDialog.aspectX = Config.video_x;
					OptionsDialog.aspectY = Config.video_y;
				}
				int local2_2 = ((10 * OptionsDialog.aspectX) / OptionsDialog.aspectY);
				aspect = local2_2;
				if ((aspect == 10))
				{
					Res_Aspect = "1:1";
				} else
				{
					if ((aspect == 12))
					{
						Res_Aspect = "5:4";
					} else
					{
						if ((aspect == 13))
						{
							Res_Aspect = "4:3";
						} else
						{
							if ((aspect == 14))
							{
								Res_Aspect = "13:9";
							} else
							{
								if ((aspect == 15))
								{
									Res_Aspect = "14:9";
								} else
								{
									if ((aspect == 16))
									{
										Res_Aspect = "5:3";
									} else
									{
										if ((aspect == 17))
										{
											Res_Aspect = "16:9";
										} else
										{
											if ((aspect == 20))
											{
												Res_Aspect = "6:3";
											} else
											{
												if ((aspect == 23))
												{
													Res_Aspect = "12:5";
												} else
												{
													if ((aspect == 24))
													{
														Res_Aspect = "12:5";
													} else
													{
														Res_Aspect = "OTHER";
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
				if (Resolution_button)
				{
					Resolution_button.changeLabelText((((((("SCREEN RESOLUTION: " + Float.toString(OptionsDialog.aspectX, "%.0f")) + " X ") + Float.toString(OptionsDialog.aspectY, "%.0f")) + " (") + Res_Aspect) + ")"));
					Depth_button.changeLabelText(("COLOR DEPTH: " + color_depth));
					if ((windowed == 0))
					{
						windowed_button.changeLabelText("DISPLAY MODE: FULL SCREEN");
					} else
					{
						windowed_button.changeLabelText("DISPLAY MODE: WINDOWED");
					}
				}
			} else
			{
				if ((arg0 == OptionsDialog.CMD_DEPTH))
				{
					if ((Config.video_depth == 16))
					{
						color_depth = 32;
					} else
					{
						color_depth = 16;
					}
					Depth_button.changeLabelText(("COLOR DEPTH: " + color_depth));
				}
			}
		}
		if ((arg0 == OptionsDialog.CMD_MODE))
		{
			if ((windowed == 0))
			{
				windowed = 1;
				windowed_button.changeLabelText("DISPLAY MODE: WINDOWED");
			} else
			{
				windowed = 0;
				windowed_button.changeLabelText("DISPLAY MODE: FULL SCREEN");
			}
		}
		if ((arg0 == OptionsDialog.CMD_UNRES))
		{
			new UR_Dialog().display();
			osdCommand(OptionsDialog.CMD_REFRESH_TEXT);
		}
		if ((arg0 == OptionsDialog.CMD_TEXTURE_DETAIL))
		{
			textureDetail = osd.multiValue;
			Config.texture_size = (5 - textureDetail);
		} else
		{
			if ((arg0 == OptionsDialog.CMD_ABS_SLIDER))
			{
				Config.player_abs = osd.sliderValue;
			} else
			{
				if ((arg0 == OptionsDialog.CMD_ASR_SLIDER))
				{
					Config.player_asr = osd.sliderValue;
				} else
				{
					if ((arg0 == OptionsDialog.CMD_HMF_1))
					{
						Config.head_move_steer = osd.sliderValue;
					} else
					{
						if ((arg0 == OptionsDialog.CMD_HMF_2))
						{
							Config.head_move_vel = osd.sliderValue;
						} else
						{
							if ((arg0 == OptionsDialog.CMD_HMF_3))
							{
								Config.head_move_acc = osd.sliderValue;
							} else
							{
								if ((arg0 == OptionsDialog.CMD_SHADOW_DETAIL))
								{
									shadow_size = osd.multiValue;
									if ((shadow_size == 5))
									{
										Config.shadow_size = 1024;
										Config.shadows = 2;
									} else
									{
										if ((shadow_size == 4))
										{
											Config.shadow_size = 512;
											Config.shadows = 2;
										} else
										{
											if ((shadow_size == 3))
											{
												Config.shadow_size = 256;
												Config.shadows = 2;
											} else
											{
												if ((shadow_size == 2))
												{
													Config.shadow_size = 128;
													Config.shadows = 1;
												} else
												{
													if ((shadow_size == 1))
													{
														Config.shadow_size = 64;
														Config.shadows = 1;
													} else
													{
														Config.shadow_size = 32;
														Config.shadows = 0;
													}
												}
											}
										}
									}
									refreshCameras();
								} else
								{
									if ((arg0 == OptionsDialog.CMD_SHADOW_RANGE))
									{
										Config.shadow_detail = (1 / osd.sliderValue);
										Config.shadow_detail = (Config.shadow_detail * 10);
										ShadowDist_Text.changeLabelText(("SHADOW VIEW DISTANCE: " + Float.toString(((1 / Config.shadow_detail) * 10), "%.0f m")));
										refreshCameras();
									} else
									{
										if ((arg0 == OptionsDialog.CMD_LP))
										{
											loading_primary = osd.multiValue;
											if ((loading_primary == 0))
											{
												Config.resource_refreshrate = 2;
												Config.resource_loadrate = 10;
											} else
											{
												if ((loading_primary == 1))
												{
													Config.resource_refreshrate = 512;
													Config.resource_loadrate = 512;
												}
											}
											if ((loading_primary == 2))
											{
												Config.resource_refreshrate = 2048;
												Config.resource_loadrate = 512;
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
		if ((arg0 == OptionsDialog.CMD_FLARES))
		{
			Config.flares = osd.multiValue;
			refreshCameras();
		} else
		{
			if ((arg0 == OptionsDialog.CMD_HEADLIGHTS))
			{
				Config.headlight_rays = osd.multiValue;
				refreshCameras();
			} else
			{
				if ((arg0 == OptionsDialog.CMD_BGSCENE))
				{
					Config.BgScene = osd.multiValue;
				} else
				{
					if ((arg0 == OptionsDialog.CMD_MIRROR))
					{
						Config.mirror = osd.multiValue;
					} else
					{
						if ((arg0 == OptionsDialog.CMD_VIEW_RANGE))
						{
							Config.cameras_viewrange = osd.sliderValue;
							Config.camera_ext_viewrange = osd.sliderValue;
							Config.camera_int_viewrange = osd.sliderValue;
							Dist_Text.changeLabelText(("VIEW DISTANCE: " + Float.toString(Config.camera_ext_viewrange, "%.0f m")));
							refreshCameras();
						} else
						{
							if ((arg0 == OptionsDialog.CMD_MIRROR_DISTANCE))
							{
								Config.camera_mirror_viewrange = osd.sliderValue;
								MirrorDist_Text.changeLabelText(("MIRROR VIEW DISTANCE: " + Float.toString(Config.camera_mirror_viewrange, "%.0f m")));
								refreshCameras();
							} else
							{
								if ((arg0 == OptionsDialog.CMD_OBJECT_DETAIL))
								{
									Config.object_detail = osd.sliderValue;
									refreshCameras();
								} else
								{
									if ((arg0 == OptionsDialog.CMD_LOD_DETAIL))
									{
										Config.object_detail_amp = osd.sliderValue;
										refreshCameras();
									} else
									{
										if ((arg0 == OptionsDialog.CMD_PARTICLE))
										{
											Config.particle_density = osd.sliderValue;
											Particle_Text.changeLabelText((("PARTICLE DENSITY: " + Float.toString((Config.particle_density * 100), "%.0f")) + "%"));
											refreshCameras();
										} else
										{
											if ((arg0 == OptionsDialog.CMD_GAMMA))
											{
												Config.video_gamma = osd.sliderValue;
												refreshCameras();
											} else
											{
												if ((arg0 == OptionsDialog.CMD_CONTROL_OPTIONS))
												{
													changeMode(controlGroup);
												} else
												{
													if ((arg0 == OptionsDialog.CMD_GAME_OPTIONS))
													{
														changeMode(gameGroup);
													} else
													{
														if ((arg0 == OptionsDialog.CMD_GAME_OPTIONS2))
														{
															changeMode(game2Group);
														} else
														{
															if ((arg0 == OptionsDialog.CMD_GPSMODE))
															{
																Config.gpsMode = osd.multiValue;
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
			}
		}
		if ((arg0 == OptionsDialog.CMD_METRIC))
		{
			Config.metricSystem = osd.multiValue;
			if ((Config.metricSystem == 0))
			{
				System.setMeasure(1600);
			} else
			{
				System.setMeasure(1000);
			}
		} else
		{
			if ((arg0 == OptionsDialog.CMD_DIFFICULTY))
			{
				Config.deformation = osd.sliderValue;
				Config.internal_damage = osd.sliderValue;
				Config.player_damage_multiplier = (osd.sliderValue / 2.0);
			} else
			{
				if ((arg0 == OptionsDialog.CMD_TRAFFICDENSITY))
				{
					Config.trafficDensity = osd.sliderValue;
				} else
				{
					if ((arg0 == OptionsDialog.CMD_PEDESTRIANDENSITY))
					{
						Config.pedestrianDensity = osd.sliderValue;
					}
				}
			}
		}
		if ((arg0 == OptionsDialog.CMD_SKIDMARK))
		{
			Config.skidmark_max = (osd.sliderValue * 4.0);
			SkidMark_Text.changeLabelText((("SKIDMARKS LENGTH: " + (Config.skidmark_max / 4)) + " m"));
		} else
		{
			if ((arg0 == OptionsDialog.CMD_CLUTCH))
			{
				Config.player_transmission = clutchTrfInv[osd.multiValue];
			} else
			{
				if ((arg0 == OptionsDialog.CMD_STEERHELP))
				{
					Config.player_steeringhelp = osd.sliderValue;
				} else
				{
					if ((arg0 == OptionsDialog.CMD_GAME_OPTIONS_DONE))
					{
						System.getConfigOptions();
						changeMode(optionsGroup);
					} else
					{
						if ((arg0 == OptionsDialog.CMD_SOUND_OPTIONS))
						{
							int local2_3 = Config.Sound_Mix_HW;
							if ((local2_3 == 2))
							{
								local2_3++;
							}
							if (((Config.Sound_Mix_HW == 1) && (Config.Sound_3D_HW == 1)))
							{
								local2_3++;
							}
							soundHWMulti.setValue(local2_3);
							changeMode(soundGroup);
						} else
						{
							if ((arg0 == OptionsDialog.CMD_SOUND_HW))
							{
								switch (osd.multiValue)
								{
									case 0:
										Config.Sound_Mix_HW = 0;
										Config.Sound_3D_HW = 0;
										break;
									case 1:
										Config.Sound_Mix_HW = 1;
										Config.Sound_3D_HW = 0;
										break;
									case 2:
										Config.Sound_Mix_HW = 1;
										Config.Sound_3D_HW = 1;
										break;
									case 3:
										Config.Sound_Mix_HW = 2;
										Config.Sound_3D_HW = 2;
										break;
								}
							} else
							{
								if ((arg0 == OptionsDialog.CMD_MAIN))
								{
									changeMode(mainGroup);
								} else
								{
									if (((((arg0 == OptionsDialog.CMD_RED_SUSP) || (arg0 == OptionsDialog.CMD_REDEFINE_CONTROLS)) || (arg0 == OptionsDialog.CMD_REDEFINE_CONTROLS2)) || (arg0 == OptionsDialog.CMD_REDEFINE_CONTROLS3)))
									{
										int local2_4;
										controlSet = new ControlSet();
										controlSet.load(GameLogic.activeControlFile);
										int local3_4 = 0;
										while ((local3_4 < OptionsDialog.NCONTROLS))
										{
											if (keyText[local3_4])
											{
												if (((controlSet.deviceID[local3_4] < 0) || (controlSet.axisID[local3_4] < 0)))
												{
													keyText[local3_4].changeText("Undefined");
												} else
												{
													keyText[local3_4].changeText(Input.axisName(controlSet.deviceID[local3_4], controlSet.axisID[local3_4]));
												}
											}
											local3_4++;
										}
										powerSlider[0].setValue(((controlSet.vasp.elementAt(0).power - 1.0) / 4.0));
										powerSlider[1].setValue(((controlSet.vasp.elementAt(1).power - 1.0) / 4.0));
										powerSlider[2].setValue(((controlSet.vasp.elementAt(2).power - 1.0) / 4.0));
										powerSlider[3].setValue(((controlSet.vasp.elementAt(3).power - 1.0) / 4.0));
										dzSlider[0].setValue((controlSet.dead_zone[0] / 0.2));
										dzSlider[1].setValue((controlSet.dead_zone[1] / 0.2));
										dzSlider[2].setValue((controlSet.dead_zone[2] / 0.2));
										dzSlider[3].setValue((controlSet.dead_zone[7] / 0.2));
										if ((arg0 == OptionsDialog.CMD_REDEFINE_CONTROLS))
										{
											changeMode(redefineGroup);
										} else
										{
											if ((arg0 == OptionsDialog.CMD_REDEFINE_CONTROLS2))
											{
												changeMode(redefine2Group);
											} else
											{
												if ((arg0 == OptionsDialog.CMD_RED_SUSP))
												{
													changeMode(redefineSuspGroup);
												} else
												{
													changeMode(redefine3Group);
												}
											}
										}
									} else
									{
										if ((arg0 == OptionsDialog.CMD_REDEFINE_DONE))
										{
											controlSet.save(GameLogic.activeControlFile);
											changeMode(controlGroup);
										} else
										{
											if ((arg0 == OptionsDialog.CMD_RESET_CONTROLS))
											{
												controlSet = new ControlSet();
												controlSet.save(GameLogic.activeControlFile);
												player.controller.setcontrol(controlSet);
											} else
											{
												if ((arg0 == OptionsDialog.CMD_LOAD_CONTROLS))
												{
													ControlsFileReqDialog local2_5 = new ControlsFileReqDialog(
														player.controller,
														(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
														"LOAD CONTROL DEFINITIONS",
														"LOAD CONTROLS",
														GameLogic.controlSaveDir,
														"*"
													);
													if ((local2_5.display() == 0))
													{
														String local3_5 = (GameLogic.controlSaveDir + local2_5.fileName);
														controlSet = new ControlSet();
														controlSet.load(local3_5);
														controlSet.save(GameLogic.activeControlFile);
														player.controller.setcontrol(controlSet);
													}
												} else
												{
													if ((arg0 == OptionsDialog.CMD_SAVE_CONTROLS))
													{
														ControlsFileReqDialog local2_6 = new ControlsFileReqDialog(
															player.controller,
															((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.FRF_SAVE),
															"SAVE CONTROL DEFINITIONS",
															"SAVE CONTROLS",
															GameLogic.controlSaveDir,
															"*"
														);
														if ((local2_6.display() == 0))
														{
															String local3_6 = (GameLogic.controlSaveDir + local2_6.fileName);
															if (File.exists(GameLogic.activeControlFile))
															{
																File.copy(
																	GameLogic.activeControlFile,
																	local3_6
																);
															} else
															{
																controlSet = new ControlSet();
																controlSet.save(local3_6);
															}
														}
													} else
													{
														if (((arg0 >= OptionsDialog.CMD_GETKEY) && (arg0 < (OptionsDialog.CMD_GETKEY + OptionsDialog.NCONTROLS))))
														{
															if ((getKey == (-(1))))
															{
																getKey = (arg0 - OptionsDialog.CMD_GETKEY);
																keyText[getKey].changeText("_");
																getKeyStat = 0;
																Input.getAxis(
																	1,
																	((-(1)) - (Config.mouseSensitivity * 100.0))
																);
															}
														} else
														{
															if (((arg0 >= OptionsDialog.CMD_DEAD_ZONE) && (arg0 < (OptionsDialog.CMD_DEAD_ZONE + 4))))
															{
																int local2_7;
																local2_7 = (arg0 - OptionsDialog.CMD_DEAD_ZONE);
																if ((local2_7 == 3))
																{
																	local2_7 = 7;
																}
																controlSet.dead_zone[local2_7] = (osd.sliderValue * 0.2);
																controlSet.dead_zone[(local2_7 + 15)] = (osd.sliderValue * 0.2);
																player.controller.setcontrol(controlSet);
															} else
															{
																if (((arg0 >= OptionsDialog.CMD_POWER) && (arg0 < (OptionsDialog.CMD_POWER + 4))))
																{
																	float local2_8 = (1.125 + (4.0 * osd.sliderValue));
																	int local3_7 = (local2_8 * 4);
																	local2_8 = local3_7;
																	local2_8 = (local2_8 / 4.0);
																	controlSet.vasp.elementAt((arg0 - OptionsDialog.CMD_POWER)).power = local2_8;
																	player.controller.setcontrol(controlSet);
																} else
																{
																	if ((arg0 == (OptionsDialog.CMD_FFB + 0)))
																	{
																		Config.FFB_strength = osd.sliderValue;
																		System.getConfigOptions();
																	} else
																	{
																		if ((arg0 == (OptionsDialog.CMD_FFB + 1)))
																		{
																			Config.FFB_strength_emulated = osd.sliderValue;
																			System.getConfigOptions();
																		} else
																		{
																			if ((arg0 == OptionsDialog.CMD_MOUSE_SENS))
																			{
																				Config.mouseSensitivity = osd.sliderValue;
																				if ((Config.mouseSensitivity < 0.1))
																				{
																					Config.mouseSensitivity = 0.1;
																				}
																				Input.getAxis(
																					1,
																					((-(1)) - (Config.mouseSensitivity * 100.0))
																				);
																				Input.cursor.config();
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
							}
						}
					}
				}
			}
		}
	}

	public void changeMode(int arg0)
	{
		if ((actGroup != arg0))
		{
			if ((actGroup >= 0))
			{
				osd.hideGroup(actGroup);
			}
			actGroup = arg0;
			osd.showGroup(actGroup);
			osd.changeSelection2((-(1)), 0);
			if ((actGroup == videoGroup))
			{
				osdCommand(OptionsDialog.CMD_REFRESH_TEXT);
			}
		}
	}

	public void animate()
	{
		if ((((actGroup == redefineGroup) || (actGroup == redefine2Group)) || (actGroup == redefineSuspGroup)))
		{
			if (((getKey >= 0) && (getKey < OptionsDialog.NCONTROLS)))
			{
				int local1;
				int local2 = (-(1));
				local1 = 0;
				while ((local1 < controlSet.nDevices))
				{
					local2 = Input.activeAxis(local1);
					if ((local2 >= 0))
					{
						break;
					}
					local1++;
				}
				if ((getKeyStat == 0))
				{
					if ((local2 < 0))
					{
						getKeyStat = 1;
					}
				} else
				{
					if ((getKeyStat == 1))
					{
						if (((local1 == 0) && (local2 == Input.RCDIK_ESCAPE)))
						{
							keyText[getKey].changeText("Undefined");
							controlSet.change(getKey, (-(1)), (-(1)));
							player.controller.setcontrol(controlSet);
							getKeyStat = 2;
						} else
						{
							if ((local2 >= 0))
							{
								float local3 = Input.getAxis(local1, local2);
								keyText[getKey].changeText(Input.axisName(local1, local2));
								controlSet.change(getKey, local1, local2);
								int local4;
								local4 = 0;
								while ((local4 < OptionsDialog.NCONTROLS))
								{
									if ((((local4 != getKey) && (controlSet.deviceID[local4] == local1)) && (controlSet.axisID[local4] == local2)))
									{
										break;
									}
									local4++;
								}
								if (((local1 == 0) || (local3 > 0.95000005)))
								{
									controlSet.from_min[getKey] = 0.0;
									controlSet.from_max[getKey] = 1.0;
								} else
								{
									if ((local4 < OptionsDialog.NCONTROLS))
									{
										if ((local3 < 0.0))
										{
											controlSet.from_min[getKey] = 0.0;
											controlSet.from_max[getKey] = (-(1.0));
											controlSet.from_min[local4] = 0.0;
											controlSet.from_max[local4] = 1.0;
										} else
										{
											controlSet.from_min[getKey] = 0.0;
											controlSet.from_max[getKey] = 1.0;
											controlSet.from_min[local4] = 0.0;
											controlSet.from_max[local4] = (-(1.0));
										}
									} else
									{
										controlSet.from_min[getKey] = (-(1.0));
										controlSet.from_max[getKey] = 1.0;
									}
								}
								player.controller.setcontrol(controlSet);
								getKeyStat = 2;
							}
						}
					} else
					{
						if ((local2 < 0))
						{
							getKey = (-(1));
						}
					}
				}
			}
		}
		if ((actGroup == axisCheckGroup))
		{
			acGasSlider.setValue((Input.getInput(Input.AXIS_THROTTLE) / 128.0));
			acBreakSlider.setValue((Input.getInput(Input.AXIS_BRAKE) / 128.0));
			acSteerSlider.setValue(((Input.getInput(Input.AXIS_TURN_LEFTRIGHT) / (-(256.0))) + 0.5));
			acClutchSlider.setValue((Input.getInput(Input.AXIS_CLUTCH) / 128.0));
		}
	}

	public void refreshCameras()
	{
		System.getConfigOptions();
		if (activeCamera)
		{
			activeCamera.command("render 0");
		}
		if ((GameLogic.player && GameLogic.player.car))
		{
			GameLogic.player.car.command(("render 0 " + GameLogic.player.controller.id()));
		}
	}

	public void setActiveCamera(GameRef arg0)
	{
		activeCamera = arg0;
	}

}

public class ControlsFileReqDialog extends FileRequesterDialog
{
	public ControlsFileReqDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, String arg5)
	{
		super(arg0, arg1, arg2, arg3, arg4, arg5);
		if ((arg1 & Dialog.FRF_SAVE))
		{
			osd.defSelection = 5;
		}
	}

	public int validator(String arg0)
	{
		return ControlSet.fileCheck(arg0);
	}

}

public class UR_Dialog extends Dialog
{
	static final int CMD_BACK = 15;
	static final int CMD_OK = 16;
	static final int CMD_X_UP = 20;
	static final int CMD_X_DOWN = 21;
	static final int CMD_Y_UP = 22;
	static final int CMD_Y_DOWN = 23;
	Menu m;
	Style Menu_Center;
	Style Menu_Button;
	Text ResText;
	Text AspectText;
	String Res_Aspect;
	int[] Res_list;
	int SizeX;
	int SizeY;
	int numberX;
	int numberY;
	float aspect;

	public UR_Dialog()
	{
		super(
			GameLogic.player.controller,
			(((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_HIGHPRI) | Dialog.DF_DARKEN),
			"UNCOMMON RESOLUTION",
			""
		);
		Res_list = new int[24];
		Res_list[0] = 120;
		Res_list[1] = 160;
		Res_list[2] = 200;
		Res_list[3] = 213;
		Res_list[4] = 240;
		Res_list[5] = 300;
		Res_list[6] = 320;
		Res_list[7] = 360;
		Res_list[8] = 384;
		Res_list[9] = 400;
		Res_list[10] = 480;
		Res_list[11] = 512;
		Res_list[12] = 576;
		Res_list[13] = 600;
		Res_list[14] = 640;
		Res_list[15] = 720;
		Res_list[16] = 768;
		Res_list[17] = 800;
		Res_list[18] = 848;
		Res_list[19] = 864;
		Res_list[20] = 960;
		Res_list[21] = 1024;
		Res_list[22] = 1152;
		Res_list[23] = 1280;
		int local1 = 0;
		while ((local1 <= (Res_list.length - 1)))
		{
			if ((OptionsDialog.aspectX == Res_list[local1]))
			{
				numberX = local1;
				local1 += Res_list.length;
			}
			local1++;
		}
		int local1_2 = 0;
		while ((local1_2 <= (Res_list.length - 1)))
		{
			if ((OptionsDialog.aspectY == Res_list[local1_2]))
			{
				numberY = local1_2;
				local1_2 += Res_list.length;
			}
			local1_2++;
		}
		Menu_Center = new Style(0.5, 0.13, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
		Menu_Button = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		ResText = osd.createText("----   X   ----", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.3)));
		ResText.changeColor(-4144960);
		m = osd.createMenu(Menu_Button, (-(0.5)), (-(0.5)), 0, Osd.MD_VERTICAL);
		m.addItem(new ResourceRef(frontend:0x4cr), UR_Dialog.CMD_X_UP, "Increase X size");
		m = osd.createMenu(Menu_Button, (-(0.5)), 0.0, 0, Osd.MD_VERTICAL);
		m.addItem(new ResourceRef(frontend:0x84r), UR_Dialog.CMD_X_DOWN, "Decrease X size");
		m = osd.createMenu(Menu_Button, 0.5, (-(0.5)), 0, Osd.MD_VERTICAL);
		m.addItem(new ResourceRef(frontend:0x4cr), UR_Dialog.CMD_Y_UP, "Increase Y size");
		m = osd.createMenu(Menu_Button, 0.5, 0.0, 0, Osd.MD_VERTICAL);
		m.addItem(new ResourceRef(frontend:0x84r), UR_Dialog.CMD_Y_DOWN, "Decrease Y size");
		AspectText = osd.createText("Aspect: N/A", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, 0.4);
		AspectText.changeColor(-4144960);
		m = osd.createMenu(Menu_Center, (-(0.4)), 0.90000004, 0, Osd.MD_HORIZONTAL);
		m.addItem("OK", UR_Dialog.CMD_OK);
		m = osd.createMenu(Menu_Center, 0.4, 0.90000004, 0, Osd.MD_HORIZONTAL);
		m.addItem("BACK", UR_Dialog.CMD_BACK);
		Refresh();
		super.show();
	}

	public void Refresh()
	{
		float local1 = Res_list[numberX];
		float local2 = Res_list[numberY];
		aspect = (local1 / local2);
		int local3 = (10 * aspect);
		if ((local3 == 10))
		{
			Res_Aspect = "1:1";
		} else
		{
			if ((local3 == 12))
			{
				Res_Aspect = "5:4";
			} else
			{
				if ((local3 == 13))
				{
					Res_Aspect = "4:3";
				} else
				{
					if ((local3 == 14))
					{
						Res_Aspect = "13:9";
					} else
					{
						if ((local3 == 15))
						{
							Res_Aspect = "14:9";
						} else
						{
							if ((local3 == 16))
							{
								Res_Aspect = "5:3";
							} else
							{
								if ((local3 == 17))
								{
									Res_Aspect = "16:9";
								} else
								{
									if ((local3 == 20))
									{
										Res_Aspect = "6:3";
									} else
									{
										if ((local3 == 23))
										{
											Res_Aspect = "12:5";
										} else
										{
											if ((local3 == 24))
											{
												Res_Aspect = "12:5";
											} else
											{
												Res_Aspect = "OTHER";
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
		ResText.changeText(((Res_list[numberX] + "   X   ") + Res_list[numberY]));
		AspectText.changeText((((("Aspect: " + aspect) + " (") + Res_Aspect) + ")"));
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == UR_Dialog.CMD_X_DOWN))
		{
			if ((numberX > 0))
			{
				numberX -= 1;
			} else
			{
				numberX = (Res_list.length - 1);
			}
			Refresh();
		} else
		{
			if ((arg0 == UR_Dialog.CMD_X_UP))
			{
				if ((numberX < (Res_list.length - 1)))
				{
					numberX += 1;
				} else
				{
					numberX = 0;
				}
				Refresh();
			} else
			{
				if ((arg0 == UR_Dialog.CMD_Y_DOWN))
				{
					if ((numberY > 0))
					{
						numberY -= 1;
					} else
					{
						numberY = (Res_list.length - 1);
					}
					Refresh();
				} else
				{
					if ((arg0 == UR_Dialog.CMD_Y_UP))
					{
						if ((numberY < (Res_list.length - 1)))
						{
							numberY += 1;
						} else
						{
							numberY = 0;
						}
						Refresh();
					} else
					{
						if ((arg0 == UR_Dialog.CMD_BACK))
						{
							osd.hide();
							super.hide();
						} else
						{
							if ((arg0 == UR_Dialog.CMD_OK))
							{
								OptionsDialog.aspectX = Res_list[numberX];
								OptionsDialog.aspectY = Res_list[numberY];
								osd.hide();
								super.hide();
							}
						}
					}
				}
			}
		}
	}

}
