package java.game;

import java.util.resource.*;

public class Marker
{
	static final RenderRef RR_PLAYER = new RenderRef(frontend:0x5cr);
	static final RenderRef RR_CAR1 = new RenderRef(frontend:0x59r);
	static final RenderRef RR_CAR2 = new RenderRef(frontend:0x6br);
	static final RenderRef RR_CAR3 = new RenderRef(frontend:0x6cr);
	static final RenderRef RR_CAR4 = new RenderRef(frontend:0x6dr);
	static final RenderRef RR_POLICE = new RenderRef(frontend:0x5dr);
	static final RenderRef RR_START = new RenderRef(frontend:0x60r);
	static final RenderRef RR_FINISH = new RenderRef(frontend:0x5ar);
	static final RenderRef RR_GARAGE1 = new RenderRef(frontend:0x5br);
	static final RenderRef RR_GARAGE2 = new RenderRef(frontend:0x5fr);
	static final RenderRef RR_GARAGE3 = new RenderRef(frontend:0x73r);
	RenderRef symbol;

	public Marker(RenderRef arg0)
	{
		super();
		symbol = arg0;
	}

}
