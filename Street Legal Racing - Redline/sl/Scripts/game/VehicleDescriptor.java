package java.game;

import java.io.*;
import java.util.resource.*;

public class VehicleDescriptor
{
	int id;
	int colorIndex;
	float power = 1.0;
	float optical = 1.0;
	float tear = 1.0;
	float wear = 1.0;
	float stockPrestige;
	float fullPrestige;
	float stockQM = 16.0;
	float fullQM = 9.0;
	String vehicleName = "unknown";

	public void save(File arg0)
	{
		int local2 = 3;
		arg0.write(local2);
		if ((local2 >= 1))
		{
			arg0.write(new GameRef(id));
			arg0.write(colorIndex);
			arg0.write(power);
			arg0.write(optical);
			arg0.write(tear);
			arg0.write(wear);
			arg0.write(stockPrestige);
			arg0.write(fullPrestige);
		}
		if ((local2 >= 2))
		{
			arg0.write(vehicleName);
		}
		if ((local2 >= 3))
		{
			arg0.write(stockQM);
			arg0.write(fullQM);
		}
	}

	public VehicleDescriptor load(File arg0)
	{
		int local2;
		local2 = arg0.readInt();
		if ((local2 >= 1))
		{
			id = arg0.readResID();
			colorIndex = arg0.readInt();
			power = arg0.readFloat();
			optical = arg0.readFloat();
			tear = arg0.readFloat();
			wear = arg0.readFloat();
			stockPrestige = arg0.readFloat();
			fullPrestige = arg0.readFloat();
		}
		if ((local2 >= 2))
		{
			vehicleName = arg0.readString();
		}
		if ((local2 >= 3))
		{
			stockQM = arg0.readFloat();
			fullQM = arg0.readFloat();
		}
		return this;
	}

	public float estimatePrestige()
	{
		float local1 = (((power - 1.0) + (optical - 1.0)) * 0.5);
		if ((local1 < 0.0))
		{
			local1 = (stockPrestige * (1.0 - local1));
		} else
		{
			local1 = (stockPrestige + ((fullPrestige - stockPrestige) * local1));
		}
		local1 *= 0.01;
		return local1;
	}

	public float estimateQM()
	{
		float local1 = 1.0;
		if ((power >= 1.0))
		{
			local1 *= (stockQM + ((fullQM - stockQM) * (power - 1.0)));
		} else
		{
			local1 *= stockQM;
		}
		return local1;
	}

}
