package java.game.parts;

import java.util.resource.*;

public class SfxTable extends Native
{
	public native int getItems();

	public native void clear();

	public native void addItem(ResourceRef arg0, float arg1, float arg2, float arg3, float arg4, float arg5);

}
