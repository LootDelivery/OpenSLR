package java.game;

import java.io.*;
import java.render.*;
import java.util.resource.*;

public class SplashScreen extends GameType implements GameState
{
	Osd osd;
	ResourceRef pic;

	public SplashScreen(ResourceRef arg0)
	{
		super();
		createNativeInstance();
		pic = arg0;
	}

	public void enter(GameState arg0)
	{
		osd = new Osd();
		osd.createBG(pic);
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), Input.AXIS_CANCEL, this);
		osd.show();
		setEventMask(GameRef.EVENT_TIME);
		addTimer(3, 1);
	}

	public void handleEvent(GameRef arg0, int arg1, int arg2)
	{
		osdCommand(Input.AXIS_CANCEL);
	}

	public void exit(GameState arg0)
	{
		clearEventMask(GameRef.EVENT_ANY);
		osd.hide();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == Input.AXIS_CANCEL))
		{
			GameLogic.changeActiveSection(new MainMenu());
		}
	}

}
