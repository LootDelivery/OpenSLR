package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.dialog.*;
import java.util.resource.*;

public class Valocity extends City
{
	public Valocity()
	{
		super();
		posGarage[0] = new Vector3((-(278.518)), 9.8, 1033.002);
		oriGarage[0] = new Ypr(1.58, 0.0, 0.0);
		posGarage[1] = new Vector3(355.38098, 1.6, 418.24402);
		oriGarage[1] = new Ypr((-(1.763)), 0.0, 0.0);
		posGarage[2] = new Vector3((-(531.138)), 5.05, (-(149.35701)));
		oriGarage[2] = new Ypr(2.077, 0.0, 0.0);
		map = new GroundRef(maps.city:0x1r);
		nav = new Navigator(
			(-(23.482)),
			(-(24.449999)),
			5.828,
			maps.city.smallmap:0x1r,
			maps.city.smallmap:0x2r,
			maps.city.smallmap:0x5r,
			8,
			8,
			8
		);
	}

	public void enter(GameState arg0)
	{
		if ((arg0 instanceof RaceSetup))
		{
			if (((GameLogic.gameMode == GameLogic.GM_QUICKRACE) || (GameLogic.gameMode == GameLogic.GM_DEMO)))
			{
				if ((!(raceState)))
				{
					GameLogic.changeActiveSection(parentState);
					return;
				}
			}
		} else
		{
			Frontend.loadingScreen.show();
			GfxEngine.flush();
			if (((arg0 instanceof Garage) || (arg0 instanceof TrackSelect)))
			{
				posStart = posGarage[GameLogic.player.club];
				oriStart = oriGarage[GameLogic.player.club];
			}
			if (((GameLogic.gameMode == GameLogic.GM_SINGLECAR) || (GameLogic.gameMode == GameLogic.GM_FREERIDE)))
			{
				posStart = new Vector3((-(83.8)), 12.9, (-(558.7)));
				oriStart = new Ypr((-(2.0)), 0, 0);
			}
			if (((GameLogic.gameMode == GameLogic.GM_QUICKRACE) || (GameLogic.gameMode == GameLogic.GM_DEMO)))
			{
				posStart = map.getNearestCross(new Vector3(0.0, 0.0, 500.0), (Math.random() * 1000));
			}
			addNotification(map, GameRef.EVENT_COMMAND, GameRef.EVENT_SAME, null);
			float local2 = (GameLogic.getTime() / 3600);
			if (((local2 > 4) && (local2 < 22)))
			{
				map.addTraffic(new GameRef(cars.traffic.Taxi:0x6r), 80, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.Ambulance:0x6r), 20, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.FireEngine:0x6r), 12, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.Coach:0x6r), 30, 2, 10, 2);
				map.addTraffic(new GameRef(cars.traffic.Schoolbus:0x6r), 55, 2, 10, 2);
				map.addTraffic(new GameRef(cars.traffic.ArmoredVan:0x6r), 20, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.Wagon:0x6r), 200, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.Erbilac:0x6r), 200, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.CivilVan:0x6r), 200, 2, 5, 2);
				if ((GameLogic.gameMode == GameLogic.GM_CARREER))
				{
					map.addTraffic(City.GRT_POLICECAR, 30, 2, 5, 2);
					int local3 = 0;
					while ((local3 < GameLogic.speedymen.length))
					{
						if ((Math.random() > 0.2))
						{
							Racer local4 = GameLogic.speedymen[local3];
							if ((local4 instanceof Bot))
							{
								Bot local5 = local4;
								if ((!(local5.dummycar)))
								{
									local5.dummycar = new GameRef();
									local5.dummycar.create_native(
										map,
										new GameRef(local5.botVd.id),
										"0,-10000,0,0,0,0",
										"dummycar"
									);
									local5.dummycar.command((("texture " + GameLogic.CARCOLORS[local5.botVd.colorIndex]) + " 1"));
									local5.traffic_id = map.addTrafficCar(local5.dummycar, null);
									map.notifyTrafficCar(local5.traffic_id, 1);
								}
							}
						}
						local3++;
					}
				} else
				{
					if ((GameLogic.gameMode == GameLogic.GM_DEMO))
					{
						map.addTraffic(City.GRT_POLICECAR, 50, 2, 5, 2);
					}
				}
				map.setPedestrianDensity(0.0029999998);
			} else
			{
				map.addTraffic(new GameRef(cars.traffic.Erbilac:0x6r), 20, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.CivilVan:0x6r), 10, 2, 5, 2);
				map.addTraffic(new GameRef(cars.traffic.Taxi:0x6r), 150, 2, 5, 2);
				if ((GameLogic.gameMode == GameLogic.GM_CARREER))
				{
					map.addTraffic(City.GRT_POLICECAR, 3, 2, 5, 2);
					prepareNightRace();
				} else
				{
					if ((GameLogic.gameMode == GameLogic.GM_DEMO))
					{
						map.addTraffic(City.GRT_POLICECAR, 20, 2, 5, 2);
					}
				}
				map.setPedestrianDensity(0.00049999997);
			}
			map.addPedestrianType(new GameRef(humans:0x57r));
			map.addPedestrianType(new GameRef(humans:0x58r));
			map.addPedestrianType(new GameRef(humans:0x59r));
			map.addPedestrianType(new GameRef(humans:0x5ar));
			map.addPedestrianType(new GameRef(humans:0x5br));
			map.addPedestrianType(new GameRef(humans:0x22r));
			map.setWater(new Vector3(0.0, (-(8.0)), (-(1500.0))), new Vector3(0.0, 1.0, 0.0), 300.0, 50.0);
			map.addWaterLimit(new Vector3(0.0, 0.0, (-(500.0))), new Vector3(0.0, 0.0, 1.0));
			if (((GameLogic.gameMode == GameLogic.GM_CARREER) || (GameLogic.gameMode == GameLogic.GM_SINGLECAR)))
			{
				addTrigger(
					posGarage[0],
					null,
					Marker.RR_GARAGE1,
					"event_handlerGarage1",
					6.0,
					"garage1_trigger"
				);
				addTrigger(
					posGarage[1],
					null,
					Marker.RR_GARAGE2,
					"event_handlerGarage2",
					6.0,
					"garage2_trigger"
				);
				addTrigger(
					posGarage[2],
					null,
					Marker.RR_GARAGE3,
					"event_handlerGarage3",
					6.0,
					"garage3_trigger"
				);
			}
		}
		super.enter(arg0);
		if ((!((arg0 instanceof RaceSetup))))
		{
			if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
			{
				challenger = player;
				challenged = raceBot;
				raceState = 1;
				if ((!(GameLogic.racesetup)))
				{
					GameLogic.racesetup = new RaceSetup();
				}
				GameLogic.changeActiveSection(GameLogic.racesetup);
			} else
			{
				if ((GameLogic.gameMode == GameLogic.GM_DEMO))
				{
					osdEnabled = 0;
					enableOsd(osdEnabled);
					challenger = demoBot;
					challenged = raceBot;
					raceState = 1;
					Vector3 local2_2;
					do
					{
						local2_2 = map.getNearestCross(posStart, (500 + (Math.random() * 1000)));
					} while ((!(map.getStartDirection(posStart, local2_2))));
					changeCamChase();
					startRace(posStart, local2_2, 0);
				}
			}
		}
	}

	public void exit(GameState arg0)
	{
		if ((!((arg0 instanceof RaceSetup))))
		{
			removeAllTimers();
			int local2 = 0;
			while ((local2 < GameLogic.speedymen.length))
			{
				Racer local3 = GameLogic.speedymen[local2];
				if ((local3 instanceof Bot))
				{
					Bot local4 = local3;
					if (local4.dummycar)
					{
						map.remTrafficCar(local4.traffic_id);
						local4.traffic_id = 0;
						local4.dummycar.destroy();
						local4.dummycar.release();
						local4.dummycar = null;
					}
				}
				local2++;
			}
		}
		super.exit(arg0);
	}

	public void event_handlerGarage1(GameRef arg0, int arg1, String arg2)
	{
		handleGarageTrigger(1, arg1, arg2);
	}

	public void event_handlerGarage2(GameRef arg0, int arg1, String arg2)
	{
		handleGarageTrigger(2, arg1, arg2);
	}

	public void event_handlerGarage3(GameRef arg0, int arg1, String arg2)
	{
		handleGarageTrigger(3, arg1, arg2);
	}

	public void handleGarageTrigger(int arg0, int arg1, String arg2)
	{
		if ((arg2.token(0).intValue() == player.car.id()))
		{
			if ((arg1 == GameRef.EVENT_TRIGGER_ON))
			{
				if ((!(activeTrigger)))
				{
					activeTrigger = arg0;
				}
			} else
			{
				activeTrigger = 0;
			}
		}
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		super.handleEvent(arg0, arg1, arg2);
		if ((arg1 == GameRef.EVENT_TIME))
		{
			if ((arg2 == 2))
			{
				if ((activeTrigger > 0))
				{
					if (((!(policeState)) && (!(raceState))))
					{
						if ((player.car.getSpeedSquare() < 0.25))
						{
							if (((activeTrigger - 1) <= player.club))
							{
								activeTrigger = 0;
								if ((0 == new YesNoDialog(player.controller, ((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_FREEZE), "QUESTION", "Go back to the garage?").display()))
								{
									GameLogic.changeActiveSection(GameLogic.garage);
								}
							} else
							{
								activeTrigger = 0;
								new WarningDialog(
									player.controller,
									((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_FREEZE),
									"INFO",
									"This is a higher club's garage, you can't enter!").display(
								);
							}
						}
					}
				}
			}
		}
	}

}
