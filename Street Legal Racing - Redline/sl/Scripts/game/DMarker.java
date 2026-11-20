package java.game;

import java.util.resource.*;

public class DMarker extends Marker
{
	GameRef mover;

	public DMarker(RenderRef arg0, GameRef arg1)
	{
		super(arg0);
		mover = arg1;
	}

}
