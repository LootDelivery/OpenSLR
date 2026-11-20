package java.game;

import java.render.*;
import java.util.*;
import java.util.resource.*;

public class Navigator
{
	static final int lda;
	static final float DEF_ZOOM = 4.5;
	Viewport vp;
	Dummy localroot;
	Camera cam;
	RenderRef[] tiles;
	RenderRef[] tiletypes;
	RenderRef route;
	ResourceRef[] meshes;
	GameRef button;
	RenderRef rmCamera;
	GameRef gmCamera;
	Marker dmCamera;
	Vector marker = new Vector();
	Vector dynamarker = new Vector();
	float vp_l = 0.01;
	float vp_t = 0.75;
	float vp_w = 0.21000001;
	float vp_h = 0.23;
	float zoom = Navigator.DEF_ZOOM;
	float offsetX;
	float offsetZ;
	float top;
	float left;
	float size;
	int mode;

	public Navigator(float arg0, float arg1, float arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8)
	{
		super();
		localroot = new Dummy(GameRef.WORLDTREEROOT);
		RenderRef local10 = new RenderRef(arg3);
		ResourceRef local11 = new ResourceRef(arg5);
		ResourceRef local12 = new ResourceRef(arg4);
		int local13 = (arg6 * arg7);
		tiles = new RenderRef[local13];
		tiletypes = new RenderRef[local13];
		meshes = new ResourceRef[local13];
		top = arg1;
		left = arg0;
		size = arg2;
		int local14;
		int local15;
		float local16 = arg0;
		float local17 = arg1;
		rmCamera = new RenderRef(localroot, MW_Mod:0x104r, "Camera GPS Marker");
		gmCamera = new GameRef(localroot, MW_Mod:0x105r, "0", "Camera GPS Marker Base");
		dmCamera = addMarker(new RenderRef(MW_Mod:0x108r), gmCamera);
		int local18 = 0;
		while ((local18 < local13))
		{
			tiletypes[local18] = new RenderRef();
			tiletypes[local18].duplicate(local10);
			tiletypes[local18].changeResource(local11, new ResourceRef(arg5));
			meshes[local18] = new ResourceRef();
			meshes[local18].duplicate(local12);
			tiletypes[local18].changeResource(local12, meshes[local18]);
			tiles[local18] = new RenderRef(localroot, tiletypes[local18], "navigator_segment");
			tiles[local18].setMatrix(
				new Vector3((local16 + (local14 * arg2)), (-(0.0009999999)), (local17 + (local15 * arg2))),
				null
			);
			arg5++;
			if ((++local14 >= arg6))
			{
				local14 = 0;
				local15++;
				arg5 += arg8;
			}
			local18++;
		}
	}

	public void finalize()
	{
		int local1 = 0;
		while ((local1 < tiletypes.length))
		{
			tiles[local1].destroy();
			meshes[local1].destroy();
			tiletypes[local1].destroy();
			local1++;
		}
	}

	public void setInteractive()
	{
	}

	public void show()
	{
		if ((!(vp)))
		{
			vp = new Viewport(12, vp_l, vp_t, vp_w, vp_h);
		}
		cam = new Camera(localroot, vp, 1, 90, 1.0, 100.0, 0.2, 2.0, 0, 1);
		changeMode(Config.gpsMode);
		vp.activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
	}

	public void ViewMap()
	{
		if (vp)
		{
			vp.destroy();
		}
		if (cam)
		{
			cam.destroy();
		}
		vp = new Viewport(5, 0, 0, 1, 1);
		cam = new Camera(localroot, vp, 1, 90, 1.0, 100.0, 0.2, 2.0, 0, 1);
		changeMode(Config.gpsMode);
		vp.activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
	}

	public void hide()
	{
		cam.destroy();
		cam = null;
		vp.destroy();
		vp = null;
	}

	public void changeMode(int arg0)
	{
		mode = arg0;
		if (cam)
		{
			cam.setMatrix(new Vector3(0.0, zoom, 0.0), new Ypr(0.0, (-(1.57)), 0.0));
		}
	}

	public void updateNavigator(GameRef arg0)
	{
		updateNavigator(arg0, mode);
	}

	public native void updateNavigator(GameRef arg0, int arg1);

	public void changeSize(float arg0, float arg1, float arg2, float arg3)
	{
		vp_l = arg0;
		vp_t = arg1;
		vp_w = arg2;
		vp_h = arg3;
		if (vp)
		{
			vp.resize(arg0, arg1, arg2, arg3);
		}
	}

	public void changeZoom(float arg0)
	{
		zoom = arg0;
		if (cam)
		{
			cam.setMatrix(new Vector3(0.0, zoom, 0.0), new Ypr(0.0, (-(1.57)), 0.0));
		}
	}

	public Marker addMarker(Racer arg0)
	{
		RenderRef local2 = arg0.getMarker();
		GameRef local3 = arg0.car;
		return addMarker(local2, local3);
	}

	public Marker addMarker(RenderRef arg0, GameRef arg1)
	{
		RenderRef local3 = new RenderRef(localroot, arg0, "marker");
		Marker local4 = new DMarker(local3, arg1);
		dynamarker.addElement(local4);
		return local4;
	}

	public Marker addMarker(RenderRef arg0, Vector3 arg1, int arg2)
	{
		RenderRef local4 = new RenderRef(localroot, arg0, "marker");
		Vector3 local5 = new Vector3(arg1);
		local5.y = ((arg2 / 100.0) + 0.0009999999);
		Marker local6 = new SMarker(local4, local5);
		marker.addElement(local6);
		return local6;
	}

	public void remMarker(Marker arg0)
	{
		if (arg0)
		{
			if ((arg0 instanceof DMarker))
			{
				if (dynamarker)
				{
					dynamarker.removeElement(arg0);
					if (arg0.symbol)
					{
						arg0.symbol.destroy();
					}
				}
			} else
			{
				if (marker)
				{
					marker.removeElement(arg0);
					if (arg0.symbol)
					{
						arg0.symbol.destroy();
					}
				}
			}
		}
	}

}
