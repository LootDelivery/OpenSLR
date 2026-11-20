package java.game.parts;

import java.game.parts.bodypart.*;

public class RGearPart extends Part
{
	Chassis the_car = null;
	String name_prefix = "SL Tuners";
	float brand_prestige_factor = 1.0;

	public RGearPart(int arg0)
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
		if ((local3 instanceof RGearPart))
		{
			the_car = ((RGearPart) local3).the_car;
		} else
		{
			getCar_LocalVersion();
		}
		super.fillDynoData(arg0, arg1);
	}

}
