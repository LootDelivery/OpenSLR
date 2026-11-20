package java.game;

import java.util.*;
import java.util.resource.*;

public class Round
{
	static final int GREEN_ARROW = frontend:0x70r;
	Vector tempObj = new Vector();
	Vector trigger = new Vector();
	Vector positions = new Vector();
	Track track;
	String messageA;
	String messageB;
	int type;
	int noAuto;
	Vector3 startPos;
	Ypr startOri;

	public Round(Track arg0, String arg1, String arg2, int arg3)
	{
		this(arg0, arg1, arg2);
		noAuto = arg3;
	}

	public Round(Track arg0, String arg1, String arg2)
	{
		super();
		track = arg0;
		messageA = arg1;
		messageB = arg2;
		type = 0;
	}

	public void startdir(float arg0)
	{
		startOri = new Ypr(arg0, 0.0, 0.0);
	}

	public void point(float arg0, float arg1, float arg2, int arg3)
	{
		point(arg0, arg1, arg2, arg3, 30.0);
	}

	public void point(float arg0, float arg1, float arg2, int arg3, float arg4)
	{
		RenderRef local6;
		Vector3 local7 = new Vector3(arg0, arg1, arg2);
		if ((trigger.size() == 0))
		{
			local6 = Marker.RR_START;
			startPos = local7;
		} else
		{
			local6 = Marker.RR_FINISH;
		}
		positions.addElement(local7);
		trigger.addElement(track.addTrigger(local7, null, local6, "event_handlerTrigger", arg4, "testrack trigger"));
		if (arg3)
		{
			RenderRef local8;
			local8 = new RenderRef(track.map, Round.GREEN_ARROW, "finishObject");
			local8.setMatrix(new Vector3(arg0, (arg1 + 3.0), arg2), null);
			tempObj.addElement(local8);
		}
	}

	public void point(float arg0, float arg1, float arg2, float arg3, int arg4, int arg5, Pori arg6)
	{
		RenderRef local8;
		if (arg4)
		{
			local8 = new RenderRef(arg4);
		} else
		{
			local8 = null;
		}
		Vector3 local9 = new Vector3(arg0, arg1, arg2);
		if ((trigger.size() == 0))
		{
			startPos = local9;
		}
		positions.addElement(local9);
		trigger.addElement(track.addTrigger(local9, null, local8, "event_handlerTrigger", arg3, "testrack trigger"));
		if (arg5)
		{
			RenderRef local10;
			local10 = new RenderRef(track.map, arg5, "Checkpoint 3d Object");
			if (arg6)
			{
				local10.setMatrix(
					new Vector3((arg6.pos.x + arg0), (arg6.pos.y + arg1), (arg6.pos.z + arg2)),
					arg6.getOri()
				);
			} else
			{
				local10.setMatrix(new Vector3(arg0, arg1, arg2), null);
			}
			tempObj.addElement(local10);
		}
	}

	public void loop()
	{
		trigger.addElement(trigger.elementAt(0));
		type = 1;
	}

	public Vector3 getPoint(int arg0)
	{
		return positions.elementAt(arg0);
	}

	public int indexOf(int arg0)
	{
		int local2 = 0;
		while ((local2 < trigger.size()))
		{
			if ((arg0 == trigger.elementAt(local2).trigger.id()))
			{
				return local2;
			}
			local2++;
		}
		return (-(1));
	}

	public int size()
	{
		return positions.size();
	}

	public void destroy()
	{
		int local1 = 0;
		while ((local1 < trigger.size()))
		{
			Trigger local2 = trigger.elementAt(local1);
			if (local2)
			{
				track.removeTrigger(local2);
			}
			local1++;
		}
		trigger.removeAllElements();
		int local1_2 = 0;
		while ((local1_2 < tempObj.size()))
		{
			RenderRef local2_2 = tempObj.elementAt(local1_2);
			if (local2_2)
			{
				local2_2.destroy();
			}
			local1_2++;
		}
		tempObj.removeAllElements();
		positions = null;
	}

}
