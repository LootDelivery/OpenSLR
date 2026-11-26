package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class MainMenu implements GameState
{
	MainMenuDialog mmd;

	public static final float Check(float arg0)
	{
		return (Math.sqrt(arg0) + 3.75);
	}

	public void enter(GameState arg0)
	{
		mmd = new MainMenuDialog();
		mmd.show();
	}

	public void exit(GameState arg0)
	{
		mmd.hide();
		mmd = null;
	}

}

public class MainMenuDialog extends OptionsDialog
{
	static final int CMD_NEW = 50;
	static final int CMD_EXIT = 51;
	static final int CMD_LOAD = 52;
	static final int CMD_BACKTOGARAGE = 53;
	static final int CMD_DELETE = 54;
	static final int CMD_QUICKRACE = 55;
	static final int CMD_CREDITS = 56;
	static final int CMD_CREDITS_DONE = 57;
	static final int CMD_COMPILEFILES = 58;
	static final int CMD_DEMO = 59;
	static final int CMD_LOADCAR = 60;
	static final int CMD_SAVECAR = 61;
	static final int CMD_FREERIDE = 62;
	static final int CMD_TRACKSELECT = 63;
	static final int CMD_MULTI = 633;
	static final int CMD_MENUCAR = 64;
	static final int CMD_CAROK = 65;
	static final int CMD_ABOUT = 66;
	static final int CMD_LIGHTS = 67;
	static final int CMD_MENUCAREXIT = 68;
	static final int CMD_RANDOMCAR = 69;
	static final int CMD_AMBULANCE = 70;
	static final int CMD_ARMOREDVAN = 71;
	static final int CMD_BADGE = 72;
	static final int CMD_CIVILVAN = 73;
	static final int CMD_COACH = 74;
	static final int CMD_ERBILAC = 75;
	static final int CMD_FIREENGINE = 76;
	static final int CMD_NINJA = 77;
	static final int CMD_POLICE = 78;
	static final int CMD_SCHOOLBUS = 79;
	static final int CMD_STALLION = 80;
	static final int CMD_SUNSET = 81;
	static final int CMD_TAXI = 82;
	static final int CMD_TEG = 83;
	static final int CMD_WAGON = 84;
	static final int CMD_MENUDRIVER = 85;
	static final int CMD_DRIVEROK = 86;
	static final int CMD_DRIVER0 = 87;
	static final int CMD_DRIVER1 = 88;
	static final int CMD_DRIVER2 = 89;
	static final int CMD_DRIVER3 = 90;
	static final int CMD_DRIVER4 = 91;
	static final int CMD_DRIVER5 = 92;
	static final int CMD_DRIVER6 = 93;
	static final int CMD_DRIVER7 = 94;
	static final int CMD_DRIVER8 = 95;
	static final int CMD_DRIVER9 = 96;
	static final int CMD_DRIVER10 = 97;
	static final int CMD_DRIVER11 = 98;
	static final int CMD_LANGUAGE = 99;
	// Implemented by slrr_host only (like the handbrake row in OptionsDialog).
	// The original exe never sees this declaration: the class is loaded from
	// the patch tree, ahead of the install.
	public static native void toggleLanguage();
	static final float CREDITS_SPACING0 = 0.1;
	static final float CREDITS_SPACING1 = 0.15;
	static final float CREDITS_TIMER = 0.2;
	static final float CREDITS_BOTTOM = 1.2;
	static final float CREDITS_TOP = (-(1.2));
	static final int bgVideoActive;
	static final int CarRotate;
	RenderRef DriverRender;
	RenderRef bgSun;
	RenderRef DSun;
	RenderRef CSun;
	RenderRef bgSky;
	GameRef bgScene;
	GameRef CarScene;
	GameRef DriverScene;
	Vehicle bgVehicle;
	Camera bgCam;
	Camera CarCam;
	Camera DriverCam;
	float bgCamAngle;
	float CarCamAngle;
	Pori bgCamPori;
	Pori bgCamSpeed;
	Pori bgCamSpeedDest;
	Pori[] bgCamPath;
	int bgCamPathIndex;
	float bgCamDestDistance;
	Text careerHeader;
	Button saveCarButton;
	Button backGarageButton;
	Button TakeItButton;
	Button SelectDriverButton;
	int creditsGroup;
	Text[] creditsTxt;
	float[] creditsPos;
	float creditsSpacing = 0.0;
	float creditsTime;
	int lastInLine;
	int lastOutLine;
	int creditLines;
	int CarGroup;
	int DriverGroup;

	public MainMenuDialog()
	{
		super(0);
	}

