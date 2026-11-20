package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.game.parts.enginepart.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;

public class Vehicle extends GameType
{
	static final int SAVEFILEID_VHC = -2006563806;
	static final int SAVEFILEVERSION_VHC = 5;
	static final int SAVEFILEID_SKIN = -2108385177;
	static final int SAVEFILEVERSION_SKIN = 0;
	static final int TRANSMISSION_MANUAL = 0;
	static final int TRANSMISSION_AUTO = 1;
	static final int TRANSMISSION_SEMIAUTO = 5;
	static final int TRANSMISSION_MANUALCLUTCH = 2;
	float maxTestTrackSpeedSq;
	float bestTestTrackAcc;
	float bestTestTrackAcc120;
	float bestTestTrackTime0;
	float bestTestTrackTime0_speedSq;
	float bestTestTrackTime1;
	float bestTestTrackTime2;
	float bestTestTrackTime2_speedSq;
	int races_won;
	int races_lost;
	float bestNightQM = 0.0;
	int cruiseControl;
	Chassis chassis;
	Vector iteratorStack;

	public static Vehicle load(String arg0, GameRef arg1)
	{
		Vehicle local2;
		File local3 = new File(arg0);
		if (local3.open(File.MODE_READ))
		{
			if ((local3.readInt() == Vehicle.SAVEFILEID_VHC))
			{
				if ((local3.readInt() == Vehicle.SAVEFILEVERSION_VHC))
				{
					Chassis local4 = Part.createFromFile(local3, arg1);
					local2 = new Vehicle(local4);
					local2.maxTestTrackSpeedSq = local3.readFloat();
					local2.bestTestTrackAcc = local3.readFloat();
					local2.bestTestTrackAcc120 = local3.readFloat();
					local2.bestTestTrackTime0 = local3.readFloat();
					local2.bestTestTrackTime0_speedSq = local3.readFloat();
					local2.bestTestTrackTime1 = local3.readFloat();
					local2.bestTestTrackTime2 = local3.readFloat();
					local2.bestTestTrackTime2_speedSq = local3.readFloat();
					local2.races_won = local3.readInt();
					local2.races_lost = local3.readInt();
					local2.bestNightQM = local3.readFloat();
					local2.wakeUp();
				}
			}
			local3.close();
		}
		return local2;
	}

	public static int fileCheck(String arg0)
	{
		int local1;
		File local2 = new File(arg0);
		if (local2.open(File.MODE_READ))
		{
			if ((local2.readInt() == Vehicle.SAVEFILEID_VHC))
			{
				if ((local2.readInt() == Vehicle.SAVEFILEVERSION_VHC))
				{
					local1 = 1;
				}
			}
			local2.close();
		}
		return local1;
	}

	public static int fileCheck_Skin(String arg0)
	{
		int local1;
		File local2 = new File(arg0);
		if (local2.open(File.MODE_READ))
		{
			if ((local2.readInt() == Vehicle.SAVEFILEID_SKIN))
			{
				if ((local2.readInt() == Vehicle.SAVEFILEVERSION_SKIN))
				{
					local1 = 1;
				}
			}
			local2.close();
		}
		return local1;
	}

	public Vehicle(Part arg0)
	{
		super();
		set(arg0);
		chassis = arg0;
		setDefaultTransmission();
		setDefaultSteeringHelp();
		setDefaultASR();
		setDefaultABS();
	}

	public float calcPoliceFine(float arg0)
	{
		if (chassis)
		{
			return chassis.calcPoliceFine(arg0);
		}
		return 0.0;
	}

	public Vehicle(GameRef arg0, int arg1, int arg2, float arg3, float arg4, float arg5, float arg6)
	{
		super();
		float local8 = System.currentTime();
		chassis = create(arg0, new GameRef(arg1), "0,0,0,0,0,0", "Vehicle");
		if (chassis)
		{
			chassis.suspend_update = 1;
			chassis.addStockParts(new Descriptor(GameLogic.CARCOLORS[arg2], arg3, arg4, arg5, arg6));
			chassis.suspend_update = 0;
			chassis.forceUpdate();
		}
		setDefaultTransmission();
		setDefaultSteeringHelp();
		setDefaultASR();
		setDefaultABS();
		setMatrix(null, null);
	}

	public int getInfo(int arg0)
	{
		return getInfo(arg0, 0);
	}

	public native float getSpeedSquare();

	public native int getHorn();

	public native float hasCrime();

	public Part iteratePartsInit()
	{
		iteratorStack = new Vector();
		if (chassis)
		{
			iteratorStack.addElement(chassis);
			return chassis;
		}
		return null;
	}

	public Part iterateParts()
	{
		if (iteratorStack.size())
		{
			Part local1 = iteratorStack.removeLastElement();
			int local2 = (local1.attachedParts.size() - 1);
			while ((local2 >= 0))
			{
				iteratorStack.addElement(local1.attachedParts.elementAt(local2));
				local2--;
			}
			return local1;
		}
		return null;
	}

	public void wakeUp()
	{
		queueEvent(null, GameRef.EVENT_COMMAND, "wakeup");
	}

