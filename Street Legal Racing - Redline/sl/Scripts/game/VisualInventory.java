package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.io.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class VisualInventory extends Inventory
{
	int linesPerPage;
	int partsPerLine;
	InventoryPanel[] panels;
	int cline;
	int start;
	int stop;
	int visualsUpdated;
	int interactive;

	public VisualInventory(Player arg0, float arg1, float arg2, float arg3, float arg4)
	{
		this(arg0, arg1, arg2, arg3, arg4, 1);
	}

	public VisualInventory(Player arg0, float arg1, float arg2, float arg3, float arg4, int arg5)
	{
		super(arg0);
		interactive = arg5;
		initVisuals(arg1, arg2, arg3, arg4);
	}

	public void initVisuals(float arg0, float arg1, float arg2, float arg3)
	{
		linesPerPage = 1;
		partsPerLine = 5;
		cline = 0;
		start = (cline * partsPerLine);
		stop = (start + (linesPerPage * partsPerLine));
		float local5 = 0.012999999;
		float local6 = 0.01;
		float local7;
		float local8;
		local7 = ((arg2 - ((partsPerLine - 1) * local5)) / partsPerLine);
		local8 = ((arg3 - ((linesPerPage - 1) * local6)) / linesPerPage);
		panels = new InventoryPanel[(partsPerLine * linesPerPage)];
		int local9;
		float local10 = arg1;
		int local11 = 0;
		while ((local11 < linesPerPage))
		{
			float local12 = arg0;
			int local13 = 0;
			while ((local13 < partsPerLine))
			{
				local9 = ((local11 * partsPerLine) + local13);
				panels[local9] = new InventoryPanel(this, local9, local12, local10, local7, local8);
				local12 += (local7 + local5);
				local13++;
			}
			local10 += (local8 + local6);
			local11++;
		}
	}

	public int addItem(int arg0)
	{
		int local2 = super.addItem(arg0);
		scrollTo(local2);
		return local2;
	}

	public int addItem(Part arg0)
	{
		int local2 = super.addItem(arg0);
		scrollTo(local2);
		return local2;
	}

	public int getItemIDbyButtonPhyId(int arg0)
	{
		int local2 = 0;
		while ((local2 < panels.length))
		{
			if ((panels[local2].button.phy.id() == arg0))
			{
				return (local2 + start);
			}
			local2++;
		}
		return items.size();
	}

	public int getItemIDbyButton(Gadget arg0)
	{
		int local2 = 0;
		while ((local2 < panels.length))
		{
			if ((panels[local2].button == arg0))
			{
				return (local2 + start);
			}
			local2++;
		}
		return items.size();
	}

	public InventoryItem getItembyButton(Gadget arg0)
	{
		int local2 = getItemIDbyButton(arg0);
		if ((local2 < items.size()))
		{
			return items.elementAt(local2);
		}
		return null;
	}

	public Part getPartbyButton(GameRef arg0)
	{
		int local2;
		local2 = 0;
		while ((local2 < panels.length))
		{
			if ((panels[local2].button.id() == arg0.id()))
			{
				break;
			}
			local2++;
		}
		if ((local2 != panels.length))
		{
			if (((local2 + start) < items.size()))
			{
				InventoryItem local3 = items.elementAt((local2 + start));
				return local3.getPart();
			} else
			{
				return new Part();
			}
		}
		return null;
	}

	public InventoryPanel getPanelbyButtonPhyId(int arg0)
	{
		int local2;
		local2 = 0;
		while ((local2 < panels.length))
		{
			if ((panels[local2].button.phy.id() == arg0))
			{
				break;
			}
			local2++;
		}
		if ((local2 != panels.length))
		{
			return panels[local2];
		}
		return null;
	}

	public void show()
	{
		int local1 = 0;
		while ((local1 < panels.length))
		{
			panels[local1].show();
			local1++;
		}
		setEventMask((GameRef.EVENT_CURSOR | GameRef.EVENT_COMMAND));
	}

	public void hide()
	{
		clearEventMask(GameRef.EVENT_ANY);
		int local1 = 0;
		while ((local1 < panels.length))
		{
			panels[local1].hide();
			local1++;
		}
	}

	public void panelSwap(int arg0, Gadget arg1)
	{
		int local3 = getItemIDbyButton(arg1);
		if (((arg0 < panels.length) && (local3 < items.size())))
		{
			new SfxRef(GameLogic.SFX_DRAGDROP).play();
			swap((arg0 + start), local3);
			update();
		}
	}

	public void panelDragNDrop(int arg0, GameRef arg1)
	{
		if ((arg0 < panels.length))
		{
			Object local3 = arg1.getScriptInstance();
			if ((local3 instanceof Part))
			{
				Part local4 = local3;
				if ((!((local4 instanceof Chassis))))
				{
					int local5 = local4.getInfo(GameType.GII_REMOVE_OK);
					if ((local5 != (-(1))))
					{
						float local6 = GameLogic.mechTime(local4, 0);
						new SfxRef(GameLogic.SFX_WRENCH).play();
						addItem(local4);
						player.car.wakeUp();
						if (((arg0 + start) < (items.size() - 1)))
						{
							swap((arg0 + start), (items.size() - 1));
						}
						GameLogic.spendTime(GameLogic.mechTime(local4, 0));
					}
				}
			}
		}
	}

	public void panelLeftClick(int arg0)
	{
		String local2;
		Part local3;
		int local4;
		arg0 += (currentLine() * partsPerLine);
		if ((arg0 < items.size()))
		{
			if (((local2 = installToCar(arg0, player.car, null)) && (local2 != "")))
			{
				new WarningDialog(
					player.controller,
					(((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
					"WARNING",
					local2).display(
				);
			}
		}
	}

	public void upScroll()
	{
		if (cline)
		{
			cline--;
			update();
		}
	}

	public void downScroll()
	{
		if ((items.size() > 1))
		{
			if ((cline < ((items.size() - 1) / partsPerLine)))
			{
				cline++;
				update();
			}
		}
	}

	public void scrollTo(int arg0)
	{
		int local2 = (arg0 / partsPerLine);
		scrollToLine(local2);
	}

	public void scrollToLine(int arg0)
	{
		if ((cline != arg0))
		{
			if (((arg0 >= 0) && (arg0 <= ((items.size() - 1) / partsPerLine))))
			{
				cline = arg0;
				update();
			}
		}
	}

	public int currentLine()
	{
		return cline;
	}

	public void update()
	{
		if ((cline && ((cline * partsPerLine) >= items.size())))
		{
			cline--;
		}
		int local1;
		int local2;
		int local3 = start;
		int local4 = stop;
		start = (cline * partsPerLine);
		stop = (start + (linesPerPage * partsPerLine));
		if (((local3 == start) && (local4 == stop)))
		{
			local2 = 0;
			local1 = start;
			while ((local1 < stop))
			{
				InventoryItem local5;
				if ((local1 < items.size()))
				{
					local5 = items.elementAt(local1);
				}
				if ((local5 != panels[local2].invItem))
				{
					panels[local2].attachItem(null);
				}
				local2++;
				local1++;
			}
			local2 = 0;
			local1 = start;
			while ((local1 < stop))
			{
				InventoryItem local5_2;
				if ((local1 < items.size()))
				{
					local5_2 = items.elementAt(local1);
				}
				if ((local5_2 != panels[local2].invItem))
				{
					panels[local2].attachItem(local5_2);
				}
				local2++;
				local1++;
			}
		} else
		{
			local2 = 0;
			local1 = local3;
			while ((local1 < local4))
			{
				panels[local2++].attachItem(null);
				local1++;
			}
			local2 = 0;
			local1 = start;
			while ((local1 < stop))
			{
				InventoryItem local5_3;
				if ((local1 < items.size()))
				{
					local5_3 = items.elementAt(local1);
				}
				panels[local2++].attachItem(local5_3);
				local1++;
			}
		}
		visualsUpdated = 1;
	}

}
