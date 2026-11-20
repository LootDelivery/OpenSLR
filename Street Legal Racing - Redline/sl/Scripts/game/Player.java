package java.game;

import java.io.*;
import java.render.*;
import java.util.*;
import java.util.resource.*;

public class Player extends Racer
{
	static final int H_GARAGE = 1;
	static final int H_DAYCITY = 2;
	static final int H_NIGHTCITY = 4;
	int hints;
	int winPinkSlips;
	Osd osd;
	Controller controller;
	Inventory parts;
	Vector decals = new Vector();
	int flags;
	float record_acceleration;
	float record_speed;
	int races_won;
	int races_lost;
	CarLot carlot;

	public Player()
	{
		super();
		parts = new Inventory(this);
		carlot = new CarLot(this);
		osd = new Osd(1.0, 0.0, 18);
		osd.iLevel = Osd.IL_NONE;
		osd.createRectangle((-(0.75)), 0.715, 0.55, 0.67, (-(2)), new ResourceRef(frontend:0x83r));
	}

	public int checkHint(int arg0)
	{
		int local2 = (hints & arg0);
		hints |= arg0;
		return (!(local2));
	}

	public RenderRef getMarker()
	{
		return Marker.RR_PLAYER;
	}

	public void showOsd()
	{
		osd.show();
		controller.queueEvent(null, GameType.EVENT_COMMAND, ("osd " + osd.id()));
	}

	public void hideOsd()
	{
		controller.queueEvent(null, GameType.EVENT_COMMAND, "osd 0");
		osd.hide();
	}

	public void save(File arg0)
	{
		super.save(arg0);
		parts.save(arg0);
	}

	public void load(File arg0)
	{
		super.load(arg0);
		parts.load(arg0);
	}

}
