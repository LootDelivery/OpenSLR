package java.game;

import java.game.parts.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;

public class InventoryItem_Folder extends InventoryItem
{
	Inventory inv;
	Set set;
	RenderRef canObject;

	public static InventoryItem_Folder createFromFile(File arg0, Inventory arg1)
	{
		InventoryItem_Folder local2 = new InventoryItem_Folder(arg1);
		local2.set = Set.createFromFile(arg0);
		local2.inv.load(arg0);
		return local2;
	}

	public InventoryItem_Folder(Inventory arg0)
	{
		super(arg0);
		inv = new Inventory(arg0.player);
		PaintCan local2 = ((PaintCan) null);
	}

	public void addItem(Inventory arg0, int arg1)
	{
		arg0.moveToInventory(arg1, inv);
	}

	public void flush()
	{
		inv.flushAll();
	}

	public int getName()
	{
		if (set)
		{
			return set.name;
		}
		return (((("Set: " + inv.items.elementAt(0).getPart().name) + "... (") + inv.items.size()) + " items)");
	}

	public int getDescription()
	{
		if (set)
		{
			return set.description;
		}
		return null;
	}

	public int getPrice()
	{
		int local1;
		int local2 = (inv.items.size() - 1);
		while ((local2 >= 0))
		{
			local1 += inv.items.elementAt(local2).getPrice();
			local2--;
		}
		return local1;
	}

	public int getLogo()
	{
		int local1;
		if (set)
		{
			local1 = set.logo;
		}
		if ((!(local1)))
		{
			int local2 = (inv.items.size() - 1);
			while ((local2 >= 0))
			{
				if ((local1 = inv.items.elementAt(local2).getLogo()))
				{
					break;
				}
				local2--;
			}
		}
		return local1;
	}

	public void copyToInventory(Inventory arg0)
	{
		InventoryItem_Folder local2 = new InventoryItem_Folder(arg0);
		local2.set = set;
		int local3 = (inv.items.size() - 1);
		while ((local3 >= 0))
		{
			inv.items.elementAt(local3).copyToInventory(local2.inv);
			local3--;
		}
		arg0.addItem(local2);
	}

	public String installToCar(Vehicle arg0, Vector3 arg1)
	{
		String local3;
		if (arg0)
		{
			while ((!(inv.items.isEmpty())))
			{
				if ((local3 = inv.installToCar(0, arg0, arg1)))
				{
					break;
				}
			}
		}
		return local3;
	}

	public String getInfo()
	{
		return getName();
	}

	public void show(InventoryPanel arg0)
	{
		canObject = new RenderRef(localroot, frontend:0xabr, "partsbinfolder");
		canObject.setColor(-65536);
		arg0.createDefCamera(1.6, 1);
		arg0.createDefLight();
	}

	public void hide(InventoryPanel arg0)
	{
		arg0.cleanup();
		canObject.destroy();
		canObject = null;
	}

	public void save(File arg0)
	{
		set.save(arg0);
		inv.save(arg0);
	}

	public void finalize()
	{
		if (canObject)
		{
			canObject.destroy();
			canObject = null;
		}
	}

}
