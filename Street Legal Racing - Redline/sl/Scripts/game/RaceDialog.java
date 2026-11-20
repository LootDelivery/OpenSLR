package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class RaceDialog extends CsDialog
{
	public RaceDialog(Controller arg0, int arg1, ResourceRef arg2, ResourceRef arg3, ResourceRef arg4, String arg5, String arg6, float arg7, float arg8, int arg9, int arg10)
	{
		this(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, null, null);
	}

	public RaceDialog(Controller arg0, int arg1, ResourceRef arg2, ResourceRef arg3, ResourceRef arg4, String arg5, String arg6, float arg7, float arg8, int arg9, int arg10, Racer arg11, Racer arg12)
	{
		super(arg0, (arg1 | Dialog.DF_FREEZE), arg2);
		osd.createRectangle((-(0.75)), (-(0.6)), 0.4, 0.7, 1, arg3);
		osd.createRectangle(0.75, (-(0.6)), 0.4, 0.7, 1, arg4);
		String local14 = arg5;
		String local15 = arg6;
		float local16 = 0.27;
		if (((arg10 == 1) || (arg10 == 3)))
		{
			String local17;
			if ((arg7 == 1000.0))
			{
				local17 = "--:--:--";
			} else
			{
				local17 = String.timeToString(arg7, String.TCF_NOHOURS);
			}
			local14 = ((arg5 + " \n Time: ") + local17);
			if ((arg8 == 1000.0))
			{
				local17 = "--:--:--";
			} else
			{
				local17 = String.timeToString(arg8, String.TCF_NOHOURS);
			}
			local15 = ((arg6 + " \n Time: ") + local17);
			local16 = 0.26;
		}
		osd.createTextBox(
			local14,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			((-(0.75)) - 0.2),
			((-(0.6)) + local16),
			0.4
		);
		osd.createTextBox(
			local15,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			(0.75 - 0.2),
			((-(0.6)) + local16),
			0.4
		);
		String local17_2;
		if ((arg10 == 0))
		{
			local17_2 = (((arg5 + " racing against ") + arg6) + " \n The bet is: ");
		} else
		{
			if (((arg10 == 1) || (arg10 == 5)))
			{
				local17_2 = (((arg5 + " has won against ") + arg6) + " \n The prize is: ");
			} else
			{
				if ((arg10 == 2))
				{
					local17_2 = ((arg5 + " racing against ") + arg6);
				} else
				{
					if ((arg10 == 3))
					{
						local17_2 = ((arg5 + " has won against ") + arg6);
					}
				}
			}
		}
		if ((((arg10 == 0) || (arg10 == 1)) || (arg10 == 5)))
		{
			if ((arg9 == 0))
			{
				if ((arg10 == 5))
				{
					local17_2 = (local17_2 + "PRESTIGE ONLY");
				} else
				{
					local17_2 = (local17_2 + "PINK SLIPS");
				}
			} else
			{
				local17_2 = ((local17_2 + "$") + arg9);
			}
		} else
		{
			if ((arg10 == 2))
			{
				local17_2 = (((local17_2 + " \n \n Run ") + arg9) + " of 3");
			}
		}
		osd.createTextBox(
			((" \n " + local17_2) + " \n \n "),
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			(-(0.4)),
			(-(0.90000004)),
			0.8,
			City.RRT_FRAME
		);
		if ((Config.majomParade && (((arg10 != 4) && arg11) && arg12)))
		{
			Racer[] local18 = new Racer[2];
			String[] local19 = new String[2];
			String[] local20 = new String[2];
			local18[0] = arg11;
			local18[1] = arg12;
			int local21 = 0;
			while ((local21 < 2))
			{
				if ((local18[local21].car && local18[local21].car.chassis))
				{
					local19[local21] = local18[local21].getPrestigeString();
					local20[local21] = local18[local21].car.chassis.vehicleName;
				} else
				{
					if (((((arg10 == 0) || (arg10 == 1)) && (local18[local21] instanceof Bot)) && ((Bot) local18[local21]).nightVd))
					{
						local19[local21] = ((Bot) local18[local21]).getPrestigeString(arg10);
						local20[local21] = ((Bot) local18[local21]).nightVd.vehicleName;
					} else
					{
						if (((((arg10 == 4) || (arg10 == 5)) && (local18[local21] instanceof Bot)) && ((Bot) local18[local21]).botVd))
						{
							local19[local21] = ((Bot) local18[local21]).getPrestigeString(arg10);
							local20[local21] = ((Bot) local18[local21]).botVd.vehicleName;
						} else
						{
							local19[local21] = "0/0";
							local20[local21] = "unknown";
						}
					}
				}
				local21++;
			}
			float local21_2 = 0.95000005;
			float local22 = (-(0.2));
			int local23 = 0;
			osd.createText("Prestige", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, local22, local23);
			osd.createText(local19[0], Frontend.mediumFont, Text.ALIGN_LEFT, (-(local21_2)), local22, local23);
			osd.createText(local19[1], Frontend.mediumFont, Text.ALIGN_RIGHT, local21_2, local22, local23);
			local23++;
			osd.createText("Vehicle", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, local22, local23);
			osd.createText(local20[0], Frontend.mediumFont, Text.ALIGN_LEFT, (-(local21_2)), local22, local23);
			osd.createText(local20[1], Frontend.mediumFont, Text.ALIGN_RIGHT, local21_2, local22, local23);
			local23++;
		}
		if (((arg10 == 1) || (arg10 == 5)))
		{
			if ((arg9 == 0))
			{
				if ((arg10 == 1))
				{
					osd.createRectangle(0.55, 0.65000004, 0.75, 0.5, 1, new ResourceRef(frontend:0x117r));
				}
			} else
			{
				osd.createRectangle(0.55, 0.65000004, 0.75, 0.5, 1, new ResourceRef(frontend:0x118r));
			}
		}
		if ((arg10 == 3))
		{
			int local18_2;
			if ((arg9 < 0))
			{
				local18_2 = 1;
				arg9 = (-(arg9));
			}
			int local19_2 = (arg9 % 10);
			arg9 = (arg9 / 10);
			int local20_2 = (arg9 % 10);
			arg9 = (arg9 / 10);
			int local21_3 = arg9;
			int local22_2 = ((local20_2 == 2) || (local21_3 == 2));
			int local23_2 = ((local19_2 == 1) && (local21_3 == 2));
			int local24 = (local22_2 && ((local20_2 + local21_3) == 2));
			if (local23_2)
			{
				local17_2 = "Congratulations, you won the Race of Champions!";
				if (local18_2)
				{
					local17_2 = (local17_2 + " \n (Thought you lost your last run.)");
				}
			} else
			{
				if (local22_2)
				{
					if (local18_2)
					{
						local17_2 = "You've lost this race and the Race of Champions. The next event will take place in 6 months time.";
					} else
					{
						local17_2 = (("Congratulations, you qualified to the " + Roc.roundNames[(local19_2 - 1)]) + " of the Race of Champions!");
					}
					if (local24)
					{
						local17_2 = (local17_2 + " \n (Run 3 is skipped)");
					}
				} else
				{
					if (local18_2)
					{
						local17_2 = "You've lost this run, but not this round. Go for the next run!";
					} else
					{
						local17_2 = "Congratulations, you've won this run. Go for the next run!";
					}
				}
			}
			osd.createTextBox(
				((" \n " + local17_2) + " \n \n "),
				Frontend.mediumFont,
				Text.ALIGN_CENTER,
				0.175,
				0.55,
				0.75,
				City.RRT_FRAME
			);
		}
	}

}
