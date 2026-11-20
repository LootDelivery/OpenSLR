package java.game.parts;

public class WheelRef extends Native
{
	public native Vector3 getPos();

	public native Ypr getYpr();

	public native float getDrive();

	public native float getSteer();

	public native float getRadius();

	public native void setPos(Vector3 arg0);

	public native void setYpr(Ypr arg0);

	public native void setDrive(float arg0);

	public native void setSteer(float arg0);

	public native void setRadius(float arg0);

	public native void setWidth(float arg0);

	public native void setCPatch(float arg0, float arg1, float arg2);

	public native void setFriction(float arg0);

	public native void setFrictn_x(float arg0);

	public native void setSliction(float arg0);

	public native void setStiffness(float arg0);

	public native void setRollRes(float arg0);

	public native void setBearing(float arg0);

	public native void setMaxLoad(float arg0);

	public native void setLoadSmooth(float arg0);

	public native void setPacejka(int arg0, float arg1);

	public native void setForce(float arg0);

	public native void setDamping(float arg0);

	public native void setDamping(float arg0, float arg1);

	public native void setRestLen(float arg0);

	public native void setMinLen(float arg0);

	public native void setMaxLen(float arg0);

	public native void setInstantCenter(float arg0, float arg1, float arg2, float arg3, float arg4, float arg5);

	public native void setBrake(float arg0);

	public native void setHBrake(float arg0);

	public native void setArm(
		float arg0,
		float arg1,
		float arg2,
		float arg3,
		float arg4,
		float arg5,
		float arg6
	);

	public native void setHub(
		float arg0,
		float arg1,
		float arg2,
		float arg3,
		float arg4,
		float arg5,
		float arg6,
		float arg7,
		float arg8,
		float arg9
	);

}
