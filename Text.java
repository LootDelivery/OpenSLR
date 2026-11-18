package java.render;

import java.util.*;
import java.util.resource.*;

public class Text
{
	static final int RID_CONSOLE5 = frontend:0xb2r;
	static final int RID_CONSOLE10 = frontend:0x23r;
	static final int RID_SL28 = frontend:0x2er;
	static final int RID_SLII11 = frontend:0x138r;
	static final int RID_SLII17 = frontend:0x139r;
	static final int RID_SLII24 = frontend:0x25r;
	static final int RID_SL14 = frontend:0x8cr;
	static final int RID_SIMPLE20 = frontend:0x20r;
	static final int RID_SIMPLE40 = frontend:0x22r;
	static final int RID_SYM_POINTERS = frontend:0x3cr;
	static final int ALIGN_LEFT = 2;
	static final int ALIGN_CENTER = 1;
	static final int ALIGN_RIGHT = 0;
	static final int DEF_COLOR = -1;
	String text;
	RenderRef renderinst = new RenderRef();

	public static int getFontSize(ResourceRef arg0)
	{
		int local1;
		if ((arg0.id() == Text.RID_SLII24))
		{
			local1 = 24;
		} else
		{
			if ((arg0.id() == Text.RID_SLII17))
			{
				local1 = 15;
			} else
			{
				if ((arg0.id() == Text.RID_SLII11))
				{
					local1 = 10;
				} else
				{
					if ((arg0.id() == Text.RID_SIMPLE40))
					{
						local1 = 40;
					} else
					{
						if ((arg0.id() == Text.RID_SIMPLE20))
						{
							local1 = 20;
						} else
						{
							if ((arg0.id() == Text.RID_SL14))
							{
								local1 = 14;
							} else
							{
								if ((arg0.id() == Text.RID_SL28))
								{
									local1 = 28;
								} else
								{
									if ((arg0.id() == Text.RID_CONSOLE10))
									{
										local1 = 10;
									} else
									{
										if ((arg0.id() == Text.RID_CONSOLE5))
										{
											local1 = 7;
										} else
										{
											local1 = 20;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return local1;
	}

	public static float getHeight(ResourceRef arg0, Viewport arg1)
	{
		return ((2 * getFontSize(arg0)) / (Config.video_y * arg1.getHeight()));
	}

	public static float getWidth(String arg0, ResourceRef arg1, Viewport arg2)
	{
		return ((2 * getWidthPixels(arg0, arg1)) / (Config.video_x * arg2.getWidth()));
	}

	public static native float getWidthPixels(String arg0, ResourceRef arg1);

	public Text(ResourceRef arg0, ResourceRef arg1, String arg2, float arg3, float arg4)
	{
		super();
		create(arg0, arg1, arg3, arg4);
		changeText(arg2);
	}

	public Text(ResourceRef arg0, ResourceRef arg1, String arg2, float arg3, float arg4, int arg5, Viewport arg6)
	{
		this(
			arg0,
			arg1,
			arg2,
			arg3,
			(arg4 + ((arg5 * 2) * ((1.2 * getFontSize(arg1)) / (Config.video_y * arg6.getHeight()))))
		);
	}

	public native void create(ResourceRef arg0, ResourceRef arg1, float arg2, float arg3);

	public void finalize()
	{
		renderinst.destroy();
	}

	public native void changeColor(int arg0);

	public native void changeAlign(int arg0);

	public native void setPos(float arg0, float arg1);

	public native Vector3 getPos();

	public native void update();

	public void changeText(String arg0)
	{
		text = arg0;
		update();
	}

}
