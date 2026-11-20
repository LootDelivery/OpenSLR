package java.game.parts;

import java.game.parts.bodypart.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;

public class Part extends GameType
{
	static final int COMMON = 0;
	static final int PACKAGE = 1;
	static float PRESTIGE_DOWNGRADE_MUL = 1.08;
	static float PRESTIGE_VALUE_MUL = 0.0121875;
	static final int MF_STOCK = 0;
	static final int MF_HOLLEY = parts:0x35r;
	static final int MF_HOLLEY2 = parts:0x35r;
	static final int MF_AUTHORITY = parts:0x36r;
	static final int MF_NEUSPEED = 0;
	static final int MF_BILLSTEIN = parts:0x33r;
	static final int MF_OZ = 0;
	static final int MF_BAER = parts:0x37r;
	static final int MF_BFGOODRICH = 0;
	static final int MF_BFGOODRICH2 = 0;
	static final int MF_BFGOODRICH3 = 0;
	static final int MF_BORLA = parts:0x3ar;
	static final int MF_NOS = parts:0x3br;
	static final int MF_HR = parts:0x3cr;
	static final int MF_JSP = parts:0x3dr;
	static final int MF_JSP2 = parts:0x3dr;
	static final int MF_MICHELIN = 0;
	static final int MF_MICHELIN2 = 0;
	static final int MF_PROCHARGER = parts:0x42r;
	static final int MF_PROCHARGER2 = parts:0x42r;
	static final int MF_KN = parts:0x44r;
	static final int SAVEFILEVERSION_PART = 1;
	static float seed;
	String name = "<unnamed>";
	String description = "<description not available yet>";
	float value = 100.0;
	float police_check_fine_value = 0.0;
	float thoroughness_limit = 0.0;
	float brand_new_prestige_value = 10.0;
	float prestige_calc_weight = 25.0;
	float price_catalog = 1.0;
	float price_shop = 0.90000004;
	float price_used = 0.7;
	float price_repair = 0.8;
	Ypr catalog_view_ypr = null;
	float repair_min_wear = 0.1;
	float repair_min_tear = 0.1;
	float repair_max_wear = 0.95000005;
	float repair_max_tear = 0.95000005;
	int carCategory = Part.COMMON;
	int manufacturer = Part.MF_STOCK;
	int need_update = 1;
	float drag_occluded = 0.0;
	float drag_own = 0.0;
	Vector3 drag_own_center = new Vector3(0, 0, 0);
	float C_drag = 0.0;
	Vector3 drag_center = new Vector3(0, 0, 0);
	Vector attachedParts = new Vector();
	float prev_prestige = 0.0;

	public static Part createFromFile(File arg0, GameRef arg1)
	{
		int local2 = arg0.readResID();
		if (local2)
		{
			int local3 = arg0.readInt();
			GameRef local4 = new GameRef();
			Part local5 = local4.create(arg1, new GameRef(local2), "100,0,0,0,0,0", "loaded carpart");
			local5.load(arg0);
			return local5;
		}
		return null;
	}

	public static void randomize(float arg0)
	{
		Part.seed = arg0;
		while ((Part.seed >= 1.0))
		{
			Part.seed -= 1.0;
		}
	}

	public static float random()
	{
		Part.seed += 0.09786573;
		Part.seed *= 1.3141592;
		while ((Part.seed >= 1.0))
		{
			Part.seed -= 1.0;
		}
		return Part.seed;
	}

	public Part(int arg0)
	{
		super();
	}

	public String installCheck(Part arg0, int[] arg1)
	{
		return null;
	}

	public String isDynoable()
	{
		return null;
	}

	public String isDriveable()
	{
		return null;
	}

	public int getInfo(int arg0)
	{
		return getInfo(arg0, 0);
	}

	public int isTuneable()
	{
		return 0;
	}

	public void load(File arg0)
	{
		setTexture(arg0.readResID());
		setMesh(arg0.readResID());
		setWear(arg0.readFloat());
		setTear(arg0.readFloat());
		String local2;
		int local3;
		int local4;
		int local5;
		while (((local3 = arg0.readInt()) != (-(1))))
		{
			local2 = arg0.readString();
			local5 = arg0.readInt();
			setSlotDamage(local3, local2);
			if (local5)
			{
				Part local6 = Part.createFromFile(arg0, getParent());
				local4 = getSlotID(local3);
				local6.command(((((((((("install " + 0) + " ") + id()) + " ") + local5) + " ") + id()) + " ") + local4));
			}
		}
	}

