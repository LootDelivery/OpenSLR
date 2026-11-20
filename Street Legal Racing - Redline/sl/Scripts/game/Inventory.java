package java.game;

import java.game.parts.*;
import java.io.*;
import java.render.*;
import java.util.*;
import java.util.resource.*;

public class Inventory extends GameType
{
	Player player;
	Vector items;

	public Inventory(Player arg0)
	{
		super();
		createNativeInstance();
		player = arg0;
		items = new Vector();
	}

	public int size()
	{
		return items.size();
	}

	public void addItem(InventoryItem arg0)
	{
		items.addElement(arg0);
	}

	public int addItem(PaintCan arg0)
	{
		int local2 = items.size();
		items.addElement(new InventoryItem_Paint(this, arg0));
		update();
		return local2;
	}

	public int addItem(int arg0)
	{
		int local2 = items.size();
		items.addElement(new InventoryItem_Part(this, arg0));
		update();
		return local2;
	}

	public int insertItem(int arg0)
	{
		int local2 = 0;
		items.insertElementAt(new InventoryItem_Part(this, arg0), local2);
		update();
		return local2;
	}

	public int addItem(Part arg0)
	{
		int local2 = items.size();
		items.addElement(new InventoryItem_Part(this, arg0));
		update();
		return local2;
	}

	public void addItem(Part arg0, int arg1)
	{
		items.setElementAt(new InventoryItem_Part(this, arg0), arg1);
		update();
	}

	public String installToCar(int arg0, Vehicle arg1, Vector3 arg2)
	{
		String local4;
		if ((arg0 < items.size()))
		{
			InventoryItem local5 = items.elementAt(arg0);
			if ((local5 instanceof InventoryItem_Folder))
			{
				if ((!((local4 = local5.installToCar(arg1, arg2)))))
				{
					items.removeElementAt(arg0);
					update();
				}
			} else
			{
				if (arg1)
				{
					Part local6 = local5.getPart();
					if ((!((local4 = local5.installToCar(arg1, arg2)))))
					{
						arg1.wakeUp();
						items.removeElementAt(arg0);
						update();
						new SfxRef(GameLogic.SFX_WRENCH).play();
						GameLogic.spendTime(GameLogic.mechTime(local6, 1));
					}
				}
			}
		}
		if (local4)
		{
			new SfxRef(Frontend.SFX_WARNING).play();
		}
		return local4;
	}

	public void moveToInventory(int arg0, Inventory arg1)
	{
		if ((arg0 < items.size()))
		{
			InventoryItem local3 = items.removeElementAt(arg0);
			update();
			arg1.items.addElement(local3);
			arg1.update();
		}
	}

	public void removeItem(int arg0)
	{
		if ((arg0 < items.size()))
		{
			items.removeElementAt(arg0);
			update();
		}
	}

	public void removeItem(InventoryItem arg0)
	{
		items.removeElement(arg0);
		update();
	}

	public void flushAll()
	{
		InventoryItem local1;
		int local2 = 0;
		while ((local2 < items.size()))
		{
			local1 = items.elementAt(local2);
			local1.flush();
			local2++;
		}
	}

	public int getItemIDbyPart(GameRef arg0)
	{
		int local2;
		InventoryItem local3;
		local2 = 0;
		while ((local2 < items.size()))
		{
			local3 = items.elementAt(local2);
			if ((local3.getPart().id() == arg0.id()))
			{
				break;
			}
			local2++;
		}
		return local2;
	}

	public PaintCan getCanbyIndex(int arg0)
	{
		if ((arg0 < items.size()))
		{
			InventoryItem local2 = items.elementAt(arg0);
			return local2.getCan();
		}
		return null;
	}

	public Part getPartbyItemID(int arg0)
	{
		if ((arg0 < items.size()))
		{
			InventoryItem local2 = items.elementAt(arg0);
			return local2.getPart();
		}
		return null;
	}

	public void swap(int arg0, int arg1)
	{
		if (((arg0 < items.size()) && (arg1 < items.size())))
		{
			InventoryItem local3;
			local3 = items.elementAt(arg0);
			items.setElementAt(items.elementAt(arg1), arg0);
			items.setElementAt(local3, arg1);
		}
	}

	public void drop(int arg0, int arg1)
	{
		if (((arg0 < items.size()) && (arg1 < items.size())))
		{
			InventoryItem local3 = items.elementAt(arg0);
			InventoryItem local4 = items.elementAt(arg1);
			if ((!((local3 instanceof InventoryItem_Folder))))
			{
				if ((!((local4 instanceof InventoryItem_Folder))))
				{
					InventoryItem_Folder local5 = new InventoryItem_Folder(this);
					local5.addItem(this, arg1);
					items.insertElementAt(local5, arg1);
					local4 = local5;
				}
				local4.addItem(this, arg0);
			}
		}
	}

	public void update()
	{
	}

	public void save(File arg0)
	{
		InventoryItem local2;
		int local3 = items.size();
		arg0.write(local3);
		int local4 = 0;
		while ((local4 < local3))
		{
			local2 = items.elementAt(local4);
			if ((local2 instanceof InventoryItem_Part))
			{
				arg0.write(0);
				local2.getPart().save(arg0);
			} else
			{
				arg0.write(1);
				local2.save(arg0);
			}
			local4++;
		}
	}

	public void load(File arg0)
	{
		int local2 = arg0.readInt();
		int local3 = 0;
		while ((local3 < local2))
		{
			int local4 = arg0.readInt();
			if ((local4 == 0))
			{
				Part local5 = Part.createFromFile(arg0, player);
				addItem(local5);
			} else
			{
				InventoryItem_Folder local5_2 = InventoryItem_Folder.createFromFile(arg0, this);
				addItem(local5_2);
			}
			local3++;
		}
	}

}
