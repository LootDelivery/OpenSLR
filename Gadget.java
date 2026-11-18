package java.render.osd;

import java.render.*;
import java.util.*;
import java.util.resource.*;

public class Gadget
{
	static final ResourceRef HIGHLIGHT_NORMAL = new ResourceRef(frontend:0x12br);
	static final ResourceRef HIGHLIGHT_DISABLED = new ResourceRef(frontend:0x13cr);
	static final float HIGHLIGHT_NORMAL_SCALE = 1.1;
	static final float HIGHLIGHT_DISABLED_SCALE = 1.1;
	static final float HIGHLIGHT_ACTIVE_SCALE = 1.2;
	Osd osd;
	PhysicsRef phy;
	Rectangle rect;
	Label label;
	int cmd;
	Object handler;
	String tooltip;
	int disabled;
	int pressed;
	int dropEnabled;
	int noleaverelease;
	int nofocus;
	int highliteMethod = Osd.HL_LABELCOLOR;
	Rectangle hr;
	Gadget parent;

	public Gadget(Osd arg0, PhysicsRef arg1, Rectangle arg2, int arg3, Object arg4)
	{
		super();
		osd = arg0;
		phy = arg1;
		rect = arg2;
		cmd = arg3;
		handler = arg4;
	}

	public void setLabel(Text arg0)
	{
		label = new Label(arg0);
	}

	public Text getLabel()
	{
		if (label)
		{
			return label.txt;
		}
		return null;
	}

	public String getLabelText()
	{
		if (label)
		{
			return label.txt.text;
		}
		return null;
	}

	public void changeLabelText(String arg0)
	{
		if (label)
		{
			label.txt.changeText(arg0);
		}
	}

	public void changeTexture(ResourceRef arg0)
	{
		if (rect)
		{
			rect.changeTexture(arg0);
		}
	}

	public void setToolTip(String arg0)
	{
		tooltip = arg0;
	}

	public void enableDrop()
	{
		dropEnabled = 1;
	}

	public void disable()
	{
		disabled = 1;
		if ((osd.pointedGadget == this))
		{
			osd.deactivateText();
		}
		if (label)
		{
			label.txt.changeColor(Osd.COLOR_DISABLED);
		}
	}

	public void enable()
	{
		disabled = 0;
		if (label)
		{
			label.txt.changeColor(Osd.COLOR_UNSELECTED);
		}
	}

	public void focus()
	{
		if (((highliteMethod & Osd.HL_LABELCOLOR) && label))
		{
			label.txt.changeColor(Osd.COLOR_SELECTED);
		}
		if (((highliteMethod & Osd.HL_BACKLIGHT) && rect))
		{
			Vector3 local1 = new Vector3(rect.pos);
			if ((!(disabled)))
			{
				local1.z -= 0.00049999997;
				hr = new Rectangle(
					rect.root,
					local1,
					new RectangleTemplate((rect.rt.width * Gadget.HIGHLIGHT_NORMAL_SCALE), (rect.rt.height * Gadget.HIGHLIGHT_NORMAL_SCALE), Gadget.HIGHLIGHT_NORMAL)
				);
			} else
			{
				local1.z += 0.00049999997;
				hr = new Rectangle(
					rect.root,
					local1,
					new RectangleTemplate((rect.rt.width * Gadget.HIGHLIGHT_DISABLED_SCALE), (rect.rt.height * Gadget.HIGHLIGHT_DISABLED_SCALE), Gadget.HIGHLIGHT_DISABLED)
				);
			}
		}
	}

	public void unfocus()
	{
		if (((highliteMethod & Osd.HL_LABELCOLOR) && label))
		{
			label.txt.changeColor(Osd.COLOR_UNSELECTED);
		}
		if (hr)
		{
			hr.finalize();
			hr = null;
		}
	}

	public void press()
	{
		pressed = 1;
		if (((highliteMethod & Osd.HL_LABELCOLOR) && label))
		{
			label.txt.changeColor(Osd.COLOR_PRESSED);
		}
		if (hr)
		{
			hr.finalize();
			hr = null;
		}
		if (((highliteMethod & Osd.HL_BACKLIGHT) && rect))
		{
			Vector3 local1 = new Vector3(rect.pos);
			local1.z -= 0.00049999997;
			hr = new Rectangle(
				rect.root,
				local1,
				new RectangleTemplate((rect.rt.width * Gadget.HIGHLIGHT_ACTIVE_SCALE), (rect.rt.height * Gadget.HIGHLIGHT_ACTIVE_SCALE), Gadget.HIGHLIGHT_NORMAL)
			);
		}
		osd.pressEffect();
	}

	public void release()
	{
		pressed = 0;
		if (((highliteMethod & Osd.HL_LABELCOLOR) && label))
		{
			label.txt.changeColor(Osd.COLOR_SELECTED);
		}
		if (hr)
		{
			hr.finalize();
			hr = null;
		}
		if (((highliteMethod & Osd.HL_BACKLIGHT) && rect))
		{
			Vector3 local1 = new Vector3(rect.pos);
			local1.z -= 0.00049999997;
			hr = new Rectangle(
				rect.root,
				local1,
				new RectangleTemplate((rect.rt.width * Gadget.HIGHLIGHT_NORMAL_SCALE), (rect.rt.height * Gadget.HIGHLIGHT_NORMAL_SCALE), Gadget.HIGHLIGHT_NORMAL)
			);
		}
		osd.pressEffectStop();
	}

	public void click()
	{
	}

	public void sendMessage()
	{
		if (handler)
		{
			int local1;
			if ((handler instanceof GameType))
			{
				MessagePort local2 = ((GameType) handler).mp;
				if (local2)
				{
					Event local3 = new Event(cmd, Event.F_GAD_CHANGE);
					local3.gadget = this;
					local2.putMessage(local3);
					local1 = 1;
				}
			}
			if ((!(local1)))
			{
				handler.osdCommand(cmd);
			}
		}
	}

	public Vector3 getHotSpot()
	{
		return phy.getPos();
	}

}
