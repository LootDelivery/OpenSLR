package java.game;

import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class City extends Track
{
	static final float SPEED_LIMIT = 18.0;
	static final float SPEED_LIMIT_SQ = (City.SPEED_LIMIT * City.SPEED_LIMIT);
	static final float TINY_SPEED_SQ = 9.0;
	static final int GRI_POLICECAR = cars.misc.Police:0x6r;
	static final GameRef GRT_POLICECAR = new GameRef(City.GRI_POLICECAR);
	static final int RID_BUNTESS = frontend:0x94r;
	static final int RID_NRSTARTBG = frontend:0xb6r;
	static final int RID_NRFIN_WIN = frontend:0xa7r;
	static final int RID_NRFIN_LOSE = frontend:0xc9r;
	static final int RID_NRFIN_AI = frontend:0xccr;
	static final int RID_SPEECH3 = sound:0x1dr;
	static final int RID_SPEECH2 = sound:0x1cr;
	static final int RID_SPEECH1 = sound:0x1br;
	static final int RID_SPEECHGO = sound:0x1er;
	static final int RID_SPEECHYOUWIN = sound:0x1fr;
	static final int RID_APPLAUSE1 = sound:0x21r;
	static final int RID_APPLAUSE2 = sound:0x22r;
	static final int GREEN_ARROW = frontend:0x70r;
	static final float ODDS_CASH_1 = 1.0;
	static final float ODDS_CASH_2 = 0.95000005;
	static final float ODDS_PINKS = 0.95000005;
	static final int RID_SFX_DAY_WIN = sound:0x23r;
	static final int RID_SFX_DAY_LOOSE = sound:0x24r;
	static final int RID_DAY_WIN = frontend:0xcfr;
	static final int RID_DAY_LOOSE = frontend:0xd0r;
	static final int RID_DAY_CHALLENGE = frontend:0xa5r;
	static final ResourceRef RRT_FRAME = new ResourceRef(frontend:0xcbr);
	static final int CMD_PARTICIPATE = 1001;
	static final int CMD_WATCH_RACE = 1002;
	static final int CMD_STOP_WATCHING = 1003;
	static final int NR_INVALID = (-(1));
	static final int NR_IDLE = 0;
	static final int NR_SHOWSTART = 1;
	static final int NR_3 = 2;
	static final int NR_2 = 3;
	static final int NR_1 = 4;
	static final int NR_START = 5;
	static final int NR_RACE = 6;
	static final int NR_FINISH = 7;
	static final int NR_SHOWFINISH = 8;
	Vector3[] posGarage = new Vector3[GameLogic.CLUBS];
	Ypr[] oriGarage = new Ypr[GameLogic.CLUBS];
	int time;
	int prize;
	Text statusTxt;
	Text oppStatusTxt;
	int oppStatusDisplayed;
	int collision;
	Vector3 raceStart;
	Vector3 raceFinish;
	Trigger trRaceFinish;
	RenderRef finishObject;
	RaceDialog raceDialog;
	Racer challenger;
	Racer challenged;
	Marker mStart;
	Marker mFinish;
	Bot raceBot;
	Bot demoBot;
	Marker mRaceBot;
	int raceState;
	int aiChallengeState;
	int abandoned;
	int abandoned2;
	Vector opponentCars = new Vector();
	GameRef nrcameraTarget;
	int nightTime;
	RenderRef nrStarterLady;
	Animation nrStarterLadyAnim;
	int nrStat;
	int nrDelay = 0;
	int nrBotID1;
	int nrBotID2;
	Vector3 pS;
	Vector3 pF;
	Vector3 dirS;
	Vector3 dirF;
	Ypr oriS;
	Ypr oriF;
	Bot nrBot1;
	Bot nrBot2;
	Trigger nrFinishTrigger;
	Trigger nrFinishSoundTrigger;
	int nrPlayerRace;
	int nrNear;
	int nrWatching;
	int nrPlayerPaused;
	int nrShowRaceStart;
	int nrShowRaceFinish;
	int backupnrWatching;
	int nrPrize;
	ResourceRef[] fakers;
	ParkingCar[] parkingCars;
	RenderRef[] nrMen;
	Animation[] nrMenAnim;
	float yawNudge = 0.2;
	float posNudge = 0.3;
	int nightRaceGroup;
	int nrWatchingGroup;
	int nrLookAt;
	int nrFinished1;
	int nrFinished2;
	int nrQuit;
	float nrStartTime;
	float nrTime1;
	float nrTime2;
	ResourceRef nrHead1;
	ResourceRef nrHead2;
	String nrName1;
	String nrName2;
	Racer pl1;
	Racer pl2;
	NightracesData nrData = new NightracesData();
	int nrLastPlaceDay = (-(1));
	Vector nrOpponents = new Vector();
	int[] nrPrizeList;
	int nrcameraMode_before;
	int applauseSfxID;
	int applauseSfxOn;
	SfxRef applause1 = new SfxRef(City.RID_APPLAUSE1);
	SfxRef applause2 = new SfxRef(City.RID_APPLAUSE2);
	SfxRef speech3 = new SfxRef(City.RID_SPEECH3);
	SfxRef speech2 = new SfxRef(City.RID_SPEECH2);
	SfxRef speech1 = new SfxRef(City.RID_SPEECH1);
	SfxRef speechGO = new SfxRef(City.RID_SPEECHGO);
	SfxRef speechYOUWIN = new SfxRef(City.RID_SPEECHYOUWIN);
	SfxRef speechDAYYOUWIN = new SfxRef(City.RID_SFX_DAY_WIN);
	SfxRef speechDAYYOULOOSE = new SfxRef(City.RID_SFX_DAY_LOOSE);
	int maxScouts = 2;
	Vector policeCars = new Vector();
	Vector alertedScouts = new Vector();
	float lastCollisionTime;
	float lastAlertTime;
	float firstAlertTime;
	float pullOverTime;
	int policeState;
	int roamFree;
	float overSpeed;
	int crashes;
	int fleedAway;
	int backupCam;
	int timeLock;

	public void prepareNightRace()
	{
		int local1;
		if ((nrLastPlaceDay != GameLogic.day))
		{
			nrLastPlaceDay = GameLogic.day;
			local1 = ((Math.random() * 5) + (GameLogic.player.club * 6));
		}
		if (1)
		{
			pS = nrData.startPos[local1];
			pF = nrData.finishPos[local1];
			oriS = nrData.startOri[local1];
			oriF = nrData.finishOri[local1];
			dirS = new Vector3(oriS);
			dirF = new Vector3(oriF);
		} else
		{
			pS = map.getNearestCross(posStart);
			pF = map.getNearestCross(pS, 150);
			dirS = map.getStartDirection(pS, pF);
			dirF = map.getStartDirection(pF, pS);
			oriS = new Ypr(dirS);
			oriF = new Ypr(dirF);
			Vector3 local2 = new Vector3(dirS);
			local2.mul(20.0);
			pS.add(local2);
			local2 = new Vector3(dirF);
			local2.mul(20.0);
			pF.add(local2);
		}
		map.haltTrafficPath(pS, pF);
		nrNear = 0;
		nrWatching = 0;
		nrPlayerPaused = 0;
		nrPlayerRace = 0;
		nrShowRaceStart = 0;
		nrShowRaceFinish = 0;
	}

	public void enter(GameState arg0)
	{
		if ((arg0 instanceof RaceSetup))
		{
			Frontend.loadingScreen.show();
			osd.show();
		} else
		{
			activeTrigger = (-(1));
			abandoned = (raceState = 0);
			aiChallengeState = 0;
			policeState = 0;
			roamFree = 0;
			overSpeed = 0;
			crashes = 0;
			fleedAway = 0;
			nightTime = 0;
			if (((GameLogic.gameMode == GameLogic.GM_FREERIDE) || (GameLogic.gameMode == GameLogic.GM_SINGLECAR)))
			{
			} else
			{
				if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
				{
					createQuickRaceBot();
					changeCamTarget2(raceBot.car);
				} else
				{
					if ((GameLogic.gameMode == GameLogic.GM_DEMO))
					{
						osdEnabled = 0;
						enableOsd(osdEnabled);
						if (1)
						{
							if (player.car)
							{
								player.car.destroy();
								player.controller.command(("leave " + player.car.id()));
								player.car = null;
							}
							raceBot = new Bot(
								(Math.random() * 59),
								(Math.random() * 1234),
								Math.random(),
								2.0,
								2.0,
								1.0
							);
							demoBot = new Bot(
								(Math.random() * 59),
								(Math.random() * 1234),
								Math.random(),
								2.0,
								2.0,
								1.0
							);
							raceBot.command("osd 0");
							demoBot.command("osd 0");
							VehicleDescriptor local2;
							local2 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
							raceBot.createCar(
								map,
								new Vehicle(map, local2.id, local2.colorIndex, local2.optical, local2.power, local2.wear, local2.tear)
							);
							local2 = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
							demoBot.createCar(
								map,
								new Vehicle(map, local2.id, local2.colorIndex, local2.optical, local2.power, local2.wear, local2.tear)
							);
							if (raceBot.car)
							{
								raceBot.car.setParent(map);
								raceBot.car.setMatrix(posStart, oriStart);
								raceBot.car.command("reset");
								raceBot.car.command("reload");
							}
							if (demoBot.car)
							{
								demoBot.car.setParent(map);
								demoBot.car.setMatrix(posStart, oriStart);
								demoBot.car.command("reset");
								demoBot.car.command("reload");
							}
							player.car = demoBot.car;
						}
						changeCamTarget(demoBot.car);
						changeCamTarget2(raceBot.car);
					} else
					{
						float local2_2 = (GameLogic.getTime() / 3600);
						if (((local2_2 < 4) || (local2_2 > 22)))
						{
							nightTime = 1;
							nrStat = City.NR_IDLE;
							fakers = new ResourceRef(cars:0x27r).getChildNodes();
							parkingCars = new ParkingCar[20];
							int local3 = 0;
							nrMen = new RenderRef[20];
							nrMenAnim = new Animation[20];
							int local4 = 0;
							float local5 = 8;
							float local6 = 0.7;
							Vector3[] local7;
							local7 = map.alignToRoad(pS);
							pS = new Vector3(local7[0]);
							local7 = map.alignToRoad(pF);
							pF = new Vector3(local7[0]);
							int[] local8 = new int[6];
							local8[0] = humans:0x2fr;
							local8[1] = humans:0x30r;
							local8[2] = humans:0x31r;
							local8[3] = humans:0x32r;
							local8[4] = humans:0x33r;
							local8[5] = humans:0x34r;
							int[] local9 = new int[6];
							local9[0] = humans:0x14r;
							local9[1] = humans:0x1br;
							local9[2] = humans:0x7dr;
							local9[3] = humans:0x7er;
							local9[4] = humans:0x17r;
							local9[5] = humans:0x7fr;
							Vector3 local10;
							Vector3 local11;
							Vector3 local12;
							Ypr local13;
							RenderRef local14;
							local7 = map.alignToRoad(pS);
							local7[1].normalize();
							local10 = new Vector3(local7[1]);
							local10.mul(2.5);
							local11 = new Vector3(local7[1]);
							local11.y = 0;
							local11.mul(local5);
							local11.rotate(new Ypr(1.57, 0.0, 0.0));
							int local15 = 0;
							while ((local15 < 5))
							{
								float local16;
								float local17;
								int local18;
								int local19;
								int local20;
								local16 = (((Math.random() * 2) - 1) * posNudge);
								local17 = (((Math.random() * 2) - 1) * yawNudge);
								local18 = (Math.random() * fakers.length);
								local19 = (Math.random() * 12345);
								local12 = new Vector3(local7[0]);
								local12.add(local11);
								local13 = new Ypr(local7[1]);
								local13.y -= (local6 + local17);
								parkingCars[local3++] = new ParkingCar(
									map,
									fakers[local18],
									local12,
									local13,
									local19
								);
								local12.add(local10);
								local20 = (Math.random() * local8.length);
								nrMen[local4] = new RenderRef(map, local8[local20], "tesztmen");
								nrMenAnim[local4] = new Animation(
									nrMen[local4],
									new ResourceRef(local9[local20])
								);
								nrMenAnim[local4].setSpeed((0.5 + Math.random()));
								nrMenAnim[local4].loopPlay();
								local13.y += 3.1399999;
								nrMen[local4].setMatrix(local12, local13);
								local13.y -= 3.1399999;
								local4++;
								local16 = (((Math.random() * 2) - 1) * posNudge);
								local17 = (((Math.random() * 2) - 1) * yawNudge);
								local18 = (Math.random() * fakers.length);
								local19 = (Math.random() * 12345);
								local12 = new Vector3(local7[0]);
								local12.sub(local11);
								local13 = new Ypr(local7[1]);
								local13.y += (local6 + local17);
								parkingCars[local3++] = new ParkingCar(
									map,
									fakers[local18],
									local12,
									local13,
									local19
								);
								local12.add(local10);
								local20 = (Math.random() * local8.length);
								nrMen[local4] = new RenderRef(map, local8[local20], "tesztmen");
								nrMenAnim[local4] = new Animation(
									nrMen[local4],
									new ResourceRef(local9[local20])
								);
								nrMenAnim[local4].setSpeed((0.5 + Math.random()));
								nrMenAnim[local4].loopPlay();
								local13.y += 3.1399999;
								nrMen[local4].setMatrix(local12, local13);
								local13.y -= 3.1399999;
								local4++;
								local7[0].add(local10);
								local7[0].add(local10);
								local7 = map.alignToRoad(local7[0]);
								local7[1].normalize();
								local15++;
							}
							local7 = map.alignToRoad(pF);
							local7[1].normalize();
							local10 = new Vector3(local7[1]);
							local10.mul(2.5);
							local11 = new Vector3(local7[1]);
							local11.y = 0;
							local11.mul(local5);
							local11.rotate(new Ypr(1.57, 0.0, 0.0));
							int local15_2 = 0;
							while ((local15_2 < 5))
							{
								float local16_2;
								float local17_2;
								int local18_2;
								int local19_2;
								int local20_2;
								local16_2 = (((Math.random() * 2) - 1) * posNudge);
								local17_2 = (((Math.random() * 2) - 1) * yawNudge);
								local18_2 = (Math.random() * fakers.length);
								local19_2 = (Math.random() * 12345);
								local12 = new Vector3(local7[0]);
								local12.add(local11);
								local13 = new Ypr(local7[1]);
								local13.y -= (local6 + local17_2);
								parkingCars[local3++] = new ParkingCar(
									map,
									fakers[local18_2],
									local12,
									local13,
									local19_2
								);
								local12.add(local10);
								local20_2 = (Math.random() * 4);
								nrMen[local4] = new RenderRef(map, local8[local20_2], "tesztmen");
								nrMenAnim[local4] = new Animation(
									nrMen[local4],
									new ResourceRef(local9[local20_2])
								);
								nrMenAnim[local4].setSpeed((0.5 + Math.random()));
								nrMenAnim[local4].loopPlay();
								local13.y += 3.1399999;
								nrMen[local4].setMatrix(local12, local13);
								local13.y -= 3.1399999;
								local4++;
								local16_2 = (((Math.random() * 2) - 1) * posNudge);
								local17_2 = (((Math.random() * 2) - 1) * yawNudge);
								local18_2 = (Math.random() * fakers.length);
								local19_2 = (Math.random() * 12345);
								local12 = new Vector3(local7[0]);
								local12.sub(local11);
								local13 = new Ypr(local7[1]);
								local13.y += (local6 + local17_2);
								parkingCars[local3++] = new ParkingCar(
									map,
									fakers[local18_2],
									local12,
									local13,
									local19_2
								);
								local12.add(local10);
								local20_2 = (Math.random() * 4);
								nrMen[local4] = new RenderRef(map, local8[local20_2], "tesztmen");
								nrMenAnim[local4] = new Animation(
									nrMen[local4],
									new ResourceRef(local9[local20_2])
								);
								nrMenAnim[local4].setSpeed((0.5 + Math.random()));
								nrMenAnim[local4].loopPlay();
								local13.y += 3.1399999;
								nrMen[local4].setMatrix(local12, local13);
								local13.y -= 3.1399999;
								local4++;
								local7[0].add(local10);
								local7[0].add(local10);
								local7 = map.alignToRoad(local7[0]);
								local7[1].normalize();
								local15_2++;
							}
							finishObject = new RenderRef(map, City.GREEN_ARROW, "finishObject");
							finishObject.setMatrix(new Vector3(pF.x, (pF.y + 3.0), pF.z), null);
							addTrigger(
								pS,
								null,
								Marker.RR_START,
								"event_handlerNrStart",
								13,
								"night race start trigger"
							);
						}
					}
				}
			}
		}
		new SfxRef(sound:0x1r).cache();
		new SfxRef(sound:0x2r).cache();
		new SfxRef(sound:0x3r).cache();
		new SfxRef(sound:0x16r).precache();
		super.enter(arg0);
		if ((arg0 instanceof RaceSetup))
		{
		} else
		{
			setEventMask(GameRef.EVENT_COLLISION);
			addNotification(GameLogic.player.car, GameRef.EVENT_COLLISION, GameRef.EVENT_SAME, null);
			if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
			{
				changeCamTarget2(raceBot.car);
			}
			if (((GameLogic.gameMode != GameLogic.GM_DEMO) && (GameLogic.gameMode != GameLogic.GM_SINGLECAR)))
			{
				osd.createRectangle(
					(-(0.42000002)),
					(-(0.95000005)),
					1.2,
					0.1,
					(-(1)),
					new ResourceRef(frontend:0x92r)
				);
				statusTxt = osd.createText(null, Frontend.smallFont, Text.ALIGN_LEFT, (-(0.98)), (-(0.98)));
				if ((GameLogic.gameMode != GameLogic.GM_FREERIDE))
				{
					osd.createRectangle(
						(-(0.47)),
						(-(0.86)),
						1.1,
						0.1,
						(-(1)),
						new ResourceRef(frontend:0x92r)
					);
					oppStatusTxt = osd.createText(
						null,
						Frontend.smallFont,
						Text.ALIGN_LEFT,
						(-(0.98)),
						(-(0.89))
					);
					if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
					{
						String local2_3 = raceBot.name;
						if (raceBot.car.chassis)
						{
							local2_3 = ((local2_3 + " riding a ") + raceBot.car.chassis.vehicleName);
						}
						oppStatusTxt.changeText(local2_3);
					}
				}
			}
			if ((GameLogic.gameMode == GameLogic.GM_CARREER))
			{
				if (nightTime)
				{
					if (player.checkHint(Player.H_NIGHTCITY))
					{
						new WarningDialog(
							player.controller,
							(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
							"Welcome to ValoCity!",
							"At night the racing community gathers to arrange drag races for high prizes even for pink slips! \n You can find them at the location marked with the green flag, to race or just to watch others race.").display(
						);
					}
				} else
				{
					if (player.checkHint(Player.H_DAYCITY))
					{
						new WarningDialog(
							player.controller,
							(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
							"Welcome to ValoCity!",
							"Find opponents from your club to race, for money prizes or just for prestige. \n Get to know them so you'll know who to race at the night races! (Night races take place around midnight) \n Beware of the cops, pulling over is often easier than trying to get away.").display(
						);
					}
				}
			}
			osd.endGroup();
			osd.globalHandler = this;
			Style local2_4 = new Style(0.3, 0.120000005, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
			Menu local3_2;
			local3_2 = osd.createMenu(local2_4, (-(1.0)), (-(0.5)), 0);
			local3_2.addItem("PARTICIPATE", City.CMD_PARTICIPATE);
			local3_2.addItem("WATCH RACE", City.CMD_WATCH_RACE);
			osd.hideGroup((nightRaceGroup = osd.endGroup()));
			local3_2 = osd.createMenu(local2_4, (-(1.0)), (-(0.5)), 0);
			local3_2.addItem("STOP WATCHING", City.CMD_STOP_WATCHING);
			local3_2.addItem("PARTICIPATE", City.CMD_PARTICIPATE);
			osd.hideGroup((nrWatchingGroup = osd.endGroup()));
		}
		refreshStatus();
	}

	public void exit(GameState arg0)
	{
		if ((arg0 instanceof RaceSetup))
		{
			osd.hide();
		} else
		{
			int local2;
			clearEventMask(GameRef.EVENT_COLLISION);
			if (raceState)
			{
				cleanupRace();
			}
			removeAllTimers();
			destroyRaceBot();
			if (demoBot)
			{
				demoBot.deleteCar();
				demoBot = null;
			}
			if ((((GameLogic.gameMode == GameLogic.GM_DEMO) || (GameLogic.gameMode == GameLogic.GM_QUICKRACE)) || (GameLogic.gameMode == GameLogic.GM_FREERIDE)))
			{
				killCar = 1;
			}
			local2 = (alertedScouts.size() - 1);
			while ((local2 >= 0))
			{
				PoliceScout local3 = alertedScouts.removeLastElement();
				local3.bot.deleteCar();
				nav.remMarker(local3.tracker.m);
				local2--;
			}
			local2 = (policeCars.size() - 1);
			while ((local2 >= 0))
			{
				TrafficTracker local3_2 = policeCars.removeLastElement();
				local3_2.car.release();
				nav.remMarker(local3_2.m);
				local2--;
			}
			local2 = (opponentCars.size() - 1);
			while ((local2 >= 0))
			{
				TrafficTracker local3_3 = opponentCars.removeLastElement();
				local3_3.car.release();
				nav.remMarker(local3_3.m);
				local2--;
			}
			cleanupNightRace();
			parkingCars = null;
			nrMen = null;
			fakers = null;
		}
		super.exit(arg0);
	}

	public void refreshStatus()
	{
		if (statusTxt)
		{
			if (player.car)
			{
				String local1;
				if (((GameLogic.gameMode == GameLogic.GM_QUICKRACE) || (GameLogic.gameMode == GameLogic.GM_FREERIDE)))
				{
					local1 = player.name;
				} else
				{
					int local2 = (GameLogic.CLUBMEMBERS - (GameLogic.findRacer(player) - (GameLogic.CLUBMEMBERS * player.club)));
					local1 = ((((((((player.name + "  ") + player.club) + "/") + local2) + "  $") + player.money) + " >") + player.getPrestigeString());
				}
				if ((player.car && player.car.chassis))
				{
					local1 = ((local1 + " riding a ") + player.car.chassis.vehicleName);
				}
				statusTxt.changeText(local1);
			}
		}
	}

	public void alertPolice()
	{
		float local1 = System.simTime();
		if (((local1 - lastAlertTime) < 3.0))
		{
			return;
		}
		lastAlertTime = local1;
		if (roamFree)
		{
			return;
		}
		int local2;
		float local3;
		PoliceScout local4;
		PoliceScout local5;
		int local6 = (alertedScouts.size() - 1);
		while ((local6 >= 0))
		{
			if ((!((local4 = alertedScouts.elementAt(local6)).returningTraffic)))
			{
				local2++;
				if ((local4.distance > local3))
				{
					local3 = local4.distance;
					local5 = local4;
				}
			}
			local6--;
		}
		float local6_2 = (50.0 + (25 * player.club));
		float local7;
		TrafficTracker local8;
		int local9;
		int local10 = 0;
		while ((local10 < policeCars.size()))
		{
			TrafficTracker local11 = policeCars.elementAt(local10);
			GameRef local12 = local11.car;
			if (local12.id())
			{
				local11.trafficId = local12.getInfo(GameType.GII_CAR_TRAFFICPTR);
				if (local11.trafficId)
				{
					local7 = map.getRouteLength(local12.getPos(), player.car.getPos());
					if (((local7 > 0) && (local7 < local6_2)))
					{
						local6_2 = local7;
						local8 = local11;
						local9 = local10;
					}
				}
			} else
			{
				policeCars.removeElementAt(local10);
				nav.remMarker(local11.m);
			}
			local10++;
		}
		if (local8)
		{
			if ((local6_2 < 80.0))
			{
				int local10_2;
				if ((local2 >= maxScouts))
				{
					if (((local6_2 * 3) < local3))
					{
						local10_2 = 1;
						local2--;
					}
				}
				if ((local2 < maxScouts))
				{
					policeCars.removeElementAt(local9);
					PoliceScout local11_2 = new PoliceScout();
					local11_2.distance = local6_2;
					local11_2.tracker = local8;
					wakePoliceScout(local11_2);
					alertedScouts.addElement(local11_2);
				}
				if (local10_2)
				{
					sleepPoliceScoutQuick(local5);
				}
			}
		}
	}

	public void wakePoliceScout(PoliceScout arg0)
	{
		if ((!(policeState)))
		{
			firstAlertTime = System.simTime();
		}
		arg0.bot = new Bot(0, 12345, (0.0 + (player.club * 0.5)));
		arg0.bot.setDriverObject(GameLogic.HUMAN_POLICEMAN);
		arg0.bot.createCar(map, new Vehicle(arg0.tracker.car));
		arg0.bot.traffic_id = arg0.tracker.trafficId;
		arg0.bot.imaPoliceDriver = 1;
		policeState = 10;
		arg0.bot.followCar(player.car, 10);
		arg0.bot.pressHorn();
		if ((GameLogic.gameMode != GameLogic.GM_DEMO))
		{
			changeCamTarget2(arg0.bot.car);
		}
	}

	public void sleepPoliceScout(PoliceScout arg0)
	{
		arg0.bot.releaseHorn();
		arg0.returningTraffic = 1;
		arg0.bot.addNotification(arg0.bot.car, GameRef.EVENT_COMMAND, GameRef.EVENT_SAME, null);
		arg0.bot.reJoinTraffic();
	}

	public void sleepPoliceScoutQuick(PoliceScout arg0)
	{
		Vector3 local2 = arg0.bot.car.getPos();
		nav.remMarker(arg0.tracker.m);
		arg0.bot.releaseHorn();
		arg0.bot.deleteCar();
		alertedScouts.removeElement(arg0);
		map.addTrafficP(City.GRT_POLICECAR, local2, 1, 2, 5, 2);
	}

	public void createQuickRaceBot()
	{
		int local1 = (Math.random() * 59);
		if (((local1 + Racer.RID_FEJ) == player.character.id()))
		{
			local1++;
		}
		raceBot = new Bot(local1, (Math.random() * 1234), Math.random(), 2.0, 2.0, 1.0);
		raceBot.botVd = GameLogic.getVehicleDescriptor(VehicleType.VS_DEMO);
		Vector3 local2 = player.car.getPos();
		Vector3 local3 = player.car.getVel();
		local3.normalize();
		local3.mul(500.0);
		local2.add(local3);
		raceBot.createCar(map);
	}

	public void destroyRaceBot()
	{
		if (raceBot)
		{
			if (mRaceBot)
			{
				nav.remMarker(mRaceBot);
				mRaceBot = null;
			}
			raceBot.deleteCar();
			raceBot = null;
		}
	}

	public void startRace(Vector3 arg0, Vector3 arg1, int arg2)
	{
		raceStart = arg0;
		raceFinish = arg1;
		prize = arg2;
		Vector3 local4 = map.getStartDirection(arg0, arg1);
		Ypr local5 = new Ypr(local4);
		if (raceBot.dummycar)
		{
			Vector3 local6 = raceBot.dummycar.getPos();
			Ypr local7 = raceBot.dummycar.getOri();
			if (raceBot.brain)
			{
				raceBot.brain.destroy();
				raceBot.brain = null;
				raceBot.car.release();
				raceBot.car = null;
			} else
			{
				map.remTrafficCar(raceBot.traffic_id);
				raceBot.traffic_id = 0;
			}
			raceBot.dummycar.command("reset");
			raceBot.dummycar.setMatrix(new Vector3(0.0, (-(10000.0)), 0.0), local7);
			raceBot.dummycar.setParent(raceBot);
			raceBot.createCar(map);
			raceBot.car.command("reset");
			raceBot.car.setMatrix(local6, local7);
			raceBot.car.setParent(map);
		}
		raceBot.stop();
		mRaceBot = nav.addMarker(raceBot);
		map.haltTrafficCross(raceStart, 15.0);
		Vector3 local6_2 = new Vector3(local4);
		local6_2.mul((-(7.0)));
		local6_2.y += 3;
		local6_2.add(raceStart);
		if (cam)
		{
			Ypr local7_2 = new Ypr(local5);
			local7_2.p -= 0.3;
			cam.setMatrix(local6_2, local5);
		}
		Vector3 local7_3 = new Vector3(local4);
		local7_3.mul(1.75);
		local7_3.rotate(new Ypr(1.57, 0.0, 0.0));
		local7_3.add(raceStart);
		Vector3 local8 = new Vector3(local4);
		local8.mul(1.75);
		local8.rotate(new Ypr((-(1.57)), 0.0, 0.0));
		local8.add(raceStart);
		if ((Math.random() > 0.5))
		{
			Vector3 local9 = local7_3;
			local7_3 = local8;
			local8 = local9;
		}
		mStart = nav.addMarker(Marker.RR_START, arg0, 3);
		mFinish = nav.addMarker(Marker.RR_FINISH, arg1, 3);
		trRaceFinish = new Trigger(map, null, raceFinish, "dayrace_finish_trigger");
		addNotification(
			trRaceFinish.trigger,
			GameRef.EVENT_TRIGGER_ON,
			GameRef.EVENT_SAME,
			null,
			"event_handlerRaceFinish"
		);
		finishObject = new RenderRef(map, frontend:0x70r, "finishObject");
		Vector3 local9_2 = new Vector3(raceFinish);
		local9_2.y += 3;
		finishObject.setMatrix(local9_2, null);
		if (raceBot)
		{
			raceBot.car.command("reset");
			raceBot.car.setMatrix(local7_3, local5);
			raceBot.car.setParent(map);
			raceBot.car.command("stop");
			raceBot.car.command("idle");
			raceBot.brain.command("AI_BeginRace 0.5");
		}
		if (demoBot)
		{
			demoBot.car.command("reset");
			demoBot.car.setMatrix(local8, local5);
			demoBot.car.setParent(map);
			demoBot.car.command("stop");
			demoBot.car.command("idle");
			demoBot.brain.command("AI_BeginRace 0.5");
		} else
		{
			if (player.car)
			{
				player.car.command("reset");
				player.car.setMatrix(local8, local5);
				player.car.command("stop");
				player.car.command("idle");
			}
		}
		setEventMask(GameRef.EVENT_TIME);
		abandoned = 0;
		addTimer(1, 9);
	}

	public void startRace2()
	{
		if (player.car)
		{
			player.car.setCruiseControl(0);
			player.car.command("start");
		}
		if (raceBot)
		{
			if (demoBot)
			{
				raceBot.car.command("start");
				demoBot.car.command("start");
				raceBot.startRace(raceFinish, demoBot);
				demoBot.startRace(raceFinish, raceBot);
			} else
			{
				raceBot.car.command("start");
				raceBot.startRace(raceFinish, player);
			}
		}
		if ((GameLogic.klampiPatch >= 2))
		{
			if ((GameLogic.klampiPatch >= 3))
			{
				player.controller.command("viewport 0");
				player.controller.command("osd 0");
			}
			raceBot.brain.command(("camera " + cam.id()));
			if ((GameLogic.klampiPatch >= 3))
			{
				if (osdEnabled)
				{
					raceBot.brain.command(("osd " + osd.id()));
				}
				raceBot.brain.command(("viewport " + osd.getViewport().id()));
			}
		}
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void lookBot(Bot arg0, int arg1)
	{
		if (arg1)
		{
			changeCamNone();
		}
		if (arg0)
		{
			changeCamTarget(arg0.car);
			changeCamChase();
		} else
		{
			changeCamNone();
			Vector3 local3 = new Vector3(dirS);
			local3.mul((-(6.0)));
			local3.y += 1.5;
			local3.add(pS);
			cam = new GameRef(
				map,
				GameRef.RID_CAMERA,
				(((local3.toString() + ",") + oriS.y) + ",0,0, 0x02, 1.0,0.0,0.1"),
				"no_bot_cam"
			);
			cam.command((((((((("look " + map.id()) + " ") + pS.x) + ",") + (pS.y + 1.0)) + ",") + pS.z) + " 0,0,0"));
			cam.command((((("move " + map.id()) + " ") + local3.toString()) + " 8"));
			cam.command(((("render " + osd.getViewport().id()) + " 0 0 1 ") + (Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET)));
		}
	}

	public int selectNrOpponent()
	{
		if ((nrOpponents.size() == 0))
		{
			int local1 = (player.club * GameLogic.CLUBMEMBERS);
			int local2 = (local1 + GameLogic.CLUBMEMBERS);
			int local3 = GameLogic.findRacer(player);
			if (((player.club < 2) && (local3 == (local2 - 1))))
			{
				local2 += 1;
			}
			int local4 = (-(1));
			float local5 = (((player.winPinkSlips + 1) * (player.winPinkSlips + 1)) + 1);
			int local6;
			local6 = (local3 + 2);
			while ((local6 >= (local3 + 1)))
			{
				if (((((local6 < local2) && (local6 >= local1)) && (local6 != local3)) && (GameLogic.speedymen[local6].lastRaceDay < GameLogic.day)))
				{
					nrOpponents.addElement(new Integer(local6));
					GameLogic.speedymen[local6].enabledPinkSlips = 0;
					if (((local6 >= 30) && (Math.random() < (1.0 / local5))))
					{
						GameLogic.speedymen[local6].enabledPinkSlips = 1;
					}
				}
				local6--;
			}
			local6 = (local3 - 1);
			while ((local6 >= (local3 - 1)))
			{
				if (((((local6 < local2) && (local6 >= local1)) && (local6 != local3)) && (GameLogic.speedymen[local6].lastRaceDay < GameLogic.day)))
				{
					nrOpponents.addElement(new Integer(local6));
					GameLogic.speedymen[local6].enabledPinkSlips = 0;
				}
				local6--;
			}
		}
		if ((nrOpponents.size() != 0))
		{
			int local1_2 = nrOpponents.size();
			if ((local1_2 > 3))
			{
				local1_2 = 3;
			}
			String[] local2_2 = new String[local1_2];
			int[] local3_2 = new int[local1_2];
			ResourceRef[] local4_2 = new ResourceRef[local1_2];
			int[] local5_2 = new int[4];
			int local6_2 = ((GameLogic.findRacer(player) / GameLogic.CLUBMEMBERS) + 1);
			local5_2[0] = (1000 * local6_2);
			local5_2[1] = (2000 * local6_2);
			local5_2[2] = (4000 * local6_2);
			local5_2[3] = 0;
			nrPrizeList = new int[local1_2];
			float local7 = 1.0;
			if ((player.car && player.car.chassis))
			{
				local7 -= player.car.chassis.C_drag;
			}
			local7 *= local7;
			int local8 = 0;
			while ((local8 < local1_2))
			{
				local3_2[local8] = nrOpponents.elementAt(local8).intValue();
				Bot local9 = ((Bot) GameLogic.speedymen[local3_2[local8]]);
				local4_2[local8] = local9.character;
				int local10 = (GameLogic.CLUBMEMBERS - (local3_2[local8] % GameLogic.CLUBMEMBERS));
				float local11 = 1.0;
				float local12;
				float local13;
				if (((player.car.bestNightQM < 0.1) || (local9.bestNightQM < 0.1)))
				{
					local11 *= 3.0;
				} else
				{
					local11 *= (local9.bestNightQM / player.car.bestNightQM);
				}
				local11 /= (1.0 - ((1.0 - local7) * 0.5));
				if ((((local11 <= City.ODDS_PINKS) && (player.club > 0)) && (Math.random() < 0.3)))
				{
					nrPrizeList[local8] = local5_2[3];
					GameLogic.cheats += 1;
				} else
				{
					if ((local11 <= City.ODDS_CASH_2))
					{
						nrPrizeList[local8] = local5_2[2];
					} else
					{
						if ((local11 <= City.ODDS_CASH_1))
						{
							nrPrizeList[local8] = local5_2[1];
						} else
						{
							nrPrizeList[local8] = local5_2[0];
						}
					}
				}
				String local14;
				if (((local10 % 10) == 1))
				{
					local14 = "st - ";
				} else
				{
					if (((local10 % 10) == 2))
					{
						local14 = "nd - ";
					} else
					{
						if (((local10 % 10) == 3))
						{
							local14 = "rd - ";
						} else
						{
							local14 = "th - ";
						}
					}
				}
				if ((((local10 % 100) >= 11) && ((local10 % 100) <= 13)))
				{
					local14 = "th - ";
				}
				if ((nrPrizeList[local8] == 0))
				{
					local2_2[local8] = (((local10 + local14) + local9.name) + " - PINK SLIPS");
				} else
				{
					local2_2[local8] = ((((local10 + local14) + local9.name) + " - $") + nrPrizeList[local8]);
				}
				local2_2[local8] = ((((local2_2[local8] + " - ") + local9.getPrestigeString(0)) + " - ") + local9.nightVd.vehicleName);
				local8++;
			}
			int local8_2 = new NrOpponentDialog(
				player.controller,
				(Dialog.DF_MODAL | Dialog.DF_FULLSCREEN),
				local4_2,
				local2_2,
				nrPrizeList,
				player.money).display(
			);
			if ((local8_2 < local1_2))
			{
				nrOpponents.removeAllElements();
				nrBotID1 = local3_2[local8_2];
				nrPrize = nrPrizeList[local8_2];
				GameLogic.speedymen[nrBotID1].lastRaceDay = GameLogic.day;
				return 1;
			}
		} else
		{
			new WarningDialog(
				player.controller,
				(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
				"NO OPPONENTS",
				"Sorry, there are no opponents to race against. \n Come back later!").display(
			);
		}
		return 0;
	}

	public void startNightRace(int arg0)
	{
		int local2 = nrStat;
		nrStat = City.NR_INVALID;
		nrPlayerRace = arg0;
		Vector3 local3 = new Vector3();
		local3.diff(player.car.getPos(), pS);
		float local4 = local3.length();
		nrShowRaceStart = (nrPlayerRace || nrWatching);
		if (nrShowRaceStart)
		{
			Input.cursor.enable(0);
			osd.hideGroup(nrWatchingGroup);
			osd.hideGroup(nightRaceGroup);
			if ((!((nrWatching || nrPlayerRace))))
			{
				enableOsd(0);
			}
		}
		pl1 = null;
		pl2 = null;
		if (nrPlayerRace)
		{
			if ((!(selectNrOpponent())))
			{
				nrWatching = backupnrWatching;
				Input.cursor.enable(1);
				if (nrWatching)
				{
					osd.showGroup(nrWatchingGroup);
					enableOsd(0);
				} else
				{
					osd.showGroup(nightRaceGroup);
				}
				nrStat = local2;
				nrPlayerRace = 0;
				return;
			}
			nrHead1 = GameLogic.speedymen[nrBotID1].character;
			nrHead2 = player.character;
			nrName1 = GameLogic.speedymen[nrBotID1].name;
			nrName2 = player.name;
			pl1 = GameLogic.speedymen[nrBotID1];
			pl2 = player;
		} else
		{
			int local5 = (player.club * GameLogic.CLUBMEMBERS);
			int local6 = ((local5 + GameLogic.CLUBMEMBERS) - 1);
			int local7 = GameLogic.findRacer(player);
			nrBotID1 = (Math.random() * GameLogic.CLUBMEMBERS);
			nrBotID1 = (nrBotID1 + local5);
			if ((nrBotID1 == local7))
			{
				if ((local7 == local6))
				{
					nrBotID1 = (local7 - 1);
				} else
				{
					nrBotID1 = (local7 + 1);
				}
			}
			int local8 = 1;
			if ((nrBotID1 >= ((local5 + local6) / 2)))
			{
				local8 = (-(1));
			}
			nrBotID2 = (nrBotID1 + local8);
			if ((nrBotID2 == local7))
			{
				nrBotID2 += local8;
			}
			nrHead1 = GameLogic.speedymen[nrBotID1].character;
			nrHead2 = GameLogic.speedymen[nrBotID2].character;
			nrName1 = GameLogic.speedymen[nrBotID1].name;
			nrName2 = GameLogic.speedymen[nrBotID2].name;
			pl1 = GameLogic.speedymen[nrBotID1];
			pl2 = GameLogic.speedymen[nrBotID2];
		}
		RaceDialog local5_2;
		if (nrShowRaceStart)
		{
			applauseSfxID = applause1.play(pS, 150.0, 1.0, 1.0, (SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP));
			applauseSfxOn = 1;
			int local6_2 = (Math.random() * 5.0);
			local5_2 = new RaceDialog(
				player.controller,
				Dialog.DF_MODAL,
				new ResourceRef((City.RID_NRSTARTBG + local6_2)),
				nrHead2,
				nrHead1,
				nrName2,
				nrName1,
				1,
				1,
				nrPrize,
				0,
				pl2,
				pl1
			);
			Frontend.loadingScreen.show(local5_2);
		}
		cleanupNightRace();
		if ((((!(nrPlayerPaused)) && (!(nrPlayerRace))) && (local4 <= 10.0)))
		{
			nrPlayerPaused = 1;
			Vector3 local6_3 = new Vector3(dirS);
			local6_3.mul((-(10.0)));
			local6_3.add(pS);
			Vector3[] local7_2 = map.alignToRoad(local6_3);
			local6_3.y = local7_2[0].y;
			oriS.y += 0.5;
			player.car.setMatrix(local6_3, oriS);
			oriS.y -= 0.5;
			player.car.command("stop");
		}
		Vector3[] local6_4 = map.alignToRoad(pS);
		Vector3 local7_3;
		Vector3 local8_2;
		if ((!(nrStarterLady)))
		{
			local7_3 = new Vector3(local6_4[0]);
			local8_2 = new Vector3(local6_4[1]);
			local8_2.mul(5.0);
			local7_3.add(local8_2);
			nrStarterLady = new RenderRef(map, humans:0x70r, "gizi");
			nrStarterLady.setMatrix(local7_3, new Ypr(local8_2));
			nrStarterLadyAnim = new Animation(nrStarterLady, new ResourceRef(humans:0x7cr));
		}
		local7_3 = new Vector3(local6_4[0]);
		local8_2 = new Vector3(local6_4[1]);
		local8_2.mul(2.0);
		local8_2.rotate(new Ypr(1.57, 0.0, 0.0));
		local7_3.add(local8_2);
		float local9;
		int local10;
		int local11;
		if (nrPlayerRace)
		{
			nrBot1 = GameLogic.speedymen[nrBotID1];
			nrBot1.setDriverObject(GameLogic.HUMAN_OPPONENT);
			int local12 = GameLogic.findRacer(nrBot1);
			Vehicle local13 = new Vehicle(
				this,
				nrBot1.nightVd.id,
				nrBot1.nightVd.colorIndex,
				nrBot1.nightVd.optical,
				nrBot1.nightVd.power,
				nrBot1.nightVd.wear,
				nrBot1.nightVd.tear
			);
			local13.races_won = nrBot1.nightWins;
			local13.races_lost = nrBot1.nightLoses;
			nrBot1.createCar(map, local13);
			nrBot1.car.setMatrix(local7_3, oriS);
			nrBot1.car.setParent(map);
			nrBot1.car.command("reload");
			nrBot1.car.command("stop");
			nrBot1.car.command("idle");
			mRaceBot = nav.addMarker(nrBot1);
		} else
		{
			local9 = (((Math.random() * 2) - 1) * yawNudge);
			local10 = (Math.random() * fakers.length);
			local11 = (Math.random() * 12345);
			oriS.y += local9;
			GameRef local12_2 = new GameRef(
				map,
				fakers[local10],
				((((local7_3.toString() + ",") + oriS.y) + ",0,0,") + local11),
				"nightracer1"
			);
			local12_2.setMatrix(local7_3, oriS);
			oriS.y -= local9;
			nrBot1 = GameLogic.speedymen[nrBotID1];
			nrBot1.setDriverObject(GameLogic.HUMAN_OPPONENT);
			nrBot1.createCar(map, new Vehicle(local12_2));
			nrBot1.car.command("stop");
			nrBot1.car.command("idle");
		}
		local7_3.sub(local8_2);
		local7_3.sub(local8_2);
		if (nrPlayerRace)
		{
			player.car.setMatrix(local7_3, oriS);
			player.car.command("stop");
			player.car.command("idle");
			nrPlayerRace = 2;
		} else
		{
			local9 = (((Math.random() * 2) - 1) * yawNudge);
			local10 = (Math.random() * fakers.length);
			local11 = (Math.random() * 12345);
			oriS.y += local9;
			GameRef local12_3 = new GameRef(
				map,
				fakers[local10],
				((((local7_3.toString() + ",") + oriS.y) + ",0,0,") + local11),
				"nightracer1"
			);
			local12_3.setMatrix(local7_3, oriS);
			oriS.y -= local9;
			nrBot2 = GameLogic.speedymen[nrBotID2];
			nrBot2.setDriverObject(GameLogic.HUMAN_OPPONENT);
			nrBot2.createCar(map, new Vehicle(local12_3));
			nrBot2.car.command("stop");
			nrBot2.car.command("idle");
		}
		nrFinishTrigger = addTrigger(
			pF,
			null,
			Marker.RR_FINISH,
			"event_handlerNrFinish",
			8,
			"night race finish trigger"
		);
		nrFinishSoundTrigger = addTrigger(
			pF,
			null,
			Marker.RR_FINISH,
			"event_handlerNrFinishSound",
			30,
			"night race finish sound trigger"
		);
		if (nrShowRaceStart)
		{
			backupCamera();
			if (nrPlayerRace)
			{
				changeCamTarget(player.car);
				changeCamTarget2(nrBot1.car);
				changeCamFollow();
			} else
			{
				if (nrWatching)
				{
					lookBot(null, 1);
					cameraTarget = nrBot1.car;
					cameraTarget2 = nrBot2.car;
					nrLookAt = 0;
				} else
				{
					changeCamTarget2(nrBot1.car);
					changeCamChase();
				}
			}
			Frontend.loadingScreen.display(local5_2, 0.0);
			nrStat = City.NR_3;
			if (nrPlayerRace)
			{
				if (cam)
				{
					changeCamTarget2(nrBot1.car);
					cam.command("angle 0 4.0 0.7853");
					cam.command("dist 5.5 6.5");
				}
				nrDelay = 7;
			} else
			{
				Input.cursor.enable(1);
				if (nrWatching)
				{
					osd.showGroup(nrWatchingGroup);
				}
				nrDelay = 1;
			}
		} else
		{
			nrStat = City.NR_START;
			nrDelay = 0;
		}
	}

	public void startNightRace2()
	{
		nrFinished1 = 0;
		nrFinished2 = 0;
		if (nrShowRaceStart)
		{
			setMessage("GO!");
			speechGO.play();
		}
		nrStartTime = System.simTime();
		if (nrPlayerRace)
		{
			nrBot1.car.command("start");
			nrBot1.startRace(pF, player);
			nrBot1.brain.command("AI_NightRace");
			player.car.setCruiseControl(0);
			player.car.command("start");
			cam.command("angle 0 0");
			cam.command("dist 8.0 16.0");
		} else
		{
			nrBot1.car.command("start");
			nrBot2.car.command("start");
			nrBot1.startRace(pF, nrBot2);
			nrBot2.startRace(pF, nrBot1);
			if ((!(nrWatching)))
			{
			}
		}
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void finishNightRace()
	{
		nrStat = City.NR_INVALID;
		RaceDialog local1;
		int local2;
		nrQuit = 0;
		if ((nrPlayerRace == 2))
		{
			if ((nrTime2 < nrTime1))
			{
				local2 = City.RID_NRFIN_WIN;
			} else
			{
				local2 = City.RID_NRFIN_LOSE;
			}
		} else
		{
			local2 = City.RID_NRFIN_AI;
		}
		if (nrShowRaceFinish)
		{
			if (((nrPlayerRace == 2) || nrWatching))
			{
				cam.command("angle 0 0");
				cam.command("dist 8.0 16.0");
			}
			if (nrWatching)
			{
				nrBot1.brain.command("camera 0");
				nrBot2.brain.command("camera 0");
			} else
			{
				enableOsd(0);
			}
			if ((nrTime2 < nrTime1))
			{
				local1 = new RaceDialog(
					player.controller,
					Dialog.DF_MODAL,
					new ResourceRef(local2),
					nrHead2,
					nrHead1,
					nrName2,
					nrName1,
					nrTime2,
					nrTime1,
					nrPrize,
					1,
					pl2,
					pl1
				);
			} else
			{
				local1 = new RaceDialog(
					player.controller,
					Dialog.DF_MODAL,
					new ResourceRef(local2),
					nrHead1,
					nrHead2,
					nrName1,
					nrName2,
					nrTime1,
					nrTime2,
					nrPrize,
					1,
					pl1,
					pl2
				);
			}
			Frontend.loadingScreen.show(local1);
			Vector3 local3 = player.car.getPos();
			Ypr local4 = player.car.getOri();
			GameLogic.autoSaveQuiet();
			player.car.setParent(map);
			player.car.setMatrix(local3, local4);
			player.car.command("idle");
		}
		if ((nrPlayerRace == 2))
		{
			if (((nrBot1.bestNightQM == 0.0) || (nrTime1 < nrBot1.bestNightQM)))
			{
				nrBot1.bestNightQM = nrTime1;
			}
			if ((nrTime2 < nrTime1))
			{
				if (((player.car.bestNightQM == 0.0) || (nrTime2 < player.car.bestNightQM)))
				{
					player.car.bestNightQM = nrTime2;
				}
				speechYOUWIN.play();
				player.car.races_won++;
				player.races_won++;
				nrBot1.nightLoses++;
				nrBot1.car.races_lost++;
				nrBot1.brain.command("camera 0");
				if ((nrPrize > 0))
				{
					player.money += nrPrize;
				} else
				{
					player.winPinkSlips++;
					nrBot1.leaveCar(0);
					player.carlot.addCar(nrBot1.car);
					nrBot1.car = null;
					player.carlot.saveCar(player.carlot.curcar);
					player.carlot.flushCars();
					Vector3 local3_2 = player.car.getPos();
					Ypr local4_2 = player.car.getOri();
					GameLogic.autoSaveQuiet();
					player.car.setParent(map);
					player.car.setMatrix(local3_2, local4_2);
				}
			} else
			{
				player.car.races_lost++;
				player.races_lost++;
				nrBot1.nightWins++;
				nrBot1.car.races_won++;
				if ((nrPrize > 0))
				{
					player.money -= nrPrize;
				} else
				{
					nrQuit = 1;
				}
			}
			GameLogic.challenge(
				GameLogic.findRacer(player),
				GameLogic.findRacer(nrBot1),
				0,
				(nrTime2 < nrTime1),
				1
			);
			refreshStatus();
		} else
		{
			GameLogic.challenge(
				GameLogic.findRacer(nrBot2),
				GameLogic.findRacer(nrBot1),
				0,
				(nrTime2 < nrTime1),
				1
			);
		}
		Vector3 local3_3 = player.car.getPos();
		int local4_3 = 0;
		while ((local4_3 < parkingCars.length))
		{
			if (parkingCars[local4_3])
			{
				parkingCars[local4_3].reset(local3_3);
			}
			local4_3++;
		}
		cleanupNightRace();
		if (nrShowRaceFinish)
		{
			if ((nrPlayerRace == 2))
			{
				player.car.command("start");
				nrPlayerRace = 0;
			}
			if (nrWatching)
			{
				player.controller.command(("camera " + cam.id()));
				restoreCamera();
			} else
			{
				enableOsd(1);
			}
			Frontend.loadingScreen.display(local1, 3.0);
		}
		if (nrQuit)
		{
			Thread.sleep(1000);
			killCar = 1;
			GameLogic.changeActiveSection(GameLogic.garage);
		} else
		{
			nrDelay = 15;
			nrStat = City.NR_IDLE;
		}
	}

	public void cleanupNightRace()
	{
		if (nrBot1)
		{
			nrBot1.deleteCar();
			if (mRaceBot)
			{
				nav.remMarker(mRaceBot);
				mRaceBot = null;
			}
		}
		if (nrBot2)
		{
			nrBot2.deleteCar();
		}
		if (nrFinishTrigger)
		{
			removeTrigger(nrFinishTrigger);
		}
		if (nrFinishSoundTrigger)
		{
			removeTrigger(nrFinishSoundTrigger);
		}
		nrBot1 = (nrBot2 = (nrFinishTrigger = (nrFinishSoundTrigger = null)));
	}

	public void nightRaceStep()
	{
		if ((nrDelay > 0))
		{
			nrDelay--;
			return;
		}
		if ((nrStat == City.NR_IDLE))
		{
			if ((player.car.getPos().sub(pS).length() < 500.0))
			{
				startNightRace(0);
			} else
			{
				nrDelay = 10;
			}
		} else
		{
			if ((nrStat == City.NR_3))
			{
				if (nrPlayerRace)
				{
					cam.command("angle 0 3.0");
				}
				setMessage("3");
				speech3.play();
				if (nrBot1)
				{
					nrBot1.brain.command("AI_BeginRace 0.5");
				}
				if (nrBot2)
				{
					nrBot2.brain.command("AI_BeginRace 0.5");
				}
				nrStat = City.NR_2;
				nrDelay = 0;
			} else
			{
				if ((nrStat == City.NR_2))
				{
					setMessage("2");
					speech2.play();
					nrStarterLadyAnim.setSpeed(2.0);
					nrStarterLadyAnim.play();
					nrStat = City.NR_1;
					nrDelay = 0;
				} else
				{
					if ((nrStat == City.NR_1))
					{
						setMessage("1");
						speech1.play();
						if (nrBot1)
						{
							nrBot1.brain.command("AI_BeginRace 1.0");
						}
						if (nrBot2)
						{
							nrBot2.brain.command("AI_BeginRace 1.0");
						}
						nrStat = City.NR_START;
						nrDelay = 0;
					} else
					{
						if ((nrStat == City.NR_START))
						{
							startNightRace2();
							nrStat = City.NR_RACE;
							nrDelay = 1;
						} else
						{
							if ((nrStat == City.NR_RACE))
							{
								if (nrStarterLady)
								{
									nrStarterLady.destroy();
									nrStarterLady = null;
									nrStarterLadyAnim = null;
								}
								if ((nrPlayerPaused && (!(nrWatching))))
								{
									player.car.command("start");
									nrPlayerPaused = 0;
								}
								if (nrWatching)
								{
									if ((((nrBot1 && nrBot2) && (!(nrFinished1))) && (!(nrFinished2))))
									{
										Vector3 local1 = new Vector3();
										Vector3 local2 = new Vector3();
										local1.diff(nrBot1.car.getPos(), pF);
										local2.diff(nrBot2.car.getPos(), pF);
										float local3 = local1.length();
										float local4 = local2.length();
										if ((local3 > local4))
										{
											if ((nrLookAt != 1))
											{
												if (cameraTarget)
												{
													changeCamTarget2(nrBot2.car);
												}
												lookBot(nrBot1, 0);
												nrLookAt = 1;
											}
										} else
										{
											if ((nrLookAt != 2))
											{
												if (cameraTarget)
												{
													changeCamTarget2(nrBot1.car);
												}
												lookBot(nrBot2, 0);
												nrLookAt = 2;
											}
										}
									}
								}
							} else
							{
								if ((nrStat == City.NR_FINISH))
								{
									finishNightRace();
								}
							}
						}
					}
				}
			}
		}
	}

	public void event_handlerNrFinishSound(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((arg1 == GameRef.EVENT_TRIGGER_ON))
		{
			if ((nrStat == City.NR_RACE))
			{
				int local5;
				int local6 = arg2.token(0).intValue();
				if ((local6 == nrBot1.car.id()))
				{
					local5 = 1;
				}
				if ((nrPlayerRace == 2))
				{
					if ((local6 == player.car.id()))
					{
						local5 = 1;
					}
				} else
				{
					if ((local6 == nrBot2.car.id()))
					{
						local5 = 1;
					}
				}
				if (local5)
				{
					applause2.play(pF, 150.0, 1.0, 1.0, 0);
				}
			}
		}
	}

	public void event_handlerNrFinish(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((arg1 == GameRef.EVENT_TRIGGER_ON))
		{
			if ((nrStat == City.NR_RACE))
			{
				int local5 = arg2.token(0).intValue();
				if ((!(nrFinished1)))
				{
					if ((local5 == nrBot1.car.id()))
					{
						nrTime1 = (System.simTime() - nrStartTime);
						nrFinished1 = 1;
						nrBot1.stop();
						if ((!(nrFinished2)))
						{
							addTimer(10, 7);
						}
					}
				}
				if ((!(nrFinished2)))
				{
					if ((nrPlayerRace == 2))
					{
						if ((local5 == player.car.id()))
						{
							nrTime2 = (System.simTime() - nrStartTime);
							nrFinished2 = 1;
							player.car.command("brake");
							if ((!(nrFinished1)))
							{
								addTimer(10, 7);
							}
						}
					} else
					{
						if ((local5 == nrBot2.car.id()))
						{
							nrTime2 = (System.simTime() - nrStartTime);
							nrFinished2 = 1;
							nrBot2.stop();
							if ((!(nrFinished1)))
							{
								addTimer(10, 7);
							}
						}
					}
				}
				if ((nrFinished1 && nrFinished2))
				{
					nrStat = City.NR_INVALID;
					Vector3 local6 = new Vector3();
					local6.diff(player.car.getPos(), pF);
					float local7 = local6.length();
					nrShowRaceFinish = (((nrPlayerRace == 2) || nrWatching) || ((local7 <= 25.0) && (player.car.getSpeedSquare() < City.TINY_SPEED_SQ)));
					if (nrShowRaceFinish)
					{
						if (((nrPlayerRace == 2) || nrWatching))
						{
							changeCamFollow();
							cam.command("dist 2.5 10.0");
							cam.command("smooth 0.5 0.5");
							cam.command("force 1.6 0.5 -0.7");
							cam.command("torque 0.05");
							cam.command("angle 0 4.0 0.7853");
							cam.command("dist 5.5 6.5");
							nrDelay = 8;
						}
					}
					nrStat = City.NR_FINISH;
				}
			}
		}
	}

	public void event_handlerNrStart(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((arg1 == GameRef.EVENT_TRIGGER_ON))
		{
			if ((local4 == player.car.id()))
			{
				if ((!(nrPlayerRace)))
				{
					nrNear = 1;
					osd.showGroup(nightRaceGroup);
					Input.cursor.enable(1);
				}
			}
		} else
		{
			if ((arg1 == GameRef.EVENT_TRIGGER_OFF))
			{
				if ((local4 == player.car.id()))
				{
					if ((!(nrPlayerRace)))
					{
						if (nrWatching)
						{
							if ((nrBot1 && nrBot2))
							{
								nrBot1.brain.command("camera 0");
								nrBot2.brain.command("camera 0");
							}
							restoreCamera();
							osd.hideGroup(nrWatchingGroup);
							nrWatching = 0;
							enableOsd(1);
						} else
						{
							osd.hideGroup(nightRaceGroup);
						}
						Input.cursor.enable(0);
						nrShowRaceStart = 0;
						nrShowRaceFinish = 0;
						nrNear = 0;
					}
				}
				if (((nrStat == City.NR_RACE) && applauseSfxOn))
				{
					int local5;
					if ((local4 == nrBot1.car.id()))
					{
						local5 = 1;
					}
					if ((nrPlayerRace == 2))
					{
						if ((local4 == player.car.id()))
						{
							local5 = 1;
						}
					} else
					{
						if ((local4 == nrBot2.car.id()))
						{
							local5 = 1;
						}
					}
					if (local5)
					{
						applause1.stop(applauseSfxID);
						applauseSfxOn = 0;
					}
				}
			}
		}
	}

	public void event_handlerRaceFinish(GameRef arg0, int arg1, String arg2)
	{
		if ((raceState == 1))
		{
			int local4 = arg2.token(0).intValue();
			int local5;
			if ((local4 == player.car.id()))
			{
				local5 = 1;
			} else
			{
				if ((local4 == raceBot.car.id()))
				{
					local5 = 2;
				} else
				{
					if ((demoBot && (local4 == demoBot.car.id())))
					{
						local5 = 1;
					}
				}
			}
			if (local5)
			{
				raceState = 2;
				System.timeWarp(0.1);
				addTimer((0.1 * 10.0), 14);
				if ((player && (local4 == player.car.id())))
				{
					player.car.command("brake");
					changeCamTarget(player.car);
					changeCamTarget2(raceBot.car);
				} else
				{
					if ((demoBot && (local4 == demoBot.car.id())))
					{
						demoBot.car.command("brake");
						demoBot.stop();
						changeCamTarget(demoBot.car);
						changeCamTarget2(raceBot.car);
					} else
					{
						if ((raceBot && (local4 == raceBot.car.id())))
						{
							raceBot.car.command("brake");
							raceBot.stop();
							changeCamTarget(raceBot.car);
							if ((demoBot && demoBot.car))
							{
								changeCamTarget2(demoBot.car);
							} else
							{
								if (player.car)
								{
									changeCamTarget2(player.car);
								}
							}
						}
					}
				}
				changeCamNone();
				lastCamPos = arg0.getPos();
				lastCamPos.y += 2.0;
				changeCamChase();
				if (cam)
				{
					cam.command("simulate 1");
				}
				if ((GameLogic.gameMode == GameLogic.GM_DEMO))
				{
					return;
				} else
				{
					player.car.command("brake");
					int local6 = (!(((challenger == player) ^ (local5 == 1))));
					if ((local5 == 1))
					{
						speechDAYYOUWIN.play();
						raceDialog = new RaceDialog(
							player.controller,
							(Dialog.DF_MODAL | Dialog.DF_FREEZE),
							new ResourceRef(City.RID_DAY_WIN),
							player.character,
							raceBot.character,
							player.name,
							raceBot.name,
							0.0,
							0.0,
							prize,
							0,
							player,
							raceBot
						);
					} else
					{
						speechDAYYOULOOSE.play();
						raceDialog = new RaceDialog(
							player.controller,
							(Dialog.DF_MODAL | Dialog.DF_FREEZE),
							new ResourceRef(City.RID_DAY_LOOSE),
							raceBot.character,
							player.character,
							raceBot.name,
							player.name,
							0.0,
							0.0,
							prize,
							0,
							raceBot,
							player
						);
					}
					if ((GameLogic.gameMode == GameLogic.GM_CARREER))
					{
						if ((local5 == 1))
						{
							player.money += prize;
							player.car.races_won++;
							player.races_won++;
						} else
						{
							player.money -= prize;
							player.car.races_lost++;
							player.races_lost++;
						}
						GameLogic.challenge(
							GameLogic.findRacer(challenger),
							GameLogic.findRacer(challenged),
							abandoned,
							local6,
							0
						);
						refreshStatus();
					}
				}
			}
		}
	}

	public void startQuickRace()
	{
		Frontend.loadingScreen.show();
		createQuickRaceBot();
		raceState = 1;
		if ((!(GameLogic.racesetup)))
		{
			GameLogic.racesetup = new RaceSetup();
		}
		GameLogic.changeActiveSection(GameLogic.racesetup);
	}

	public void cleanupRace()
	{
		abandoned = 1;
		if ((!(raceState)))
		{
			return;
		}
		raceState = 0;
		if (raceBot)
		{
			if (raceBot.dummycar)
			{
				if ((raceBot.dummycar.id() != raceBot.car.id()))
				{
					Vector3 local1 = raceBot.car.getPos();
					Ypr local2 = raceBot.car.getOri();
					raceBot.deleteCar();
					raceBot.dummycar.command("reset");
					raceBot.dummycar.setMatrix(local1, local2);
					raceBot.dummycar.setParent(map);
					raceBot.car = new Vehicle(raceBot.dummycar);
					raceBot.brain = new GameRef(map, sl:0x6er, "", "BOTBRAIN");
					RenderRef local3 = new RenderRef(map, raceBot.driverID, "botfigura-afterrace");
					raceBot.brain.command(("renderinstance " + local3.id()));
					raceBot.brain.command(("controllable " + raceBot.dummycar.id()));
					raceBot.traffic_id = 0;
				}
				if (mRaceBot)
				{
					nav.remMarker(mRaceBot);
					mRaceBot = null;
				}
				raceBot.reJoinTraffic();
				raceBot.setTrafficBehaviour(GameRef.TC_PASSIVE);
			} else
			{
				destroyRaceBot();
			}
		}
		if (trRaceFinish)
		{
			trRaceFinish.finalize();
			trRaceFinish = null;
		}
		nav.remMarker(mStart);
		nav.remMarker(mFinish);
		if (nav.route)
		{
			nav.route.destroy();
			nav.route = null;
		}
		finishObject.destroy();
	}

	public void abandonRace()
	{
		GameLogic.challenge(GameLogic.findRacer(challenger), GameLogic.findRacer(challenged), 1, 0, 0);
		refreshStatus();
		raceState = 0;
		raceBot.setTrafficBehaviour(GameRef.TC_PASSIVE);
		abandoned = 1;
		if (nav.route)
		{
			nav.route.destroy();
			nav.route = null;
		}
	}

	public void backupCamera()
	{
		if ((!(backupCam)))
		{
			backupCam = 1;
			nrcameraMode_before = cameraMode;
			nrcameraTarget = cameraTarget;
		}
	}

	public void restoreCamera()
	{
		if (backupCam)
		{
			backupCam = 0;
			cameraTarget = nrcameraTarget;
			cameraMode = nrcameraMode_before;
			if ((cameraMode == Track.CAMMODE_FOLLOW))
			{
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
						changeCamFollow();
					}
				}
			}
		}
	}

	public int[] calculateFineSum(int arg0)
	{
		int[] local2 = new int[5];
		if (overSpeed)
		{
			local2[1] = (((overSpeed * 3.6) * 5) + 30);
		}
		local2[2] = (crashes * 100);
		if (fleedAway)
		{
			float local3;
			if (pullOverTime)
			{
				local3 = pullOverTime;
			} else
			{
				local3 = System.simTime();
			}
			local2[3] = (200 + ((local3 - firstAlertTime) * 10));
		}
		local2[4] = 0.0;
		if (((System.simTime() - firstAlertTime) > (1.0 * 60.0)))
		{
			if (GameLogic.player)
			{
				if (GameLogic.player.car)
				{
					local2[4] = GameLogic.player.car.calcPoliceFine((((System.simTime() - firstAlertTime) - (1.0 * 60.0)) / 30.0));
				}
			}
		}
		local2[0] = (((local2[1] + local2[2]) + local2[3]) + local2[4]);
		if (arg0)
		{
			overSpeed = 0;
			crashes = 0;
			fleedAway = 0;
		}
		return local2;
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		super.handleEvent(arg0, arg1, arg2);
		if ((arg1 == GameRef.EVENT_TIME))
		{
			switch (arg2)
			{
				case 2:
					if (timeLock)
					{
					} else
					{
						timeLock = 1;
						time++;
						if ((GameLogic.actualState == this))
						{
							if (nightTime)
							{
								nightRaceStep();
								if (nrQuit)
								{
									timeLock = 0;
									break;
								}
							}
							float local4 = player.car.getSpeedSquare();
							int local5;
							if ((policeState || (local4 > City.SPEED_LIMIT_SQ)))
							{
								alertPolice();
							}
							int local6 = (alertedScouts.size() - 1);
							while ((local6 >= 0))
							{
								PoliceScout local7 = alertedScouts.elementAt(local6);
								if (local7.returningTraffic)
								{
									if (local7.bot.traffic_id)
									{
										alertedScouts.removeElementAt(local6);
										policeCars.addElement(local7.tracker);
									} else
									{
										Vector3 local8 = local7.bot.car.getPos();
										local8.sub(player.car.getPos());
										float local9 = local8.length();
										if ((local9 > 600))
										{
											sleepPoliceScoutQuick(local7);
										}
									}
								} else
								{
									if ((!(local7.bot.car.id())))
									{
										System.exit((("s elhulltanak legjobbjaink a hosszu harc alatt: " + Integer.toHexString(local7.bot.debugid)) + "\n"));
									}
									local7.distance = map.getRouteLength(
										local7.bot.car.getPos(),
										player.car.getPos()
									);
									local5++;
									if ((local7.distance >= 0))
									{
										if ((local7.distance < 300.0))
										{
											float local8_2 = (player.car.hasCrime() * 1.1);
											if ((local8_2 >= 0))
											{
												local8_2 = (Math.sqrt(local4) - local8_2);
												if ((local8_2 > overSpeed))
												{
													overSpeed = local8_2;
												}
											}
										}
										if (fleedAway)
										{
											if ((!(pullOverTime)))
											{
												if ((local4 < City.TINY_SPEED_SQ))
												{
													pullOverTime = System.simTime();
												}
											} else
											{
												if ((local4 >= City.TINY_SPEED_SQ))
												{
													pullOverTime = 0.0;
												}
											}
										} else
										{
											if ((local4 >= City.TINY_SPEED_SQ))
											{
												if (((System.simTime() - firstAlertTime) > 10.0))
												{
													fleedAway = 1;
													pullOverTime = 0.0;
												}
											}
										}
										if ((local7.distance < 10.0))
										{
											if ((local4 < City.TINY_SPEED_SQ))
											{
												if ((local7.bot.car.getSpeedSquare() < City.TINY_SPEED_SQ))
												{
													policeState = 0;
													roamFree = 1;
													addTimer(5, 13);
													if ((GameLogic.gameMode != GameLogic.GM_DEMO))
													{
														int[] local8_3 = calculateFineSum(1);
														int local9_2 = osdEnabled;
														enableOsd(0);
														Frontend.loadingScreen.display(
															new FineDialog(player.controller, ((Dialog.DF_MODAL | Dialog.DF_FREEZE) | Dialog.DF_FULLSCREEN), player.name, local8_3),
															5.0
														);
														enableOsd(1);
														player.money -= local8_3[0];
														if ((player.money < 0))
														{
															player.money = 0;
														}
														player.decreasePrestige(Racer.PRESTIGE_STEP);
														refreshStatus();
													}
													int local8_4 = (alertedScouts.size() - 1);
													while ((local8_4 >= 0))
													{
														PoliceScout local9_3 = alertedScouts.elementAt(local8_4);
														sleepPoliceScout(local9_3);
														local8_4--;
													}
													break;
												}
											}
										} else
										{
											if ((local7.distance < 100.0))
											{
												if ((GameLogic.gameMode != GameLogic.GM_DEMO))
												{
													if ((policeState == 10))
													{
														setMessage("POLICE: PULL OVER NOW!");
													}
												}
											} else
											{
												if ((local7.distance > (400.0 + (100.0 * player.club))))
												{
													Vector3 local8_5 = local7.bot.car.getPos();
													local8_5.sub(player.car.getPos());
													float local9_4 = local8_5.length();
													if ((local9_4 > 600))
													{
														sleepPoliceScoutQuick(local7);
													} else
													{
														sleepPoliceScout(local7);
													}
												}
											}
										}
									}
								}
								local6--;
							}
							if ((policeState && (!(local5))))
							{
								if ((!(--policeState)))
								{
									setMessage("GOT AWAY!");
									player.increasePrestige((2 * Racer.PRESTIGE_STEP));
									calculateFineSum(1);
									refreshStatus();
								}
							}
							if ((!(raceState)))
							{
								if (((GameLogic.gameMode == GameLogic.GM_CARREER) && GameLogic.carrerInProgress))
								{
									TrafficTracker local6_2 = null;
									float local7_2 = 100000.0;
									int local8_6 = (opponentCars.size() - 1);
									while ((local8_6 >= 0))
									{
										TrafficTracker local9_5 = opponentCars.elementAt(local8_6);
										GameRef local10 = local9_5.car;
										if (local10.id())
										{
											Vector3 local11 = local10.getPos();
											local11.sub(player.car.getPos());
											float local12 = local11.length();
											if ((local12 < local7_2))
											{
												local7_2 = local12;
												local6_2 = local9_5;
											}
										}
										local8_6--;
									}
									int local8_7 = 0;
									if (local6_2)
									{
										if ((local7_2 < 40.0))
										{
											if (raceBot)
											{
												if ((raceBot != local6_2.bot))
												{
													raceBot.releaseHorn();
													raceBot = null;
													oppStatusTxt.changeText(null);
													oppStatusDisplayed = 0;
													aiChallengeState = 0;
													abandoned2 = 0;
												}
											}
											if ((!(raceBot)))
											{
												raceBot = local6_2.bot;
											}
											if ((!(oppStatusDisplayed)))
											{
												int local9_6 = (GameLogic.CLUBMEMBERS - (GameLogic.findRacer(raceBot) - (GameLogic.CLUBMEMBERS * raceBot.club)));
												oppStatusTxt.changeText(((((((raceBot.name + "  ") + GameLogic.CLUBNAMES[raceBot.club]) + "/") + local9_6) + " >") + raceBot.getPrestigeString()));
												if (GameLogic.canChallenge(player, raceBot))
												{
													oppStatusDisplayed = 1;
												} else
												{
													oppStatusDisplayed = 2;
												}
												if ((!(policeState)))
												{
													changeCamTarget2(raceBot.dummycar);
												}
											}
											if ((oppStatusDisplayed == 1))
											{
												oppStatusTxt.changeColor(1627389951);
											} else
											{
												if ((oppStatusDisplayed == 2))
												{
													oppStatusTxt.changeColor(1627346261);
												}
											}
											if (((!(policeState)) && (local7_2 < 20.0)))
											{
												if ((!(abandoned2)))
												{
													if (aiChallengeState)
													{
														if ((Math.random() > 0.4))
														{
															raceBot.releaseHorn();
														} else
														{
															raceBot.pressHorn();
														}
													} else
													{
														if ((Math.random() < 0.4))
														{
															if (GameLogic.canChallenge(raceBot, player))
															{
																raceBot.pressHorn();
																aiChallengeState = 1;
															} else
															{
																abandoned2 = 1;
															}
														} else
														{
															abandoned2 = 1;
														}
													}
												}
											}
											if ((local7_2 < 10.0))
											{
												if (player.car.getHorn())
												{
													raceBot.releaseHorn();
													player.car.command("sethorn 0");
													int local9_7 = aiChallengeState;
													if ((!(local9_7)))
													{
														local9_7 = new RacerTalkDialog(
															player.controller,
															player,
															raceBot,
															abandoned,
															policeState).display(
														);
													}
													if (local9_7)
													{
														Bot local10_2 = raceBot;
														raceState = 1;
														if (aiChallengeState)
														{
															aiChallengeState = 0;
															challenger = raceBot;
															challenged = player;
														} else
														{
															challenger = player;
															challenged = raceBot;
														}
														if ((!(GameLogic.racesetup)))
														{
															GameLogic.racesetup = new RaceSetup();
														}
														GameLogic.changeActiveSection(GameLogic.racesetup);
													} else
													{
														abandoned2 = 1;
													}
												}
											}
											local8_7 = 1;
										}
									}
									if ((!(local8_7)))
									{
										if (oppStatusDisplayed)
										{
											if (raceBot)
											{
												raceBot.releaseHorn();
												raceBot = null;
											}
											oppStatusTxt.changeText(null);
											oppStatusDisplayed = 0;
											if ((!(policeState)))
											{
												changeCamTarget2(null);
											}
											abandoned = 0;
											abandoned2 = 0;
										}
									}
								}
							}
						}
						timeLock = 0;
					}
					break;
				case 7:
					if ((nrStat == City.NR_RACE))
					{
						if ((nrFinished1 != nrFinished2))
						{
							if ((nrFinished1 || nrFinished2))
							{
								int local4_2;
								if (nrFinished1)
								{
									nrFinished2 = 1;
									nrTime2 = 1000.0;
									if ((nrPlayerRace == 2))
									{
										player.car.command("brake");
										local4_2 = 1;
									} else
									{
										nrBot2.stop();
									}
								} else
								{
									nrFinished1 = 1;
									nrTime1 = 1000.0;
									nrBot1.stop();
								}
								Vector3 local5_2 = new Vector3();
								local5_2.diff(player.car.getPos(), pF);
								float local6_3 = local5_2.length();
								nrShowRaceFinish = (((nrPlayerRace == 2) || nrWatching) || ((local6_3 <= 25.0) && (player.car.getSpeedSquare() < City.TINY_SPEED_SQ)));
								if (nrShowRaceFinish)
								{
									if (((nrPlayerRace == 2) || nrWatching))
									{
										changeCamFollow();
										cam.command("dist 2.5 10.0");
										cam.command("smooth 0.5 0.5");
										cam.command("force 1.6 0.5 -0.7");
										cam.command("torque 0.05");
										cam.command("angle 0 4.0 0.7853");
										cam.command("dist 5.5 6.5");
										if (local4_2)
										{
											nrDelay = 3;
										} else
										{
											nrDelay = 8;
										}
									}
								}
								nrStat = City.NR_FINISH;
							}
						}
					}
					break;
				case 8:
					if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
					{
						if ((!(new YesNoDialog(player.controller, ((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_FREEZE), "QUICKRACE", "Want another Race?").display())))
						{
							startQuickRace();
							break;
						}
					}
					GameLogic.changeActiveSection(parentState);
					break;
				case 9:
					setMessage("3");
					speech3.play();
					addTimer(1, 10);
					break;
				case 10:
					setMessage("2");
					speech2.play();
					addTimer(1, 11);
					break;
				case 11:
					setMessage("1");
					speech1.play();
					raceBot.brain.command("AI_BeginRace 1.0");
					addTimer(1, 12);
					break;
				case 12:
					setMessage("GO!");
					speechGO.play();
					startRace2();
					break;
				case 13:
					roamFree = 0;
					break;
				case 14:
					if ((GameLogic.gameMode == GameLogic.GM_DEMO))
					{
						System.timeWarp(1.0);
						if (cam)
						{
							cam.command("simulate 0");
						}
						cleanupRace();
						GameLogic.changeActiveSection(parentState);
					} else
					{
						System.timeWarp(1.0);
						if (cam)
						{
							cam.command("simulate 0");
						}
						if (raceDialog)
						{
							Frontend.loadingScreen.show(raceDialog);
							Frontend.loadingScreen.userWait();
						}
						cleanupRace();
						if (player.car)
						{
							player.car.command("reset");
							player.car.command("start");
							changeCamTarget(player.car);
							changeCamFollow();
						}
						if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
						{
							addTimer(3, 8);
						} else
						{
							Sound.changeMusicSet(Sound.MUSIC_SET_DRIVING);
						}
					}
					break;
			}
		}
	}

	public void handleEvent(GameRef arg0, int arg1, String arg2)
	{
		if ((arg1 == GameRef.EVENT_COLLISION))
		{
			collision = 1;
			float local4 = System.simTime();
			if (((local4 - lastCollisionTime) > 1.0))
			{
				lastCollisionTime = local4;
				GameRef local5 = new GameRef(arg2.token(0).intValue());
				int local6 = local5.getInfo(GameType.GII_CATEGORY);
				if ((local6 == GameType.GIR_CAT_VEHICLE))
				{
					if ((GameLogic.gameMode != GameLogic.GM_DEMO))
					{
						changeCamTarget2(local5);
					}
					alertPolice();
					if (policeState)
					{
						crashes++;
					}
				}
			}
		} else
		{
			if ((arg1 == GameRef.EVENT_COMMAND))
			{
				String local4_2 = arg2.token(0);
				if ((local4_2 == "car_add"))
				{
					TrafficTracker local5_2 = new TrafficTracker();
					local5_2.id = arg2.token(1).intValue();
					local5_2.trafficId = arg2.token(2).intValue();
					local5_2.car = new GameRef(local5_2.id);
					int local6_2 = local5_2.car.getInfo(GameType.GII_TYPE);
					if ((local6_2 == City.GRI_POLICECAR))
					{
						local5_2.m = nav.addMarker(Marker.RR_POLICE, local5_2.car);
						policeCars.addElement(local5_2);
					} else
					{
						int local7 = 0;
						while ((local7 < GameLogic.speedymen.length))
						{
							Racer local8 = GameLogic.speedymen[local7];
							if ((local8 instanceof Bot))
							{
								if ((((Bot) local8).dummycar && (((Bot) local8).dummycar.id() == local5_2.id)))
								{
									local5_2.m = nav.addMarker(local8.getMarker(), ((Bot) local8).dummycar);
									local5_2.bot = local8;
									opponentCars.addElement(local5_2);
									RenderRef local9 = new RenderRef(
										map,
										((Bot) local8).driverID,
										"botfigura-corpse"
									);
									local5_2.car.command(("corpse 0 " + local9.id()));
									((Bot) local8).world = map;
									local8.addNotification(
										((Bot) local8).dummycar,
										GameRef.EVENT_COMMAND,
										GameRef.EVENT_SAME,
										null
									);
									break;
								}
							}
							local7++;
						}
					}
				} else
				{
					if ((local4_2 == "car_rem"))
					{
						int local5_3 = arg2.token(1).intValue();
						int local6_3;
						local6_3 = (policeCars.size() - 1);
						while ((local6_3 >= 0))
						{
							if ((policeCars.elementAt(local6_3).id == local5_3))
							{
								TrafficTracker local7_2 = policeCars.removeElementAt(local6_3);
								nav.remMarker(local7_2.m);
								return;
							}
							local6_3--;
						}
						local6_3 = (opponentCars.size() - 1);
						while ((local6_3 >= 0))
						{
							if ((opponentCars.elementAt(local6_3).id == local5_3))
							{
								TrafficTracker local7_3 = opponentCars.removeElementAt(local6_3);
								nav.remMarker(local7_3.m);
								local7_3.car.command("corpse 0 0");
								local7_3.bot.remNotification(local7_3.bot.dummycar, GameRef.EVENT_COMMAND);
								return;
							}
							local6_3--;
						}
					}
				}
			}
		}
	}

	public void handleMessage(Message arg0)
	{
		int local2 = 0;
		if ((arg0.type == Message.MT_EVENT))
		{
			int local3 = arg0.cmd;
			if ((local3 == City.CMD_PARTICIPATE))
			{
				Input.cursor.enable(0);
				backupnrWatching = nrWatching;
				if (nrWatching)
				{
					osd.hideGroup(nrWatchingGroup);
					enableOsd(1);
					nrWatching = 0;
				} else
				{
					osd.hideGroup(nightRaceGroup);
				}
				startNightRace(1);
				local2 = 1;
			} else
			{
				if ((local3 == City.CMD_WATCH_RACE))
				{
					osd.hideGroup(nightRaceGroup);
					osd.showGroup(nrWatchingGroup);
					nrWatching = 1;
					enableOsd(0);
					if (nrBot2)
					{
						backupCamera();
						cameraTarget = nrBot2.car;
						cameraTarget2 = nrBot1.car;
						lookBot(nrBot2, 1);
					} else
					{
						startNightRace(0);
					}
					if ((!(nrPlayerPaused)))
					{
						Vector3 local4 = new Vector3(dirS);
						local4.mul((-(10.0)));
						local4.add(pS);
						Vector3[] local5 = map.alignToRoad(local4);
						local4.y = local5[0].y;
						player.car.setMatrix(local4, oriS);
						player.car.command("stop");
						nrPlayerPaused = 1;
					}
				} else
				{
					if ((local3 == City.CMD_STOP_WATCHING))
					{
						osd.hideGroup(nrWatchingGroup);
						osd.showGroup(nightRaceGroup);
						nrWatching = 0;
						nrShowRaceFinish = 0;
						if ((nrBot1 && nrBot2))
						{
							nrBot1.brain.command("camera 0");
							nrBot2.brain.command("camera 0");
						}
						restoreCamera();
						enableOsd(1);
						if (nrPlayerPaused)
						{
							player.car.command("start");
							nrPlayerPaused = 0;
						}
					}
				}
			}
		}
		if ((!(local2)))
		{
			super.handleMessage(arg0);
		}
	}

}

public class RacerTalkDialog extends Dialog
{
	static final int CMD_RACE = 0;
	static final int CMD_EXIT = 1;
	static final int CMD_EXITNRACE = 2;
	static String[] Text_honkin;
	static String[] Text_letsRace;
	static String[] Text_goAway;
	static String[] Text_noWay;
	static String[] Text_seeYou;
	static String[] Text_right;
	Player player;
	Racer bot;
	int mainGroup;
	int raceGroup1;
	int raceGroup2;
	int raceGroup3;
	int raceGroup4;
	int canChallenge;
	int justRaced;
	int policeStateCopy;

	public void init()
	{
		if ((RacerTalkDialog.Text_honkin == null))
		{
			RacerTalkDialog.Text_honkin = new String[2];
			RacerTalkDialog.Text_honkin[0] = "\"You honkin' at me punk?\"";
			RacerTalkDialog.Text_honkin[1] = "\"What do ya want?\"";
			RacerTalkDialog.Text_letsRace = new String[1];
			RacerTalkDialog.Text_letsRace[0] = "O.K.";
			RacerTalkDialog.Text_goAway = new String[4];
			RacerTalkDialog.Text_goAway[0] = "\"Go away, kiddy!\"";
			RacerTalkDialog.Text_goAway[1] = "\"Don't make me laugh!\"";
			RacerTalkDialog.Text_goAway[2] = "\"Not now.\"";
			RacerTalkDialog.Text_goAway[3] = "\"Forget me.\"";
			RacerTalkDialog.Text_noWay = new String[2];
			RacerTalkDialog.Text_noWay[0] = "\"No way, my ride needs some tuning!\"";
			RacerTalkDialog.Text_noWay[1] = "\"Maybe next time, Pal.\"";
			RacerTalkDialog.Text_seeYou = new String[2];
			RacerTalkDialog.Text_seeYou[0] = "\"See ya next time!\"";
			RacerTalkDialog.Text_seeYou[1] = "\"Bye for now!\"";
			RacerTalkDialog.Text_right = new String[4];
			RacerTalkDialog.Text_right[0] = "\"Right.\"";
			RacerTalkDialog.Text_right[1] = "\"Fine.\"";
			RacerTalkDialog.Text_right[2] = "\"Cool.\"";
			RacerTalkDialog.Text_right[3] = "\"O.K.\"";
		}
	}

	public RacerTalkDialog(Controller arg0, Player arg1, Racer arg2, int arg3, int arg4)
	{
		super(
			arg0,
			(((Dialog.DF_FULLSCREEN | Dialog.DF_DARKEN) | Dialog.DF_MODAL) | Dialog.DF_FREEZE),
			null,
			null
		);
		player = arg1;
		bot = arg2;
		policeStateCopy = arg4;
		this.init();
		justRaced = arg3;
		canChallenge = GameLogic.canChallenge(arg1, arg2);
	}

	public void show()
	{
		float local1 = (-(0.8));
		float local2 = (-(0.5));
		float local3 = 0.0;
		float local4 = 0.1;
		float local5;
		float local6;
		int local7;
		osd.createRectangle((-(0.75)), (-(0.61)), 0.36, 0.66, 1, player.character);
		osd.createRectangle(0.75, (-(0.61)), 0.36, 0.66, 1, bot.character);
		osd.createRectangle(0.0, 0.4, 1.4, 1.0, 1, new ResourceRef(City.RID_DAY_CHALLENGE));
		osd.createRectangle(0.0, (-(0.6)), 1.05, 0.55, 1, City.RRT_FRAME);
		osd.createText(player.name, Frontend.smallFont, Text.ALIGN_CENTER, (-(0.75)), (-(0.35000002)));
		osd.createText(bot.name, Frontend.smallFont, Text.ALIGN_CENTER, 0.75, (-(0.35000002)));
		osd.endGroup();
		Menu local8;
		Style local9 = new Style(0.75, 0.1, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST);
		local5 = local3;
		local6 = local1;
		local7 = (Text_honkin.length * Math.random());
		osd.createText(
			RacerTalkDialog.Text_honkin[local7],
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			local5,
			local6
		);
		local6 += local4;
		local6 += local4;
		local8 = osd.createMenu(local9, local5, local6, 0);
		local8.addItem(" 1. \"Wanna race with you!\"", RacerTalkDialog.CMD_RACE);
		local8.addItem(" 2. \"Nah, never mind.\"", RacerTalkDialog.CMD_EXIT);
		osd.createHotkey(Input.RCDIK_1, Input.KEY, RacerTalkDialog.CMD_RACE, this);
		osd.createHotkey(Input.RCDIK_2, Input.KEY, RacerTalkDialog.CMD_EXIT, this);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, RacerTalkDialog.CMD_EXIT, this);
		mainGroup = osd.endGroup();
		local5 = local3;
		local6 = local1;
		osd.createText(
			"\"So you think you got what it takes huh?",
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			local5,
			local6
		);
		local6 += local4;
		osd.createText(
			" Well, put yer money where your mouth is!\"",
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			local5,
			local6
		);
		local6 += local4;
		local6 += local4;
		local8 = osd.createMenu(local9, local5, local6, 0);
		local7 = (Text_right.length * Math.random());
		local8.addItem((" 1. " + RacerTalkDialog.Text_right[local7]), RacerTalkDialog.CMD_EXITNRACE);
		osd.createHotkey(Input.RCDIK_1, Input.KEY, RacerTalkDialog.CMD_EXITNRACE, this);
		osd.hideGroup((raceGroup1 = osd.endGroup()));
		local5 = local3;
		local6 = local1;
		local7 = (Text_goAway.length * Math.random());
		osd.createText(
			RacerTalkDialog.Text_goAway[local7],
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			local5,
			local6
		);
		local6 += local4;
		local6 += local4;
		local8 = osd.createMenu(local9, local5, local6, 0);
		local7 = (Text_right.length * Math.random());
		local8.addItem((" 1. " + RacerTalkDialog.Text_right[local7]), RacerTalkDialog.CMD_EXIT);
		osd.createHotkey(Input.RCDIK_1, Input.KEY, RacerTalkDialog.CMD_EXIT, this);
		osd.hideGroup((raceGroup2 = osd.endGroup()));
		local5 = local3;
		local6 = local1;
		local7 = (Text_noWay.length * Math.random());
		osd.createText(
			RacerTalkDialog.Text_noWay[local7],
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			local5,
			local6
		);
		local6 += local4;
		local6 += local4;
		local8 = osd.createMenu(local9, local5, local6, 0);
		local7 = (Text_seeYou.length * Math.random());
		local8.addItem((" 1. " + RacerTalkDialog.Text_seeYou[local7]), RacerTalkDialog.CMD_EXIT);
		osd.createHotkey(Input.RCDIK_1, Input.KEY, RacerTalkDialog.CMD_EXIT, this);
		osd.hideGroup((raceGroup3 = osd.endGroup()));
		local5 = local3;
		local6 = local1;
		osd.createText(
			"\"Loose your tail first buddy!\"",
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			local5,
			local6
		);
		local6 += local4;
		local6 += local4;
		local8 = osd.createMenu(local9, local5, local6, 0);
		local7 = (Text_right.length * Math.random());
		local8.addItem((" 1. " + RacerTalkDialog.Text_right[local7]), RacerTalkDialog.CMD_EXIT);
		osd.createHotkey(Input.RCDIK_1, Input.KEY, RacerTalkDialog.CMD_EXIT, this);
		osd.hideGroup((raceGroup4 = osd.endGroup()));
		super.show();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == RacerTalkDialog.CMD_RACE))
		{
			osd.hideGroup(mainGroup);
			if (policeStateCopy)
			{
				osd.showGroup(raceGroup4);
			} else
			{
				if (canChallenge)
				{
					if (justRaced)
					{
						osd.showGroup(raceGroup3);
					} else
					{
						osd.showGroup(raceGroup1);
					}
				} else
				{
					osd.showGroup(raceGroup2);
				}
			}
			osd.changeSelection2((-(1)), 0);
		} else
		{
			if ((arg0 == RacerTalkDialog.CMD_EXIT))
			{
				result = 0;
				notify();
			} else
			{
				if ((arg0 == RacerTalkDialog.CMD_EXITNRACE))
				{
					result = 1;
					notify();
				}
			}
		}
	}

}

public class FineDialog extends CsDialog
{
	public FineDialog(Controller arg0, int arg1, String arg2, int[] arg3)
	{
		super(arg0, arg1, new ResourceRef(City.RID_BUNTESS));
		float local5 = (-(0.04));
		float local6 = (-(0.48000002));
		float local7 = (-(0.1));
		float local8 = 0.0039999997;
		int local9;
		int local10;
		if (((Frontend.smallFont.id() == Text.RID_CONSOLE10) || (Frontend.smallFont.id() == Text.RID_CONSOLE5)))
		{
			local10 = -16777216;
		} else
		{
			local10 = -1;
		}
		if (arg3[1])
		{
			osd.createText(
				"SPEEDING:",
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local6,
				local5,
				local9).changeColor(local10
			);
			osd.createText(
				("$" + arg3[1]),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local7,
				local5,
				local9++).changeColor(local10
			);
			local6 += local8;
			local7 += local8;
		}
		if (arg3[2])
		{
			osd.createText(
				"CRASHES:",
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local6,
				local5,
				local9).changeColor(local10
			);
			osd.createText(
				("$" + arg3[2]),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local7,
				local5,
				local9++).changeColor(local10
			);
			local6 += local8;
			local7 += local8;
		}
		if (arg3[3])
		{
			osd.createText(
				"CHASE:",
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local6,
				local5,
				local9).changeColor(local10
			);
			osd.createText(
				("$" + arg3[3]),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local7,
				local5,
				local9++).changeColor(local10
			);
			local6 += local8;
			local7 += local8;
		}
		if (arg3[4])
		{
			osd.createText(
				"ILLEGAL PARTS:",
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local6,
				local5,
				local9).changeColor(local10
			);
			osd.createText(
				("$" + arg3[4]),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local7,
				local5,
				local9++).changeColor(local10
			);
			local6 += local8;
			local7 += local8;
		}
		local9++;
		local6 += local8;
		local7 += local8;
		osd.createText(
			"TOTAL:",
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local6,
			local5,
			local9).changeColor(local10
		);
		osd.createText(
			("$" + arg3[0]),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local7,
			local5,
			local9).changeColor(local10
		);
		new SfxRef((sound:0x26r + (Math.random() * 6))).play();
	}

}

public class NrOpponentDialog extends Dialog
{
	int nItems;

	public NrOpponentDialog(Controller arg0, int arg1, ResourceRef[] arg2, String[] arg3, int[] arg4, int arg5)
	{
		super(arg0, (((arg1 | Dialog.DF_FULLSCREEN) | Dialog.DF_DARKEN) | Dialog.DF_FREEZE), null, null);
		osd.createBG(Osd.RRT_DARKEN);
		osd.createTextBox(
			" \n The following racers are willing to race with you. \n Please select your opponent! \n ",
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			(-(0.25)),
			(-(0.95000005)),
			1.14,
			City.RRT_FRAME
		);
		nItems = arg3.length;
		float local7 = ((-(0.92)) + ((3 - nItems) * 0.3));
		Style local8;
		Style local9;
		if ((1 || Config.majomParade))
		{
			local8 = new Style(1.4, 0.15, Frontend.smallFont, Text.ALIGN_LEFT, City.RRT_FRAME);
			local9 = new Style(0.45, 0.120000005, Frontend.smallFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		} else
		{
			local8 = new Style(1.4, 0.15, Frontend.mediumFont, Text.ALIGN_LEFT, City.RRT_FRAME);
			local9 = new Style(0.45, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		}
		Menu local10;
		local10 = osd.createMenu(local8, (-(0.6)), (local7 + 0.48000002), 0.6);
		int local11 = 0;
		while ((local11 < nItems))
		{
			osd.createRectangle(((-(0.96000004)) + 0.15), (local7 + 0.29), 0.3, 0.58, 1, arg2[local11]);
			Gadget local12 = local10.addItem(arg3[local11], local11);
			if (((arg4[local11] > 0) && (arg4[local11] > arg5)))
			{
				local12.disable();
			}
			local7 += 0.6;
			local11++;
		}
		local10 = osd.createMenu(local9, 1.0, 0.8, 0);
		local10.addItem("Cancel", nItems);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, nItems, this);
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 <= nItems))
		{
			result = arg0;
			notify();
		}
	}

}

public class TrafficTracker
{
	int id;
	GameRef car;
	int trafficId;
	Marker m;
	Bot bot;

}

public class PoliceScout
{
	Bot bot;
	int returningTraffic;
	float distance;
	TrafficTracker tracker;

}

public class ParkingCar
{
	GameRef gr;
	Vector3 origPos;
	Ypr origOri;

	public ParkingCar(GameRef arg0, GameRef arg1, Vector3 arg2, Ypr arg3, int arg4)
	{
		super();
		origPos = new Vector3(arg2);
		origOri = new Ypr(arg3);
		gr = new GameRef(arg0, arg1, ((((origPos.toString() + ",") + origOri.y) + ",0,0,") + arg4), "_.*0*._");
		gr.setMatrix(origPos, origOri);
	}

	public void finalize()
	{
		if (gr)
		{
			gr.destroy();
		}
	}

	public void reset(Vector3 arg0)
	{
		Vector3 local2 = new Vector3(arg0);
		local2.sub(origPos);
		if ((local2.length() > 10))
		{
			gr.setMatrix(origPos, origOri);
		}
	}

}

public class NightracesData
{
	final Vector3[] startPos = new Vector3[18];
	final Ypr[] startOri = new Ypr[18];
	final Vector3[] finishPos = new Vector3[18];
	final Ypr[] finishOri = new Ypr[18];

	public NightracesData()
	{
		super();
		startPos[0] = new Vector3((-(296.60602)), 7.6029997, 1356.3041);
		startOri[0] = new Ypr((-(1.6389999)), 0.0, 0.0);
		startPos[1] = new Vector3((-(360.481)), 12.457, 663.52704);
		startOri[1] = new Ypr((-(2.999)), 0.0, 0.0);
		startPos[2] = new Vector3(149.582, 13.039, 1066.371);
		startOri[2] = new Ypr(0.21100001, 0.0, 0.0);
		startPos[3] = new Vector3((-(523.833)), 10.102, 1447.879);
		startOri[3] = new Ypr(1.253, 0.0, 0.0);
		startPos[4] = new Vector3((-(827.339)), 7.823, 940.699);
		startOri[4] = new Ypr((-(1.8679999)), 0.0, 0.0);
		startPos[5] = new Vector3(595.82404, 0.784, 1297.508);
		startOri[5] = new Ypr(0.48000002, 0.0, 0.0);
		startPos[6] = new Vector3(226.576, 9.498, 464.79202);
		startOri[6] = new Ypr((-(1.909)), 0.0, 0.0);
		startPos[7] = new Vector3(328.415, 2.3609998, (-(99.669)));
		startOri[7] = new Ypr(1.8429999, 0.0, 0.0);
		startPos[8] = new Vector3(1039.0969, 14.433999, 385.346);
		startOri[8] = new Ypr((-(3.036)), 0.0, 0.0);
		startPos[9] = new Vector3(1323.8041, 10.885, 184.227);
		startOri[9] = new Ypr(3.1399999, 0.0, 0.0);
		startPos[10] = new Vector3(1324.637, 10.885, 133.709);
		startOri[10] = new Ypr(0.037, 0.0, 0.0);
		startPos[11] = new Vector3(1025.139, 3.226, (-(506.96)));
		startOri[11] = new Ypr(2.1239998, 0.0, 0.0);
		startPos[12] = new Vector3((-(597.025)), 6.195, (-(79.34701)));
		startOri[12] = new Ypr(0.605, 0.0, 0.0);
		startPos[13] = new Vector3((-(323.593)), 9.188001, (-(310.091)));
		startOri[13] = new Ypr((-(1.303)), 0.0, 0.0);
		startPos[14] = new Vector3((-(309.92798)), 2.396, 295.465);
		startOri[14] = new Ypr((-(1.546)), 0.0, 0.0);
		startPos[15] = new Vector3((-(633.293)), 9.092, 394.97);
		startOri[15] = new Ypr(2.483, 0.0, 0.0);
		startPos[16] = new Vector3(109.644, 11.9869995, (-(263.815)));
		startOri[16] = new Ypr(1.692, 0.0, 0.0);
		startPos[17] = new Vector3(350.465, 4.6429997, (-(160.232)));
		startOri[17] = new Ypr(0.479, 0.0, 0.0);
		finishPos[0] = new Vector3(102.489006, 6.309, 1383.4241);
		finishOri[0] = new Ypr((-(1.6389999)), 0.0, 0.0);
		finishPos[1] = new Vector3((-(303.727)), 11.174, 1059.489);
		finishOri[1] = new Ypr((-(2.999)), 0.0, 0.0);
		finishPos[2] = new Vector3(65.93902, 14.232596, 675.21576);
		finishOri[2] = new Ypr(0.21100001, 0.0, 0.0);
		finishPos[3] = new Vector3((-(904.633)), 9.929, 1322.552);
		finishOri[3] = new Ypr(1.253, 0.0, 0.0);
		finishPos[4] = new Vector3((-(444.301)), 8.676, 1057.92);
		finishOri[4] = new Ypr((-(1.8679999)), 0.0, 0.0);
		finishPos[5] = new Vector3(411.031, 3.4350002, 942.638);
		finishOri[5] = new Ypr(0.48000002, 0.0, 0.0);
		finishPos[6] = new Vector3(604.144, 5.633, 597.41205);
		finishOri[6] = new Ypr((-(1.909)), 0.0, 0.0);
		finishPos[7] = new Vector3((-(57.186)), 0.0, 8.065001);
		finishOri[7] = new Ypr(1.8429999, 0.0, 0.0);
		finishPos[8] = new Vector3(1081.2859, 8.047, 783.1);
		finishOri[8] = new Ypr((-(3.036)), 0.0, 0.0);
		finishPos[9] = new Vector3(1323.13, 11.527, 584.675);
		finishOri[9] = new Ypr(3.1399999, 0.0, 0.0);
		finishPos[10] = new Vector3(1309.875, 12.7699995, (-(266.077)));
		finishOri[10] = new Ypr(0.037, 0.0, 0.0);
		finishPos[11] = new Vector3(684.28503, 6.6419997, (-(296.689)));
		finishOri[11] = new Ypr(2.1239998, 0.0, 0.0);
		finishPos[12] = new Vector3((-(824.92804)), 11.271999, (-(408.966)));
		finishOri[12] = new Ypr(0.605, 0.0, 0.0);
		finishPos[13] = new Vector3(61.955, 15.959, (-(415.879)));
		finishOri[13] = new Ypr((-(1.303)), 0.0, 0.0);
		finishPos[14] = new Vector3(90.156, 2.6409998, 285.623);
		finishOri[14] = new Ypr((-(1.546)), 0.0, 0.0);
		finishPos[15] = new Vector3((-(878.33)), 12.976999, 711.55);
		finishOri[15] = new Ypr(2.483, 0.0, 0.0);
		finishPos[16] = new Vector3((-(288.414)), 11.408999, (-(215.491)));
		finishOri[16] = new Ypr(1.692, 0.0, 0.0);
		finishPos[17] = new Vector3(165.907, 17.337, (-(515.365)));
		finishOri[17] = new Ypr(0.479, 0.0, 0.0);
	}

}
