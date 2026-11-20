package java.game.parts;

import java.game.parts.bodypart.*;

public class BodyPart extends Part
{
	Chassis the_car = null;
	float drag_reduction = 0.0;

	public BodyPart(int arg0)
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

	public void fillDynoData(DynoData arg0, int arg1)
	{
		Part local3 = partOnSlot(arg1);
		if ((local3 instanceof BodyPart))
		{
			the_car = ((BodyPart) local3).the_car;
		} else
		{
			getCar_LocalVersion();
		}
		super.fillDynoData(arg0, arg1);
	}

}
