package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class RaceSetup extends GameType implements GameState
{
	static final ResourceRef RR_FRAME = new ResourceRef(frontend:0x16r);
	static final int[] prizeMax;
	static final int[] prizeStep;
	static final int CMD_RACE = 0;
	static final int CMD_ABANDON = 1;
	static final int CMD_ZOOM_IN = 2;
	static final int CMD_ZOOM_OUT = 3;
	static final int CMD_LESS_MONEY = 4;
	static final int CMD_MORE_MONEY = 5;
	static final int CMD_PRIZE = 6;
	String[] prizeValues;
	int prizeMultiplier;
	int forMoney;
	Text prizeText;
	float timewarp;
	Osd osd;
	Thread scrollerThread;
	GameRef cursor;
	GameRef localroot;
	GameRef click;
	Vector3 pStart;
	Vector3 pFinish;
	Marker mStart;
	Marker mFinish;
	Racer opponent;
	GameState lastState;
	City track;

	public RaceSetup()
	{
		super();
		createNativeInstance();
		RaceSetup.prizeMax = new int[GameLogic.CLUBS];
		RaceSetup.prizeStep = new int[GameLogic.CLUBS];
		RaceSetup.prizeMax[0] = 1000;
		RaceSetup.prizeStep[0] = 100;
		RaceSetup.prizeMax[1] = 2000;
		RaceSetup.prizeStep[1] = 250;
		RaceSetup.prizeMax[2] = 4000;
		RaceSetup.prizeStep[2] = 500;
	}

	public void run()
	{
		while (true)
		{
			float local1 = 0.1;
			float local2 = (-(0.90000004));
			float local3 = 0.90000004;
			float local4 = 10.0;
			Vector3 local5 = Input.cursor.getPos();
			if ((local5.x <= local2))
			{
				track.nav.offsetX += ((local4 * (local5.x - local2)) / local1);
				track.nav.updateNavigator(track.player.car);
			} else
			{
				if ((local5.x >= local3))
				{
					track.nav.offsetX += ((local4 * (local5.x - local3)) / local1);
					track.nav.updateNavigator(track.player.car);
				}
			}
			if ((local5.y <= local2))
			{
				track.nav.offsetZ += ((local4 * (local5.y - local2)) / local1);
				track.nav.updateNavigator(track.player.car);
			} else
			{
				if ((local5.y >= local3))
				{
					track.nav.offsetZ += ((local4 * (local5.y - local3)) / local1);
					track.nav.updateNavigator(track.player.car);
				}
			}
			scrollerThread.sleep(20);
		}
	}

	public void enter(GameState arg0)
	{
		lastState = arg0;
		track = lastState;
		if (Frontend.loadingScreen.loadingDialog)
		{
			timewarp = Frontend.loadingScreen.loadingDialog.timewarp;
			Frontend.loadingScreen.loadingDialog.timewarp = 0.0;
			System.timeWarp(0.0);
		} else
		{
			timewarp = System.timeWarp(0.0);
		}
		int local2 = (RaceSetup.prizeMax[GameLogic.player.club] / RaceSetup.prizeStep[GameLogic.player.club]);
		prizeValues = new String[local2];
		int local3 = 0;
		while ((local3 < local2))
		{
			if ((local3 == 0))
			{
				prizeValues[local3] = "Prestige only";
			} else
			{
				prizeValues[local3] = ("$" + (local3 * RaceSetup.prizeStep[GameLogic.player.club]));
			}
			local3++;
		}
		if ((((forMoney > GameLogic.player.money) || (forMoney > RaceSetup.prizeMax[GameLogic.player.club])) || (GameLogic.gameMode == GameLogic.GM_QUICKRACE)))
		{
			forMoney = 0;
		}
		if (forMoney)
		{
			prizeMultiplier = (forMoney / RaceSetup.prizeStep[GameLogic.player.club]);
		} else
		{
			prizeMultiplier = 0;
		}
		if ((track.challenger == GameLogic.player))
		{
			opponent = track.challenged;
		} else
		{
			opponent = track.challenger;
		}
		pStart = track.map.getNearestCross(track.player.car.getPos());
		do
		{
			pFinish = track.map.getNearestCross(pStart, (500 + (Math.random() * 300)));
		} while ((!(track.map.getStartDirection(pStart, pFinish))));
		float local3_2 = track.map.getRouteLength(pStart, pFinish);
		RenderRef local4 = new RenderRef(particles:0x17r);
		local4.cache();
		RenderRef local5 = track.nav.route;
		if (local5)
		{
			local5.destroy();
		}
		local5 = new RenderRef();
		local5.plotRoute(track.nav.localroot, local4, -65536, 10.0, new Vector3(0.01, 0, 0.01));
		track.nav.route = local5;
		mStart = track.nav.addMarker(Marker.RR_START, pStart, 3);
		mFinish = track.nav.addMarker(Marker.RR_FINISH, pFinish, 3);
		osd = new Osd(1.0, 0.0, 15);
		osd.globalHandler = this;
		osd.defSelection = 1;
		createOSDObjects();
		updatePrize();
		osd.show();
		Input.cursor.enable(1);
		if ((GameLogic.player.prestige >= opponent.prestige))
		{
			addNotification(click, GameType.EVENT_CURSOR, GameType.EVENT_SAME, null, "event_handlerClick");
		}
		GameLogic.player.hideOsd();
		setEventMask(GameRef.EVENT_CURSOR);
		scrollerThread = new Thread(this, "Racesetup map scroller");
		scrollerThread.start();
		if (Frontend.loadingScreen.loadingDialog)
		{
			Frontend.loadingScreen.hide();
			timewarp = 1.0;
		}
	}

	public void exit(GameState arg0)
	{
		System.timeWarp(timewarp);
		clearEventMask(GameRef.EVENT_ANY);
		scrollerThread.stop();
		Input.cursor.enable(0);
		osd.hide();
		deleteOSDObjects();
		track.nav.remMarker(mStart);
		track.nav.remMarker(mFinish);
		GameLogic.player.showOsd();
		lastState = null;
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == RaceSetup.CMD_RACE))
		{
			track.startRace(pStart, pFinish, forMoney);
			GameLogic.changeActiveSection(track);
		} else
		{
			if ((arg0 == RaceSetup.CMD_ABANDON))
			{
				if ((GameLogic.gameMode == GameLogic.GM_QUICKRACE))
				{
					track.raceState = 0;
				} else
				{
					track.abandonRace();
				}
				GameLogic.changeActiveSection(track);
			} else
			{
				if ((arg0 == RaceSetup.CMD_ZOOM_IN))
				{
					if ((track.nav.zoom > 10))
					{
						track.nav.changeZoom((track.nav.zoom - 2));
						track.nav.updateNavigator(track.player.car);
					}
				} else
				{
					if ((arg0 == RaceSetup.CMD_ZOOM_OUT))
					{
						if ((track.nav.zoom < 30))
						{
							track.nav.changeZoom((track.nav.zoom + 2));
							track.nav.updateNavigator(track.player.car);
						}
					} else
					{
						if ((arg0 == RaceSetup.CMD_PRIZE))
						{
							prizeMultiplier++;
							updatePrize();
						}
					}
				}
			}
		}
	}

	public void updatePrize()
	{
		if ((prizeMultiplier >= prizeValues.length))
		{
			prizeMultiplier = 0;
		}
		forMoney = (prizeMultiplier * RaceSetup.prizeStep[GameLogic.player.club]);
		if ((forMoney > GameLogic.player.money))
		{
			forMoney = 0;
			prizeMultiplier = 0;
		}
		if (prizeText)
		{
			prizeText.changeText(("Prize: " + prizeValues[prizeMultiplier]));
		}
	}

	public void createOSDObjects()
	{
		osd.createHeader("RACE SETUP");
		osd.createRectangle(0.0, 0.0, 2.0, 2.0, (-(1)), RaceSetup.RR_FRAME);
		osd.createText(
			((track.challenger.name + " vs ") + track.challenged.name),
			Frontend.mediumFont,
			Text.ALIGN_LEFT,
			(-(0.95000005)),
			(-(0.85))
		);
		if ((GameLogic.player.prestige >= opponent.prestige))
		{
			osd.createText(
				"YOU choose the track.",
				Frontend.mediumFont,
				Text.ALIGN_LEFT,
				(-(0.95000005)),
				0.85
			);
		} else
		{
			osd.createText(
				(opponent.name + " chooses the track."),
				Frontend.mediumFont,
				Text.ALIGN_LEFT,
				(-(0.95000005)),
				0.85
			);
		}
		Menu local1;
		Style local2 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, null);
		local1 = osd.createMenu(local2, 0.94000006, 0.86, 0, Osd.MD_HORIZONTAL);
		local1.addItem(new ResourceRef(Osd.RID_CANCEL), RaceSetup.CMD_ABANDON, "ABANDON RACE");
		local1.addItem(new ResourceRef(Osd.RID_OK), RaceSetup.CMD_RACE, "START RACE");
		if ((GameLogic.gameMode != GameLogic.GM_QUICKRACE))
		{
			local1.addSeparator();
			Gadget local3 = local1.addItem(
				new ResourceRef(frontend:0xb4r),
				RaceSetup.CMD_PRIZE,
				"CHANGE THE PRIZE"
			);
			prizeText = osd.createText(null, Frontend.mediumFont, Text.ALIGN_RIGHT, 0.95000005, (-(0.85)));
		}
		local1.addSeparator();
		local1.addItem(new ResourceRef(frontend:0x6ar), RaceSetup.CMD_ZOOM_OUT, "ZOOM OUT");
		local1.addItem(new ResourceRef(frontend:0x61r), RaceSetup.CMD_ZOOM_IN, "ZOOM IN");
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), RaceSetup.CMD_ABANDON, this);
		osd.createHotkey(Input.RCDIK_PGUP, (Input.KEY | Osd.HK_STATIC), RaceSetup.CMD_ZOOM_IN, this);
		osd.createHotkey(Input.RCDIK_PGDN, (Input.KEY | Osd.HK_STATIC), RaceSetup.CMD_ZOOM_OUT, this);
		track.nav.offsetX = (track.nav.offsetZ = 0.0);
		track.nav.changeSize(0.0, 0.120000005, 1.0, 0.78);
		track.nav.changeMode(0);
		track.nav.changeZoom(20.0);
		track.nav.updateNavigator(track.player.car);
		click = new GameRef(track.nav.localroot, frontend:0x65r, "0,0,0 0,0,0", "navigator click-object");
	}

	public void deleteOSDObjects()
	{
		click.destroy();
		track.nav.changeSize(0.01, 0.75, 0.21000001, 0.23);
		track.nav.changeMode(Config.gpsMode);
		track.nav.offsetX = (track.nav.offsetZ = 0.0);
		track.nav.changeZoom(Navigator.DEF_ZOOM);
		track.nav.updateNavigator(track.player.car);
	}

	public void event_handlerClick(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((local4 == GameType.EC_LCLICK))
		{
			Vector3 local5 = Input.cursor.getPickedPos();
			local5.mul(100);
			Vector3 local6 = track.map.getNearestCross(local5);
			if (track.map.getStartDirection(pStart, local6))
			{
				new SfxRef(Frontend.SFX_MENU_SELECT).play();
				pFinish = local6;
				float local7 = track.map.getRouteLength(pStart, pFinish);
				RenderRef local8 = new RenderRef(particles:0x17r);
				local8.cache();
				RenderRef local9 = track.nav.route;
				if (local9)
				{
					local9.destroy();
				}
				local9 = new RenderRef();
				local9.plotRoute(track.nav.localroot, local8, -65536, 10.0, new Vector3(0.01, 0, 0.01));
				track.nav.route = local9;
				track.nav.remMarker(mFinish);
				mFinish = track.nav.addMarker(Marker.RR_FINISH, pFinish, 3);
				track.nav.updateNavigator(track.player.car);
			} else
			{
				new SfxRef(Frontend.SFX_WARNING).play();
			}
		}
	}

}
