package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class CarSelect extends Scene implements GameState
{
	static final int CMD_BACK = 1;
	static final int CMD_OK = 2;
	static final int CMD_ABOUT = 3;
	static final int CMD_TUNNING = 4;
	static final int CMD_LIGHTS = 5;
	static final int CMD_COLOR = 6;
	static final int CMD_UP = 7;
	static final int CMD_DOWN = 8;
	static final int CMD_VEH0 = 20;
	static final int CMD_VEH1 = 21;
	static final int CMD_VEH2 = 22;
	static final int CMD_VEH3 = 23;
	static final int CMD_VEH4 = 24;
	static final int CMD_VEH5 = 25;
	static final int CMD_VEH6 = 26;
	static final int CMD_VEH7 = 27;
	static final int CMD_VEH8 = 28;
	static final int CMD_VEH9 = 29;
	static final int CMD_VEH10 = 30;
	static final int CMD_VEH11 = 31;
	static final int CMD_VEH12 = 32;
	static final int CMD_VEH13 = 33;
	static final int CMD_VEH14 = 34;
	static final int CMD_VEH15 = 35;
	static final int CMD_VEH16 = 36;
	static final int CMD_VEH17 = 37;
	static final int CMD_VEH18 = 38;
	static final int CMD_VEH19 = 39;
	static final int CMD_LIST = 69;
	static final int CMD_AMBULANCE = 70;
	static final int CMD_ARMOREDVAN = 71;
	static final int CMD_BADGE = 72;
	static final int CMD_CIVILVAN = 73;
	static final int CMD_COACH = 74;
	static final int CMD_ERBILAC = 75;
	static final int CMD_FIREENGINE = 76;
	static final int CMD_NINJA = 77;
	static final int CMD_POLICE = 78;
	static final int CMD_RCTRUCK = 79;
	static final int CMD_SCHOOLBUS = 80;
	static final int CMD_STALLION = 81;
	static final int CMD_SUNSET = 82;
	static final int CMD_TAXI = 83;
	static final int CMD_TEG = 84;
	static final int CMD_WAGON = 85;
	static final int BackGroundVideo;
	Button TunningButton;
	Button UpButton;
	Button DownButton;
	Button TakeButton;
	Button VehButton0;
	Button VehButton1;
	Button VehButton2;
	Button VehButton3;
	Button VehButton4;
	Button VehButton5;
	Button VehButton6;
	Button VehButton7;
	Button VehButton8;
	Button VehButton9;
	Button VehButton10;
	Button VehButton11;
	Button VehButton12;
	Button VehButton13;
	Button VehButton14;
	Button VehButton15;
	Button VehButton16;
	Button VehButton17;
	Button VehButton18;
	Button VehButton19;
	Style Menu_Button;
	Style Menu_Center;
	Style Menu_Left;
	Style Menu_Right;
	Style Menu_Left_Small;
	Style Menu_Right_Small;
	Text PageTxt;
	Text CurrentCarName;
	Menu m;
	GameState prev_state;
	int actGroup;
	int MainGroup;
	int ListGroup;
	int Tunning = 1;
	int Mode;
	int ButtonNum;
	int VehAmount;
	int LastCommand;
	int ColorIDX;
	int CurrentName;
	int CurrentPage;
	float OldTime;
	float colorSeed;
	GameRef CarScene;
	Camera Cam;
	RenderRef SceneSun;
	Thread CamThread;
	Ypr CamYpr;
	Vector3 CamVector;
	Osd osd;
	GameState parentState;
	Player player;

	public static final float Check(float arg0)
	{
		return (Math.sqrt(arg0) + 3.75);
	}

	public CarSelect()
	{
		super();
		player = GameLogic.player;
		Mode = 0;
		float local1 = (Math.random() * 100);
		if ((MainMenu.Check(local1) != (Math.sqrt(local1) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		createNativeInstance();
	}

	public CarSelect(Player arg0)
	{
		super();
		player = arg0;
		Mode = 1;
		float local2 = (Math.random() * 100);
		if ((MainMenu.Check(local2) != (Math.sqrt(local2) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		createNativeInstance();
	}

	public void enter(GameState arg0)
	{
		Frontend.loadingScreen.show();
		osd = new Osd();
		osd.globalHandler = this;
		setEventMask(GameRef.EVENT_TIME);
		createOSDObjects();
		osd.show();
		Input.cursor.enable(1);
		setEventMask(GameRef.EVENT_CURSOR);
		if ((!(parentState)))
		{
			parentState = arg0;
		}
		if (((!(arg0)) instanceof Garage))
		{
			new SfxRef(GameLogic.SFX_LEAVEGARAGE).play();
		}
		GfxEngine.openVideo("Data\\Fmv\\Prime.avi", 1, 1);
		CarSelect.BackGroundVideo = 1;
		GfxEngine.setGlobalEnvmap(new ResourceRef(MW_Mod:0x69r));
		OldTime = GameLogic.getTime();
		GameLogic.setTime(0);
		CarScene = new Dummy(GameRef.WORLDTREEROOT);
		Cam = new Camera(CarScene, osd.getViewport(), 0);
		RenderRef local2 = new RenderRef(maps.skydome:0x24r);
		local2.setLight(14443620, 6579300, 13158600);
		SceneSun = new RenderRef(CarScene, local2, "light for this scene");
		if ((Mode == 1))
		{
			if (player.car)
			{
				player.car.setParent(CarScene);
				CurrentCarName.changeText("Dummy car");
				if (player.car.chassis)
				{
					CurrentCarName.changeText(player.car.chassis.vehicleName);
				}
			}
		}
		CamThread = new Thread(this, "Cam Animate Refresher");
		CamThread.start();
		Frontend.loadingScreen.display();
	}

	public void exit(GameState arg0)
	{
		Frontend.loadingScreen.show();
		clearEventMask(GameRef.EVENT_ANY);
		removeAllTimers();
		Input.cursor.enable(0);
		osd.hide();
		osd.finalize();
		SceneSun.destroy();
		CamThread.stop();
		CamThread = null;
		GameLogic.setTime(OldTime);
		if (((!(arg0)) instanceof Garage))
		{
			new SfxRef(GameLogic.SFX_LEAVEGARAGE).play();
		}
		if (CarSelect.BackGroundVideo)
		{
			GfxEngine.closeVideo();
		}
		Frontend.loadingScreen.display();
	}

	public void createOSDObjects()
	{
		Menu_Center = new Style(0.4, 0.13, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		Menu_Left = new Style(0.4, 0.13, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
		Menu_Right = new Style(0.6, 0.13, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		Menu_Left_Small = new Style(0.6, 0.13, Frontend.smallFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
		Menu_Right_Small = new Style(0.6, 0.13, Frontend.smallFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		Menu_Button = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		osd.createHeader("CAR SELECTOR");
		osd.createBG(new ResourceRef(frontend:0x16r));
		CurrentCarName = osd.createText(
			"- NO CAR SELECTED -",
			Frontend.largeFont,
			Text.ALIGN_LEFT,
			(-(0.95000005)),
			(-(0.86))
		);
		CurrentCarName.changeColor(-4144960);
		m = osd.createMenu(Menu_Right, 0.98, (-(0.83)), 0);
		m.addItem("ABOUT", CarSelect.CMD_ABOUT);
		m = osd.createMenu(Menu_Left, (-(0.98)), 0.85, 0);
		m.addItem("BACK", CarSelect.CMD_BACK);
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), CarSelect.CMD_BACK, this);
		osd.endGroup();
		m = osd.createMenu(Menu_Center, 0.25, 0.85, 0);
		m.addItem("LIGHTS ON/OFF", CarSelect.CMD_LIGHTS);
		m = osd.createMenu(Menu_Center, (-(0.25)), 0.85, 0);
		m.addItem("COLOR", CarSelect.CMD_COLOR);
		m = osd.createMenu(Menu_Right, 0.98, 0.85, 0);
		TakeButton = m.addItem("TAKE IT", CarSelect.CMD_OK);
		if ((!(player.car)))
		{
			TakeButton.disable();
		}
		m = osd.createMenu(Menu_Left, (-(0.98)), (-(0.45)), 0);
		m.addItem("Ambulance", CarSelect.CMD_AMBULANCE);
		m.addItem("Armored Van", CarSelect.CMD_ARMOREDVAN);
		m.addItem("Badge", CarSelect.CMD_BADGE);
		m.addItem("Civil Van", CarSelect.CMD_CIVILVAN);
		m.addItem("Coach", CarSelect.CMD_COACH);
		m.addItem("Erbilac", CarSelect.CMD_ERBILAC);
		m.addItem("Fire Engine", CarSelect.CMD_FIREENGINE);
		m.addItem("Ninja", CarSelect.CMD_NINJA);
		m = osd.createMenu(Menu_Right, 0.98, (-(0.45)), 0);
		m.addItem("Police", CarSelect.CMD_POLICE);
		m.addItem("School Bus", CarSelect.CMD_SCHOOLBUS);
		m.addItem("Stallion", CarSelect.CMD_STALLION);
		m.addItem("Sunset", CarSelect.CMD_SUNSET);
		m.addItem("Taxi", CarSelect.CMD_TAXI);
		m.addItem("Teg", CarSelect.CMD_TEG);
		m.addItem("Wagon", CarSelect.CMD_WAGON);
		m.addItem("CARS LIST", CarSelect.CMD_LIST);
		actGroup = (MainGroup = osd.endGroup());
		m = osd.createMenu(Menu_Center, (-(0.4)), 0.85, 0);
		TunningButton = m.addItem("STOCK", CarSelect.CMD_TUNNING);
		m = osd.createMenu(Menu_Center, (-(0.1)), 0.85, 0);
		m.addItem("COLOR", CarSelect.CMD_COLOR);
		m = osd.createMenu(Menu_Center, 0.3, 0.85, 0);
		m.addItem("LIGHTS ON/OFF", CarSelect.CMD_LIGHTS);
		m = osd.createMenu(Menu_Button, 0.65000004, 0.85, 0, Osd.MD_VERTICAL);
		UpButton = m.addItem(new ResourceRef(frontend:0xafr), CarSelect.CMD_UP, "Prev Page");
		PageTxt = osd.createText("p. 1", Frontend.largeFont, Text.ALIGN_CENTER, 0.78499997, 0.82);
		PageTxt.changeColor(-4144960);
		m = osd.createMenu(Menu_Button, 0.90000004, 0.85, 0, Osd.MD_VERTICAL);
		DownButton = m.addItem(new ResourceRef(frontend:0x88r), CarSelect.CMD_DOWN, "Next Page");
		m = osd.createMenu(Menu_Left_Small, (-(0.98)), (-(0.6)), 0);
		VehButton0 = m.addItem("", CarSelect.CMD_VEH0);
		VehButton1 = m.addItem("", CarSelect.CMD_VEH1);
		VehButton2 = m.addItem("", CarSelect.CMD_VEH2);
		VehButton3 = m.addItem("", CarSelect.CMD_VEH3);
		VehButton4 = m.addItem("", CarSelect.CMD_VEH4);
		VehButton5 = m.addItem("", CarSelect.CMD_VEH5);
		VehButton6 = m.addItem("", CarSelect.CMD_VEH6);
		VehButton7 = m.addItem("", CarSelect.CMD_VEH7);
		VehButton8 = m.addItem("", CarSelect.CMD_VEH8);
		VehButton9 = m.addItem("", CarSelect.CMD_VEH9);
		m = osd.createMenu(Menu_Right_Small, 0.98, (-(0.6)), 0);
		VehButton10 = m.addItem("", CarSelect.CMD_VEH10);
		VehButton11 = m.addItem("", CarSelect.CMD_VEH11);
		VehButton12 = m.addItem("", CarSelect.CMD_VEH12);
		VehButton13 = m.addItem("", CarSelect.CMD_VEH13);
		VehButton14 = m.addItem("", CarSelect.CMD_VEH14);
		VehButton15 = m.addItem("", CarSelect.CMD_VEH15);
		VehButton16 = m.addItem("", CarSelect.CMD_VEH16);
		VehButton17 = m.addItem("", CarSelect.CMD_VEH17);
		VehButton18 = m.addItem("", CarSelect.CMD_VEH18);
		VehButton19 = m.addItem("", CarSelect.CMD_VEH19);
		osd.hideGroup((ListGroup = osd.endGroup()));
	}

	public void RefreshOsd()
	{
		CurrentName = (CurrentPage * 20);
		VehAmount = (GameLogic.ChassisBox.size() - 1);
		if (((CurrentName + 0) <= VehAmount))
		{
			VehButton0.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 0)].vehicleName);
			VehButton0.enable();
		} else
		{
			VehButton0.changeLabelText("");
			VehButton0.disable();
		}
		if (((CurrentName + 1) <= VehAmount))
		{
			VehButton1.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 1)].vehicleName);
			VehButton1.enable();
		} else
		{
			VehButton1.changeLabelText("");
			VehButton1.disable();
		}
		if (((CurrentName + 2) <= VehAmount))
		{
			VehButton2.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 2)].vehicleName);
			VehButton2.enable();
		} else
		{
			VehButton2.changeLabelText("");
			VehButton2.disable();
		}
		if (((CurrentName + 3) <= VehAmount))
		{
			VehButton3.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 3)].vehicleName);
			VehButton3.enable();
		} else
		{
			VehButton3.changeLabelText("");
			VehButton3.disable();
		}
		if (((CurrentName + 4) <= VehAmount))
		{
			VehButton4.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 4)].vehicleName);
			VehButton4.enable();
		} else
		{
			VehButton4.changeLabelText("");
			VehButton4.disable();
		}
		if (((CurrentName + 5) <= VehAmount))
		{
			VehButton5.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 5)].vehicleName);
			VehButton5.enable();
		} else
		{
			VehButton5.changeLabelText("");
			VehButton5.disable();
		}
		if (((CurrentName + 6) <= VehAmount))
		{
			VehButton6.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 6)].vehicleName);
			VehButton6.enable();
		} else
		{
			VehButton6.changeLabelText("");
			VehButton6.disable();
		}
		if (((CurrentName + 7) <= VehAmount))
		{
			VehButton7.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 7)].vehicleName);
			VehButton7.enable();
		} else
		{
			VehButton7.changeLabelText("");
			VehButton7.disable();
		}
		if (((CurrentName + 8) <= VehAmount))
		{
			VehButton8.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 8)].vehicleName);
			VehButton8.enable();
		} else
		{
			VehButton8.changeLabelText("");
			VehButton8.disable();
		}
		if (((CurrentName + 9) <= VehAmount))
		{
			VehButton9.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 9)].vehicleName);
			VehButton9.enable();
		} else
		{
			VehButton9.changeLabelText("");
			VehButton9.disable();
		}
		if (((CurrentName + 10) <= VehAmount))
		{
			VehButton10.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 10)].vehicleName);
			VehButton10.enable();
		} else
		{
			VehButton10.changeLabelText("");
			VehButton10.disable();
		}
		if (((CurrentName + 11) <= VehAmount))
		{
			VehButton11.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 11)].vehicleName);
			VehButton11.enable();
		} else
		{
			VehButton11.changeLabelText("");
			VehButton11.disable();
		}
		if (((CurrentName + 12) <= VehAmount))
		{
			VehButton12.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 12)].vehicleName);
			VehButton12.enable();
		} else
		{
			VehButton12.changeLabelText("");
			VehButton12.disable();
		}
		if (((CurrentName + 13) <= VehAmount))
		{
			VehButton13.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 13)].vehicleName);
			VehButton13.enable();
		} else
		{
			VehButton13.changeLabelText("");
			VehButton13.disable();
		}
		if (((CurrentName + 14) <= VehAmount))
		{
			VehButton14.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 14)].vehicleName);
			VehButton14.enable();
		} else
		{
			VehButton14.changeLabelText("");
			VehButton14.disable();
		}
		if (((CurrentName + 15) <= VehAmount))
		{
			VehButton15.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 15)].vehicleName);
			VehButton15.enable();
		} else
		{
			VehButton15.changeLabelText("");
			VehButton15.disable();
		}
		if (((CurrentName + 16) <= VehAmount))
		{
			VehButton16.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 16)].vehicleName);
			VehButton16.enable();
		} else
		{
			VehButton16.changeLabelText("");
			VehButton16.disable();
		}
		if (((CurrentName + 17) <= VehAmount))
		{
			VehButton17.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 17)].vehicleName);
			VehButton17.enable();
		} else
		{
			VehButton17.changeLabelText("");
			VehButton17.disable();
		}
		if (((CurrentName + 18) <= VehAmount))
		{
			VehButton18.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 18)].vehicleName);
			VehButton18.enable();
		} else
		{
			VehButton18.changeLabelText("");
			VehButton18.disable();
		}
		if (((CurrentName + 19) <= VehAmount))
		{
			VehButton19.changeLabelText(GameLogic.ChassisBox.elementData[(CurrentName + 19)].vehicleName);
			VehButton19.enable();
		} else
		{
			VehButton19.changeLabelText("");
			VehButton19.disable();
		}
	}

	public void RefreshCar()
	{
		if (player.car)
		{
			player.car.command("reset");
			player.car.setMatrix(null, null);
			player.car.command("idle");
			player.car.command("setsteer -0.6");
			TakeButton.enable();
		}
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == CarSelect.CMD_BACK))
		{
			if ((actGroup == MainGroup))
			{
				if (player.car)
				{
					player.car.command("reset");
					player.car.setMatrix(null, null);
					player.car.setParent(player);
				}
				GameLogic.changeActiveSection(parentState);
			} else
			{
				if ((actGroup = ListGroup))
				{
					if (player.car)
					{
						if (player.car.chassis)
						{
							if (File.exists("save/temp/CopyCar"))
							{
								File.delete("save/temp/CopyCar");
								File.delete("save/temp/CopyCar.*");
							}
							player.car.save("save/temp/CopyCar");
							player.car.destroy();
							player.car = Vehicle.load("save/temp/CopyCar", CarScene);
							RefreshCar();
						}
					}
					changeGroup(MainGroup);
				}
			}
		} else
		{
			if ((arg0 == CarSelect.CMD_OK))
			{
				if ((Mode == 1))
				{
					player.car.command("reset");
					player.car.setMatrix(null, null);
					player.car.setParent(player);
					GameLogic.changeActiveSection(parentState);
				} else
				{
					Frontend.loadingScreen.show();
					player.car.command("reset");
					player.car.setMatrix(null, null);
					player.car.setParent(player);
					player.driverID = new RenderRef(GameLogic.HUMAN_PLAYER);
					if ((!(player.character)))
					{
						player.character = new ResourceRef(Racer.RID_FEJ);
						player.name = "Player";
					}
					GameLogic.gameMode = GameLogic.GM_SINGLECAR;
					GameLogic.player.money = 1000000;
					player.parts = new Inventory(player);
					player.carlot = new CarLot(player);
					GameLogic.changeActiveSection(GameLogic.garage);
				}
			} else
			{
				if ((arg0 == CarSelect.CMD_UP))
				{
					if ((CurrentPage > 0))
					{
						CurrentPage -= 1;
					}
					CurrentName = (CurrentPage * 20);
					PageTxt.changeText(("p. " + (CurrentPage + 1)));
					DownButton.changeTexture(new ResourceRef(frontend:0x88r));
					DownButton.enable();
					if ((CurrentPage == 0))
					{
						UpButton.changeTexture(new ResourceRef(frontend:0xafr));
						UpButton.disable();
					}
					RefreshOsd();
				} else
				{
					if ((arg0 == CarSelect.CMD_DOWN))
					{
						if ((GameLogic.ChassisRoot.length >= (CurrentName + 10)))
						{
							CurrentPage += 1;
						}
						CurrentName = (CurrentPage * 20);
						PageTxt.changeText(("p. " + (CurrentPage + 1)));
						UpButton.changeTexture(new ResourceRef(frontend:0x85r));
						UpButton.enable();
						VehAmount = (GameLogic.ChassisBox.size() - 1);
						if ((VehAmount < (CurrentName + 20)))
						{
							DownButton.changeTexture(new ResourceRef(frontend:0xafr));
							DownButton.disable();
						}
						RefreshOsd();
					} else
					{
						if ((arg0 == CarSelect.CMD_ABOUT))
						{
							new WarningDialog(
								GameLogic.player.controller,
								((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
								"CAR SELECTOR v1.1",
								" \n Choose car and click 'TAKE IT' to drive. \n \n Now you can change color and choose car from list. \n \n By Miran").display(
							);
						} else
						{
							if ((arg0 == CarSelect.CMD_COLOR))
							{
								if (player.car.chassis)
								{
									if ((ColorIDX == (GameLogic.CARCOLORS.length - 1)))
									{
										ColorIDX = 0;
									} else
									{
										ColorIDX += 1;
									}
									player.car.command((("texture " + GameLogic.CARCOLORS[ColorIDX]) + " 1"));
								} else
								{
									osdCommand(LastCommand);
								}
							}
						}
					}
				}
			}
		}
		if ((arg0 == CarSelect.CMD_TUNNING))
		{
			if ((Tunning == 1))
			{
				Tunning = 2;
				TunningButton.changeLabelText("TUNNING");
			} else
			{
				Tunning = 1;
				TunningButton.changeLabelText("STOCK");
			}
		} else
		{
			if ((arg0 == CarSelect.CMD_LIGHTS))
			{
				player.car.command("reset");
				GameLogic.setTime((GameLogic.getTime() + (12 * 3600)));
				player.car.command("idle");
			}
		}
		if ((arg0 == CarSelect.CMD_LIST))
		{
			changeGroup(ListGroup);
			UpButton.disable();
			CurrentPage = 0;
			osdCommand(CarSelect.CMD_UP);
			RefreshOsd();
		} else
		{
			if (((arg0 >= CarSelect.CMD_VEH0) && (arg0 <= CarSelect.CMD_VEH19)))
			{
				if ((arg0 == CarSelect.CMD_VEH0))
				{
					ButtonNum = 0;
				}
				if ((arg0 == CarSelect.CMD_VEH1))
				{
					ButtonNum = 1;
				}
				if ((arg0 == CarSelect.CMD_VEH2))
				{
					ButtonNum = 2;
				}
				if ((arg0 == CarSelect.CMD_VEH3))
				{
					ButtonNum = 3;
				}
				if ((arg0 == CarSelect.CMD_VEH4))
				{
					ButtonNum = 4;
				}
				if ((arg0 == CarSelect.CMD_VEH5))
				{
					ButtonNum = 5;
				}
				if ((arg0 == CarSelect.CMD_VEH6))
				{
					ButtonNum = 6;
				}
				if ((arg0 == CarSelect.CMD_VEH7))
				{
					ButtonNum = 7;
				}
				if ((arg0 == CarSelect.CMD_VEH8))
				{
					ButtonNum = 8;
				}
				if ((arg0 == CarSelect.CMD_VEH9))
				{
					ButtonNum = 9;
				}
				if ((arg0 == CarSelect.CMD_VEH10))
				{
					ButtonNum = 10;
				}
				if ((arg0 == CarSelect.CMD_VEH11))
				{
					ButtonNum = 11;
				}
				if ((arg0 == CarSelect.CMD_VEH12))
				{
					ButtonNum = 12;
				}
				if ((arg0 == CarSelect.CMD_VEH13))
				{
					ButtonNum = 13;
				}
				if ((arg0 == CarSelect.CMD_VEH14))
				{
					ButtonNum = 14;
				}
				if ((arg0 == CarSelect.CMD_VEH15))
				{
					ButtonNum = 15;
				}
				if ((arg0 == CarSelect.CMD_VEH16))
				{
					ButtonNum = 16;
				}
				if ((arg0 == CarSelect.CMD_VEH17))
				{
					ButtonNum = 17;
				}
				if ((arg0 == CarSelect.CMD_VEH18))
				{
					ButtonNum = 18;
				}
				if ((arg0 == CarSelect.CMD_VEH19))
				{
					ButtonNum = 19;
				}
				if (player.car)
				{
					player.car.destroy();
				}
				player.car = new Vehicle();
				player.car.chassis = new Chassis();
				player.car.chassis = create(
					CarScene,
					new GameRef(GameLogic.ChassisRoot[GameLogic.ChassisResNoumber[(CurrentName + ButtonNum)]]),
					"0,0,0,0,0,0,",
					"chassis"
				);
				player.car = new Vehicle(player.car.chassis);
				CurrentCarName.changeText(player.car.chassis.vehicleName);
				player.car.chassis.addStockParts(new Descriptor(player.car.chassis.getTexture(), Tunning, Tunning, 1, 1));
				player.car.command((("texture " + GameLogic.CARCOLORS[ColorIDX]) + " 1"));
				RefreshCar();
			}
		}
		if ((arg0 == CarSelect.CMD_AMBULANCE))
		{
			if (player.car)
			{
				player.car.destroy();
			}
			colorSeed = (Math.random() * 12345);
			GameRef local2 = new GameRef(
				CarScene,
				cars.traffic.Ambulance:0x6r,
				((("0,0,0" + ",") + "0,0,0,") + colorSeed),
				"nightracer1"
			);
			player.car = new Vehicle(local2);
			CurrentCarName.changeText("Ambulance");
			LastCommand = arg0;
			RefreshCar();
		} else
		{
			if ((arg0 == CarSelect.CMD_ARMOREDVAN))
			{
				if (player.car)
				{
					player.car.destroy();
				}
				colorSeed = (Math.random() * 12345);
				GameRef local2_2 = new GameRef(
					CarScene,
					cars.traffic.ArmoredVan:0x6r,
					((("0,0,0" + ",") + "0,0,0,") + colorSeed),
					"nightracer1"
				);
				player.car = new Vehicle(local2_2);
				CurrentCarName.changeText("Armored Van");
				LastCommand = arg0;
				RefreshCar();
			} else
			{
				if ((arg0 == CarSelect.CMD_BADGE))
				{
					if (player.car)
					{
						player.car.destroy();
					}
					colorSeed = (Math.random() * 12345);
					GameRef local2_3 = new GameRef(
						CarScene,
						cars.fake_racers.Badge:0x6r,
						((("0,0,0" + ",") + "0,0,0,") + colorSeed),
						"nightracer1"
					);
					player.car = new Vehicle(local2_3);
					CurrentCarName.changeText("Badge");
					LastCommand = arg0;
					RefreshCar();
				} else
				{
					if ((arg0 == CarSelect.CMD_CIVILVAN))
					{
						if (player.car)
						{
							player.car.destroy();
						}
						colorSeed = (Math.random() * 12345);
						GameRef local2_4 = new GameRef(
							CarScene,
							cars.traffic.CIVILVAN:0x6r,
							((("0,0,0" + ",") + "0,0,0,") + colorSeed),
							"nightracer1"
						);
						player.car = new Vehicle(local2_4);
						CurrentCarName.changeText("Civil Van");
						RefreshCar();
						LastCommand = arg0;
					} else
					{
						if ((arg0 == CarSelect.CMD_COACH))
						{
							if (player.car)
							{
								player.car.destroy();
							}
							colorSeed = (Math.random() * 12345);
							GameRef local2_5 = new GameRef(
								CarScene,
								cars.traffic.Coach:0x6r,
								((("0,0,0" + ",") + "0,0,0,") + colorSeed),
								"nightracer1"
							);
							player.car = new Vehicle(local2_5);
							CurrentCarName.changeText("Coach");
							RefreshCar();
							LastCommand = arg0;
						} else
						{
							if ((arg0 == CarSelect.CMD_ERBILAC))
							{
								if (player.car)
								{
									player.car.destroy();
								}
								colorSeed = (Math.random() * 12345);
								GameRef local2_6 = new GameRef(
									CarScene,
									cars.traffic.Erbilac:0x6r,
									((("0,0,0" + ",") + "0,0,0,") + colorSeed),
									"nightracer1"
								);
								player.car = new Vehicle(local2_6);
								CurrentCarName.changeText("Erbilac");
								RefreshCar();
								LastCommand = arg0;
							} else
							{
								if ((arg0 == CarSelect.CMD_FIREENGINE))
								{
									if (player.car)
									{
										player.car.destroy();
									}
									colorSeed = (Math.random() * 12345);
									GameRef local2_7 = new GameRef(
										CarScene,
										cars.traffic.FireEngine:0x6r,
										((("0,0,0" + ",") + "0,0,0,") + colorSeed),
										"nightracer1"
									);
									player.car = new Vehicle(local2_7);
									CurrentCarName.changeText("Fire Engine");
									RefreshCar();
									LastCommand = arg0;
								} else
								{
									if ((arg0 == CarSelect.CMD_NINJA))
									{
										if (player.car)
										{
											player.car.destroy();
										}
										colorSeed = (Math.random() * 12345);
										GameRef local2_8 = new GameRef(
											CarScene,
											cars.fake_racers.Ninja:0x6r,
											((("0,0,0" + ",") + "0,0,0,") + colorSeed),
											"nightracer1"
										);
										player.car = new Vehicle(local2_8);
										CurrentCarName.changeText("Ninja");
										RefreshCar();
										LastCommand = arg0;
									} else
									{
										if ((arg0 == CarSelect.CMD_POLICE))
										{
											if (player.car)
											{
												player.car.destroy();
											}
											colorSeed = (Math.random() * 12345);
											GameRef local2_9 = new GameRef(
												CarScene,
												cars.misc.Police:0x6r,
												((("0,0,0" + ",") + "0,0,0,") + colorSeed),
												"nightracer1"
											);
											player.car = new Vehicle(local2_9);
											CurrentCarName.changeText("Police");
											RefreshCar();
											LastCommand = arg0;
										} else
										{
											if ((arg0 == CarSelect.CMD_RCTRUCK))
											{
												if (player.car)
												{
													player.car.destroy();
												}
												player.car = new Vehicle(
													CarScene,
													cars.Misc.RC_Truck:0x15r,
													1,
													1,
													1,
													1,
													1
												);
												CurrentCarName.changeText(player.car.chassis.vehicleName);
												player.car.command((("texture " + GameLogic.CARCOLORS[ColorIDX]) + " 1"));
												RefreshCar();
												LastCommand = arg0;
											} else
											{
												if ((arg0 == CarSelect.CMD_SCHOOLBUS))
												{
													if (player.car)
													{
														player.car.destroy();
													}
													colorSeed = (Math.random() * 12345);
													GameRef local2_10 = new GameRef(
														CarScene,
														cars.Traffic.Schoolbus:0x6r,
														((("0,0,0" + ",") + "0,0,0,") + colorSeed),
														"nightracer1"
													);
													player.car = new Vehicle(local2_10);
													CurrentCarName.changeText("School Bus");
													RefreshCar();
													LastCommand = arg0;
												} else
												{
													if ((arg0 == CarSelect.CMD_STALLION))
													{
														if (player.car)
														{
															player.car.destroy();
														}
														colorSeed = (Math.random() * 12345);
														GameRef local2_11 = new GameRef(
															CarScene,
															cars.fake_racers.Stallion:0x6r,
															((("0,0,0" + ",") + "0,0,0,") + colorSeed),
															"nightracer1"
														);
														player.car = new Vehicle(local2_11);
														CurrentCarName.changeText("Stallion");
														RefreshCar();
														LastCommand = arg0;
													} else
													{
														if ((arg0 == CarSelect.CMD_SUNSET))
														{
															if (player.car)
															{
																player.car.destroy();
															}
															colorSeed = (Math.random() * 12345);
															GameRef local2_12 = new GameRef(
																CarScene,
																cars.fake_racers.Sunset:0x6r,
																((("0,0,0" + ",") + "0,0,0,") + colorSeed),
																"nightracer1"
															);
															player.car = new Vehicle(local2_12);
															CurrentCarName.changeText("Sunset");
															RefreshCar();
															LastCommand = arg0;
														} else
														{
															if ((arg0 == CarSelect.CMD_TAXI))
															{
																if (player.car)
																{
																	player.car.destroy();
																}
																colorSeed = (Math.random() * 12345);
																GameRef local2_13 = new GameRef(
																	CarScene,
																	cars.traffic.Taxi:0x6r,
																	((("0,0,0" + ",") + "0,0,0,") + colorSeed),
																	"nightracer1"
																);
																player.car = new Vehicle(local2_13);
																CurrentCarName.changeText("Taxi");
																RefreshCar();
																LastCommand = arg0;
															} else
															{
																if ((arg0 == CarSelect.CMD_TEG))
																{
																	if (player.car)
																	{
																		player.car.destroy();
																	}
																	colorSeed = (Math.random() * 12345);
																	GameRef local2_14 = new GameRef(
																		CarScene,
																		cars.fake_racers.Teg:0x6r,
																		((("0,0,0" + ",") + "0,0,0,") + colorSeed),
																		"nightracer1"
																	);
																	player.car = new Vehicle(local2_14);
																	CurrentCarName.changeText("Teg");
																	RefreshCar();
																	LastCommand = arg0;
																} else
																{
																	if ((arg0 == CarSelect.CMD_WAGON))
																	{
																		if (player.car)
																		{
																			player.car.destroy();
																		}
																		colorSeed = (Math.random() * 12345);
																		GameRef local2_15 = new GameRef(
																			CarScene,
																			cars.traffic.Wagon:0x6r,
																			((("0,0,0" + ",") + "0,0,0,") + colorSeed),
																			"nightracer1"
																		);
																		player.car = new Vehicle(local2_15);
																		CurrentCarName.changeText("Wagon");
																		RefreshCar();
																		LastCommand = arg0;
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

	public void run()
	{
		while (true)
		{
			CamYpr = new Ypr(((-(0.01)) + (System.currentTime() * 0.5)), (-(0.15)), 0);
			CamVector = new Vector3(0.0, 1.5, 8.5);
			CamVector.rotate(CamYpr);
			CamYpr.p = (-(0.23));
			Cam.setMatrix(CamVector, CamYpr);
			CamThread.sleep(35);
		}
	}

	public void show()
	{
		super.show();
		osd.show();
	}

	public void hide()
	{
		osd.hide();
		super.hide();
	}

	public void changeGroup(int arg0)
	{
		if ((actGroup != arg0))
		{
			if ((actGroup >= 0))
			{
				osd.hideGroup(actGroup);
			}
			actGroup = arg0;
			if ((actGroup >= 0))
			{
				osd.showGroup(actGroup);
				osd.changeSelection2((-(1)), 0);
			}
		}
	}

}
