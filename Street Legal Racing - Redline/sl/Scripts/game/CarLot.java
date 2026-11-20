package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.util.*;
import java.util.resource.*;

public class CarLot extends GameType implements GameState
{
	static final int RID_MAP_CARLOT = misc.carlot:0x1r;
	static final float FLOOR_HEIGHT = 3.3;
	static final int PARKS = 10;
	static final int FLOORS = 10;
	GameState parentState;
	Osd osd;
	int loadingGroup;
	Player player;
	GameRef map;
	CarLotData data = new CarLotData();
	RenderRef sun;
	RenderRef suntype;
	Vehicle[] cars = new Vehicle[(CarLot.PARKS * CarLot.FLOORS)];
	int[] carSlots = new int[(CarLot.PARKS * CarLot.FLOORS)];
	int curcar;
	int old_curcar;
	int cancelled;
	int cacheSection;
	int initSection;
	Thread loadTextThread;
	Text carName;
	GameRef cam;

	public CarLot(Player arg0)
	{
		super();
		createNativeInstance();
		player = arg0;
		map = new GroundRef(CarLot.RID_MAP_CARLOT);
	}

	public Vector3 getCarPos(int arg0)
	{
		Vector3 local2 = new Vector3(data.kocsidummyPos[(arg0 % CarLot.PARKS)]);
		local2.y += (0.0 + (CarLot.FLOOR_HEIGHT * (arg0 / CarLot.PARKS)));
		return local2;
	}

	public Vector3 getCameraPos(int arg0)
	{
		Vector3 local2 = new Vector3((((arg0 % CarLot.PARKS) * 2.75) - 12.25), 1.3, 8.0);
		local2.y += (CarLot.FLOOR_HEIGHT * (arg0 / CarLot.PARKS));
		return local2;
	}

	public Vector3 getCameraOri(int arg0)
	{
		return new Vector3(0.0, (-(0.2)), 0.0);
	}

	public void moveCamera()
	{
		if (carSlots[curcar])
		{
			if ((!(cars[curcar])))
			{
				loadCar(curcar);
			}
		}
		if ((!(cacheSection)))
		{
			loadVisibleCars();
			cam.queueEvent(
				null,
				GameType.EVENT_COMMAND,
				(((("move " + map.id()) + " ") + getCameraPos(curcar).toString()) + " 0.01")
			);
			if (carSlots[curcar])
			{
				carName.changeText(cars[curcar].toString());
			} else
			{
				carName.changeText(null);
			}
		}
	}

