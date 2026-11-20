package java.game;

import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class Track extends Scene implements GameState
{
	static final int RID_CHEVYVAN = system:0x0r;
	static final int CMD_CHANGECAM_TV = 0;
	static final int CMD_INGAMEMENU = 1;
	static final int CMD_MECHANIC = 2;
	static final int CMD_CAMMOVE = 3;
	static final int CMD_OPPCAM = 4;
	static final int CMD_PAINT = 5;
	static final int CMD_OSDONOFF = 6;
	static final int CMD_CHANGECAM_INT = 7;
	static final int CMD_CHANGECAM_EXT = 8;
	static final int CMD_CRUISECONTROL = 9;
	static final int CMD_CHANGECAM_CHASE = 10;
	static final int CMD_CHANGECAM_INVCH = 11;
	static final int CMD_CHANGECAM_FREE = 12;
	static final int CMD_SIMSPEEDINC = 13;
	static final int CMD_SIMSPEEDDEC = 14;
	static final int CMD_SIMPAUSE = 15;
	static final int CMD_CAMROTATE = 16;
	static final int CMD_QUICKREPAIR = 17;
	static final int CMD_CHANGECAMTARGET = 18;
	static final int CMD_EXIT = 19;
	static final int CMD_MIRROR = 20;
	static final int CMD_REPAIR = 21;
	static final int CMD_MAP = 22;
	static final int CMD_LOOKBACK = 23;
	static final int CMD_SUSP_FL = 41;
	static final int CMD_SUSP_F = 42;
	static final int CMD_SUSP_FR = 43;
	static final int CMD_SUSP_LS = 44;
	static final int CMD_SUSP_ALL = 45;
	static final int CMD_SUSP_RS = 46;
	static final int CMD_SUSP_RL = 47;
	static final int CMD_SUSP_R = 48;
	static final int CMD_SUSP_RR = 49;
	static final int CMD_SUSP_MODE = 50;
	static final int CAMMODE_NONE = 0;
	static final int CAMMODE_FOLLOW = 1;
	static final int CAMMODE_TV = 2;
	static final int CAMMODE_CHASE = 3;
	static final int CAMMODE_INVCH = 4;
	static final int CAMMODE_INTERNAL = 5;
	static final int CAMMODE_FREE = 6;
	static final int CAMMODE_POINT = 7;
	static final float GPS_Min_Zoom = 2;
	static final float GPS_Max_Zoom = 3;
	abstract Vector3 posStart;
	abstract Vector3 posStart2;
	abstract Ypr oriStart;
	abstract Ypr oriStart2;
	int activeTrigger;
	Vector trigger = new Vector();
	GameState parentState;
	Osd osd;
	Osd MirrorOsd;
	int osdCounter;
	IngameMenu ingameMenu;
	int im_active;
	int camRotate;
	int killCar;
	int Color_Number;
	Navigator nav;
	Marker mPlayer;
	Marker mPlayer2;
	Player player;
	Player player2;
	GameRef cursor;
	Text messages;
	Text infoline;
	int msgtimers;
	int osdEnabled;
	GameRef cam_external;
	GameRef cam;
	GameRef MirCam;
	GameRef cameraTarget;
	GameRef cameraTarget2;
	int cameraMode;
	int cameraNum;
	Vector3 lastCamPos;
	Vector3 lastCrossPos;
	Viewport vport2;
	Viewport vp2;
	Mechanic mechanic;
	Painter painter;
	int mc_active;
	int pt_active;
	Rectangle MirrorFrame;
	Rectangle MapViewFrame;
	GameRef MirCam2;
	float Rot;
	float Speed;
	float TimeTextWait;
	float camDist;
	int IntCamNumber;
	int MirrorStatus = 0;
	int TimeTextStatus;
	int GetCarStatus = 0;
	int RepairStatus = 0;
	int MapStatus;
	int OsdStatus;
	int MapZ1;
	int MapZ2;
	Text TimeText;
	Text TimeText2;
	GameRef Test;
	RenderRef Test_Render;
	Vector3 rota;

	public static final float Check(float arg0)
	{
		return (Math.sqrt(arg0) + 3.75);
	}

	public Track()
	{
		super();
		createNativeInstance();
		player = GameLogic.player;
		player2 = GameLogic.player2;
	}

	public void animate()
	{
		if (((TimeTextStatus == 1) && (TimeTextWait < System.currentTime())))
		{
			float local1 = System.timeWarp((-(1.0)));
			if (((local1 == 1.0) || (local1 == 0.0)))
			{
				TimeTextStatus = 0;
				TimeText.changeText("");
			}
		}
		if ((nav && nav.cam))
		{
			if (player.car)
			{
				Speed = (Math.sqrt(player.car.getSpeedSquare()) * 0.08);
				if ((Speed > Track.GPS_Max_Zoom))
				{
					Speed = Track.GPS_Max_Zoom;
				}
			}
			Vector3 local1_2;
			Ypr local2;
			Vector3 local3;
			Ypr local4;
			if (renderer)
			{
				local3 = renderer.getPos();
				local4 = renderer.getOri();
				nav.gmCamera.setMatrix(new Vector3(local3.x, 0.1, local3.z), new Ypr(local4.y, 0.0, 0.0));
				nav.rmCamera.setMatrix(
					new Vector3((local3.x / 100), 0.1, (local3.z / 100)),
					new Ypr(local4.y, 0.0, 0.0)
				);
			}
			if ((((Config.gpsMode == 0) && player.car) || ((Config.gpsMode == 1) && player.car)))
			{
				local1_2 = player.car.getPos();
				Rot = 0;
				if ((Config.gpsMode == 1))
				{
					Rot = player.car.getOri().y;
				}
			} else
			{
				if ((((Config.gpsMode == 2) && renderer) || ((Config.gpsMode == 3) && renderer)))
				{
					local1_2 = local3;
					Rot = 0;
					if ((Config.gpsMode == 3))
					{
						Rot = local4.y;
					}
				}
			}
			nav.updateNavigator(player.car);
			nav.cam.setMatrix(
				new Vector3((local1_2.x / 100), (Track.GPS_Min_Zoom + Speed), (local1_2.z / 100)),
				new Ypr(Rot, (-(1.57)), 0.0)
			);
		}
	}

	public void Mirror(int arg0)
	{
		if ((arg0 == 1))
		{
			if ((!(MirrorFrame)))
			{
				MirrorFrame = MirrorOsd.createRectangle(
					0.0,
					(-(0.76)),
					1.04,
					0.46,
					2,
					new ResourceRef(MW_Mod:0x23r)
				);
			}
			if ((!(vp2)))
			{
				vp2 = new Viewport(16, 0.25, 0.02, 0.5, 0.2);
			}
			GameRef local2 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
			cameraTarget.command(((((("render " + vp2.id()) + " ") + local2.id()) + " ") + 6));
			vp2.activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
		} else
		{
			if ((arg0 == 0))
			{
				if (MirrorFrame)
				{
					MirrorFrame.finalize();
					MirrorFrame = null;
				}
				if (vp2)
				{
					vp2.deactivate();
					vp2 = null;
				}
			}
		}
	}

	public void enter(GameState arg0)
	{
		float local2 = (Math.random() * 100);
		if ((MainMenu.Check(local2) != (Math.sqrt(local2) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		if ((arg0 instanceof RaceSetup))
		{
		} else
		{
			enterAsyncMode_Script();
			t.setPriority(Thread.MAX_PRIORITY);
			GameLogic.autoSaveQuiet();
			parentState = arg0;
			MirrorOsd = new Osd(1.0, 0.0, 18);
			osd = new Osd();
			osd.menuKeysCreated = 1;
			osd.alienMp = mp;
			addSceneElements(GameLogic.getTime());
			msgtimers = 0;
			messages = osd.createText(null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.4)));
			TimeText = osd.createText(null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.5)));
			TimeText.changeColor(-10752);
			TimeText2 = osd.createText(null, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.4)));
			TimeText2.changeColor(-10752);
			setEventMask((((((GameRef.EVENT_CURSOR | GameRef.EVENT_COMMAND) | GameRef.EVENT_HOTKEY) | GameRef.EVENT_TRIGGER_ON) | GameRef.EVENT_TRIGGER_OFF) | GameRef.EVENT_TIME));
			player.render = new RenderRef(map, player.driverID, "player");
			player.controller.command(("renderinstance " + player.render.id()));
			if (player.car)
			{
				if ((GameLogic.gameMode != GameLogic.GM_DEMO))
				{
					lockCar();
					player.car.command("setsteer 0");
					player.car.setDefaultTransmission();
					player.car.setDefaultSteeringHelp();
					player.controller.command(("controllable " + player.car.id()));
				}
				lastCamPos = new Vector3(0, 3, 6);
				lastCamPos.add(player.car.getPos());
				changeCamTarget(player.car);
				changeCamFollow();
				map.command(("obs_add " + player.car.id()));
			}
			if ((GameLogic.gameMode == GameLogic.GM_MULTIPLAYER))
			{
				player2.render = new RenderRef(map, player2.driverID, "player2");
				player2.controller.command(("renderinstance " + player2.render.id()));
				if (player2.car)
				{
					if ((!(posStart2)))
					{
						posStart2 = new Vector3((posStart.x + 10), posStart.y, posStart.z);
						oriStart2 = new Ypr(oriStart.y, oriStart.p, oriStart.r);
					}
					player2.car.setParent(map);
					player2.car.setMatrix(posStart2, oriStart2);
					player2.car.command("reset");
					player2.car.command("reload");
					player2.car.command("setsteer 0");
					player2.car.setDefaultTransmission();
					player2.car.setDefaultSteeringHelp();
					player2.controller.command(("controllable " + player2.car.id()));
				}
			}
			ingameMenu = new IngameMenu(this);
			infoline = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.97, 0.57);
			osd.endGroup();
			painter = new Painter(player, osd, null, infoline, 1);
			mechanic = new Mechanic(player, osd, null, infoline, 1);
			osd.createHotkey(Input.RCDIK_TAB, (Input.KEY | Osd.HK_STATIC), Track.CMD_OSDONOFF, this);
			osd.createHotkey(Input.RCDIK_1, (Input.KEY | Osd.HK_STATIC), Track.CMD_SIMSPEEDDEC, this);
			osd.createHotkey(Input.RCDIK_2, (Input.KEY | Osd.HK_STATIC), Track.CMD_SIMSPEEDINC, this);
			osd.createHotkey(Input.RCDIK_3, (Input.KEY | Osd.HK_STATIC), Track.CMD_SIMPAUSE, this);
			osd.createHotkey(Input.RCDIK_4, (Input.KEY | Osd.HK_STATIC), Track.CMD_CAMROTATE, this);
			osd.createHotkey(Input.RCDIK_F1, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAM_INT, this);
			osd.createHotkey(Input.RCDIK_F2, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAM_TV, this);
			osd.createHotkey(Input.RCDIK_F3, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAM_EXT, this);
			osd.createHotkey(Input.RCDIK_F4, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAM_CHASE, this);
			osd.createHotkey(Input.RCDIK_F5, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAM_INVCH, this);
			osd.createHotkey(Input.RCDIK_F9, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAM_FREE, this);
			if (GameLogic.cheats)
			{
				osd.createHotkey(Input.RCDIK_F6, (Input.KEY | Osd.HK_STATIC), Track.CMD_REPAIR, this);
				osd.createHotkey(Input.RCDIK_F10, (Input.KEY | Osd.HK_STATIC), Track.CMD_QUICKREPAIR, this);
				osd.createHotkey(Input.RCDIK_F11, (Input.KEY | Osd.HK_STATIC), Track.CMD_CHANGECAMTARGET, this);
			}
			osd.createHotkey(Input.AXIS_MENU, (Input.VIRTUAL | Osd.HK_STATIC), Track.CMD_INGAMEMENU, this);
			osd.createHotkey(
				Input.AXIS_CURSOR_BUTTON2,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_CAMMOVE,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(Input.RCDIK_0, (Input.KEY | Osd.HK_STATIC), Track.CMD_MIRROR, this);
			osd.createHotkey(Input.AXIS_MAP, (Input.VIRTUAL | Osd.HK_STATIC), Track.CMD_MAP, this);
			osd.createHotkey(Input.AXIS_HELP, (Input.VIRTUAL | Osd.HK_STATIC), Track.CMD_CRUISECONTROL, this);
			osd.createHotkey(
				Input.AXIS_LOOK_REAR,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_LOOKBACK,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_1,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_FL,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_2,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_F,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_3,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_FR,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_4,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_LS,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_5,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_ALL,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_6,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_RS,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_7,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_RL,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_SPECIAL_8,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_R,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(
				Input.AXIS_FIRE_PRI,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Track.CMD_SUSP_RR,
				(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
			);
			osd.createHotkey(Input.AXIS_FIRE_SEC, (Input.VIRTUAL | Osd.HK_STATIC), Track.CMD_SUSP_MODE, this);
			MirrorOsd.show();
			osd.show();
			osdEnabled = 1;
			enableOsd(osdEnabled);
			if (nav)
			{
				mPlayer = nav.addMarker(player);
				if ((GameLogic.gameMode == GameLogic.GM_MULTIPLAYER))
				{
					mPlayer2 = nav.addMarker(GameLogic.player2);
				}
			}
			Sound.changeMusicSet(Sound.MUSIC_SET_DRIVING);
			Input.getAxis(1, ((-(1)) - (Config.mouseSensitivity * 100.0)));
			addTimer(1, 2);
			if (1)
			{
				if ((GameLogic.gameMode == GameLogic.GM_DEMO))
				{
					addTimer(5, 4);
				}
			}
		}
		new ResourceRef(particles:0x106r).cache();
		new ResourceRef(particles:0x2r).cache();
		new ResourceRef(particles:0xar).cache();
		new SfxRef(sound:0xbr).cache();
		new SfxRef(sound:0x6r).cache();
		new SfxRef(sound:0x15r).cache();
		new SfxRef(sound:0xar).cache();
		new SfxRef(sound:0x17r).cache();
		new SfxRef(sound:0x18r).cache();
		new SfxRef(sound:0x1ar).precache();
		new SfxRef(sound:0xdr).precache();
		if (((GameLogic.gameMode == GameLogic.GM_QUICKRACE) && (!((arg0 instanceof RaceSetup)))))
		{
		} else
		{
			Frontend.loadingScreen.display();
		}
		player.controller.reset();
		player.controller.activateState(ControlSet.DRIVERSET);
		player2.controller.reset();
		player2.controller.activateState(ControlSet.DRIVERSET);
		enableAnimateHook();
		Mirror(MirrorStatus);
	}

	public void exit(GameState arg0)
	{
		disableAnimateHook();
		if ((arg0 instanceof RaceSetup))
		{
		} else
		{
			removeAllTimers();
			trigger.removeAllElements();
			changeCamNone();
			if (player.car)
			{
				map.command(("obs_rem " + player.car.id()));
				posStart = player.car.getPos();
				oriStart = player.car.getOri();
				player.controller.command(("leave " + player.car.id()));
			}
			player.render.destroy();
			releaseCar();
			if ((killCar || ((GetCarStatus == 1) && (GameLogic.gameMode == GameLogic.GM_CARREER))))
			{
				player.car.destroy();
				player.car = null;
				killCar = 0;
			}
			if (((GameLogic.gameMode == GameLogic.GM_MULTIPLAYER) && player2.car))
			{
				map.command(("obs_rem " + player2.car.id()));
				posStart2 = player2.car.getPos();
				oriStart2 = player2.car.getOri();
				player2.controller.command(("leave " + player2.car.id()));
				player2.render.destroy();
				player2.car.command("start");
				player2.car.command("reset");
				player2.car.setParent(player2);
				player2.car.setMatrix(null, null);
			}
			mechanic.hide();
			painter.hide();
			remSceneElements();
			map.setWater(
				new Vector3(0.0, (-(1000000.0)), (-(1500.0))),
				new Vector3(0.0, 1.0, 0.0),
				300.0,
				50.0
			);
			map.delTraffic();
			map.setPedestrianDensity(0.0);
			map.unload();
			osdEnabled = 0;
			enableOsd(osdEnabled);
			if (nav)
			{
				nav.remMarker(mPlayer);
				mPlayer = null;
				if ((GameLogic.gameMode == GameLogic.GM_MULTIPLAYER))
				{
					nav.remMarker(mPlayer2);
				}
				mPlayer2 = null;
			}
			if (MirrorFrame)
			{
				MirrorFrame.finalize();
			}
			Mirror(0);
			osd.hide();
			osd = null;
			clearEventMask(GameRef.EVENT_ANY);
			leaveAsyncMode_Script();
			GfxEngine.flush();
		}
		System.timeWarp(1.0);
		player.controller.reset();
		player.controller.activateState(ControlSet.MENUSET);
	}

	public void resetCamera()
	{
		if (((cameraMode == Track.CAMMODE_TV) || (cameraMode == Track.CAMMODE_FOLLOW)))
		{
			cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
			renderer = cam;
			if (MB_Animator)
			{
				MB_Animator.changeCam(renderer);
			}
		}
	}

	public void changeCamTV()
	{
		if ((!(cameraTarget)))
		{
			return;
		}
		if ((!(cameraTarget.id())))
		{
			return;
		}
		Vector3 local1 = cameraTarget.getPos();
		if ((!(local1)))
		{
			return;
		}
		Vector3 local2 = new Vector3(local1);
		Vector3 local3 = cameraTarget.getVel();
		if (local3)
		{
			local3.mul(3.0);
			local1.add(local3);
		}
		if (((cameraMode == Track.CAMMODE_INTERNAL) && (!(cameraNum))))
		{
			player.car.command("filter 3 0");
		}
		Vector3 local4;
		if ((cameraMode == Track.CAMMODE_FREE))
		{
			player.controller.command(("leave " + cam.id()));
			player.controller.command(("controllable " + player.car.id()));
			player.controller.activateState(5, 0);
			lastCrossPos = map.getNearestCross(local1, 0);
			if (cam)
			{
				local4 = cam.getPos();
			} else
			{
				local4 = local1;
			}
		} else
		{
			local4 = map.getNearestCross(local1, 0);
			if (local4)
			{
				if ((cameraMode == Track.CAMMODE_TV))
				{
					if (lastCrossPos)
					{
						if ((local4.distance(lastCrossPos) < 0.1))
						{
							ResourceRef local5 = new ResourceRef(cameraTarget.getInfo(GameType.GII_BONE));
							cam.command((("look " + local5.id()) + " 0,0,-1"));
							return;
						}
					}
				}
				lastCrossPos = new Vector3(local4);
				local4.y += (0.2 + (3.0 * Math.random()));
			} else
			{
				if (cam)
				{
					local4 = cam.getPos();
				} else
				{
					local4 = local1;
				}
				if ((!(local4)))
				{
					local4 = lastCamPos;
				}
			}
			if ((cameraMode == Track.CAMMODE_TV))
			{
				if (lastCamPos)
				{
					if ((local4.distance(lastCamPos) < 0.1))
					{
						return;
					}
					if ((lastCamPos.distance(local2) < local4.distance(local2)))
					{
						return;
					}
				}
			} else
			{
				addTimer(3, 3);
			}
		}
		changeCamNone();
		if ((!(local4)))
		{
			return;
		}
		Vector3 local5_2 = new Vector3(local2);
		local5_2.sub(local4);
		Ypr local6 = new Ypr(local5_2);
		cam = new GameRef(
			map,
			GameRef.RID_CAMERA,
			(((local4.toString() + ",") + local6.toString()) + ",0x02"),
			"track tv cam"
		);
		cam.command(((("autozoom 1 0 " + (0.2 + (Math.random() * 0.8))) + " ") + (Math.random() * 0.2)));
		cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
		renderer = cam;
		if (MB_Animator)
		{
			MB_Animator.changeCam(renderer);
		}
		cam.command("torque 0.1");
		cam.command(("roll " + (Math.random() - 0.5)));
		lastCamPos = new Vector3(local4);
		cameraMode = Track.CAMMODE_TV;
		ResourceRef local7 = new ResourceRef(cameraTarget.getInfo(GameType.GII_BONE));
		cam.command((("look " + local7.id()) + " 0,0,-1"));
	}

	public void changeCamInternal()
	{
		if ((!(cameraTarget)))
		{
			return;
		}
		if ((!(cameraTarget.id())))
		{
			return;
		}
		if ((cameraMode == Track.CAMMODE_FREE))
		{
			player.controller.command(("leave " + cam.id()));
			player.controller.command(("controllable " + player.car.id()));
			player.controller.activateState(5, 0);
		}
		int local1 = cameraTarget.getInfo(GameType.GII_RENDER);
		if ((local1 == 0))
		{
			return;
		}
		if ((cameraMode == Track.CAMMODE_INTERNAL))
		{
			if ((++cameraNum >= local1))
			{
				cameraNum = 0;
			}
			if (((local1 > 2) && (cameraNum >= (local1 - 2))))
			{
				cameraNum = 0;
			}
			GameRef local2 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
			cameraTarget.command(((((("render " + osd.getViewport().id()) + " ") + local2.id()) + " ") + cameraNum));
			renderer = cameraTarget;
			if (MB_Animator)
			{
				MB_Animator.changeCam(renderer);
			}
		} else
		{
			GameRef local2_2 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
			if (cam)
			{
				lastCamPos = cam.getPos();
				cam.command(("hide " + osd.getViewport().id()));
				if ((cam != cam_external))
				{
					cam.destroy();
				}
			}
			if ((cameraNum < 0))
			{
				cameraNum = ((-(cameraNum)) % local1);
			} else
			{
				cameraNum = 0;
			}
			cameraTarget.command(((((("render " + osd.getViewport().id()) + " ") + local2_2.id()) + " ") + cameraNum));
			renderer = cameraTarget;
			if (MB_Animator)
			{
				MB_Animator.changeCam(renderer);
			}
		}
		cameraMode = Track.CAMMODE_INTERNAL;
		if ((!(cameraNum)))
		{
			player.car.command("filter 3 2");
		} else
		{
			player.car.command("filter 3 0");
		}
	}

	public void changeCamInternal(int arg0)
	{
		if ((cameraMode == Track.CAMMODE_INTERNAL))
		{
			GameRef local2 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
			cameraTarget.command(((((("render " + osd.getViewport().id()) + " ") + local2.id()) + " ") + arg0));
			renderer = cameraTarget;
			if (MB_Animator)
			{
				MB_Animator.changeCam(renderer);
			}
		} else
		{
			cameraNum = (-(arg0));
			changeCamInternal();
		}
	}

	void makeCamExternal()
	{
		Vector3 local1;
		if ((cameraMode == Track.CAMMODE_FREE))
		{
			player.controller.command(("leave " + cam.id()));
			player.controller.command(("controllable " + player.car.id()));
			player.controller.activateState(5, 0);
		}
		if (((cameraMode == Track.CAMMODE_INTERNAL) && (!(cameraNum))))
		{
			player.car.command("filter 3 0");
		}
		if (cam_external)
		{
			if ((!(cam_external.id())))
			{
				cam_external = null;
			}
		}
		if (cam_external)
		{
			if (renderer)
			{
				if ((renderer != cam_external))
				{
					renderer.command(("hide " + osd.getViewport().id()));
					renderer = null;
				}
			}
			if ((cam && (cam != cam_external)))
			{
				cam.destroy();
				cam = null;
			}
			cam = cam_external;
		}
		if ((!(cam_external)))
		{
			changeCamNone();
			if (lastCamPos)
			{
				local1 = lastCamPos;
			} else
			{
				local1 = new Vector3(0, (-(10000)), 0);
			}
			Vector3 local2 = cameraTarget.getPos();
			if ((!(local2)))
			{
				return;
			}
			Ypr local3;
			local2.sub(local1);
			if ((local2.length() > 70.0))
			{
				local1 = cameraTarget.getPos();
				if ((!(local1)))
				{
					return;
				}
				Vector3 local4 = new Vector3(local2);
				local4.normalize();
				local4.mul(5.0);
				local2 = new Vector3(local1);
				local1.sub(local4);
				local1.y += 2.0;
				local2.sub(local1);
				local3 = new Ypr(local2);
			} else
			{
				local3 = new Ypr(local2);
			}
			cam = new GameRef(
				map,
				GameRef.RID_CAMERA,
				(((local1.toString() + ",") + local3.toString()) + ",0x02, 1.0,0, 0.01"),
				"external track cam"
			);
			cam_external = cam;
			lastCamPos = new Vector3(local1);
		}
	}

	public void changeCamFollow()
	{
		makeCamExternal();
		cam.command("dist 2.0 10.0");
		cam.command("smooth 0.5 0.5");
		cam.command("force 1.6 0.5 -0.7");
		cam.command("torque 0.1");
		cam.command("roll 0");
		cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
		renderer = cam;
		if (MB_Animator)
		{
			MB_Animator.changeCam(renderer);
		}
		GameRef local1 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
		local1.command(("viewport " + osd.getViewport().id()));
		local1.command(("camera " + cam.id()));
		cameraMode = Track.CAMMODE_FOLLOW;
	}

	public void changeCamPoint(Vector3 arg0)
	{
		makeCamExternal();
		GameRef local2 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
		local2.command("viewport 0");
		ResourceRef local3 = new ResourceRef(cameraTarget.getInfo(GameType.GII_BONE));
		Vector3 local4 = cameraTarget.getPos();
		if ((!(local4)))
		{
			return;
		}
		local4.sub(arg0);
		Ypr local5 = new Ypr(local4);
		cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
		renderer = cam;
		if (MB_Animator)
		{
			MB_Animator.changeCam(renderer);
		}
		cam.command((("move " + local3.id()) + " 0,1,0 0.1 0,-0.2,-3"));
		cam.command(((((((("look " + map.id()) + " ") + arg0.x) + ",") + arg0.y) + ",") + arg0.z));
		cam.command(("roll " + (Math.random() - 0.5)));
		cameraMode = Track.CAMMODE_POINT;
	}

	public void changeCamChase()
	{
		if ((!(cameraTarget2)))
		{
			changeCamFollow();
		} else
		{
			if ((!(cameraTarget2.id())))
			{
				changeCamFollow();
			} else
			{
				makeCamExternal();
				GameRef local1 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
				local1.command("viewport 0");
				ResourceRef local2 = new ResourceRef(cameraTarget.getInfo(GameType.GII_BONE));
				cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
				renderer = cam;
				if (MB_Animator)
				{
					MB_Animator.changeCam(renderer);
				}
				float local3 = (Math.random() - 0.5);
				cam.command(((((((("move " + local2.id()) + " 0,") + Math.random()) + ",0 0.01 ") + (local3 * (-(3.0)))) + ",-0.2,") + ((-(3)) - (5 * Math.random()))));
				cam.command((((("look " + cameraTarget2.id()) + " 0,0,-1 ") + (local3 * 0.6)) + ",0,-1"));
				cam.command(("roll " + local3));
				cam.command((("zoom " + (20.0 + (70.0 * Math.random()))) + " 5"));
			}
		}
		cameraMode = Track.CAMMODE_CHASE;
	}

	public void changeCamInvChase()
	{
		if ((!(cameraTarget2)))
		{
			changeCamFollow();
		} else
		{
			if ((!(cameraTarget2.id())))
			{
				changeCamFollow();
			} else
			{
				makeCamExternal();
				GameRef local1 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
				local1.command("viewport 0");
				ResourceRef local2 = new ResourceRef(cameraTarget2.getInfo(GameType.GII_BONE));
				cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
				renderer = cam;
				if (MB_Animator)
				{
					MB_Animator.changeCam(renderer);
				}
				float local3 = (Math.random() - 0.5);
				cam.command(((((((("move " + local2.id()) + " 0,") + Math.random()) + ",0 0.01 ") + (local3 * (-(3.0)))) + ",-0.2,") + ((-(3)) - (5 * Math.random()))));
				cam.command((((("look " + cameraTarget.id()) + " 0,0,-1 ") + (local3 * 0.6)) + ",0,-1"));
				cam.command(("roll " + local3));
				cam.command((("zoom " + (20.0 + (70.0 * Math.random()))) + " 5"));
			}
		}
		cameraMode = Track.CAMMODE_INVCH;
	}

	public void changeCamFree()
	{
		makeCamExternal();
		player.controller.command(("leave " + player.car.id()));
		player.controller.command(("controllable " + cam.id()));
		player.controller.activateState(5, 1);
		cameraMode = Track.CAMMODE_FREE;
	}

	public void enablePicInPic()
	{
		vport2 = new Viewport(12, 0.99, 0.01, 0.3, (4.0 / 3.0), (-(1.0)), 0.0);
		vport2.activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
		GameRef local1 = new GameRef(cameraTarget.getInfo(GameType.GII_OWNER));
		cameraTarget.command(((((("render " + vport2.id()) + " ") + local1.id()) + " ") + cameraNum));
	}

	public void changeCamNone()
	{
		if (renderer)
		{
			renderer.command(("hide " + osd.getViewport().id()));
			renderer = null;
		}
		if (((cameraMode == Track.CAMMODE_INTERNAL) && (!(cameraNum))))
		{
			player.car.command("filter 3 0");
		}
		if (cam)
		{
			cam.destroy();
			if ((cam_external == cam))
			{
				cam_external = null;
			}
			if ((renderer == cam))
			{
				renderer = null;
			}
			cam = null;
		}
	}

	public void refreshCamera()
	{
		if ((cameraMode == Track.CAMMODE_FOLLOW))
		{
			changeCamNone();
			changeCamFollow();
		} else
		{
			if ((cameraMode == Track.CAMMODE_INTERNAL))
			{
				changeCamInternal();
			} else
			{
				if ((cameraMode == Track.CAMMODE_TV))
				{
					changeCamTV();
				} else
				{
					if ((cameraMode == Track.CAMMODE_CHASE))
					{
						changeCamChase();
					} else
					{
						if ((cameraMode == Track.CAMMODE_INVCH))
						{
							changeCamInvChase();
						}
					}
				}
			}
		}
	}

	public void changeCamTarget(GameRef arg0)
	{
		if ((cameraTarget == arg0))
		{
			return;
		}
		if ((renderer && cameraTarget))
		{
			if ((renderer.id() == cameraTarget.id()))
			{
				renderer.command(("hide " + osd.getViewport().id()));
				renderer = null;
			}
		}
		cameraTarget = arg0;
		refreshCamera();
	}

	public void changeCamTarget2(GameRef arg0)
	{
		if ((cameraTarget2 == arg0))
		{
			return;
		}
		if ((renderer && cameraTarget2))
		{
			if ((renderer.id() == cameraTarget2.id()))
			{
				renderer.command(("hide " + osd.getViewport().id()));
				renderer = null;
			}
		}
		cameraTarget2 = arg0;
		if (((cameraMode == Track.CAMMODE_CHASE) || (cameraMode == Track.CAMMODE_INVCH)))
		{
			refreshCamera();
		}
	}

	public Trigger addTrigger(Trigger arg0, Vector3 arg1, RenderRef arg2, String arg3)
	{
		trigger.addElement(arg0);
		addNotification(arg0.trigger, GameRef.EVENT_TRIGGER_ON, GameRef.EVENT_SAME, null, arg3);
		addNotification(arg0.trigger, GameRef.EVENT_TRIGGER_OFF, GameRef.EVENT_SAME, null, arg3);
		if (arg2)
		{
			arg0.marker = nav.addMarker(arg2, arg1, 0);
			arg0.nav = nav;
		}
		return arg0;
	}

	public Trigger addTrigger(Vector3 arg0, GameRef arg1, RenderRef arg2, String arg3, float arg4, String arg5)
	{
		return addTrigger(new Trigger(map, null, arg0, arg4, arg5), arg0, arg2, arg3);
	}

	public void removeTrigger(Trigger arg0)
	{
		if (arg0)
		{
			remNotification(arg0.trigger, (GameRef.EVENT_TRIGGER_ON | GameRef.EVENT_TRIGGER_OFF));
			trigger.removeElement(arg0);
			arg0.finalize();
		}
	}

	public void lockCar()
	{
		if (player.car)
		{
			player.car.setParent(map);
			player.car.setMatrix(posStart, oriStart);
			player.car.command("reset");
			player.car.command("reload");
		}
	}

	public void releaseCar()
	{
		if (player.car)
		{
			player.car.setParent(player);
			player.car.command("reset");
		}
	}

	public void setMessage(String arg0)
	{
		messages.changeText(arg0);
		addTimer(2, 0);
		++msgtimers;
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		if ((arg1 == GameRef.EVENT_TIME))
		{
			if ((arg2 == 0))
			{
				if ((!(--msgtimers)))
				{
					messages.changeText("");
				}
			} else
			{
				if ((arg2 == 2))
				{
					addTimer(1, 2);
					if (GameLogic.timeout)
					{
						if ((!(--GameLogic.timeout)))
						{
							GameLogic.changeActiveSection(parentState);
						}
					}
				} else
				{
					if ((arg2 == 3))
					{
						if ((cameraMode == Track.CAMMODE_TV))
						{
							changeCamTV();
							addTimer(3, 3);
						}
					} else
					{
						if ((arg2 == 4))
						{
							if ((GameLogic.gameMode == GameLogic.GM_DEMO))
							{
								int local4 = 0;
								if ((Math.random() > 0.3))
								{
									int local5 = (2.0 * Math.random());
									cameraMode = (2 + (local5 % 3));
									local4++;
								} else
								{
									int local5_2 = (3.0 * Math.random());
									changeCamInternal((1 + (local5_2 % 3)));
								}
								if ((Math.random() > 0.7))
								{
									if (cameraTarget2)
									{
										GameRef local5_3 = cameraTarget2;
										if ((renderer && cameraTarget))
										{
											if ((renderer.id() == cameraTarget.id()))
											{
												renderer.command(("hide " + osd.getViewport().id()));
												renderer = null;
											}
										}
										cameraTarget2 = cameraTarget;
										cameraTarget = local5_3;
										local4++;
									}
								}
								if (local4)
								{
									refreshCamera();
									addTimer(5, 4);
								} else
								{
									addTimer(2, 4);
								}
							}
						}
					}
				}
			}
		}
	}

	public void handleEvent(GameRef arg0, int arg1, String arg2)
	{
		int local4 = (-(1));
		if ((arg1 == GameRef.EVENT_CURSOR))
		{
			int local5 = arg2.token(++local4).intValue();
			int local6 = arg2.token(++local4).intValue();
			if ((local5 == GameType.EC_RCLICK))
			{
				GameRef local7 = new GameRef(arg2.token(++local4).intValue());
				int local8 = local7.getInfo(GameType.GII_CATEGORY);
				if (((local8 == GameType.GIR_CAT_PART) || (local8 == GameType.GIR_CAT_VEHICLE)))
				{
					cam.command(((((((("look " + local7.id()) + " ") + arg2.token(++local4)) + ",") + arg2.token(++local4)) + ",") + arg2.token(++local4)));
				}
			}
		}
	}

	public void handleMessage(Message arg0)
	{
		if ((arg0.type == Message.MT_EVENT))
		{
			int local2 = arg0.cmd;
			if ((local2 == Track.CMD_LOOKBACK))
			{
				if ((cameraMode == Track.CAMMODE_FOLLOW))
				{
					if (((Event) arg0).flags)
					{
						Vector3 local3 = new Vector3();
						local3.diff(cameraTarget.getPos(), cam.getPos());
						camDist = local3.length();
						Vector3 local4 = new Vector3(0.0, 1.5, (-(camDist)));
						Vector3 local5 = cameraTarget.getPos();
						float local6 = cameraTarget.getOri().y;
						local4.rotate(new Ypr(local6, 0.0, 0.0));
						local4.sub(new Vector3((-(local5.x)), (-(local5.y)), (-(local5.z))));
						cam.command("force 2.0 1.0 -1.0");
						cam.command("simulate 0");
						ResourceRef local7 = new ResourceRef(cameraTarget.getInfo(GameType.GII_BONE));
						cam.command((((("move " + local7.id()) + " 0,1.5,-") + camDist) + " 0.001 0,-0.2, 0"));
						cam.command((("look " + local7.id()) + " 0,1.3,0"));
						local4.y += 1;
						cam.setMatrix(local4, new Ypr((local6 + 3.1399999), 0, 0));
					} else
					{
						changeCamFollow();
						Vector3 local3_2 = new Vector3(0.0, 2.0, (0.90000004 * camDist));
						Vector3 local4_2 = cameraTarget.getPos();
						float local5_2 = cameraTarget.getOri().y;
						local3_2.rotate(new Ypr(local5_2, 0.0, 0.0));
						local3_2.sub(new Vector3((-(local4_2.x)), (-(local4_2.y)), (-(local4_2.z))));
						cam.setMatrix(local3_2, new Ypr(local5_2, 0, 0));
					}
				} else
				{
					if ((cameraMode == Track.CAMMODE_INTERNAL))
					{
						if (((Event) arg0).flags)
						{
							IntCamNumber = cameraNum;
							changeCamInternal(5);
						} else
						{
							changeCamInternal(IntCamNumber);
						}
					}
				}
			} else
			{
				if ((local2 == Track.CMD_MIRROR))
				{
					MirrorStatus += 1;
					if ((MirrorStatus >= 2))
					{
						MirrorStatus = 0;
					}
					Mirror(MirrorStatus);
				} else
				{
					if ((local2 == Track.CMD_MAP))
					{
						if (nav)
						{
							if ((MapStatus == 1))
							{
								osd.vp.resize(0, 0, 1, 1);
								nav.hide();
								enableOsd(OsdStatus);
								MapViewFrame.finalize();
								Track.GPS_Min_Zoom = MapZ1;
								Track.GPS_Max_Zoom = MapZ2;
								MapStatus = 0;
							} else
							{
								OsdStatus = osdEnabled;
								enableOsd(0);
								MapViewFrame = MirrorOsd.createRectangle(
									0.63,
									0.63,
									0.71999997,
									0.71999997,
									(-(1)),
									new ResourceRef(MW_Mod:0x54r)
								);
								osd.vp.resize(0.64000005, 0.64000005, 0.35000002, 0.35000002);
								nav.ViewMap();
								MapZ1 = Track.GPS_Min_Zoom;
								MapZ2 = Track.GPS_Max_Zoom;
								Track.GPS_Min_Zoom += 3;
								Track.GPS_Max_Zoom += 4.5;
								MapStatus = 1;
							}
						}
					} else
					{
						if (((local2 >= Track.CMD_SUSP_FL) && (local2 <= Track.CMD_SUSP_MODE)))
						{
							if (player.car)
							{
								if (player.car.chassis)
								{
									if (((Event) arg0).flags)
									{
										player.car.chassis.Hsuspension(1, (local2 - 40));
									} else
									{
										player.car.chassis.Hsuspension(0, (local2 - 40));
									}
								}
							}
						} else
						{
							if ((local2 == Track.CMD_CRUISECONTROL))
							{
								int local3_3 = (1 - player.car.getCruiseControl());
								player.car.setCruiseControl(local3_3);
								if (local3_3)
								{
									setMessage("Cruise control on");
								} else
								{
									setMessage("Cruise control off");
								}
							} else
							{
								if ((local2 == Track.CMD_CAMMOVE))
								{
									if (((Event) arg0).flags)
									{
										player.controller.user_Add(
											Input.AXIS_LOOK_UPDOWN,
											ControlSet.MOUSE,
											1,
											(-(1.0)),
											1.0,
											(-(1.0)),
											1.0
										);
										player.controller.user_Add(
											Input.AXIS_LOOK_LEFTRIGHT,
											ControlSet.MOUSE,
											0,
											(-(1.0)),
											1.0,
											(-(1.0)),
											1.0
										);
									} else
									{
										player.controller.user_Del(Input.AXIS_LOOK_UPDOWN, ControlSet.MOUSE, 1);
										player.controller.user_Del(
											Input.AXIS_LOOK_LEFTRIGHT,
											ControlSet.MOUSE,
											0
										);
									}
								} else
								{
									if ((local2 == Track.CMD_CHANGECAM_TV))
									{
										changeCamTV();
									} else
									{
										if ((local2 == Track.CMD_CHANGECAM_INT))
										{
											changeCamInternal();
										} else
										{
											if ((local2 == Track.CMD_CHANGECAM_EXT))
											{
												if ((cameraMode != Track.CAMMODE_FOLLOW))
												{
													changeCamFollow();
												}
											} else
											{
												if ((local2 == Track.CMD_CHANGECAM_CHASE))
												{
													changeCamChase();
												} else
												{
													if ((local2 == Track.CMD_CHANGECAM_INVCH))
													{
														changeCamInvChase();
													} else
													{
														if ((local2 == Track.CMD_CHANGECAM_FREE))
														{
															if ((cameraMode != Track.CAMMODE_FOLLOW))
															{
																changeCamFollow();
															}
															changeCamFree();
														} else
														{
															if ((local2 == Track.CMD_CHANGECAMTARGET))
															{
																if (player.car)
																{
																	int local3_4 = System.timeWarp((-(1)));
																	System.timeWarp(0.0);
																	Vector3 local4_3 = new Vector3(player.car.getPos());
																	Ypr local5_3 = new Ypr(player.car.getOri());
																	player.render.destroy();
																	if (nav)
																	{
																		nav.remMarker(mPlayer);
																	}
																	if ((GetCarStatus == 0))
																	{
																		releaseCar();
																		player.carlot.lockPlayerCar();
																		player.carlot.saveCar(player.carlot.curcar);
																		player.carlot.flushCars();
																		GetCarStatus = 1;
																	} else
																	{
																		player.car.destroy();
																		player.car = null;
																	}
																	VehicleDescriptor local6_2 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
																	player.car = new Vehicle(
																		player,
																		local6_2.id,
																		local6_2.colorIndex,
																		(1 + Math.random()),
																		(1 + Math.random()),
																		1,
																		1
																	);
																	player.car.setParent(map);
																	player.controller.command(("controllable " + player.car.id()));
																	player.car.setMatrix(
																		local4_3,
																		new Ypr(local5_3.y, 0.0, 0.0)
																	);
																	player.render = new RenderRef(
																		map,
																		player.driverID,
																		"player"
																	);
																	player.controller.command(("renderinstance " + player.render.id()));
																	player.car.setDefaultTransmission();
																	player.car.setDefaultSteeringHelp();
																	changeCamTarget(player.car);
																	changeCamFollow();
																	map.command(("obs_add " + player.car.id()));
																	player.car.command("reload");
																	System.timeWarp(local3_4);
																	player.car.wakeUp();
																	if (nav)
																	{
																		mPlayer = nav.addMarker(player);
																	}
																}
															} else
															{
																if ((local2 == Track.CMD_OSDONOFF))
																{
																	osdEnabled = (1 - osdEnabled);
																	enableOsd(osdEnabled);
																	if ((MirrorStatus == 1))
																	{
																		Mirror(osdEnabled);
																	}
																} else
																{
																	if ((local2 == Track.CMD_CAMROTATE))
																	{
																		if (camRotate)
																		{
																			cam.command("angle 0 0");
																		} else
																		{
																			cam.command("angle 0 8.0 0.7853");
																		}
																		camRotate = (1 - camRotate);
																	} else
																	{
																		if ((local2 == Track.CMD_QUICKREPAIR))
																		{
																			Ypr local3_5 = new Ypr(player.car.getOri());
																			if (((((local3_5.r > 1.3) || (local3_5.r < (-(1.3)))) || (local3_5.p > 1.3)) || (local3_5.p < (-(1.3)))))
																			{
																				Vector3 local4_4 = new Vector3(player.car.getPos());
																				player.car.setMatrix(
																					local4_4,
																					new Ypr(local3_5.y, 0.0, 0.0)
																				);
																				player.car.command("idle");
																				player.car.wakeUp();
																			}
																			player.car.repair();
																			player.car.command("reload");
																		} else
																		{
																			if ((local2 == Track.CMD_SIMPAUSE))
																			{
																				TimeTextStatus = 1;
																				TimeTextWait = (System.currentTime() + 2);
																				if ((System.timeWarp((-(1.0))) > 0.0))
																				{
																					System.timeWarp(0.0);
																					cam.command("simulate 1");
																					TimeText.changeText("Time: Pause");
																				} else
																				{
																					cam.command("simulate 0");
																					TimeText.changeText("Time: Normal");
																					System.timeWarp(1.0);
																					if (camRotate)
																					{
																						cam.command("angle 0 10.0 0.7853");
																					}
																				}
																			} else
																			{
																				if ((local2 == Track.CMD_SIMSPEEDINC))
																				{
																					TimeTextStatus = 1;
																					TimeTextWait = (System.currentTime() + 2);
																					float local3_6 = (2.0 * System.timeWarp((-(1.0))));
																					if ((local3_6 < 8.0))
																					{
																						if ((local3_6 == 0.0))
																						{
																							local3_6 = (1.0 / 512);
																						}
																						System.timeWarp(local3_6);
																					}
																					if ((local3_6 == 1.0))
																					{
																						cam.command("simulate 0");
																						TimeText.changeText("Time: Normal");
																					} else
																					{
																						if ((local3_6 > 1.0))
																						{
																							cam.command("simulate 0");
																							TimeText.changeText(("Time: x" + System.timeWarp((-(1.0)))));
																						} else
																						{
																							cam.command("simulate 1");
																							TimeText.changeText(("Time: x" + System.timeWarp((-(1.0)))));
																						}
																					}
																					if (camRotate)
																					{
																						cam.command("angle 0 10.0 0.7853");
																					}
																				} else
																				{
																					if ((local2 == Track.CMD_SIMSPEEDDEC))
																					{
																						TimeTextStatus = 1;
																						TimeTextWait = (System.currentTime() + 2);
																						float local3_7 = (0.5 * System.timeWarp((-(1.0))));
																						if ((local3_7 < (1.0 / 512)))
																						{
																							local3_7 = 0.0;
																						}
																						System.timeWarp(local3_7);
																						if ((local3_7 == 0))
																						{
																							TimeText.changeText("Time: Pause");
																						} else
																						{
																							if ((local3_7 == 1.0))
																							{
																								cam.command("simulate 0");
																								TimeText.changeText("Time: Normal");
																							} else
																							{
																								if ((local3_7 > 1.0))
																								{
																									cam.command("simulate 0");
																									TimeText.changeText(("Time: x" + System.timeWarp((-(1.0)))));
																								} else
																								{
																									cam.command("simulate 1");
																									TimeText.changeText(("Time: x" + System.timeWarp((-(1.0)))));
																								}
																							}
																						}
																						if (camRotate)
																						{
																							cam.command("angle 0 10.0 0.7853");
																						}
																					} else
																					{
																						if ((local2 == Track.CMD_INGAMEMENU))
																						{
																							if (mc_active)
																							{
																								osdCommand(Track.CMD_MECHANIC);
																							} else
																							{
																								if (pt_active)
																								{
																									osdCommand(Track.CMD_PAINT);
																								} else
																								{
																									if (im_active)
																									{
																										ingameMenu.hide();
																										player.car.setDefaultTransmission();
																										player.car.setDefaultSteeringHelp();
																										player.car.setDefaultASR();
																										player.car.setDefaultABS();
																										if (nav)
																										{
																											nav.changeMode(Config.gpsMode);
																										}
																										if (ingameMenu.reqTrackExit)
																										{
																											int local3_8;
																											if ((this instanceof ROCTrack))
																											{
																												if ((!(this.testMode)))
																												{
																													local3_8 = 1;
																												}
																											}
																											if (local3_8)
																											{
																												this.giveUpRace();
																											} else
																											{
																												GameLogic.changeActiveSection(parentState);
																											}
																										}
																									} else
																									{
																										ingameMenu.show();
																										if ((cam && cam.id()))
																										{
																											ingameMenu.setActiveCamera(cam);
																										}
																									}
																									im_active = (1 - im_active);
																								}
																							}
																						} else
																						{
																							if ((local2 == Track.CMD_EXIT))
																							{
																								if (mc_active)
																								{
																									osdCommand(Track.CMD_MECHANIC);
																								}
																								if (pt_active)
																								{
																									osdCommand(Track.CMD_PAINT);
																								}
																								if (im_active)
																								{
																									osdCommand(Track.CMD_INGAMEMENU);
																								}
																								GameLogic.changeActiveSection(parentState);
																							} else
																							{
																								if ((local2 == Track.CMD_REPAIR))
																								{
																									if ((RepairStatus == 0))
																									{
																										System.timeWarp(0.0);
																										enableOsd(0);
																										mechanic.filterInventory(
																											0,
																											0,
																											0
																										);
																										mechanic.show();
																										cam.command((("look " + player.car.id()) + " 0,0,0 0,0,0 0.2"));
																										cam.command((("move " + player.car.id()) + " 0,0,0 3.5"));
																										cam.command("dist 1.0 10.0");
																										cam.command("smooth 0.05 0.5");
																										cam.command("simulate 1");
																										Input.cursor.addHandler(this);
																										RepairStatus = 1;
																									} else
																									{
																										if ((RepairStatus == 1))
																										{
																											mechanic.hide();
																											painter.show();
																											RepairStatus = 2;
																										} else
																										{
																											if ((RepairStatus == 2))
																											{
																												RepairStatus = 0;
																												mechanic.hide();
																												painter.hide();
																												player.controller.command(("camera" + cam.id()));
																												System.timeWarp(1.0);
																												painter.hide();
																												enableOsd(1);
																											}
																										}
																									}
																								} else
																								{
																									if ((local2 == Track.CMD_MECHANIC))
																									{
																										if (mc_active)
																										{
																											cam.command("dist 2.5 10.0");
																											cam.command("smooth 0.5 0.5");
																											cam.command("force 1.6 0.4 -0.4");
																											cam.command("torque 0.05");
																											player.controller.command(("camera" + cam.id()));
																											Input.cursor.remHandler(this);
																											player.car.command("start");
																											mechanic.hide();
																											mechanic.filterInventory(
																												1,
																												1,
																												1
																											);
																											enableOsd(1);
																										} else
																										{
																											enableOsd(0);
																											mechanic.filterInventory(
																												0,
																												0,
																												0
																											);
																											mechanic.show();
																											cam.command((("look " + player.car.id()) + " 0,0,0 0,0,0 0.2"));
																											cam.command((("move " + player.car.id()) + " 0,0,0 3.5"));
																											cam.command("dist 2.5 4.6");
																											cam.command("smooth 0.05 0.5");
																											player.car.command("stop");
																											Input.cursor.addHandler(this);
																										}
																										mc_active = (1 - mc_active);
																									} else
																									{
																										if ((local2 == Track.CMD_PAINT))
																										{
																											if (pt_active)
																											{
																												cam.command("dist 2.5 10.0");
																												cam.command("smooth 0.5 0.5");
																												cam.command("force 1.6 0.4 -0.4");
																												cam.command("torque 0.01");
																												player.controller.command(("camera" + cam.id()));
																												player.car.command("start");
																												painter.hide();
																												enableOsd(1);
																											} else
																											{
																												enableOsd(0);
																												painter.show();
																												cam.command((("look " + player.car.id()) + " 0,0,0 0,0,0 0.2"));
																												cam.command((("move " + player.car.id()) + " 0,0,0 3.5"));
																												cam.command("dist 2.5 4.6");
																												cam.command("smooth 0.05 0.5");
																												player.car.command("stop");
																											}
																											pt_active = (1 - pt_active);
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
							}
						}
					}
				}
			}
		}
	}

	public void osdCommand(int arg0)
	{
		mp.putMessage(new Event(arg0));
	}

	public void enableOsd(int arg0)
	{
		if (arg0)
		{
			osdCounter++;
			if ((osdCounter == 1))
			{
				player.showOsd();
				if (nav)
				{
					nav.show();
				}
			}
		} else
		{
			osdCounter--;
			if ((osdCounter == 0))
			{
				player.hideOsd();
				if (nav)
				{
					nav.hide();
				}
			}
		}
	}

}
