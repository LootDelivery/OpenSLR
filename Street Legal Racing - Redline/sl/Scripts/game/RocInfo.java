package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.util.resource.*;

public class RocInfo extends GameType implements GameState
{
	static final int RID_BG = frontend:0x9fr;
	GameState parentState;
	Osd osd;
	Roc roc;
	int showRound;
	Text[] table1;
	Text[] table2;
	Text title;

	public RocInfo()
	{
		super();
		createNativeInstance();
	}

	public void enter(GameState arg0)
	{
		parentState = arg0;
		roc = ((Garage) parentState).roc;
		osd = new Osd();
		osd.globalHandler = this;
		showRound = roc.getCurrentRound();
		createOSDObjects();
		fillTable();
		osd.show();
		Input.cursor.enable(1);
		setEventMask(GameRef.EVENT_CURSOR);
	}

	public void exit(GameState arg0)
	{
		clearEventMask(GameRef.EVENT_ANY);
		Input.cursor.enable(0);
		osd.hide();
		parentState = null;
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == Input.AXIS_CANCEL))
		{
			GameLogic.changeActiveSection(parentState);
		}
		if ((arg0 == Input.AXIS_MENU_LEFT))
		{
			if ((showRound > 0))
			{
				showRound--;
				fillTable();
			}
		}
		if ((arg0 == Input.AXIS_MENU_RIGHT))
		{
			if ((showRound < (Roc.ROUNDS - 1)))
			{
				showRound++;
				fillTable();
			}
		}
	}

	public void createOSDObjects()
	{
		osd.createBG(new ResourceRef(RocInfo.RID_BG));
		osd.createHeader("RACE OF CHAMPIONS");
		osd.createRectangle(1.2, (-(0.82)), 1.2, 0.22, (-(1)), new ResourceRef(frontend:0x24r));
		title = osd.createText("x", Frontend.mediumFont, Text.ALIGN_RIGHT, 0.98, (-(0.98)));
		Style local1 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_RIGHT, null);
		Menu local2 = osd.createMenu(local1, 0.98, (-(0.82)), 0, Osd.MD_HORIZONTAL);
		local2.addItem(new ResourceRef(frontend:0x126r), Input.AXIS_CANCEL, null);
		local2.addSeparator();
		local2.addSeparator();
		local2.addItem(new ResourceRef(Osd.RID_ARROWRG), Input.AXIS_MENU_RIGHT, null);
		local2.addItem(new ResourceRef(Osd.RID_ARROWLF), Input.AXIS_MENU_LEFT, null);
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_CANCEL, this);
		table1 = new Text[Roc.COMPETITORS];
		table2 = new Text[(Roc.COMPETITORS / 2)];
		float local3 = (-(0.6));
		int local4 = 0;
		while ((local4 < table1.length))
		{
			table1[local4] = osd.createText("x", Frontend.mediumFont, Text.ALIGN_LEFT, (-(0.8)), local3);
			if (((local4 % 2) == 0))
			{
				table2[(local4 / 2)] = osd.createText("x", Frontend.mediumFont, Text.ALIGN_RIGHT, (-(0.8)), (local3 + 0.035));
			} else
			{
				local3 += 0.030000001;
			}
			local3 += 0.07;
			local4++;
		}
	}

	public void fillTable()
	{
		int local1 = roc.getCurrentRound();
		Racer[] local2 = roc.rounds[showRound];
		int local3;
		int local4;
		while ((local4 < local2.length))
		{
			local3 = -1;
			if ((showRound == local1))
			{
				if ((local4 < 2))
				{
					local3 = -171;
				}
			}
			if (local2[local4])
			{
				table1[local4].changeText(local2[local4].name);
			} else
			{
				table1[local4].changeText("-");
			}
			table1[local4].changeColor(local3);
			if (((local4 % 2) == 0))
			{
				table2[(local4 / 2)].changeText((((local4 / 2) + 1) + ". "));
				table2[(local4 / 2)].changeColor(local3);
			}
			local4++;
		}
		while ((local4 < table1.length))
		{
			table1[local4].changeText(null);
			if (((local4 % 2) == 0))
			{
				table2[(local4 / 2)].changeText(null);
			}
			local4++;
		}
		title.changeText(Roc.roundNames[((Roc.ROUNDS - showRound) - 1)]);
		if ((showRound < local1))
		{
			title.changeColor(-171);
		}
	}

}