	public void save(File arg0)
	{
		arg0.write(new GameRef(getInfo(GameType.GII_TYPE)));
		arg0.write(Part.SAVEFILEVERSION_PART);
		int local2 = getTexture();
		ResourceRef local3 = new ResourceRef(local2);
		arg0.write(local3);
		int local4 = getMesh();
		ResourceRef local5 = new ResourceRef(local4);
		arg0.write(local5);
		arg0.write(getWear());
		arg0.write(getTear());
		int local6;
		int local7;
		int local8;
		int local9;
		int local10;
		Part local11;
		String local12;
		local6 = getSlots();
		local10 = getSlotID((-(1)));
		local7 = 0;
		while ((local7 < local6))
		{
			local8 = getSlotID(local7);
			if ((local8 != local10))
			{
				local12 = getSlotDamage(local7);
				local9 = slotIDOnSlot(local8);
				if (((local12 != null) || local9))
				{
					arg0.write(local7);
					arg0.write(local12);
					arg0.write(local9);
					if (local9)
					{
						local11 = partOnSlot(local8);
						local11.save(arg0);
					}
				}
			}
			local7++;
		}
		arg0.write((-(1)));
	}

	public void addpart(GameRef arg0)
	{
		attachedParts.addElement(arg0.getScriptInstance());
		prev_prestige = 0.0;
	}

	public void rempart(GameRef arg0)
	{
		attachedParts.removeElement(arg0.getScriptInstance());
		prev_prestige = 0.0;
	}

	public int isStreetLegal()
	{
		return (police_check_fine_value == 0.0);
	}

	public int isComplex()
	{
		return attachedParts.size();
	}

	public float calcPoliceFine(float arg0)
	{
		if ((arg0 > thoroughness_limit))
		{
			return police_check_fine_value;
		}
		return 0.0;
	}

	public float calcPrestige(float arg0, float arg1)
	{
		float local3 = (((arg0 * (0.3 + (0.7 * arg1))) * brand_new_prestige_value) * prestige_calc_weight);
		local3 *= Part.PRESTIGE_VALUE_MUL;
		return local3;
	}

	public float currentPrestige()
	{
		if ((prev_prestige <= 0.0))
		{
			Part local1;
			float local2 = 0.0;
			float local3 = 0.0;
			float local4 = 0.0;
			float local5 = 0.0;
			float local6 = 0.0;
			float local7 = 0.0;
			float local8 = 1.0;
			float local9 = 1.0;
			float local10 = 1.0;
			if ((this instanceof EnginePart))
			{
				local2 = calcPrestige(getTear(), getWear());
				local5 = prestige_calc_weight;
				local8 *= Part.PRESTIGE_DOWNGRADE_MUL;
			} else
			{
				if ((this instanceof BodyPart))
				{
					local3 = calcPrestige(getTear(), getWear());
					local6 = prestige_calc_weight;
					local9 *= Part.PRESTIGE_DOWNGRADE_MUL;
				} else
				{
					if ((this instanceof RGearPart))
					{
						local4 = calcPrestige(getTear(), getWear());
						local7 = prestige_calc_weight;
						local10 *= Part.PRESTIGE_DOWNGRADE_MUL;
					}
				}
			}
			int local11 = (attachedParts.size() - 1);
			while ((local11 >= 0))
			{
				local1 = attachedParts.elementAt(local11);
				if ((local1 instanceof EnginePart))
				{
					local2 += local1.calcPrestige(local1.getTear(), local1.getWear());
					local5 += local1.prestige_calc_weight;
					local8 *= Part.PRESTIGE_DOWNGRADE_MUL;
				} else
				{
					if ((local1 instanceof BodyPart))
					{
						local3 += local1.calcPrestige(local1.getTear(), local1.getWear());
						local6 += local1.prestige_calc_weight;
						local9 *= Part.PRESTIGE_DOWNGRADE_MUL;
					} else
					{
						if ((local1 instanceof RGearPart))
						{
							local4 += local1.calcPrestige(local1.getTear(), local1.getWear());
							local7 += local1.prestige_calc_weight;
							local10 *= Part.PRESTIGE_DOWNGRADE_MUL;
						}
					}
				}
				local11--;
			}
			local2 *= local8;
			local3 *= local9;
			local4 *= local10;
			float local11_2 = 0.0;
			float local12 = (-(1.0));
			float local13 = (-(1.0));
			float local14 = (-(1.0));
			if ((local5 > 0.0))
			{
				local12 = ((local2 / local5) * 0.3);
				local11_2 += local12;
			}
			if ((local6 > 0.0))
			{
				local13 = ((local3 / local6) * 0.5);
				local11_2 += local13;
			}
			if ((local7 > 0.0))
			{
				local14 = ((local4 / local7) * 0.2);
				local11_2 += local14;
			}
			prev_prestige = local11_2;
		}
		return prev_prestige;
	}

