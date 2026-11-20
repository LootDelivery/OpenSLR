package java.game;

import java.io.*;
import java.util.resource.*;

public class Racer extends GameType
{
	static final int RID_FEJ = frontend:0xdar;
	static final float PRESTIGE_STEP = 0.049999997;
	static final int PRESTIGE_SCALE = 300;
	static final int VHC_PRESTIGE_SCALE = 100;
	Vehicle car;
	String name;
	RenderRef render;
	RenderRef driverID;
	ResourceRef character;
	float prestige;
	int money = 10000;
	int club;
	int lastRaceDay = (-(1));
	int enabledPinkSlips = 0;
	float bestQMtime = 0.0;
	float bestQMspd = 0.0;

	public Racer()
	{
		super();
		createNativeInstance();
	}

	public void increasePrestige(float arg0)
	{
		float local2 = ((1.0 - prestige) * arg0);
		float local3 = ((0.02 * arg0) / Racer.PRESTIGE_STEP);
		if ((local2 > local3))
		{
			local2 = local3;
		}
		prestige += local2;
	}

	public void decreasePrestige(float arg0)
	{
		float local2 = (prestige * arg0);
		float local3 = ((0.02 * arg0) / Racer.PRESTIGE_STEP);
		if ((local2 > local3))
		{
			local2 = local3;
		}
		prestige -= local2;
	}

	public float getCarPrestige()
	{
		if (car)
		{
			return car.getPrestige();
		}
		return 0.0;
	}

	public String getPrestigeString()
	{
		int local1;
		int local2;
		int local3;
		local1 = (prestige * Racer.PRESTIGE_SCALE);
		local2 = (getCarPrestige() * Racer.VHC_PRESTIGE_SCALE);
		local3 = ((local1 * 0.5) + (local2 * 0.5));
		return ((local1 + "/") + local2);
	}

	public void calcPrestige(Racer arg0, int arg1, int arg2)
	{
		if ((prestige != arg0.prestige))
		{
			if ((prestige < arg0.prestige))
			{
				increasePrestige(Racer.PRESTIGE_STEP);
				arg0.decreasePrestige(Racer.PRESTIGE_STEP);
			} else
			{
				decreasePrestige(Racer.PRESTIGE_STEP);
				arg0.increasePrestige(Racer.PRESTIGE_STEP);
			}
		}
		if (arg1)
		{
			arg0.decreasePrestige(Racer.PRESTIGE_STEP);
		} else
		{
			if (arg2)
			{
				increasePrestige((2 * Racer.PRESTIGE_STEP));
				arg0.decreasePrestige((2 * Racer.PRESTIGE_STEP));
			} else
			{
				decreasePrestige((2 * Racer.PRESTIGE_STEP));
				arg0.increasePrestige((2 * Racer.PRESTIGE_STEP));
			}
		}
	}

	public void save(File arg0)
	{
		int local2 = 1;
		arg0.write(local2);
		if ((local2 >= 1))
		{
		}
	}

	public void load(File arg0)
	{
		int local2;
		local2 = arg0.readInt();
		if ((local2 >= 1))
		{
		}
	}

}
