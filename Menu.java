package java.render.osd;

import java.render.*;
import java.util.*;
import java.util.resource.*;

public class Menu
{
	Osd osd;
	Style sty;
	Style stySld;
	Style styKnob;
	float x;
	float y;
	float spacing;
	int orientation;

	public Menu(Osd arg0, Style arg1, float arg2, float arg3, float arg4)
	{
		this(arg0, arg1, arg2, arg3, arg4, Osd.MD_VERTICAL);
	}

	public Menu(Osd arg0, Style arg1, float arg2, float arg3, float arg4, int arg5)
	{
		super();
		osd = arg0;
		sty = arg1;
		spacing = arg4;
		x = arg2;
		y = arg3;
		orientation = arg5;
		if ((spacing == 0.0))
		{
			if ((arg5 == Osd.MD_VERTICAL))
			{
				spacing = arg1.height;
			} else
			{
				spacing = arg1.width;
			}
		}
	}

	public void setStyle(Style arg0)
	{
		sty = arg0;
	}

	public void setSliderStyle(Style arg0, Style arg1)
	{
		stySld = arg0;
		styKnob = arg1;
	}

	public void nextLine()
	{
		addSeparator();
		addSeparator();
	}

	public void addSeparator()
	{
		if ((orientation == Osd.MD_VERTICAL))
		{
			y += ((spacing / 2) / osd.vpHeight);
		} else
		{
			if ((sty.align == Text.ALIGN_RIGHT))
			{
				x -= ((spacing / 2) / osd.vpHeight);
			} else
			{
				x += ((spacing / 2) / osd.vpHeight);
			}
		}
	}

	public Button addItem(ResourceRef arg0, int arg1, String arg2)
	{
		Style local4 = new Style(sty.width, sty.height, sty.charset, sty.align, arg0);
		Button local5 = osd.createButton(local4, x, y, arg1, arg2);
		nextLine();
		return local5;
	}

	public Button addItem(String arg0, int arg1)
	{
		Button local3 = osd.createButton(sty, x, y, arg0, arg1);
		nextLine();
		return local3;
	}

	public Button addItem(String arg0, int arg1, String arg2)
	{
		Button local4 = osd.createButton(sty, x, y, arg0, arg1);
		local4.setToolTip(arg2);
		nextLine();
		return local4;
	}

	public Slider addItem(String arg0, int arg1, float arg2, String arg3)
	{
		Slider local5 = osd.createSlider(stySld, styKnob, x, y, arg0, arg1, arg2);
		local5.setToolTip(arg3);
		nextLine();
		return local5;
	}

	public Slider addItem(String arg0, int arg1, float arg2, float arg3, float arg4, int arg5, String arg6)
	{
		Slider local8 = osd.createSlider(stySld, styKnob, x, y, arg0, arg1, arg2, arg3, arg4, arg5);
		local8.setToolTip(arg6);
		nextLine();
		return local8;
	}

	public MultiChoice addItem(String arg0, int arg1, int arg2, Vector arg3, String arg4)
	{
		MultiChoice local6 = osd.createMulti(sty, x, y, arg0, arg1, arg2, arg3);
		local6.setToolTip(arg4);
		nextLine();
		return local6;
	}

	public MultiChoice addItem(String arg0, int arg1, int arg2, String[] arg3, String arg4)
	{
		Vector local6 = new Vector(arg3.length);
		int local7 = 0;
		while ((local7 < arg3.length))
		{
			local6.addElement(arg3[local7]);
			local7++;
		}
		MultiChoice local7_2 = osd.createMulti(sty, x, y, arg0, arg1, arg2, local6);
		local7_2.setToolTip(arg4);
		nextLine();
		return local7_2;
	}

}