	public float currentPrestigeNoAttach()
	{
		return calcPrestige(getTear(), getWear());
	}

	public float calcPrice(float arg0, float arg1)
	{
		float local3 = ((arg0 * (0.3 + (0.7 * arg1))) * value);
		return local3;
	}

	public float getConditionNoAttach()
	{
		return (getWear() * getTear());
	}

	public float getCondition()
	{
		float local1 = (getWear() * getTear());
		int local2 = (attachedParts.size() - 1);
		while ((local2 >= 0))
		{
			local1 *= attachedParts.elementAt(local2).getCondition();
			local2--;
		}
		return local1;
	}

	public float currentPrice()
	{
		float local1 = calcPrice(getTear(), getWear());
		int local2 = (attachedParts.size() - 1);
		while ((local2 >= 0))
		{
			local1 += attachedParts.elementAt(local2).currentPrice();
			local2--;
		}
		return local1;
	}

	public float currentPriceNoAttach()
	{
		return calcPrice(getTear(), getWear());
	}

	public int isRepairable()
	{
		int local1 = (((getWear() > repair_min_wear) && (getTear() > repair_min_tear)) && ((repair_max_wear > 0.0) || (repair_max_tear > 0.0)));
		int local2 = (attachedParts.size() - 1);
		while ((local2 >= 0))
		{
			local1 = (local1 || attachedParts.elementAt(local2).isRepairable());
			local2--;
		}
		return local1;
	}

	public float repairCost()
	{
		float local1 = getTear();
		float local2 = getWear();
		float local3;
		if (((local1 > repair_min_tear) || (local2 > repair_min_wear)))
		{
			if ((local1 < repair_max_tear))
			{
				local1 = repair_max_tear;
			}
			if ((local2 < repair_max_wear))
			{
				local2 = repair_max_wear;
			}
			local3 = ((calcPrice(local1, local2) - calcPrice(getTear(), getWear())) * price_repair);
		}
		int local4 = (attachedParts.size() - 1);
		while ((local4 >= 0))
		{
			local3 += attachedParts.elementAt(local4).repairCost();
			local4--;
		}
		return local3;
	}

	public float getRealCondition(float arg0, float arg1, float arg2, float arg3, String arg4)
	{
		if ((arg2 < arg1))
		{
			float local6;
			local6 = arg2;
			arg2 = arg1;
			arg1 = local6;
		}
		float local6_2 = ((arg0 * arg1) + ((arg0 * (arg2 - arg1)) * arg3));
		return local6_2;
	}

	public float clampTo(float arg0, float arg1, float arg2)
	{
		if ((arg0 < arg1))
		{
			return arg1;
		} else
		{
			if ((arg0 > arg2))
			{
				return arg2;
			}
		}
		return arg0;
	}

	public float rollTo(float arg0, float arg1, float arg2)
	{
		float local4 = arg0;
		if ((local4 < arg1))
		{
			while ((local4 < arg1))
			{
				local4 += (arg2 - arg1);
			}
		} else
		{
			if ((local4 > arg2))
			{
				while ((local4 > arg2))
				{
					local4 -= (arg2 - arg1);
				}
			}
		}
		return local4;
	}

	public float HUF2USD(float arg0)
	{
		return (arg0 / 211.0);
	}

	public float tHUF2USD(float arg0)
	{
		return ((arg0 * 1000.0) / 211.0);
	}

	public float mHUF2USD(float arg0)
	{
		return ((arg0 * 1000000.0) / 211.0);
	}

	public float Inch2Meter(float arg0)
	{
		return ((arg0 * 25.4) / 1000.0);
	}

	public float kmToMaxWear(float arg0)
	{
		return (arg0 * 1000.0);
	}

