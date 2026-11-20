package java.game;

import java.util.resource.*;

public class InventoryItem_Paint extends InventoryItem
{
	PaintCan paintCan;
	RenderRef canObject;

	public InventoryItem_Paint(Inventory arg0, PaintCan arg1)
	{
		super(arg0);
		paintCan = arg1;
	}

	public PaintCan getCan()
	{
		return paintCan;
	}

	public void show(InventoryPanel arg0)
	{
		canObject = new RenderRef(localroot, frontend:0x21r, "cannaatska");
		canObject.setColor(paintCan.color);
		arg0.createDefCamera(0.13, 1);
		arg0.createDefLight();
	}

	public void hide(InventoryPanel arg0)
	{
		arg0.cleanup();
		canObject.destroy();
		canObject = null;
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
