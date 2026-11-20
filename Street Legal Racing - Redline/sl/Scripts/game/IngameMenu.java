package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.resource.*;

public class IngameMenu extends OptionsDialog
{
	static final int CMD_BACKTOGARAGE = 50;
	static final int CMD_MODIFY = 51;
	static final int CMD_MECHANIC = 52;
	static final int CMD_PAINT = 53;
	static final int CMD_QUICKREPAIR = 54;
	static final int CMD_CONTINUE = 55;
	static final int MM_ROC = 0;
	static final int MM_CITY = 1;
	static final int MM_TESTTRACK = 2;
	int mode;
	Track track;
	int reqTrackExit;
	int modifyGroup;
	Button goBackButton;
	int towCarFee;
	int agatyadisramegy;

	public IngameMenu(Track arg0)
	{
		super(((Dialog.DF_DARKEN | Dialog.DF_MODAL) | Dialog.DF_FREEZE));
		track = arg0;
		if ((arg0 instanceof ROCTrack))
		{
			mode = IngameMenu.MM_ROC;
		} else
		{
			if ((arg0 instanceof City))
			{
				mode = IngameMenu.MM_CITY;
			} else
			{
				mode = IngameMenu.MM_TESTTRACK;
			}
		}
		createOSDObjects();
		changeMode(mainGroup);
	}

	public void show()
	{
		String local1;
		towCarFee = 0;
		agatyadisramegy = 0;
		if ((GameLogic.gameMode == GameLogic.GM_CARREER))
		{
			if ((mode == IngameMenu.MM_CITY))
			{
				City local2 = track;
				Vector3 local3 = player.car.getPos();
				local3.sub(local2.posStart);
				towCarFee += ((local3.length() * 0.03) + 10);
				local1 = "TOW CAR TO GARAGE FOR $";
				if (local2.nightTime)
				{
					if ((local2.nrPlayerRace == 2))
					{
						if (local2.nrPrize)
						{
							local1 = ("PAY BET AND " + local1);
							towCarFee += local2.nrPrize;
						} else
						{
							local1 = ("LOOSE CAR AND " + local1);
							agatyadisramegy = 1;
						}
					}
				} else
				{
					if (local2.raceState)
					{
						if (local2.prize)
						{
							local1 = ("PAY BET AND " + local1);
						} else
						{
							local1 = ("QUIT RACE AND " + local1);
						}
						towCarFee += local2.prize;
					}
				}
				if (local2.policeState)
				{
					local1 = ("PAY FINE AND " + local1);
					int[] local4 = local2.calculateFineSum(0);
					towCarFee += local4[0];
				}
				local1 = (local1 + towCarFee);
				if ((towCarFee < 13))
				{
					towCarFee = 0;
					local1 = "GO BACK TO GARAGE";
				}
			} else
			{
				if ((mode == IngameMenu.MM_ROC))
				{
					if (((ROCTrack) track).testMode)
					{
						local1 = "EXIT TEST RUN";
					} else
					{
						local1 = "GIVE UP RACE";
					}
				} else
				{
					local1 = "GO BACK TO GARAGE";
				}
			}
		} else
		{
			if ((GameLogic.gameMode == GameLogic.GM_SINGLECAR))
			{
				local1 = "GO BACK TO GARAGE";
			} else
			{
				local1 = "EXIT TO MAIN MENU";
			}
		}
		goBackButton.changeLabelText(local1);
		super.show();
	}

	public void createOSDObjects()
	{
		Menu local1;
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("INGAME MENU");
		local1 = osd.createMenu(butt1, 0.0, (-(0.5)), 0);
		local1.addItem("CONTINUE", IngameMenu.CMD_CONTINUE);
		local1.addItem("OPTIONS", OptionsDialog.CMD_OPTIONS);
		goBackButton = local1.addItem("CHANGE ME", IngameMenu.CMD_BACKTOGARAGE);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, IngameMenu.CMD_CONTINUE, this);
		osd.hideGroup((mainGroup = osd.endGroup()));
		osd.createBG(OptionsDialog.RID_GENERALBG);
		osd.createHeader("MODIFY CAR");
		local1 = osd.createMenu(butt1, 0.0, (-(0.5)), 0);
		local1.addItem("QUICK REPAIR", IngameMenu.CMD_QUICKREPAIR);
		local1.addItem("MANUAL REPAIR", IngameMenu.CMD_MECHANIC);
		local1.addItem("BODY PAINT", IngameMenu.CMD_PAINT);
		local1.addItem("BACK", OptionsDialog.CMD_MAIN);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, OptionsDialog.CMD_MAIN, this);
		osd.hideGroup((modifyGroup = osd.endGroup()));
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == IngameMenu.CMD_MODIFY))
		{
			changeMode(modifyGroup);
		} else
		{
			if ((arg0 == IngameMenu.CMD_QUICKREPAIR))
			{
				osdCommand(OptionsDialog.CMD_MAIN);
				osdCommand(IngameMenu.CMD_CONTINUE);
				player.car.repair();
			} else
			{
				if ((arg0 == IngameMenu.CMD_MECHANIC))
				{
					osdCommand(OptionsDialog.CMD_MAIN);
					osdCommand(IngameMenu.CMD_CONTINUE);
					track.osdCommand(track.CMD_MECHANIC);
				} else
				{
					if ((arg0 == IngameMenu.CMD_PAINT))
					{
						osdCommand(OptionsDialog.CMD_MAIN);
						osdCommand(IngameMenu.CMD_CONTINUE);
						track.osdCommand(track.CMD_PAINT);
					} else
					{
						if ((arg0 == IngameMenu.CMD_BACKTOGARAGE))
						{
							if ((mode == IngameMenu.MM_CITY))
							{
								track.calculateFineSum(1);
								player.money -= towCarFee;
								if ((player.money < 0))
								{
									player.money = 0;
								}
								if (agatyadisramegy)
								{
									track.killCar = 1;
								}
								GameLogic.spendTime((towCarFee * 60));
							}
							reqTrackExit = 1;
							track.osdCommand(Track.CMD_INGAMEMENU);
						} else
						{
							if ((arg0 == IngameMenu.CMD_CONTINUE))
							{
								new SfxRef(Frontend.SFX_MENU_BACK).play();
								track.osdCommand(Track.CMD_INGAMEMENU);
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
