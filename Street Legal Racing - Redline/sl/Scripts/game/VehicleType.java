package java.game;

import java.util.*;

public class VehicleType extends GameType
{
	static final int VS_DEMO = 1;
	static final int VS_USED = 2;
	static final int VS_STOCK = 4;
	static final int VS_DRACE = 8;
	static final int VS_NRACE = 16;
	static final int VS_RRACE = 32;
	static final float qm_stock_Baiern_CoupeSport_2_5 = 14.8294;
	static final float qm_full_Baiern_CoupeSport_2_5 = 12.681601;
	static final float qm_stock_Baiern_CoupeSport_GT_III = 9.9572;
	static final float qm_full_Baiern_CoupeSport_GT_III = 8.688801;
	static final float qm_stock_Baiern_DevilSport = 12.5978;
	static final float qm_full_Baiern_DevilSport = 10.9210005;
	static final float qm_stock_Duhen_Racing_SunStrip_2_0_CDVC = 11.448;
	static final float qm_full_Duhen_Racing_SunStrip_2_0_CDVC = 10.139;
	static final float qm_stock_Duhen_SunStrip_1_5_DVC = 13.9432;
	static final float qm_full_Duhen_SunStrip_1_5_DVC = 11.1434;
	static final float qm_stock_Duhen_SunStrip_1_8_DVC = 14.067;
	static final float qm_full_Duhen_SunStrip_1_8_DVC = 11.3494;
	static final float qm_stock_Duhen_SunStrip_2_2_DVC = 13.5854;
	static final float qm_full_Duhen_SunStrip_2_2_DVC = 11.337;
	static final float qm_stock_Einvagen_110_GT = 14.312667;
	static final float qm_full_Einvagen_110_GT = 11.693833;
	static final float qm_stock_Einvagen_110_GTK = 13.670167;
	static final float qm_full_Einvagen_110_GTK = 11.741333;
	static final float qm_stock_Einvagen_140_GTA = 12.559667;
	static final float qm_full_Einvagen_140_GTA = 10.538167;
	static final float qm_stock_Emer_MotorSport_Nonus_GT2 = 10.1594;
	static final float qm_full_Emer_MotorSport_Nonus_GT2 = 8.704399;
	static final float qm_stock_Emer_Nonus_Street_GT = 13.632;
	static final float qm_full_Emer_Nonus_Street_GT = 11.133201;
	static final float qm_stock_Hauler_s_SuperDuty_500 = 12.919;
	static final float qm_full_Hauler_s_SuperDuty_500 = 8.17;
	static final float qm_stock_Hauler_s_SuperDuty_Extra_750 = 12.231;
	static final float qm_full_Hauler_s_SuperDuty_Extra_750 = 8.9984;
	static final float qm_stock_Ishima_Enula_WR_SuperTurizmo = 12.307857;
	static final float qm_full_Ishima_Enula_WR_SuperTurizmo = 10.31457;
	static final float qm_stock_Ishima_Enula_WRY = 15.145;
	static final float qm_full_Ishima_Enula_WRY = 11.860287;
	static final float qm_stock_Ishima_Enula_WRZ = 13.845286;
	static final float qm_full_Ishima_Enula_WRZ = 10.614;
	static final float qm_stock_MC_GT = 12.7918005;
	static final float qm_full_MC_GT = 9.588201;
	static final float qm_stock_MC_GT_B_series = 12.663601;
	static final float qm_full_MC_GT_B_series = 9.91;
	static final float qm_stock_MC_GT_Limited_Edition = 11.747;
	static final float qm_full_MC_GT_Limited_Edition = 10.6166;
	static final float qm_stock_Prime_DLH_500 = 11.484799;
	static final float qm_full_Prime_DLH_500 = 8.8484;
	static final float qm_stock_Shimutshibu_Focer_RC_200 = 15.549286;
	static final float qm_full_Shimutshibu_Focer_RC_200 = 12.308144;
	static final float qm_stock_Shimutshibu_Focer_RC_300 = 14.014286;
	static final float qm_full_Shimutshibu_Focer_RC_300 = 11.544428;
	static final float qm_stock_Shimutshibu_Focer_WRC = 12.9868555;
	static final float qm_full_Shimutshibu_Focer_WRC = 10.725714;
	Vector vtdarr = new Vector();
	float prevalence = 1.0;
	int vehicleSetMask;
	Vector preferredColorIndexes = new Vector();

	public void addColorIndex(int arg0)
	{
		preferredColorIndexes.addElement(new Integer(arg0));
	}

	public VehicleDescriptor getVehicleDescriptor(int arg0, float arg1)
	{
		VehicleModel local3 = getVehicleModel(arg0);
		VehicleDescriptor local4 = new VehicleDescriptor();
		local4.id = local3.id;
		local4.stockPrestige = local3.stockPrestige;
		local4.fullPrestige = local3.fullPrestige;
		local4.vehicleName = local3.vehicleName;
		local4.stockQM = local3.stockQM;
		local4.fullQM = local3.fullQM;
		Vector local5;
		if ((local3.exclusiveColors && local3.preferredColorIndexes.size()))
		{
			local5 = local3.preferredColorIndexes;
		} else
		{
			if ((local3.preferredColorIndexes.size() || preferredColorIndexes.size()))
			{
				int local6 = local3.preferredColorIndexes.size();
				int local7 = preferredColorIndexes.size();
				if ((((local6 + local7) * Math.random()) < local6))
				{
					local5 = local3.preferredColorIndexes;
				} else
				{
					local5 = preferredColorIndexes;
				}
			}
		}
		if (local5)
		{
			local4.colorIndex = local5.elementAt(((int) (Math.random() * local5.size()))).intValue();
		}
		if ((arg1 < 0))
		{
			local4.power = (local3.minPower + (Math.random() * (local3.maxPower - local3.minPower)));
			local4.optical = (local3.minOptical + (Math.random() * (local3.maxOptical - local3.minOptical)));
			local4.tear = (local3.minTear + (Math.random() * (local3.maxTear - local3.minTear)));
			local4.wear = (local3.minWear + (Math.random() * (local3.maxWear - local3.minWear)));
		} else
		{
			if ((arg1 > 1.0))
			{
				arg1 = 1.0;
			}
			local4.power = (local3.minPower + (arg1 * (local3.maxPower - local3.minPower)));
			local4.optical = (local3.minOptical + (arg1 * (local3.maxOptical - local3.minOptical)));
			local4.tear = (local3.minTear + (arg1 * (local3.maxTear - local3.minTear)));
			local4.wear = (local3.minWear + (arg1 * (local3.maxWear - local3.minWear)));
		}
		return local4;
	}

	private VehicleModel getVehicleModel(int arg0)
	{
		VehicleModel local2;
		float local3;
		float local4;
		int local5 = (vtdarr.size() - 1);
		while ((local5 >= 0))
		{
			local2 = vtdarr.elementAt(local5);
			if ((arg0 & local2.vehicleSetMask))
			{
				local3 += local2.prevalence;
			}
			local5--;
		}
		local3 *= Math.random();
		int local5_2 = (vtdarr.size() - 1);
		while ((local5_2 >= 0))
		{
			local2 = vtdarr.elementAt(local5_2);
			if ((arg0 & local2.vehicleSetMask))
			{
				local4 += local2.prevalence;
				if ((local4 > local3))
				{
					break;
				}
			}
			local5_2--;
		}
		return local2;
	}

	public void init()
	{
		int local1 = (vtdarr.size() - 1);
		while ((local1 >= 0))
		{
			vehicleSetMask |= vtdarr.elementAt(local1).vehicleSetMask;
			local1--;
		}
	}

}
