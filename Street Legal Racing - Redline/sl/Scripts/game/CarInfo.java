package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.game.parts.enginepart.block.*;
import java.game.parts.enginepart.slidingenginepart.reciprocatingenginepart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.util.*;
import java.util.resource.*;

public class CarInfo extends GameType implements GameState
{
	static final int RID_CAR_BG = frontend:0x95r;
	static final int RID_ENGINE_BG = frontend:0x87r;
	static final int RID_FINANCIAL_BG = frontend:0x97r;
	static final int RID_RECORDS_BG = frontend:0x96r;
	static final int CMD_CAR_PAGE = 1;
	static final int CMD_ENGINE_PAGE = 2;
	static final int CMD_FINANCIAL_PAGE = 3;
	static final int CMD_RECORDS_PAGE = 4;
	static final int CMD_EXIT = 5;
	static final int CMD_SCROLL_UP = 6;
	static final int CMD_SCROLL_DOWN = 7;
	static final float graphX = (-(0.205));
	static final float graphY = 0.395;
	static final float graphW = 1.02;
	static final float graphH = 0.85;
	float graphRPMMin = 0.0;
	float graphRPMMax = 10000.0;
	float graphHPMin = 0.0;
	float graphHPMax = 900.0;
	float graphTorqueMin = 0.0;
	float graphTorqueMax = 600.0;
	GameState parentState;
	Osd osd;
	Vehicle car;
	int carGroup;
	int engineGroup;
	int financialGroup;
	int recordsGroup;
	int actGroup;
	int firstPart;
	int nParts;
	Text[] partText = new Text[100];
	ResourceRef graphFont;

	public CarInfo(Vehicle arg0)
	{
		super();
		createNativeInstance();
		car = arg0;
	}

	public void page(int arg0)
	{
		if ((actGroup != arg0))
		{
			osd.hideGroup(carGroup);
			osd.hideGroup(engineGroup);
			osd.hideGroup(financialGroup);
			osd.hideGroup(recordsGroup);
			actGroup = arg0;
			osd.showGroup(actGroup);
		}
	}

	public void enter(GameState arg0)
	{
		parentState = arg0;
		osd = new Osd();
		osd.globalHandler = this;
		firstPart = 0;
		nParts = 0;
		createOSDObjects();
		osd.show();
		Input.cursor.enable(1);
		setEventMask(GameRef.EVENT_CURSOR);
	}

	public void exit(GameState arg0)
	{
		clearEventMask(GameRef.EVENT_ANY);
		Input.cursor.enable(0);
		osd.hide();
		deleteOSDObjects();
		parentState = null;
	}

	public int wearColor(float arg0)
	{
		int local2;
		if ((arg0 > 1.0))
		{
			arg0 = 1.0;
		}
		if ((arg0 < 0.0))
		{
			arg0 = 0.0;
		}
		if ((arg0 >= 0.5))
		{
			local2 = (((1.0 - arg0) * 2.0) * 255);
			local2 = (((local2 & 255) << 16) + -16711936);
		} else
		{
			local2 = ((arg0 * 2.0) * 255);
			local2 = (((local2 & 255) << 8) + -65536);
		}
		return local2;
	}

	public float listParts(Vehicle arg0, int arg1)
	{
		int local3;
		int local4;
		float local5;
		float local6;
		nParts = 0;
		if (arg0.iteratePartsInit())
		{
			Part local7;
			while ((local7 = arg0.iterateParts()))
			{
				float local8 = local7.currentPriceNoAttach();
				float local9 = local7.police_check_fine_value;
				if ((arg1 > 0))
				{
					arg1--;
				} else
				{
					if ((local3 < 100))
					{
						partText[local3++].changeText(local7.name);
						local5 = local7.getWear();
						local4 = (local5 * 100.0);
						partText[local3].changeColor(wearColor(local5));
						partText[local3++].changeText((local4 + "%"));
						local5 = local7.getTear();
						local4 = (local5 * 100.0);
						partText[local3].changeColor(wearColor(local5));
						partText[local3++].changeText((local4 + "%"));
						if ((local9 > 0.0))
						{
							local4 = local9;
							partText[local3++].changeText(("$" + local4));
						} else
						{
							partText[local3++].changeText("");
						}
						local4 = local8;
						partText[local3++].changeText(("$" + local4));
					}
				}
				local6 += local8;
				nParts++;
			}
		}
		while ((local3 < 100))
		{
			partText[local3++].changeText("");
		}
		return local6;
	}

