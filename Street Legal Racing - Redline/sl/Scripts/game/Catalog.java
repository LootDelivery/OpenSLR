package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.rgearpart.reciprocatingrgearpart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class Catalog extends GameType implements GameState
{
	static final ResourceRef RR_ICON_CAR_COMP = new ResourceRef(misc.catalog:0x4r);
	static final ResourceRef RR_ICON_CAR_1STEP = new ResourceRef(misc.catalog:0x5r);
	static final ResourceRef RR_ICON_INV_COMP = new ResourceRef(misc.catalog:0x7r);
	static final int DECALPRICE = 20;
	static final int SC_ENGINE = 0;
	static final int SC_BODY = 1;
	static final int SC_RGEAR = 2;
	static final int SC_NONE = 3;
	static final int CMD_EXIT = 0;
	static final int CMD_FRONTPAGE = 1;
	static final int CMD_MAIN1 = 2;
	static final int CMD_MAIN2 = 3;
	static final int CMD_MAIN3 = 4;
	static final int CMD_MAIN4 = 5;
	static final int CMD_MAIN5 = 6;
	static final int CMD_MAIN6 = 7;
	static final int CMD_MAIN1_1 = 8;
	static final int CMD_MAIN1_2 = 9;
	static final int CMD_MAIN1_3 = 10;
	static final int CMD_MAIN1_4 = 11;
	static final int CMD_MAIN1_5 = 12;
	static final int CMD_MAIN1_6 = 13;
	static final int CMD_MAIN1_7 = 14;
	static final int CMD_MAIN1_8 = 15;
	static final int CMD_MAIN1_9 = 16;
	static final int CMD_MAIN1_10 = 17;
	static final int CMD_MAIN1_11 = 18;
	static final int CMD_MAIN1_12 = 19;
	static final int CMD_MAIN1_13 = 20;
	static final int CMD_MAIN1_14 = 21;
	static final int CMD_MAIN1_15 = 22;
	static final int CMD_MAIN1_16 = 23;
	static final int CMD_MAIN1_17 = 24;
	static final int CMD_MAIN1_18 = 25;
	static final int CMD_MAIN1_19 = 26;
	static final int CMD_MAIN2_1 = 27;
	static final int CMD_MAIN2_2 = 28;
	static final int CMD_MAIN2_3 = 29;
	static final int CMD_MAIN2_4 = 30;
	static final int CMD_MAIN2_5 = 31;
	static final int CMD_MAIN2_6 = 32;
	static final int CMD_MAIN2_7 = 33;
	static final int CMD_MAIN3_1 = 34;
	static final int CMD_MAIN3_2 = 35;
	static final int CMD_MAIN3_3 = 36;
	static final int CMD_MAIN3_4 = 37;
	static final int CMD_MAIN3_5 = 38;
	static final int CMD_MAIN3_6 = 39;
	static final int CMD_MAIN3_7 = 40;
	static final int CMD_MAIN3_8 = 42;
	static final int CMD_MAIN3_9 = 43;
	static final int CMD_MAIN3_10 = 44;
	static final int CMD_MAIN3_11 = 45;
	static final int CMD_MAIN3_12 = 46;
	static final int CMD_MAIN3_13 = 47;
	static final int CMD_MAIN3_14 = 48;
	static final int CMD_MAIN3_15 = 49;
	static final int CMD_MAIN3_16 = 50;
	static final int CMD_MAIN3_17 = 51;
	static final int CMD_MAIN3_18 = 52;
	static final int CMD_MAIN4_1 = 53;
	static final int CMD_MAIN4_2 = 54;
	static final int CMD_MAIN4_3 = 55;
	static final int CMD_MAIN4_4 = 56;
	static final int CMD_MAIN4_5 = 57;
	static final int CMD_MAIN4_6 = 58;
	static final int CMD_MAIN4_7 = 59;
	static final int CMD_MAIN4_8 = 60;
	static final int CMD_MAIN5_1 = 62;
	static final int CMD_MAIN5_2 = 63;
	static final int CMD_MAIN5_3 = 64;
	static final int CMD_MAIN5_4 = 65;
	static final int CMD_MAIN5_5 = 66;
	static final int CMD_MAIN5_6 = 67;
	static final int CMD_MAIN6_1 = 68;
	static final int CMD_MAIN6_2 = 69;
	static final int CMD_MAIN6_3 = 70;
	static final int CMD_MAIN6_4 = 71;
	static final int CMD_MAIN6_5 = 72;
	static final int CMD_MAIN6_6 = 73;
	static final int CMD_MAIN6_7 = 74;
	static final int CMD_DB0 = 75;
	static final int CMD_DB1 = 76;
	static final int CMD_DB2 = 77;
	static final int CMD_DB3 = 78;
	static final int CMD_DB4 = 79;
	static final int CMD_DB5 = 80;
	static final int CMD_DB6 = 81;
	static final int CMD_DB7 = 82;
	static final int CMD_PREVDECALPAGE = 83;
	static final int CMD_NEXTDECALPAGE = 84;
	static final int CMD_PREVPARTPAGE = 85;
	static final int CMD_NEXTPARTPAGE = 86;
	static final int CMD_VEHICLE = 100;
	static final int CMD_VEH0 = 101;
	static final int CMD_VEH1 = 102;
	static final int CMD_VEH2 = 103;
	static final int CMD_VEH3 = 104;
	static final int CMD_VEH4 = 105;
	static final int CMD_VEH5 = 106;
	static final int CMD_VEH6 = 107;
	static final int CMD_VEH7 = 108;
	static final int CMD_VEH8 = 109;
	static final int CMD_VEH9 = 110;
	static final int CMD_UP = 111;
	static final int CMD_DOWN = 112;
	static final int CMD_VEHI = 113;
	static final int CMD_CHAS = 114;
	static final int CMD_VBUY = 115;
	static final int CMD_VCOLOR = 116;
	static final int NO_FILTER = 120;
	static final int RIMS_FILTER = 121;
	static final String pageNumberPrefix;
	static final float PRICERATIO = 1.1;
	Player player;
	Osd osd;
	int actGroup;
	Text moneytxt;
	Text pgNumberL;
	Text pgNumberR;
	Text[] pageNumberL = new Text[7];
	Text[] pageNumberR = new Text[7];
	CatalogInventory inventory;
	Vector collector;
	GameRef parts;
	int curpage = 1;
	int step;
	int filter;
	int currentCarPackId;
	int partsPackId;
	Decal[] curDecals;
	Button[] decalButtons;
	Vector decals;
	int showDecals;
	int mainGroup;
	int VehicleGroup;
	int main1Group;
	int main2Group;
	int main3Group;
	int main4Group;
	int main5Group;
	int main6Group;
	int decalsGroup;
	int parts1Group;
	int parts2Group;
	int parts3Group;
	int parts4Group;
	int parts5Group;
	ResourceRef[] carTypes;
	Button VColor_Button;
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
	Button UpButton;
	Button DownButton;
	int Rim_size;
	int VehAmount;
	int ColorIDX;
	int CurrentPage;
	int CurrentName;
	int CompleteCar;
	int RetailP;
	int DeliveryP;
	int TotalP;
	int ButtonNum;
	Text CurrentPageText;
	GameRef CarScene;
	GameRef Cam;
	RenderRef SceneSun;
	Thread CamThread;
	Ypr CamYpr;
	Vector3 CamVector;
	Viewport CarViewport;
	Vehicle CurrentCar;
	GameRef CurrentChassis;
	Text CurrentCarName;
	Text RetailText;
	Text DeliveryText;
	Text TotalText;
	TextBox CurrentCarDescription;
	Vector3 Size;

	public static Vector collectDecals(String arg0)
	{
		Vector local1 = new Vector();
		FindFile local2 = new FindFile();
		String local3 = local2.first((("decals/textures/catalog/" + arg0) + "/*.png"));
		while (local3)
		{
			Decal local4 = new Decal(((("decals/textures/catalog/" + arg0) + "/") + local3));
			local1.addElement(local4);
			local3 = local2.next();
		}
		local2.close();
		return local1;
	}

	public Catalog()
	{
		super();
		createNativeInstance();
		player = GameLogic.player;
		carTypes = GameLogic.VEHICLETYPE_ROOT.getChildNodes();
	}

	public void clearObjectCache()
	{
		if (inventory)
		{
			inventory.hide();
			inventory.flushAll();
			inventory = null;
		}
		collector = null;
	}

	public void collectObjectsBegin()
	{
		clearObjectCache();
		inventory = new CatalogInventory(this, player, 0.02, 0.25, 0.96000004, 0.5);
		collector = new Vector();
		if (player.car)
		{
			currentCarPackId = (player.car.getInfo(GameType.GII_TYPE) >> 16);
		} else
		{
			currentCarPackId = 0;
		}
		partsPackId = System.openLib("parts.rpk");
	}

	public void collectObjectsEnd()
	{
		collectObjectsStep(9);
		inventory.update();
		inventory.show();
	}

	public void collectObjects(GameRef arg0)
	{
		collectObjects(arg0, null, null, null, null);
	}

	public void collectObjects(GameRef arg0, GameRef arg1)
	{
		collectObjects(arg0, arg1, null, null, null);
	}

	public void collectObjects(GameRef arg0, GameRef arg1, GameRef arg2)
	{
		collectObjects(arg0, arg1, arg2, null, null);
	}

	public void collectObjects(GameRef arg0, GameRef arg1, GameRef arg2, GameRef arg3)
	{
		collectObjects(arg0, arg1, arg2, arg3, null);
	}

	public void collectObjects(GameRef arg0, GameRef arg1, GameRef arg2, GameRef arg3, GameRef arg4)
	{
		if (arg0)
		{
			collector.addElement(arg0);
		}
		if (arg1)
		{
			collector.addElement(arg1);
		}
		if (arg2)
		{
			collector.addElement(arg2);
		}
		if (arg3)
		{
			collector.addElement(arg3);
		}
		if (arg4)
		{
			collector.addElement(arg4);
		}
	}

	public int collectObjectsStep(int arg0)
	{
		step = arg0;
		if (collector)
		{
			while (((!(collector.isEmpty())) && step))
			{
				parts = collector.removeLastElement();
				if (parts.isScripted("java.game.parts.Part"))
				{
					osdCommand(filter);
				} else
				{
					if (parts.isScripted("java.game.parts.Set"))
					{
						Set local2 = parts.create(null, parts, null, "set_loader");
						InventoryItem_Folder local3 = new InventoryItem_Folder(inventory);
						local3.set = local2;
						local2.build(local3.inv);
						inventory.items.addElement(local3);
					} else
					{
						if ((parts = parts.getFirstChild()))
						{
							while (parts)
							{
								if (parts.isScripted())
								{
									collector.addElement(parts);
								} else
								{
									int local2_2;
									int local3_2 = (parts.id() >> 16);
									int local4 = (carTypes.length - 1);
									while ((local4 >= 0))
									{
										if ((local3_2 == (carTypes[local4].id() >> 16)))
										{
											local2_2 = 1;
											break;
										}
										local4--;
									}
									if (((!(local2_2)) || (local3_2 == currentCarPackId)))
									{
										collector.addElement(parts);
									}
								}
								parts = parts.getNextChild();
							}
						}
					}
				}
			}
			inventory.update();
			return (!(collector.isEmpty()));
		}
		return 0;
	}

	public void setDecalButtons()
	{
		int local1 = decals.size();
		clearDecalButtons();
		int local2 = 0;
		while ((local2 < decalButtons.length))
		{
			RenderRef local3 = new RenderRef(frontend:0xaer);
			if ((local1 > (showDecals + local2)))
			{
				curDecals[local2] = decals.elementAt((showDecals + local2));
				decalButtons[local2].rect.changeTexture(decals.elementAt((showDecals + local2)));
			}
			local2++;
		}
	}

	public void clearDecalButtons()
	{
		int local1 = 0;
		while ((local1 < decalButtons.length))
		{
			decalButtons[local1].rect.changeTexture(Osd.RRT_EMPTY);
			if (curDecals[local1])
			{
				curDecals[local1].unload();
				curDecals[local1] = null;
			}
			local1++;
		}
	}

	public void enter(GameState arg0)
	{
		osd = new Osd();
		osd.globalHandler = this;
		showDecals = 0;
		setEventMask(GameRef.EVENT_TIME);
		createOSDObjects();
		osd.show();
		Input.cursor.enable(1);
		setEventMask(GameRef.EVENT_CURSOR);
		GfxEngine.setGlobalEnvmap(new ResourceRef(MW_Mod:0x69r));
	}

	public void exit(GameState arg0)
	{
		Frontend.loadingScreen.hide();
		clearEventMask(GameRef.EVENT_ANY);
		removeAllTimers();
		Input.cursor.enable(0);
		Catalog.pageNumberPrefix = null;
		osd.hide();
	}

	public void VehicleScene()
	{
		CarScene = new Dummy(GameRef.WORLDTREEROOT);
		CarViewport = new Viewport(12, 0.53, 0.205, 0.45, 0.4);
		CarViewport.activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
		Cam = new GameRef(CarScene, GameRef.RID_CAMERA, "-2,1,-3 , -2.5,0,0 ,0x02", "camera");
		Cam.setMatrix(new Vector3((-(2)), 1, (-(4))), new Ypr((-(2.7)), (-(0.2)), 0));
		Cam.command(((("render " + CarViewport.id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
		Cam.command((("look " + CarScene.id()) + " 0.3,-0.6,0"));
		Cam.command("zoom 35 10");
		CurrentCar = new Vehicle();
		RenderRef local1 = new RenderRef(misc.garage:0x1dr);
		SceneSun = new RenderRef(CarScene, local1, "light for this scene");
		ColorIDX = 0;
		CurrentPage = 0;
		CompleteCar = 0;
		osdCommand(Catalog.CMD_UP);
		osdCommand(Catalog.CMD_VEH0);
	}

	public void createOSDObjects()
	{
		Style local1 = new Style(0.7, 0.13, Frontend.largeFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		Style local2 = new Style(0.7, 0.13, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		Style local3 = new Style(
			0.120000005,
			0.120000005,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_RIGHT,
			new ResourceRef(Osd.RID_ARROWRG)
		);
		Style local4 = new Style(
			0.120000005,
			0.120000005,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			new ResourceRef(Osd.RID_ARROWLF)
		);
		Style local5 = new Style(
			0.120000005,
			0.120000005,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_EXIT)
		);
		Style local6 = new Style(
			0.120000005,
			0.120000005,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_BACK)
		);
		Style local7 = new Style(0.7, 0.13, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
		Style local8 = new Style(0.7, 0.13, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		Style local9 = new Style(0.4, 0.13, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
		Style local10 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		Style local11 = new Style(0.17, 0.17, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		Style local12 = new Style(
			0.2,
			0.25,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(frontend:0x125r)
		);
		Menu local13;
		moneytxt = osd.createText(null, Frontend.mediumFont, Text.ALIGN_CENTER, 0.5, (-(0.95000005)));
		osd.endGroup();
		osd.createBG(new ResourceRef(misc.catalog:0x6r));
		local13 = osd.createMenu(local1, 1.0, (-(0.55)), 0.18);
		local13.addItem("VEHICLE", Catalog.CMD_VEHICLE);
		local13.addItem("ENGINE", Catalog.CMD_MAIN1);
		local13.addItem("BODY", Catalog.CMD_MAIN2);
		local13.addItem("RUNNING GEAR", Catalog.CMD_MAIN3);
		local13.addItem("INTERIOR", Catalog.CMD_MAIN4);
		local13.addItem("AUDIO", Catalog.CMD_MAIN5);
		local13.addItem("DECALS", Catalog.CMD_MAIN6);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_EXIT, this);
		actGroup = (mainGroup = osd.endGroup());
		osd.createBG(new ResourceRef(misc.catalog:0x9r));
		local13 = osd.createMenu(local7, (-(0.98)), (-(0.83)), 0.14);
		local13.addItem("ENGINE BLOCKS", Catalog.CMD_MAIN1_1);
		local13.addItem("CRANKSHAFTS & BEARING", Catalog.CMD_MAIN1_2);
		local13.addItem("CONNECTING RODS", Catalog.CMD_MAIN1_3);
		local13.addItem("PISTONS", Catalog.CMD_MAIN1_4);
		local13.addItem("OIL PANS", Catalog.CMD_MAIN1_5);
		local13.addItem("CYLINDER HEADS", Catalog.CMD_MAIN1_6);
		local13.addItem("INTAKE & EXHAUST CRANKSHAFTS", Catalog.CMD_MAIN1_7);
		local13.addItem("AIR DELIVERY", Catalog.CMD_MAIN1_8);
		local13.addItem("FUEL DELIVERY", Catalog.CMD_MAIN1_9);
		local13.addItem("CLUTCHS & FLYWHEELS", Catalog.CMD_MAIN1_10);
		local13.addItem("TRANSMISSION & DRIVETRAIN", Catalog.CMD_MAIN1_11);
		local13 = osd.createMenu(local2, 1.0, (-(0.55)), 0.14);
		local13.addItem("EXHAUST SYSTEM", Catalog.CMD_MAIN1_12);
		local13.addItem("EXHAUST TIPS", Catalog.CMD_MAIN1_13);
		local13.addSeparator();
		local13.addItem("DRIVE BELTS", Catalog.CMD_MAIN1_14);
		local13.addItem("ACCESSORIES & OTHERS", Catalog.CMD_MAIN1_15);
		local13.addSeparator();
		local13.addItem("CHARGING SYSTEM", Catalog.CMD_MAIN1_16);
		local13.addItem("N2O SYSTEM", Catalog.CMD_MAIN1_17);
		local13.addItem("PRO TUNING", Catalog.CMD_MAIN1_18);
		local13.addItem("ENGINE KITS", Catalog.CMD_MAIN1_19);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((main1Group = osd.endGroup()));
		osd.createBG(new ResourceRef(MW_Mod:0x68r));
		CurrentName = (CurrentPage * 10);
		local13 = osd.createMenu(local9, 0.8, (-(0.775)), Osd.MD_HORIZONTAL);
		local13.addItem("VEHICLE", Catalog.CMD_VEHI);
		local13 = osd.createMenu(local9, 0.45, (-(0.775)), Osd.MD_HORIZONTAL);
		local13.addItem("CHASSIS", Catalog.CMD_CHAS);
		local13 = osd.createMenu(local11, 0.90000004, 0.1, 0, Osd.MD_VERTICAL);
		VColor_Button = local13.addItem(new ResourceRef(frontend:0xafr), Catalog.CMD_VCOLOR, "Change color");
		VColor_Button.disable();
		CurrentPageText = osd.createText(
			("Current Page: " + (CurrentPage + 1)),
			Frontend.largeFont,
			Text.ALIGN_CENTER,
			(-(0.45)),
			(-(0.86))
		);
		CurrentPageText.changeColor(-4144960);
		CurrentCarName = osd.createText(
			"-NO VEHICLE SELECTED-",
			Frontend.largeFont,
			Text.ALIGN_CENTER,
			0.5,
			(-(0.65000004))
		);
		CurrentCarName.changeColor(-4144960);
		CurrentCarDescription = osd.createTextBox(
			"Description: N/A",
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.089999996,
			0.21000001,
			0.85,
			null,
			10
		);
		CurrentCarDescription.changeColor(-4144960);
		osd.createText(
			"Retail Price:",
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			0.120000005,
			0.77).changeColor(-4144960
		);
		RetailText = osd.createText("N/A", Frontend.smallFont, Text.ALIGN_RIGHT, 0.65000004, 0.77);
		RetailText.changeColor(-4144960);
		osd.createText(
			"Delivery Cost:",
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			0.120000005,
			0.82).changeColor(-4144960
		);
		DeliveryText = osd.createText("N/A", Frontend.smallFont, Text.ALIGN_RIGHT, 0.65000004, 0.82);
		DeliveryText.changeColor(-4144960);
		osd.createText("Total:", Frontend.smallFont, Text.ALIGN_LEFT, 0.120000005, 0.88).changeColor(-4144960);
		TotalText = osd.createText("N/A", Frontend.smallFont, Text.ALIGN_RIGHT, 0.65000004, 0.88);
		TotalText.changeColor(-4144960);
		osd.createText(
			"What you want to have today?",
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			(-(0.45)),
			0.8).changeColor(-4144960
		);
		osd.createText(
			(("We have " + GameLogic.ChassisBox.size()) + " vehicles at catalog."),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			(-(0.45)),
			0.85).changeColor(-4144960
		);
		VehAmount = (GameLogic.ChassisBox.size() - 1);
		local13 = osd.createMenu(local7, (-(0.98)), (-(0.6)), 0);
		if (((CurrentName + 0) <= VehAmount))
		{
			VehButton0 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 0)].vehicleName,
				Catalog.CMD_VEH0
			);
		} else
		{
			VehButton0 = local13.addItem("", Catalog.CMD_VEH0);
			VehButton0.disable();
		}
		if (((CurrentName + 1) <= VehAmount))
		{
			VehButton1 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 1)].vehicleName,
				Catalog.CMD_VEH1
			);
		} else
		{
			VehButton1 = local13.addItem("", Catalog.CMD_VEH1);
			VehButton0.disable();
		}
		if (((CurrentName + 2) <= VehAmount))
		{
			VehButton2 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 2)].vehicleName,
				Catalog.CMD_VEH2
			);
		} else
		{
			VehButton2 = local13.addItem("", Catalog.CMD_VEH2);
			VehButton0.disable();
		}
		if (((CurrentName + 3) <= VehAmount))
		{
			VehButton3 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 3)].vehicleName,
				Catalog.CMD_VEH3
			);
		} else
		{
			VehButton3 = local13.addItem("", Catalog.CMD_VEH3);
			VehButton0.disable();
		}
		if (((CurrentName + 4) <= VehAmount))
		{
			VehButton4 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 4)].vehicleName,
				Catalog.CMD_VEH4
			);
		} else
		{
			VehButton4 = local13.addItem("", Catalog.CMD_VEH4);
			VehButton0.disable();
		}
		if (((CurrentName + 5) <= VehAmount))
		{
			VehButton5 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 5)].vehicleName,
				Catalog.CMD_VEH5
			);
		} else
		{
			VehButton5 = local13.addItem("", Catalog.CMD_VEH5);
			VehButton0.disable();
		}
		if (((CurrentName + 6) <= VehAmount))
		{
			VehButton6 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 6)].vehicleName,
				Catalog.CMD_VEH6
			);
		} else
		{
			VehButton6 = local13.addItem("", Catalog.CMD_VEH6);
			VehButton0.disable();
		}
		if (((CurrentName + 7) <= VehAmount))
		{
			VehButton7 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 7)].vehicleName,
				Catalog.CMD_VEH7
			);
		} else
		{
			VehButton7 = local13.addItem("", Catalog.CMD_VEH7);
			VehButton0.disable();
		}
		if (((CurrentName + 8) <= VehAmount))
		{
			VehButton8 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 8)].vehicleName,
				Catalog.CMD_VEH8
			);
		} else
		{
			VehButton8 = local13.addItem("", Catalog.CMD_VEH8);
			VehButton0.disable();
		}
		if (((CurrentName + 9) <= VehAmount))
		{
			VehButton9 = local13.addItem(
				GameLogic.ChassisBox.elementData[(CurrentName + 9)].vehicleName,
				Catalog.CMD_VEH9
			);
		} else
		{
			VehButton9 = local13.addItem("", Catalog.CMD_VEH9);
			VehButton0.disable();
		}
		local13 = osd.createMenu(local10, (-(0.90000004)), (-(0.82)), Osd.MD_VERTICAL);
		UpButton = local13.addItem(new ResourceRef(frontend:0xafr), Catalog.CMD_UP, "Prev Page");
		local13 = osd.createMenu(local10, (-(0.90000004)), 0.82, Osd.MD_VERTICAL);
		DownButton = local13.addItem(new ResourceRef(frontend:0x84r), Catalog.CMD_DOWN, "Next Page");
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createButton(local12, 0.88, 0.847, Catalog.CMD_VBUY, "Buy this vehicle");
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((VehicleGroup = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x10r));
		local13 = osd.createMenu(local2, 1.0, (-(0.55)), 0.18);
		local13.addItem("BODY PANELS", Catalog.CMD_MAIN2_1);
		local13.addItem("REPLACEMENT PARTS", Catalog.CMD_MAIN2_2);
		local13.addItem("LIGHTS & WINDOWS", Catalog.CMD_MAIN2_3);
		local13.addItem("NEON LIGHTS", Catalog.CMD_MAIN2_4);
		local13.addItem("AERODYNAMIC TUNING", Catalog.CMD_MAIN2_5);
		local13.addItem("OTHERS", Catalog.CMD_MAIN2_6);
		local13.addItem("BODY KITS", Catalog.CMD_MAIN2_7);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((main2Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x11r));
		local13 = osd.createMenu(local8, 0.98, (-(0.6)), 0.15);
		local13.addItem("SUSPENSIONS", Catalog.CMD_MAIN3_1);
		local13.addItem("SHOCK ABSORBERS", Catalog.CMD_MAIN3_2);
		local13.addItem("SPRINGS", Catalog.CMD_MAIN3_3);
		local13.addItem("BRAKE SYSTEMS", Catalog.CMD_MAIN3_4);
		local13.addSeparator();
		local13.addItem("TYRES", Catalog.CMD_MAIN3_5);
		local13.addSeparator();
		local13.addItem("OTHERS", Catalog.CMD_MAIN3_6);
		local13.addItem("PRO TUNNING", Catalog.CMD_MAIN3_7);
		local13.addItem("RUNNING GEAR KITS", Catalog.CMD_MAIN3_8);
		local13 = osd.createMenu(local7, (-(0.98)), (-(0.6)), 0.133);
		local13.addItem("SMALLER RIMS", Catalog.CMD_MAIN3_9);
		local13.addItem("RIMS 15''", Catalog.CMD_MAIN3_10);
		local13.addItem("RIMS 16''", Catalog.CMD_MAIN3_11);
		local13.addItem("RIMS 17''", Catalog.CMD_MAIN3_12);
		local13.addItem("RIMS 18''", Catalog.CMD_MAIN3_13);
		local13.addItem("RIMS 19''", Catalog.CMD_MAIN3_14);
		local13.addItem("RIMS 20''", Catalog.CMD_MAIN3_15);
		local13.addItem("RIMS 21''", Catalog.CMD_MAIN3_16);
		local13.addItem("RIMS 22''", Catalog.CMD_MAIN3_17);
		local13.addItem("BIGGER RIMS ", Catalog.CMD_MAIN3_18);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((main3Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x1er));
		local13 = osd.createMenu(local2, 1.0, (-(0.55)), 0);
		local13.addItem("STEERING WHEELS", Catalog.CMD_MAIN4_1);
		local13.addItem("DASHES", Catalog.CMD_MAIN4_2);
		local13.addItem("GEAR KNOBS", Catalog.CMD_MAIN4_3);
		local13.addItem("PEDALS", Catalog.CMD_MAIN4_4);
		local13.addItem("GAUGES", Catalog.CMD_MAIN4_5);
		local13.addItem("SEATS", Catalog.CMD_MAIN4_6);
		local13.addItem("ROLLBARS", Catalog.CMD_MAIN4_7);
		local13.addItem("DECORATION", Catalog.CMD_MAIN4_8);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((main4Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x1fr));
		local13 = osd.createMenu(local2, 1.0, (-(0.55)), 0.2);
		local13.addItem("HEAD UNITS", Catalog.CMD_MAIN5_1);
		local13.addItem("AMPLIFIERS", Catalog.CMD_MAIN5_2);
		local13.addItem("BOXES", Catalog.CMD_MAIN5_3);
		local13.addItem("SUBWOOFERS", Catalog.CMD_MAIN5_4);
		local13.addItem("ACCESSORIES", Catalog.CMD_MAIN5_5);
		local13.addItem("AUDIO KITS", Catalog.CMD_MAIN5_6);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((main5Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x22r));
		local13 = osd.createMenu(local2, 1.0, (-(0.55)), 0.18);
		local13.addItem("MANUFACTURERS", Catalog.CMD_MAIN6_1);
		local13.addItem("LOGOS", Catalog.CMD_MAIN6_2);
		local13.addItem("NUMBERS", Catalog.CMD_MAIN6_3);
		local13.addItem("DIGITS", Catalog.CMD_MAIN6_4);
		local13.addItem("SMILIES", Catalog.CMD_MAIN6_5);
		local13.addItem("ANIMALS", Catalog.CMD_MAIN6_6);
		local13.addItem("MISC", Catalog.CMD_MAIN6_7);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_FRONTPAGE, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_FRONTPAGE, this);
		osd.hideGroup((main6Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x9r));
		pageNumberL[0] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.96000004)), 0.82);
		pageNumberR[0] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.96000004, 0.82);
		osd.createButton(local4, (-(0.96000004)), 0.92, Catalog.CMD_PREVPARTPAGE, null);
		osd.createButton(local3, 0.96000004, 0.92, Catalog.CMD_NEXTPARTPAGE, null);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_MAIN1, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_MAIN1, this);
		osd.hideGroup((parts1Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x10r));
		pageNumberL[1] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.96000004)), 0.82);
		pageNumberR[1] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.96000004, 0.82);
		osd.createButton(local4, (-(0.96000004)), 0.92, Catalog.CMD_PREVPARTPAGE, null);
		osd.createButton(local3, 0.96000004, 0.92, Catalog.CMD_NEXTPARTPAGE, null);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_MAIN2, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_MAIN2, this);
		osd.hideGroup((parts2Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x11r));
		pageNumberL[2] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.96000004)), 0.82);
		pageNumberR[2] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.96000004, 0.82);
		osd.createButton(local4, (-(0.96000004)), 0.92, Catalog.CMD_PREVPARTPAGE, null);
		osd.createButton(local3, 0.96000004, 0.92, Catalog.CMD_NEXTPARTPAGE, null);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_MAIN3, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_MAIN3, this);
		osd.hideGroup((parts3Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x1er));
		pageNumberL[3] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.96000004)), 0.82);
		pageNumberR[3] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.96000004, 0.82);
		osd.createButton(local4, (-(0.96000004)), 0.92, Catalog.CMD_PREVPARTPAGE, null);
		osd.createButton(local3, 0.96000004, 0.92, Catalog.CMD_NEXTPARTPAGE, null);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_MAIN4, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_MAIN4, this);
		osd.hideGroup((parts4Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x1fr));
		pageNumberL[4] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.96000004)), 0.82);
		pageNumberR[4] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.96000004, 0.82);
		osd.createButton(local4, (-(0.96000004)), 0.92, Catalog.CMD_PREVPARTPAGE, null);
		osd.createButton(local3, 0.96000004, 0.92, Catalog.CMD_NEXTPARTPAGE, null);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_MAIN5, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_MAIN5, this);
		osd.hideGroup((parts5Group = osd.endGroup()));
		osd.createBG(new ResourceRef(misc.catalog:0x22r));
		Style local14 = new Style(0.45, 0.45, Frontend.largeFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
		curDecals = new Decal[8];
		decalButtons = new Button[8];
		decalButtons[0] = osd.createButton(local14, (-(0.75)), (-(0.35000002)), "", Catalog.CMD_DB0);
		decalButtons[1] = osd.createButton(local14, (-(0.25)), (-(0.35000002)), "", Catalog.CMD_DB1);
		decalButtons[2] = osd.createButton(local14, 0.25, (-(0.35000002)), "", Catalog.CMD_DB2);
		decalButtons[3] = osd.createButton(local14, 0.75, (-(0.35000002)), "", Catalog.CMD_DB3);
		decalButtons[4] = osd.createButton(local14, (-(0.75)), 0.4, "", Catalog.CMD_DB4);
		decalButtons[5] = osd.createButton(local14, (-(0.25)), 0.4, "", Catalog.CMD_DB5);
		decalButtons[6] = osd.createButton(local14, 0.25, 0.4, "", Catalog.CMD_DB6);
		decalButtons[7] = osd.createButton(local14, 0.75, 0.4, "", Catalog.CMD_DB7);
		pageNumberL[5] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.96000004)), 0.82);
		pageNumberR[5] = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.96000004, 0.82);
		osd.createButton(local4, (-(0.96000004)), 0.92, Catalog.CMD_PREVDECALPAGE, null);
		osd.createButton(local3, 0.96000004, 0.92, Catalog.CMD_NEXTDECALPAGE, null);
		osd.createButton(local6, 0.08, (-(0.92)), Catalog.CMD_MAIN6, null);
		osd.createButton(local5, 0.92, (-(0.92)), Catalog.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, Catalog.CMD_MAIN6, this);
		osd.hideGroup((decalsGroup = osd.endGroup()));
		refreshMoneyString();
		refreshPage();
	}

	public void changeGroup(int arg0)
	{
		if ((actGroup != arg0))
		{
			if ((actGroup >= 0))
			{
				osd.hideGroup(actGroup);
				if ((actGroup == decalsGroup))
				{
					clearDecalButtons();
				} else
				{
					if (((actGroup >= parts1Group) && (actGroup <= parts5Group)))
					{
						clearObjectCache();
					}
				}
			}
			actGroup = arg0;
			if ((actGroup >= 0))
			{
				osd.showGroup(actGroup);
				osd.changeSelection2((-(1)), 0);
			}
		}
	}

	public void refreshMoneyString()
	{
		new SfxRef(Frontend.SFX_MONEY).play();
		moneytxt.changeText(("$" + Integer.toString(player.money)));
	}

	public void refreshPage()
	{
		if (Catalog.pageNumberPrefix)
		{
			pgNumberL.changeText((Catalog.pageNumberPrefix + curpage));
			pgNumberR.changeText((Catalog.pageNumberPrefix + (curpage + 1)));
		}
	}

	public void decalButtonPressed(int arg0)
	{
		if (((showDecals + arg0) < decals.size()))
		{
			int local2 = Catalog.DECALPRICE;
			if ((local2 <= player.money))
			{
				Decal local3 = decals.elementAt((showDecals + arg0));
				Dialog local4 = new YesNoDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"BUY DECAL SET",
					(("Do you want to buy this decal set for $" + local2) + " ?\n(Each set contains 5 decals)")
				);
				if ((local4.display() == 0))
				{
					player.money -= local2;
					player.decals.addElement(new Decal(local3.id()));
					refreshMoneyString();
				}
			} else
			{
				new WarningDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"NOT ENOUGHT MONEY",
					(("You don't have $" + local2) + " to buy a decal!")).display(
				);
			}
		}
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == Catalog.CMD_VCOLOR))
		{
			if (CurrentCar)
			{
				if ((ColorIDX == (GameLogic.CARCOLORS.length - 1)))
				{
					ColorIDX = 0;
				} else
				{
					ColorIDX += 1;
				}
				CurrentCar.command((("texture " + GameLogic.CARCOLORS[ColorIDX]) + " 1"));
				VColor_Button.changeTexture(new ResourceRef(GameLogic.CARCOLORS[ColorIDX]));
			}
		}
		if ((arg0 == Catalog.CMD_VBUY))
		{
			if ((TotalP <= player.money))
			{
				String local2;
				if ((CompleteCar > 0))
				{
					local2 = CurrentCar.chassis.vehicleName;
				} else
				{
					local2 = CurrentCar.chassis.name;
				}
				int local3;
				if (GameLogic.player.car)
				{
					GameLogic.player.carlot.lockPlayerCar();
					local3 = GameLogic.player.carlot.getFreeSlot();
					GameLogic.player.carlot.releasePlayerCar();
					GameLogic.garage.StartPos = new Vector3(0, 0, 0);
					GameLogic.garage.StartYpr = new Ypr(0, 0, 0);
				} else
				{
					local3 = 1;
				}
				if ((local3 >= 0))
				{
					Dialog local4 = new NoYesDialog(
						player.controller,
						(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
						"BUY CAR",
						(((("Do you want to buy this " + local2) + " for $") + TotalP) + " ?")
					);
					if ((local4.display() == 0))
					{
						player.money -= TotalP;
						refreshMoneyString();
						player.carlot.addCar(player.car);
						player.carlot.saveCar(player.carlot.curcar);
						player.carlot.flushCars();
						if (File.exists("save/temp/CopyCar"))
						{
							File.delete("save/temp/CopyCar");
							File.delete("save/temp/CopyCar.*");
						}
						CurrentCar.save("save/temp/CopyCar");
						if (CurrentCar)
						{
							CurrentCar.destroy();
						}
						if (CurrentChassis)
						{
							CurrentChassis.destroy();
						}
						GameLogic.player.car = Vehicle.load("save/temp/CopyCar", GameLogic.player);
						changeGroup((-(1)));
						GameLogic.changeActiveSection(GameLogic.garage);
					}
				} else
				{
					new WarningDialog(
						player.controller,
						(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
						"CAR LOT FULL",
						"There is no more free space in your car lot. \n Sell some cars to free up parking space!").display(
					);
				}
			} else
			{
				new WarningDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"NOT ENOUGHT MONEY",
					(("You don't have $" + TotalP) + " to buy this car!")).display(
				);
			}
		}
		if ((arg0 == Catalog.CMD_CHAS))
		{
			if ((CompleteCar > 0))
			{
				if (CurrentCar)
				{
					CurrentCar.destroy();
				}
				if (CurrentChassis)
				{
					CurrentChassis.destroy();
				}
				CurrentCar = new Vehicle();
				CurrentCar.chassis = new Chassis();
				CurrentCar.chassis = create(
					CarScene,
					new GameRef(GameLogic.ChassisRoot[GameLogic.ChassisResNoumber[(CurrentName + ButtonNum)]]),
					"0,0,0,0,0,0,",
					"chassis"
				);
				CurrentCar = new Vehicle(CurrentCar.chassis);
				CurrentCarName.changeText(CurrentCar.chassis.vehicleName);
				CurrentCarDescription.changeText(CurrentCar.chassis.description);
				RetailP = (CurrentCar.getTotalPrice() * Catalog.PRICERATIO);
				RetailText.changeText(("$" + RetailP));
				DeliveryP = (CurrentCar.chassis.getMass() * 5.8);
				DeliveryText.changeText(("$" + DeliveryP));
				TotalP = (RetailP + DeliveryP);
				TotalText.changeText(("$" + TotalP));
				VColor_Button.disable();
				VColor_Button.changeTexture(new ResourceRef(frontend:0xafr));
			}
			CompleteCar = 0;
		}
		if ((arg0 == Catalog.CMD_VEHI))
		{
			if (((CompleteCar == 0) && CurrentCar))
			{
				CurrentCar.chassis.addStockParts(new Descriptor(CurrentCar.chassis.getTexture(), 1, 1, 1, 1));
				CurrentCar.chassis.command("reset");
				CurrentCar.chassis.command("setsteer -0.6");
				RetailP = (CurrentCar.getTotalPrice() * Catalog.PRICERATIO);
				RetailText.changeText(("$" + RetailP));
				DeliveryP = (CurrentCar.chassis.getMass() * 5.8);
				DeliveryText.changeText(("$" + DeliveryP));
				TotalP = (RetailP + DeliveryP);
				TotalText.changeText(("$" + TotalP));
				VColor_Button.enable();
				VColor_Button.changeTexture(new ResourceRef(CurrentCar.chassis.getTexture()));
			}
			CompleteCar = 1;
		}
		if (((arg0 >= Catalog.CMD_VEH0) && (arg0 <= Catalog.CMD_VEH9)))
		{
			if ((arg0 == Catalog.CMD_VEH0))
			{
				ButtonNum = 0;
			}
			if ((arg0 == Catalog.CMD_VEH1))
			{
				ButtonNum = 1;
			}
			if ((arg0 == Catalog.CMD_VEH2))
			{
				ButtonNum = 2;
			}
			if ((arg0 == Catalog.CMD_VEH3))
			{
				ButtonNum = 3;
			}
			if ((arg0 == Catalog.CMD_VEH4))
			{
				ButtonNum = 4;
			}
			if ((arg0 == Catalog.CMD_VEH5))
			{
				ButtonNum = 5;
			}
			if ((arg0 == Catalog.CMD_VEH6))
			{
				ButtonNum = 6;
			}
			if ((arg0 == Catalog.CMD_VEH7))
			{
				ButtonNum = 7;
			}
			if ((arg0 == Catalog.CMD_VEH8))
			{
				ButtonNum = 8;
			}
			if ((arg0 == Catalog.CMD_VEH9))
			{
				ButtonNum = 9;
			}
			if (CurrentCar)
			{
				CurrentCar.destroy();
			}
			if (CurrentChassis)
			{
				CurrentChassis.destroy();
			}
			CurrentCar = new Vehicle();
			CurrentCar.chassis = new Chassis();
			CurrentCar.chassis = create(
				CarScene,
				new GameRef(GameLogic.ChassisRoot[GameLogic.ChassisResNoumber[(CurrentName + ButtonNum)]]),
				"0,0,0,0,0,0,",
				"chassis"
			);
			CurrentCar = new Vehicle(CurrentCar.chassis);
			Size = CurrentCar.chassis.getMin();
			Cam.command((((((((("move " + CarScene.id()) + " ") + (Size.x * 2.2)) + ",") + (Size.y * 2.2)) + ",") + (Size.z * 2.2)) + " 1.0 0, -0.2, 0"));
			if (CompleteCar)
			{
				CurrentCar.chassis.addStockParts(new Descriptor(CurrentCar.chassis.getTexture(), 1, 1, 1, 1));
				CurrentCar.chassis.command("reset");
				CurrentCar.chassis.command("setsteer -0.6");
				VColor_Button.enable();
				VColor_Button.changeTexture(new ResourceRef(CurrentCar.chassis.getTexture()));
			}
			CurrentCarName.changeText(CurrentCar.chassis.vehicleName);
			CurrentCarDescription.changeText(CurrentCar.chassis.description);
			RetailP = (CurrentCar.getTotalPrice() * Catalog.PRICERATIO);
			RetailText.changeText(("$" + RetailP));
			DeliveryP = (CurrentCar.chassis.getMass() * 5.8);
			DeliveryText.changeText(("$" + DeliveryP));
			TotalP = (RetailP + DeliveryP);
			TotalText.changeText(("$" + TotalP));
		} else
		{
			if ((arg0 == Catalog.CMD_UP))
			{
				if ((CurrentPage > 0))
				{
					CurrentPage -= 1;
				}
				CurrentName = (CurrentPage * 10);
				CurrentPageText.changeText(("Current Page: " + (CurrentPage + 1)));
				VehAmount = (GameLogic.ChassisBox.size() - 1);
				DownButton.changeTexture(new ResourceRef(frontend:0x84r));
				DownButton.enable();
				if ((CurrentPage == 0))
				{
					UpButton.changeTexture(new ResourceRef(frontend:0xafr));
					UpButton.disable();
				}
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
			} else
			{
				if ((arg0 == Catalog.CMD_DOWN))
				{
					if ((GameLogic.ChassisRoot.length >= (CurrentName + 10)))
					{
						CurrentPage += 1;
					}
					CurrentName = (CurrentPage * 10);
					CurrentPageText.changeText(("Current Page: " + (CurrentPage + 1)));
					UpButton.changeTexture(new ResourceRef(frontend:0x4cr));
					UpButton.enable();
					VehAmount = (GameLogic.ChassisBox.size() - 1);
					if ((VehAmount < (CurrentName + 10)))
					{
						DownButton.changeTexture(new ResourceRef(frontend:0xafr));
						DownButton.disable();
					}
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
				} else
				{
					if ((arg0 == Catalog.CMD_EXIT))
					{
						changeGroup((-(1)));
						GameLogic.changeActiveSection(GameLogic.garage);
						if (CurrentCar)
						{
							CurrentCar.destroy();
						}
						if (CurrentChassis)
						{
							CurrentChassis.destroy();
						}
					} else
					{
						if ((arg0 == Catalog.CMD_FRONTPAGE))
						{
							changeGroup(mainGroup);
							if (CurrentCar)
							{
								CurrentCar.destroy();
							}
							if (CurrentChassis)
							{
								CurrentChassis.destroy();
							}
						} else
						{
							if ((arg0 == Catalog.CMD_VEHICLE))
							{
								changeGroup(VehicleGroup);
								VehicleScene();
							} else
							{
								if ((arg0 == Catalog.CMD_MAIN1))
								{
									pgNumberL = pageNumberL[0];
									pgNumberR = pageNumberR[0];
									changeGroup(main1Group);
								} else
								{
									if ((arg0 == Catalog.CMD_MAIN2))
									{
										pgNumberL = pageNumberL[1];
										pgNumberR = pageNumberR[1];
										changeGroup(main2Group);
									} else
									{
										if ((arg0 == Catalog.CMD_MAIN3))
										{
											pgNumberL = pageNumberL[2];
											pgNumberR = pageNumberR[2];
											changeGroup(main3Group);
										} else
										{
											if ((arg0 == Catalog.CMD_MAIN4))
											{
												pgNumberL = pageNumberL[3];
												pgNumberR = pageNumberR[3];
												changeGroup(main4Group);
											} else
											{
												if ((arg0 == Catalog.CMD_MAIN5))
												{
													pgNumberL = pageNumberL[4];
													pgNumberR = pageNumberR[4];
													changeGroup(main5Group);
												} else
												{
													if ((arg0 == Catalog.CMD_MAIN6))
													{
														pgNumberL = pageNumberL[5];
														pgNumberR = pageNumberR[5];
														changeGroup(main6Group);
													} else
													{
														if (((arg0 >= Catalog.CMD_DB0) && (arg0 <= Catalog.CMD_DB7)))
														{
															decalButtonPressed((arg0 - Catalog.CMD_DB0));
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
		if (((arg0 >= Catalog.CMD_MAIN6_1) && (arg0 <= Catalog.CMD_MAIN6_7)))
		{
			String local2_2;
			if ((arg0 == Catalog.CMD_MAIN6_1))
			{
				Catalog.pageNumberPrefix = "D-I/";
				local2_2 = "manufacturers";
			} else
			{
				if ((arg0 == Catalog.CMD_MAIN6_2))
				{
					Catalog.pageNumberPrefix = "D-II/";
					local2_2 = "logos";
				} else
				{
					if ((arg0 == Catalog.CMD_MAIN6_3))
					{
						Catalog.pageNumberPrefix = "D-III/";
						local2_2 = "numbers";
					} else
					{
						if ((arg0 == Catalog.CMD_MAIN6_4))
						{
							Catalog.pageNumberPrefix = "D-IV/";
							local2_2 = "digits";
						} else
						{
							if ((arg0 == Catalog.CMD_MAIN6_5))
							{
								Catalog.pageNumberPrefix = "D-V/";
								local2_2 = "smilies";
							} else
							{
								if ((arg0 == Catalog.CMD_MAIN6_6))
								{
									Catalog.pageNumberPrefix = "D-VI/";
									local2_2 = "animals";
								} else
								{
									if ((arg0 == Catalog.CMD_MAIN6_7))
									{
										Catalog.pageNumberPrefix = "D-VII/";
										local2_2 = "misc";
									}
								}
							}
						}
					}
				}
			}
			decals = collectDecals(local2_2);
			curpage = 1;
			showDecals = 0;
			setDecalButtons();
			refreshPage();
			changeGroup(decalsGroup);
		} else
		{
			if ((arg0 == Catalog.CMD_PREVDECALPAGE))
			{
				if ((showDecals >= decalButtons.length))
				{
					showDecals -= decalButtons.length;
					setDecalButtons();
					curpage -= 2;
					refreshPage();
				}
			} else
			{
				if ((arg0 == Catalog.CMD_NEXTDECALPAGE))
				{
					int local2_3 = decals.size();
					if (((showDecals + decalButtons.length) < local2_3))
					{
						showDecals += decalButtons.length;
						setDecalButtons();
						curpage += 2;
						refreshPage();
					}
				} else
				{
					if ((arg0 == Catalog.CMD_PREVPARTPAGE))
					{
						if (inventory.upScroll())
						{
							curpage -= 2;
							refreshPage();
						}
					} else
					{
						if ((arg0 == Catalog.CMD_NEXTPARTPAGE))
						{
							collectObjectsStep(8);
							if (inventory.downScroll())
							{
								curpage += 2;
								refreshPage();
							}
						} else
						{
							if ((arg0 == Catalog.NO_FILTER))
							{
								inventory.items.addElement(new InventoryItem_Part(inventory, parts.id()));
								step--;
							} else
							{
								if ((arg0 == Catalog.RIMS_FILTER))
								{
									Wheel local2_4 = new Wheel();
									local2_4 = create(null, new GameRef(parts), "0,0,0,0,0,0,", "temp rim");
									if ((Rim_size <= 14))
									{
										if ((local2_4.getRadius() <= 14))
										{
											inventory.items.addElement(new InventoryItem_Part(inventory, parts.id()));
											step--;
										}
									} else
									{
										if ((Rim_size >= 23))
										{
											if ((local2_4.getRadius() >= 23))
											{
												inventory.items.addElement(new InventoryItem_Part(inventory, parts.id()));
												step--;
											}
										} else
										{
											if (((local2_4.getRadius() >= Rim_size) && (local2_4.getRadius() < (Rim_size + 1))))
											{
												inventory.items.addElement(new InventoryItem_Part(inventory, parts.id()));
												step--;
											}
										}
									}
								} else
								{
									if (((arg0 >= Catalog.CMD_MAIN1_1) && (arg0 < Catalog.CMD_MAIN6_1)))
									{
										collectObjectsBegin();
										if (((arg0 >= Catalog.CMD_MAIN1_1) && (arg0 < Catalog.CMD_MAIN2_1)))
										{
											changeGroup(parts1Group);
											if ((arg0 == Catalog.CMD_MAIN1_1))
											{
												Catalog.pageNumberPrefix = "E-I/";
												collectObjects(new GameRef(parts:0x25cr));
												filter = Catalog.NO_FILTER;
											} else
											{
												if ((arg0 == Catalog.CMD_MAIN1_2))
												{
													Catalog.pageNumberPrefix = "E-II/";
													collectObjects(new GameRef(parts:0x25er));
													filter = Catalog.NO_FILTER;
												} else
												{
													if ((arg0 == Catalog.CMD_MAIN1_3))
													{
														Catalog.pageNumberPrefix = "E-III/";
														collectObjects(new GameRef(parts:0xb7r));
														filter = Catalog.NO_FILTER;
													} else
													{
														if ((arg0 == Catalog.CMD_MAIN1_4))
														{
															Catalog.pageNumberPrefix = "E-IV/";
															collectObjects(new GameRef(parts:0xb6r));
															filter = Catalog.NO_FILTER;
														} else
														{
															if ((arg0 == Catalog.CMD_MAIN1_5))
															{
																Catalog.pageNumberPrefix = "E-V/";
																collectObjects(new GameRef(parts:0x252r));
																filter = Catalog.NO_FILTER;
															} else
															{
																if ((arg0 == Catalog.CMD_MAIN1_6))
																{
																	Catalog.pageNumberPrefix = "E-VI/";
																	collectObjects(new GameRef(parts:0x25dr));
																	filter = Catalog.NO_FILTER;
																} else
																{
																	if ((arg0 == Catalog.CMD_MAIN1_7))
																	{
																		Catalog.pageNumberPrefix = "E-VII/";
																		collectObjects(new GameRef(parts:0x25fr));
																		filter = Catalog.NO_FILTER;
																	} else
																	{
																		if ((arg0 == Catalog.CMD_MAIN1_8))
																		{
																			Catalog.pageNumberPrefix = "E-VIII/";
																			collectObjects(
																				new GameRef(parts:0x255r),
																				new GameRef(parts:0x26fr)
																			);
																			filter = Catalog.NO_FILTER;
																		} else
																		{
																			if ((arg0 == Catalog.CMD_MAIN1_9))
																			{
																				Catalog.pageNumberPrefix = "E-IX/";
																				collectObjects(
																					new GameRef(parts:0x26br),
																					new GameRef(parts:0x26er)
																				);
																				filter = Catalog.NO_FILTER;
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
											if ((arg0 == Catalog.CMD_MAIN1_10))
											{
												Catalog.pageNumberPrefix = "E-X/";
												collectObjects(
													new GameRef(parts:0x249r),
													new GameRef(parts:0x23cr)
												);
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_11))
											{
												Catalog.pageNumberPrefix = "E-XI/";
												collectObjects(
													new GameRef(parts:0x24ar),
													new GameRef(parts:0x23br)
												);
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_12))
											{
												Catalog.pageNumberPrefix = "E-XII/";
												collectObjects(
													new GameRef(parts:0x282r),
													new GameRef(parts:0x250r)
												);
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_13))
											{
												Catalog.pageNumberPrefix = "E-XIII/";
												collectObjects(new GameRef(parts:0xc9r));
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_14))
											{
												Catalog.pageNumberPrefix = "E-XIIV/";
												collectObjects(new GameRef(parts:0xber));
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_15))
											{
												Catalog.pageNumberPrefix = "E-XIV/";
												collectObjects(
													new GameRef(parts:0x10r),
													new GameRef(parts:0x12r)
												);
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_16))
											{
												Catalog.pageNumberPrefix = "E-XVI/";
												collectObjects(
													new GameRef(parts:0xbdr),
													new GameRef(parts:0x281r),
													new GameRef(parts:0x279r)
												);
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_17))
											{
												Catalog.pageNumberPrefix = "E-XVII/";
												collectObjects(new GameRef(parts:0x277r));
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_18))
											{
												Catalog.pageNumberPrefix = "E-XVIII/";
												collectObjects(new GameRef(parts:0xf27dr));
												filter = Catalog.NO_FILTER;
											}
											if ((arg0 == Catalog.CMD_MAIN1_19))
											{
												Catalog.pageNumberPrefix = "E-XIX/";
												collectObjects(new GameRef(parts:0xf23cr));
												filter = Catalog.NO_FILTER;
											}
										} else
										{
											if (((arg0 >= Catalog.CMD_MAIN2_1) && (arg0 < Catalog.CMD_MAIN3_1)))
											{
												changeGroup(parts2Group);
												if ((arg0 == Catalog.CMD_MAIN2_1))
												{
													Catalog.pageNumberPrefix = "B-I/";
													collectObjects(new GameRef(parts:0xf242r));
													filter = Catalog.NO_FILTER;
												} else
												{
													if ((arg0 == Catalog.CMD_MAIN2_2))
													{
														Catalog.pageNumberPrefix = "B-II/";
														collectObjects(new GameRef(parts:0xf24fr));
														filter = Catalog.NO_FILTER;
													} else
													{
														if ((arg0 == Catalog.CMD_MAIN2_3))
														{
															Catalog.pageNumberPrefix = "B-III/";
															collectObjects(new GameRef(parts:0xf24cr));
															filter = Catalog.NO_FILTER;
														} else
														{
															if ((arg0 == Catalog.CMD_MAIN2_4))
															{
																Catalog.pageNumberPrefix = "B-IV/";
																collectObjects(new GameRef(parts:0xf24br));
																filter = Catalog.NO_FILTER;
															} else
															{
																if ((arg0 == Catalog.CMD_MAIN2_5))
																{
																	Catalog.pageNumberPrefix = "B-V/";
																	collectObjects(new GameRef(parts:0xf233r));
																	filter = Catalog.NO_FILTER;
																} else
																{
																	if ((arg0 == Catalog.CMD_MAIN2_6))
																	{
																		Catalog.pageNumberPrefix = "B-VI/";
																		collectObjects(new GameRef(parts:0xf228r));
																		filter = Catalog.NO_FILTER;
																	} else
																	{
																		if ((arg0 == Catalog.CMD_MAIN2_7))
																		{
																			Catalog.pageNumberPrefix = "B-VII/";
																			collectObjects(new GameRef(parts:0xf23br));
																			filter = Catalog.NO_FILTER;
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
										if (((arg0 >= Catalog.CMD_MAIN3_1) && (arg0 < Catalog.CMD_MAIN4_1)))
										{
											changeGroup(parts3Group);
											if (((arg0 >= Catalog.CMD_MAIN3_9) && (arg0 <= Catalog.CMD_MAIN3_18)))
											{
												collectObjectsBegin();
												collectObjects(new GameRef(parts:0xf235r));
												filter = Catalog.RIMS_FILTER;
												int local2_5;
												if ((arg0 == Catalog.CMD_MAIN3_9))
												{
													local2_5 = 14;
												}
												if ((arg0 == Catalog.CMD_MAIN3_10))
												{
													local2_5 = 15;
												}
												if ((arg0 == Catalog.CMD_MAIN3_11))
												{
													local2_5 = 16;
												}
												if ((arg0 == Catalog.CMD_MAIN3_12))
												{
													local2_5 = 17;
												}
												if ((arg0 == Catalog.CMD_MAIN3_13))
												{
													local2_5 = 18;
												}
												if ((arg0 == Catalog.CMD_MAIN3_14))
												{
													local2_5 = 19;
												}
												if ((arg0 == Catalog.CMD_MAIN3_15))
												{
													local2_5 = 20;
												}
												if ((arg0 == Catalog.CMD_MAIN3_16))
												{
													local2_5 = 21;
												}
												if ((arg0 == Catalog.CMD_MAIN3_17))
												{
													local2_5 = 22;
												}
												if ((arg0 == Catalog.CMD_MAIN3_18))
												{
													local2_5 = 23;
												}
												Rim_size = local2_5;
												Catalog.pageNumberPrefix = (("R-R" + local2_5) + "''/");
											} else
											{
												if ((arg0 == Catalog.CMD_MAIN3_1))
												{
													Catalog.pageNumberPrefix = "R-I/";
													collectObjects(new GameRef(parts:0xf229r));
													filter = Catalog.NO_FILTER;
												} else
												{
													if ((arg0 == Catalog.CMD_MAIN3_2))
													{
														Catalog.pageNumberPrefix = "R-II/";
														collectObjects(new GameRef(parts:0x1cr));
														filter = Catalog.NO_FILTER;
													} else
													{
														if ((arg0 == Catalog.CMD_MAIN3_3))
														{
															Catalog.pageNumberPrefix = "R-III/";
															collectObjects(new GameRef(parts:0xf22ar));
															filter = Catalog.NO_FILTER;
														} else
														{
															if ((arg0 == Catalog.CMD_MAIN3_4))
															{
																Catalog.pageNumberPrefix = "R-IV/";
																collectObjects(new GameRef(parts:0xf22dr));
																filter = Catalog.NO_FILTER;
															} else
															{
																if ((arg0 == Catalog.CMD_MAIN3_5))
																{
																	Catalog.pageNumberPrefix = "R-V/";
																	collectObjects(new GameRef(parts:0xf23er));
																	filter = Catalog.NO_FILTER;
																} else
																{
																	if ((arg0 == Catalog.CMD_MAIN3_6))
																	{
																		Catalog.pageNumberPrefix = "R-VI/";
																		collectObjects(new GameRef(parts:0xd120r));
																		filter = Catalog.NO_FILTER;
																	} else
																	{
																		if ((arg0 == Catalog.CMD_MAIN3_7))
																		{
																			Catalog.pageNumberPrefix = "R-VII/";
																			collectObjects(new GameRef(parts:0xd121r));
																			filter = Catalog.NO_FILTER;
																		}
																	}
																}
															}
														}
													}
												}
											}
											if ((arg0 == Catalog.CMD_MAIN3_8))
											{
												Catalog.pageNumberPrefix = "R-VIII/";
												collectObjects(new GameRef(parts:0xf249r));
												filter = Catalog.NO_FILTER;
											}
										} else
										{
											if (((arg0 >= Catalog.CMD_MAIN4_1) && (arg0 < Catalog.CMD_MAIN5_1)))
											{
												changeGroup(parts4Group);
												if ((arg0 == Catalog.CMD_MAIN4_1))
												{
													Catalog.pageNumberPrefix = "I-I/";
													collectObjects(new GameRef(parts:0xf243r));
													filter = Catalog.NO_FILTER;
												} else
												{
													if ((arg0 == Catalog.CMD_MAIN4_2))
													{
														Catalog.pageNumberPrefix = "I-II/";
														collectObjects(new GameRef(parts:0xf245r));
														filter = Catalog.NO_FILTER;
													} else
													{
														if ((arg0 == Catalog.CMD_MAIN4_3))
														{
															Catalog.pageNumberPrefix = "I-III/";
															collectObjects(new GameRef(parts:0xf23fr));
															filter = Catalog.NO_FILTER;
														} else
														{
															if ((arg0 == Catalog.CMD_MAIN4_4))
															{
																Catalog.pageNumberPrefix = "I-IV/";
																collectObjects(new GameRef(parts:0xf241r));
																filter = Catalog.NO_FILTER;
															} else
															{
																if ((arg0 == Catalog.CMD_MAIN4_5))
																{
																	Catalog.pageNumberPrefix = "I-V/";
																	collectObjects(new GameRef(parts:0xf244r));
																	filter = Catalog.NO_FILTER;
																} else
																{
																	if ((arg0 == Catalog.CMD_MAIN4_6))
																	{
																		Catalog.pageNumberPrefix = "I-VI/";
																		collectObjects(new GameRef(parts:0xf246r));
																		filter = Catalog.NO_FILTER;
																	} else
																	{
																		if ((arg0 == Catalog.CMD_MAIN4_7))
																		{
																			Catalog.pageNumberPrefix = "I-VII/";
																			collectObjects(new GameRef(parts:0xf247r));
																			filter = Catalog.NO_FILTER;
																		} else
																		{
																			if ((arg0 == Catalog.CMD_MAIN4_8))
																			{
																				Catalog.pageNumberPrefix = "I-VIII/";
																				collectObjects(new GameRef(parts:0xf248r));
																				filter = Catalog.NO_FILTER;
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
										if (((arg0 >= Catalog.CMD_MAIN5_1) && (arg0 < Catalog.CMD_MAIN6_1)))
										{
											changeGroup(parts5Group);
											if ((arg0 == Catalog.CMD_MAIN5_1))
											{
												Catalog.pageNumberPrefix = "A-I/";
												collectObjects(new GameRef(parts:0xf240r));
												filter = Catalog.NO_FILTER;
											} else
											{
												if ((arg0 == Catalog.CMD_MAIN5_2))
												{
													Catalog.pageNumberPrefix = "A-II/";
													collectObjects(new GameRef(parts:0x246r));
													filter = Catalog.NO_FILTER;
												} else
												{
													if ((arg0 == Catalog.CMD_MAIN5_3))
													{
														Catalog.pageNumberPrefix = "A-III/";
														collectObjects(new GameRef(parts:0x247r));
														filter = Catalog.NO_FILTER;
													} else
													{
														if ((arg0 == Catalog.CMD_MAIN5_4))
														{
															Catalog.pageNumberPrefix = "A-IV/";
															collectObjects(new GameRef(parts:0x248r));
															filter = Catalog.NO_FILTER;
														} else
														{
															if ((arg0 == Catalog.CMD_MAIN5_5))
															{
																Catalog.pageNumberPrefix = "A-V/";
																collectObjects(new GameRef(parts:0x24br));
																filter = Catalog.NO_FILTER;
															} else
															{
																if ((arg0 == Catalog.CMD_MAIN5_6))
																{
																	Catalog.pageNumberPrefix = "A-VI/";
																	collectObjects(new GameRef(parts:0x25br));
																	filter = Catalog.NO_FILTER;
																}
															}
														}
													}
												}
											}
										}
										collectObjectsEnd();
										refreshPage();
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

public class CatalogInventory extends VisualInventory
{
	Osd osd;
	TextBox[] names;
	Catalog catalog;

	public CatalogInventory(Catalog arg0, Player arg1, float arg2, float arg3, float arg4, float arg5)
	{
		super(arg1, arg2, arg3, arg4, arg5);
		catalog = arg0;
	}

	public void initVisuals(float arg0, float arg1, float arg2, float arg3)
	{
		linesPerPage = 2;
		partsPerLine = 4;
		cline = 0;
		start = (cline * partsPerLine);
		stop = (start + (linesPerPage * partsPerLine));
		float local5 = 0.2;
		float local6 = 0.15;
		float local7 = ((arg2 - (local5 * partsPerLine)) / partsPerLine);
		float local8 = ((arg3 - (local6 * linesPerPage)) / (linesPerPage - 1));
		panels = new InventoryPanel[(partsPerLine * linesPerPage)];
		names = new TextBox[panels.length];
		osd = new Osd(1.0, 0.0, 11);
		osd.iLevel = Osd.IL_NONE;
		int local9;
		float local10 = arg1;
		int local11 = 0;
		while ((local11 < linesPerPage))
		{
			float local12 = arg0;
			int local13 = 0;
			while ((local13 < partsPerLine))
			{
				if ((local13 == (partsPerLine / 2)))
				{
					local12 += local7;
				}
				local9 = ((local11 * partsPerLine) + local13);
				panels[local9] = new CatalogInventoryPanel(
					this,
					local9,
					local12,
					local10,
					local5,
					local6,
					player
				);
				names[local9] = osd.createTextBox(
					null,
					Frontend.smallFont,
					Text.ALIGN_CENTER,
					((local12 * 2) - 1),
					(((local10 + local6) * 2) - 1),
					(local5 * 2)
				);
				if (((Frontend.smallFont.id() == Text.RID_CONSOLE10) || (Frontend.smallFont.id() == Text.RID_CONSOLE5)))
				{
					names[local9].changeColor(-16777216);
				}
				local12 += (local5 + local7);
				local13++;
			}
			local10 += (local6 + local8);
			local11++;
		}
	}

	public int upScroll()
	{
		if (cline)
		{
			cline -= linesPerPage;
			update();
			return 1;
		}
		return 0;
	}

	public int downScroll()
	{
		if (((cline + linesPerPage) < (pages() * linesPerPage)))
		{
			cline += linesPerPage;
			update();
			return 1;
		}
		return 0;
	}

	public int pages()
	{
		if ((items.size() > 1))
		{
			return (((items.size() - 1) / (partsPerLine * linesPerPage)) + 1);
		}
		return 1;
	}

	public void panelLeftClick(int arg0)
	{
		arg0 += (currentLine() * partsPerLine);
		if ((arg0 < items.size()))
		{
			InventoryItem local2 = items.elementAt(arg0);
			int local3 = (local2.getPrice() * Catalog.PRICERATIO);
			if ((local3 <= player.money))
			{
				Dialog local4 = new BuyCatalogItemDialog(
					player.controller,
					((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
					local2,
					local3
				);
				if ((local4.display() == 0))
				{
					player.money -= local3;
					local2.copyToInventory(player.parts);
					catalog.refreshMoneyString();
					int local5 = local2.getLogo();
					if (local5)
					{
						player.decals.addElement(new Decal(local5));
					}
				}
			} else
			{
				new WarningDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"NOT ENOUGHT MONEY",
					"You don't have enought money to buy this part!").display(
				);
			}
		}
	}

	public void update()
	{
		int local1 = start;
		int local2 = stop;
		start = (cline * partsPerLine);
		stop = (start + (linesPerPage * partsPerLine));
		int local3;
		int local4;
		local4 = 0;
		local3 = local1;
		while ((local3 < local2))
		{
			names[local4++].changeText(null);
			local3++;
		}
		local4 = 0;
		local3 = start;
		while ((local3 < stop))
		{
			if ((local3 < items.size()))
			{
				InventoryItem local5 = items.elementAt(local3);
				names[local4++].changeText(((local5.getName() + " $") + ((int) (local5.getPrice() * Catalog.PRICERATIO))));
			}
			local3++;
		}
		super.update();
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

}

public class CatalogInventoryPanel extends InventoryPanel
{
	Player player;
	ResourceRef infoTex0;
	ResourceRef infoTex1;
	Rectangle infoRect0;
	Rectangle infoRect1;

	public CatalogInventoryPanel(CatalogInventory arg0, int arg1, float arg2, float arg3, float arg4, float arg5, Player arg6)
	{
		super(arg0, arg1, arg2, arg3, arg4, arg5);
		infoRect0 = arg0.osd.createRectangle(
			(((arg2 + (arg4 / 2)) * 2) - 1),
			(((arg3 - 0.04) * 2) - 1),
			0.1,
			0.1,
			1.0,
			(-(0.5)),
			0.0,
			0,
			null
		);
		infoRect1 = arg0.osd.createRectangle(
			(((arg2 + (arg4 / 2)) * 2) - 1),
			(((arg3 - 0.04) * 2) - 1),
			0.1,
			0.1,
			1.0,
			0.5,
			0.0,
			0,
			null
		);
		player = arg6;
	}

	public void swap(int arg0, int arg1)
	{
	}

	public void attachItem(InventoryItem arg0)
	{
		ypr = new Ypr((-(1.4)), (-(0.7)), 0.0);
		super.attachItem(arg0);
		infoTex0 = (infoTex1 = null);
		if ((arg0 && (arg0 instanceof InventoryItem_Part)))
		{
			((InventoryItem_Part) arg0).compatibility = 0;
			Part local2 = arg0.getPart();
			if (player.car)
			{
				if (local2.getInfo(local2.GII_COMPATIBLE, (player.car.id() + "")))
				{
					if (local2.getInfo(local2.GII_INSTALL_OK, (player.car.id() + "")))
					{
						infoTex0 = Catalog.RR_ICON_CAR_1STEP;
						((InventoryItem_Part) arg0).compatibility |= 1;
					} else
					{
						infoTex0 = Catalog.RR_ICON_CAR_COMP;
						((InventoryItem_Part) arg0).compatibility |= 2;
					}
				}
			}
			int local3;
			int local4 = (player.parts.size() - 1);
			while ((local4 >= 0))
			{
				if ((player.parts.items.elementAt(local4) instanceof InventoryItem_Part))
				{
					if (local2.getInfo(local2.GII_INSTALL_OK, (player.parts.items.elementAt(local4).getPart().id() + "")))
					{
						local3++;
						((InventoryItem_Part) arg0).compatibility |= 4;
						break;
					}
				}
				local4--;
			}
			if (local3)
			{
				infoTex1 = Catalog.RR_ICON_INV_COMP;
			}
		}
		infoRect0.changeTexture(infoTex0);
		infoRect1.changeTexture(infoTex1);
	}

}

public class BuyCatalogItemDialog extends YesNoDialog
{
	public BuyCatalogItemDialog(Controller arg0, int arg1, InventoryItem arg2, int arg3)
	{
		super(arg0, arg1, "BUY PART", genBody(arg2, arg3));
	}

	public String genBody(InventoryItem arg0, int arg1)
	{
		String local3 = (((((("Do you want to buy this " + arg0.getName()) + " for $") + arg1) + " ? \n \n \n") + arg0.getDescription()) + " \n \n Note: ");
		if ((arg0 instanceof InventoryItem_Part))
		{
			if ((!(((InventoryItem_Part) arg0).compatibility)))
			{
				local3 = (local3 + "NOT compatible with you current car / parts bin!");
			} else
			{
				if ((((InventoryItem_Part) arg0).compatibility & 1))
				{
					local3 = (local3 + "installable right away to your car");
				}
				if ((((InventoryItem_Part) arg0).compatibility & 2))
				{
					local3 = (local3 + "interchangeable with your cars parts");
				}
				if ((((InventoryItem_Part) arg0).compatibility & 4))
				{
					if ((((InventoryItem_Part) arg0).compatibility & 3))
					{
						local3 = (local3 + " and ");
					}
					local3 = (local3 + "parts bin compatible");
				}
			}
		}
		return local3;
	}

}
