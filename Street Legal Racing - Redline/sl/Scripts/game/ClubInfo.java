package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class ClubInfo extends GameType implements GameState
{
	static final int RID_BG = frontend:0x9fr;
	static final int CMD_BACK = 0;
	static final int CMD_DBG0 = 1;
	static final int CMD_DBG1 = 2;
	static final int CMD_DBG2 = 3;
	static final int CMD_DBG3 = 4;
	static final int CMD_DBG4 = 5;
	static final int CMD_DBG5 = 6;
	static final int CMD_INFO = 7;
	GameState parentState;
	Osd osd;

	public ClubInfo()
	{
		super();
		createNativeInstance();
	}

	public void enter(GameState arg0)
	{
		if ((!(parentState)))
		{
			parentState = arg0;
		}
		osd = new Osd();
		osd.globalHandler = this;
		createOSDObjects();
		osd.show();
		Input.cursor.enable(1);
		setEventMask(GameRef.EVENT_CURSOR);
	}

	public void exit(GameState arg0)
	{
		clearEventMask(GameRef.EVENT_ANY);
		Input.cursor.enable(0);
		osd.hide();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == ClubInfo.CMD_BACK))
		{
			GameLogic.changeActiveSection(parentState);
		} else
		{
			if ((arg0 == ClubInfo.CMD_INFO))
			{
				new WarningDialog(
					GameLogic.player.controller,
					((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
					"ADDITIONAL INFO",
					" \n Your name and position is marked RED. Club members marked YELLOW are ready to be challenged at daytime. \n The higher prestige you have, the more club members will like to race with you, day or night. Prestige grows as you won races and loose the police in a chase, but decreases if you loose a race or the police fines you. \n Night races affect your ranking besides your prestige but prepare, night race prizes are very high.").display(
				);
			} else
			{
				if (((arg0 >= ClubInfo.CMD_DBG0) && (arg0 <= ClubInfo.CMD_DBG3)))
				{
					int local2 = GameLogic.findRacer(GameLogic.player);
					if ((local2 < (GameLogic.speedymen.length - 1)))
					{
						if ((arg0 >= ClubInfo.CMD_DBG2))
						{
							if (GameLogic.canChallenge(GameLogic.player, GameLogic.speedymen[(local2 + 1)]))
							{
								if ((arg0 == ClubInfo.CMD_DBG2))
								{
									GameLogic.challenge(local2, (local2 + 1), 0, 1, 0);
								} else
								{
									GameLogic.challenge(local2, (local2 + 1), 0, 0, 0);
								}
							}
						} else
						{
							if ((arg0 == ClubInfo.CMD_DBG0))
							{
								GameLogic.challenge(local2, (local2 + 1), 0, 1, 1);
							} else
							{
								GameLogic.challenge(local2, (local2 + 1), 0, 0, 1);
							}
						}
						GameLogic.changeActiveSection(this);
					}
				}
			}
		}
	}

	public void createOSDObjects()
	{
		osd.createBG(new ResourceRef((ClubInfo.RID_BG + GameLogic.player.club)));
		osd.createHeader("CLUB INFORMATION");
		osd.createText(
			GameLogic.CLUBNAMES[GameLogic.player.club],
			Frontend.mediumFont,
			Text.ALIGN_RIGHT,
			0.98,
			(-(0.98))
		);
		osd.createRectangle(1.32, (-(0.82)), 1.2, 0.22, (-(1)), new ResourceRef(frontend:0x24r));
		Style local1 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, null);
		Menu local2 = osd.createMenu(local1, 0.98, (-(0.82)), 0, Osd.MD_HORIZONTAL);
		local2.addItem(new ResourceRef(Osd.RID_BACK), ClubInfo.CMD_BACK, "GO BACK TO GARAGE");
		local2.addSeparator();
		local2.addItem(new ResourceRef(frontend:0x124r), ClubInfo.CMD_INFO, "ADDITIONAL INFO");
		if (Config.majomParade)
		{
			local2.addSeparator();
			local2.addSeparator();
			local2.addItem(
				new ResourceRef(Osd.RID_ARROWUP),
				ClubInfo.CMD_DBG0,
				"[CHEAT] NIGHT RACE NEXT OPPONENT (WIN)"
			);
			local2.addItem(
				new ResourceRef(Osd.RID_ARROWUP),
				ClubInfo.CMD_DBG1,
				"[CHEAT] NIGHT RACE NEXT OPPONENT (LOOSE)"
			);
			local2.addSeparator();
			local2.addItem(
				new ResourceRef(Osd.RID_ARROWUP),
				ClubInfo.CMD_DBG2,
				"[CHEAT] DAY RACE NEXT OPPONENT (WIN)"
			);
			local2.addItem(
				new ResourceRef(Osd.RID_ARROWUP),
				ClubInfo.CMD_DBG3,
				"[CHEAT] DAY RACE NEXT OPPONENT (LOOSE)"
			);
		}
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), ClubInfo.CMD_BACK, this);
		int local3;
		int local4 = (GameLogic.player.club * GameLogic.CLUBMEMBERS);
		int local5 = 0;
		while ((local5 < GameLogic.CLUBMEMBERS))
		{
			if ((GameLogic.speedymen[(local4 + local5)] == GameLogic.player))
			{
				local3 = -43691;
			} else
			{
				if (GameLogic.canChallenge(GameLogic.player, GameLogic.speedymen[(local4 + local5)]))
				{
					local3 = -171;
				} else
				{
					local3 = -1;
				}
			}
			osd.createText(
				((GameLogic.CLUBMEMBERS - local5) + ". "),
				Frontend.mediumFont,
				Text.ALIGN_RIGHT,
				(-(0.8)),
				(0.68 - (local5 * 0.07))).changeColor(local3
			);
			osd.createText(
				GameLogic.speedymen[(local4 + local5)].name,
				Frontend.mediumFont,
				Text.ALIGN_LEFT,
				(-(0.8)),
				(0.68 - (local5 * 0.07))).changeColor(local3
			);
			int local6 = (GameLogic.speedymen[(local4 + local5)].prestige * Racer.PRESTIGE_SCALE);
			osd.createText(
				("prestige: " + local6),
				Frontend.mediumFont,
				Text.ALIGN_LEFT,
				0.05,
				(0.68 - (local5 * 0.07))).changeColor(local3
			);
			local5++;
		}
	}

}
