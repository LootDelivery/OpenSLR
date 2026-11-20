package java.game;

import java.util.resource.*;

public class Decal extends ResourceRef
{
	int stickies = 5;

	public Decal(String arg0)
	{
		super();
		makeTexture(new ResourceRef(system:0x8r), arg0);
	}

	public Decal(int arg0)
	{
		super(arg0);
	}

}
