package java.render.osd;

import java.render.*;
import java.util.resource.*;

public class Button extends Gadget
{
	public Button(Osd arg0, Rectangle arg1, PhysicsRef arg2, int arg3, Object arg4)
	{
		super(arg0, arg2, arg1, arg3, arg4);
	}

	public void drop(GameRef arg0)
	{
		osd.dropObject = arg0;
		sendMessage();
		osd.dropObject = null;
	}

	public void drop(Gadget arg0)
	{
		osd.dropGadget = arg0;
		sendMessage();
		osd.dropGadget = null;
	}

	public void click()
	{
		sendMessage();
	}

}
