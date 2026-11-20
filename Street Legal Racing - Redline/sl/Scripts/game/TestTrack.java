package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.dialog.*;
import java.sound.*;
import java.util.*;
import java.util.resource.*;

public class TestTrack extends Track
{
	static final String MapName = "Test Track";
	static final String MapDescription = "\n It is default game's Test Track with Miran's Destruction Tools 1.0 \n \n \n There You can test Your car acceleration, handling etc, take a part in The Weight Challenge, get some damge using Elevator or simply: take a ride.";
	static final String MapAuthor = "Authors: Invictus & Miran";
	static final ResourceRef MapImage = new ResourceRef(MW_Mod:0x32r);
	static final float TS_Version = 1.0;
	static final int MPH_60 = 771.6049;
	static final int MPH_124 = 3086.4197;
	static final int Weight_Name;
	static final int WeightSFXname;
	static final RenderRef GMarker;
	Vector3 posExit = new Vector3(189.2, (-(3.8)), (-(1417.0)));
	Ypr oriExit = new Ypr(2.8799999, 0.049999997, 0.0);
	Vector3 WCH_Start = new Vector3(251.5, 1.5, (-(558)));
	Vector3 Elevator_Pos = new Vector3((-(233)), (-(0.8)), (-(12)));
	Vector3 PolicePos = new Vector3(14, 0, (-(1128)));
	Text messageTextA;
	Text messageTextB;
	Text messageText0;
	Text messageText1;
	Text messageText2;
	int testStatus = 0;
	int triggerIndex = 0;
	int acceleration = 0;
	int acceleration120 = 0;
	float testStartTime = 0.0;
	Marker mPolice;
	Round actRound = null;
	Round[] round = new Round[2];
	int rounds = 2;
	int WeightSFXbuffor;
	int WeightSFXspeed;
	int WeightSFXstatus;
	int ElevatorRotate;
	int KeyCheckStat;
	int KeyCheck;
	int OptionE;
	int Survived;
	int LightsStatus;
	int Lights;
	int PrimeWin;
	int LastLevel;
	int SFX_E_Loop_ID;
	int WCH_Level;
	int Level = 0;
	int WeightBornStatus;
	int SurvivedGoal;
	int Weight_Created;
	int Player_Dialog_Text_Status;
	RenderRef WMarker;
	RenderRef WMarkerF;
	RenderRef Elevator_Platform;
	RenderRef Elevator_Arm;
	RenderRef Light1;
	RenderRef Light2;
	RenderRef Light3;
	RenderRef Light4;
	PhysicsRef PlatformB = new PhysicsRef();
	PhysicsRef Platform = new PhysicsRef();
	PhysicsRef Arm = new PhysicsRef();
	float Frequency;
	float CalcRotSpeed;
	float RotateGoal;
	float RotateSpeed;
	float CalcSpeed;
	float TimX;
	float LightsTimer;
	float ToSlowTime;
	float ToSlow;
	float PDTS_time;
	float PDTS_time_start;
	float Weight_Time;
	float X = 0.01;
	float ElevatorStatus = 0;
	float ElevatorSpeed;
	float Weight_Rotate;
	float Weight_Dist;
	Text TimeInfo;
	Text SpeedInfo;
	Text PosInfo;
	Text YprInfo;
	Text Level_Info;
	Text Weight_Text;
	Text Weight_Frequency_Text;
	Text Weight_Survived_Text;
	Text Weight_Level_Text;
	Text Weight_Distance_Text;
	Text Player_Dialog_Text;
	Rectangle Level_Info_Frame;
	Rectangle Info_Strip;
	Vector3 WeightSFXpos = new Vector3(0, 0, 0);
	Vector3 MarkerVector = new Vector3(WCH_Start);
	Vector3 ElevatorPlatformVectorSub = new Vector3(Elevator_Pos);
	Vector3 ElevatorPlatformVector = new Vector3(Elevator_Pos);
	Vector3 ElevatorArmVector = new Vector3(Elevator_Pos);
	Vector3 PlatformBPhysVector = new Vector3(Elevator_Pos);
	Vector3 PlatformPhysVector = new Vector3(Elevator_Pos);
	Vector3 ArmPhysVector = new Vector3(Elevator_Pos);
	Vector3 ElevatorMarkerVector = new Vector3(Elevator_Pos);
	Vector3 Light1Pos = new Vector3(Elevator_Pos);
	Vector3 Light2Pos = new Vector3(Elevator_Pos);
	Vector3 Light3Pos = new Vector3(Elevator_Pos);
	Vector3 Light4Pos = new Vector3(Elevator_Pos);
	Vector3 CarVector;
	Ypr CarYpr = new Ypr(0.0, 0.0, 0.0);
	Ypr ElevatorPlatformYpr = new Ypr(0.0, 0.0, 0.0);
	Ypr ElevatorPhysYpr = new Ypr(0.0, 0.0, 0.0);
	Ypr ElevatorPhysBYpr = new Ypr(0.0, 0.0, 0.0);
	Pori PlatformPhysPori;
	Pori PlatformBPhysPori;
	Vehicle[] Weight = new Vehicle[5];
	Vehicle prime;
	Bot[] raceBot = new Bot[2];
	Vector3[] EPPhysPos = new Vector3[18];
	PhysicsRef EPPhys1 = new PhysicsRef();
	PhysicsRef EPPhys2 = new PhysicsRef();
	PhysicsRef EPPhys3 = new PhysicsRef();
	PhysicsRef EPPhys4 = new PhysicsRef();
	PhysicsRef EPPhys5 = new PhysicsRef();
	PhysicsRef EPPhys6 = new PhysicsRef();
	PhysicsRef EPPhys7 = new PhysicsRef();
	PhysicsRef EPPhys8 = new PhysicsRef();
	PhysicsRef EPPhys9 = new PhysicsRef();
	PhysicsRef EPPhys10 = new PhysicsRef();
	PhysicsRef EPPhys11 = new PhysicsRef();
	PhysicsRef EPPhys12 = new PhysicsRef();
	PhysicsRef EPPhys13 = new PhysicsRef();
	PhysicsRef EPPhys14 = new PhysicsRef();
	PhysicsRef EPPhys15 = new PhysicsRef();
	PhysicsRef EPPhys16 = new PhysicsRef();
	PhysicsRef EPPhys17 = new PhysicsRef();
	PhysicsRef EPPhys18 = new PhysicsRef();

	public TestTrack()
	{
		super();
		map = new GroundRef(maps.test_track:0x1r);
		nav = new Navigator(
			((-(5.3170004)) - 2.6399999),
			(-(15.995999)),
			2.6399999,
			maps.test_track.smallmap:0x1r,
			maps.test_track.smallmap:0x2r,
			maps.test_track.smallmap:0x5r,
			6,
			8,
			10
		);
	}

	public void StartMap()
	{
		GameLogic.changeActiveSection(new TestTrack());
	}

