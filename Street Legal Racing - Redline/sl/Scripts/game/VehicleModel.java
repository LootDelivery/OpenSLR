package java.game;

import java.util.*;

public class VehicleModel
{
	float prevalence = 1.0;
	int vehicleSetMask = (((((VehicleType.VS_DEMO | VehicleType.VS_STOCK) | VehicleType.VS_USED) | VehicleType.VS_DRACE) | VehicleType.VS_NRACE) | VehicleType.VS_RRACE);
	Vector preferredColorIndexes = new Vector();
	int exclusiveColors;
	int id;
	float minPower = 1.0;
	float maxPower = 1.5;
	float minOptical = 1.0;
	float maxOptical = 1.5;
	float minTear = 1.0;
	float maxTear = 1.0;
	float minWear = 1.0;
	float maxWear = 1.0;
	float stockPrestige = 0.0;
	float fullPrestige = 0.0;
	String vehicleName = "unknown";
	float stockQM = 16.0;
	float fullQM = 9.0;

	public VehicleModel(int arg0, int arg1)
	{
		super();
		id = arg0;
		vehicleSetMask = arg1;
	}

	public void addColorIndex(int arg0)
	{
		preferredColorIndexes.addElement(new Integer(arg0));
	}

}
