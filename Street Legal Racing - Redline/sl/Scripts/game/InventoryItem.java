package java.game;

import java.util.resource.*;

public class InventoryItem
{
	Inventory inventory;
	Dummy localroot;

	public InventoryItem(Inventory arg0)
	{
		super();
		inventory = arg0;
		localroot = new Dummy(inventory, GameRef.WORLDTREEROOT);
	}

	public String getInfo()
	{
	}

}
