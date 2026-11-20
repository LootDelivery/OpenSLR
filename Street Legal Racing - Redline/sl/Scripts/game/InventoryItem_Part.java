package java.game;

import java.game.parts.*;
import java.util.resource.*;

public class InventoryItem_Part extends InventoryItem
{
	private Part partXXX;
	private int partTypeID;
	int compatibility;

	public InventoryItem_Part(Inventory arg0, int arg1)
	{
		super(arg0);
		setPart(arg1);
	}

	public InventoryItem_Part(Inventory arg0, Part arg1)
	{
		this(arg0);
		arg1.command(("remove 0 " + localroot.id()));
		arg1.setParent(localroot);
		arg1.setPos(new Vector3(0, 0, 0));
		setPart(arg1);
	}

	public void setPart(int arg0)
	{
		partTypeID = arg0;
		partXXX = null;
	}

	public void setPart(Part arg0)
	{
		partTypeID = 0;
		partXXX = arg0;
	}

	public Part getPart()
	{
		if ((!(partXXX)))
		{
			if ((!(partTypeID)))
			{
				return null;
			} else
			{
				GameRef local1 = new GameRef();
				partXXX = local1.create(
					localroot,
					new GameRef(partTypeID),
					"0,0,0,0,0,0",
					"part_created_by_inventory"
				);
				partTypeID = 0;
			}
		}
		return partXXX;
	}

	public void flush()
	{
		if (partXXX)
		{
			partTypeID = partXXX.getInfo(GameType.GII_TYPE);
			partXXX.destroy();
			partXXX = null;
		}
	}

	public int getName()
	{
		return getPart().name;
	}

	public int getDescription()
	{
		return getPart().description;
	}

	public int getPrice()
	{
		return getPart().currentPrice();
	}

	public int getLogo()
	{
		return getPart().getLogo();
	}

	public void copyToInventory(Inventory arg0)
	{
		arg0.addItem(getPart().getInfo(GameType.GII_TYPE));
	}

	public String installToCar(Vehicle arg0, Vector3 arg1)
	{
		String local3;
		if (arg0)
		{
			Part local4 = getPart();
			GameRef local5 = new GameRef();
			int[] local6 = local4.install_OK(arg0, 0, local5, 0, arg1);
			if (local6)
			{
				if ((!((local3 = local4.installCheck(local5.getScriptInstance(), local6)))))
				{
					if (arg1)
					{
						local4.command(((((((("install 0 " + arg0.id()) + " 0 0 0 ") + arg1.x) + " ") + arg1.y) + " ") + arg1.z));
					} else
					{
						local4.command(("install 0 " + arg0.id()));
					}
				}
			} else
			{
				local3 = "";
			}
			if ((!(local3)))
			{
				setPart(null);
			}
		}
		return local3;
	}

	public String getInfo()
	{
		Part local1 = getPart();
		int local2 = (local1.getConditionNoAttach() * 100);
		String local3;
		if (local1.isComplex())
		{
			local3 = (local3 + "+");
		}
		if (local1.isTuneable())
		{
			local3 = (local3 + "T");
		}
		if ((!(local1.isStreetLegal())))
		{
			local3 = (local3 + "!");
		}
		if (local3)
		{
			local3 = ((" [" + local3) + "]");
		}
		return ((((local1.name + local3) + " (") + local2) + "%)");
	}

	public void show(InventoryPanel arg0)
	{
		Part local2 = getPart();
		if (local2)
		{
			local2.setMatrix(null, null);
			arg0.createDefCamera((getPart().getInfo(GameType.GII_SIZE) / 100.0), 1);
			arg0.createDefLight();
		}
	}

	public void hide(InventoryPanel arg0)
	{
		arg0.cleanup();
	}

}
