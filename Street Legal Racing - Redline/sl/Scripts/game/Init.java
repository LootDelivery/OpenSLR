package java.game;

import java.io.*;
import java.render.*;
import java.sound.*;
import java.util.resource.*;

public class Init extends GameType
{
	GameLogic gameLogic;

	public Init(int arg0)
	{
		super();
		Sound.init();
		Frontend.init();
		Frontend.loadingScreen.show();
		Input.initControllers(this, "save/controls/active_control_set");
		Input.enablePointingDevice();
		gameLogic = new GameLogic();
	}

	public void finalize()
	{
		gameLogic.finalize();
		Input.disablePointingDevice();
		Input.destroyControllers();
		Frontend.destroy();
		super.finalize();
	}

}
