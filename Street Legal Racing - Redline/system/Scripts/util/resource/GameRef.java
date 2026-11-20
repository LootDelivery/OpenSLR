package java.util.resource;

public class GameRef extends ResourceRef
{
	static final int WORLDTREEROOT = 16;
	static final int WORLDTREELEAF = 64;
	static final int RID_DUMMY = system:0x1br;
	static final int RID_FRONTEND = system:0x2fr;
	static final int RID_CAMERA = system:0x33r;
	static final int RID_TRIGGER = system:0x34r;
	static final int RID_CURSOR = frontend:0x2ar;
	static final int RID_BUTTON = frontend:0x24r;
	static final int RID_INVITEM = frontend:0x2br;
	static final int EVENT_COLLISION = 1;
	static final int EVENT_DELETE = 2;
	static final int EVENT_HIT = 4;
	static final int EVENT_DAMAGE = 8;
	static final int EVENT_COMMAND = 16;
	static final int EVENT_TRIGGER_ON = 32;
	static final int EVENT_TRIGGER_OFF = 64;
	static final int EVENT_TIME = 128;
	static final int EVENT_DESTROY = 256;
	static final int EVENT_WATER = 512;
	static final int EVENT_INFO = 1024;
	static final int EVENT_BODYCOLLISION = 8192;
	static final int EVENT_GNDCOLLISION = 16384;
	static final int EVENT_NETEVENT = 32768;
	static final int EVENT_CURSOR = 65536;
	static final int EVENT_HOTKEY = 1048576;
	static final int EVENT_ANY = 268435455;
	static final int EVENT_NONE = 0;
	static final int EVENT_CUSTOMINFO = 268435456;
	static final int EVENT_SAME = 0;
	static final int TC_ACTIVE = 1;
	static final int TC_PASSIVE = 2;

	public GameRef(GameRef arg0)
	{
		super(arg0);
	}

	public GameRef(GameRef arg0, GameRef arg1, String arg2, String arg3)
	{
		super();
		create(arg0, arg1, arg2, arg3);
	}

	public GameRef(GameRef arg0, int arg1, String arg2, String arg3)
	{
		super();
		create(arg0, new GameRef(arg1), arg2, arg3);
	}

	public GameRef(int arg0)
	{
		super(arg0);
	}

	public GameRef(int arg0, int arg1, String arg2, String arg3)
	{
		this(new GameRef(arg0), arg1, arg2, arg3);
	}

	public native GameType create(GameRef arg0, GameRef arg1, String arg2, String arg3);

	public native void create_native(GameRef arg0, GameRef arg1, String arg2, String arg3);

	public native int getFlags();

	public native void setFlags(int arg0);

	public native void clearFlags(int arg0);

	public native Ypr getOri();

	public native Vector3 getPos();

	public native Vector3 getVel();

	public native void setPos(Vector3 arg0);

	public native void setMatrix(Vector3 arg0, Ypr arg1);

	public native void setParent(ResourceRef arg0);

	public native void setState(Vector3 arg0, Ypr arg1, Vector3 arg2, Vector3 arg3);

	public native int isEmpty();

	public int isScripted()
	{
		return isScripted(null);
	}

	public native int isScripted(String arg0);

	public native Object getScriptInstance();

	public native int getInfo(int arg0, int arg1);

	public native int getInfo(int arg0, String arg1);

	public int getInfo(int arg0)
	{
		return getInfo(arg0, 0);
	}

	public native void queueEvent(ResourceRef arg0, int arg1, String arg2);

	void command(String arg0)
	{
		queueEvent(null, GameRef.EVENT_COMMAND, arg0);
	}

	public native void setActiveCollision();

}
