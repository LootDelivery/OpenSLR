package java.game;

import java.util.resource.*;

public class Roc
{
	static final int COMPETITORS = 16;
	static final int ROUNDS = 4;
	static final String carSaveDir = "cars/racers/ROC_cars/";
	static final String[] roundNames = new String[Roc.ROUNDS];
	Object[] rounds = new Object[Roc.ROUNDS];
	String[] carNames = new String[Roc.ROUNDS];
	String[] splineNames = new String[Roc.ROUNDS];
	String[] aiParams = new String[Roc.ROUNDS];
	int actRound;
	int lostRuns;
	int wonRuns;
	int init = 1;

	public Roc(Player arg0)
	{
		super();
		Roc.roundNames[0] = "final";
		Roc.roundNames[1] = "semi finals";
		Roc.roundNames[2] = "quarter-finals";
		Roc.roundNames[3] = "qualifying rounds";
		Racer[] local2;
		int local3 = Roc.COMPETITORS;
		int local4 = 0;
		while ((local4 < Roc.ROUNDS))
		{
			local2 = new Racer[local3];
			rounds[local4] = local2;
			local3 /= 2;
			local4++;
		}
		int local4_2;
		if ((Math.random() < 0.5))
		{
			carNames[0] = "roc_SD";
			aiParams[0] = " 8.1 0.36";
			local4_2 = (Math.random() * 1);
		} else
		{
			carNames[0] = "roc_ST";
			aiParams[0] = " 4.0 0.4";
			local4_2 = (Math.random() * 1);
		}
		splineNames[0] = (carNames[0] + ".spl");
		carNames[0] = ((carNames[0] + "_") + (local4_2 + 1));
		if ((Math.random() < 0.5))
		{
			carNames[1] = "roc_MC";
			aiParams[1] = " 5.4 0.36";
			local4_2 = (Math.random() * 1);
		} else
		{
			carNames[1] = "roc_SS";
			aiParams[1] = " 7.5 0.4";
			local4_2 = (Math.random() * 1);
		}
		splineNames[1] = (carNames[1] + ".spl");
		carNames[1] = ((carNames[1] + "_") + (local4_2 + 1));
		if ((Math.random() < 0.5))
		{
			carNames[2] = "roc_GT2";
			aiParams[2] = " 7.5 0.42";
			local4_2 = (Math.random() * 1);
		} else
		{
			carNames[2] = "roc_DS";
			aiParams[2] = " 7.5 0.42";
			local4_2 = (Math.random() * 2);
		}
		splineNames[2] = (carNames[2] + ".spl");
		carNames[2] = ((carNames[2] + "_") + (local4_2 + 1));
		carNames[3] = "roc_GT3";
		aiParams[3] = " 8.0 0.4";
		local4_2 = (Math.random() * 1);
		splineNames[3] = (carNames[3] + ".spl");
		carNames[3] = ((carNames[3] + "_") + (local4_2 + 1));
		local2 = rounds[0];
		local2[0] = arg0;
		int local5;
		int local6 = 1;
		while ((local6 < Roc.COMPETITORS))
		{
			if ((((Racer.RID_FEJ + local6) + local5) == arg0.character.id()))
			{
				local5++;
			}
			Bot local7 = new Bot((local6 + local5), (Math.random() * 10000), Math.random(), 2.0, 2.0, 1.0);
			local7.botVd = GameLogic.getVehicleDescriptor(VehicleType.VS_RRACE, 1.0);
			local2[local6] = local7;
			local6++;
		}
	}

	public int numRounds()
	{
		return rounds.length;
	}

	public int getCurrentRound()
	{
		return actRound;
	}

	public String getCurrentCarName()
	{
		return (Roc.carSaveDir + carNames[actRound]);
	}

	public String getCurrentSplineName()
	{
		return (Roc.carSaveDir + splineNames[actRound]);
	}

	public String getCurrentAIParam()
	{
		return ("AI_params " + aiParams[actRound]);
	}

	public int isFinalRound()
	{
		return (actRound == (Roc.ROUNDS - 1));
	}

	public int isRoundClosed()
	{
		return ((lostRuns == 2) || (wonRuns == 2));
	}

	public Bot getNextOpponent()
	{
		Bot local1;
		Racer[] local2;
		local2 = rounds[actRound];
		local1 = local2[1];
		return local1;
	}

	public void closeRun()
	{
		Racer[] local1;
		Racer[] local2;
		int local3 = isRoundClosed();
		if (local3)
		{
			lostRuns = 0;
			wonRuns = 0;
			if (((actRound + 1) < Roc.ROUNDS))
			{
				local1 = rounds[actRound];
				local2 = rounds[(actRound + 1)];
				local2[0] = local1[0];
				int local4 = (local2.length - 1);
				while (local4)
				{
					Racer local5;
					if ((Math.random() > 0.5))
					{
						local5 = local1[(local4 * 2)];
					} else
					{
						local5 = local1[((local4 * 2) + 1)];
					}
					local2[local4] = local5;
					local4--;
				}
			}
			actRound++;
		}
	}

}
