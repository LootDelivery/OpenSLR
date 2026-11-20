package java.game.parts;

import java.game.parts.bodypart.*;

public class EnginePart extends Part
{
	Chassis the_car = null;

	public EnginePart(int arg0)
	{
		super(arg0);
	}

	public void getCar_LocalVersion()
	{
		Part local1 = getCarRef();
		if ((local1 && (local1 instanceof Chassis)))
		{
			the_car = local1;
		} else
		{
			the_car = null;
		}
	}

	public float sparkAngleTo4cycleTime(float arg0)
	{
		return (arg0 / 720.0);
	}

	public void fillDynoData(DynoData arg0, int arg1)
	{
		Part local3 = partOnSlot(arg1);
		if ((local3 instanceof EnginePart))
		{
			the_car = ((EnginePart) local3).the_car;
		} else
		{
			getCar_LocalVersion();
		}
		super.fillDynoData(arg0, arg1);
	}

	public String isDynoable()
	{
		return null;
	}

	public String isDriveable()
	{
		String local1 = isDynoable();
		if (local1)
		{
			return ("the engine is not in working condition. " + local1);
		}
		return super.isDriveable();
	}

}