	public void show()
	{
		float local1 = (Math.random() * 100);
		if ((TrackSelect.Check(local1) != (Math.sqrt(local1) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		float local2 = (Math.random() * 100);
		if ((Garage.Check(local2) != (Math.sqrt(local2) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		float local3 = (Math.random() * 100);
		if ((Track.Check(local3) != (Math.sqrt(local3) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		GameLogic.klampiPatch = 0;
		Sound.changeMusicSet(Sound.MUSIC_SET_MENU);
		osd.createText("Miran Wichur Mod BETA", Frontend.smallFont, Text.ALIGN_RIGHT, 0.95000005, (-(1.0)));
		osd.createText(
			(((Config.version + " (build ") + System.buildNumber()) + ")"),
			Frontend.smallFont,
			Text.ALIGN_RIGHT,
			0.95000005,
			(-(0.95000005))
		);
		osd.endGroup();
		if ((MainMenuDialog.bgVideoActive = (GfxEngine.openVideo("data\\fmv\\prime.avi", 1, 1) == 0)))
		{
		} else
		{
			bgScene = new Dummy(GameRef.WORLDTREEROOT);
			bgCam = new Camera(bgScene, osd.getViewport(), 0);
			RenderRef local4 = new RenderRef(maps.skydome:0x24r);
			local4.setLight(14443620, 6579300, 13158600);
			bgSun = new RenderRef(bgScene, local4, "menu_bgSun");
			bgSky = new RenderRef();
			GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x35r));
			bgVehicle = new Vehicle(bgScene, cars.racers.prime:0x6r, 1, 1.0, 0.0, 1.0, 1.0);
			bgVehicle.command("reset");
			bgVehicle.command("setsteer -0.6");
			bgVehicle.setMatrix(new Vector3(0, 0, 0), new Ypr(0, 0, 0));
			bgCamPath = new Pori[28];
			bgCamPath[0] = new Pori(
				new Vector3((-(1.0860001)), 0.483, (-(2.086))),
				new Ypr((-(1.904)), (-(0.46400002)), 0.0)
			);
			bgCamPath[1] = new Pori(
				new Vector3((-(1.133)), 0.636, (-(1.248))),
				new Ypr((-(1.637)), (-(0.49)), 0.0)
			);
			bgCamPath[2] = new Pori(
				new Vector3((-(1.182)), 0.87, (-(0.476))),
				new Ypr((-(1.723)), (-(0.148)), 0.0)
			);
			bgCamPath[3] = new Pori(
				new Vector3((-(1.163)), 0.996, 0.391),
				new Ypr((-(1.3529999)), (-(0.26799998)), 0.0)
			);
			bgCamPath[4] = new Pori(
				new Vector3((-(1.141)), 0.60300005, 1.013),
				new Ypr((-(1.428)), 0.375, 0.0)
			);
			bgCamPath[5] = new Pori(
				new Vector3((-(1.133)), 0.269, 1.435),
				new Ypr((-(1.3249999)), 0.21900001, 0.0)
			);
			bgCamPath[6] = new Pori(new Vector3((-(1.217)), 0.273, 2.01), new Ypr((-(1.118)), 0.108, 0.0));
			bgCamPath[7] = new Pori(
				new Vector3((-(1.068)), 0.69, 2.4490001),
				new Ypr((-(0.92700005)), (-(0.20300001)), 0.0)
			);
			bgCamPath[8] = new Pori(
				new Vector3((-(0.573)), 0.875, 2.585),
				new Ypr((-(0.7)), (-(0.22399999)), 0.0)
			);
			bgCamPath[9] = new Pori(
				new Vector3((-(0.071)), 0.95800006, 2.58),
				new Ypr((-(0.409)), (-(0.26099998)), 0.0)
			);
			bgCamPath[10] = new Pori(
				new Vector3(0.35600004, 1.066, 2.4420002),
				new Ypr((-(0.050999995)), (-(0.303)), 0.0)
			);
			bgCamPath[11] = new Pori(new Vector3(0.508, 1.097, 1.8939999), new Ypr(0.226, (-(0.315)), 0.0));
			bgCamPath[12] = new Pori(
				new Vector3(0.77699995, 1.1389999, 1.532),
				new Ypr(0.552, (-(0.312)), 0.0)
			);
			bgCamPath[13] = new Pori(new Vector3(1.119, 1.132, 1.225), new Ypr(0.816, (-(0.254)), 0.0));
			bgCamPath[14] = new Pori(new Vector3(1.284, 1.0860001, 0.68700004), new Ypr(1.03, (-(0.254)), 0.0));
			bgCamPath[15] = new Pori(
				new Vector3(1.2980001, 1.053, 0.085999996),
				new Ypr(1.187, (-(0.30400002)), 0.0)
			);
			bgCamPath[16] = new Pori(
				new Vector3(1.2880001, 1.123, (-(0.505))),
				new Ypr(1.406, (-(0.48000002)), 0.0)
			);
			bgCamPath[17] = new Pori(new Vector3(1.251, 1.12, (-(1.03))), new Ypr(1.712, (-(0.65000004)), 0.0));
			bgCamPath[18] = new Pori(
				new Vector3(1.215, 1.0589999, (-(1.509))),
				new Ypr(2.1739998, (-(0.741)), 0.0)
			);
			bgCamPath[19] = new Pori(new Vector3(1.248, 0.938, (-(1.9840001))), new Ypr(2.458, (-(0.63)), 0.0));
			bgCamPath[20] = new Pori(
				new Vector3(1.215, 0.81, (-(2.3839998))),
				new Ypr(2.5839999, (-(0.474)), 0.0)
			);
			bgCamPath[21] = new Pori(
				new Vector3(0.962, 0.727, (-(2.636))),
				new Ypr(2.6539998, (-(0.343)), 0.0)
			);
			bgCamPath[22] = new Pori(new Vector3(0.611, 0.679, (-(2.785))), new Ypr(2.723, (-(0.235)), 0.0));
			bgCamPath[23] = new Pori(
				new Vector3(0.229, 0.683, (-(2.8539999))),
				new Ypr(2.873, (-(0.196)), 0.0)
			);
			bgCamPath[24] = new Pori(
				new Vector3((-(0.169)), 0.704, (-(2.836))),
				new Ypr(3.066, (-(0.197)), 0.0)
			);
			bgCamPath[25] = new Pori(
				new Vector3((-(0.56700003)), 0.742, (-(2.757))),
				new Ypr((-(2.9)), (-(0.2)), 0.0)
			);
			bgCamPath[26] = new Pori(
				new Vector3((-(0.899)), 0.704, (-(2.534))),
				new Ypr((-(2.531)), (-(0.246)), 0.0)
			);
			bgCamPath[27] = new Pori(
				new Vector3((-(1.0860001)), 0.483, (-(2.086))),
				new Ypr((-(1.904)), (-(0.46400002)), 0.0)
			);
		}
		super.show();
		changeMode(mainGroup);
	}

	public void hide()
	{
		super.hide();
		if (MainMenuDialog.bgVideoActive)
		{
			GfxEngine.closeVideo();
		} else
		{
			bgCam = null;
		}
	}

	public void addCustomGroups()
	{
		Menu local1;
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("MAIN MENU");
		local1 = osd.createMenu(butt2, (-(0.049999997)), (-(0.5)), 0);
		local1.addItem("NEW CAREER", MainMenuDialog.CMD_NEW);
		local1.addItem("LOAD CAREER", MainMenuDialog.CMD_LOAD);
		local1.addItem("DELETE CAREER", MainMenuDialog.CMD_DELETE);
		local1.addSeparator();
		local1.addSeparator();
		backGarageButton = local1.addItem("BACK TO GARAGE", MainMenuDialog.CMD_BACKTOGARAGE);
		local1.addSeparator();
		local1.addItem("LOAD CAR", MainMenuDialog.CMD_LOADCAR);
		saveCarButton = local1.addItem("SAVE CAR", MainMenuDialog.CMD_SAVECAR);
		local1.addItem("SELECT CAR", MainMenuDialog.CMD_MENUCAR);
		local1 = osd.createMenu(butt0, 0.049999997, (-(0.5)), 0);
		local1.addItem("QUICKRACE", MainMenuDialog.CMD_QUICKRACE);
		local1.addItem("SELECT TRACK", MainMenuDialog.CMD_TRACKSELECT);
		local1.addItem("DEMO", MainMenuDialog.CMD_DEMO);
		local1.addSeparator();
		local1.addSeparator();
		local1.addSeparator();
		local1.addSeparator();
		local1.addItem("OPTIONS", OptionsDialog.CMD_OPTIONS);
		local1.addSeparator();
		local1.addItem("LANGUAGE", MainMenuDialog.CMD_LANGUAGE);
		local1.addItem("CREDITS", MainMenuDialog.CMD_CREDITS);
		local1.addItem("EXIT GAME", MainMenuDialog.CMD_EXIT);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, MainMenuDialog.CMD_EXIT, this);
		osd.hideGroup((mainGroup = osd.endGroup()));
		osd.hideGroup((creditsGroup = osd.beginGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("CREDITS");
		local1 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local1.addItem("BACK", MainMenuDialog.CMD_CREDITS_DONE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, MainMenuDialog.CMD_CREDITS_DONE, this);
		buildCredits();
		osd.endGroup();
		osd.hideGroup((CarGroup = osd.beginGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("SELECT CAR");
		local1 = osd.createMenu(butt0, (-(0.98)), (-(0.45)), 0);
		local1.addItem("Ambulance", MainMenuDialog.CMD_AMBULANCE);
		local1.addItem("Armored Van", MainMenuDialog.CMD_ARMOREDVAN);
		local1.addItem("Badge", MainMenuDialog.CMD_BADGE);
		local1.addItem("Civil Van", MainMenuDialog.CMD_CIVILVAN);
		local1.addItem("Coach", MainMenuDialog.CMD_COACH);
		local1.addItem("Erbilac", MainMenuDialog.CMD_ERBILAC);
		local1.addItem("Fire Engine", MainMenuDialog.CMD_FIREENGINE);
		local1.addItem("Ninja", MainMenuDialog.CMD_NINJA);
		local1 = osd.createMenu(butt2, 0.98, (-(0.45)), 0);
		local1.addItem("Police", MainMenuDialog.CMD_POLICE);
		local1.addItem("School Bus", MainMenuDialog.CMD_SCHOOLBUS);
		local1.addItem("Stallion", MainMenuDialog.CMD_STALLION);
		local1.addItem("Sunset", MainMenuDialog.CMD_SUNSET);
		local1.addItem("Taxi", MainMenuDialog.CMD_TAXI);
		local1.addItem("Teg", MainMenuDialog.CMD_TEG);
		local1.addItem("Wagon", MainMenuDialog.CMD_WAGON);
		local1.addItem("RANDOM CAR", MainMenuDialog.CMD_RANDOMCAR);
		local1 = osd.createMenu(butt0, (-(0.98)), 0.85, 0);
		local1.addItem("BACK", MainMenuDialog.CMD_MENUCAREXIT);
		local1 = osd.createMenu(butt1, 0.0, 0.85, 0);
		local1.addItem("LIGHTS ON/OFF", MainMenuDialog.CMD_LIGHTS);
		local1 = osd.createMenu(butt1, 0.0, (-(0.83)), 0);
		SelectDriverButton = local1.addItem("SELECT DRIVER", MainMenuDialog.CMD_MENUDRIVER);
		local1 = osd.createMenu(butt2, 0.98, (-(0.83)), 0);
		local1.addItem("ABOUT", MainMenuDialog.CMD_ABOUT);
		local1 = osd.createMenu(butt2, 0.98, 0.85, 0);
		TakeItButton = local1.addItem("TAKE IT", MainMenuDialog.CMD_CAROK);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, MainMenuDialog.CMD_MENUCAREXIT, this);
		osd.endGroup();
		osd.hideGroup((DriverGroup = osd.beginGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("SELECT DRIVER");
		local1 = osd.createMenu(butt2, 0.98, (-(0.83)), 0);
		local1.addItem("ABOUT", MainMenuDialog.CMD_ABOUT);
		local1 = osd.createMenu(butt0, (-(0.98)), (-(0.65000004)), 0);
		local1.addItem("NO DRIVER", MainMenuDialog.CMD_DRIVER0);
		local1.addItem("Jessy James", MainMenuDialog.CMD_DRIVER5, "From Monster Garage");
		local1.addItem("Mitch Whole", MainMenuDialog.CMD_DRIVER1, "Default Driver");
		local1.addItem("Opponent 1", MainMenuDialog.CMD_DRIVER2);
		local1.addItem("Opponent 2", MainMenuDialog.CMD_DRIVER3);
		local1.addItem("Policeman", MainMenuDialog.CMD_DRIVER4);
		local1.addItem("Proffesional racer", MainMenuDialog.CMD_DRIVER6, "By GameRED");
		local1.addItem("Tommy Vercetti", MainMenuDialog.CMD_DRIVER7, "By Miran");
		local1.addItem("Common Guy", MainMenuDialog.CMD_DRIVER8, "By Gruchowski");
		local1.addItem("Elegant racer", MainMenuDialog.CMD_DRIVER9, "By Lokerlol");
		local1.addItem("Rambo Ziom", MainMenuDialog.CMD_DRIVER10, "By Lokerlol");
		local1.addItem("Idler style", MainMenuDialog.CMD_DRIVER11, "By Lokerlol");
		local1 = osd.createMenu(butt2, 0.98, 0.85, 0);
		local1.addItem("OK", MainMenuDialog.CMD_DRIVEROK);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, MainMenuDialog.CMD_DRIVEROK, this);
		osd.endGroup();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 < 0))
		{
			return;
		} else
		{
			if ((arg0 == MainMenuDialog.CMD_AMBULANCE))
			{
				player.car.destroy();
				TakeItButton.enable();
				SelectDriverButton.disable();
				player.driverID = system:0x0r;
				player.car = new Vehicle(CarScene, cars.traffic.Ambulance:0x6r, 1, 1.0, 0.0, 1.0, 1.0);
				player.car.command("idle");
				player.car.command("setsteer -0.6");
			} else
			{
				if ((arg0 == MainMenuDialog.CMD_ARMOREDVAN))
				{
					player.car.destroy();
					TakeItButton.enable();
					SelectDriverButton.disable();
					player.driverID = system:0x0r;
					player.car.destroy();
					player.car = new Vehicle(CarScene, cars.traffic.ArmoredVan:0x6r, 6, 2.0, 0.0, 1.0, 1.0);
					player.car.command("idle");
					player.car.command("setsteer -0.6");
				} else
				{
					if ((arg0 == MainMenuDialog.CMD_BADGE))
					{
						player.car.destroy();
						TakeItButton.enable();
						SelectDriverButton.disable();
						player.driverID = system:0x0r;
						player.car = new Vehicle(CarScene, cars.fake_racers.Badge:0x6r, 0, 0, 0, 0, 0.0);
						player.car.command("idle");
						player.car.command("setsteer -0.6");
					} else
					{
						if ((arg0 == MainMenuDialog.CMD_CIVILVAN))
						{
							player.car.destroy();
							TakeItButton.enable();
							SelectDriverButton.disable();
							player.driverID = system:0x0r;
							player.car = new Vehicle(
								CarScene,
								cars.traffic.CIVILVAN:0x6r,
								6,
								2.0,
								0.0,
								1.0,
								1.0
							);
							player.car.command("idle");
							player.car.command("setsteer -0.6");
						} else
						{
							if ((arg0 == MainMenuDialog.CMD_COACH))
							{
								player.car.destroy();
								TakeItButton.enable();
								SelectDriverButton.disable();
								player.driverID = system:0x0r;
								player.car = new Vehicle(
									CarScene,
									cars.traffic.Coach:0x6r,
									6,
									2.0,
									0.0,
									1.0,
									1.0
								);
								player.car.command("idle");
								player.car.command("setsteer -0.6");
							} else
							{
								if ((arg0 == MainMenuDialog.CMD_ERBILAC))
								{
									player.car.destroy();
									TakeItButton.enable();
									SelectDriverButton.disable();
									player.driverID = system:0x0r;
									player.car = new Vehicle(
										CarScene,
										cars.traffic.Erbilac:0x6r,
										6,
										2.0,
										0.0,
										1.0,
										1.0
									);
									player.car.command("idle");
									player.car.command("setsteer -0.6");
								} else
								{
									if ((arg0 == MainMenuDialog.CMD_FIREENGINE))
									{
										player.car.destroy();
										TakeItButton.enable();
										SelectDriverButton.disable();
										player.driverID = system:0x0r;
										player.car = new Vehicle(
											CarScene,
											cars.traffic.FireEngine:0x6r,
											6,
											2.0,
											0.0,
											1.0,
											1.0
										);
										player.car.command("idle");
										player.car.command("setsteer -0.6");
									} else
									{
										if ((arg0 == MainMenuDialog.CMD_NINJA))
										{
											player.car.destroy();
											TakeItButton.enable();
											SelectDriverButton.disable();
											player.driverID = system:0x0r;
											player.car = new Vehicle(
												CarScene,
												cars.fake_racers.Ninja:0x6r,
												6,
												2.5,
												0.0,
												1.0,
												1.0
											);
											player.car.command("idle");
											player.car.command("setsteer -0.6");
										} else
										{
											if ((arg0 == MainMenuDialog.CMD_POLICE))
											{
												player.car.destroy();
												TakeItButton.enable();
												SelectDriverButton.enable();
												player.car = new Vehicle(
													CarScene,
													cars.Misc.Police:0x6r,
													0,
													0.0,
													0.0,
													0.0,
													0.0
												);
												player.car.command("idle");
												player.car.command("setsteer -0.6");
												player.driverID = humans:0xcr;
											} else
											{
												if ((arg0 == MainMenuDialog.CMD_SCHOOLBUS))
												{
													player.car.destroy();
													TakeItButton.enable();
													SelectDriverButton.disable();
													player.driverID = system:0x0r;
													player.car = new Vehicle(
														CarScene,
														cars.Traffic.Schoolbus:0x6r,
														6,
														2.0,
														0.0,
														1.0,
														1.0
													);
													player.car.command("idle");
													player.car.command("setsteer -0.6");
												} else
												{
													if ((arg0 == MainMenuDialog.CMD_STALLION))
													{
														player.car.destroy();
														TakeItButton.enable();
														SelectDriverButton.disable();
														player.driverID = system:0x0r;
														player.car = new Vehicle(
															CarScene,
															cars.fake_racers.Stallion:0x6r,
															6,
															2.5,
															0.0,
															1.0,
															1.0
														);
														player.car.command("idle");
														player.car.command("setsteer -0.6");
													} else
													{
														if ((arg0 == MainMenuDialog.CMD_SUNSET))
														{
															player.car.destroy();
															TakeItButton.enable();
															SelectDriverButton.disable();
															player.driverID = system:0x0r;
															player.car = new Vehicle(
																CarScene,
																cars.fake_racers.Sunset:0x6r,
																6,
																2.5,
																0.0,
																1.0,
																1.0
															);
															player.car.command("idle");
															player.car.command("setsteer -0.6");
														} else
														{
															if ((arg0 == MainMenuDialog.CMD_TAXI))
															{
																player.car.destroy();
																TakeItButton.enable();
																SelectDriverButton.disable();
																player.driverID = system:0x0r;
																player.car = new Vehicle(
																	CarScene,
																	cars.traffic.Taxi:0x6r,
																	6,
																	2.0,
																	0.0,
																	1.0,
																	1.0
																);
																player.car.command("idle");
																player.car.command("setsteer -0.6");
															} else
															{
																if ((arg0 == MainMenuDialog.CMD_TEG))
																{
																	player.car.destroy();
																	TakeItButton.enable();
																	SelectDriverButton.disable();
																	player.driverID = system:0x0r;
																	player.car = new Vehicle(
																		CarScene,
																		cars.fake_racers.Teg:0x6r,
																		6,
																		2.5,
																		0.0,
																		1.0,
																		1.0
																	);
																	player.car.command("idle");
																	player.car.command("setsteer -0.6");
																} else
																{
																	if ((arg0 == MainMenuDialog.CMD_WAGON))
																	{
																		player.car.destroy();
																		TakeItButton.enable();
																		SelectDriverButton.disable();
																		player.driverID = system:0x0r;
																		player.car = new Vehicle(
																			CarScene,
																			cars.traffic.Wagon:0x6r,
																			6,
																			2.0,
																			0.0,
																			1.0,
																			1.0
																		);
																		player.car.command("idle");
																		player.car.command("setsteer -0.6");
																	} else
																	{
																		if ((arg0 == MainMenuDialog.CMD_RANDOMCAR))
																		{
																			player.car.destroy();
																			TakeItButton.enable();
																			SelectDriverButton.enable();
																			VehicleDescriptor local2 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
																			player.car = new Vehicle(
																				CarScene,
																				local2.id,
																				local2.colorIndex,
																				local2.optical,
																				local2.power,
																				local2.wear,
																				local2.tear
																			);
																			player.car.command("idle");
																			player.car.command("setsteer -0.6");
																			player.driverID = humans:0xcr;
																		} else
																		{
																			if ((arg0 == MainMenuDialog.CMD_CAROK))
																			{
																				Frontend.loadingScreen.show();
																				GameLogic.gameMode = GameLogic.GM_SINGLECAR;
																				GameLogic.setTime((((Math.random() * 14) + 4) * 3600));
																				player.money = 0;
																				GameLogic.changeActiveSection(GameLogic.garage);
																				CarScene.destroy();
																			} else
																			{
																				if ((arg0 == MainMenuDialog.CMD_TRACKSELECT))
																				{
																					if (GameLogic.autoSave())
																					{
																						GameLogic.loadDefaults();
																						GameLogic.gameMode = GameLogic.GM_FREERIDE;
																						GameLogic.setTime((((Math.random() * 14) + 4) * 3600));
																						if ((!(player.car)))
																						{
																							VehicleDescriptor local2_2 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
																							player.car = new Vehicle(
																								player,
																								local2_2.id,
																								local2_2.colorIndex,
																								local2_2.optical,
																								local2_2.power,
																								local2_2.wear,
																								local2_2.tear
																							);
																						}
																						GameLogic.changeActiveSection(new TrackSelect());
																					}
																				} else
																				{
																					if ((arg0 == MainMenuDialog.CMD_MULTI))
																					{
																						GameLogic.changeActiveSection(new MultiPlayer());
																					} else
																					{
																						if ((arg0 == MainMenuDialog.CMD_LIGHTS))
																						{
																							player.car.command("reset");
																							GameLogic.spendTime((12 * 3600));
																							player.car.command("idle");
																						} else
																						{
																							if ((arg0 == MainMenuDialog.CMD_ABOUT))
																							{
																								new WarningDialog(
																									GameLogic.player.controller,
																									((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
																									"ABOUT",
																									"It's ALPCHA version! \n \n By Miran").display(
																								);
																							} else
																							{
																								if ((arg0 == MainMenuDialog.CMD_MENUCAR))
																								{
																									GameLogic.changeActiveSection(new CarSelect());
																								} else
																								{
																									if ((arg0 == MainMenuDialog.CMD_DRIVEROK))
																									{
																										changeMode(CarGroup);
																										player.car.setMatrix(
																											new Vector3(0, 0, 0),
																											new Ypr(0, 0, 0)
																										);
																										player.car.command("reset");
																										player.car.command("idle");
																										player.car.command("setsteer -0.6");
																										DriverScene.destroy();
																									} else
																									{
																										if ((arg0 == MainMenuDialog.CMD_DRIVER0))
																										{
																											DriverRender.destroy();
																											DriverRender = new RenderRef(
																												DriverScene,
																												system:0x0r,
																												"No Driver"
																											);
																											player.driverID = system:0x0r;
																										} else
																										{
																											if ((arg0 == MainMenuDialog.CMD_DRIVER1))
																											{
																												DriverRender.destroy();
																												DriverRender = new RenderRef(
																													DriverScene,
																													humans:0xcr,
																													"Default driver"
																												);
																												player.driverID = humans:0xcr;
																											} else
																											{
																												if ((arg0 == MainMenuDialog.CMD_DRIVER2))
																												{
																													DriverRender.destroy();
																													DriverRender = new RenderRef(
																														DriverScene,
																														humans:0x7r,
																														"Opponent 1"
																													);
																													player.driverID = humans:0x7r;
																												} else
																												{
																													if ((arg0 == MainMenuDialog.CMD_DRIVER3))
																													{
																														DriverRender.destroy();
																														DriverRender = new RenderRef(
																															DriverScene,
																															humans:0x8r,
																															"Opponent 2"
																														);
																														player.driverID = humans:0x8r;
																													} else
																													{
																														if ((arg0 == MainMenuDialog.CMD_DRIVER4))
																														{
																															DriverRender.destroy();
																															DriverRender = new RenderRef(
																																DriverScene,
																																humans:0x18r,
																																"Policeman"
																															);
																															player.driverID = humans:0x18r;
																														} else
																														{
																															if ((arg0 == MainMenuDialog.CMD_DRIVER5))
																															{
																																DriverRender.destroy();
																																DriverRender = new RenderRef(
																																	DriverScene,
																																	MW_Mod:0x22r,
																																	"MG Jessy James"
																																);
																																player.driverID = MW_Mod:0x22r;
																															} else
																															{
																																if ((arg0 == MainMenuDialog.CMD_DRIVER6))
																																{
																																	DriverRender.destroy();
																																	DriverRender = new RenderRef(
																																		DriverScene,
																																		MW_Mod:0x59r,
																																		"Proffesional racer"
																																	);
																																	player.driverID = MW_Mod:0x59r;
																																} else
																																{
																																	if ((arg0 == MainMenuDialog.CMD_DRIVER7))
																																	{
																																		DriverRender.destroy();
																																		DriverRender = new RenderRef(
																																			DriverScene,
																																			MW_Mod:0x57r,
																																			"Tommy Vercetti"
																																		);
																																		player.driverID = MW_Mod:0x57r;
																																	} else
																																	{
																																		if ((arg0 == MainMenuDialog.CMD_DRIVER8))
																																		{
																																			DriverRender.destroy();
																																			DriverRender = new RenderRef(
																																				DriverScene,
																																				MW_Mod:0x61r,
																																				"Common Guy"
																																			);
																																			player.driverID = MW_Mod:0x61r;
																																		} else
																																		{
																																			if ((arg0 == MainMenuDialog.CMD_DRIVER9))
																																			{
																																				DriverRender.destroy();
																																				DriverRender = new RenderRef(
																																					DriverScene,
																																					MW_Mod:0x63r,
																																					"Elegant"
																																				);
																																				player.driverID = MW_Mod:0x63r;
																																			} else
																																			{
																																				if ((arg0 == MainMenuDialog.CMD_DRIVER10))
																																				{
																																					DriverRender.destroy();
																																					DriverRender = new RenderRef(
																																						DriverScene,
																																						MW_Mod:0x65r,
																																						"Rambo"
																																					);
																																					player.driverID = MW_Mod:0x65r;
																																				} else
																																				{
																																					if ((arg0 == MainMenuDialog.CMD_DRIVER11))
																																					{
																																						DriverRender.destroy();
																																						DriverRender = new RenderRef(
																																							DriverScene,
																																							MW_Mod:0x67r,
																																							"Idler"
																																						);
																																						player.driverID = MW_Mod:0x67r;
																																					} else
																																					{
																																						if ((arg0 == MainMenuDialog.CMD_LOADCAR))
																																						{
																																							if (GameLogic.autoSave())
																																							{
																																								VehicleFileReqDialog local2_3 = new VehicleFileReqDialog(
																																									player.controller,
																																									(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																									"LOAD CAR",
																																									"LOAD CAR",
																																									GameLogic.carSaveDir,
																																									"*"
																																								);
																																								if ((local2_3.display() == 0))
																																								{
																																									String local3 = (GameLogic.carSaveDir + local2_3.fileName);
																																									Frontend.loadingScreen.show();
																																									GameLogic.loadDefaults();
																																									GameLogic.gameMode = GameLogic.GM_SINGLECAR;
																																									player.car = Vehicle.load(
																																										local3,
																																										player
																																									);
																																									player.money = 1000000;
																																									GameLogic.changeActiveSection(GameLogic.garage);
																																								}
																																							}
																																						} else
																																						{
																																							if ((arg0 == MainMenuDialog.CMD_SAVECAR))
																																							{
																																								VehicleFileReqDialog local2_4 = new VehicleFileReqDialog(
																																									player.controller,
																																									((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.FRF_SAVE),
																																									"SAVE CAR",
																																									"SAVE CAR",
																																									GameLogic.carSaveDir,
																																									"*"
																																								);
																																								if ((local2_4.display() == 0))
																																								{
																																									String local3_2 = (GameLogic.carSaveDir + local2_4.fileName);
																																									if (File.exists(local3_2))
																																									{
																																										File.delete(local3_2);
																																										File.delete((local3_2 + ".*"));
																																									}
																																									player.car.save(local3_2);
																																									new WarningDialog(
																																										player.controller,
																																										(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																										"SAVE CAR",
																																										"Your current car has been saved.").display(
																																									);
																																								}
																																							} else
																																							{
																																								if ((arg0 == MainMenuDialog.CMD_QUICKRACE))
																																								{
																																									if (GameLogic.autoSave())
																																									{
																																										Frontend.loadingScreen.show();
																																										GameLogic.loadDefaults();
																																										GameLogic.gameMode = GameLogic.GM_QUICKRACE;
																																										GameLogic.setTime(((21 + (Math.random() * 8)) * 3600));
																																										if ((!(player.car)))
																																										{
																																											VehicleDescriptor local2_5 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
																																											player.car = new Vehicle(
																																												player,
																																												local2_5.id,
																																												local2_5.colorIndex,
																																												local2_5.optical,
																																												local2_5.power,
																																												local2_5.wear,
																																												local2_5.tear
																																											);
																																										}
																																										GameLogic.changeActiveSection(new Valocity());
																																									}
																																								} else
																																								{
																																									if ((arg0 == MainMenuDialog.CMD_FREERIDE))
																																									{
																																										if (GameLogic.autoSave())
																																										{
																																											Frontend.loadingScreen.show();
																																											GameLogic.loadDefaults();
																																											GameLogic.gameMode = GameLogic.GM_FREERIDE;
																																											GameLogic.setTime((((Math.random() * 14) + 4) * 3600));
																																											if ((!(player.car)))
																																											{
																																												VehicleDescriptor local2_6 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
																																												player.car = new Vehicle(
																																													player,
																																													local2_6.id,
																																													local2_6.colorIndex,
																																													local2_6.optical,
																																													local2_6.power,
																																													local2_6.wear,
																																													local2_6.tear
																																												);
																																											}
																																											GameLogic.changeActiveSection(new Valocity());
																																										}
																																									} else
																																									{
																																										if ((arg0 == MainMenuDialog.CMD_DEMO))
																																										{
																																											if (GameLogic.autoSave())
																																											{
																																												Frontend.loadingScreen.show();
																																												GameLogic.loadDefaults();
																																												GameLogic.timeout = (60 * 10);
																																												GameLogic.gameMode = GameLogic.GM_DEMO;
																																												GameLogic.setTime(((Math.random() * 24) * 3600));
																																												GameLogic.changeActiveSection(new Valocity());
																																											}
																																										} else
																																										{
																																											if ((arg0 == MainMenuDialog.CMD_COMPILEFILES))
																																											{
																																												System.compileAll(".");
																																												new SfxRef(Frontend.SFX_WARNING).play();
																																											} else
																																											{
																																												if ((arg0 == MainMenuDialog.CMD_NEW))
																																												{
																																													if (GameLogic.autoSave())
																																													{
																																														StringRequesterDialog local2_7 = new StringRequesterDialog(
																																															player.controller,
																																															((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.SIF_NOEMPTY),
																																															"Enter player name",
																																															""
																																														);
																																														if ((local2_7.display() == 0))
																																														{
																																															player.name = local2_7.input;
																																															new PlayerSetupDialog(
																																																player.controller,
																																																(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																																"Choose character",
																																																player).display(
																																															);
																																															Frontend.loadingScreen.show();
																																															GameLogic.loadDefaults();
																																															GameLogic.carrerInProgress = 1;
																																															GameLogic.autoSaveQuiet();
																																															GameLogic.changeActiveSection(GameLogic.garage);
																																														}
																																													}
																																												} else
																																												{
																																													if ((arg0 == MainMenuDialog.CMD_LOAD))
																																													{
																																														CarrerFileReqDialog local2_8 = new CarrerFileReqDialog(
																																															player.controller,
																																															(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																															"LOAD CAREER",
																																															"LOAD CAREER",
																																															GameLogic.carrerSaveDir,
																																															"*",
																																															FindFile.DIRS_ONLY
																																														);
																																														if ((local2_8.display() == 0))
																																														{
																																															String local3_3 = (GameLogic.carrerSaveDir + local2_8.fileName);
																																															int local4 = 0;
																																															if (local4)
																																															{
																																																new WarningDialog(
																																																	player.controller,
																																																	(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																																	"Load career",
																																																	"This is your current career file, you cannot reload it.").display(
																																																);
																																															} else
																																															{
																																																if (GameLogic.autoSave())
																																																{
																																																	Frontend.loadingScreen.show();
																																																	int local5 = GameLogic.load((local3_3 + "/"));
																																																	if ((!(local5)))
																																																	{
																																																		GameLogic.carrerInProgress = 1;
																																																		GameLogic.changeActiveSection(GameLogic.garage);
																																																	} else
																																																	{
																																																		new SfxRef(Frontend.SFX_WARNING).play();
																																																		Frontend.loadingScreen.hide();
																																																		new WarningDialog(
																																																			player.controller,
																																																			(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																																			"WARNING",
																																																			"Corupt savegame file. \n Loading terminated.").display(
																																																		);
																																																		GameLogic.erase();
																																																	}
																																																}
																																															}
																																														}
																																													} else
																																													{
																																														if ((arg0 == MainMenuDialog.CMD_DELETE))
																																														{
																																															CarrerFileReqDialog local2_9 = new CarrerFileReqDialog(
																																																player.controller,
																																																(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
																																																"DELETE CAREER",
																																																"DELETE CAREER",
																																																GameLogic.carrerSaveDir,
																																																"*",
																																																FindFile.DIRS_ONLY
																																															);
																																															if ((local2_9.display() == 0))
																																															{
																																																String local3_4 = (GameLogic.carrerSaveDir + local2_9.fileName);
																																																int local4_2 = 0;
																																																int local5_2;
																																																if (local4_2)
																																																{
																																																	local5_2 = (0 == new NoYesDialog(player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "Delete career", "Do you really want to delete your ongoing career?").display());
																																																} else
																																																{
																																																	local5_2 = (0 == new NoYesDialog(player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "Delete career", "Do you really want to continue?").display());
																																																}
																																																if (local5_2)
																																																{
																																																	if (local4_2)
																																																	{
																																																		GameLogic.erase();
																																																		backGarageButton.disable();
																																																	}
																																																	File.delete(
																																																		(local3_4 + "/"),
																																																		"*"
																																																	);
																																																	File.delete(local3_4);
																																																}
																																															}
																																														} else
																																														{
																																															if ((arg0 == MainMenuDialog.CMD_BACKTOGARAGE))
																																															{
																																																GameLogic.changeActiveSection(GameLogic.garage);
																																															} else
																																															{
																																																if ((arg0 == MainMenuDialog.CMD_EXIT))
																																																{
																																																	int local2_10;
																																																	int local3_5 = ((GameLogic.gameMode == GameLogic.GM_CARREER) && GameLogic.carrerInProgress);
																																																	if (local3_5)
																																																	{
																																																		local2_10 = (0 == new NoYesDialog(player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "EXIT", "Do you really want to Exit? \n \n Note that your current career will be automatically saved.").display());
																																																	} else
																																																	{
																																																		local2_10 = (0 == new NoYesDialog(player.controller, (Dialog.DF_MODAL | Dialog.DF_DEFAULTBG), "EXIT", "Do you really want to Exit?").display());
																																																	}
																																																	if (local2_10)
																																																	{
																																																		if (local3_5)
																																																		{
																																																			GameLogic.autoSaveQuiet();
																																																		}
																																																		GameLogic.changeActiveSection(null);
																																																	}
																																																} else
																																																{
																																																	if ((arg0 == MainMenuDialog.CMD_CREDITS))
																																																	{
																																																		Vector3 local2_11 = new Vector3(
																																																			0.0,
																																																			1.0,
																																																			0.0
																																																		);
																																																		int local3_6 = 0;
																																																		while ((local3_6 < creditLines))
																																																		{
																																																			creditsTxt[local3_6].renderinst.setMatrix(
																																																				local2_11,
																																																				null
																																																			);
																																																			creditsPos[local3_6] = 0.0;
																																																			local3_6++;
																																																		}
																																																		creditsPos[0] = MainMenuDialog.CREDITS_BOTTOM;
																																																		lastInLine = 0;
																																																		lastOutLine = 0;
																																																		creditsSpacing = 0.0;
																																																		changeMode(creditsGroup);
																																																		creditsTime = System.currentTime();
																																																	} else
																																																	{
																																																		if ((arg0 == MainMenuDialog.CMD_CREDITS_DONE))
																																																		{
																																																			changeMode(mainGroup);
																																																		} else
																																																		{
																																																			if ((arg0 == MainMenuDialog.CMD_LANGUAGE))
																											{
																												MainMenuDialog.toggleLanguage();
																											} else
																											{
																												super.osdCommand(arg0);
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

	public void changeMode(int arg0)
	{
		super.changeMode(arg0);
		if ((actGroup == mainGroup))
		{
			if ((((GameLogic.gameMode == GameLogic.GM_CARREER) || (GameLogic.gameMode == GameLogic.GM_SINGLECAR)) && GameLogic.player.car))
			{
				saveCarButton.enable();
			} else
			{
				saveCarButton.disable();
			}
			if (GameLogic.played)
			{
				backGarageButton.enable();
			} else
			{
				backGarageButton.disable();
			}
		}
	}

	public void animate()
	{
		if ((actGroup == CarGroup))
		{
			Ypr local1 = new Ypr(((-(0.01)) + (System.currentTime() * 0.5)), (-(0.15)), 0);
			Vector3 local2 = new Vector3(0.0, 1.2, 8.5);
			local2.rotate(local1);
			local1.p = (-(0.23));
			CarCam.setMatrix(local2, local1);
		} else
		{
			if ((!(MainMenuDialog.bgVideoActive)))
			{
				float local1_2;
				if (bgCamPori)
				{
					local1_2 = new Vector3().diff(bgCamPath[bgCamPathIndex].pos, bgCamPori.pos).length();
				} else
				{
					bgCamPori = new Pori(bgCamPath[0]);
					bgCamSpeed = new Pori();
				}
				if ((bgCamDestDistance <= local1_2))
				{
					bgCamPathIndex = (++bgCamPathIndex % bgCamPath.length);
					bgCamSpeedDest = new Pori().diff(bgCamPath[bgCamPathIndex], bgCamPori).mul(0.0009999999);
					bgCamDestDistance = 1000000.0;
				} else
				{
					bgCamDestDistance = local1_2;
				}
				bgCamSpeed = bgCamSpeed.mul(0.97).add(new Pori(bgCamSpeedDest).mul(0.03));
				bgCamPori.add(bgCamSpeed);
				bgCam.setMatrix(bgCamPori);
			}
		}
		if ((actGroup == creditsGroup))
		{
			int local1_3;
			int local2_2;
			float local3;
			float local4;
			Vector3 local5 = new Vector3(0.0, 0.0, 0.0);
			String local6;
			local4 = System.currentTime();
			local3 = ((local4 - creditsTime) * MainMenuDialog.CREDITS_TIMER);
			creditsTime = local4;
			if ((creditsPos[lastInLine] <= (MainMenuDialog.CREDITS_BOTTOM - creditsSpacing)))
			{
				if ((++lastInLine >= creditLines))
				{
					lastInLine = 0;
				}
				creditsPos[lastInLine] = MainMenuDialog.CREDITS_BOTTOM;
				local5.y = creditsPos[lastInLine];
				creditsSpacing = MainMenuDialog.CREDITS_SPACING0;
				creditsTxt[lastInLine].renderinst.setMatrix(local5, null);
			}
			local1_3 = lastOutLine;
			int local7;
			int local8 = lastInLine;
			if ((local8 < local1_3))
			{
				local8 += creditLines;
			}
			while ((local1_3 <= local8))
			{
				local7 = (local1_3 % creditLines);
				creditsPos[local7] -= local3;
				local5.y = creditsPos[local7];
				creditsTxt[local7].renderinst.setMatrix(local5, null);
				if ((local5.y < MainMenuDialog.CREDITS_TOP))
				{
					lastOutLine = local7;
				}
				local1_3++;
			}
		}
		super.animate();
	}

	public void buildCredits()
	{
		creditsTxt = new Text[167];
		creditsPos = new float[167];
		creditLines = 0;
		addCreditLine(1, "ACTIVISION VALUE CREDITS");
		addCreditLine(1, "");
		addCreditLine(0, "");
		addCreditLine(0, "General Manager ");
		addCreditLine(1, "Dave Oxford");
		addCreditLine(0, "");
		addCreditLine(0, "Senior Vice President");
		addCreditLine(1, "Chad Koehler");
		addCreditLine(0, "");
		addCreditLine(0, "Vice President of Studios");
		addCreditLine(1, "Patrick Kelly");
		addCreditLine(0, "");
		addCreditLine(0, "Vice President of Marketing and Creative Services");
		addCreditLine(1, "Mark Meadows");
		addCreditLine(0, "");
		addCreditLine(0, "Vice President of Sales");
		addCreditLine(1, "Tim Flaherty");
		addCreditLine(0, "");
		addCreditLine(0, "Senior Counsel");
		addCreditLine(1, "Joe Hedges");
		addCreditLine(0, "");
		addCreditLine(0, "Producer");
		addCreditLine(1, "Sean Dunnigan");
		addCreditLine(0, "");
		addCreditLine(0, "Assistant Producer");
		addCreditLine(1, "Jason Lembcke");
		addCreditLine(0, "");
		addCreditLine(0, "Manager of Quality Assurance");
		addCreditLine(1, "Chris Arends");
		addCreditLine(0, "");
		addCreditLine(0, "QA Lead");
		addCreditLine(1, "Chad Schilling");
		addCreditLine(0, "");
		addCreditLine(0, "QA Team");
		addCreditLine(1, "Bob Viau");
		addCreditLine(1, "Isham Ashour");
		addCreditLine(1, "Travis Clarke");
		addCreditLine(1, "Bill Hart");
		addCreditLine(1, "Donna Johnston");
		addCreditLine(1, "Ryan Lee");
		addCreditLine(1, "Kris Young");
		addCreditLine(0, "");
		addCreditLine(0, "Brand and Licensing Manager");
		addCreditLine(1, "Andy Koehler");
		addCreditLine(0, "");
		addCreditLine(0, "Trade Marketing Manager");
		addCreditLine(1, "Robbin Livernois");
		addCreditLine(0, "");
		addCreditLine(0, "Senior Graphic Artist");
		addCreditLine(1, "Trevor Harveaux");
		addCreditLine(0, "");
		addCreditLine(0, "Graphic Artist");
		addCreditLine(1, "Sean James");
		addCreditLine(0, "");
		addCreditLine(0, "Video Production Coordinator");
		addCreditLine(1, "Skye Thomas");
		addCreditLine(0, "");
		addCreditLine(0, "Information Systems Administrator");
		addCreditLine(1, "Josh Miedema");
		addCreditLine(0, "");
		addCreditLine(0, "Regional Sales Directors");
		addCreditLine(1, "Dan Matschina");
		addCreditLine(1, "Jim Holland");
		addCreditLine(1, "Jennifer Mirabelli");
		addCreditLine(0, "");
		addCreditLine(0, "Director of OEM & Alternative Channels");
		addCreditLine(1, "Brian Johnson");
		addCreditLine(0, "");
		addCreditLine(0, "Music by:");
		addCreditLine(1, "Lost City www.wolfpack1.com");
		addCreditLine(1, "Nathan Howard");
		addCreditLine(1, "David Kellogg");
		addCreditLine(1, "Diandre Hodges");
		addCreditLine(0, "");
		addCreditLine(0, "Song list:");
		addCreditLine(1, "Redline");
		addCreditLine(1, "Ali, Ali");
		addCreditLine(1, "We Roll");
		addCreditLine(0, "");
		addCreditLine(0, "Special Thanks");
		addCreditLine(1, "Anne Beggs");
		addCreditLine(1, "Don Borchers");
		addCreditLine(1, "Mike Dalton");
		addCreditLine(1, "Nicole Bement");
		addCreditLine(1, "Brian Kirkvold");
		addCreditLine(1, "Keri Gross");
		addCreditLine(1, "Mike Groshens");
		addCreditLine(1, "Kurt Niederloh");
		addCreditLine(1, "Chris Owen");
		addCreditLine(1, "Laura Saugen");
		addCreditLine(0, "");
		addCreditLine(0, "Additional Thanks");
		addCreditLine(1, "Emily Bauman");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "INVICTUS CREDITS");
		addCreditLine(1, "");
		addCreditLine(0, "");
		addCreditLine(0, "Producer");
		addCreditLine(1, "Tamas Kozak");
		addCreditLine(0, "");
		addCreditLine(0, "Lead Programmer");
		addCreditLine(1, "Denes Nagymathe");
		addCreditLine(0, "");
		addCreditLine(0, "Programming");
		addCreditLine(1, "Akos Divianszky");
		addCreditLine(1, "Laszlo Javorszky");
		addCreditLine(1, "Zsolt Klampeczki");
		addCreditLine(1, "Attila Kocsis");
		addCreditLine(1, "Jozsef Punk");
		addCreditLine(0, "");
		addCreditLine(0, "Tool Programming");
		addCreditLine(1, "Gabor Simon");
		addCreditLine(1, "Jozsef Punk");
		addCreditLine(1, "Zsolt Klampeczki");
		addCreditLine(0, "");
		addCreditLine(0, "Artwork");
		addCreditLine(1, "Tibor Mester");
		addCreditLine(1, "Csaba Csukas");
		addCreditLine(1, "Viktor Sass");
		addCreditLine(1, "Jozsef Bakos");
		addCreditLine(1, "David Szabo");
		addCreditLine(1, "Otto Feldmajer");
		addCreditLine(1, "Janos Varadi");
		addCreditLine(1, "Gabor Simon");
		addCreditLine(1, "Laszlo Fesus");
		addCreditLine(1, "Peter Gazso");
		addCreditLine(1, "Tibor Valyi");
		addCreditLine(1, "Zoltan Saghy");
		addCreditLine(1, "Balazs Borsos");
		addCreditLine(0, "");
		addCreditLine(0, "Music by");
		addCreditLine(1, "HOTEL SINUS www.hotelsinus.com");
		addCreditLine(1, "Laszlo Zizics");
		addCreditLine(1, "Gergo Ergelyi");
		addCreditLine(0, "");
		addCreditLine(0, "SFX");
		addCreditLine(1, "Tibor Simon");
		addCreditLine(1, "");
		addCreditLine(0, "Additional Testing");
		addCreditLine(1, "Gabor Szappanos");
		addCreditLine(1, "Laszlo Zizics");
		addCreditLine(1, "");
		addCreditLine(0, "Special Thanks To");
		addCreditLine(1, "Daniel 'Furball' Alabaster");
		addCreditLine(1, "and the www.buildersedge.org community");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "INVICTUS GAMES LTD. 2003.");
		addCreditLine(0, "all rights reserved");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
		addCreditLine(1, "");
	}

	public void addCreditLine(int arg0, String arg1)
	{
		if ((arg0 == 0))
		{
			creditsTxt[creditLines] = osd.createText(arg1, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 1.0);
			creditsTxt[creditLines++].changeColor(Osd.COLOR_UNSELECTED);
		} else
		{
			creditsTxt[creditLines++] = osd.createText(arg1, Frontend.largeFont, Text.ALIGN_CENTER, 0.0, 1.0);
		}
	}

}

public class PlayerSetupDialog extends Dialog
{
	static final int CMD_NEXT = 100;
	static final int CMD_PREV = 101;
	static final int CMD_HNEXT = 102;
	static final int CMD_HPREV = 103;
	Player plr;
	int startId = Racer.RID_FEJ;
	int numId = 60;
	int actId = (startId + (Math.random() * (60 - 1)));
	Rectangle rect;

	public PlayerSetupDialog(Controller arg0, int arg1, String arg2, Player arg3)
	{
		super(arg0, (arg1 | Dialog.DF_DARKEN), arg2, "OK");
		plr = arg3;
	}

	public void show()
	{
		Style local1 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWLF)
		);
		osd.createButton(local1, (-(0.6)), 0.4, PlayerSetupDialog.CMD_PREV, null);
		Style local2 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWRG)
		);
		osd.createButton(local2, 0.6, 0.4, PlayerSetupDialog.CMD_NEXT, null);
		osd.createHotkey(Input.AXIS_MENU_LEFT, Input.VIRTUAL, PlayerSetupDialog.CMD_HPREV, this);
		osd.createHotkey(Input.AXIS_MENU_RIGHT, Input.VIRTUAL, PlayerSetupDialog.CMD_HNEXT, this);
		osd.createText(plr.name, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.6).changeColor(-5592406);
		plr.character = new ResourceRef(actId);
		rect = osd.createRectangle(0.0, 0.04, 0.8, 1.4499999, 1, plr.character);
		super.show();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == PlayerSetupDialog.CMD_HPREV))
		{
			osd.changeSelection2(2, 0);
			osd.changeSelection2(2, 1);
			osdCommand(PlayerSetupDialog.CMD_PREV);
		} else
		{
			if ((arg0 == PlayerSetupDialog.CMD_HNEXT))
			{
				osd.changeSelection2(3, 0);
				osd.changeSelection2(3, 1);
				osdCommand(PlayerSetupDialog.CMD_NEXT);
			} else
			{
				if ((arg0 == PlayerSetupDialog.CMD_PREV))
				{
					if ((actId > startId))
					{
						actId--;
					} else
					{
						actId = ((startId + numId) - 1);
					}
					refreshCharacter();
				} else
				{
					if ((arg0 == PlayerSetupDialog.CMD_NEXT))
					{
						if ((actId < ((startId + numId) - 1)))
						{
							actId++;
						} else
						{
							actId = startId;
						}
						refreshCharacter();
					} else
					{
						super.osdCommand(arg0);
					}
				}
			}
		}
	}

	public void refreshCharacter()
	{
		plr.character = new ResourceRef(actId);
		rect.changeTexture(plr.character);
	}

}

public class VehicleFileReqDialog extends FileRequesterDialog
{
	public VehicleFileReqDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, String arg5)
	{
		super(arg0, arg1, arg2, arg3, arg4, arg5);
		if ((arg1 & Dialog.FRF_SAVE))
		{
			osd.defSelection = 5;
		}
	}

	public int validator(String arg0)
	{
		return Vehicle.fileCheck(arg0);
	}

}

public class CarrerFileReqDialog extends FileRequesterDialog
{
	public CarrerFileReqDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, String arg5, int arg6)
	{
		super(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	public int validator(String arg0)
	{
		return GameLogic.fileCheck(arg0);
	}

}