	public void enter(GameState arg0)
	{
		enterAsyncMode();
		t.setPriority(Thread.MAX_PRIORITY);
		parentState = arg0;
		Frontend.loadingScreen.show();
		cacheSection = 0;
		GfxEngine.setGlobalEnvmap(new ResourceRef(MW_Mod:0x69r));
		suntype = new RenderRef();
		suntype.duplicate(new RenderRef(misc.carlot:0x25r));
		sun = new RenderRef(map, suntype, "carlot light");
		Style local2 = new Style(0.45, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		osd = new Osd();
		osd.globalHandler = this;
		osd.createHeader("Car Lot");
		carName = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.98, (-(0.98)));
		osd.createRectangle(0.94000006, (-(0.82)), 1.2, 0.22, (-(1)), new ResourceRef(frontend:0x24r));
		Style local3 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, null);
		Menu local4 = osd.createMenu(local3, 0.98, (-(0.82)), 0, Osd.MD_HORIZONTAL);
		local4.addItem(new ResourceRef(Osd.RID_BACK), Input.AXIS_CANCEL, null);
		local4.addSeparator();
		local4.addSeparator();
		local4.addItem(new ResourceRef(Osd.RID_OK), Input.AXIS_SELECT, null);
		local4.addItem(new ResourceRef(Osd.RID_ARROWRG), Input.AXIS_MENU_RIGHT, null);
		local4.addItem(new ResourceRef(Osd.RID_ARROWLF), Input.AXIS_MENU_LEFT, null);
		local4.addItem(new ResourceRef(Osd.RID_ARROWDN), Input.AXIS_MENU_DOWN, null);
		local4.addItem(new ResourceRef(Osd.RID_ARROWUP), Input.AXIS_MENU_UP, null);
		osd.createHotkey(Input.AXIS_MENU_LEFT, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_MENU_LEFT, this);
		osd.createHotkey(Input.AXIS_MENU_RIGHT, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_MENU_RIGHT, this);
		osd.createHotkey(Input.AXIS_MENU_UP, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_MENU_UP, this);
		osd.createHotkey(Input.AXIS_MENU_DOWN, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_MENU_DOWN, this);
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_CANCEL, this);
		osd.endGroup();
		osd.createText("LOADING...", Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.95000005)), 0.90000004);
		osd.hideGroup((loadingGroup = osd.endGroup()));
		lockPlayerCar();
		cam = new GameRef(
			map,
			GameRef.RID_CAMERA,
			(((getCameraPos(curcar).toString() + ",") + getCameraOri(curcar).toString()) + ", 0x01,1.5,0.0"),
			"s_cam"
		);
		cam.command("zoom 60 5");
		cam.queueEvent(
			null,
			GameType.EVENT_COMMAND,
			((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET))
		);
		Input.cursor.enable(1);
		osd.show();
		setEventMask(GameRef.EVENT_CURSOR);
		initSection = 1;
		moveCamera();
		initSection = 0;
		Frontend.loadingScreen.display();
	}

	public void exit(GameState arg0)
	{
		clearEventMask(GameRef.EVENT_ANY);
		Input.cursor.enable(0);
		cam.destroy();
		sun.destroy();
		suntype.destroy();
		releasePlayerCar();
		flushCars();
		if (loadTextThread)
		{
			loadTextThread.stop();
		}
		osd.hide();
		osd = null;
		cancelled = 0;
		parentState = null;
		leaveAsyncMode();
	}

	public void lockPlayerCar()
	{
		if (player.car)
		{
			addCar(player.car);
			player.car = null;
		}
		cancelled = 0;
		old_curcar = curcar;
	}

	public void releasePlayerCar()
	{
		if (cancelled)
		{
			curcar = old_curcar;
		}
		if (carSlots[curcar])
		{
			if ((!(cars[curcar])))
			{
				loadCar(curcar);
			}
			cars[curcar].queueEvent(null, GameType.EVENT_COMMAND, "start");
			player.car = cars[curcar];
			player.car.setParent(player);
			cars[curcar] = null;
			carSlots[curcar] = 0;
		}
		if ((curcar != old_curcar))
		{
			saveCar(old_curcar);
		}
	}

	public void saveCar(int arg0)
	{
		File.delete(((GameLogic.tempSaveDir + "PlayerCar") + arg0));
		File.delete(GameLogic.tempSaveDir, (("PlayerCar" + arg0) + ".*"));
		if (carSlots[arg0])
		{
			cars[arg0].save(((GameLogic.tempSaveDir + "PlayerCar") + arg0));
		}
	}

	public int isEmpty()
	{
		int local1 = 0;
		while ((local1 < carSlots.length))
		{
			if (carSlots[local1])
			{
				return 0;
			}
			local1++;
		}
		return 1;
	}

	public int getFreeSlot()
	{
		int local1 = 0;
		while ((local1 < carSlots.length))
		{
			if ((!(carSlots[local1])))
			{
				return local1;
			}
			local1++;
		}
		return (-(1));
	}

	public void loadCar(int arg0)
	{
		if ((carSlots[arg0] && (!(cars[arg0]))))
		{
			addCar(Vehicle.load(((GameLogic.tempSaveDir + "PlayerCar") + curcar), map), curcar);
		}
	}

	public void loadVisibleCars()
	{
		cacheSection = 1;
		if ((!(initSection)))
		{
			loadTextThread = new LoadingText(osd, loadingGroup);
			loadTextThread.start();
			loadTextThread.setPriority(Thread.MAX_PRIORITY);
		}
		if (goLeft())
		{
			goRight();
		}
		if (goRight())
		{
			goLeft();
		}
		if ((!(initSection)))
		{
			loadTextThread.stop();
		}
		cacheSection = 0;
	}

	public void addCar(Vehicle arg0)
	{
		if (carSlots[curcar])
		{
			curcar = getFreeSlot();
		}
		addCar(arg0, curcar);
	}

	public void addCar(Vehicle arg0, int arg1)
	{
		if (arg0)
		{
			if ((!(cars[arg1])))
			{
				cars[arg1] = arg0;
				carSlots[arg1] = 1;
				cars[arg1].setParent(map);
				cars[arg1].setMatrix(getCarPos(arg1), new Ypr(3.1399999, 0, 0));
				cars[arg1].command("reset");
				cars[arg1].command("stop");
			}
		}
	}

	public void flushCars()
	{
		int local1 = 0;
		while ((local1 < cars.length))
		{
			if (cars[local1])
			{
				cars[local1].destroy();
				cars[local1] = null;
			}
			local1++;
		}
	}

	public void scanCars()
	{
		curcar = 0;
		old_curcar = 0;
		int local1 = 0;
		while ((local1 < carSlots.length))
		{
			if (File.exists(((GameLogic.tempSaveDir + "PlayerCar") + local1)))
			{
				carSlots[local1] = 1;
			} else
			{
				carSlots[local1] = 0;
			}
			local1++;
		}
	}

	public void osdCommand(int arg0)
	{
		mp.putMessage(new Event(arg0));
	}

	public int goLeft()
	{
		if (((curcar % CarLot.PARKS) != 0))
		{
			curcar--;
			moveCamera();
			return 1;
		}
		return 0;
	}

	public int goRight()
	{
		if (((curcar % CarLot.PARKS) != (CarLot.PARKS - 1)))
		{
			curcar++;
			moveCamera();
			return 1;
		}
		return 0;
	}

	public int goUp()
	{
		if (((curcar / CarLot.PARKS) != (CarLot.FLOORS - 1)))
		{
			curcar += CarLot.PARKS;
			moveCamera();
			return 1;
		}
		return 0;
	}

	public int goDown()
	{
		if (((curcar / CarLot.PARKS) != 0))
		{
			curcar -= CarLot.PARKS;
			moveCamera();
			return 1;
		}
		return 0;
	}

	public void event_handlerMoveUp(GameRef arg0, int arg1, String arg2)
	{
		if ((arg2.token(0).intValue() == GameType.EC_LCLICK))
		{
			osdCommand(Input.AXIS_MENU_UP);
		}
	}

	public void event_handlerMoveDown(GameRef arg0, int arg1, String arg2)
	{
		if ((arg2.token(0).intValue() == GameType.EC_LCLICK))
		{
			osdCommand(Input.AXIS_MENU_DOWN);
		}
	}

	public void event_handlerMoveLeft(GameRef arg0, int arg1, String arg2)
	{
		if ((arg2.token(0).intValue() == GameType.EC_LCLICK))
		{
			osdCommand(Input.AXIS_MENU_LEFT);
		}
	}

	public void event_handlerMoveRight(GameRef arg0, int arg1, String arg2)
	{
		if ((arg2.token(0).intValue() == GameType.EC_LCLICK))
		{
			osdCommand(Input.AXIS_MENU_RIGHT);
		}
	}

	public void handleMessage(Message arg0)
	{
		if ((arg0.type == Message.MT_EVENT))
		{
			Event local2 = arg0;
			if ((local2.cmd == Input.AXIS_MENU_LEFT))
			{
				goLeft();
			} else
			{
				if ((local2.cmd == Input.AXIS_MENU_RIGHT))
				{
					goRight();
				} else
				{
					if ((local2.cmd == Input.AXIS_MENU_UP))
					{
						goUp();
					} else
					{
						if ((local2.cmd == Input.AXIS_MENU_DOWN))
						{
							goDown();
						} else
						{
							if ((local2.cmd == Input.AXIS_CANCEL))
							{
								cancelled = 1;
								GameLogic.changeActiveSection(parentState);
							} else
							{
								if ((local2.cmd == Input.AXIS_SELECT))
								{
									cancelled = 0;
									GameLogic.changeActiveSection(parentState);
								}
							}
						}
					}
				}
			}
		}
	}

}

