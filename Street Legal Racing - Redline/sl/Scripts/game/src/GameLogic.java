package java.game;

import java.game.parts.*;
import java.io.*;
import java.render.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class GameLogic extends GameType implements Runnable
{
	static final ResourceRef VEHICLETYPE_ROOT = new ResourceRef(cars:0x1000r);
	static final ResourceRef CHASSIS_ROOT = new ResourceRef(cars:0x101r);
	static final ResourceRef MAPS_ROOT = new ResourceRef(MW_Mod:0xabcr);
	static final int INITIAL_PLAYER_MONEY = 2000000;
	static final float INITIAL_PLAYER_PRESTIGE = 0.3;
	static final float INITIAL_OPPONENT_PRESTIGE_MIN = 0.3;
	static final float INITIAL_OPPONENT_PRESTIGE_MAX = 0.90000004;
	static final float INITIAL_OPPONENT_PRESTIGE_RND = 0.03;
	static final float INITIAL_OPPONENT_AI_MIN = 0.51;
	static final float INITIAL_OPPONENT_AI_MAX = 1.0;
	static final float INITIAL_OPPONENT_AI_RND = 0.0;
	static final float INITIAL_OPPONENT_VHC_MIN = 0.0;
	static final float INITIAL_OPPONENT_VHC_MAX = 0.90000004;
	static final float INITIAL_OPPONENT_VHC_RND = 0.3;
	static final float INITIAL_OPPONENT_NIGHT_VHC_MIN = 0.0;
	static final float INITIAL_OPPONENT_NIGHT_VHC_MAX = 1.2;
	static final float INITIAL_OPPONENT_NIGHT_VHC_RND = 0.1;
	static final int SAVEFILEID_MAIN = -2023406815;
	static final int SAVEFILEVERSION_MAIN = 21;
	static final int SAVEFILEVERSION_MAIN_LOAD = 20;
	public static final int SFX_ENTERGARAGE = frontend:0x7dr;
	public static final int SFX_LEAVEGARAGE = frontend:0x7cr;
	public static final int SFX_WRENCH = frontend:0x7br;
	public static final int SFX_HORN = sound:0x15r;
	public static final int SFX_DECAL = frontend:0x9er;
	public static final int SFX_SPRAY = frontend:0x9cr;
	public static final int SFX_DRAGDROP = frontend:0x9dr;
	public static final int HUMAN_SMILEY = humans:0x9r;
	public static final int HUMAN_OPPONENT = humans:0x7r;
	public static final int HUMAN_OPPONENT2 = humans:0x8r;
	public static final int HUMAN_PLAYER = humans:0xcr;
	public static final int HUMAN_POLICEMAN = humans:0x18r;
	static final int RID_CARCOLOR_Baiern_Devils_eye_red = 0;
	static final int RID_CARCOLOR_Baiern_Spring_yellow = 1;
	static final int RID_CARCOLOR_Einvagen_Zucker = 2;
	static final int RID_CARCOLOR_Einvagen_Tornado_rot = 3;
	static final int RID_CARCOLOR_Einvagen_Nacht = 4;
	static final int RID_CARCOLOR_Einvagen_Smaragd = 5;
	static final int RID_CARCOLOR_Einvagen_Black_mage = 6;
	static final int RID_CARCOLOR_Einvagen_Hamvas_Grun = 7;
	static final int RID_CARCOLOR_Einvagen_Indigo = 8;
	static final int RID_CARCOLOR_Einvagen_Jazz = 9;
	static final int RID_CARCOLOR_Einvagen_Antracit = 10;
	static final int RID_CARCOLOR_Einvagen_Mercator_Blau = 11;
	static final int RID_CARCOLOR_Einvagen_Murano = 12;
	static final int RID_CARCOLOR_Einvagen_Champagner = 13;
	static final int RID_CARCOLOR_Einvagen_Ozean = 14;
	static final int RID_CARCOLOR_Einvagen_Reflex = 15;
	static final int RID_CARCOLOR_Einvagen_Saratoga = 16;
	static final int RID_CARCOLOR_Used_Rusty_Cherry = 17;
	static final int RID_CARCOLOR_Used_Rusty_Smaragd = 18;
	static final int RID_CARCOLOR_Used_Rusty_Nacht = 19;
	static final int RID_CARCOLOR_Used_Rusty_Zucker = 20;
	static final int[] CARCOLORS = new int[21];
	static final String carrerSaveDir = "save/carrer/";
	static final String tempSaveDir = "save/temp/";
	static final String carSaveDir = "save/cars/";
	static final String skinSaveDir = "save/skins/";
	static final String controlSaveDir = "save/controls/";
	static final String activeControlFile;
	static final int ROC_ENTRYFEE = 100000;
	static final int CLUBS = 3;
	static final int CLUBMEMBERS = 20;
	static final String[] CLUBNAMES = new String[GameLogic.CLUBS];
	static final int GM_INVALID = 0;
	static final int GM_CARREER = 1;
	static final int GM_FREERIDE = 2;
	static final int GM_QUICKRACE = 3;
	static final int GM_SINGLECAR = 4;
	static final int GM_DEMO = 5;
	static final int GM_MULTIPLAYER = 6;
	static int gameMode;
	static int timeout;
	static int klampiPatch;
	static int played;
	static int saved;
	static int carrerInProgress;
	static String[] kismajom;
	static VehicleType[] vehicleTypes;
	static VehicleDescriptor[] carDescriptors_New;
	static VehicleDescriptor[] carDescriptors_Used;
	static float dealerVisitTimeStamp_New;
	static float dealerVisitTimeStamp_Used;
	static Hotkey hotkey0;
	static Hotkey hotkey1;
	static Hotkey hotkey2;
	static Hotkey hotkey3;
	static Hotkey hotkey4;
	static GameState actualState;
	static Garage garage;
	static RaceSetup racesetup;
	static int numplayers = 2;
	static GameRef[] controllers;
	static Player player;
	static Player player2;
	static Racer[] speedymen = new Racer[(GameLogic.CLUBS * GameLogic.CLUBMEMBERS)];
	static int day;
	static float time;
	static float timeFactor = 4.0;
	static int timeRefreshRate = 10;
	static Text timeTxt;
	static Text dayTxt;
	static Thread timeRefresher;
	static final int MotionB = 0;
	static final int cheats;
	static final ResourceRef[] ChassisRoot;
	static final ResourceRef[] MapRoot;
	static final Vector MapBox;
	static final Vector ChassisBox;
	static final Vector DefaultSkinBox;
	static final Vector PoliceManSkinBox;
	static final int[] ChassisResNoumber;
	ControlSet controlSet2;

	public static native int kismajomCheck(String[] arg0);

	public static void initVehicleTypes()
	{
		ResourceRef[] local0 = VEHICLETYPE_ROOT.getChildNodes();
		GameLogic.vehicleTypes = new VehicleType[local0.length];
		int local1 = (local0.length - 1);
		while ((local1 >= 0))
		{
			GameRef local2 = new GameRef();
			GameLogic.vehicleTypes[local1] = local2.create(null, local0[local1], null, "VehicleType");
			GameLogic.vehicleTypes[local1].init();
			local1--;
		}
	}

	public static void MWM_Items_Scan()
	{
		GameLogic.ChassisRoot = CHASSIS_ROOT.getChildNodes();
		GameLogic.ChassisBox = new Vector(1);
		GameLogic.ChassisResNoumber = new int[ChassisRoot.length];
		int local0 = 0;
		int local1 = 0;
		while ((local1 <= (ChassisRoot.length - 1)))
		{
			if (GameLogic.ChassisRoot[local1].isScripted())
			{
				GameLogic.ChassisResNoumber[local0] = local1;
				local0++;
				GameRef local2 = new GameRef();
				ChassisBox.addElement(local2.create(null, new GameRef(GameLogic.ChassisRoot[local1]), "0,0,0,0,0,0,", "chassis"));
			}
			local1++;
		}
		System.rpkScan("maps\\Addon_Maps\\");
		GameLogic.MapRoot = MAPS_ROOT.getChildNodes();
		GameLogic.MapBox = new Vector(1);
		int local1_2 = 0;
		while ((local1_2 <= (MapRoot.length - 1)))
		{
			GameRef local2_2 = new GameRef();
			MapBox.addElement(local2_2.create(null, new GameRef(GameLogic.MapRoot[local1_2]), "0,0,0,0,0,0", "load Track script"));
			local1_2++;
		}
		GameLogic.DefaultSkinBox = new Vector(1);
		FindFile local1_3 = new FindFile();
		String local2_3 = local1_3.first("humans/drivers/Default_Driver/*.*", FindFile.FILES_ONLY);
		while (local2_3)
		{
			DefaultSkinBox.addElement(local2_3);
			local2_3 = local1_3.next();
		}
		local1_3.close();
		GameLogic.PoliceManSkinBox = new Vector(1);
		local1_3 = null;
		local1_3 = new FindFile();
		local2_3 = local1_3.first("humans/drivers/PoliceMan_Driver/*.*", FindFile.FILES_ONLY);
		while (local2_3)
		{
			PoliceManSkinBox.addElement(local2_3);
			local2_3 = local1_3.next();
		}
		local1_3.close();
	}

	public static VehicleDescriptor getVehicleDescriptor(int arg0)
	{
		return getVehicleDescriptor(arg0, (-(1)));
	}

	public static VehicleDescriptor getVehicleDescriptor(int arg0, float arg1)
	{
		VehicleType local2 = getVehicleType(arg0);
		return local2.getVehicleDescriptor(arg0, arg1);
	}

	private static VehicleType getVehicleType(int arg0)
	{
		VehicleType local1;
		float local2;
		float local3;
		int local4 = (vehicleTypes.length - 1);
		while ((local4 >= 0))
		{
			if ((arg0 & GameLogic.vehicleTypes[local4].vehicleSetMask))
			{
				local2 += GameLogic.vehicleTypes[local4].prevalence;
			}
			local4--;
		}
		local2 *= Math.random();
		int local4_2 = (vehicleTypes.length - 1);
		while ((local4_2 >= 0))
		{
			if ((arg0 & GameLogic.vehicleTypes[local4_2].vehicleSetMask))
			{
				local3 += GameLogic.vehicleTypes[local4_2].prevalence;
				if ((local3 > local2))
				{
					local1 = GameLogic.vehicleTypes[local4_2];
					break;
				}
			}
			local4_2--;
		}
		return local1;
	}

	public static void preCacheGametypes(ResourceRef arg0)
	{
		arg0 = arg0.getFirstChild();
		while (arg0)
		{
			arg0.cache();
			arg0 = arg0.getNextChild();
		}
	}

	public static void setTimerText(Text arg0, Text arg1)
	{
		GameLogic.timeTxt = arg1;
		GameLogic.dayTxt = arg0;
		timeRefresher.notify();
	}

	public static void setTimerText(Text arg0)
	{
		setTimerText(arg0, null);
	}

	public static void setTime(float arg0)
	{
		GameLogic.time = arg0;
		System.syncGameTime(GameLogic.time);
	}

	public static float getTime()
	{
		return GameLogic.time;
	}

	public static void spendTime(float arg0)
	{
		GameLogic.time += arg0;
		while ((GameLogic.time > (24 * 3600)))
		{
			GameLogic.time -= (24 * 3600);
			GameLogic.day++;
		}
		timeRefresher.notify();
		System.syncGameTime(GameLogic.time);
	}

	public static float mechTime(Part arg0, int arg1)
	{
		if ((!((Mechanic.flags & Mechanic.MF_FREE_REPAIRS))))
		{
			float local2 = ((60 + (arg0.value / 50)) + (arg0.isComplex() * 60));
			if (arg1)
			{
				local2 *= 3.0;
			}
			return local2;
		}
		return 0.0;
	}

	public static void run()
	{
		while (1)
		{
			timeRefresher.wait();
			if (GameLogic.timeTxt)
			{
				timeTxt.changeText(String.timeToString(GameLogic.time, (String.TCF_NOSECONDS | String.TCF_US)));
			}
			if (GameLogic.dayTxt)
			{
				dayTxt.changeText(GameLogic.day);
			}
		}
	}

	public static int canChallenge(Racer arg0, Racer arg1)
	{
		float local2 = 0.049999997;
		if (((arg0.prestige + local2) >= arg1.prestige))
		{
			if ((arg0.club == arg1.club))
			{
				return 1;
			}
		}
		return 0;
	}

	public static void challenge(int arg0, int arg1, int arg2, int arg3, int arg4)
	{
		GameLogic.speedymen[arg0].calcPrestige(GameLogic.speedymen[arg1], arg2, arg3);
		if (arg4)
		{
			if (((!(arg2)) && (((arg0 < arg1) && arg3) || ((arg0 > arg1) && (!(arg3))))))
			{
				Racer local5;
				if (arg3)
				{
					local5 = GameLogic.speedymen[arg0];
				} else
				{
					local5 = GameLogic.speedymen[arg1];
				}
				int local6;
				if ((GameLogic.speedymen[arg0] instanceof Player))
				{
					local6 = findRacer(GameLogic.speedymen[arg0]);
				} else
				{
					if ((GameLogic.speedymen[arg1] instanceof Player))
					{
						local6 = findRacer(GameLogic.speedymen[arg1]);
					}
				}
				Racer local7 = GameLogic.speedymen[arg0];
				GameLogic.speedymen[arg0] = GameLogic.speedymen[arg1];
				GameLogic.speedymen[arg1] = local7;
				if ((GameLogic.speedymen[arg0].club != GameLogic.speedymen[arg1].club))
				{
					int local8 = GameLogic.speedymen[arg0].club;
					GameLogic.speedymen[arg0].club = GameLogic.speedymen[arg1].club;
					GameLogic.speedymen[arg1].club = local8;
				}
				if ((local5 instanceof Player))
				{
					if ((player.club == (GameLogic.CLUBS - 1)))
					{
						if (((findRacer(local5) >= (speedymen.length - 5)) && (local6 < (speedymen.length - 5))))
						{
							new CongratsDialog(
								((Player) local5).controller,
								(((Dialog.DF_HIGHPRI | Dialog.DF_MODAL) | Dialog.DF_FULLSCREEN) | Dialog.DF_FREEZE),
								(frontend:0x8dr + player.club)).display(
							);
						}
					} else
					{
						if (((findRacer(local5) % GameLogic.CLUBMEMBERS) == (GameLogic.CLUBMEMBERS - 1)))
						{
							new CongratsDialog(
								((Player) local5).controller,
								(((Dialog.DF_HIGHPRI | Dialog.DF_MODAL) | Dialog.DF_FULLSCREEN) | Dialog.DF_FREEZE),
								(frontend:0x8dr + player.club)).display(
							);
						}
					}
				}
			}
		}
	}

	public static int findRacer(Racer arg0)
	{
		int local1 = 0;
		while ((local1 < speedymen.length))
		{
			if ((GameLogic.speedymen[local1] == arg0))
			{
				return local1;
			}
			local1++;
		}
		return (-(1));
	}

	public static void changeActiveSection(GameState arg0)
	{
		if (GameLogic.actualState)
		{
			actualState.exit(arg0);
		}
		GameState local1 = GameLogic.actualState;
		GameLogic.actualState = arg0;
		if (GameLogic.actualState)
		{
			actualState.enter(local1);
		} else
		{
			System.exit();
		}
	}

	public static int autoSave()
	{
		if ((GameLogic.played && (!(GameLogic.saved))))
		{
			if (((GameLogic.gameMode == GameLogic.GM_CARREER) && GameLogic.carrerInProgress))
			{
				new WarningDialog(
					player.controller,
					(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
					"CAREER INFO",
					"You are exiting your ongoing career. \n Your career has been automatically saved.").display(
				);
				autoSaveQuiet();
				new SfxRef(Frontend.SFX_OK).play();
			}
		}
		return 1;
	}

	public static void autoSaveQuiet()
	{
		if (((GameLogic.gameMode == GameLogic.GM_CARREER) && GameLogic.carrerInProgress))
		{
			String local0 = ((player.name + "-") + (player.club + 1));
			save(((GameLogic.carrerSaveDir + local0) + "/"));
		}
	}

	public static int save(String arg0)
	{
		File.delete(arg0, "*");
		File local1 = new File((arg0 + "main"));
		if (local1.open(File.MODE_WRITE))
		{
			local1.write(GameLogic.SAVEFILEID_MAIN);
			local1.write(GameLogic.SAVEFILEVERSION_MAIN);
			int local2 = findRacer(GameLogic.player);
			local1.write(player.character);
			local1.write(player.money);
			local1.write(player.prestige);
			local1.write(local2);
			local1.write(player.flags);
			local1.write(player.name);
			local1.write(player.races_won);
			local1.write(player.races_lost);
			local1.write(player.hints);
			local1.write(player.winPinkSlips);
			player.parts.save(local1);
			int local3 = player.decals.size();
			local1.write(local3);
			int local4 = 0;
			while ((local4 < local3))
			{
				Decal local5 = player.decals.elementAt(local4);
				local1.write(local5);
				local1.write(local5.stickies);
				local4++;
			}
			int local4_2 = 0;
			while ((local4_2 < speedymen.length))
			{
				if ((local4_2 != local2))
				{
					local1.write(GameLogic.speedymen[local4_2].fileid);
					local1.write(GameLogic.speedymen[local4_2].seed);
					local1.write(GameLogic.speedymen[local4_2].prestige);
					GameLogic.speedymen[local4_2].botVd.save(local1);
					GameLogic.speedymen[local4_2].nightVd.save(local1);
					GameLogic.speedymen[local4_2].save(local1);
				}
				local4_2++;
			}
			local1.write(carDescriptors_New.length);
			int local4_3 = 0;
			while ((local4_3 < carDescriptors_New.length))
			{
				if (GameLogic.carDescriptors_New[local4_3])
				{
					local1.write(1);
					GameLogic.carDescriptors_New[local4_3].save(local1);
				} else
				{
					local1.write(0);
				}
				local4_3++;
			}
			local1.write(carDescriptors_Used.length);
			int local4_4 = 0;
			while ((local4_4 < carDescriptors_Used.length))
			{
				if (GameLogic.carDescriptors_Used[local4_4])
				{
					local1.write(1);
					GameLogic.carDescriptors_Used[local4_4].save(local1);
				} else
				{
					local1.write(0);
				}
				local4_4++;
			}
			player.carlot.lockPlayerCar();
			int local4_5;
			local1.write(player.carlot.curcar);
			local1.write(getTime());
			local1.write(GameLogic.day);
			local1.write(local4_5);
			local1.write(local4_5);
			local1.write(local4_5);
			local1.write(local4_5);
			local1.write(local4_5);
			local1.write(local4_5);
			local1.write(local4_5);
			local1.close();
			player.carlot.saveCar(player.carlot.curcar);
			player.carlot.releasePlayerCar();
			File.copy(GameLogic.tempSaveDir, "PlayerCar*", arg0);
			return 1;
		}
		return 0;
	}

	public static int fileCheck(String arg0)
	{
		int local1;
		File local2 = new File((arg0 + "/main"));
		if (local2.open(File.MODE_READ))
		{
			if ((local2.readInt() == GameLogic.SAVEFILEID_MAIN))
			{
				if ((local2.readInt() >= GameLogic.SAVEFILEVERSION_MAIN_LOAD))
				{
					local1 = 1;
				}
			}
			local2.close();
		}
		return local1;
	}

	public static int load(String arg0)
	{
		int local1;
		int local2;
		File local3 = new File((arg0 + "main"));
		if (local3.open(File.MODE_READ))
		{
			if ((local3.readInt() == GameLogic.SAVEFILEID_MAIN))
			{
				local2 = local3.readInt();
				if ((local2 >= GameLogic.SAVEFILEVERSION_MAIN_LOAD))
				{
					erase();
					int local4;
					player.character = new ResourceRef(local3.readResID());
					player.money = local3.readInt();
					player.prestige = local3.readFloat();
					local4 = local3.readInt();
					player.flags = local3.readInt();
					player.name = local3.readString();
					player.races_won = local3.readInt();
					player.races_lost = local3.readInt();
					player.hints = local3.readInt();
					if ((local2 > 20))
					{
						player.winPinkSlips = local3.readInt();
					}
					player.club = (local4 / GameLogic.CLUBMEMBERS);
					GameLogic.speedymen[local4] = GameLogic.player;
					player.parts.load(local3);
					int local5 = local3.readInt();
					int local6 = 0;
					while ((local6 < local5))
					{
						int local7 = local3.readResID();
						Decal local8 = new Decal(local7);
						local8.stickies = local3.readInt();
						player.decals.addElement(local8);
						local6++;
					}
					int local6_2 = 0;
					while ((local6_2 < speedymen.length))
					{
						if ((local6_2 != local4))
						{
							GameLogic.speedymen[local6_2] = new Bot(
								local3.readInt(),
								local3.readInt(),
								((1.0 + local6_2) / speedymen.length)
							);
							GameLogic.speedymen[local6_2].prestige = local3.readFloat();
							GameLogic.speedymen[local6_2].botVd = new VehicleDescriptor().load(local3);
							GameLogic.speedymen[local6_2].nightVd = new VehicleDescriptor().load(local3);
							GameLogic.speedymen[local6_2].club = (local6_2 / GameLogic.CLUBMEMBERS);
							GameLogic.speedymen[local6_2].load(local3);
						}
						local6_2++;
					}
					loadUnsavedData();
				} else
				{
					local1 = (-(1));
				}
			} else
			{
				local1 = (-(2));
			}
			GameLogic.carDescriptors_New = new VehicleDescriptor[local3.readInt()];
			int local4_2 = 0;
			while ((local4_2 < carDescriptors_New.length))
			{
				if (local3.readInt())
				{
					GameLogic.carDescriptors_New[local4_2] = new VehicleDescriptor().load(local3);
				}
				local4_2++;
			}
			GameLogic.carDescriptors_Used = new VehicleDescriptor[local3.readInt()];
			int local4_3 = 0;
			while ((local4_3 < carDescriptors_Used.length))
			{
				if (local3.readInt())
				{
					GameLogic.carDescriptors_Used[local4_3] = new VehicleDescriptor().load(local3);
				}
				local4_3++;
			}
			if ((!(local1)))
			{
				File.delete(GameLogic.tempSaveDir, "*");
				File.copy(arg0, "PlayerCar*", GameLogic.tempSaveDir);
				player.carlot.scanCars();
				int local4_4;
				player.carlot.curcar = local3.readInt();
				player.carlot.old_curcar = player.carlot.curcar;
				setTime(local3.readFloat());
				GameLogic.day = local3.readInt();
				local4_4 = local3.readInt();
				local4_4 = local3.readInt();
				local4_4 = local3.readInt();
				local4_4 = local3.readInt();
				local4_4 = local3.readInt();
				local4_4 = local3.readInt();
				local4_4 = local3.readInt();
				player.carlot.releasePlayerCar();
			}
			local3.close();
		} else
		{
			local1 = (-(3));
		}
		return local1;
	}

	public static void loadUnsavedData()
	{
		player.driverID = new RenderRef(GameLogic.HUMAN_PLAYER);
		GameLogic.timeout = 0;
		GameLogic.gameMode = GameLogic.GM_CARREER;
		setTime((12 * 3600));
		GameLogic.day = 1;
	}

	public static void loadDefaults()
	{
		erase();
		player.carlot.scanCars();
		player.money = GameLogic.INITIAL_PLAYER_MONEY;
		player.prestige = GameLogic.INITIAL_PLAYER_PRESTIGE;
		player.club = 0;
		player.flags = 0;
		player.hints = 0;
		player.winPinkSlips = 0;
		if ((!(player.character)))
		{
			player.character = new ResourceRef(Racer.RID_FEJ);
			player.name = "Player";
		}
		GameLogic.speedymen[0] = GameLogic.player;
		int local0 = (-(1));
		int local1 = 1;
		while ((local1 < speedymen.length))
		{
			if ((player.character.id() == ((Racer.RID_FEJ + local1) + local0)))
			{
				local0++;
			}
			GameLogic.speedymen[local1] = new Bot(
				(local1 + local0),
				(local1 * 7243),
				((GameLogic.INITIAL_OPPONENT_AI_MIN + (((GameLogic.INITIAL_OPPONENT_AI_MAX - GameLogic.INITIAL_OPPONENT_AI_MIN) * local1) / speedymen.length)) + (GameLogic.INITIAL_OPPONENT_AI_RND * ((Math.random() * 2.0) - 1.0)))
			);
			GameLogic.speedymen[local1].botVd = getVehicleDescriptor(
				VehicleType.VS_DRACE,
				((GameLogic.INITIAL_OPPONENT_VHC_MIN + (((GameLogic.INITIAL_OPPONENT_VHC_MAX - GameLogic.INITIAL_OPPONENT_VHC_MIN) * local1) / speedymen.length)) + (GameLogic.INITIAL_OPPONENT_VHC_RND * ((Math.random() * 2.0) - 1.0)))
			);
			GameLogic.speedymen[local1].nightVd = getVehicleDescriptor(
				VehicleType.VS_NRACE,
				((GameLogic.INITIAL_OPPONENT_NIGHT_VHC_MIN + (((GameLogic.INITIAL_OPPONENT_NIGHT_VHC_MAX - GameLogic.INITIAL_OPPONENT_NIGHT_VHC_MIN) * local1) / speedymen.length)) + (GameLogic.INITIAL_OPPONENT_NIGHT_VHC_RND * ((Math.random() * 2.0) - 1.0)))
			);
			GameLogic.speedymen[local1].bestNightQM = GameLogic.speedymen[local1].nightVd.estimateQM();
			local1++;
		}
		int local1_2 = 1;
		while ((local1_2 < (speedymen.length - 1)))
		{
			int local2 = (local1_2 + 1);
			while ((local2 < speedymen.length))
			{
				if ((GameLogic.speedymen[local1_2].nightVd.estimateQM() < GameLogic.speedymen[local2].nightVd.estimateQM()))
				{
					Bot local3 = GameLogic.speedymen[local1_2];
					GameLogic.speedymen[local1_2] = GameLogic.speedymen[local2];
					GameLogic.speedymen[local2] = local3;
				}
				local2++;
			}
			local1_2++;
		}
		int local1_3 = 1;
		while ((local1_3 < speedymen.length))
		{
			GameLogic.speedymen[local1_3].prestige = ((GameLogic.INITIAL_OPPONENT_PRESTIGE_MIN + (((GameLogic.INITIAL_OPPONENT_PRESTIGE_MAX - GameLogic.INITIAL_OPPONENT_PRESTIGE_MIN) * local1_3) / speedymen.length)) + (GameLogic.INITIAL_OPPONENT_PRESTIGE_RND * ((Math.random() * 2.0) - 1.0)));
			GameLogic.speedymen[local1_3].club = (local1_3 / GameLogic.CLUBMEMBERS);
			local1_3++;
		}
		loadUnsavedData();
		GameLogic.carDescriptors_New = CarMarket.getInitialCars(0);
		GameLogic.carDescriptors_Used = CarMarket.getInitialCars(1);
		GameLogic.dealerVisitTimeStamp_Used = 0.0;
		GameLogic.dealerVisitTimeStamp_New = 0.0;
	}

	public static void erase()
	{
		if (player.car)
		{
			player.car.destroy();
			player.car = null;
		}
		player.parts = new Inventory(GameLogic.player);
		player.carlot = new CarLot(GameLogic.player);
		while ((!(player.decals.isEmpty())))
		{
			ResourceRef local0 = player.decals.elementAt(0);
			local0.unload();
			local0.destroy();
			player.decals.removeElementAt(0);
		}
		File.delete(GameLogic.tempSaveDir, "*");
		GameLogic.carDescriptors_New = null;
		GameLogic.carDescriptors_Used = null;
		GameLogic.played = 0;
		GameLogic.saved = 0;
		GameLogic.carrerInProgress = 0;
	}

	public GameLogic()
	{
		super();
		createNativeInstance();
		GameLogic.activeControlFile = Controller.controlFile;
		GameLogic.CLUBNAMES[0] = "Green Slip";
		GameLogic.CLUBNAMES[1] = "Blue Cheetah";
		GameLogic.CLUBNAMES[2] = "Red Flame";
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Baiern_Devils_eye_red] = cars:0x2ar;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Baiern_Spring_yellow] = cars:0x2br;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Zucker] = cars:0x42r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Tornado_rot] = cars:0x43r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Nacht] = cars:0x44r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Smaragd] = cars:0x45r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Black_mage] = cars:0x46r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Hamvas_Grun] = cars:0x47r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Indigo] = cars:0x48r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Jazz] = cars:0x49r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Antracit] = cars:0x4ar;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Mercator_Blau] = cars:0x4br;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Murano] = cars:0x4cr;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Champagner] = cars:0x4dr;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Ozean] = cars:0x4er;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Reflex] = cars:0x4fr;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Einvagen_Saratoga] = cars:0x50r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Used_Rusty_Cherry] = cars:0x65r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Used_Rusty_Smaragd] = cars:0x66r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Used_Rusty_Nacht] = cars:0x67r;
		GameLogic.CARCOLORS[GameLogic.RID_CARCOLOR_Used_Rusty_Zucker] = cars:0x68r;
		GameLogic.kismajom = new String[5];
		GameLogic.kismajom[0] = "mfunfspd";
		GameLogic.kismajom[1] = "cfhgpsnpofz";
		GameLogic.kismajom[2] = "gbtuqspnpujpo";
		GameLogic.kismajom[3] = "jmjlfdifbujoh";
		GameLogic.kismajom[4] = "ujnfnbdijof";
		Math.randomize();
		new SfxRef(City.RID_SPEECH3).cache();
		new SfxRef(City.RID_SPEECH2).cache();
		new SfxRef(City.RID_SPEECH1).cache();
		new SfxRef(City.RID_SPEECHGO).cache();
		new SfxRef(City.RID_SFX_DAY_WIN).cache();
		new SfxRef(City.RID_SFX_DAY_LOOSE).cache();
		System.rpkScan("parts\\engines\\");
		System.rpkScan("parts\\");
		System.rpkScan("cars\\racers\\");
		System.rpkScan("cars\\fake_racers\\");
		System.rpkScan("maps\\");
		System.openLib("MW_Mod.rpk");
		initVehicleTypes();
		MWM_Items_Scan();
		float local1 = (Math.random() * 100);
		if ((MainMenu.Check(local1) != (Math.sqrt(local1) + 3.75)))
		{
			new WarningDialog(
				GameLogic.player.controller,
				(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
				"PROTECTION ERROR",
				"Miran Wichur Mod protection error: \n \n CORRUPTED FILES \n \n Sorry game will be close. \n Please reinstall Miran Wichur Mod.").display(
			);
			GameLogic.changeActiveSection(null);
		}
		preCacheGametypes(new GameRef(humans:0x1r));
		preCacheGametypes(new GameRef(cars:0x4r));
		GameLogic.controllers = new GameRef[GameLogic.numplayers];
		int[] local2 = new int[Input.MAXPLAYERS];
		int local3;
		do
		{
			int local4;
			float local5 = 0.0;
			int local6 = 0;
			while ((local6 < Input.MAXPLAYERS))
			{
				if ((((!(local2[local6])) && Input.isPlayerActive(local6)) && (Input.player_input[local6] >= local5)))
				{
					local5 = Input.player_input[local6];
					local4 = local6;
				}
				local6++;
			}
			GameLogic.controllers[local3] = Input.controllers[local4];
			local2[local4] = 1;
		} while ((++local3 < GameLogic.numplayers));
		GameLogic.player = new Player();
		player.controller = GameLogic.controllers[0];
		Input.cursor.setController(player.controller);
		player.controller.reset();
		player.controller.activateState(ControlSet.MENUSET);
		GameLogic.player2 = new Player();
		player2.controller = GameLogic.controllers[1];
		GameLogic.garage = new Garage();
		GameLogic.hotkey0 = new Hotkey(
			Input.AXIS_MUSIC_VOLUME_UP,
			Input.VIRTUAL,
			Input.AXIS_MUSIC_VOLUME_UP,
			this
		);
		GameLogic.hotkey1 = new Hotkey(
			Input.AXIS_MUSIC_VOLUME_DOWN,
			Input.VIRTUAL,
			Input.AXIS_MUSIC_VOLUME_DOWN,
			this
		);
		GameLogic.hotkey2 = new Hotkey(
			Input.AXIS_MUSIC_SELECT_NEXT,
			Input.VIRTUAL,
			Input.AXIS_MUSIC_SELECT_NEXT,
			this
		);
		GameLogic.hotkey3 = new Hotkey(
			Input.AXIS_MUSIC_SELECT_PREV,
			Input.VIRTUAL,
			Input.AXIS_MUSIC_SELECT_PREV,
			this
		);
		GameLogic.hotkey4 = new Hotkey(Input.AXIS_PRINTSCREEN, Input.VIRTUAL, Input.AXIS_PRINTSCREEN, this);
		GameLogic.timeRefresher = new Thread(this, "Game time refresher");
		timeRefresher.start();
		setEventMask((GameRef.EVENT_HOTKEY | GameRef.EVENT_TIME));
		addTimer(GameLogic.timeRefreshRate, 0);
		Frontend.loadingScreen.hide();
		//changeActiveSection(new SplashScreen(new ResourceRef(frontend:0x5r)));
		changeActiveSection( new MainMenu() );
	}

	public void finalize()
	{
		timeRefresher.stop();
		hotkey0.inactivate();
		hotkey1.inactivate();
		hotkey2.inactivate();
		hotkey3.inactivate();
		hotkey4.inactivate();
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		if ((arg1 == GameRef.EVENT_TIME))
		{
			GameLogic.spendTime((GameLogic.timeRefreshRate * GameLogic.timeFactor));
			addTimer(GameLogic.timeRefreshRate, 0);
		}
	}

	public void handleEvent(Hotkey arg0)
	{
		int local2 = arg0.command;
		if ((local2 == Input.AXIS_MUSIC_VOLUME_UP))
		{
			Sound.increaseVolume(1, 0.1);
		} else
		{
			if ((local2 == Input.AXIS_MUSIC_VOLUME_DOWN))
			{
				Sound.decreaseVolume(1, 0.1);
			} else
			{
				if ((local2 == Input.AXIS_MUSIC_SELECT_NEXT))
				{
					Sound.nextTrack();
				} else
				{
					if ((local2 == Input.AXIS_MUSIC_SELECT_PREV))
					{
						Sound.prevTrack();
					} else
					{
						if ((local2 == Input.AXIS_PRINTSCREEN))
						{
							GfxEngine.printScreenIndexed("screenshots\\sl_shot");
						}
					}
				}
			}
		}
	}

}