	public void enter(GameState arg0)
	{
		Frontend.loadingScreen.show();
		GfxEngine.flush();
		posStart = posExit;
		oriStart = oriExit;
		PrimeWin = 0;
		round[0] = new Round(this, "Go straight ahead", "to time the accelearation of your car!");
		round[0].startdir(3.1399999);
		round[0].point(0.0, 0.0, (-(1461.7849)), 1, 10);
		round[0].point(0.0, 0.0, (-(1059.7849)), 1, 20);
		round[0].point(0.0, 0.0, 148.215, 1, 20);
		round[1] = new Round(this, "Test lap time!", "GO!");
		round[1].startdir(3.1399999);
		round[1].point((-(450.598)), 0.0, (-(524.816)), 1);
		round[1].point((-(450.598)), 0.0, (-(223.142)), 1);
		round[1].point((-(444.73602)), 1.011, 72.27, 1);
		round[1].point((-(27.535)), 1.425, 450.475, 1);
		round[1].point(444.689, 1.011, 72.56, 1);
		round[1].point(450.598, 0.0, (-(223.142)), 1);
		round[1].point(450.598, 0.0, (-(544.816)), 1);
		round[1].point(450.598, 0.0, (-(893.99506)), 1);
		round[1].point(446.293, 0.96500003, (-(1124.731)), 1);
		round[1].point((-(40.0)), 1.425, (-(1513.3281)), 1);
		round[1].point((-(446.0)), 1.425, (-(1124.0)), 1);
		round[1].point((-(450.598)), 0.0, (-(893.99603)), 1);
		round[1].loop();
		actRound = null;
		super.enter(arg0);
		messageTextA = osd.createText("", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.8)));
		messageTextB = osd.createText("", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.7)));
		messageText0 = osd.createText("", Frontend.largeFont, Text.ALIGN_RIGHT, 0.0, (-(0.7)));
		messageText1 = osd.createText("", Frontend.largeFont, Text.ALIGN_LEFT, 0.0, (-(0.7)));
		messageText2 = osd.createText("", Frontend.largeFont, Text.ALIGN_CENTER, 0.0, (-(0.6)));
		PosInfo = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, (-(0.85)), (-(0.90000004)));
		PosInfo.changeColor(-8192256);
		YprInfo = osd.createText("", Frontend.smallFont, Text.ALIGN_RIGHT, 0.85, (-(0.90000004)));
		YprInfo.changeColor(-8192256);
		SpeedInfo = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, (-(0.85)), (-(0.8)));
		SpeedInfo.changeColor(-8192256);
		TimeInfo = osd.createText("", Frontend.smallFont, Text.ALIGN_RIGHT, 0.85, (-(0.8)));
		TimeInfo.changeColor(-8192256);
		Level_Info = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, 0.815, (-(0.84000003)));
		Player_Dialog_Text = osd.createText(
			"",
			new ResourceRef(Text.RID_SLII24),
			Text.ALIGN_CENTER,
			0.0,
			(-(0.7))
		);
		Weight_Text = osd.createText("", Frontend.smallFont, Text.ALIGN_LEFT, (-(0.95000005)), (-(0.98)));
		Weight_Level_Text = osd.createText("", Frontend.smallFont, Text.ALIGN_CENTER, (-(0.38)), (-(0.98)));
		Weight_Survived_Text = osd.createText("", Frontend.smallFont, Text.ALIGN_CENTER, (-(0.06)), (-(0.98)));
		Weight_Frequency_Text = osd.createText("", Frontend.smallFont, Text.ALIGN_CENTER, 0.31, (-(0.98)));
		Weight_Distance_Text = osd.createText("", Frontend.smallFont, Text.ALIGN_RIGHT, 0.95000005, (-(0.98)));
		if ((player.club == 2))
		{
			TestTrack.GMarker = new RenderRef(frontend:0x73r);
		} else
		{
			if ((player.club == 1))
			{
				TestTrack.GMarker = new RenderRef(frontend:0x5fr);
			} else
			{
				TestTrack.GMarker = new RenderRef(frontend:0x5br);
			}
		}
		addTrigger(new Vector3(PolicePos), null, null, "Police_Trigger", 15.0, "trigger");
		addTrigger(
			new Vector3(WCH_Start),
			null,
			new RenderRef(DestructionTools:0x22r),
			"Weight_Challenge_Trigger",
			1.5,
			"trigger"
		);
		addTrigger(
			new Vector3(Elevator_Pos),
			null,
			new RenderRef(DestructionTools:0x86r),
			"Elevator_Trigger",
			2.0,
			"trigger"
		);
		addTrigger(
			new Vector3(193, (-(4.5)), (-(1427))),
			null,
			TestTrack.GMarker,
			"Exit_Trigger",
			3.0,
			"trigger"
		);
		WMarker = new RenderRef(map, DestructionTools:0x24r, "3D marker");
		WMarker.setMatrix(new Vector3(MarkerVector), null);
		WMarkerF = new RenderRef(map, DestructionTools:0x26r, "3D marker frame");
		Elevator_Arm = new RenderRef(map, DestructionTools:0x33r, "Elevator's arm");
		Elevator_Arm.setMatrix(new Vector3(ElevatorArmVector), null);
		Arm.createBox(map, 1.135, 72.84701, 1.135, "Arm physic");
		ArmPhysVector.y -= 36.549;
		Arm.setMatrix(new Vector3(ArmPhysVector), null);
		Elevator_Platform = new RenderRef(map, DestructionTools:0x31r, "Elevator's platform");
		Elevator_Platform.setMatrix(new Vector3(ElevatorPlatformVector), null);
		Platform.createBox(map, 3.986, 0.01, 9.85, "Platform Top physic");
		Platform.createBox(map, 3.986, 0.02, 9.85, "Platform Top physic");
		PlatformPhysVector.y += 1.0;
		Platform.setMatrix(new Vector3(PlatformPhysVector), null);
		PlatformB.createBox(map, 3.986, 0.01, 9.85, "Platform Bottom physic");
		PlatformB.createBox(map, 3.986, 0.02, 9.85, "Platform Bottom physic layer 2");
		PlatformBPhysVector.y += 0.4;
		PlatformB.setMatrix(new Vector3(PlatformBPhysVector), null);
		EPPhys1.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys2.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys3.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys4.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys5.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys6.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys7.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys8.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys9.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys10.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys11.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys12.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys13.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys14.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys15.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys16.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys17.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys18.createBox(map, 0.2, 0.01, 9.85, "Platform Rotate physic");
		EPPhys1.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys2.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys3.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys4.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys5.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys6.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys7.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys8.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys9.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys10.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys11.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys12.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys13.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys14.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys15.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys16.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys17.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		EPPhys18.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		new SfxRef(DestructionTools:0x81r).cache();
		Light1Pos.x += 1.9200001;
		Light1Pos.y += 1;
		Light1Pos.z += 4.92;
		Light2Pos.x += 1.9200001;
		Light2Pos.y += 1;
		Light2Pos.z -= 4.92;
		Light3Pos.x -= 1.9200001;
		Light3Pos.y += 1;
		Light3Pos.z += 4.92;
		Light4Pos.x -= 1.9200001;
		Light4Pos.y += 1;
		Light4Pos.z -= 4.92;
		Weight[0] = null;
		Weight[1] = null;
		Weight[2] = null;
		Weight[3] = null;
		Weight[4] = null;
		raceBot[0] = new Bot((Math.random() * 59), 4, 1, 2.0, 2.0, 20.0);
		GameRef local2 = new GameRef(
			map,
			cars.misc.Police:0x6r,
			((("0,0,0" + ",") + "0,0,0,") + 1),
			"Police Bot"
		);
		raceBot[0].createCar(map, new Vehicle(local2));
		RenderRef local3 = new RenderRef(map, humans:0x18r, "driver");
		raceBot[0].car.command(("corpse 0 " + local3.id()));
		raceBot[0].brain.queueEvent(null, GameRef.EVENT_COMMAND, "AI_params 50.0 50.35");
		raceBot[0].car.setMatrix(new Vector3(PolicePos), new Ypr(1.5, 0, 0));
		raceBot[0].car.wakeUp();
		changeCamTarget2(raceBot[0].car);
		if (nav)
		{
			mPolice = nav.addMarker(new RenderRef(MW_Mod:0x101r), raceBot[0].car);
		}
		Player_Dialog_Text.changeColor(-10752);
		Thread.sleep(1000);
		new SfxRef(DestructionTools:0x39r).play();
		Player_Dialog_Text.changeText("Miran's Destruction Tools - (C) Miran 2007");
		Thread.sleep(4000);
		new SfxRef(DestructionTools:0x39r).play();
		Player_Dialog_Text.changeText("Special for annivesary of:");
		Thread.sleep(4000);
		new SfxRef(DestructionTools:0x39r).play();
		Player_Dialog_Text.changeText("www.Redline-Racers.info");
		Thread.sleep(5000);
		Player_Dialog_Text.changeText("Have a fun - Miran");
		Player_Dialog_Text_Status = 2;
		PDTS_time = 3.0;
	}

	public void exit(GameState arg0)
	{
		if ((!((arg0 instanceof RaceSetup))))
		{
			removeAllTimers();
		}
		if (round[0])
		{
			round[0].destroy();
			round[0] = null;
		}
		if (round[1])
		{
			round[1].destroy();
			round[1] = null;
		}
		nav.remMarker(mPolice);
		mPolice = null;
		if ((PrimeWin == 1))
		{
			player.carlot.addCar(player.car);
			player.carlot.saveCar(player.carlot.curcar);
			player.carlot.flushCars();
			player.car = prime;
			PrimeWin = 0;
		}
		super.exit(arg0);
	}

	public void Police_Trigger(GameRef arg0, int arg1, String arg2)
	{
		if ((arg2.token(0).intValue() == player.car.id()))
		{
			raceBot[0].followCar(player.car, 0.0009999999);
			raceBot[0].pressHorn();
		}
	}

	public void Elevator_Trigger(GameRef arg0, int arg1, String arg2)
	{
		if (((arg1 == GameRef.EVENT_TRIGGER_ON) && (WeightBornStatus == 0)))
		{
			if ((ElevatorStatus == 0))
			{
				Level_Info.changeText(("Level: " + Level));
				Level_Info_Frame = osd.createRectangle(
					1.3499999,
					(-(0.82)),
					1.2,
					0.22,
					(-(1)),
					new ResourceRef(frontend:0x24r)
				);
				new SfxRef(
					DestructionTools:0x34r).play(ElevatorPlatformVector,
					0.0,
					System.timeWarp((-(1))),
					1.0,
					0
				);
				Thread.sleep((2500 * TimX));
				ElevatorSpeed = 0.01;
				ElevatorStatus = 1;
				player.car.wakeUp();
				Lights = 1;
				new SfxRef(
					DestructionTools:0x35r).play(ElevatorPlatformVector,
					0.0,
					System.timeWarp((-(1))),
					1.0,
					0
				);
				Thread.sleep((300 * TimX));
				SFX_E_Loop_ID = new SfxRef(
					DestructionTools:0x36r).play(Elevator_Pos,
					0.0,
					(System.timeWarp((-(1))) - 0.5),
					1.0,
					0
				);
			} else
			{
				if ((((ElevatorStatus == 1) && (ElevatorRotate == 0)) || ((ElevatorStatus == 2) & (ElevatorRotate == 0))))
				{
					Player_Dialog_Text.changeText("Watch Your head!");
					Player_Dialog_Text_Status = 2;
					PDTS_time = 4.0;
					ElevatorStatus = 3;
					ElevatorSpeed = (-(0.049999997));
					new SfxRef(DestructionTools:0x36r).stop(SFX_E_Loop_ID);
					new SfxRef(DestructionTools:0x87r).stop(SFX_E_Loop_ID);
					new SfxRef(
						DestructionTools:0x37r).play(ElevatorPlatformVector,
						0.0,
						System.timeWarp((-(1))),
						1.0,
						0
					);
					new SfxRef(DestructionTools:0x37r).play(Elevator_Pos, 0.0, System.timeWarp((-(1))), 1.0, 0);
					SFX_E_Loop_ID = new SfxRef(
						DestructionTools:0x36r).play(Elevator_Pos,
						0.0,
						(System.timeWarp((-(1))) - 0.5),
						1.0,
						0
					);
				}
			}
		}
	}

	public void Weight_Challenge_Trigger(GameRef arg0, int arg1, String arg2)
	{
		if ((arg1 == GameRef.EVENT_TRIGGER_ON))
		{
			if ((player.car.getSpeedSquare() < 250.0))
			{
				if (((((WeightBornStatus == 0) && (PrimeWin == 0)) && (ElevatorStatus == 0)) && (ElevatorRotate == 0)))
				{
					player.car.command("brake");
					cam.command("angle 0 3.0 0.4853");
					new SfxRef(frontend:0x7er).play();
					if ((0 == new YesNoDialog(player.controller, ((Dialog.DF_LOWPRI | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG), "The Weight Challenge", " \n You wanna take this challenge? \n \n YOUR CAR PAROBABLE BEEN SMASHED! \n \n Is one rule: Your minimum speed is 40 kmh (25 mph) \n \n Have a fun - Miran").display()))
					{
						player.car.command("start");
						cam.command("angle 0 0.0 0.0");
						Weight_Start();
					} else
					{
						cam.command("angle 0 0.0 0.0");
						player.car.command("start");
					}
				}
			}
		}
	}

	public void Exit_Trigger(GameRef arg0, int arg1, String arg2)
	{
		if ((arg1 == GameRef.EVENT_TRIGGER_ON))
		{
			player.car.command("stop");
			if ((0 == new YesNoDialog(player.controller, ((Dialog.DF_LOWPRI | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG), "QUESTION", "Go back to the garage?").display()))
			{
				player.car.command("start");
				GameLogic.changeActiveSection(GameLogic.garage);
			} else
			{
				player.car.command("start");
			}
		}
	}

	public void Weight_Start()
	{
		if (round[0])
		{
			round[0].destroy();
			round[0] = null;
		}
		if (round[1])
		{
			round[1].destroy();
			round[1] = null;
		}
		Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
		Info_Strip = osd.createRectangle(
			0.0,
			(-(0.95000005)),
			2.0,
			0.1,
			(-(1)),
			new ResourceRef(frontend:0x28r)
		);
		WCH_Level = 1;
		Survived = 0;
		Weight_Time = 4;
		Weight_Dist = 9.0;
		SurvivedGoal = 5;
		LastLevel = 0;
		Weight_Text.changeText("The Weight Challenge:");
		Weight_Level_Text.changeText(("Level: " + WCH_Level));
		Weight_Survived_Text.changeText(("Survived: " + Survived));
		Weight_Frequency_Text.changeText(Float.toString(Weight_Time, "Frequency: %1.1f s"));
		Weight_Distance_Text.changeText(Float.toString(Weight_Dist, "Initial distance: %1.1f m"));
		Thread.sleep((1000 * TimX));
		Player_Dialog_Text.changeText("Go, Run, Escape... and Don't Stop!");
		new SfxRef(DestructionTools:0x39r).play();
		if ((Weight_Created == 0))
		{
			Weight[0] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
			Weight[1] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
			Weight[2] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
			Weight[3] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
			Weight[4] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
			Weight_Created = 1;
		}
		Weight[0].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
		Weight[1].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
		Weight[2].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
		Weight[3].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
		Weight[4].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
		Thread.sleep((5000 * TimX));
		Player_Dialog_Text.changeText("Watch Your head!");
		Player_Dialog_Text_Status = 2;
		PDTS_time = 3;
		WeightBornStatus = 1;
		WeightSFXstatus = 1;
		TestTrack.WeightSFXname = TestTrack.Weight_Name;
		Create_Weight();
	}

	public void Create_Weight()
	{
		if ((WeightBornStatus == 1))
		{
			Vector3 local1 = new Vector3(
				0.0,
				20.0,
				((-(1.8)) * (Weight_Dist + Math.sqrt(player.car.getSpeedSquare())))
			);
			Vector3 local2 = player.car.getPos();
			Vector3 local3 = new Vector3((-(local2.x)), (-(local2.y)), (-(local2.z)));
			Ypr local4 = player.car.getOri();
			Ypr local5 = new Ypr(local4.y, 0.0, 0.0);
			local1.rotate(local5);
			local1.sub(local3);
			Weight[TestTrack.Weight_Name].setMatrix(local1, new Ypr(0.0, 0, 0));
			Weight[TestTrack.Weight_Name].wakeUp();
			if ((WeightBornStatus == 1))
			{
				if ((TestTrack.Weight_Name >= 4))
				{
					TestTrack.Weight_Name = 0;
				} else
				{
					TestTrack.Weight_Name += 1;
				}
				Survived += 1;
				Weight_Survived_Text.changeText(("Survived: " + Survived));
				Frequency = (System.simTime() + Weight_Time);
			}
		}
	}

	public void Elevator_Top()
	{
		if ((ElevatorStatus == 0))
		{
			Player_Dialog_Text.changeText("Normal Motion - Time: x1.0");
			Player_Dialog_Text_Status = 2;
			PDTS_time = 4;
			System.timeWarp(1.0);
			new SfxRef(DestructionTools:0x91r).play(ElevatorPlatformVector, 0.0, 1.0, 1.0, 0);
			Thread.sleep(7000);
			if ((cameraMode = Track.CAMMODE_FOLLOW))
			{
				changeCamTV();
				changeCamFollow();
			}
			ElevatorStatus = 3;
			ElevatorSpeed = (-(0.049999997));
			new SfxRef(DestructionTools:0x36r).stop(SFX_E_Loop_ID);
			new SfxRef(DestructionTools:0x87r).stop(SFX_E_Loop_ID);
			new SfxRef(
				DestructionTools:0x37r).play(ElevatorPlatformVector,
				0.0,
				System.timeWarp((-(1))),
				1.0,
				0
			);
			SFX_E_Loop_ID = new SfxRef(
				DestructionTools:0x36r).play(Elevator_Pos,
				0.0,
				(System.timeWarp((-(1))) - 0.5),
				1.0,
				0
			);
		} else
		{
			if ((ElevatorStatus == 2))
			{
				CarVector = player.car.getPos();
				if ((CarVector.y > 40))
				{
					Thread.sleep(5000);
					KeyCheck = 1;
					KeyCheckStat = player.car.getCruiseControl();
					Player_Dialog_Text.changeColor(-10752);
					Player_Dialog_Text.changeText("Drawing");
					new SfxRef(DestructionTools:0x39r).play(0.6, 1.0, 0);
					Thread.sleep(1500);
					Player_Dialog_Text.changeText(". Drawing .");
					new SfxRef(DestructionTools:0x39r).play(0.7, 1.0, 0);
					Thread.sleep(1400);
					Player_Dialog_Text.changeText(". . Drawing . .");
					new SfxRef(DestructionTools:0x39r).play(0.8, 1.0, 0);
					Thread.sleep(1300);
					Player_Dialog_Text.changeText(". . . Drawing . . .");
					new SfxRef(DestructionTools:0x39r).play(0.90000004, 1.0, 0);
					Thread.sleep(1200);
					OptionE = (KeyCheck - 2);
					if (((OptionE >= 1) && (OptionE <= 6)))
					{
						Player_Dialog_Text.changeText(("Chosen option: " + OptionE));
					} else
					{
						OptionE = (1 + (Math.random() * 6));
						Player_Dialog_Text.changeText(("Drawed option: " + OptionE));
					}
					Player_Dialog_Text_Status = 2;
					PDTS_time = 3.0;
					KeyCheck = 0;
					Thread.sleep(3000);
				} else
				{
					OptionE = 3;
					Player_Dialog_Text.changeText((("Option " + OptionE) + " activated!"));
					Player_Dialog_Text_Status = 2;
					PDTS_time = 3.0;
					Thread.sleep(3500);
				}
				if ((OptionE == 1))
				{
					RotateSpeed = 0.0019999999;
					RotateGoal = 0.8;
					RotateStart(1);
				} else
				{
					if ((OptionE == 2))
					{
						RotateSpeed = (-(0.0019999999));
						RotateGoal = (-(0.8));
						RotateStart(2);
					} else
					{
						if ((OptionE == 3))
						{
							if ((Weight_Created == 0))
							{
								Weight[0] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
								Weight[1] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
								Weight[2] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
								Weight[3] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
								Weight[4] = new Vehicle(map, DestructionTools:0x15r, 6, 2.0, 0.0, 1.0, 1.0);
								Weight_Created = 1;
								Weight[0].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
								Weight[1].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
								Weight[2].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
								Weight[3].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
								Weight[4].setMatrix(new Vector3(0.0, (-(1500.0)), 0.0), null);
							}
							Weight[TestTrack.Weight_Name].setMatrix(
								new Vector3(ElevatorPlatformVector.x, (ElevatorPlatformVector.y + 15), ElevatorPlatformVector.z),
								new Ypr((Math.random() * 3), 0, 0)
							);
							Weight[TestTrack.Weight_Name].wakeUp();
							TestTrack.WeightSFXname = TestTrack.Weight_Name;
							WeightSFXstatus = 1;
							if ((TestTrack.Weight_Name >= 4))
							{
								TestTrack.Weight_Name = 0;
							} else
							{
								TestTrack.Weight_Name += 1;
							}
							OptionE = (Math.random() * 10);
							Thread.sleep((5000 * TimX));
							WeightSFXstatus = 0;
							if ((OptionE < 5))
							{
								RotateSpeed = (-(0.0019999999));
								RotateGoal = (-(0.8));
								RotateStart(2);
							} else
							{
								RotateSpeed = 0.0019999999;
								RotateGoal = 0.8;
								RotateStart(1);
							}
							OptionE = 0;
						} else
						{
							if ((OptionE == 4))
							{
								if ((cameraMode != Track.CAMMODE_FOLLOW))
								{
									changeCamFollow();
								}
								cam.command((("move " + map.id()) + " 0,0,0 0.0"));
								cam.command((("look " + player.car.id()) + " 0,0,0 0,0,0"));
								cam.setMatrix(
									new Vector3((ElevatorPlatformVector.x + 6), (ElevatorPlatformVector.y + 1.5), (ElevatorPlatformVector.z + 6)),
									new Ypr(1.0, 0, 0)
								);
								cam.command("simulate 1");
								ElevatorSpeed = (-(0.03));
								ElevatorStatus = 4;
								player.car.wakeUp();
								new SfxRef(
									DestructionTools:0x37r).play(ElevatorPlatformVector,
									0.0,
									System.timeWarp((-(1))),
									1.0,
									0
								);
								new SfxRef(
									DestructionTools:0x37r).play(Elevator_Pos,
									0.0,
									System.timeWarp((-(1))),
									1.0,
									0
								);
								SFX_E_Loop_ID = new SfxRef(
									DestructionTools:0x36r).play(Elevator_Pos,
									0.0,
									(System.timeWarp((-(1))) - 0.5),
									1.0,
									0
								);
							} else
							{
								if ((OptionE == 5))
								{
									Player_Dialog_Text.changeColor(-1);
									Player_Dialog_Text.changeText("You have only one way to down... Jump");
									Player_Dialog_Text_Status = 2;
									PDTS_time = 3.0;
								} else
								{
									ElevatorSpeed = (-(0.02));
									ElevatorStatus = 3;
									player.car.wakeUp();
									new SfxRef(
										DestructionTools:0x35r).play(Elevator_Pos,
										0.0,
										System.timeWarp((-(1))),
										1.0,
										0
									);
									Thread.sleep((300 * TimX));
									new SfxRef(
										DestructionTools:0x37r).play(ElevatorPlatformVector,
										0.0,
										System.timeWarp((-(1))),
										1.0,
										0
									);
									SFX_E_Loop_ID = new SfxRef(
										DestructionTools:0x36r).play(Elevator_Pos,
										0.0,
										(System.timeWarp((-(1))) - 0.5),
										1.0,
										0
									);
								}
							}
						}
					}
				}
			}
		}
	}

	public void RotateStart(int arg0)
	{
		Platform.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		PlatformB.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
		ElevatorRotate = arg0;
		player.car.wakeUp();
		new SfxRef(DestructionTools:0x88r).play(ElevatorPlatformVector, 0.0, System.timeWarp((-(1))), 1.0, 0);
		SFX_E_Loop_ID = new SfxRef(
			DestructionTools:0x87r).play(ElevatorPlatformVector,
			0.0,
			(System.timeWarp((-(1))) - 0.4),
			1.0,
			0
		);
	}

	public void RotateEnd()
	{
		new SfxRef(DestructionTools:0x89r).play(ElevatorPlatformVector, 0.0, System.timeWarp((-(1))), 1.0, 0);
		new SfxRef(DestructionTools:0x87r).stop(SFX_E_Loop_ID);
		if ((ElevatorPlatformYpr.r > 0.1))
		{
			Thread.sleep((3000 * TimX));
			RotateSpeed = (-(0.0029999998));
			RotateGoal = 0.0;
			ElevatorRotate = 2;
			new SfxRef(
				DestructionTools:0x88r).play(ElevatorPlatformVector,
				0.0,
				System.timeWarp((-(1))),
				1.0,
				0
			);
			SFX_E_Loop_ID = new SfxRef(
				DestructionTools:0x87r).play(ElevatorPlatformVector,
				0.0,
				(System.timeWarp((-(1))) - 0.4),
				1.0,
				0
			);
		} else
		{
			if ((ElevatorPlatformYpr.r < (-(0.1))))
			{
				Thread.sleep((3000 * TimX));
				RotateSpeed = 0.0029999998;
				RotateGoal = 0.0;
				ElevatorRotate = 1;
				new SfxRef(
					DestructionTools:0x88r).play(ElevatorPlatformVector,
					0.0,
					System.timeWarp((-(1))),
					1.0,
					0
				);
				SFX_E_Loop_ID = new SfxRef(
					DestructionTools:0x87r).play(ElevatorPlatformVector,
					0.0,
					(System.timeWarp((-(1))) - 0.4),
					1.0,
					0
				);
			} else
			{
				EPPhys1.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys2.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys3.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys4.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys5.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys6.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys7.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys8.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys9.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys10.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys11.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys12.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys13.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys14.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys15.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys16.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys17.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				EPPhys18.setMatrix(new Vector3(0.0, (-(1000)), 0.0), null);
				PlatformPhysVector = new Vector3(
					ElevatorPlatformVector.x,
					(ElevatorPlatformVector.y + 1),
					ElevatorPlatformVector.z
				);
				PlatformBPhysVector = new Vector3(
					ElevatorPlatformVector.x,
					(ElevatorPlatformVector.y + 0.4),
					ElevatorPlatformVector.z
				);
				Platform.setMatrix(new Vector3(PlatformPhysVector), null);
				PlatformB.setMatrix(new Vector3(PlatformBPhysVector), null);
				Thread.sleep((3000 * TimX));
				ElevatorSpeed = (-(0.02));
				ElevatorStatus = 3;
				player.car.wakeUp();
				new SfxRef(
					DestructionTools:0x35r).play(ElevatorPlatformVector,
					0.0,
					System.timeWarp((-(1))),
					1.0,
					0
				);
				new SfxRef(DestructionTools:0x37r).play(Elevator_Pos, 0.0, System.timeWarp((-(1))), 1.0, 0);
				Thread.sleep((300 * TimX));
				SFX_E_Loop_ID = new SfxRef(
					DestructionTools:0x36r).play(Elevator_Pos,
					0.0,
					(System.timeWarp((-(1))) - 0.5),
					1.0,
					0
				);
			}
		}
	}

	public void event_handlerTrigger(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((local4 == player.car.id()))
		{
			if ((arg1 == GameRef.EVENT_TRIGGER_ON))
			{
				if ((!(activeTrigger)))
				{
					activeTrigger = arg0.id();
				}
			} else
			{
				activeTrigger = 0;
			}
		}
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		super.handleEvent(arg0, arg1, arg2);
		if ((arg1 == GameRef.EVENT_TIME))
		{
			if ((arg2 == 2))
			{
				if (activeTrigger)
				{
					if ((player.car.getSpeedSquare() < 0.25))
					{
						int local4 = 0;
						while ((local4 < rounds))
						{
							if (round[local4])
							{
								if ((activeTrigger == round[local4].trigger.elementAt(0).trigger.id()))
								{
									Ypr local5 = player.car.getOri();
									float local6 = (local5.y - round[local4].startOri.y);
									if ((local6 >= 3.1399999))
									{
										local6 = (6.2799997 - local6);
									}
									if ((local6 <= (-(3.1399999))))
									{
										local6 = ((-(6.2799997)) - local6);
									}
									if (((local6 < 0.5) && (local6 > (-(0.5)))))
									{
										actRound = round[local4];
										testStatus = 0;
										triggerIndex = 1;
										if ((actRound.type == 0))
										{
											acceleration = 1;
											acceleration120 = 1;
										} else
										{
											acceleration = 0;
											acceleration120 = 0;
										}
									}
								}
							}
							local4++;
						}
					}
				}
			}
		}
	}

	public void animate()
	{
		super.animate();
		if ((KeyCheck > 0))
		{
			if ((player.car.getCruiseControl() == 1))
			{
				if ((KeyCheckStat == 1))
				{
					KeyCheck += 1;
					KeyCheckStat = 0;
				}
			} else
			{
				if ((KeyCheckStat == 0))
				{
					KeyCheck += 1;
					KeyCheckStat = 1;
				}
			}
		}
		if ((MarkerVector.y > (WCH_Start.y + 0.1)))
		{
			X = (-(0.005));
		} else
		{
			if ((MarkerVector.y < (WCH_Start.y - 0.1)))
			{
				X = 0.005;
			}
		}
		MarkerVector.y += X;
		WMarker.setMatrix(new Vector3(MarkerVector), null);
		Ypr local1 = new Ypr((System.currentTime() * 3), 0.0, 0.0);
		WMarkerF.setMatrix(new Vector3(WCH_Start), new Ypr(local1));
		CalcSpeed = (ElevatorSpeed * System.timeWarp((-(1))));
		CalcRotSpeed = (RotateSpeed * System.timeWarp((-(1))));
		if ((System.timeWarp((-(1))) == 0))
		{
			TimX = 0.0069999993;
		} else
		{
			TimX = (1 / System.timeWarp((-(1))));
		}
		if ((Player_Dialog_Text_Status == 2))
		{
			new SfxRef(DestructionTools:0x39r).play();
			PDTS_time_start = System.simTime();
			Player_Dialog_Text_Status = 1;
		} else
		{
			if ((Player_Dialog_Text_Status == 1))
			{
				if (((System.simTime() - PDTS_time_start) >= PDTS_time))
				{
					Player_Dialog_Text.changeText("");
					Player_Dialog_Text.changeColor(-1);
					Player_Dialog_Text_Status = 0;
				}
			}
		}
		if ((LightsTimer <= System.simTime()))
		{
			if ((Light1 && Light4))
			{
				Light1.destroy();
				Light1 = null;
				Light2.destroy();
				Light2 = null;
				Light3.destroy();
				Light3 = null;
				Light4.destroy();
				Light4 = null;
				player.car.wakeUp();
			}
			if (((LightsStatus == 0) && (Lights == 1)))
			{
				Light1 = new RenderRef(map, DestructionTools:0x82r, "Light");
				Light2 = new RenderRef(map, DestructionTools:0x82r, "Light");
				Light3 = new RenderRef(map, DestructionTools:0x82r, "Light");
				Light4 = new RenderRef(map, DestructionTools:0x82r, "Light");
				LightsStatus = 1;
				LightsTimer = (System.simTime() + 0.049999997);
			} else
			{
				if ((LightsStatus == 1))
				{
					Light1 = new RenderRef(map, DestructionTools:0x83r, "Light");
					Light2 = new RenderRef(map, DestructionTools:0x83r, "Light");
					Light3 = new RenderRef(map, DestructionTools:0x83r, "Light");
					Light4 = new RenderRef(map, DestructionTools:0x83r, "Light");
					LightsStatus = 2;
					LightsTimer = (System.simTime() + 0.049999997);
				} else
				{
					if ((LightsStatus == 2))
					{
						Light1 = new RenderRef(map, DestructionTools:0x84r, "Light");
						Light2 = new RenderRef(map, DestructionTools:0x84r, "Light");
						Light3 = new RenderRef(map, DestructionTools:0x84r, "Light");
						Light4 = new RenderRef(map, DestructionTools:0x84r, "Light");
						LightsStatus = 3;
						LightsTimer = (System.simTime() + 1.5);
					} else
					{
						if ((LightsStatus == 3))
						{
							Light1 = new RenderRef(map, DestructionTools:0x83r, "Light");
							Light2 = new RenderRef(map, DestructionTools:0x83r, "Light");
							Light3 = new RenderRef(map, DestructionTools:0x83r, "Light");
							Light4 = new RenderRef(map, DestructionTools:0x83r, "Light");
							LightsStatus = 4;
							LightsTimer = (System.simTime() + 0.049999997);
						} else
						{
							if ((LightsStatus == 4))
							{
								Light1 = new RenderRef(map, DestructionTools:0x82r, "Light");
								Light2 = new RenderRef(map, DestructionTools:0x82r, "Light");
								Light3 = new RenderRef(map, DestructionTools:0x82r, "Light");
								Light4 = new RenderRef(map, DestructionTools:0x82r, "Light");
								LightsStatus = 5;
								LightsTimer = (System.simTime() + 0.049999997);
							} else
							{
								if ((LightsStatus == 5))
								{
									LightsStatus = 0;
									LightsTimer = (System.simTime() + 1.5);
								}
							}
						}
					}
				}
			}
			if ((((((ElevatorStatus == 0) && Light1) && Light4) && (ElevatorRotate == 0)) || ((((ElevatorStatus == 2) && Light1) && Light4) && (ElevatorRotate == 0))))
			{
				Light1.setMatrix(Light1Pos, null);
				Light2.setMatrix(Light2Pos, null);
				Light3.setMatrix(Light3Pos, null);
				Light4.setMatrix(Light4Pos, null);
			}
		}
		if ((WeightSFXstatus == 1))
		{
			WeightSFXspeed = Weight[TestTrack.WeightSFXname].getSpeedSquare();
			if ((WeightSFXbuffor == 0))
			{
				if ((WeightSFXspeed > 35))
				{
					WeightSFXbuffor = 1;
				}
			} else
			{
				if ((WeightSFXspeed < 30))
				{
					WeightSFXpos = Weight[TestTrack.WeightSFXname].getPos();
					new SfxRef(
						DestructionTools:0x81r).play(new Vector3(WeightSFXpos),
						0.0,
						System.timeWarp((-(1))),
						1.0,
						0
					);
					WeightSFXbuffor = 0;
					if ((TestTrack.WeightSFXname >= 4))
					{
						TestTrack.WeightSFXname = 0;
					} else
					{
						TestTrack.WeightSFXname += 1;
					}
				}
			}
		}
		if (((ElevatorStatus == 2) && (ElevatorRotate > 0)))
		{
			ElevatorPlatformYpr.r += CalcRotSpeed;
			ElevatorPlatformVectorSub = new Vector3(
				(-(ElevatorPlatformVector.x)),
				(-(ElevatorPlatformVector.y)),
				(-(ElevatorPlatformVector.z))
			);
			Light1Pos = new Vector3(1.9200001, 1, 4.92);
			Light2Pos = new Vector3(1.9200001, 1, (-(4.92)));
			Light3Pos = new Vector3((-(1.9200001)), 1, 4.92);
			Light4Pos = new Vector3((-(1.9200001)), 1, (-(4.92)));
			EPPhysPos[0] = new Vector3(1.8199999, 1.0, 0.0);
			EPPhysPos[1] = new Vector3(1.62, 1.0, 0.0);
			EPPhysPos[2] = new Vector3(1.41, 1.0, 0.0);
			EPPhysPos[3] = new Vector3(1.21, 1.0, 0.0);
			EPPhysPos[4] = new Vector3(1.0, 1.0, 0.0);
			EPPhysPos[5] = new Vector3(0.8, 1.0, 0.0);
			EPPhysPos[6] = new Vector3(0.6, 1.0, 0.0);
			EPPhysPos[7] = new Vector3(0.4, 1.0, 0.0);
			EPPhysPos[8] = new Vector3(0.2, 1.0, 0.0);
			EPPhysPos[9] = new Vector3((-(0.2)), 1.0, 0.0);
			EPPhysPos[10] = new Vector3((-(0.4)), 1.0, 0.0);
			EPPhysPos[11] = new Vector3((-(0.6)), 1.0, 0.0);
			EPPhysPos[12] = new Vector3((-(0.8)), 1.0, 0.0);
			EPPhysPos[13] = new Vector3((-(1.0)), 1.0, 0.0);
			EPPhysPos[14] = new Vector3((-(1.21)), 1.0, 0.0);
			EPPhysPos[15] = new Vector3((-(1.41)), 1.0, 0.0);
			EPPhysPos[16] = new Vector3((-(1.62)), 1.0, 0.0);
			EPPhysPos[17] = new Vector3((-(1.8199999)), 1.0, 0.0);
			EPPhysPos[0].rotate(ElevatorPlatformYpr);
			EPPhysPos[1].rotate(ElevatorPlatformYpr);
			EPPhysPos[2].rotate(ElevatorPlatformYpr);
			EPPhysPos[3].rotate(ElevatorPlatformYpr);
			EPPhysPos[4].rotate(ElevatorPlatformYpr);
			EPPhysPos[5].rotate(ElevatorPlatformYpr);
			EPPhysPos[6].rotate(ElevatorPlatformYpr);
			EPPhysPos[7].rotate(ElevatorPlatformYpr);
			EPPhysPos[8].rotate(ElevatorPlatformYpr);
			EPPhysPos[9].rotate(ElevatorPlatformYpr);
			EPPhysPos[10].rotate(ElevatorPlatformYpr);
			EPPhysPos[11].rotate(ElevatorPlatformYpr);
			EPPhysPos[12].rotate(ElevatorPlatformYpr);
			EPPhysPos[13].rotate(ElevatorPlatformYpr);
			EPPhysPos[14].rotate(ElevatorPlatformYpr);
			EPPhysPos[15].rotate(ElevatorPlatformYpr);
			EPPhysPos[16].rotate(ElevatorPlatformYpr);
			EPPhysPos[17].rotate(ElevatorPlatformYpr);
			EPPhysPos[0].sub(ElevatorPlatformVectorSub);
			EPPhysPos[1].sub(ElevatorPlatformVectorSub);
			EPPhysPos[2].sub(ElevatorPlatformVectorSub);
			EPPhysPos[3].sub(ElevatorPlatformVectorSub);
			EPPhysPos[4].sub(ElevatorPlatformVectorSub);
			EPPhysPos[5].sub(ElevatorPlatformVectorSub);
			EPPhysPos[6].sub(ElevatorPlatformVectorSub);
			EPPhysPos[7].sub(ElevatorPlatformVectorSub);
			EPPhysPos[8].sub(ElevatorPlatformVectorSub);
			EPPhysPos[9].sub(ElevatorPlatformVectorSub);
			EPPhysPos[10].sub(ElevatorPlatformVectorSub);
			EPPhysPos[11].sub(ElevatorPlatformVectorSub);
			EPPhysPos[12].sub(ElevatorPlatformVectorSub);
			EPPhysPos[13].sub(ElevatorPlatformVectorSub);
			EPPhysPos[14].sub(ElevatorPlatformVectorSub);
			EPPhysPos[15].sub(ElevatorPlatformVectorSub);
			EPPhysPos[16].sub(ElevatorPlatformVectorSub);
			EPPhysPos[17].sub(ElevatorPlatformVectorSub);
			EPPhys1.setMatrix(new Vector3(EPPhysPos[0]), null);
			EPPhys2.setMatrix(new Vector3(EPPhysPos[1]), null);
			EPPhys3.setMatrix(new Vector3(EPPhysPos[2]), null);
			EPPhys4.setMatrix(new Vector3(EPPhysPos[3]), null);
			EPPhys5.setMatrix(new Vector3(EPPhysPos[4]), null);
			EPPhys6.setMatrix(new Vector3(EPPhysPos[5]), null);
			EPPhys7.setMatrix(new Vector3(EPPhysPos[6]), null);
			EPPhys8.setMatrix(new Vector3(EPPhysPos[7]), null);
			EPPhys9.setMatrix(new Vector3(EPPhysPos[8]), null);
			EPPhys10.setMatrix(new Vector3(EPPhysPos[9]), null);
			EPPhys11.setMatrix(new Vector3(EPPhysPos[10]), null);
			EPPhys12.setMatrix(new Vector3(EPPhysPos[11]), null);
			EPPhys13.setMatrix(new Vector3(EPPhysPos[12]), null);
			EPPhys14.setMatrix(new Vector3(EPPhysPos[13]), null);
			EPPhys15.setMatrix(new Vector3(EPPhysPos[14]), null);
			EPPhys16.setMatrix(new Vector3(EPPhysPos[15]), null);
			EPPhys17.setMatrix(new Vector3(EPPhysPos[16]), null);
			EPPhys18.setMatrix(new Vector3(EPPhysPos[17]), null);
			Light1Pos.rotate(ElevatorPlatformYpr);
			Light2Pos.rotate(ElevatorPlatformYpr);
			Light3Pos.rotate(ElevatorPlatformYpr);
			Light4Pos.rotate(ElevatorPlatformYpr);
			Light1Pos.sub(ElevatorPlatformVectorSub);
			Light2Pos.sub(ElevatorPlatformVectorSub);
			Light3Pos.sub(ElevatorPlatformVectorSub);
			Light4Pos.sub(ElevatorPlatformVectorSub);
			if ((Light1 && Light4))
			{
				Light1.setMatrix(Light1Pos, null);
				Light2.setMatrix(Light2Pos, null);
				Light3.setMatrix(Light3Pos, null);
				Light4.setMatrix(Light4Pos, null);
			}
			Elevator_Platform.setMatrix(new Vector3(ElevatorPlatformVector), new Ypr(ElevatorPlatformYpr));
			if (((ElevatorRotate == 1) && (ElevatorPlatformYpr.r >= RotateGoal)))
			{
				ElevatorRotate = 0;
				RotateEnd();
			} else
			{
				if (((ElevatorRotate == 2) && (ElevatorPlatformYpr.r <= RotateGoal)))
				{
					ElevatorRotate = 0;
					RotateEnd();
				}
			}
		} else
		{
			if (((((ElevatorStatus == 1) || (ElevatorStatus == 3)) || (ElevatorStatus == 4)) || (ElevatorStatus == 5)))
			{
				ElevatorPlatformVector.y += CalcSpeed;
				PlatformPhysVector.y += CalcSpeed;
				PlatformBPhysVector.y += CalcSpeed;
				ElevatorArmVector.y += CalcSpeed;
				ArmPhysVector.y += CalcSpeed;
				Light1Pos.y += CalcSpeed;
				Light2Pos.y += CalcSpeed;
				Light3Pos.y += CalcSpeed;
				Light4Pos.y += CalcSpeed;
				Elevator_Platform.setMatrix(new Vector3(ElevatorPlatformVector), null);
				Platform.setMatrix(new Vector3(PlatformPhysVector), null);
				PlatformB.setMatrix(new Vector3(PlatformBPhysVector), null);
				Elevator_Arm.setMatrix(new Vector3(ElevatorArmVector), null);
				Arm.setMatrix(new Vector3(ArmPhysVector), null);
				if ((Light1 && Light4))
				{
					Light1.setMatrix(Light1Pos, null);
					Light2.setMatrix(Light2Pos, null);
					Light3.setMatrix(Light3Pos, null);
					Light4.setMatrix(Light4Pos, null);
				}
				Level = (ElevatorPlatformVector.y / 2.3);
				Level_Info.changeText(("Level: " + Level));
				if ((ElevatorStatus == 1))
				{
					if (((ElevatorPlatformVector.y > (-(0.6))) && (ElevatorPlatformVector.y < (-(0.5)))))
					{
						ElevatorSpeed = 0.015;
					} else
					{
						if (((ElevatorPlatformVector.y > (-(0.4))) && (ElevatorPlatformVector.y < (-(0.3)))))
						{
							ElevatorSpeed = 0.02;
						} else
						{
							if (((ElevatorPlatformVector.y > 0.0) && (ElevatorPlatformVector.y < 0.1)))
							{
								ElevatorSpeed = 0.03;
							} else
							{
								if (((ElevatorPlatformVector.y > 0.8) && (ElevatorPlatformVector.y < 0.90000004)))
								{
									ElevatorSpeed = 0.04;
								} else
								{
									if (((ElevatorPlatformVector.y > 1.6) && (ElevatorPlatformVector.y < 1.7)))
									{
										ElevatorSpeed = 0.049999997;
									} else
									{
										if ((ElevatorPlatformVector.y > 70))
										{
											ElevatorStatus = 2;
											new SfxRef(
												DestructionTools:0x37r).play(ElevatorPlatformVector,
												0.0,
												System.timeWarp((-(1))),
												1.0,
												0
											);
											new SfxRef(DestructionTools:0x36r).stop(SFX_E_Loop_ID);
											Elevator_Top();
										}
									}
								}
							}
						}
					}
				} else
				{
					if ((ElevatorStatus == 3))
					{
						if (((ElevatorPlatformVector.y > 69.7) && (ElevatorPlatformVector.y < 69.8)))
						{
							ElevatorSpeed = (-(0.03));
						} else
						{
							if (((ElevatorPlatformVector.y > 69.3) && (ElevatorPlatformVector.y < 69.4)))
							{
								ElevatorSpeed = (-(0.04));
							} else
							{
								if (((ElevatorPlatformVector.y > 68.5) && (ElevatorPlatformVector.y < 68.6)))
								{
									ElevatorSpeed = (-(0.049999997));
								} else
								{
									if (((ElevatorPlatformVector.y > 3.0) && (ElevatorPlatformVector.y < 3.1)))
									{
										ElevatorSpeed = (-(0.04));
									} else
									{
										if (((ElevatorPlatformVector.y > 2.5) && (ElevatorPlatformVector.y < 2.6)))
										{
											ElevatorSpeed = (-(0.03));
										} else
										{
											if (((ElevatorPlatformVector.y > 2.0) && (ElevatorPlatformVector.y < 2.1)))
											{
												ElevatorSpeed = (-(0.02));
											} else
											{
												if (((ElevatorPlatformVector.y > 1.5) && (ElevatorPlatformVector.y < 1.6)))
												{
													ElevatorSpeed = (-(0.01));
												} else
												{
													if ((ElevatorPlatformVector.y <= Elevator_Pos.y))
													{
														ElevatorStatus = 0;
														new SfxRef(
															DestructionTools:0x90r).play(ElevatorPlatformVector,
															0.0,
															System.timeWarp((-(1))),
															1.0,
															0
														);
														new SfxRef(DestructionTools:0x36r).stop(SFX_E_Loop_ID);
														Level_Info.changeText("");
														Level_Info_Frame.finalize();
														Thread.sleep((4000 * TimX));
														Lights = 0;
														player.car.wakeUp();
													}
												}
											}
										}
									}
								}
							}
						}
					} else
					{
						if ((ElevatorStatus == 4))
						{
							if ((ElevatorPlatformVector.y <= 64.0))
							{
								CarVector = player.car.getPos();
								if ((CarVector.y > 40))
								{
									ElevatorStatus = 0;
									new SfxRef(
										DestructionTools:0x37r).play(ElevatorPlatformVector,
										0.0,
										System.timeWarp((-(1))),
										1.0,
										0
									);
									new SfxRef(
										DestructionTools:0x37r).play(Elevator_Pos,
										0.0,
										System.timeWarp((-(1))),
										1.0,
										0
									);
									new SfxRef(DestructionTools:0x36r).stop(SFX_E_Loop_ID);
									Thread.sleep(1000);
									Player_Dialog_Text.changeColor(-10752);
									Player_Dialog_Text.changeText("Slow Motion - Time: x0.015");
									Player_Dialog_Text_Status = 2;
									PDTS_time = 3;
									System.timeWarp(0.015);
									Thread.sleep(1000);
									ElevatorSpeed = 0.5;
									ElevatorStatus = 5;
									player.car.wakeUp();
								} else
								{
									ElevatorSpeed = (-(0.07));
									ElevatorStatus = 3;
								}
							}
						} else
						{
							if ((ElevatorStatus == 5))
							{
								if (((ElevatorPlatformVector.y > 65.0) && (ElevatorPlatformVector.y < 65.1)))
								{
									ElevatorSpeed = 1.0;
								} else
								{
									if (((ElevatorPlatformVector.y > 65.5) && (ElevatorPlatformVector.y < 65.6)))
									{
										ElevatorSpeed = 1.4;
									} else
									{
										if (((ElevatorPlatformVector.y > 66.0) && (ElevatorPlatformVector.y < 66.1)))
										{
											ElevatorSpeed = 2.3;
										} else
										{
											if (((ElevatorPlatformVector.y > 67.0) && (ElevatorPlatformVector.y < 67.1)))
											{
												ElevatorSpeed = 3.2;
											} else
											{
												if (((ElevatorPlatformVector.y > 68.0) && (ElevatorPlatformVector.y < 68.1)))
												{
													ElevatorSpeed = 4.1;
												} else
												{
													if (((ElevatorPlatformVector.y > 69.0) && (ElevatorPlatformVector.y < 69.1)))
													{
														ElevatorSpeed = 5.0;
													} else
													{
														if ((ElevatorPlatformVector.y >= 70.0))
														{
															ElevatorStatus = 0;
															Elevator_Top();
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
			} else
			{
				if ((WeightBornStatus == 1))
				{
					if ((Frequency <= System.simTime()))
					{
						Create_Weight();
					}
					if ((Survived == SurvivedGoal))
					{
						WCH_Level += 1;
						if ((Survived >= 175))
						{
							if ((LastLevel == 0))
							{
								SurvivedGoal += 6;
								LastLevel = 1;
								if ((Weight_Time >= 1.3))
								{
									Weight_Time -= 0.3;
								}
								Weight_Level_Text.changeText(("Level: " + WCH_Level));
								Weight_Survived_Text.changeText(("Survived: " + Survived));
								Weight_Frequency_Text.changeText(Float.toString(Weight_Time, "Frequency: %1.1f s"));
								Weight_Distance_Text.changeText(Float.toString(Weight_Dist, "Initial distance: %1.1f m"));
								Player_Dialog_Text.changeColor(-65536);
								Player_Dialog_Text.changeText((("You survived to the Last Level ( " + WCH_Level) + " ) !"));
								new SfxRef(DestructionTools:0x39r).play();
							} else
							{
								if ((LastLevel == 1))
								{
									SurvivedGoal += 1;
									Weight_Survived_Text.changeText(("Survived: " + Survived));
									Player_Dialog_Text.changeColor(-65536);
									Player_Dialog_Text.changeText((("Only " + (275 - Survived)) + " Weights left!"));
									if (((275 - Survived) <= 0))
									{
										WeightBornStatus = 0;
										enableOsd(0);
										if ((cameraMode != Track.CAMMODE_FOLLOW))
										{
											changeCamFollow();
										}
										Thread.sleep((1500 * TimX));
										Player_Dialog_Text.changeColor(-10752);
										Player_Dialog_Text.changeText("Well Done! You finish The Weight Challenge!");
										new SfxRef(sound:0x22r).play();
										Thread.sleep((2000 * TimX));
										player.car.command("brake");
										cam.command("angle 0 5.0 0.4853");
										new SfxRef(sound:0x22r).play();
										Thread.sleep(10000);
										Weight_Text.changeText("");
										Weight_Level_Text.changeText("");
										Weight_Survived_Text.changeText("");
										Weight_Frequency_Text.changeText("");
										Weight_Distance_Text.changeText("");
										Info_Strip.finalize();
										Vector3 local2 = player.car.getPos();
										cam.command((("move " + player.car.id()) + " 10,20,0 3.0"));
										cam.command((((("look " + map.id()) + " ") + local2.toString()) + " 0,0,0"));
										Sound.changeMusicSet(Sound.MUSIC_SET_NONE);
										Player_Dialog_Text.changeText("But don't think Your car can't be float!");
										new SfxRef(DestructionTools:0x39r).play();
										Thread.sleep(2000);
										local2.y += 20;
										Weight[TestTrack.Weight_Name].setMatrix(
											local2,
											new Ypr((Math.random() * 3.0), 0.0, 0.0)
										);
										Weight[TestTrack.Weight_Name].wakeUp();
										WeightSFXstatus = 1;
										if ((TestTrack.Weight_Name >= 4))
										{
											TestTrack.Weight_Name = 0;
										} else
										{
											TestTrack.Weight_Name += 1;
										}
										Thread.sleep((1200 * TimX));
										Vector3 local3 = cam.getPos();
										cam.command((((("move " + map.id()) + " ") + local3.toString()) + " 0.0"));
										local2.y -= 20;
										cam.command((((("look " + map.id()) + " ") + local2.toString()) + " 0,0,0"));
										player.car.setMatrix(
											new Vector3(101.3, 0.1, 153.2),
											new Ypr(0.4, 0.0, 0.0)
										);
										if (nav)
										{
											nav.remMarker(mPlayer);
										}
										Thread.sleep(2000);
										WeightSFXstatus = 0;
										player.car.wakeUp();
										player.car.command("reset");
										player.car.command("setsteer -0.7");
										PrimeWin = 1;
										prime = Vehicle.load("DestructionTools/Scripts/Muscle", map);
										prime.setMatrix(new Vector3(171.7, 0.1, 129.2), new Ypr(2.1, 0.0, 0.0));
										prime.wakeUp();
										prime.command("reload");
										prime.command("setsteer -0.7");
										Thread.sleep(3000);
										Sound.changeMusicSet(Sound.MUSIC_SET_DRIVING);
										Player_Dialog_Text.changeText("OK... it's only JOKE");
										new SfxRef(DestructionTools:0x39r).play();
										cam.command((("look " + player.car.id()) + " 0,0,0 0,0,0"));
										cam.setMatrix(new Vector3(80, 6, 148), new Ypr((-(1.7)), 0.0, 0.0));
										cam.command("zoom 15 0.5");
										Thread.sleep(7000);
										player.car.command("start");
										Player_Dialog_Text.changeText("You win this Prime DHL 500 !!");
										Player_Dialog_Text_Status = 2;
										PDTS_time = 6;
										new SfxRef(sound:0x22r).play();
										enableOsd(1);
										player.controller.command(("leave " + player.car.id()));
										player.car.setParent(map);
										prime.setParent(player);
										prime.setParent(map);
										player.controller.command(("controllable " + prime.id()));
										changeCamTarget(prime);
										changeCamFollow();
										map.command(("obs_add " + prime.id()));
										if (nav)
										{
											mPlayer = nav.addMarker(player);
										}
									}
								}
							}
						} else
						{
							if ((Survived >= 115))
							{
								SurvivedGoal += 30;
								if ((Weight_Time >= 1.3))
								{
									Weight_Time -= 0.3;
								}
								Weight_Level_Text.changeText(("Level: " + WCH_Level));
								Weight_Survived_Text.changeText(("Survived: " + Survived));
								Weight_Frequency_Text.changeText(Float.toString(Weight_Time, "Frequency: %1.1f s"));
								Weight_Distance_Text.changeText(Float.toString(Weight_Dist, "Initial distance: %1.1f m"));
								Player_Dialog_Text.changeColor(-32256);
								Player_Dialog_Text.changeText((("You advanced to Level " + WCH_Level) + " !"));
								Player_Dialog_Text_Status = 2;
								PDTS_time = 6;
							} else
							{
								if ((Survived >= 40))
								{
									SurvivedGoal += 25;
									if ((Weight_Time >= 1.3))
									{
										Weight_Time -= 0.3;
									}
									Weight_Level_Text.changeText(("Level: " + WCH_Level));
									Weight_Survived_Text.changeText(("Survived: " + Survived));
									Weight_Frequency_Text.changeText(Float.toString(Weight_Time, "Frequency: %1.1f s"));
									Weight_Distance_Text.changeText(Float.toString(Weight_Dist, "Initial distance: %1.1f m"));
									Player_Dialog_Text.changeColor(-10752);
									Player_Dialog_Text.changeText((("You advanced to Level " + WCH_Level) + " !"));
									Player_Dialog_Text_Status = 2;
									PDTS_time = 6;
								} else
								{
									if ((Survived == 25))
									{
										SurvivedGoal += 15;
										if ((Weight_Dist > 0))
										{
											Weight_Dist -= 3.0;
										}
										if ((Weight_Time >= 1.3))
										{
											Weight_Time -= 0.3;
										}
										Weight_Level_Text.changeText(("Level: " + WCH_Level));
										Weight_Survived_Text.changeText(("Survived: " + Survived));
										Weight_Frequency_Text.changeText(Float.toString(Weight_Time, "Frequency: %1.1f s"));
										Weight_Distance_Text.changeText(Float.toString(Weight_Dist, "Initial distance: %1.1f m"));
										Player_Dialog_Text.changeColor(-8192256);
										Player_Dialog_Text.changeText((("Level " + WCH_Level) + " ! End of fun! Now Initial Distance is 0 m"));
										Player_Dialog_Text_Status = 2;
										PDTS_time = 8;
									} else
									{
										SurvivedGoal += 10;
										if ((Weight_Time >= 1.3))
										{
											Weight_Time -= 0.3;
										}
										if ((Weight_Dist > 0))
										{
											Weight_Dist -= 3.0;
										}
										Weight_Level_Text.changeText(("Level: " + WCH_Level));
										Weight_Survived_Text.changeText(("Survived: " + Survived));
										Weight_Frequency_Text.changeText(Float.toString(Weight_Time, "Frequency: %1.1f s"));
										Weight_Distance_Text.changeText(Float.toString(Weight_Dist, "Initial distance: %1.1f m"));
										Player_Dialog_Text.changeColor(-16711936);
										Player_Dialog_Text.changeText((("You advanced to Level " + WCH_Level) + " !"));
										Player_Dialog_Text_Status = 2;
										PDTS_time = 6;
									}
								}
							}
						}
					}
					if ((WeightBornStatus == 1))
					{
						if ((player.car.getSpeedSquare() < 126.5))
						{
							if ((ToSlow == 0))
							{
								Player_Dialog_Text.changeColor(-65536);
								Player_Dialog_Text.changeText("More speed!!");
								Player_Dialog_Text_Status = 2;
								PDTS_time = 1;
								ToSlow = 1;
								ToSlowTime = (System.simTime() + 3);
							} else
							{
								Player_Dialog_Text.changeColor(-65536);
								Player_Dialog_Text.changeText("More speed!!");
								PDTS_time_start = System.simTime();
								Player_Dialog_Text_Status = 1;
								PDTS_time = 1;
								if ((ToSlowTime <= System.simTime()))
								{
									String local2_2 = player.car.isDriveable();
									if (local2_2)
									{
										Player_Dialog_Text.changeColor(-10752);
										Player_Dialog_Text.changeText("End of Challenge. You busted!");
									} else
									{
										Player_Dialog_Text.changeColor(-10752);
										Player_Dialog_Text.changeText("End of Challenge. To slow!");
									}
									ToSlow = 0;
									Player_Dialog_Text_Status = 2;
									PDTS_time = 7;
									WeightBornStatus = 0;
									WeightSFXstatus = 0;
									Sound.changeMusicSet(Sound.MUSIC_SET_DRIVING);
									Thread.sleep(7000);
									Weight_Text.changeText("");
									Weight_Level_Text.changeText("");
									Weight_Survived_Text.changeText("");
									Weight_Frequency_Text.changeText("");
									Weight_Distance_Text.changeText("");
									Info_Strip.finalize();
								}
							}
						} else
						{
							if ((player.car.getSpeedSquare() < 193.6))
							{
								if ((Player_Dialog_Text_Status == 0))
								{
									Player_Dialog_Text.changeColor(-256);
									Player_Dialog_Text.changeText("More speed!");
									Player_Dialog_Text_Status = 2;
									PDTS_time = 1;
									ToSlow = 0;
								} else
								{
									if ((Player_Dialog_Text_Status > 0))
									{
										Player_Dialog_Text.changeColor(-256);
										Player_Dialog_Text.changeText("More speed!");
										PDTS_time_start = System.simTime();
										Player_Dialog_Text_Status = 1;
										PDTS_time = 1;
										ToSlow = 0;
									}
								}
							}
						}
					}
				} else
				{
					if (actRound)
					{
						float local2_3 = player.car.getSpeedSquare();
						if ((player.car.maxTestTrackSpeedSq < local2_3))
						{
							player.car.maxTestTrackSpeedSq = local2_3;
						}
						if ((testStatus == 0))
						{
							messageTextA.changeText(actRound.messageA);
							messageTextB.changeText(actRound.messageB);
							messageText0.changeText("");
							messageText1.changeText("");
							messageText2.changeText("");
							testStatus = 1;
						}
						if ((testStatus == 1))
						{
							if ((local2_3 > 0.25))
							{
								messageTextA.changeText("");
								messageTextB.changeText("");
								messageText0.changeText("Time: ");
								testStartTime = System.simTime();
								testStatus = 2;
								Sound.changeMusicSet(Sound.MUSIC_SET_RACE);
							}
						}
						if ((testStatus == 2))
						{
							float local3_2 = (System.simTime() - testStartTime);
							messageText1.changeText((String.timeToString(local3_2, String.TCF_NOHOURS) + " s"));
							if (acceleration)
							{
								if ((local2_3 >= TestTrack.MPH_60))
								{
									messageText2.changeText((("Acceleration 0-100 KPH (0-62.1 MPH): " + String.timeToString(local3_2, String.TCF_NOHOURS)) + " s"));
									acceleration = 0;
									if (((player.car.bestTestTrackAcc > local3_2) || (player.car.bestTestTrackAcc < 0.1)))
									{
										player.car.bestTestTrackAcc = local3_2;
									}
								}
							}
							if (acceleration120)
							{
								if ((local2_3 >= TestTrack.MPH_124))
								{
									messageText2.changeText((("Acceleration 0-200 KPH (0-124.2 MPH): " + String.timeToString(local3_2, String.TCF_NOHOURS)) + " s"));
									acceleration120 = 0;
									if (((player.car.bestTestTrackAcc120 > local3_2) || (player.car.bestTestTrackAcc120 < 0.1)))
									{
										player.car.bestTestTrackAcc120 = local3_2;
									}
								}
							}
							if (activeTrigger)
							{
								Trigger local4 = actRound.trigger.elementAt(triggerIndex);
								if ((activeTrigger == local4.trigger.id()))
								{
									if ((triggerIndex == 1))
									{
										if ((actRound.type == 0))
										{
											messageText2.changeText((("1/4 mile time: " + String.timeToString(local3_2, String.TCF_NOHOURS)) + " s"));
											if (((player.car.bestTestTrackTime2 > local3_2) || (player.car.bestTestTrackTime2 < 0.1)))
											{
												player.car.bestTestTrackTime2_speedSq = local2_3;
												player.car.bestTestTrackTime2 = local3_2;
											}
										}
									}
									triggerIndex++;
									if ((triggerIndex == actRound.trigger.size()))
									{
										if ((actRound.type == 0))
										{
											messageText2.changeText((("1 mile time: " + String.timeToString(local3_2, String.TCF_NOHOURS)) + " s"));
											if (((player.car.bestTestTrackTime0 > local3_2) || (player.car.bestTestTrackTime0 < 0.1)))
											{
												player.car.bestTestTrackTime0_speedSq = local2_3;
												player.car.bestTestTrackTime0 = local3_2;
											}
										} else
										{
											if (((player.car.bestTestTrackTime1 > local3_2) || (player.car.bestTestTrackTime1 < 0.1)))
											{
												player.car.bestTestTrackTime1 = local3_2;
											}
										}
										actRound = null;
										Sound.changeMusicSet(Sound.MUSIC_SET_DRIVING);
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