public class LoadingText extends Thread
{
	Osd osd;
	int group;

	public LoadingText(Osd arg0, int arg1)
	{
		super();
		osd = arg0;
		group = arg1;
	}

	public void stop()
	{
		super.stop();
		osd.hideGroup(group);
	}

	public void run()
	{
		osd.showGroup(group);
		sleep(100);
		osd.hideGroup(group);
		sleep(300);
	}

}

public class CarLotData
{
	final Vector3[] CameraPos = new Vector3[10];
	final Ypr[] CameraOri = new Ypr[10];
	final Vector3[] kocsidummyPos = new Vector3[10];
	final Ypr[] kocsidummyOri = new Ypr[10];

	public CarLotData()
	{
		super();
		CameraPos[0] = new Vector3((-(10.974999)), 1.9000001, 7.905);
		CameraOri[0] = new Ypr(0.284, (-(0.253)), 0.0);
		CameraPos[1] = new Vector3((-(8.207)), 1.9000001, 7.905);
		CameraOri[1] = new Ypr(0.284, (-(0.253)), 0.0);
		CameraPos[2] = new Vector3((-(5.486)), 1.9000001, 7.905);
		CameraOri[2] = new Ypr(0.284, (-(0.253)), 0.0);
		CameraPos[3] = new Vector3((-(2.767)), 1.9000001, 7.905);
		CameraOri[3] = new Ypr(0.284, (-(0.253)), 0.0);
		CameraPos[4] = new Vector3((-(0.047)), 1.9000001, 7.905);
		CameraOri[4] = new Ypr(0.284, (-(0.253)), 0.0);
		CameraPos[5] = new Vector3(0.02, 1.9000001, 7.905);
		CameraOri[5] = new Ypr((-(0.278)), (-(0.253)), 0.0);
		CameraPos[6] = new Vector3(2.751, 1.9000001, 7.905);
		CameraOri[6] = new Ypr((-(0.278)), (-(0.253)), 0.0);
		CameraPos[7] = new Vector3(5.4700003, 1.9000001, 7.905);
		CameraOri[7] = new Ypr((-(0.278)), (-(0.253)), 0.0);
		CameraPos[8] = new Vector3(8.193001, 1.9000001, 7.905);
		CameraOri[8] = new Ypr((-(0.278)), (-(0.253)), 0.0);
		CameraPos[9] = new Vector3(10.911, 1.9000001, 7.905);
		CameraOri[9] = new Ypr((-(0.278)), (-(0.253)), 0.0);
		kocsidummyPos[0] = new Vector3((-(12.318001)), 0.0, 3.305);
		kocsidummyOri[0] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[1] = new Vector3((-(9.549)), 0.0, 3.305);
		kocsidummyOri[1] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[2] = new Vector3((-(6.829)), 0.0, 3.305);
		kocsidummyOri[2] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[3] = new Vector3((-(4.11)), 0.0, 3.305);
		kocsidummyOri[3] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[4] = new Vector3((-(1.3889999)), 0.0, 3.305);
		kocsidummyOri[4] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[5] = new Vector3(1.3319999, 0.0, 3.305);
		kocsidummyOri[5] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[6] = new Vector3(4.0629997, 0.0, 3.305);
		kocsidummyOri[6] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[7] = new Vector3(6.7819996, 0.0, 3.305);
		kocsidummyOri[7] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[8] = new Vector3(9.505, 0.0, 3.305);
		kocsidummyOri[8] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
		kocsidummyPos[9] = new Vector3(12.223001, 0.0, 3.305);
		kocsidummyOri[9] = new Ypr(0.0, (-(3.142)), (-(1.5710001)));
	}

}