	public void repair()
	{
		prev_prestige = 0.0;
		if ((getWear() < repair_max_wear))
		{
			setWear(repair_max_wear);
		}
		if ((getTear() < repair_max_tear))
		{
			setTear(repair_max_tear);
		}
		GameRef local1 = new GameRef();
		GameRef local2 = getParent();
		command("repair");
		int local3 = (attachedParts.size() - 1);
		while ((local3 >= 0))
		{
			attachedParts.elementAt(local3).repair();
			local3--;
		}
	}

	public void updatevariables()
	{
		need_update = 0;
	}

	public void updateDamage()
	{
	}

	public void install()
	{
	}

	public void remove()
	{
	}

	public void falloff()
	{
	}

	public native float getWear();

	public native float setWear(float arg0);

	public native void setMaxWear(float arg0);

	public native float getTear();

	public native float setTear(float arg0);

	public native int getTexture();

	public native int setTexture(int arg0);

	public native int getMesh();

	public native int setMesh(int arg0);

	public native int getRenderType();

	public native int setRenderType(int arg0);

	public native int[] install_OK(GameRef arg0, int arg1, GameRef arg2, int arg3, Vector3 arg4);

	public int getFlap()
	{
		return flap(0);
	}

	public void toggleFlap()
	{
		flap(1);
	}

	public native int flap(int arg0);

	public int getLogo()
	{
		return manufacturer;
	}

	public native int getCar();

	public native int getWheelID();

	public WheelRef getWheel()
	{
		Part local1 = getCarRef();
		if ((local1 instanceof Chassis))
		{
			return ((Chassis) local1).getWheel(getWheelID());
		}
		return null;
	}

	public native void disableSlot(int arg0, int arg1);

	public native String getSlotDamage(int arg0);

	public native void setSlotDamage(int arg0, String arg1);

	public native void setSlotPos(int arg0, Vector3 arg1, Ypr arg2);

	public native int getSlots();

	public native int getSlotID(int arg0);

	public native Part partOnSlot(int arg0);

	public native int slotIDOnSlot(int arg0);

	public native Part getCarRef();

	public void reCreate()
	{
	}

	public void addStockParts()
	{
		addStockParts(0, 1.0, 1.0);
	}

	public void addStockParts(int arg0, float arg1, float arg2)
	{
	}

	public void addStockParts(Descriptor arg0)
	{
		setWear(arg0.wear);
		setTear(arg0.tear);
		int local2 = arg0.color;
		setTexture(local2);
		addStockParts(arg0.color, arg0.optical, arg0.power);
	}

	public Part addPart(int arg0, String arg1)
	{
		return addPart(arg0, arg1, 0, 1.0, 1.0);
	}

	public Part addPart(int arg0, String arg1, int arg2, float arg3, float arg4)
	{
		return addPart(arg0, arg1, new Descriptor(arg2, arg3, arg4, 1.0, 1.0));
	}

	public Part addPart(int arg0, String arg1, Descriptor arg2)
	{
		int local4 = getCar();
		int local5;
		if ((!(local4)))
		{
			local4 = this.id();
		}
		GameRef local6 = new GameRef(local4);
		local5 = local6.getParentID();
		if ((!(local5)))
		{
			return null;
		}
		GameRef local7 = new GameRef();
		Part local8 = local7.create(new GameRef(local5), new GameRef(arg0), "", arg1);
		if (local8)
		{
			local8.setWear(arg2.wear);
			local8.setTear(arg2.tear);
			local7.command((((("install 0 " + local4) + " 0 ") + this.id()) + " 0"));
			int local9 = (local7.getParentID() != local5);
			if (local9)
			{
				local8.addStockParts(arg2);
				if (arg2.color)
				{
					local8.setTexture(arg2.color);
				}
			} else
			{
				local7.destroy();
			}
			return local8;
		}
		return null;
	}

	public void fillDynoData(DynoData arg0, int arg1)
	{
		int local3 = getSlots();
		while (local3--)
		{
			int local4 = getSlotID(local3);
			if ((local4 != arg1))
			{
				getDynoData(arg0, local4);
			}
		}
	}

	public int getDynoData(DynoData arg0, int arg1)
	{
		Part local3;
		if ((local3 = partOnSlot(arg1)))
		{
			local3.fillDynoData(arg0, slotIDOnSlot(arg1));
			return 1;
		}
		return 0;
	}

	public void check4warnings()
	{
	}

}
