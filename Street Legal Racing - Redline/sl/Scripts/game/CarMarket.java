package java.game;

import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.resource.*;

public class CarMarket extends Scene implements GameState
{
	static final int RID_MAP_DEALER = misc.dealer:0x1r;
	static final int RID_MAP_DEALER2 = misc.dealer2:0x1r;
	static final int RID_STUFF_DEALER = misc.dealer:0xfr;
	static final int RID_STUFF_DEALER2 = misc.dealer2:0xdr;
	static final float FLOOR_HEIGHT = 3.3;
	static final float PRICERATIO_NEW = 1.3;
	static final float PRICERATIO_USED = 1.1;
	static final float PRICERATIO_BUY = 0.90000004;
	static final float PRESTIGE_RATIO_BUY = 0.25;
	static final float PRESTIGE_RATIO_SELL = 0.5;
	static final int CMD_PREVCAR = 0;
	static final int CMD_NEXTCAR = 1;
	Player player;
	GameState parentState;
	GameRef cam;
	int move;
	Osd osd;
	Text moneytxt;
	Text carName;
	Gadget buyButton;
	int used;
	DealerData positions;
	int numpos;
	int welcomeTextureId;
	int sellTextureId;
	float priceRatio;
	RenderRef stuff1;
	VehicleDescriptor[] carDescriptors;
	Vehicle[] cars;
	int curcar;

	public static VehicleDescriptor[] getInitialCars(int arg0)
	{
		VehicleDescriptor[] local1 = new VehicleDescriptor[4];
		int local2;
		int local3;
		if (arg0)
		{
			local3 = VehicleType.VS_USED;
			local1[local2] = new VehicleDescriptor();
			local1[local2].id = cars.racers.Einvagen:0x6r;
			local1[local2].power = 0.90000004;
			local1[local2].optical = 0.95000005;
			local1[local2].tear = 0.93;
			local1[local2].wear = 0.83;
			local1[local2].stockPrestige = 204;
			local1[local2].fullPrestige = 244;
			local1[local2].stockQM = VehicleType.qm_stock_Einvagen_110_GT;
			local1[local2].fullQM = VehicleType.qm_full_Einvagen_110_GT;
			local1[local2].vehicleName = "Einvagen 110 GT";
			local1[local2++].colorIndex = (Math.random() * GameLogic.CARCOLORS.length);
			local1[local2] = new VehicleDescriptor();
			local1[local2].id = cars.racers.Einvagen:0x112r;
			local1[local2].power = 0.85;
			local1[local2].optical = 0.92;
			local1[local2].tear = 0.89;
			local1[local2].wear = 0.75;
			local1[local2].stockPrestige = 225;
			local1[local2].fullPrestige = 265;
			local1[local2].stockQM = VehicleType.qm_stock_Einvagen_110_GTK;
			local1[local2].fullQM = VehicleType.qm_full_Einvagen_110_GTK;
			local1[local2].vehicleName = "Einvagen 110 GTK";
			local1[local2++].colorIndex = (Math.random() * GameLogic.CARCOLORS.length);
			local1[local2] = new VehicleDescriptor();
			local1[local2].id = cars.racers.Duhen:0x6r;
			local1[local2].power = 0.90000004;
			local1[local2].optical = 0.90000004;
			local1[local2].tear = 0.87;
			local1[local2].wear = 0.88;
			local1[local2].stockPrestige = 265;
			local1[local2].fullPrestige = 286;
			local1[local2].stockQM = VehicleType.qm_stock_Duhen_SunStrip_1_5_DVC;
			local1[local2].fullQM = VehicleType.qm_full_Duhen_SunStrip_1_5_DVC;
			local1[local2].vehicleName = "Duhen SunStrip 1.5 DVC";
			local1[local2++].colorIndex = (Math.random() * GameLogic.CARCOLORS.length);
			local1[local2] = new VehicleDescriptor();
			local1[local2].id = cars.racers.Duhen:0x126r;
			local1[local2].power = 0.90000004;
			local1[local2].optical = 0.90000004;
			local1[local2].tear = 0.85;
			local1[local2].wear = 0.83;
			local1[local2].stockPrestige = 292;
			local1[local2].fullPrestige = 313;
			local1[local2].stockQM = VehicleType.qm_stock_Duhen_SunStrip_1_8_DVC;
			local1[local2].fullQM = VehicleType.qm_full_Duhen_SunStrip_1_8_DVC;
			local1[local2].vehicleName = "Duhen SunStrip 1.8 DVC";
			local1[local2++].colorIndex = (Math.random() * GameLogic.CARCOLORS.length);
		} else
		{
			local3 = VehicleType.VS_STOCK;
		}
		while ((local2 < local1.length))
		{
			local1[local2] = GameLogic.getVehicleDescriptor(local3);
			local2++;
		}
		return local1;
	}