	public void addStockParts(float arg0, float arg1, float arg2)
	{
		chassis.addStockParts(arg0, arg1, arg2);
	}

	public void setDefaultTransmission()
	{
		setTransmission(Config.player_transmission);
	}

	public void setTransmission(int arg0)
	{
		queueEvent(null, GameRef.EVENT_COMMAND, ("transmission " + arg0));
	}

	public void setDefaultSteeringHelp()
	{
		setSteeringHelp(Config.player_steeringhelp);
	}

	public void setSteeringHelp(float arg0)
	{
		queueEvent(null, GameRef.EVENT_COMMAND, ("steerhelp " + arg0));
	}

	public void setDefaultASR()
	{
		setASR(Config.player_asr);
	}

	public void setASR(float arg0)
	{
		queueEvent(null, GameRef.EVENT_COMMAND, ("asr " + arg0));
	}

	public void setDefaultABS()
	{
		setABS(Config.player_abs);
	}

	public void setABS(float arg0)
	{
		queueEvent(null, GameRef.EVENT_COMMAND, ("abs " + arg0));
	}

	public void setDifflock(float arg0)
	{
		queueEvent(null, GameRef.EVENT_COMMAND, ("difflock " + arg0));
	}

	public void setCruiseControl(int arg0)
	{
		cruiseControl = arg0;
		queueEvent(null, GameRef.EVENT_COMMAND, ("cruise " + arg0));
	}

	public int getCruiseControl()
	{
		return cruiseControl;
	}

	public void setDamageMultiplier(float arg0)
	{
		queueEvent(null, GameType.EVENT_COMMAND, ("damage_multiplier " + arg0));
	}

	public String toString()
	{
		return chassis.vehicleName;
	}

	public String canTakeSeat()
	{
		String local1 = isDriveable2();
		if (local1)
		{
			return local1;
		}
		return null;
	}

	public String isDriveable()
	{
		String local1 = isDriveable2();
		if (local1)
		{
			return ("You can't drive this car because " + local1);
		}
		return null;
	}

	public String isTestable()
	{
		String local1 = null;
		if (chassis)
		{
			if ((chassis.getConditionNoAttach() == 0.0))
			{
				return "the chassis is totally damaged.";
			}
			Part local2 = null;
			Block local3 = null;
			Battery local4 = null;
			SteeringWheel local5 = null;
			FrontSeat local6 = null;
			int local7 = (chassis.attachedParts.size() - 1);
			while ((local7 >= 0))
			{
				local2 = chassis.attachedParts.elementAt(local7);
				if (local2)
				{
					local1 = local2.isDriveable();
					if (local1)
					{
						return local1;
					}
					if ((local2 instanceof Block))
					{
						local3 = local2;
					} else
					{
						if ((local2 instanceof Battery))
						{
							local4 = local2;
						} else
						{
							if ((local2 instanceof SteeringWheel))
							{
								local5 = local2;
							} else
							{
								if ((local2 instanceof FrontSeat))
								{
									if ((local2.slotIDOnSlot(1) == 901))
									{
										local6 = local2;
									}
								} else
								{
									local2 = null;
								}
							}
						}
					}
				}
				local7--;
			}
			if ((!(local3)))
			{
				return "there's no engine installed.";
			} else
			{
				if ((local3.dynodata.maxTorque <= 0.0))
				{
					return "the engine is not in working condition.";
				}
			}
			if ((chassis.starter_torque == 0.0))
			{
				return "there's problem with the starter.";
			}
			if ((!(local4)))
			{
				return "the car is missing a battery.";
			}
			if ((!(local5)))
			{
				return "there's no steering wheel installed.";
			}
			if ((!(local6)))
			{
				return "there's no driver's seat installed.";
			}
		}
		return null;
	}

