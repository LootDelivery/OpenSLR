package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class ROCTrack extends Track
{
	static final int RID_STARTBG = frontend:0x116r;
	static final int RID_FIN_WIN = frontend:0x13er;
	static final int RID_FIN_LOSE = frontend:0x13dr;
	static final int RID_SPEECH3 = sound:0x1dr;
	static final int RID_SPEECH2 = sound:0x1cr;
	static final int RID_SPEECH1 = sound:0x1br;
	static final int RID_SPEECHGO = sound:0x1er;
	static final int RID_SPEECHYOUWIN = sound:0x1fr;
	static final int RID_APPLAUSE1 = sound:0x21r;
	static final int RID_APPLAUSE2 = sound:0x22r;
	Text messageText0;
	Text messageText1;
	Bot raceBot;
	Marker mRaceBot;
	int testMode;
	int testStatus;
	int testCountDown;
	float timerStarted = 0.0;
	int displayTimer;
	int applauseSfxID;
	int applauseSfxOn;
	int finishSfxID;
	int finishSfxOn;
	int triggerIndex0;
	int triggerIndex1;
	int plRank;
	int finished;
	float startTime;
	float botTime;
	float playerTime;
	Trigger[] fence;
	int fence_repeater_semaphore;
	Round actRound;
	Vector3 startPos;
	Ypr startOri;
	Vector3 finishPos;
	Trigger finishSoundTrigger;
	RenderRef[] rocMen = new RenderRef[40];
	Animation[] rocMenAnim = new Animation[40];
	int nRocMen;
	Roc roc;

	public ROCTrack()
	{
		super();
		map = new GroundRef(maps.ROC_track:0x1r);
		nav = new Navigator(
			(-(17.1137)),
			(-(26.6)),
			4.9404,
			maps.ROC_Track.smallmap:0x1r,
			maps.ROC_Track.smallmap:0x2r,
			maps.ROC_Track.smallmap:0x5r,
			8,
			8,
			8
		);
		fence = new Trigger[4];
		fence[0] = new Trigger(map, null, new Vector3(520, 0, (-(1371))), 2000, 300, 1, "fence 0");
		fence[1] = new Trigger(map, null, new Vector3(108, 0, 504), 2000, 300, 1, "fence 1");
		fence[2] = new Trigger(map, null, new Vector3((-(366)), 0, (-(436))), 1, 300, 2000, "fence 2");
		fence[3] = new Trigger(map, null, new Vector3(817, 0, (-(436))), 1, 300, 2000, "fence 3");
		addNotification(
			fence[0].trigger,
			GameRef.EVENT_TRIGGER_ON,
			GameRef.EVENT_SAME,
			null,
			"event_handlerRocFence"
		);
		addNotification(
			fence[1].trigger,
			GameRef.EVENT_TRIGGER_ON,
			GameRef.EVENT_SAME,
			null,
			"event_handlerRocFence"
		);
		addNotification(
			fence[2].trigger,
			GameRef.EVENT_TRIGGER_ON,
			GameRef.EVENT_SAME,
			null,
			"event_handlerRocFence"
		);
		addNotification(
			fence[3].trigger,
			GameRef.EVENT_TRIGGER_ON,
			GameRef.EVENT_SAME,
			null,
			"event_handlerRocFence"
		);
	}

	public void placeMen(Vector3 arg0, float arg1)
	{
		int local3;
		Vector3 local4;
		int[] local5 = new int[6];
		local5[0] = humans:0x2fr;
		local5[1] = humans:0x30r;
		local5[2] = humans:0x31r;
		local5[3] = humans:0x32r;
		local5[4] = humans:0x33r;
		local5[5] = humans:0x34r;
		int[] local6 = new int[6];
		local6[0] = humans:0x14r;
		local6[1] = humans:0x1br;
		local6[2] = humans:0x7dr;
		local6[3] = humans:0x7er;
		local6[4] = humans:0x17r;
		local6[5] = humans:0x7fr;
		Vector3 local7 = new Vector3(1.0, 0, 0);
		local7.rotate(new Ypr((arg1 + 1.57), 0.0, 0.0));
		Vector3 local8 = new Vector3(7.2, 0, 0);
		local8.rotate(new Ypr(arg1, 0.0, 0.0));
		Ypr local9 = new Ypr((arg1 + 1.57), 0, 0);
		Vector3 local10 = new Vector3(arg0);
		int local11 = 0;
		while ((local11 < 10))
		{
			local4 = new Vector3((Math.random() - 0.5), 0, 0);
			local4.rotate(new Ypr(arg1, 0.0, 0.0));
			local10.add(local4);
			local10.sub(local8);
			local3 = (Math.random() * local5.length);
			rocMen[nRocMen] = new RenderRef(map, local5[local3], "tesztmen");
			rocMenAnim[nRocMen] = new Animation(rocMen[nRocMen], new ResourceRef(local6[local3]));
			rocMenAnim[nRocMen].setSpeed((0.5 + Math.random()));
			rocMenAnim[nRocMen].loopPlay();
			rocMen[nRocMen].setMatrix(local10, local9);
			nRocMen++;
			local10.add(local8);
			local10.sub(local4);
			local9.y -= 3.1399999;
			local4 = new Vector3((Math.random() - 0.5), 0, 0);
			local4.rotate(new Ypr(arg1, 0.0, 0.0));
			local10.add(local4);
			local10.add(local8);
			local3 = (Math.random() * local5.length);
			rocMen[nRocMen] = new RenderRef(map, local5[local3], "tesztmen");
			rocMenAnim[nRocMen] = new Animation(rocMen[nRocMen], new ResourceRef(local6[local3]));
			rocMenAnim[nRocMen].setSpeed((0.5 + Math.random()));
			rocMenAnim[nRocMen].loopPlay();
			rocMen[nRocMen].setMatrix(local10, local9);
			nRocMen++;
			local10.sub(local8);
			local10.sub(local4);
			local9.y += 3.1399999;
			local4 = new Vector3(local7);
			local4.mul(((Math.random() * 1.0) + 0.8));
			local10.add(local4);
			local11++;
		}
	}

	public void enter(GameState arg0)
	{
		Frontend.loadingScreen.show();
		GfxEngine.flush();
		roc = ((Garage) arg0).roc;
		actRound = new Round(this, null, null);
		actRound.startdir((-(0.55)));
		actRound.point(5.4, 0.0, (-(6)), 0);
		actRound.point((-(25)), 0.8, (-(57)), 0, 8.0);
		startPos = new Vector3(actRound.getPoint(0));
		startOri = actRound.startOri;
		finishPos = new Vector3(actRound.getPoint((actRound.size() - 1)));
		finished = 0;
		if ((!(testMode)))
		{
			Vector3 local2 = new Vector3((-(1.75)), 0, 0);
			local2.rotate(new Ypr((-(0.55)), 0.0, 0.0));
			local2.add(startPos);
			placeMen(local2, (-(0.55)));
			placeMen(finishPos, 0.0);
			raceBot = roc.getNextOpponent();
			raceBot.createCar(map, roc.getCurrentCarName());
			raceBot.brain.queueEvent(null, GameRef.EVENT_COMMAND, roc.getCurrentAIParam());
			Vector3 local3 = new Vector3(startOri);
			local3.mul(3.5);
			local3.rotate(new Ypr(1.57, 0.0, 0.0));
			local3.add(startPos);
			raceBot.car.queueEvent(null, GameRef.EVENT_COMMAND, "reset");
			raceBot.car.queueEvent(null, GameRef.EVENT_COMMAND, "reload");
			raceBot.car.setMatrix(local3, startOri);
			raceBot.car.setParent(map);
			raceBot.car.wakeUp();
			raceBot.car.queueEvent(null, GameRef.EVENT_COMMAND, "stop");
			raceBot.car.queueEvent(null, GameRef.EVENT_COMMAND, "idle");
			testStatus = (-(2));
		}
		super.enter(arg0);
		messageText0 = osd.createText(null, Frontend.largeFont, Text.ALIGN_RIGHT, 0.0, (-(0.7)));
		messageText1 = osd.createText(null, Frontend.largeFont, Text.ALIGN_LEFT, 0.0, (-(0.7)));
		player.car.setMatrix(actRound.getPoint(0), actRound.startOri);
		player.car.wakeUp();
		Vector3 local2_2 = new Vector3(actRound.startOri);
		local2_2.mul((-(8.0)));
		local2_2.y += 5;
		local2_2.add(player.car.getPos());
		moveCamera(local2_2, actRound.startOri);
		player.car.queueEvent(null, GameType.EVENT_COMMAND, "stop");
		player.car.command("idle");
		if ((!(testMode)))
		{
			applauseSfxID = new SfxRef(
				ROCTrack.RID_APPLAUSE1).play(startPos,
				0.0,
				1.0,
				1.0,
				(SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP)
			);
			applauseSfxOn = 1;
			ResourceRef local3_2 = raceBot.character;
			ResourceRef local4 = player.character;
			String local5 = raceBot.name;
			String local6 = player.name;
			changeCamTarget2(raceBot.car);
			mRaceBot = nav.addMarker(raceBot);
			raceBot.car.command("idle");
			Frontend.loadingScreen.display(
				new RaceDialog(player.controller, Dialog.DF_MODAL, new ResourceRef(ROCTrack.RID_STARTBG), local4, local3_2, local6, local5, 0, 0, ((roc.wonRuns + roc.lostRuns) + 1), 2),
				10.0
			);
			cam.command("angle 0 5.0 0.4853");
			testCountDown = 8;
		}
		testStatus = (-(1));
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
	}

	public void exit(GameState arg0)
	{
		if ((!((arg0 instanceof RaceSetup))))
		{
			removeAllTimers();
		}
		actRound.destroy();
		actRound = null;
		super.exit(arg0);
	}

	public void resetGameMode()
	{
		if (messageText0)
		{
			messageText0.changeText(null);
			messageText1.changeText(null);
		}
		if (raceBot)
		{
			raceBot.deleteCar();
			nav.remMarker(mRaceBot);
			mRaceBot = null;
		}
		stopTimer();
		testStatus++;
	}

	public void event_handlerRocFence(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if (((!(fence_repeater_semaphore)) && (local4 == player.car.id())))
		{
			fence_repeater_semaphore = 1;
			player.car.command("brake");
			if (testMode)
			{
				new WarningDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"WARNING",
					"You are not allowed to leave the race area! \n Now going back to the tent...").display(
				);
				osdCommand(Track.CMD_EXIT);
			} else
			{
				new WarningDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"WARNING",
					"You are not allowed to leave the race area! \n You lost this run...").display(
				);
				giveUpRace();
			}
		}
	}

	public void event_handlerFinishSound(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((arg1 == GameRef.EVENT_TRIGGER_ON))
		{
			if (((!(testMode)) && (local4 == player.car.id())))
			{
				if ((triggerIndex0 >= (actRound.size() - 1)))
				{
					finishSfxID = new SfxRef(
						ROCTrack.RID_APPLAUSE2).play(finishPos,
						0.0,
						1.0,
						1.0,
						(SfxRef.SFX_LOOP | SfxRef.SFX_NOAUTOSTOP)
					);
					finishSfxOn = 1;
				}
			}
		}
	}

	public void event_handlerTrigger(GameRef arg0, int arg1, String arg2)
	{
		if ((testStatus == 1))
		{
			int local4 = arg2.token(0).intValue();
			if ((arg1 == GameRef.EVENT_TRIGGER_ON))
			{
				int local5 = arg0.id();
				if (raceBot)
				{
					if ((local4 == raceBot.car.id()))
					{
						if ((!((finished & 2))))
						{
							if ((actRound.trigger.lastElement().trigger.id() == local5))
							{
								botTime = (System.simTime() - startTime);
								finished |= 2;
								if ((!(plRank)))
								{
									plRank = 2;
								}
								raceBot.stop();
							}
						}
					}
				}
				if ((local4 == player.car.id()))
				{
					if ((!((finished & 1))))
					{
						if ((!(activeTrigger)))
						{
							if ((actRound.trigger.elementAt(triggerIndex0).trigger.id() == local5))
							{
								activeTrigger = local5;
								triggerIndex0++;
								if ((triggerIndex0 == actRound.size()))
								{
									playerTime = (System.simTime() - startTime);
									finished |= 1;
									if ((!(plRank)))
									{
										plRank = 1;
									}
									player.car.command("brake");
									pauseTimer();
									if (testMode)
									{
										setMessage("Well done!");
										Thread.sleep(6000);
										resetGameMode();
										GameLogic.changeActiveSection(parentState);
									} else
									{
										if ((plRank == 1))
										{
											setMessage("Well done!");
											new SfxRef(ROCTrack.RID_SPEECHYOUWIN).play();
										} else
										{
											setMessage("Too bad loser.");
										}
									}
								} else
								{
									setMessage((((("Checkpoint " + (triggerIndex0 - 1)) + " of ") + (actRound.size() - 2)) + " passed!"));
								}
							}
						}
					}
				}
				if (((!(testMode)) && ((finished == 1) || (finished == 2))))
				{
					finished |= 4;
					Thread.sleep(3000);
					player.car.command("brake");
					raceBot.stop();
					cam.command("angle 0 5.0 0.4853");
					Thread.sleep(8000);
					finishRocRace();
				}
			} else
			{
				if ((local4 == player.car.id()))
				{
					activeTrigger = 0;
				}
			}
		}
	}

	public void osdCommand(int arg0)
	{
		if (((arg0 == Track.CMD_INGAMEMENU) && finished))
		{
			return;
		} else
		{
			super.osdCommand(arg0);
		}
	}

	public void giveUpRace()
	{
		plRank = 2;
		finishRocRace();
	}

	public void finishRocRace()
	{
		resetGameMode();
		ResourceRef local1 = raceBot.character;
		ResourceRef local2 = player.character;
		String local3 = raceBot.name;
		String local4 = player.name;
		cam.command("angle 0.0");
		if ((plRank == 1))
		{
			roc.wonRuns++;
			int local5 = (((roc.numRounds() - roc.getCurrentRound()) + (10 * roc.lostRuns)) + (100 * roc.wonRuns));
			RaceDialog local6 = new RaceDialog(
				player.controller,
				Dialog.DF_MODAL,
				new ResourceRef(ROCTrack.RID_FIN_WIN),
				local2,
				local1,
				local4,
				local3,
				playerTime,
				botTime,
				local5,
				3
			);
			if ((roc.isFinalRound() && roc.isRoundClosed()))
			{
				changeCamNone();
				Frontend.loadingScreen.show(local6);
				Thread.sleep(1000);
				GameLogic.spendTime((((8 * 3600) + (24 * 3600)) - GameLogic.getTime()));
				player.money -= GameLogic.ROC_ENTRYFEE;
				((Garage) parentState).roc = null;
				player.car.races_won += 1000;
				player.prestige = 1.0;
				releaseCar();
				player.carlot.lockPlayerCar();
				player.carlot.saveCar(player.carlot.curcar);
				player.carlot.flushCars();
				Vector3 local7 = new Vector3((-(25.764)), 0.528, (-(63.022)));
				Ypr local8 = new Ypr((-(1.5689999)), 0.0, 0.0);
				Vector3 local9 = new Vector3((-(25.48)), 0.6, (-(62.949)));
				Ypr local10 = new Ypr(1.0, 0.0, 0.0);
				osd.createRectangle(0.0, 0.0, 2.0, 2.0, (-(1)), new ResourceRef(frontend:0x90r));
				player.car = Vehicle.load("DestructionTools/Scripts/Muscle", player);
				player.car.setParent(map);
				player.car.setMatrix(local9, local10);
				player.car.wakeUp();
				System.timeWarp(1.0);
				enableOsd(0);
				changeCamTarget(player.car);
				changeCamFollow();
				cam.command("simulate 1");
				cam.command((((("move " + map.id()) + " ") + local9.toString()) + " 15.0"));
				cam.command((((("look " + map.id()) + " ") + local9.toString()) + " 0,0,0"));
				cam.command("zoom 15 0.5");
				cam.command("angle 0 5.0 0.4853");
				Frontend.loadingScreen.display(local6, 1.0);
				Thread.sleep(16000);
				player.car.setParent(player);
			} else
			{
				roc.closeRun();
				Frontend.loadingScreen.display(local6, 1.0);
			}
		} else
		{
			roc.lostRuns++;
			int local5_2 = (-((((roc.numRounds() - roc.getCurrentRound()) + (10 * roc.lostRuns)) + (100 * roc.wonRuns))));
			Frontend.loadingScreen.display(
				new RaceDialog(player.controller, Dialog.DF_MODAL, new ResourceRef(ROCTrack.RID_FIN_LOSE), local1, local2, local3, local4, botTime, playerTime, local5_2, 3),
				10.0
			);
			if (roc.isRoundClosed())
			{
				GameLogic.spendTime((((8 * 3600) + (24 * 3600)) - GameLogic.getTime()));
				player.money -= GameLogic.ROC_ENTRYFEE;
				((Garage) parentState).roc = null;
			} else
			{
				roc.closeRun();
			}
		}
		if (finishSfxOn)
		{
			new SfxRef(ROCTrack.RID_APPLAUSE2).stop(finishSfxID);
			finishSfxOn = 0;
		}
		if (finishSoundTrigger)
		{
			removeTrigger(finishSoundTrigger);
		}
		finishSoundTrigger = null;
		GameLogic.changeActiveSection(parentState);
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		super.handleEvent(arg0, arg1, arg2);
		if ((arg1 == GameRef.EVENT_TIME))
		{
			if ((arg2 == 2))
			{
				if ((testStatus == 0))
				{
					if (testCountDown)
					{
						if ((testCountDown == 3))
						{
							if ((!(testMode)))
							{
								raceBot.brain.command("AI_BeginRace 0.5");
							}
							new SfxRef(ROCTrack.RID_SPEECH3).play();
						} else
						{
							if ((testCountDown == 2))
							{
								new SfxRef(ROCTrack.RID_SPEECH2).play();
								cam.command("angle 0 3.0");
							} else
							{
								if ((testCountDown == 1))
								{
									new SfxRef(ROCTrack.RID_SPEECH1).play();
									if ((!(testMode)))
									{
										raceBot.brain.command("AI_BeginRace 1.0");
									}
								}
							}
						}
						setMessage(testCountDown--);
					} else
					{
						cam.command("angle 0 0");
						testStatus++;
						new SfxRef(ROCTrack.RID_SPEECHGO).play();
						setMessage("GO!");
						if ((!(testMode)))
						{
							addTimer(5, 99);
							finishSoundTrigger = addTrigger(
								finishPos,
								null,
								Marker.RR_FINISH,
								"event_handlerFinishSound",
								10,
								"finish sound trigger"
							);
						}
						botTime = 0;
						playerTime = 0;
						startTime = System.simTime();
						startTimer();
						activeTrigger = actRound.trigger.elementAt(0).trigger.id();
						triggerIndex0 = 1;
						player.car.queueEvent(null, GameType.EVENT_COMMAND, "start");
						if (raceBot)
						{
							if (raceBot.car)
							{
								raceBot.car.queueEvent(null, GameType.EVENT_COMMAND, "start");
							}
						}
						if (raceBot)
						{
							raceBot.followSplineTrack(0.5, roc.getCurrentSplineName(), player.car.id());
						}
						plRank = 0;
						finished = 0;
					}
				} else
				{
					if ((testStatus == (-(1))))
					{
						if ((--testCountDown < 0))
						{
							testStatus++;
							testCountDown = 3;
						}
					}
				}
			} else
			{
				if ((arg2 == 99))
				{
					if (applauseSfxOn)
					{
						new SfxRef(ROCTrack.RID_APPLAUSE1).stop(applauseSfxID);
						applauseSfxOn = 0;
					}
				}
			}
		}
	}

	public void animate()
	{
		super.animate();
		if (displayTimer)
		{
			float local1 = (System.simTime() - timerStarted);
			messageText1.changeText((String.timeToString(local1, String.TCF_NOHOURS) + " s"));
		}
	}

	public void moveCamera(Vector3 arg0, Ypr arg1)
	{
		Vector3 local3 = cam.getPos();
		local3.sub(arg0);
		if ((local3.length() > 10))
		{
			cam.setMatrix(arg0, arg1);
		}
	}

	public void startTimer()
	{
		timerStarted = System.simTime();
		messageText0.changeText("Time: ");
		displayTimer = 1;
	}

	public void stopTimer()
	{
		displayTimer = 0;
		if (messageText0)
		{
			messageText0.changeText(null);
			messageText1.changeText(null);
		}
	}

	public void pauseTimer()
	{
		displayTimer = 0;
	}

}