	public void createButtons()
	{
		osd.createRectangle(1.01, (-(0.82)), 1.2, 0.22, (-(1)), new ResourceRef(frontend:0x24r));
		Style local1 = new Style(0.120000005, 0.120000005, Frontend.smallFont, Text.ALIGN_LEFT, null);
		Menu local2 = osd.createMenu(local1, 0.45000002, (-(0.82)), 0, Osd.MD_HORIZONTAL);
		local2.addItem(new ResourceRef(frontend:0x124r), CarInfo.CMD_CAR_PAGE, "Main car information");
		local2.addItem(new ResourceRef(frontend:0xb5r), CarInfo.CMD_ENGINE_PAGE, "Engine information");
		local2.addItem(
			new ResourceRef(frontend:0xb4r),
			CarInfo.CMD_FINANCIAL_PAGE,
			"Detailed parts information"
		);
		local2.addItem(new ResourceRef(frontend:0x123r), CarInfo.CMD_RECORDS_PAGE, "Records");
		local2.addSeparator();
		local2.addSeparator();
		local2.addItem(new ResourceRef(Osd.RID_BACK), CarInfo.CMD_EXIT, null);
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), CarInfo.CMD_EXIT, this);
	}

	public String driveType()
	{
		int local1 = car.getInfo(52);
		if ((local1 == 0))
		{
			return "No wheel drive";
		} else
		{
			if ((local1 == 1))
			{
				return "All wheel drive";
			} else
			{
				if ((local1 == 2))
				{
					return "Front wheel drive";
				} else
				{
					if ((local1 == 3))
					{
						return "Rear wheel drive";
					} else
					{
						if ((local1 == 4))
						{
							return "Cross wheel drive";
						}
					}
				}
			}
		}
		return "?";
	}

	public String exists(int arg0)
	{
		if ((arg0 == 0))
		{
			return "Not exists";
		}
		return "Exists";
	}

	public void createOSDObjects()
	{
		int local1;
		int local2;
		int local3;
		float local4;
		float local5;
		float local6;
		float local7;
		float local8;
		float local9;
		float local10;
		String local11;
		graphFont = Frontend.smallFont;
		float local12 = ((1.2 * osd.createText("", graphFont, Text.ALIGN_LEFT, 0, 0).getFontSize(graphFont)) / (Config.video_y * osd.getViewport().getHeight()));
		Chassis local13 = car.chassis;
		osd.createBG(new ResourceRef(CarInfo.RID_CAR_BG));
		osd.createHeader("CAR INFORMATION");
		createButtons();
		if (local13)
		{
			local11 = local13.name.token(0);
		} else
		{
			local11 = "undefined";
		}
		Vector3 local14 = local13.getCM();
		Vector3 local15 = local13.getMin();
		Vector3 local16 = local13.getMax();
		int local17 = local13.getWheels();
		Vector3[] local18 = new Vector3[local17];
		float[] local19 = new float[local17];
		int local20 = 0;
		while ((local20 < local17))
		{
			WheelRef local21 = local13.getWheel(local20);
			local18[local20] = local21.getPos();
			local19[local20] = local21.getRadius();
			local20++;
		}
		local4 = (-(0.95));
		local5 = (-(0.55));
		local7 = (-(0.53000003));
		osd.createText(local13.vehicleName, Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		local3++;
		local3++;
		local8 = car.chassis.getMileage();
		osd.createText("Mileage:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			(((int) (local8 * 0.01)) + " km"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3++
		);
		osd.createText(
			(((int) (local8 * 0.0062100003)) + " mi"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3++
		);
		local2 = local13.getMass();
		osd.createText("Mass:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText((local2 + " kg"), Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
		local3++;
		osd.createText(
			Float.toString((local2 * 2.2), "%1.0f pounds"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		local3++;
		osd.createText("Chassis length:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			Float.toString(((local16.z - local15.z) * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText(
			Float.toString((((local16.z - local15.z) * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText("Chassis width:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			Float.toString(((local16.x - local15.x) * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText(
			Float.toString((((local16.x - local15.x) * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText("Chassis height:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			Float.toString(((local16.y - local15.y) * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText(
			Float.toString((((local16.y - local15.y) * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText("Ground clearance:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			Float.toString((((local19[0] - local18[0].y) + local15.y) * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText(
			Float.toString(((((local19[0] - local18[0].y) + local15.y) * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local5,
			local7,
			local3
		);
		local3++;
		local3++;
		osd.createText("Powertrain layout:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		local3++;
		osd.createText(("   " + driveType()), Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		local3++;
		local3++;
		local8 = (local18[1].x - local18[0].x);
		osd.createText("Front Track", Frontend.smallFont, Text.ALIGN_CENTER, 0.08, (-(0.15)), 0);
		osd.createText(
			Float.toString((local8 * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.08,
			(-(0.15)),
			1
		);
		osd.createText(
			Float.toString(((local8 * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.08,
			(-(0.15)),
			2
		);
		local8 = (local18[3].x - local18[2].x);
		osd.createText("Rear Track", Frontend.smallFont, Text.ALIGN_CENTER, 0.68, (-(0.15)), 0);
		osd.createText(
			Float.toString((local8 * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.68,
			(-(0.15)),
			1
		);
		osd.createText(
			Float.toString(((local8 * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.68,
			(-(0.15)),
			2
		);
		local8 = (((local18[2].z + local18[3].z) * 0.5) - ((local18[0].z + local18[1].z) * 0.5));
		osd.createText("Weight distribution", Frontend.smallFont, Text.ALIGN_CENTER, 0.35, 0.5, 0);
		osd.createText("Wheelbase", Frontend.smallFont, Text.ALIGN_CENTER, 0.35, 0.5, 1);
		osd.createText(
			Float.toString((local8 * 1000.0), "%1.0f mm"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.35,
			0.5,
			2
		);
		osd.createText(
			Float.toString(((local8 * 100.0) / 2.54), "%1.1f inch"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.35,
			0.5,
			3
		);
		float local20_2 = (((local18[0].z + local18[1].z) * 0.5) - local14.z);
		float local21_2 = (((local18[2].z + local18[3].z) * 0.5) - local14.z);
		local8 = ((-(local20_2)) / (local21_2 - local20_2));
		osd.createText(
			Float.toString(((1.0 - local8) * 100.0), "%1.1f%%"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.07,
			0.5
		);
		osd.createText(
			Float.toString((local8 * 100.0), "%1.1f%%"),
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.65000004,
			0.5
		);
		osd.hideGroup((carGroup = osd.endGroup()));
		osd.createBG(new ResourceRef(CarInfo.RID_ENGINE_BG));
		osd.createHeader("ENGINE INFORMATION");
		createButtons();
		int local22;
		Block local23;
		int local24 = 0;
		int local25 = 0;
		int local26 = 0;
		int local27 = 0;
		local7 = (-(0.53000003));
		local3 = 0;
		if (car.iteratePartsInit())
		{
			Part local28;
			while ((local28 = car.iterateParts()))
			{
				if ((local28 instanceof Block))
				{
					local23 = local28;
					if ((local28 instanceof Block_Vee))
					{
						local24 = 1;
					}
				} else
				{
					if ((local28 instanceof Crankshaft))
					{
						local25 = 1;
					} else
					{
						if ((local28 instanceof Piston))
						{
							local26 = 1;
						} else
						{
							if ((local28 instanceof CylinderHead))
							{
								local27++;
							}
						}
					}
				}
			}
		}
		int local28_2 = 0;
		if ((local24 && (local27 == 2)))
		{
			local28_2 = 1;
		} else
		{
			if (((!(local24)) && (local27 == 1)))
			{
				local28_2 = 1;
			}
		}
		if ((local28_2 && local26))
		{
			local28_2 = 1;
		} else
		{
			local28_2 = 0;
		}
		String local29 = null;
		DynoData local30 = null;
		if (local23)
		{
			if (car.iteratePartsInit())
			{
				Part local31;
				while ((local31 = car.iterateParts()))
				{
					local29 = local31.isDynoable();
					if (local29)
					{
						break;
					}
				}
			}
			local30 = local23.dynodata;
			local2 = local30.cylinders;
			osd.createText("Cylinders:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText(local2, Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			local3++;
			local2 = (local30.Displacement * 1000000.0);
			osd.createText("Displacement:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText((local2 + " cc "), Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			local3++;
			osd.createText("Bore:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText(
				Float.toString((local30.bore * 1000.0), "%1.1f mm"),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local5,
				local7,
				local3
			);
			local3++;
			osd.createText("Stroke:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			if (local25)
			{
				osd.createText(
					Float.toString((local30.stroke * 1000.0), "%1.1f mm"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			} else
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			}
			local3++;
			osd.createText("Static comp.:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			if (local28_2)
			{
				osd.createText(
					Float.toString(local30.Compression, "%1.1f:1"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			} else
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			}
			local3++;
			osd.createText("HP/liter:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			if (local29)
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			} else
			{
				osd.createText(
					Float.toString((local30.maxHP / (local30.Displacement * 1000.0)), "%1.1f"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			}
			local3++;
			osd.createText("kg/HP:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			if (local29)
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			} else
			{
				osd.createText(
					Float.toString((local13.getMass() / local30.maxHP), "%1.3f"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			}
			local3++;
			osd.createText("kg/Nm:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			if (local29)
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			} else
			{
				osd.createText(
					Float.toString(((local13.getMass() / local30.maxTorque) / 10.0), "%1.3f"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			}
			local3++;
			local3++;
			osd.createText("Gears:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			local3++;
			String[] local31_2 = new String[6];
			local31_2[0] = " 1st:";
			local31_2[1] = " 2nd:";
			local31_2[2] = " 3rd:";
			local31_2[3] = " 4th:";
			local31_2[4] = " 5th:";
			local31_2[5] = " 6th:";
			local1 = 1;
			while ((local1 <= local13.gears))
			{
				osd.createText(
					local31_2[(local1 - 1)],
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local4,
					local7,
					local3
				);
				osd.createText(
					local13.ratio[local1],
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
				local3++;
				local1++;
			}
			osd.createText(" Reverse:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText((-(local13.ratio[7])), Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			local3++;
			osd.createText(" End ratio:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText(
				(local13.rearend_ratio + ":1"),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local5,
				local7,
				local3
			);
			local3++;
			local3++;
			local2 = local13.engine_rpm_idle;
			osd.createText("Idle:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText((local2 + " RPM"), Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			local3++;
			local2 = local13.RPM_limit;
			osd.createText("Redline:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText((local2 + " RPM"), Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			local3++;
			local2 = local30.maxRPM;
			osd.createText("Engine destruction:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			osd.createText((local2 + " RPM"), Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			local3++;
			osd.createText(
				"Peak flywheel torque:",
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local4,
				local7,
				local3
			);
			if (local29)
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
				local3++;
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			} else
			{
				osd.createText(
					Float.toString((local30.maxTorque * 0.7353), "%1.0f ft-lbs"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
				local3++;
				osd.createText(
					Float.toString(local30.maxTorque, "%1.0f Nm"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			}
			local2 = local30.RPM_maxTorque;
			if ((!(local29)))
			{
				osd.createText(
					((" at " + local2) + " RPM"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local4,
					local7,
					local3
				);
			}
			local3++;
			osd.createText("Peak flywheel power:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			if (local29)
			{
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
				local3++;
				osd.createText("N/A", Frontend.smallFont, Text.ALIGN_LEFT, local5, local7, local3);
			} else
			{
				osd.createText(
					Float.toString(local30.maxHP, "%1.0f HP"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
				local3++;
				osd.createText(
					Float.toString((local30.maxHP * 0.73550004), "%1.0f KW"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local5,
					local7,
					local3
				);
			}
			local2 = local30.RPM_maxHP;
			if ((!(local29)))
			{
				osd.createText(
					((" at " + local2) + " RPM"),
					Frontend.smallFont,
					Text.ALIGN_LEFT,
					local4,
					local7,
					local3
				);
			}
			local3++;
			local3++;
			osd.createText("Fuel type:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
			local3++;
			osd.createText(
				(" " + local30.fuelType),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				local4,
				local7,
				local3
			);
			local3++;
		} else
		{
			osd.createText("No engine installed!", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		}
		if (local29)
		{
			osd.createTextBox(
				("No dyno info is available, because " + local29),
				Frontend.smallFont,
				Text.ALIGN_LEFT,
				CarInfo.graphX,
				local7,
				CarInfo.graphW
			);
		} else
		{
			if (local30)
			{
				graphRPMMin = 0.0;
				graphRPMMax = 5000.0;
				graphHPMin = 0.0;
				graphHPMax = 150.0;
				graphTorqueMin = 0.0;
				graphTorqueMax = 150.0;
				while ((graphRPMMax < local30.RPM_limit))
				{
					graphRPMMax += 5000;
				}
				while ((graphHPMax < local30.maxHP))
				{
					graphHPMax *= 2;
				}
				while ((graphTorqueMax < (local30.maxTorque * 0.7376)))
				{
					graphTorqueMax *= 2;
				}
				local1 = 1;
				while ((local1 <= 6))
				{
					local9 = (CarInfo.graphX + 0.030000001);
					local10 = ((CarInfo.graphY - (local1 * (CarInfo.graphH / 6.0))) - local12);
					local2 = (local1 * (graphTorqueMax / 6.0));
					osd.createText(local2, graphFont, Text.ALIGN_LEFT, local9, local10).changeColor(-1);
					local9 = ((CarInfo.graphX + CarInfo.graphW) + 0.030000001);
					local2 = (local1 * (graphHPMax / 6.0));
					osd.createText(local2, graphFont, Text.ALIGN_LEFT, local9, local10).changeColor(-1);
					local1++;
				}
				local1 = 0;
				while ((local1 <= 10))
				{
					local9 = ((CarInfo.graphX + (local1 * (CarInfo.graphW / 9.8))) - 0.030000001);
					local10 = (CarInfo.graphY + local12);
					local2 = (local1 * (graphRPMMax / 10.0));
					osd.createText(local2, graphFont, Text.ALIGN_LEFT, local9, local10).changeColor(-1);
					local1 += 2;
				}
				float local31_3;
				float local32;
				float local33;
				local33 = graphRPMMin;
				while ((local33 <= graphRPMMax))
				{
					local32 = ((local30.getHP(local33, 0) * 0.001) * 1.341);
					local9 = (CarInfo.graphX + (((local33 - graphRPMMin) / (graphRPMMax - graphRPMMin)) * CarInfo.graphW));
					local10 = ((CarInfo.graphY - (((local32 - graphHPMin) / (graphHPMax - graphHPMin)) * CarInfo.graphH)) - local12);
					osd.createText("-", graphFont, Text.ALIGN_LEFT, local9, local10).changeColor(-8355585);
					local33 += 50.0;
				}
				local33 = graphRPMMin;
				while ((local33 <= graphRPMMax))
				{
					local31_3 = (local30.getTorque(local33, 0.0) * 0.7376);
					local9 = (CarInfo.graphX + (((local33 - graphRPMMin) / (graphRPMMax - graphRPMMin)) * CarInfo.graphW));
					local10 = ((CarInfo.graphY - (((local31_3 - graphTorqueMin) / (graphTorqueMax - graphTorqueMin)) * CarInfo.graphH)) - local12);
					osd.createText("-", graphFont, Text.ALIGN_LEFT, local9, local10).changeColor(-32640);
					local33 += 50.0;
				}
			}
		}
		osd.hideGroup((engineGroup = osd.endGroup()));
		osd.createBG(new ResourceRef(CarInfo.RID_FINANCIAL_BG));
		osd.createHeader("FINANCIAL INFORMATION");
		createButtons();
		local7 = (-(0.6));
		osd.createText("Name", Frontend.smallFont, Text.ALIGN_LEFT, (-(0.90000004)), local7);
		osd.createText("Wear", Frontend.smallFont, Text.ALIGN_LEFT, 0.08, local7);
		osd.createText("Tear", Frontend.smallFont, Text.ALIGN_LEFT, 0.26, local7);
		osd.createText("IP fine", Frontend.smallFont, Text.ALIGN_LEFT, 0.39000002, local7);
		osd.createText("Value", Frontend.smallFont, Text.ALIGN_LEFT, 0.57, local7);
		local7 += 0.1;
		local1 = 0;
		while ((local1 < 100))
		{
			partText[local1] = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, (-(0.90000004)), local7);
			partText[local1].changeColor(-4144960);
			local1++;
			partText[local1] = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, 0.08, local7);
			partText[local1].changeColor(-4144960);
			local1++;
			partText[local1] = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, 0.26, local7);
			partText[local1].changeColor(-4144960);
			local1++;
			partText[local1] = osd.createText("", Frontend.smallFont, Text.ALIGN_RIGHT, 0.49, local7);
			partText[local1].changeColor(-57312);
			local1++;
			partText[local1] = osd.createText("", Frontend.smallFont, Text.ALIGN_RIGHT, 0.67, local7);
			partText[local1].changeColor(-4144960);
			local1++;
			local7 += 0.05;
		}
		int local31_4 = listParts(car, 0);
		local7 += 0.05;
		osd.createText("Total value", Frontend.smallFont, Text.ALIGN_LEFT, (-(0.90000004)), local7);
		osd.createText(("$" + local31_4), Frontend.smallFont, Text.ALIGN_RIGHT, 0.6, local7);
		Style local32_2 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWUP)
		);
		osd.createButton(local32_2, 0.8, (-(0.45000002)), CarInfo.CMD_SCROLL_UP, null);
		Style local33_2 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWDN)
		);
		osd.createButton(local33_2, 0.8, 0.45000002, CarInfo.CMD_SCROLL_DOWN, null);
		osd.hideGroup((financialGroup = osd.endGroup()));
		osd.createBG(new ResourceRef(CarInfo.RID_RECORDS_BG));
		osd.createHeader("RECORDS INFORMATION");
		createButtons();
		local7 = (-(0.64));
		local3 = 0;
		local5 = 0.25;
		local6 = (-(0.1));
		osd.createText(
			(("Statistics for this " + local13.vehicleName) + ":"),
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local4,
			local7,
			local3
		);
		local3++;
		local3++;
		osd.createText("Test Track best results:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		local3++;
		osd.createText(" Top speed:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.maxTestTrackSpeedSq < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				Float.toString(((Math.sqrt(car.maxTestTrackSpeedSq) * 2.24) * 1.61), "%1.1f KPH"),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local6,
				local7,
				local3
			);
			osd.createText(
				Float.toString((Math.sqrt(car.maxTestTrackSpeedSq) * 2.24), "%1.1f MPH"),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(" 0-100 KPH (0-62.1 MPH):", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.bestTestTrackAcc < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				String.timeToString(car.bestTestTrackAcc, String.TCF_NOMINUTES),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(
			" 0-200 KPH (0-124.2 MPH):",
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local4,
			local7,
			local3
		);
		if ((car.bestTestTrackAcc120 < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				String.timeToString(car.bestTestTrackAcc120, String.TCF_NOMINUTES),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(" 1/4 mile time:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.bestTestTrackTime2 < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				String.timeToString(car.bestTestTrackTime2, String.TCF_NOMINUTES),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(" 1/4 mile speed:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.bestTestTrackTime2_speedSq < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				Float.toString(((Math.sqrt(car.bestTestTrackTime2_speedSq) * 2.24) * 1.61), "%1.1f KPH"),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local6,
				local7,
				local3
			);
			osd.createText(
				Float.toString((Math.sqrt(car.bestTestTrackTime2_speedSq) * 2.24), "%1.1f MPH"),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(" 1 mile time:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.bestTestTrackTime0 < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				String.timeToString(car.bestTestTrackTime0, String.TCF_NOMINUTES),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(" 1 mile speed:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.bestTestTrackTime0_speedSq < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				Float.toString(((Math.sqrt(car.bestTestTrackTime0_speedSq) * 2.24) * 1.61), "%1.1f KPH"),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local6,
				local7,
				local3
			);
			osd.createText(
				Float.toString((Math.sqrt(car.bestTestTrackTime0_speedSq) * 2.24), "%1.1f MPH"),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.createText(" Best lap time:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		if ((car.bestTestTrackTime1 < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				String.timeToString(car.bestTestTrackTime1, String.TCF_NOHOURS),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local7 += 0.1;
		osd.createText(
			"Your race history for this car:",
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local4,
			local7,
			local3
		);
		local3++;
		osd.createText(" Races run:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			(car.races_won + car.races_lost),
			Frontend.smallFont,
			Text.ALIGN_RIGHT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText(" Wins:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(car.races_won, Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		local3++;
		osd.createText(" Loses:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(car.races_lost, Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		local3++;
		osd.createText(" History prestige ratio:", Frontend.smallFont, Text.ALIGN_LEFT, local4, local7, local3);
		osd.createText(
			Float.toString((car.getPrestigeMultiplier() * 100.0), "%1.1f %%"),
			Frontend.smallFont,
			Text.ALIGN_RIGHT,
			local5,
			local7,
			local3
		);
		local3++;
		osd.createText(
			" Best night 1/4 mile time:",
			Frontend.smallFont,
			Text.ALIGN_LEFT,
			local4,
			local7,
			local3
		);
		if ((car.bestNightQM < 0.1))
		{
			osd.createText("Not set", Frontend.smallFont, Text.ALIGN_RIGHT, local5, local7, local3);
		} else
		{
			osd.createText(
				String.timeToString(car.bestNightQM, String.TCF_NOMINUTES),
				Frontend.smallFont,
				Text.ALIGN_RIGHT,
				local5,
				local7,
				local3
			);
		}
		local3++;
		osd.hideGroup((recordsGroup = osd.endGroup()));
		actGroup = (-(1));
		page(carGroup);
	}

	public void deleteOSDObjects()
	{
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 < 0))
		{
			return;
		} else
		{
			if ((arg0 == CarInfo.CMD_CAR_PAGE))
			{
				page(carGroup);
			} else
			{
				if ((arg0 == CarInfo.CMD_ENGINE_PAGE))
				{
					page(engineGroup);
				} else
				{
					if ((arg0 == CarInfo.CMD_FINANCIAL_PAGE))
					{
						page(financialGroup);
					} else
					{
						if ((arg0 == CarInfo.CMD_RECORDS_PAGE))
						{
							page(recordsGroup);
						} else
						{
							if ((arg0 == CarInfo.CMD_EXIT))
							{
								GameLogic.changeActiveSection(parentState);
							} else
							{
								if ((arg0 == CarInfo.CMD_SCROLL_UP))
								{
									if ((firstPart > 0))
									{
										firstPart -= 20;
										if ((firstPart < 0))
										{
											firstPart = 0;
										}
										listParts(car, firstPart);
									}
								} else
								{
									if ((arg0 == CarInfo.CMD_SCROLL_DOWN))
									{
										if ((firstPart < (nParts - 20)))
										{
											firstPart += 20;
											if ((firstPart > (nParts - 20)))
											{
												firstPart = (nParts - 20);
											}
											listParts(car, firstPart);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}
