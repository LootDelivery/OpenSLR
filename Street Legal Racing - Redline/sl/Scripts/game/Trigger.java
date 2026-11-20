package java.game;

import java.util.resource.*;

public class Trigger
{
	static final int DEFAULT = system:0x34r;
	GameRef trigger;
	Navigator nav;
	Marker marker;

	public Trigger(GameRef arg0, GameRef arg1, Vector3 arg2, String arg3)
	{
		this(arg0, arg1, arg2, 20.0, arg3);
	}

	public Trigger(GameRef arg0, GameRef arg1, Vector3 arg2, float arg3, String arg4)
	{
		super();
		if ((arg1 == null))
		{
			arg1 = new GameRef(Trigger.DEFAULT);
		}
		trigger = new GameRef(arg0, arg1.id(), ((arg2.toString() + ",0,0,0,sphere,") + arg3), arg4);
	}

	public Trigger(GameRef arg0, GameRef arg1, Vector3 arg2, float arg3, float arg4, float arg5, String arg6)
	{
		super();
		if ((arg1 == null))
		{
			arg1 = new GameRef(Trigger.DEFAULT);
		}
		trigger = new GameRef(
			arg0,
			arg1.id(),
			((((((arg2.toString() + ",0,0,0,box,") + arg3) + ",") + arg4) + ",") + arg5),
			arg6
		);
	}

	public void finalize()
	{
		if (trigger)
		{
			trigger.destroy();
			trigger = null;
		}
		if (nav)
		{
			nav.remMarker(marker);
		}
	}

}
