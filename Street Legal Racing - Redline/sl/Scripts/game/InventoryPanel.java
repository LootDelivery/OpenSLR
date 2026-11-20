package java.game;

import java.game.parts.*;
import java.render.*;
import java.render.osd.*;
import java.util.resource.*;

public class InventoryPanel
{
	static final int RID_INVENTORY_LIGHT = misc.garage:0x24r;
	VisualInventory inv;
	Dummy bgroot;
	Osd osd;
	InventoryItem invItem;
	RenderRef light;
	RenderRef osdlight = new RenderRef();
	RenderRef bglight = new RenderRef();
	Camera cam;
	Camera osdcam;
	Camera bgcam;
	Rectangle bgRect;
	Gadget button;
	float size;
	int flags;
	Ypr ypr = new Ypr((-(2.5)), (-(0.7)), 0.0);
	Viewport vp = new Viewport();

	public InventoryPanel(VisualInventory arg0, int arg1, float arg2, float arg3, float arg4, float arg5)
	{
		super();
		inv = arg0;
		osd = new Osd(new Viewport(15, arg2, arg3, arg4, arg5));
		osd.iLevel = Osd.IL_TIPS;
		osd.globalHandler = this;
		Style local7 = new Style((arg4 * 2), (arg5 * 2), Frontend.mediumFont, Text.ALIGN_CENTER, null);
		button = osd.createButton(local7, 0.0, 0.0, arg1, null);
		button.enableDrop();
	}

	public void finalize()
	{
		cleanup();
	}

	public void osdCommand(int arg0)
	{
		if (inv.interactive)
		{
			if (osd.dropObject)
			{
				inv.panelDragNDrop(arg0, osd.dropObject);
			} else
			{
				if (osd.dropGadget)
				{
					inv.panelSwap(arg0, osd.dropGadget);
				} else
				{
					inv.panelLeftClick(arg0);
				}
			}
		}
	}

	public void show()
	{
		osd.show();
	}

	public void hide()
	{
		osd.hide();
	}

	public void attachItem(InventoryItem arg0)
	{
		if (arg0)
		{
			if ((arg0 instanceof InventoryItem_Part))
			{
				Part local2 = arg0.getPart();
				if (local2)
				{
					if (local2.catalog_view_ypr)
					{
						ypr = local2.catalog_view_ypr;
					}
				}
			}
			invItem = arg0;
			invItem.show(this);
			button.setToolTip(invItem.getInfo());
		} else
		{
			if (invItem)
			{
				invItem.hide(this);
				invItem = null;
				button.setToolTip(null);
			}
		}
	}

	public void createDefCamera(float arg0, int arg1)
	{
		size = arg0;
		flags = arg1;
		cam = new Camera(invItem.localroot, osd.getViewport(), 1, 110, 0.0009999999, 10.0);
		Vector3 local3 = new Vector3(0.0, arg0, arg0);
		local3.rotate(new Ypr(ypr.y, 0.0, 0.0));
		cam.setMatrix(local3, ypr);
	}

	public void createDefLight()
	{
		light = new RenderRef(invItem.localroot, InventoryPanel.RID_INVENTORY_LIGHT, null);
	}

	public void cleanup()
	{
		if (light)
		{
			light.destroy();
			light = null;
		}
		if (cam)
		{
			cam.destroy();
			light = null;
		}
	}

	public void focusHook()
	{
		if (flags)
		{
			if (cam)
			{
				ypr.y += 0.03;
				Vector3 local1 = new Vector3(0.0, size, size);
				local1.rotate(new Ypr(ypr.y, 0.0, 0.0));
				cam.setMatrix(local1, ypr);
			}
		}
	}

}