	public String isDriveable2()
	{
		String local1 = null;
		if (chassis)
		{
			if ((chassis.getConditionNoAttach() == 0.0))
			{
				return "the chassis is totally damaged.";
			}
			Part local2 = null;
			Block local3 = null;
			Battery local4 = null;
			SteeringWheel local5 = null;
			FrontSeat local6 = null;
			int local7 = (chassis.attachedParts.size() - 1);
			while ((local7 >= 0))
			{
				local2 = chassis.attachedParts.elementAt(local7);
				if (local2)
				{
					local1 = local2.isDriveable();
					if (local1)
					{
						return local1;
					}
					if ((local2 instanceof Block))
					{
						local3 = local2;
					} else
					{
						if ((local2 instanceof Battery))
						{
							local4 = local2;
						} else
						{
							if ((local2 instanceof SteeringWheel))
							{
								local5 = local2;
							} else
							{
								if ((local2 instanceof FrontSeat))
								{
									if ((local2.slotIDOnSlot(1) == 901))
									{
										local6 = local2;
									}
								} else
								{
									local2 = null;
								}
							}
						}
					}
				}
				local7--;
			}
			if ((!(local3)))
			{
				return "there's no engine installed.";
			} else
			{
				if ((local3.dynodata.maxTorque <= 0.0))
				{
					return "the engine is not in working condition.";
				}
			}
			if ((chassis.starter_torque == 0.0))
			{
				return "there's problem with the starter.";
			}
			if ((!(local4)))
			{
				return "the car is missing a battery.";
			}
			if ((!(local5)))
			{
				return "there's no steering wheel installed.";
			}
			if ((!(local6)))
			{
				return "there's no driver's seat installed.";
			}
			int local7_2;
			int local8 = 0;
			local7_2 = 0;
			while ((local7_2 < chassis.wheels))
			{
				WheelRef local9 = chassis.getWheel(local7_2);
				if (local9)
				{
					Part local10 = chassis.partOnSlot((101 + local7_2));
					if (local10)
					{
						String local11 = local10.isDriveable();
						if (local11)
						{
							return local11;
						}
						local8++;
					}
				}
				local7_2++;
			}
			if (((local8 == 0) && (chassis.wheels > 0)))
			{
				return "there are no wheels on the car.";
			} else
			{
				if ((local8 < (chassis.wheels - 3)))
				{
					return "some wheels are missing from the car.";
				} else
				{
					if ((local8 == (chassis.wheels - 3)))
					{
						return "three wheels are missing from the car.";
					} else
					{
						if ((local8 == (chassis.wheels - 2)))
						{
							return "two wheels are missing from the car.";
						} else
						{
							if ((local8 == (chassis.wheels - 1)))
							{
								return "one wheel is missing from the car.";
							}
						}
					}
				}
			}
		}
		return null;
	}

	public int getTotalPrice()
	{
		if (chassis)
		{
			return chassis.currentPrice();
		}
		return 0;
	}

	public void repair()
	{
		if (chassis)
		{
			chassis.repair();
			chassis.forceUpdate();
			wakeUp();
		}
	}

	public float getPrestigeMultiplier()
	{
		float local1 = (races_won - races_lost);
		if ((local1 > 300))
		{
			local1 = 300;
		} else
		{
			if ((local1 < (-(90))))
			{
				local1 = (-(90));
			}
		}
		if ((local1 < 0.0))
		{
			return (1.0 - Math.sqrt(((-(local1)) / 100.0)));
		} else
		{
			if ((local1 > 0.0))
			{
				return (1.0 + Math.sqrt((local1 / 100.0)));
			}
		}
		return 1.0;
	}

	public float getPrestige()
	{
		if ((!(chassis)))
		{
			return 0.0;
		}
		float local1 = chassis.currentPrestige();
		float local2 = getPrestigeMultiplier();
		return (local1 * local2);
	}

	public void save(String arg0)
	{
		File local2 = new File(arg0);
		if (local2.open(File.MODE_WRITE))
		{
			local2.write(Vehicle.SAVEFILEID_VHC);
			local2.write(Vehicle.SAVEFILEVERSION_VHC);
			if (chassis)
			{
				chassis.save(local2);
			}
			local2.write(maxTestTrackSpeedSq);
			local2.write(bestTestTrackAcc);
			local2.write(bestTestTrackAcc120);
			local2.write(bestTestTrackTime0);
			local2.write(bestTestTrackTime0_speedSq);
			local2.write(bestTestTrackTime1);
			local2.write(bestTestTrackTime2);
			local2.write(bestTestTrackTime2_speedSq);
			local2.write(races_won);
			local2.write(races_lost);
			local2.write(bestNightQM);
			local2.close();
		}
	}

	public void loadSkin(String arg0)
	{
		int local2;
		Part local3;
		iteratePartsInit();
		while ((local3 = iterateParts()))
		{
			local2++;
		}
		int[] local4 = new int[local2];
		File local5 = new File(arg0);
		if (local5.open(File.MODE_READ))
		{
			if ((local5.readInt() == Vehicle.SAVEFILEID_SKIN))
			{
				if ((local5.readInt() == Vehicle.SAVEFILEVERSION_SKIN))
				{
					int local6;
					int local7;
					int local8;
					while ((local6 = local5.readResID()))
					{
						local7 = local5.readResID();
						local8 = 0;
						iteratePartsInit();
						while ((local3 = iterateParts()))
						{
							if ((!(local4[local8])))
							{
								if ((local3.getInfo(GameType.GII_TYPE) == local6))
								{
									local3.setTexture(local7);
									local4[local8] = 1;
									break;
								}
							}
							local8++;
						}
					}
				}
			}
			local5.close();
		}
	}

	public void saveSkin(String arg0)
	{
		iteratePartsInit();
		File local2 = new File(arg0);
		if (local2.open(File.MODE_WRITE))
		{
			local2.write(Vehicle.SAVEFILEID_SKIN);
			local2.write(Vehicle.SAVEFILEVERSION_SKIN);
			Part local3;
			int local4;
			while ((local3 = iterateParts()))
			{
				local4 = local3.getTexture();
				if (local4)
				{
					local2.write(new GameRef(local3.getInfo(GameType.GII_TYPE)));
					ResourceRef local5 = new ResourceRef(local4);
					local2.write(local5);
				}
			}
			local2.write(0);
		}
		local2.close();
	}

}