	public static void alterCars(int arg0, VehicleDescriptor[] arg1, float arg2)
	{
		int local3;
		if (arg0)
		{
			local3 = VehicleType.VS_USED;
		} else
		{
			local3 = VehicleType.VS_STOCK;
		}
		int local4 = 0;
		while ((local4 < arg1.length))
		{
			if (((!(arg1[local4])) || (Math.random() < (0.02 * arg2))))
			{
				arg1[local4] = GameLogic.getVehicleDescriptor(local3);
			} else
			{
				if ((Math.random() < (0.02 * arg2)))
				{
					arg1[local4] = null;
				}
			}
			local4++;
		}
	}

	public CarMarket(int arg0, VehicleDescriptor[] arg1)
	{
		super();
		createNativeInstance();
		used = arg0;
		carDescriptors = arg1;
		player = GameLogic.player;
	}

	public Vector3 getCarPos(int arg0)
	{
		return new Vector3(positions.carPos[arg0]);
	}

	public Ypr getCarOri(int arg0)
	{
		Ypr local2 = new Ypr(positions.carOri[arg0]);
		local2.y += (0.2 - (Math.random() * 0.4));
		return local2;
	}

	public void moveCamera()
	{
		if (cars[curcar])
		{
			int local1 = cars[curcar].getTotalPrice();
			if ((used && (curcar == 0)))
			{
				local1 *= (1.0 + ((cars[curcar].getPrestigeMultiplier() - 1.0) * CarMarket.PRESTIGE_RATIO_BUY));
				local1 *= CarMarket.PRICERATIO_BUY;
				carName.changeText(("YOUR CAR  $" + local1));
			} else
			{
				local1 *= (1.0 + ((cars[curcar].getPrestigeMultiplier() - 1.0) * CarMarket.PRESTIGE_RATIO_SELL));
				local1 *= priceRatio;
				carName.changeText(((cars[curcar].toString() + " $") + local1));
			}
			cam.command((("move " + cars[curcar].id()) + " 0,0,0 4.5"));
			cam.command((("look " + cars[curcar].id()) + " 0,0,0 0,0,0"));
			cam.command("height 1.9");
			if ((used && (curcar == 0)))
			{
				buyButton.changeTexture(new ResourceRef(frontend:0x12fr));
			} else
			{
				buyButton.changeTexture(new ResourceRef(frontend:0x125r));
			}
		}
	}

