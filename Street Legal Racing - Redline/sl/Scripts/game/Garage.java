package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class Garage extends Scene implements GameState
{
	static final int RID_GARAGE1 = misc.garage:0x7ar;
	static final int RID_GARAGE1_STUFF = misc.garage:0x42r;
	static final int RID_GARAGE2 = misc.garage:0x6r;
	static final int RID_GARAGE2_STUFF = misc.garage:0x18r;
	static final int RID_GARAGE3 = misc.garage:0xc7r;
	static final int RID_GARAGE3_STUFF = misc.garage:0x3cr;
	static final int RID_GARAGE4 = misc.garage:0x2fr;
	static final int RID_GARAGE4_STUFF = misc.garage:0x51r;
	static final int BUTTON_DEFAULT_STATE_COLOR = -4144960;
	static final int BUTTON_ACTIVE_STATE_COLOR = -1;
	static final int MODE_NONE = 0;
	static final int MODE_SZEREL = 1;
	static final int MODE_PAINT = 2;
	static final int MODE_TEST = 3;
	static final int MODE_TUNE = 4;
	static final int CMD_NONE = 100;
	static final int CMD_MAINMENU = 101;
	static final int CMD_MENU = 107;
	static final int CMD_ROC = 108;
	static final int CMD_HITTHESTREET = 109;
	static final int CMD_TESTTRACK = 110;
	static final int CMD_CARLOT = 111;
	static final int CMD_BUYCARS = 112;
	static final int CMD_CATALOG = 113;
	static final int CMD_CLUBINFO = 114;
	static final int CMD_CARINFO = 115;
	static final int CMD_TIME = 116;
	static final int CMD_MECHANIC = 117;
	static final int CMD_PAINT = 118;
	static final int CMD_ESCAPE = 119;
	static final int CMD_ROCRACE = 120;
	static final int CMD_ROCTEST = 121;
	static final int CMD_BUYCARSUSED = 122;
	static final int CMD_TEST = 123;
	static final int CMD_TUNE = 124;
	static final int CMD_ROCINFO = 125;
	static final int CMD_ROCQUIT = 126;
	static final int CMD_CHEATMONEY = 127;
	static final int CMD_BEGIN_ROC = 128;
	static final int CMD_RELASE_CAR = 129;
	static final int CMD_LOCK_CAR = 130;
	static final int CMD_TIME_6H = 131;
	static final int CMD_ENGINE = 132;
	static final int CMD_BODY = 133;
	static final int CMD_RUNNING_GEAR = 134;
	static final int CMD_TRACKSELECT = 135;
	static final int CMD_DRIVERELECT = 136;
	static final int CMD_JACK = 137;
	static final int CMD_SUSP_FL = 138;
	static final int CMD_SUSP_F = 139;
	static final int CMD_SUSP_FR = 140;
	static final int CMD_SUSP_LS = 141;
	static final int CMD_SUSP_ALL = 142;
	static final int CMD_SUSP_RS = 143;
	static final int CMD_SUSP_RL = 144;
	static final int CMD_SUSP_R = 145;
	static final int CMD_SUSP_RR = 146;
	static final int CMD_SUSP_MODE = 147;
	static final Vector3 StartPos = new Vector3(0, 0, 0);
	static final Ypr StartYpr = new Ypr(0, 0, 0);
	static final Vector3 defLookPos = new Vector3(0, 0.0, 0.0);
	static final SfxRef Jack_Move_SFX = new SfxRef(MW_Mod:0x95r);
	static final SfxRef Jack_Stop_SFX = new SfxRef(MW_Mod:0x96r);
	int garageIndex;
	int[] garageID;
	int[] garageStuffID;
	ResourceRef[] garageEnvMaps;
	RenderRef light;
	RenderRef lighttype;
	RenderRef tmp;
	Player player;
	ControlSetState css;
	Roc roc;
	Osd osd;
	GameRef camera;
	Vector3 cameraPos = new Vector3((-(3.0)), 1.5, (-(2.0)));
	Ypr cameraOri = new Ypr((-(2.05)), (-(0.25)), 0);
	GameState parentState;
	Mechanic mechanic;
	int filterEngine;
	int filterBody;
	int filterRGear;
	Painter painter;
	int lastPaintCanId;
	int lastPainterLine;
	int showDecals;
	int lastPaintMode = Painter.MODE_PAINTPART;
	int mode;
	int drag;
	int move;
	int mode_memory = Garage.MODE_SZEREL;
	int prevMode;
	int menuGroup;
	int menuGroup2;
	int menuVisible;
	RenderRef stuff;
	float CarMass;
	float CarY;
	float DiffY;
	float JackWait;
	int JackDamage;
	int JackStatus;
	int SFX_Loop_ID;
	int JackHit;
	int SavePos;
	int ClutchStatus = 1;
	Button JackButton;
	Button ClutchButton;
	Vector3 JackPos = new Vector3(0.0, (-(0.2)), 0.0);
	Vector3 JackVehPos = new Vector3(0.0, 0.0, 0.0);
	Vector3 CarVector = new Vector3(0.0, 0.0, 0.0);
	Ypr CarYpr = new Ypr(0.0, 0.0, 0.0);
	RenderRef JackArm;
	PhysicsRef JackPhys = new PhysicsRef();
	Thread JackThread;
	Text DebugText;
	Vehicle JackVeh;
	Vehicle Car2;
	Text moneytxt;
	Text daytxt;
	Text timetxt;
	Text invLineTxt;
	Text prestigeTxt;
	Text infoline;

	public static final float Check(float arg0)
	{
		return (Math.sqrt(arg0) + 3.75);
	}

	public Garage()
	{
		super();
		createNativeInstance();
		InventoryItem local1 = new InventoryItem();
		garageID = new int[(GameLogic.CLUBS + 1)];
		garageStuffID = new int[(GameLogic.CLUBS + 1)];
		garageID[0] = Garage.RID_GARAGE1;
		garageStuffID[0] = Garage.RID_GARAGE1_STUFF;
		garageID[1] = Garage.RID_GARAGE2;
		garageStuffID[1] = Garage.RID_GARAGE2_STUFF;
		garageID[2] = Garage.RID_GARAGE3;
		garageStuffID[2] = Garage.RID_GARAGE3_STUFF;
		garageID[3] = Garage.RID_GARAGE4;
		garageStuffID[3] = Garage.RID_GARAGE4_STUFF;
		garageEnvMaps = new ResourceRef[(GameLogic.CLUBS + 1)];
		garageEnvMaps[0] = new ResourceRef(maps.skydome:0x36r);
		garageEnvMaps[1] = new ResourceRef(maps.skydome:0x37r);
		garageEnvMaps[2] = new ResourceRef(maps.skydome:0x38r);
		garageEnvMaps[3] = new ResourceRef(maps.skydome:0x39r);
		internalScene = 1;
	}

	public void enter(GameState arg0)
	{
		float local2 = (Math.random() * 100);
		if ((MainMenu.Check(local2) != (Math.sqrt(local2) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		Frontend.loadingScreen.show();
		if ((!(parentState)))
		{
			parentState = arg0;
		}
		player = GameLogic.player;
		if (roc)
		{
			garageIndex = GameLogic.CLUBS;
		} else
		{
			garageIndex = player.club;
		}
		map = new GroundRef(garageID[garageIndex]);
		stuff = new RenderRef(map, garageStuffID[garageIndex], null);
		osd = new Osd();
		osd.globalHandler = this;
		osd.defSelection = 5;
		osd.orientation = 1;
		createOSDObjects();
		JackArm = new RenderRef(map, MW_Mod:0x37r, null);
		JackPhys.createBox(map, 1.02, 0.01, 1.59, "Phys");
		if ((((JackStatus == 0) || (JackStatus == 4)) || (JackStatus == 5)))
		{
			JackPhys.setMatrix(new Vector3(JackPos), null);
			JackArm.setMatrix(new Vector3(JackPos), null);
		} else
		{
			JackPhys.setMatrix(new Vector3(0.0, (-(3)), 0.0), null);
		}
		if (player.car)
		{
			player.car.setDamageMultiplier(0.0);
			player.car.setCruiseControl(0);
			if ((((StartPos.x == 0) && (StartPos.y == 0)) && (StartPos.z == 0)))
			{
				if (player.car.chassis)
				{
					if (((player.car.chassis.wheels < 4) || (player.car.chassis.getMax().x < 0.5)))
					{
						Garage.StartPos = new Vector3(7.0, 1.21, 6.0);
						Garage.StartYpr = new Ypr((-(1.2)), 0, 0);
					}
				}
			}
		}
		lockCar();
		addSceneElements(GameLogic.getTime());
		GfxEngine.setGlobalEnvmap(garageEnvMaps[garageIndex]);
		camera = new GameRef(
			map,
			GameRef.RID_CAMERA,
			(((((cameraPos.x + ", 1.7, ") + cameraPos.z) + ",") + cameraOri.toString()) + ", 0x13, 1.0,1.0, 0.05"),
			"Internal camera for garage"
		);
		cameraSetup(camera);
		osd.endGroup();
		painter = new Painter(player, osd, moneytxt, infoline, 0);
		painter.lastPaintMode = lastPaintMode;
		painter.showDecals = showDecals;
		painter.paintCans.lastCanId = lastPaintCanId;
		painter.paintCans.scrollToLine(lastPainterLine);
		mechanic = new Mechanic(player, osd, moneytxt, infoline, 0);
		mechanic.camera = camera;
		mechanic.map = map;
		if (roc)
		{
			painter.flags |= Painter.PF_REDUCED_FUNCTIONALITY;
			mechanic.flags |= Mechanic.MF_FREE_REPAIRS;
		} else
		{
			painter.flags &= (~(Painter.PF_REDUCED_FUNCTIONALITY));
			mechanic.flags &= (~(Mechanic.MF_FREE_REPAIRS));
		}
		lighttype = new RenderRef();
		lighttype.duplicate(new RenderRef(misc.garage:0x1dr));
		light = new RenderRef(map, lighttype, "neon");
		osd.show();
		GameLogic.setTimerText(daytxt, timetxt);
		mechanic.filterInventory(filterEngine, filterBody, filterRGear);
		mechanic.filterEngine = filterEngine;
		mechanic.filterBody = filterBody;
		mechanic.filterRGear = filterRGear;
		if (player.car)
		{
			player.car.command(("filter 1 " + filterEngine));
			player.car.command(("filter 2 " + filterBody));
			player.car.command(("filter 3 " + filterRGear));
			player.car.wakeUp();
			player.car.command("suspend");
		}
		Frontend.loadingScreen.display();
		setEventMask((((GameRef.EVENT_COLLISION | GameRef.EVENT_CURSOR) | GameRef.EVENT_COMMAND) | GameRef.EVENT_TIME));
		addNotification(player.car, GameRef.EVENT_COLLISION, GameRef.EVENT_SAME, null);
		addTimer(1, 2);
		Input.getAxis(1, (-(1)));
		Input.cursor.enable(1);
		Input.cursor.addHandler(this);
		Input.cursor.enableCameraControl(camera);
		changeMode(mode_memory);
		if ((!((((((arg0 instanceof ClubInfo) || (arg0 instanceof CarInfo)) || (arg0 instanceof Catalog)) || (arg0 instanceof Garage)) || (arg0 instanceof RocInfo)))))
		{
			Sound.changeMusicSet(Sound.MUSIC_SET_GARAGE);
			new SfxRef(GameLogic.SFX_ENTERGARAGE).play();
		}
		if (roc)
		{
			if (roc.init)
			{
				giveWarning(
					"ROC",
					"Welcome to the Race of Champions! \n \n The world's top 16 street racers are ready to fight for the prize and pride this rare event brings for one of them. \n There's 4 rounds, each has 3 runs - only the thoughest drivers and cars can make it through. Go for it!"
				);
				roc.init = 0;
			}
		} else
		{
			if ((!(player.car)))
			{
				if (player.checkHint(Player.H_GARAGE))
				{
					giveWarning(
						"WELCOME!",
						"Welcome to Street Legal Racing! \n \n Your Garage is empty. You can buy cars at the Car Dealers', now visit the Used Car Dealer and choose an affordable car to start with!"
					);
				}
			}
		}
		GameLogic.played = 1;
	}

	public void exit(GameState arg0)
	{
		Jack_Move_SFX.stop(SFX_Loop_ID);
		clearEventMask(GameRef.EVENT_ANY);
		if ((!((((((arg0 instanceof ClubInfo) || (arg0 instanceof CarInfo)) || (arg0 instanceof Catalog)) || (arg0 instanceof Garage)) || (arg0 instanceof RocInfo)))))
		{
			new SfxRef(GameLogic.SFX_LEAVEGARAGE).play();
		}
		if (JackThread)
		{
			JackThread.stop();
			JackThread = null;
		}
		JackArm.destroy();
		JackPhys.destroy();
		JackHit = 0;
		if (JackVeh)
		{
			JackVeh.destroy();
			JackVeh = null;
		}
		mode_memory = mode;
		changeMode(Garage.MODE_NONE);
		Input.cursor.enable(0);
		Input.cursor.remHandler(this);
		Input.cursor.disableCameraControl();
		cameraPos = camera.getPos();
		cameraOri = camera.getOri();
		camera.destroy();
		osd.hide();
		osd = null;
		if (tmp)
		{
			tmp.destroy();
			tmp = null;
		}
		mechanic.flushInventory();
		filterEngine = mechanic.filterEngine;
		filterBody = mechanic.filterBody;
		filterRGear = mechanic.filterRGear;
		mechanic = null;
		if (player.car)
		{
			player.car.command("filter 1 0");
			player.car.command("filter 2 0");
			player.car.command("filter 3 0");
			player.car.wakeUp();
			player.car.command("suspend");
			if ((SavePos == 1))
			{
				Garage.StartPos = player.car.getPos();
				Garage.StartYpr = player.car.getOri();
				SavePos = 0;
			} else
			{
				Garage.StartPos = new Vector3(0, 0, 0);
				Garage.StartYpr = new Ypr(0, 0, 0);
				JackStatus = 0;
			}
		} else
		{
			SavePos = 0;
			JackStatus = 0;
		}
		lastPaintCanId = painter.paintCans.lastCanId;
		lastPainterLine = painter.paintCans.currentLine();
		showDecals = painter.showDecals;
		lastPaintMode = painter.lastPaintMode;
		painter = null;
		if (player.car)
		{
			player.car.setDamageMultiplier(Config.player_damage_multiplier);
		}
		releaseCar();
		light.destroy();
		lighttype.destroy();
		remSceneElements();
		stuff.destroy();
		map.unload();
		player.controller.reset();
		player.controller.activateState(ControlSet.MENUSET);
	}

	public void run()
	{
		while (true)
		{
			if ((JackStatus == 1))
			{
				if ((JackHit == 0))
				{
					JackVehPos = JackVeh.getPos();
					JackArm.setMatrix(new Vector3(JackPos.x, JackVehPos.y, JackPos.z), null);
					player.car.wakeUp();
					player.car.command("suspend");
				}
				if ((JackHit == 1))
				{
					Jack_Move_SFX.stop(SFX_Loop_ID);
					Jack_Stop_SFX.play(JackPos, 0.0, 1.0, 1.0, 0);
					System.timeWarp(1.0);
					CarVector = player.car.getPos();
					DiffY = (JackVehPos.y - CarVector.y);
					Thread.sleep(1500);
					if (player.car.chassis)
					{
						CarMass = player.car.chassis.getMass();
					} else
					{
						CarMass = 1500;
					}
					CarVector = new Vector3(player.car.getPos());
					CarVector.z -= 0.2;
					CarYpr = new Ypr(player.car.getOri());
					if ((CarMass < 2800))
					{
						new SfxRef(MW_Mod:0x42r).play(JackPos, 0.0, 1.0, 1.0, 0);
						Vector3 local1 = new Vector3(CarVector);
						local1.y += ((CarMass * 0.0016999999) + 0.90000004);
						player.car.setMatrix(new Vector3(JackPos.x, local1.y, JackPos.z), new Ypr(CarYpr));
						player.car.command("stop");
						player.car.setMatrix(new Vector3(CarVector), new Ypr(CarYpr));
						player.car.wakeUp();
						player.car.command("setsteer -0.7");
						JackWait = (System.simTime() + 4);
					} else
					{
						if ((CarMass < 3000))
						{
							new WarningDialog(
								player.controller,
								((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_LOWPRI),
								"SURCHARGE",
								"Your car's mass is higer than jack's desinged capacity. \n Be careful...").display(
							);
							Thread.sleep(800);
							JackWait = (System.simTime() + 4);
							new SfxRef(MW_Mod:0x42r).play(JackPos, 0.0, 1.0, 1.0, 0);
							Vector3 local1_2 = new Vector3(CarVector);
							local1_2.y += ((CarMass * 0.0016999999) + 0.90000004);
							new SfxRef(MW_Mod:0x44r).play(JackPos, 0.0, 1.0, 1.5, 0);
							player.car.setMatrix(new Vector3(local1_2), new Ypr(CarYpr));
							player.car.command("stop");
							player.car.setMatrix(new Vector3(CarVector), new Ypr(CarYpr));
							player.car.wakeUp();
							player.car.command("setsteer -0.7");
						} else
						{
							JackWait = (System.simTime() + 1.1);
							JackDamage = 1;
							Vector3 local1_3 = new Vector3(CarVector);
							local1_3.y += ((CarMass * 0.0016999999) + 0.90000004);
							player.car.setMatrix(new Vector3(local1_3), new Ypr(CarYpr));
							player.car.command("stop");
							player.car.setMatrix(new Vector3(CarVector), new Ypr(CarYpr));
							player.car.wakeUp();
						}
					}
					JackStatus = 2;
				} else
				{
					if ((JackVehPos.y > 1))
					{
						Jack_Move_SFX.stop(SFX_Loop_ID);
						Jack_Stop_SFX.play(JackPos, 0.0, 1.0, 1.0, 0);
						JackStatus = 0;
						if (JackVeh)
						{
							JackVeh.destroy();
							JackVeh = null;
						}
						System.timeWarp(1.0);
						Thread.sleep(1000);
						new SfxRef(frontend:0x7er).play();
						new WarningDialog(
							player.controller,
							(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
							"INFO",
							"Your car ground clerance is to hight or your car isn't parked over the jack's arm").display(
						);
						Thread.sleep(1000);
						SFX_Loop_ID = Jack_Move_SFX.play(JackPos, 0.0, 1.0, 1.0, 0);
						JackStatus = 5;
					}
				}
			} else
			{
				if ((JackStatus == 2))
				{
					CarVector = player.car.getPos();
					JackArm.setMatrix(new Vector3(JackPos.x, (CarVector.y + DiffY), JackPos.z), null);
					if (((JackDamage == 1) && (JackWait < System.simTime())))
					{
						JackWait = (System.simTime() + 2.5);
						JackDamage = 2;
						new SfxRef(MW_Mod:0x45r).play(JackPos, 0.0, 1.0, 1.5, 0);
						player.car.command("start");
					} else
					{
						if (((JackDamage == 2) && (JackWait < System.simTime())))
						{
							JackDamage = 3;
							DamageJack();
						} else
						{
							if ((JackDamage == 3))
							{
							}
						}
					}
					if ((JackWait < System.simTime()))
					{
						JackButton.enable();
						JackButton.changeTexture(new ResourceRef(MW_Mod:0x40r));
						JackStatus = 3;
					}
				} else
				{
					if ((JackStatus == 3))
					{
						CarVector = player.car.getPos();
						JackArm.setMatrix(new Vector3(JackPos.x, (CarVector.y + DiffY), JackPos.z), null);
						player.car.command("suspend");
					}
				}
			}
			if ((JackStatus == 4))
			{
				CarVector = player.car.getPos();
				JackArm.setMatrix(new Vector3(JackPos.x, (CarVector.y + DiffY), JackPos.z), null);
				if ((JackWait < System.simTime()))
				{
					SFX_Loop_ID = Jack_Move_SFX.play(JackPos, 0.0, 1.0, 1.0, 0);
					JackStatus = 5;
					JackVehPos = player.car.getPos();
					JackVehPos.y += DiffY;
					player.car.wakeUp();
					player.car.command("start");
					player.car.command("brake");
				}
			}
			if ((JackStatus == 5))
			{
				JackVehPos.y -= 0.005;
				JackArm.setMatrix(new Vector3(JackPos.x, JackVehPos.y, JackPos.z), null);
				if ((JackVehPos.y <= JackPos.y))
				{
					Jack_Move_SFX.stop(SFX_Loop_ID);
					Jack_Stop_SFX.play(JackPos, 0.0, 1.0, 1.0, 0);
					JackStatus = 0;
					ClutchButton.enable();
					JackHit = 0;
					JackButton.changeTexture(new ResourceRef(MW_Mod:0x38r));
					JackButton.enable();
					player.car.command("start");
					player.car.command("stop");
					JackPhys.setMatrix(new Vector3(JackPos), null);
					JackThread.stop();
				}
			}
			JackThread.sleep(20);
		}
	}

	public void DamageJack()
	{
		player.car.command("stop");
		new WarningDialog(
			player.controller,
			(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
			"SURCHARGE",
			"Your car is too heavy for this jack! You damaged team's jack and you mast pay for reprair. You lose some prestige points for foolishness! You can back to garage when jack been reprair. It can take some days. \n THANK YOU...").display(
		);
		JackDamage = 0;
		JackStatus = 0;
		ClutchButton.enable();
		JackHit = 0;
		JackButton.changeTexture(new ResourceRef(MW_Mod:0x38r));
		JackButton.enable();
		JackPhys.setMatrix(new Vector3(JackPos), null);
		JackArm.setMatrix(new Vector3(JackPos), null);
		GameLogic.spendTime(((1 + (Math.random() * 5)) * 86400));
		player.prestige -= ((Math.random() * 0.035) + 0.02);
		player.money -= 1250;
		refreshMoneyString();
		player.car.setMatrix(new Vector3(7.0, (-(0.21000001)), 6.0), new Ypr((-(1.2)), 0, 0));
		player.car.command("start");
		player.car.command("stop");
		JackThread.stop();
	}

	public void cameraSetup(GameRef arg0)
	{
		arg0.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
		arg0.command("dist 1.0 10.0");
		arg0.command("smooth 0.05 0.5");
		arg0.command("zoom 60 5");
		arg0.command("force 0.3 0.5 -0.7");
		arg0.command("torque 0.08");
		if (player.car)
		{
			arg0.command((("move " + player.car.id()) + " 0,0,0 3.5"));
			arg0.command((("look " + player.car.id()) + " 0,0,0 0,0,0"));
		} else
		{
			arg0.command((((("move " + map.id()) + " ") + defLookPos.toString()) + " 3.5"));
			arg0.command((((("look " + map.id()) + " ") + defLookPos.toString()) + " 0,0,0"));
		}
		player.controller.reset();
		player.controller.activateState(ControlSet.CAMTURNSET);
	}

	public void createOSDObjects()
	{
		Style local1;
		Menu local2;
		local1 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		local2 = osd.createMenu(local1, (-(0.98)), (-(0.84000003)), 0, Osd.MD_HORIZONTAL);
		Gadget local3;
		if ((!(roc)))
		{
			local3 = local2.addItem(new ResourceRef(frontend:0x11dr), Garage.CMD_TRACKSELECT, "Choose Track");
			local3 = local2.addItem(
				new ResourceRef(frontend:0x11fr),
				Garage.CMD_ROC,
				"Go to the Race Of Champions"
			);
			if (((GameLogic.gameMode == GameLogic.GM_SINGLECAR) && (!(GameLogic.cheats))))
			{
				local3.disable();
			}
			local2.addSeparator();
			local3 = local2.addItem(
				new ResourceRef(MW_Mod:0x70r),
				Garage.CMD_HITTHESTREET,
				"Change driver skin/model"
			);
			local2.addSeparator();
			local2.addSeparator();
			local3 = local2.addItem(new ResourceRef(frontend:0x120r), Garage.CMD_CARLOT, "Go to the Car Lot");
			if ((GameLogic.gameMode == GameLogic.GM_SINGLECAR))
			{
				local3.disable();
			}
			local3 = local2.addItem(new ResourceRef(frontend:0x121r), Garage.CMD_BUYCARS, "Buy new cars");
			if ((GameLogic.gameMode == GameLogic.GM_SINGLECAR))
			{
				local3.disable();
			}
			local3 = local2.addItem(
				new ResourceRef(frontend:0x122r),
				Garage.CMD_BUYCARSUSED,
				"Buy used cars or sell your car"
			);
			if ((GameLogic.gameMode == GameLogic.GM_SINGLECAR))
			{
				local3.disable();
			}
			local2.addSeparator();
			local3 = local2.addItem(new ResourceRef(frontend:0x125r), Garage.CMD_CATALOG, "Browse the Catalog");
			if (((GameLogic.gameMode == GameLogic.GM_SINGLECAR) && (!(GameLogic.cheats))))
			{
				local3.disable();
			}
			local3 = local2.addItem(
				new ResourceRef(frontend:0x123r),
				Garage.CMD_CLUBINFO,
				"Check your ranking here"
			);
			if ((GameLogic.gameMode == GameLogic.GM_SINGLECAR))
			{
				local3.disable();
			}
			local3 = local2.addItem(
				new ResourceRef(frontend:0x124r),
				Garage.CMD_CARINFO,
				"Details of this car"
			);
			if (player.car)
			{
				if ((!(player.car.chassis)))
				{
					local3.disable();
				}
			}
			local2.addSeparator();
			local2.addSeparator();
			local3 = local2.addItem(
				new ResourceRef(frontend:0x129r),
				Garage.CMD_MECHANIC,
				"Install/Remove parts"
			);
			local3 = local2.addItem(
				new ResourceRef(frontend:0x128r),
				Garage.CMD_TUNE,
				"Fine tune specific parts"
			);
			local3 = local2.addItem(new ResourceRef(frontend:0x12cr), Garage.CMD_PAINT, "Body paint");
			local3 = local2.addItem(
				new ResourceRef(frontend:0x127r),
				Garage.CMD_TEST,
				"Test engine and steering"
			);
			local3 = local2.addItem(new ResourceRef(MW_Mod:0x55r), Garage.CMD_TIME, "Advance time 1 hour");
			local3 = local2.addItem(
				new ResourceRef(frontend:0x12ar),
				Garage.CMD_TIME_6H,
				"Advance time 6 hours"
			);
			local2.addSeparator();
			if (GameLogic.cheats)
			{
				local2.addItem(
					new ResourceRef(frontend:0xb4r),
					Garage.CMD_CHEATMONEY,
					"[CHEAT] CRASH TO DESKTOP ;)"
				);
			} else
			{
				local2.addSeparator();
				local2.addSeparator();
			}
			local2.addSeparator();
			local2.addSeparator();
			local3 = local2.addItem(new ResourceRef(Osd.RID_BACK), Garage.CMD_MAINMENU, "Go back to Main Menu");
			local2 = osd.createMenu(local1, (-(0.98)), (-(0.67)), 0, Osd.MD_VERTICAL);
			if ((JackStatus == 0))
			{
				JackButton = local2.addItem(new ResourceRef(MW_Mod:0x38r), Garage.CMD_JACK, "Jack");
			} else
			{
				JackButton = local2.addItem(new ResourceRef(MW_Mod:0x40r), Garage.CMD_JACK, "Jack");
			}
		} else
		{
			local2.addItem(new ResourceRef(frontend:0x11fr), Garage.CMD_ROCRACE, "Go on the next race");
			local2.addItem(new ResourceRef(frontend:0x11er), Garage.CMD_ROCTEST, "Try the race track");
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addItem(new ResourceRef(frontend:0x123r), Garage.CMD_ROCINFO, "Check the race status");
			local2.addItem(new ResourceRef(frontend:0x124r), Garage.CMD_CARINFO, "Details of your car");
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addItem(new ResourceRef(frontend:0x129r), Garage.CMD_MECHANIC, "Install/Remove parts");
			local2.addItem(new ResourceRef(frontend:0x128r), Garage.CMD_TUNE, "Fine tune specific parts");
			local2.addItem(new ResourceRef(frontend:0x12cr), Garage.CMD_PAINT, "Body paint");
			local2.addItem(new ResourceRef(frontend:0x127r), Garage.CMD_TEST, "Test engine and steering");
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addSeparator();
			local2.addItem(new ResourceRef(Osd.RID_BACK), Garage.CMD_ROCQUIT, "Quit the Race Of Champions");
		}
		menuGroup = osd.endGroup();
		menuVisible = 1;
		local2 = osd.createMenu(local1, 0.46, (-(0.84000003)), 0, Osd.MD_HORIZONTAL);
		local2.addItem(new ResourceRef(frontend:0x11ar), Garage.CMD_ENGINE, "Hide/Unhide ENGINE Parts");
		local2.addItem(new ResourceRef(frontend:0x11br), Garage.CMD_BODY, "Hide/Unhide BODY Parts");
		local2.addItem(
			new ResourceRef(frontend:0x11cr),
			Garage.CMD_RUNNING_GEAR,
			"Hide/Unhide RUNNING GEAR Parts"
		);
		local2.addSeparator();
		ClutchButton = local2.addItem(
			new ResourceRef(MW_Mod:0x94r),
			Garage.CMD_RELASE_CAR,
			"Click to relase clutch pedal"
		);
		local2.addItem(new ResourceRef(Osd.RID_BACK), Garage.CMD_TEST, "Exit Test Car Mode");
		osd.hideGroup((menuGroup2 = osd.endGroup()));
		osd.createRectangle(0.0, (-(0.88)), 2.0, 0.24000001, (-(1)), new ResourceRef(frontend:0x133r));
		infoline = osd.createText("Welcome!", Frontend.mediumFont, Text.ALIGN_RIGHT, 0.97, 0.54);
		DebugText = osd.createText("", Frontend.smallFont, Text.ALIGN_CENTER, 0.0, (-(0.5)));
		if (roc)
		{
			osd.createText(
				(((player.name + " - ") + Roc.roundNames[((roc.numRounds() - roc.getCurrentRound()) - 1)]) + " of R.O.C."),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				(-(0.95000005)),
				(-(0.98))
			);
		} else
		{
			if ((GameLogic.gameMode == GameLogic.GM_SINGLECAR))
			{
				osd.createText(
					"Single car mode",
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					(-(0.95000005)),
					(-(0.98))
				);
			} else
			{
				int local4 = (GameLogic.CLUBMEMBERS - (GameLogic.findRacer(player) % GameLogic.CLUBMEMBERS));
				osd.createText(
					((((player.name + " racing in ") + GameLogic.CLUBNAMES[garageIndex]) + "/") + local4),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					(-(0.95000005)),
					(-(0.98))
				);
			}
		}
		prestigeTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, (-(0.15)), (-(0.98)));
		if ((GameLogic.gameMode != GameLogic.GM_SINGLECAR))
		{
			osd.createText("DAY ", Frontend.smallFont, Text.ALIGN_RIGHT, 0.32000002, (-(0.98)));
			daytxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, 0.32000002, (-(0.98)));
		}
		osd.createText("TIME:", Frontend.smallFont, Text.ALIGN_RIGHT, 0.5, (-(0.98)));
		timetxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, 0.5, (-(0.98)));
		if (roc)
		{
			moneytxt = null;
		} else
		{
			osd.createText("MONEY:", Frontend.smallFont, Text.ALIGN_RIGHT, 0.8, (-(0.98)));
			moneytxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, 0.8, (-(0.98)));
		}
		refreshMoneyString();
		refreshPrestigeString();
		osd.createHotkey(Input.AXIS_MENU, (Input.VIRTUAL | Osd.HK_STATIC), Garage.CMD_ESCAPE, this);
		osd.endGroup();
	}

	public void lockCar()
	{
		if (player.car)
		{
			if ((((JackStatus == 0) || (JackStatus == 4)) || (JackStatus == 5)))
			{
				player.car.setParent(map);
				player.car.setMatrix(new Vector3(Garage.StartPos), new Ypr(Garage.StartYpr));
				player.car.command("reset");
				player.car.command("setsteer -0.7");
				player.car.command("stop");
				JackStatus = 0;
				JackButton.enable();
				JackButton.changeTexture(new ResourceRef(MW_Mod:0x38r));
			} else
			{
				player.car.setParent(map);
				Vector3 local1 = new Vector3(CarVector);
				if (player.car.chassis)
				{
					CarMass = player.car.chassis.getMass();
				} else
				{
					CarMass = 2000;
				}
				local1.y += ((CarMass * 0.0016999999) + 0.90000004);
				player.car.setMatrix(new Vector3(JackPos.x, local1.y, JackPos.z), new Ypr(CarYpr));
				player.car.command("stop");
				player.car.setMatrix(new Vector3(JackPos.x, 0.88699997, JackPos.z), new Ypr(CarYpr));
				player.car.wakeUp();
				player.car.command("setsteer -0.7");
				JackStatus = 3;
				JackButton.enable();
				JackButton.changeTexture(new ResourceRef(MW_Mod:0x40r));
				JackThread = new Thread(this, "Jack Animate Refresher");
				JackThread.start();
			}
		}
	}

	public void releaseCar()
	{
		if (player.car)
		{
			player.car.command("reset");
			player.car.command("start");
			player.car.setParent(player);
		}
	}

	public void refreshMoneyString()
	{
		if (moneytxt)
		{
			new SfxRef(Frontend.SFX_MONEY).play();
			moneytxt.changeText(("$" + Integer.toString(player.money)));
		}
	}

	public void refreshPrestigeString()
	{
		if ((GameLogic.gameMode != GameLogic.GM_SINGLECAR))
		{
			prestigeTxt.changeText(("Prestige: " + player.getPrestigeString()));
		}
	}

	public void changeMode(int arg0)
	{
		if ((mode != arg0))
		{
			if (((mode == Garage.MODE_SZEREL) || (mode == Garage.MODE_TUNE)))
			{
				if (((arg0 != Garage.MODE_SZEREL) && (arg0 != Garage.MODE_TUNE)))
				{
					mechanic.hide();
				}
			} else
			{
				if ((mode == Garage.MODE_PAINT))
				{
					painter.hide();
				} else
				{
					if ((mode == Garage.MODE_TEST))
					{
						player.car.command("stop");
						if (player.render)
						{
							player.render.destroy();
						}
						player.controller.command(("leave " + player.car.id()));
						player.controller.command(("camera " + camera.id()));
						player.hideOsd();
						osdCommand(Garage.CMD_MENU);
						player.car.command("reset");
						player.controller.reset();
						player.controller.activateState(ControlSet.CAMTURNSET);
					}
				}
			}
			int local2 = mode;
			mode = arg0;
			changePointer();
			if (((mode == Garage.MODE_SZEREL) || (mode == Garage.MODE_TUNE)))
			{
				if (((local2 != Garage.MODE_SZEREL) && (local2 != Garage.MODE_TUNE)))
				{
					mechanic.show();
				}
				if ((mode == Garage.MODE_SZEREL))
				{
					mechanic.mode = 0;
				} else
				{
					mechanic.mode = 1;
				}
			} else
			{
				if ((mode == Garage.MODE_PAINT))
				{
					painter.show();
				} else
				{
					if ((mode == Garage.MODE_TEST))
					{
						osdCommand(Garage.CMD_MENU);
						if ((!(mechanic.filterBody)))
						{
							player.render = new RenderRef(map, player.driverID, "player");
							player.controller.command(("renderinstance " + player.render.id()));
						}
						player.controller.command("camera 0");
						player.controller.command(("controllable " + player.car.id()));
						player.car.wakeUp();
						player.controller.reset();
						player.controller.activateState(ControlSet.DRIVERSET);
						player.showOsd();
					}
				}
			}
		}
	}

	public void changePointer()
	{
		if (move)
		{
			Input.cursor.setPointer(Frontend.pointers, "M");
		} else
		{
			if ((mode == Garage.MODE_SZEREL))
			{
				if (drag)
				{
					Input.cursor.setPointer(Frontend.pointers, "A");
				} else
				{
					Input.cursor.setPointer(Frontend.pointers, "G");
				}
			} else
			{
				if ((mode == Garage.MODE_TUNE))
				{
					Input.cursor.setPointer(Frontend.pointers, "B");
				} else
				{
					if (((painter.mode == painter.MODE_COPY) && (painter.copy_status == 0)))
					{
						Input.cursor.setPointer(Frontend.pointers, "J");
					} else
					{
						if ((mode == Garage.MODE_PAINT))
						{
							Input.cursor.setPointer(Frontend.pointers, "D");
						} else
						{
							Input.cursor.setPointer(Frontend.pointers, "J");
						}
					}
				}
			}
		}
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		if ((arg1 == GameRef.EVENT_TIME))
		{
			if ((arg2 == 2))
			{
				addTimer(1, 2);
				super.refresh(GameLogic.getTime());
				refreshPrestigeString();
			}
		}
	}

	public void handleEvent(GameRef arg0, int arg1, String arg2)
	{
		int local4 = (-(1));
		if ((((arg1 == GameRef.EVENT_COLLISION) && (JackHit == 0)) && JackVeh))
		{
			int local5 = JackVeh.id();
			if ((local5 == arg2.token(0).intValue()))
			{
				JackHit = 1;
				JackVehPos = JackVeh.getPos();
				JackVeh.destroy();
				JackVeh = null;
			}
		} else
		{
			if ((arg1 == GameRef.EVENT_CURSOR))
			{
				int local5_2 = arg2.token(++local4).intValue();
				int local6 = arg2.token(++local4).intValue();
				if ((local5_2 == GameType.EC_LDRAGBEGIN))
				{
					if ((mode == Garage.MODE_SZEREL))
					{
						GameRef local7 = arg0;
						int local8 = local7.getInfo(GameType.GII_GETOUT_OK);
						if ((local8 == 0))
						{
							drag = 1;
							changePointer();
						}
					}
				} else
				{
					if ((local5_2 == GameType.EC_LDRAGEND))
					{
						if ((mode == Garage.MODE_SZEREL))
						{
							drag = 0;
							changePointer();
						}
					} else
					{
						if ((local5_2 == GameType.EC_LDROP))
						{
							if ((mode == Garage.MODE_SZEREL))
							{
								GameRef local7_2 = new GameRef(arg2.token(++local4).intValue());
								GameRef local8_2 = new GameRef(arg2.token(++local4).intValue());
								Vector3 local9 = new Vector3(
									arg2.token(++local4).floatValue(),
									arg2.token(++local4).floatValue(),
									arg2.token(++local4).floatValue()
								);
								int local10 = arg2.token(++local4).intValue();
								Object local11 = local7_2.getScriptInstance();
								if (((local7_2.id() == map.id()) || (local11 instanceof Part)))
								{
									int local12 = local8_2.getInfo(GameType.GII_CATEGORY);
									if (((local12 == GameType.GIR_CAT_PART) || (local12 == GameType.GIR_CAT_VEHICLE)))
									{
										Part local13 = local8_2.getScriptInstance();
										GameRef local14 = new GameRef();
										int[] local15 = local13.install_OK(player.car, 0, local14, 0, local9);
										if (local15)
										{
											String local16;
											if ((!((local16 = local13.installCheck(local14.getScriptInstance(), local15)))))
											{
												new SfxRef(GameLogic.SFX_WRENCH).play();
												GameLogic.spendTime(GameLogic.mechTime(local13, 0));
												local8_2.command(("remove 0 " + map.id()));
												GameLogic.spendTime(GameLogic.mechTime(local13, 1));
												local8_2.command(((((((("install 0 " + player.car.id()) + " 0 0 0 ") + local9.x) + " ") + local9.y) + " ") + local9.z));
												player.car.wakeUp();
											} else
											{
												giveWarning(local16);
											}
										}
									} else
									{
										int local13_2 = mechanic.inventory.getItemIDbyButtonPhyId(local10);
										String local14_2;
										if (((local14_2 = mechanic.inventory.installToCar(local13_2, player.car, local9)) && (local14_2 != "")))
										{
											giveWarning(local14_2);
										}
									}
								}
							}
						} else
						{
							if ((local5_2 == GameType.EC_RCLICK))
							{
								GameRef local7_3 = new GameRef(arg2.token(++local4).intValue());
								int local8_3 = local7_3.getInfo(GameType.GII_CATEGORY);
								if (((local8_3 == GameType.GIR_CAT_PART) || (local8_3 == GameType.GIR_CAT_VEHICLE)))
								{
									mechanic.lastLookDestination = local7_3;
									camera.command(((((((("look " + local7_3.id()) + " ") + arg2.token(++local4)) + ",") + arg2.token(++local4)) + ",") + arg2.token(++local4)));
								}
							} else
							{
								if ((local5_2 == GameType.EC_RDRAGBEGIN))
								{
									move = 1;
									changePointer();
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
									player.controller.user_Del(Input.AXIS_CURSOR_X, ControlSet.MOUSE, 0);
									player.controller.user_Del(Input.AXIS_CURSOR_Y, ControlSet.MOUSE, 1);
									Input.cursor.cursor.command("lock");
								} else
								{
									if ((local5_2 == GameType.EC_RDRAGEND))
									{
										move = 0;
										changePointer();
										player.controller.user_Del(Input.AXIS_LOOK_UPDOWN, ControlSet.MOUSE, 1);
										player.controller.user_Del(
											Input.AXIS_LOOK_LEFTRIGHT,
											ControlSet.MOUSE,
											0
										);
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
										Input.cursor.cursor.command("unlock");
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
		if ((arg0 == Garage.CMD_TEST))
		{
			if ((mode == Garage.MODE_TEST))
			{
				changeMode(prevMode);
			} else
			{
				if (player.car)
				{
					ClutchStatus = 1;
					ClutchButton.changeTexture(new ResourceRef(MW_Mod:0x94r));
					String local2 = player.car.isTestable();
					if ((!(local2)))
					{
						prevMode = mode;
						player.car.command("reload");
						changeMode(Garage.MODE_TEST);
						local2 = null;
						local2 = player.car.isDriveable();
						if (local2)
						{
							ClutchButton.disable();
						}
					} else
					{
						giveWarning(("You can't test the car because " + local2));
					}
				} else
				{
					giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
				}
			}
		} else
		{
			if ((arg0 == Garage.CMD_ESCAPE))
			{
				if ((mode == Garage.MODE_TEST))
				{
					osdCommand(Garage.CMD_TEST);
				}
			} else
			{
				if ((arg0 == Garage.CMD_MENU))
				{
					if (menuVisible)
					{
						osd.hideGroup(menuGroup);
						osd.showGroup(menuGroup2);
					} else
					{
						osd.hideGroup(menuGroup2);
						osd.showGroup(menuGroup);
					}
					menuVisible = (1 - menuVisible);
				} else
				{
					if ((arg0 == Garage.CMD_MAINMENU))
					{
						GameLogic.autoSaveQuiet();
						GameLogic.changeActiveSection(parentState);
					} else
					{
						if ((arg0 == Garage.CMD_LOCK_CAR))
						{
							new SfxRef(sound:0x17r).play();
							player.car.command("setsteer -0.7");
							player.car.command("stop");
						} else
						{
							if ((arg0 == Garage.CMD_RELASE_CAR))
							{
								if ((ClutchStatus == 1))
								{
									new SfxRef(sound:0x18r).play();
									player.car.command("start");
									player.car.wakeUp();
									player.car.command("setsteer -0.7");
									ClutchStatus = 0;
									ClutchButton.setToolTip("Click to press clutch pedal");
									ClutchButton.changeTexture(new ResourceRef(MW_Mod:0x93r));
								} else
								{
									new SfxRef(sound:0x17r).play();
									player.car.command("stop");
									player.car.command("setsteer -0.7");
									ClutchStatus = 1;
									ClutchButton.setToolTip("Click to relase clutch pedal");
									ClutchButton.changeTexture(new ResourceRef(MW_Mod:0x94r));
								}
							} else
							{
								if ((arg0 == Garage.CMD_JACK))
								{
									JackDamage = 0;
									if ((JackStatus == 0))
									{
										if (player.car)
										{
											CarVector = player.car.getPos();
											if (((((CarVector.x < (JackPos.x + 0.5)) && (CarVector.x > (JackPos.x - 0.5))) && (CarVector.z < (JackPos.z + 0.5))) && (CarVector.z > (JackPos.z - 0.6))))
											{
												SFX_Loop_ID = Jack_Move_SFX.play(JackPos, 0.0, 1.0, 1.0, 0);
												player.car.wakeUp();
												JackArm.setMatrix(new Vector3(JackPos), null);
												JackPhys.setMatrix(new Vector3(0.0, (-(3)), 0.0), null);
												JackVeh = new Vehicle(map, MW_Mod:0x41r, 6, 2.0, 0.0, 1.0, 1.0);
												JackVeh.setMatrix(new Vector3(JackPos), null);
												JackVeh.setMatrix(
													new Vector3(JackPos.x, (JackPos.y + 17.5), JackPos.z),
													null
												);
												JackVeh.command("stop");
												JackVeh.setMatrix(new Vector3(JackPos), null);
												JackVeh.wakeUp();
												JackButton.changeTexture(new ResourceRef(MW_Mod:0x39r));
												JackButton.disable();
												JackStatus = 1;
												JackThread = new Thread(this, "Jack Animate Refresher");
												JackThread.start();
											} else
											{
												new WarningDialog(
													player.controller,
													(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
													"INFO",
													"Your car isn't parked center over the jack's arm").display(
												);
											}
										} else
										{
											new WarningDialog(
												player.controller,
												(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
												"INFO",
												"You need a car to do this! \n Buy a car or get one from the car lot.").display(
											);
										}
									}
									if ((JackStatus == 3))
									{
										JackButton.changeTexture(new ResourceRef(MW_Mod:0x39r));
										JackButton.disable();
										new SfxRef(MW_Mod:0x43r).play(JackPos, 0.0, 1.0, 1.0, 0);
										JackStatus = 4;
										Vector3 local2_2 = new Vector3(CarVector);
										local2_2.y = ((CarMass * 0.0016999999) - 0.2);
										player.car.setMatrix(
											new Vector3(JackPos.x, local2_2.y, JackPos.z),
											new Ypr(CarYpr)
										);
										player.car.command("stop");
										player.car.setMatrix(new Vector3(CarVector), new Ypr(CarYpr));
										player.car.wakeUp();
										player.car.command("setsteer -0.7");
										player.car.command("brake");
										JackWait = (System.simTime() + 2);
									}
								} else
								{
									if ((arg0 == Garage.CMD_ENGINE))
									{
										filterEngine = (2 - filterEngine);
										if (player.car)
										{
											player.car.command(("filter 1 " + filterEngine));
											player.car.wakeUp();
											player.car.command("suspend");
										}
										mechanic.filterInventory(filterEngine, filterBody, filterRGear);
									} else
									{
										if ((arg0 == Garage.CMD_BODY))
										{
											filterBody = (2 - filterBody);
											if (player.car)
											{
												player.car.command(("filter 2 " + filterBody));
												player.car.wakeUp();
												player.car.command("suspend");
											}
											mechanic.filterInventory(filterEngine, filterBody, filterRGear);
										} else
										{
											if ((arg0 == Garage.CMD_RUNNING_GEAR))
											{
												filterRGear = (2 - filterRGear);
												if (player.car)
												{
													player.car.command(("filter 3 " + filterRGear));
													player.car.wakeUp();
													player.car.command("suspend");
												}
												mechanic.filterInventory(filterEngine, filterBody, filterRGear);
											} else
											{
												if ((arg0 == Garage.CMD_CHEATMONEY))
												{
													player.money += 100000;
													refreshMoneyString();
												} else
												{
													if ((arg0 == Garage.CMD_ROC))
													{
														int local2_3;
														int local3;
														int local4;
														float local5;
														local2_3 = GameLogic.ROC_ENTRYFEE;
														local3 = 5;
														local5 = 7.5;
														local4 = 1000;
														float local6;
														if (player.car)
														{
															local6 = player.car.getPrestige();
														}
														int local7;
														int local8;
														if ((player.money >= local2_3))
														{
															local8 |= 1;
														}
														if ((GameLogic.findRacer(player) >= (GameLogic.speedymen.length - local3)))
														{
															local8 |= 2;
														}
														if ((local6 >= local5))
														{
															local8 |= 4;
														}
														if ((local7 <= local4))
														{
															local8 |= 8;
														}
														int local9 = (30 * 6);
														if (new ROCEntryDialog(player.controller, local8, (local9 - (GameLogic.day % (local9 + 1)))).display())
														{
															GameLogic.setTime((8 * 3600));
															osdCommand(Garage.CMD_BEGIN_ROC);
														}
													} else
													{
														if ((arg0 == Garage.CMD_BEGIN_ROC))
														{
															roc = new Roc(player);
															GameLogic.changeActiveSection(this);
														} else
														{
															if ((arg0 == Garage.CMD_ROCINFO))
															{
																GameLogic.changeActiveSection(new RocInfo());
															} else
															{
																if ((arg0 == Garage.CMD_ROCRACE))
																{
																	String local2_4 = player.car.isDriveable();
																	if ((!(local2_4)))
																	{
																		GameLogic.changeActiveSection(new ROCTrack());
																	} else
																	{
																		giveWarning(local2_4);
																	}
																} else
																{
																	if ((arg0 == Garage.CMD_ROCTEST))
																	{
																		String local2_5 = player.car.isDriveable();
																		if ((!(local2_5)))
																		{
																			ROCTrack local3_2 = new ROCTrack();
																			local3_2.testMode = 1;
																			GameLogic.changeActiveSection(local3_2);
																		} else
																		{
																			giveWarning(local2_5);
																		}
																	} else
																	{
																		if ((arg0 == Garage.CMD_ROCQUIT))
																		{
																			if ((0 == new NoYesDialog(player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "ARE YOU SURE?", "Think twice: You can only resubmit to the next (6 months away) Race Of Champions, if you quit now! Quitting also seriously degrades your prestige.").display()))
																			{
																				GameLogic.spendTime((((8 * 3600) + (24 * 3600)) - GameLogic.getTime()));
																				player.money -= GameLogic.ROC_ENTRYFEE;
																				roc = null;
																				player.prestige *= 0.2;
																				GameLogic.changeActiveSection(this);
																			}
																		} else
																		{
																			if ((arg0 == Garage.CMD_HITTHESTREET))
																			{
																				SavePos = 1;
																				GameLogic.changeActiveSection(new DriverSelect(1));
																			} else
																			{
																				if ((arg0 == Garage.CMD_TESTTRACK))
																				{
																					if (player.car)
																					{
																						String local2_6 = player.car.isDriveable();
																						if ((!(local2_6)))
																						{
																							GameLogic.spendTime(1800);
																							GameLogic.changeActiveSection(new TestTrack());
																						} else
																						{
																							giveWarning(local2_6);
																						}
																					} else
																					{
																						giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
																					}
																				} else
																				{
																					if ((arg0 == Garage.CMD_TRACKSELECT))
																					{
																						if (player.car)
																						{
																							String local2_7 = player.car.isDriveable();
																							if ((!(local2_7)))
																							{
																								GameLogic.autoSaveQuiet();
																								GameLogic.spendTime(1800);
																								GameLogic.changeActiveSection(new TrackSelect());
																							} else
																							{
																								giveWarning(local2_7);
																							}
																						} else
																						{
																							giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
																						}
																					} else
																					{
																						if ((arg0 == Garage.CMD_CARLOT))
																						{
																							GameLogic.autoSaveQuiet();
																							GameLogic.changeActiveSection(player.carlot);
																						} else
																						{
																							if (((arg0 == Garage.CMD_BUYCARS) || (arg0 == Garage.CMD_BUYCARSUSED)))
																							{
																								float local2_8 = (GameLogic.getTime() / 3600);
																								if (((local2_8 > 7) && (local2_8 < 17)))
																								{
																									GameLogic.spendTime(1800);
																									int local3_3;
																									float local4_2 = ((GameLogic.day * 24) + (GameLogic.getTime() / 3600));
																									float local5_2;
																									VehicleDescriptor[] local6_2;
																									if ((arg0 == Garage.CMD_BUYCARSUSED))
																									{
																										local3_3 = 1;
																										local6_2 = GameLogic.carDescriptors_Used;
																										if ((!(GameLogic.dealerVisitTimeStamp_Used)))
																										{
																											GameLogic.dealerVisitTimeStamp_Used = local4_2;
																										}
																										local5_2 = (local4_2 - GameLogic.dealerVisitTimeStamp_Used);
																										GameLogic.dealerVisitTimeStamp_Used = local4_2;
																									} else
																									{
																										if ((!(GameLogic.dealerVisitTimeStamp_New)))
																										{
																											GameLogic.dealerVisitTimeStamp_New = local4_2;
																										}
																										local6_2 = GameLogic.carDescriptors_New;
																										local5_2 = (local4_2 - GameLogic.dealerVisitTimeStamp_New);
																										GameLogic.dealerVisitTimeStamp_New = local4_2;
																									}
																									CarMarket.alterCars(
																										local3_3,
																										local6_2,
																										local5_2
																									);
																									if (((((!(local3_3)) && (player.money < GameLogic.INITIAL_PLAYER_MONEY)) && (!(player.car))) && player.carlot.isEmpty()))
																									{
																										GameLogic.carDescriptors_New = CarMarket.getInitialCars(0);
																									}
																									if ((player.money < GameLogic.INITIAL_PLAYER_MONEY))
																									{
																										GameLogic.carDescriptors_New = CarMarket.getInitialCars(0);
																									}
																									GameLogic.autoSaveQuiet();
																									GameLogic.changeActiveSection(new CarMarket(local3_3, local6_2));
																								} else
																								{
																									giveWarning("The car dealer is closed now! \n Opening hours: 7am to 5pm");
																								}
																							} else
																							{
																								if ((arg0 == Garage.CMD_CATALOG))
																								{
																									SavePos = 1;
																									GameLogic.changeActiveSection(new Catalog());
																								} else
																								{
																									if ((arg0 == Garage.CMD_CLUBINFO))
																									{
																										SavePos = 1;
																										GameLogic.changeActiveSection(new ClubInfo());
																									} else
																									{
																										if ((arg0 == Garage.CMD_CARINFO))
																										{
																											SavePos = 1;
																											if (player.car)
																											{
																												GameLogic.changeActiveSection(new CarInfo(GameLogic.player.car));
																											} else
																											{
																												giveWarning("You need a car to do this! \n Buy a car or get one from the car lot.");
																											}
																										} else
																										{
																											if ((arg0 == Garage.CMD_TIME))
																											{
																												GameLogic.spendTime(3600);
																												player.prestige -= (0.0033299997 / 24.0);
																											} else
																											{
																												if ((arg0 == Garage.CMD_TIME_6H))
																												{
																													GameLogic.spendTime(21600);
																													player.prestige -= (0.01998 / 24.0);
																												} else
																												{
																													if ((arg0 == Garage.CMD_MECHANIC))
																													{
																														changeMode(Garage.MODE_SZEREL);
																													} else
																													{
																														if ((arg0 == Garage.CMD_TUNE))
																														{
																															Input.flushKeys();
																															int local2_9 = GameLogic.kismajomCheck(GameLogic.kismajom);
																															if ((local2_9 >= 0))
																															{
																																new SfxRef(sound:0x24r).play();
																																switch (local2_9)
																																{
																																	case 0:
																																		osdCommand(Garage.CMD_BEGIN_ROC);
																																		break;
																																	case 1:
																																		player.money += 100000;
																																		refreshMoneyString();
																																		break;
																																	case 2:
																																		if ((player.club < 2))
																																		{
																																			player.club++;
																																			int local3_4 = GameLogic.findRacer(player);
																																			GameLogic.challenge(
																																				local3_4,
																																				(player.club * 20),
																																				0,
																																				1,
																																				1
																																			);
																																			GameLogic.challenge(
																																				local3_4,
																																				(player.club * 20),
																																				0,
																																				1,
																																				0
																																			);
																																			GameLogic.changeActiveSection(this);
																																		} else
																																		{
																																			new SfxRef(frontend:0x81r).play();
																																		}
																																		break;
																																	case 3:
																																		GameLogic.cheats = 1;
																																		infoline.changeText("Let's cheating!");
																																		break;
																																	case 4:
																																		GameLogic.day = 180;
																																		GameLogic.dayTxt.changeText(GameLogic.day);
																																		break;
																																}
																															} else
																															{
																																changeMode(Garage.MODE_TUNE);
																															}
																														} else
																														{
																															if ((arg0 == Garage.CMD_PAINT))
																															{
																																changeMode(Garage.MODE_PAINT);
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
					}
				}
			}
		}
	}

	public void giveWarning(String arg0, String arg1)
	{
		new WarningDialog(player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), arg0, arg1).display();
	}

	public void giveWarning(String arg0)
	{
		giveWarning("WARNING", arg0);
	}

}

public class ROCEntryDialog extends Dialog
{
	static final int CMD_INFO0 = 0;
	static final int CMD_INFO1 = 1;
	static final int CMD_INFO2 = 2;
	static final int CMD_INFO3 = 3;
	static final int CMD_ENTER = 4;
	static final int CMD_CANCEL = 5;
	int reqs;
	int daysleft;
	Controller ctrl;

	public ROCEntryDialog(Controller arg0, int arg1, int arg2)
	{
		super(
			arg0,
			(((Dialog.DF_FULLSCREEN | Dialog.DF_MODAL) | Dialog.DF_DARKEN) | Dialog.DF_FREEZE),
			null,
			null
		);
		reqs = arg1;
		daysleft = arg2;
		ctrl = arg0;
	}

	public void show()
	{
		float local1 = (-(0.58));
		float local2 = (-(0.52));
		float local3 = 0.52;
		float local4 = 0.14;
		float local5;
		float local6;
		osd.createRectangle(0.0, 0.0, 2.0, 2.0, (-(2)), Osd.RRT_DARKEN);
		osd.createRectangle(0.0, 0.0, 1.12, 2.0, (-(1)), new ResourceRef(frontend:0x1er));
		Style local7 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		Menu local8 = osd.createMenu(local7, local2, local1, local4, Osd.MD_VERTICAL);
		local8.addItem(new ResourceRef(frontend:0xb3r), ROCEntryDialog.CMD_INFO0, null);
		local8.addItem(new ResourceRef(frontend:0xb3r), ROCEntryDialog.CMD_INFO1, null);
		local8.addItem(new ResourceRef(frontend:0xb3r), ROCEntryDialog.CMD_INFO2, null);
		local8.addItem(new ResourceRef(frontend:0xb3r), ROCEntryDialog.CMD_INFO3, null);
		local5 = local3;
		local6 = local1;
		ResourceRef local9;
		if ((reqs & 2))
		{
			local9 = new ResourceRef(Osd.RID_OK);
		} else
		{
			local9 = new ResourceRef(Osd.RID_CANCEL);
		}
		osd.createRectangle(local5, local6, 0.1, 0.1, 1.0, 0.0, 0.0, 0, local9);
		local6 += local4;
		if ((reqs & 4))
		{
			local9 = new ResourceRef(Osd.RID_OK);
		} else
		{
			local9 = new ResourceRef(Osd.RID_CANCEL);
		}
		osd.createRectangle(local5, local6, 0.1, 0.1, 1.0, 0.0, 0.0, 0, local9);
		local6 += local4;
		if ((reqs & 1))
		{
			local9 = new ResourceRef(Osd.RID_OK);
		} else
		{
			local9 = new ResourceRef(Osd.RID_CANCEL);
		}
		osd.createRectangle(local5, local6, 0.1, 0.1, 1.0, 0.0, 0.0, 0, local9);
		local6 += local4;
		if ((reqs & 8))
		{
			local9 = new ResourceRef(Osd.RID_OK);
		} else
		{
			local9 = new ResourceRef(Osd.RID_CANCEL);
		}
		osd.createRectangle(local5, local6, 0.1, 0.1, 1.0, 0.0, 0.0, 0, local9);
		local6 += local4;
		local8 = osd.createMenu(local7, (-(0.4)), 0.82, 0.8, Osd.MD_HORIZONTAL);
		Gadget local10 = local8.addItem(new ResourceRef(Osd.RID_OK), ROCEntryDialog.CMD_ENTER, "ENTER ROC");
		local8.addItem(new ResourceRef(Osd.RID_CANCEL), ROCEntryDialog.CMD_CANCEL, "GO BACK TO GARAGE");
		if (GameLogic.cheats)
		{
			local8 = osd.createMenu(local7, (-(0.94000006)), 0.90000004, 0.1, Osd.MD_HORIZONTAL);
			local8.addItem(new ResourceRef(Osd.RID_OK), ROCEntryDialog.CMD_ENTER, "[CHEAT] ALWAYS ENTER ROC");
		}
		if (daysleft)
		{
			osd.createText(
				(("ONLY " + daysleft) + " DAYS LEFT!"),
				Frontend.mediumFont,
				Text.ALIGN_CENTER,
				0.0,
				0.8
			);
		} else
		{
			osd.createText("DAY OF ROC! ENTER NOW!", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.8);
		}
		if ((reqs != 15))
		{
			local10.disable();
		}
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, ROCEntryDialog.CMD_CANCEL, this);
		super.show();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == ROCEntryDialog.CMD_INFO0))
		{
			info("You must be among the top ranked players. \n Be within the top 5 of the Red Flame Racing Club and you can enter the R.O.C.");
		} else
		{
			if ((arg0 == ROCEntryDialog.CMD_INFO1))
			{
				info("Bring a highly tuned & respected car. Your car must have at least 750 prestige point.");
			} else
			{
				if ((arg0 == ROCEntryDialog.CMD_INFO2))
				{
					info("Collect the entry fee: $100000. You'll get free repair and tuning services for this fee.");
				} else
				{
					if ((arg0 == ROCEntryDialog.CMD_INFO3))
					{
						info("Total weight of your parts-bin is limited to 2000 lbs. \n Don't forget to bring enough spare tyres and N2O.");
					} else
					{
						if ((arg0 == ROCEntryDialog.CMD_CANCEL))
						{
							result = 0;
							notify();
						} else
						{
							if ((arg0 == ROCEntryDialog.CMD_ENTER))
							{
								result = 1;
								if ((daysleft != 0))
								{
									if ((1 == new YesNoDialog(ctrl, ((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_HIGHPRI), "QUESTION", (("There's " + daysleft) + " days left till the following Race of Champions. \n Do you want to skip the remainig days?")).display()))
									{
										result = 0;
									} else
									{
										GameLogic.spendTime(((((daysleft * 24) * 3600) - GameLogic.getTime()) + (8 * 3600)));
									}
								}
								notify();
							}
						}
					}
				}
			}
		}
	}

	public void info(String arg0)
	{
		new WarningDialog(
			controller,
			((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_HIGHPRI),
			"INFO",
			arg0).display(
		);
	}

}

public class Animater implements Runnable
{
}
