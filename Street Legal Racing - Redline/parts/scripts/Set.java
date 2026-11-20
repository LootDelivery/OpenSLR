package java.game.parts;

import java.io.*;

public class Set extends GameType
{
	String name;
	String description;
	int logo;

	public static Set createFromFile(File arg0)
	{
		Set local1 = new Set();
		local1.name = arg0.readString();
		local1.description = arg0.readString();
		return local1;
	}

	public void save(File arg0)
	{
		arg0.write(name);
		arg0.write(description);
	}

}