	public void enter(GameState arg0)
	{
		if ((arg0 instanceof CarInfo))
		{
			osd.show();
		} else
		{
			parentState = arg0;
			if (used)
			{
				map = new GroundRef(CarMarket.RID_MAP_DEALER2);
				positions = new DealerSHData();
				stuff1 = new RenderRef(map, CarMarket.RID_STUFF_DEALER2, null);
				welcomeTextureId = frontend:0xa4r;
				sellTextureId = frontend:0xcer;
				priceRatio = CarMarket.PRICERATIO_USED;
			} else
			{
				map = new GroundRef(CarMarket.RID_MAP_DEALER);
				positions = new DealerNewData();
				stuff1 = new RenderRef(map, CarMarket.RID_STUFF_DEALER, null);
				welcomeTextureId = frontend:0xcar;
				sellTextureId = frontend:0xcdr;
				priceRatio = CarMarket.PRICERATIO_NEW;
			}
			Frontend.loadingScreen.show(new ResourceRef(welcomeTextureId));
			numpos = positions.carPos.length;
			cars = new Vehicle[numpos];
			osd = new Osd();
			osd.globalHandler = this;
			carName = osd.createText(null, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, (-(0.98)));
			moneytxt = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.95000005, (-(0.98)));
			osd.createHeader("Car Dealer");
			osd.createRectangle(1.01, (-(0.82)), 1.2, 0.22, (-(1)), new ResourceRef(frontend:0x24r));
			Style local2 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_LEFT, null);
			Menu local3 = osd.createMenu(local2, 0.45, (-(0.82)), 0, Osd.MD_HORIZONTAL);
			local3.addItem(new ResourceRef(Osd.RID_ARROWLF), Input.AXIS_MENU_LEFT, null);
			local3.addItem(new ResourceRef(Osd.RID_ARROWRG), Input.AXIS_MENU_RIGHT, null);
			local3.addItem(new ResourceRef(frontend:0x124r), Input.AXIS_HELP, null);
			buyButton = local3.addItem(new ResourceRef(frontend:0x125r), Input.AXIS_SELECT, null);
			local3.addSeparator();
			local3.addSeparator();
			local3.addItem(new ResourceRef(Osd.RID_BACK), Input.AXIS_CANCEL, null);
			lockPlayerCar();
			addSceneElements(GameLogic.getTime());
			if ((!(used)))
			{
				GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x3ar));
			}
			int local4 = ((cars.length - carDescriptors.length) - used);
			int local5;
			int local6 = used;
			while ((local6 < cars.length))
			{
				if (((local4 && (Math.random() < 0.4)) || ((cars.length - local4) <= local6)))
				{
					--local4;
				} else
				{
					VehicleDescriptor local7 = carDescriptors[local5++];
					if (local7)
					{
						Vehicle local8 = new Vehicle(
							map,
							local7.id,
							local7.colorIndex,
							local7.optical,
							local7.power,
							local7.wear,
							local7.tear
						);
						local8.chassis.setMileage(((1 - local7.wear) * 10000000.0));
						addCar(local8, local6);
					}
				}
				local6++;
			}
			System.timeWarp(1.0);
			osd.getViewport().activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
			if (used)
			{
				cam = new GameRef(
					map,
					GameRef.RID_CAMERA,
					"-3,2,0, 0,0,0, 0x13, 1.8,0.0, 0.05",
					"Car Dealer camera"
				);
			} else
			{
				cam = new GameRef(
					map,
					GameRef.RID_CAMERA,
					"10,2,-3, 0,0,0, 0x13, 1.8,0.0, 0.05",
					"Car Dealer camera"
				);
			}
			cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
			cam.command("dist 2.5 4.6");
			cam.command("smooth 0.05 0.5");
			cam.command("force 0.3 0.5 -0.7");
			cam.command("torque 0.06");
			cam.command("zoom 60 5");
			goRight();
			goLeft();
			refreshMoneyString();
			Frontend.loadingScreen.userWait(5.0);
			osd.createHotkey(Input.AXIS_MENU_LEFT, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_MENU_LEFT, this);
			osd.createHotkey(
				Input.AXIS_MENU_RIGHT,
				(Input.VIRTUAL | Osd.HK_STATIC),
				Input.AXIS_MENU_RIGHT,
				this
			);
			osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_CANCEL, this);
			osd.show();
			setEventMask(GameRef.EVENT_CURSOR);
		}
		Input.getAxis(1, (-(1)));
		Input.cursor.enable(1);
		Input.cursor.addHandler(this);
		Input.cursor.enableCameraControl(cam);
	}

	public void exit(GameState arg0)
	{
		Input.cursor.enable(0);
		Input.cursor.remHandler(this);
		Input.cursor.disableCameraControl();
		if ((arg0 instanceof CarInfo))
		{
			osd.hide();
		} else
		{
			clearEventMask(GameRef.EVENT_ANY);
			cam.destroy();
			releasePlayerCar();
			int local2 = 0;
			while ((local2 < cars.length))
			{
				if (cars[local2])
				{
					cars[local2].destroy();
					cars[local2] = null;
				}
				local2++;
			}
			remSceneElements();
			map.unload();
			osd.hide();
			osd = null;
			stuff1.destroy();
			parentState = null;
		}
	}

	public void lockPlayerCar()
	{
		if ((used && GameLogic.player.car))
		{
			addCar(GameLogic.player.car, 0);
			GameLogic.player.car = null;
		}
	}

	public void releasePlayerCar()
	{
		if ((used && cars[0]))
		{
			cars[0].command("start");
			GameLogic.player.car = cars[0];
			GameLogic.player.car.setParent(GameLogic.player);
			cars[0] = null;
		}
	}

	public int addCar(Vehicle arg0, int arg1)
	{
		if (arg0)
		{
			if ((!(cars[arg1])))
			{
				cars[arg1] = arg0;
				arg0.setParent(map);
				Vector3 local3 = getCarPos(arg1);
				local3.z += (Math.random() * 0.1);
				local3.x += (Math.random() * 0.1);
				arg0.setMatrix(local3, getCarOri(arg1));
				arg0.command("reset");
				arg0.command(("setsteer " + ((-(0.7)) + (Math.random() * 1.4))));
				arg0.command("stop");
				return 1;
			}
		}
		return 0;
	}

	public GameRef createCar(GameRef arg0)
	{
		Vehicle local2;
		float local3;
		float local4;
		float local5;
		float local6;
		if (used)
		{
			local3 = (0.3 + (Math.random() * 1.5));
			local4 = (0.5 + (Math.random() * 1.0));
			local5 = (0.4 + (Math.random() * 0.6));
			local6 = (0.7 + (Math.random() * 0.3));
		} else
		{
			local3 = 1.0;
			local4 = 1.0;
			local5 = 1.0;
			local6 = 1.0;
		}
		local2 = new Vehicle(map, arg0.id(), Math.random(), local3, local4, local5, local6);
		local2.chassis.setMileage(((1 - local5) * 10000000.0));
		return local2;
	}

	public void refreshMoneyString()
	{
		new SfxRef(Frontend.SFX_MONEY).play();
		moneytxt.changeText(("$" + Integer.toString(player.money)));
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == Input.AXIS_MENU_LEFT))
		{
			goLeft();
		} else
		{
			if ((arg0 == Input.AXIS_MENU_RIGHT))
			{
				goRight();
			} else
			{
				if ((arg0 == Input.AXIS_CANCEL))
				{
					GameLogic.changeActiveSection(parentState);
				} else
				{
					if ((arg0 == Input.AXIS_HELP))
					{
						if (cars[curcar])
						{
							GameLogic.changeActiveSection(new CarInfo(cars[curcar]));
						}
					} else
					{
						if ((arg0 == Input.AXIS_SELECT))
						{
							buyCar();
						}
					}
				}
			}
		}
	}

	public void goLeft()
	{
		while (1)
		{
			if ((--curcar < 0))
			{
				curcar = (numpos - 1);
			}
			if (cars[curcar])
			{
				break;
			}
		}
		moveCamera();
	}

	public void goRight()
	{
		while (1)
		{
			if ((++curcar >= numpos))
			{
				curcar = 0;
			}
			if (cars[curcar])
			{
				break;
			}
		}
		moveCamera();
	}

	public void changePointer()
	{
		if (move)
		{
			Input.cursor.setPointer(Frontend.pointers, "M");
		} else
		{
			Input.cursor.setPointer(Frontend.pointers, "J");
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
				GameRef local7 = new GameRef(arg2.token(++local4).intValue());
				int local8 = local7.getInfo(GameType.GII_CATEGORY);
				if (((local8 == GameType.GIR_CAT_PART) || (local8 == GameType.GIR_CAT_VEHICLE)))
				{
					int local9 = local7.getScriptInstance().getCar();
					int local10 = 0;
					while ((local10 < cars.length))
					{
						if (cars[local10])
						{
							if (cars[local10].chassis)
							{
								if ((cars[local10].chassis.id() == local9))
								{
									curcar = local10;
									moveCamera();
									break;
								}
							}
						}
						local10++;
					}
				}
			} else
			{
				if ((local5 == GameType.EC_RCLICK))
				{
					GameRef local7_2 = new GameRef(arg2.token(++local4).intValue());
					int local8_2 = local7_2.getInfo(GameType.GII_CATEGORY);
					if (((local8_2 == GameType.GIR_CAT_PART) || (local8_2 == GameType.GIR_CAT_VEHICLE)))
					{
						int local9_2 = local7_2.getScriptInstance().getCar();
						if (cars[curcar])
						{
							if (cars[curcar].chassis)
							{
								if ((cars[curcar].chassis.id() == local9_2))
								{
									cam.command(((((((("look " + local7_2.id()) + " ") + arg2.token(++local4)) + ",") + arg2.token(++local4)) + ",") + arg2.token(++local4)));
								}
							}
						}
					}
				} else
				{
					if ((local5 == GameType.EC_RDRAGBEGIN))
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
						if ((local5 == GameType.EC_RDRAGEND))
						{
							move = 0;
							changePointer();
							player.controller.user_Del(Input.AXIS_LOOK_UPDOWN, ControlSet.MOUSE, 1);
							player.controller.user_Del(Input.AXIS_LOOK_LEFTRIGHT, ControlSet.MOUSE, 0);
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

	public void buyCar()
	{
		if (cars[curcar])
		{
			int local1 = cars[curcar].getTotalPrice();
			if ((used && (curcar == 0)))
			{
				local1 *= (1.0 + ((cars[curcar].getPrestigeMultiplier() - 1.0) * CarMarket.PRESTIGE_RATIO_BUY));
				local1 *= CarMarket.PRICERATIO_BUY;
				Dialog local2 = new NoYesDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"SELL CAR",
					(("Do you want to sell your car for $" + local1) + " ?")
				);
				if ((local2.display() == 0))
				{
					player.money += local1;
					refreshMoneyString();
					cars[curcar].command("reset");
					cars[curcar].command("start");
					int local3 = (numpos - 1);
					while (local3)
					{
						if ((!(cars[local3])))
						{
							addCar(cars[curcar], local3);
							cars[curcar] = null;
							goRight();
						}
						local3--;
					}
					Frontend.loadingScreen.display(
						new SimpleLoadingDialog((Dialog.DF_FULLSCREEN | Dialog.DF_MODAL), new ResourceRef(frontend:0xa3r)),
						5.0
					);
				}
			} else
			{
				local1 *= (1.0 + ((cars[curcar].getPrestigeMultiplier() - 1.0) * CarMarket.PRESTIGE_RATIO_SELL));
				local1 *= priceRatio;
				if ((local1 <= player.money))
				{
					releasePlayerCar();
					player.carlot.lockPlayerCar();
					int local2_2 = player.carlot.getFreeSlot();
					player.carlot.releasePlayerCar();
					lockPlayerCar();
					if ((local2_2 >= 0))
					{
						Dialog local3_2 = new NoYesDialog(
							player.controller,
							((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
							"BUY CAR",
							((((((("Do you want to buy this " + cars[curcar].toString()) + " for $") + local1) + " ? \n \n About the ") + cars[curcar].toString()) + ": ") + cars[curcar].chassis.description)
						);
						if ((local3_2.display() == 0))
						{
							player.money -= local1;
							refreshMoneyString();
							releasePlayerCar();
							player.carlot.lockPlayerCar();
							player.carlot.saveCar(player.carlot.curcar);
							player.carlot.flushCars();
							player.car = cars[curcar];
							GameLogic.garage.releaseCar();
							cars[curcar] = null;
							int local4 = 0;
							while ((local4 < carDescriptors.length))
							{
								if ((carDescriptors[local4] && (player.car.getInfo(GameType.GII_TYPE) == carDescriptors[local4].id)))
								{
									carDescriptors[local4] = null;
								}
								local4++;
							}
							Frontend.loadingScreen.display(
								new SimpleLoadingDialog((Dialog.DF_FULLSCREEN | Dialog.DF_MODAL), new ResourceRef(sellTextureId)),
								5.0
							);
							osdCommand(Input.AXIS_CANCEL);
						}
					} else
					{
						new WarningDialog(
							player.controller,
							(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
							"CAR LOT FULL",
							"There is no more free space in your car lot.\nSell some cars to free up parking space!").display(
						);
					}
				} else
				{
					new WarningDialog(
						player.controller,
						(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
						"NOT ENOUGHT MONEY",
						(("You don't have $" + local1) + " to buy this car!")).display(
					);
				}
			}
		}
	}

}

public class DealerData
{
	Vector3[] carPos;
	Ypr[] carOri;

}

public class DealerNewData extends DealerData
{
	public DealerNewData()
	{
		super();
		carPos = new Vector3[7];
		carOri = new Ypr[7];
		carPos[0] = new Vector3(3.901, 0.0, (-(3.499)));
		carOri[0] = new Ypr((-(1.963)), 0.0, 0.0);
		carPos[1] = new Vector3(3.901, 0.0, (-(7.6029997)));
		carOri[1] = new Ypr((-(1.963)), 0.0, 0.0);
		carPos[2] = new Vector3(5.099, 0.0, (-(12.927)));
		carOri[2] = new Ypr((-(1.876)), 0.0, 0.0);
		carPos[3] = new Vector3(10.922, 0.0, (-(11.929999)));
		carOri[3] = new Ypr((-(2.923)), 0.0, 0.0);
		carPos[4] = new Vector3(17.295, 0.0, (-(13.173)));
		carOri[4] = new Ypr(1.9810002, 0.0, 0.0);
		carPos[5] = new Vector3(17.226002, 0.0, (-(7.6029997)));
		carOri[5] = new Ypr(1.9810002, 0.0, 0.0);
		carPos[6] = new Vector3(17.226002, 0.0, (-(3.267)));
		carOri[6] = new Ypr(1.9810002, 0.0, 0.0);
	}

}

public class DealerSHData extends DealerData
{
	public DealerSHData()
	{
		super();
		carPos = new Vector3[9];
		carOri = new Ypr[9];
		carPos[0] = new Vector3(0.38, 0.0, 3.9160001);
		carOri[0] = new Ypr(2.6179998, 0.0, 0.0);
		carPos[1] = new Vector3((-(11.733)), 0.0, (-(2.666)));
		carOri[1] = new Ypr((-(1.5519999)), 0.0, 0.0);
		carPos[2] = new Vector3((-(11.733)), 0.0, (-(8.452)));
		carOri[2] = new Ypr((-(1.5519999)), 0.0, 0.0);
		carPos[3] = new Vector3((-(11.733)), 0.0, (-(14.563001)));
		carOri[3] = new Ypr((-(1.5519999)), 0.0, 0.0);
		carPos[4] = new Vector3((-(11.733)), 0.0, (-(20.467999)));
		carOri[4] = new Ypr((-(1.5519999)), 0.0, 0.0);
		carPos[5] = new Vector3((-(4.4750004)), 0.0, (-(20.492)));
		carOri[5] = new Ypr(1.592, 0.0, 0.0);
		carPos[6] = new Vector3((-(4.4750004)), 0.0, (-(14.101001)));
		carOri[6] = new Ypr(1.592, 0.0, 0.0);
		carPos[7] = new Vector3((-(4.4750004)), 0.0, (-(8.507)));
		carOri[7] = new Ypr(1.592, 0.0, 0.0);
		carPos[8] = new Vector3((-(4.4750004)), 0.0, (-(2.775)));
		carOri[8] = new Ypr(1.592, 0.0, 0.0);
	}

}
