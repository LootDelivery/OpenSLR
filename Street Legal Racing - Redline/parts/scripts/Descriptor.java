package java.game.parts;

public class Descriptor
{
	int color;
	float optical;
	float power;
	float wear;
	float tear;

	public Descriptor(int arg0, float arg1, float arg2, float arg3, float arg4)
	{
		super();
		color = arg0;
		optical = arg1;
		power = arg2;
		wear = arg3;
		tear = arg4;
	}

	public Descriptor(Descriptor arg0)
	{
		super();
		color = arg0.color;
		optical = arg0.optical;
		power = arg0.power;
		wear = arg0.wear;
		tear = arg0.tear;
	}

}
