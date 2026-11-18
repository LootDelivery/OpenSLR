package java.render;

import java.io.*;
import java.render.osd.*;
import java.util.*;
import java.util.resource.*;

public class Osd extends GameType
{
	static final int COLOR_UNSELECTED = -4144960;
	static final int COLOR_SELECTED = -13568;
	static final int COLOR_DISABLED = 1891680448;
	static final int COLOR_PRESSED = -64;
	static final int RID_SLD_BACK = frontend:0x89r;
	static final int RID_SLD_KNOB = frontend:0x91r;
	static final int RID_ARROWUP = frontend:0x4cr;
	static final int RID_ARROWDN = frontend:0x84r;
	static final int RID_ARROWLF = frontend:0x85r;
	static final int RID_ARROWRG = frontend:0x88r;
	static final int RID_OK = frontend:0x10r;
	static final int RID_CANCEL = frontend:0x1dr;
	static final int RID_BACK = frontend:0x126r;
	static final int RID_EXIT = frontend:0x135r;
	static final ResourceRef RRT_HEADERBG = new ResourceRef(frontend:0x28r);
	static final ResourceRef RRT_TOOLTIP = new ResourceRef(frontend:0xcbr);
	static final int HL_LABELCOLOR = 1;
	static final int HL_BACKLIGHT = 2;
	static final int MD_VERTICAL = 0;
	static final int MD_HORIZONTAL = 1;
	static final int RID_GHOSTBUTTON = frontend:0x38r;
	static final ResourceRef RRT_NONE = new ResourceRef(frontend:0x18r);
	static final ResourceRef RRT_EMPTY = new ResourceRef(frontend:0xafr);
	static final ResourceRef RRT_DARKEN = new ResourceRef(frontend:0xa2r);
	static final ResourceRef RRT_TEST = Osd.RRT_NONE;
	static final int HK_STATIC = 4;
	static final int PRIVATEEVENT = -2147483648;
	static final int CMD_MENU_INC = 65536;
	static final int CMD_MENU_DEC = 131072;
	static final int CMD_MENU_UP = 196608;
	static final int CMD_MENU_DN = 262144;
	static final int CMD_MENU_LF = 327680;
	static final int CMD_MENU_RG = 393216;
	static final int CMD_MENU_SEL = 458752;
	static final float CAM_AOV = 60;
	static final float CAM_OFFSET = 5.48;
	static final float SCALE_FS = 1.47;
	static final float SCALE_3D = 1.4689999;
	static final int OF_MODAL = 1;
	static final int IL_NONE = 0;
	static final int IL_TIPS = 1;
	static final int IL_KEYS = 2;
	static final int IL_MENU = 3;
	int flags;
	ParticleSystem psPress;
	ParticleSystem psMove;
	Viewport vp;
	Camera cam;
	float screenAspect;
	float vpAspect;
	float vpWidth;
	float vpHeight;
	float vpTop;
	float vpLeft;
	int orientation;
	int menuKeysCreated;
	MessagePort alienMp;
	Object globalHandler;
	int iLevel = Osd.IL_MENU;
	float lastUserActivityTimeStamp;
	int focusTracker;
	int controlCount;
	int prevKey;
	int prevKeyCount;
	float sliderValue;
	int multiValue;
	String stringValue;
	GameRef dropObject;
	Gadget dropGadget;
	GameRef[] button = new GameRef[100];
	Object[] buttonHandler = new Object[100];
	int[] buttonCommand = new int[100];
	int[] textIndex = new int[100];
	String[] description = new String[100];
	int buttons = 0;
	Vector text = new Vector();
	Vector textbox = new Vector();
	Vector groups = new Vector();
	Vector hotkey = new Vector();
	Vector object = new Vector();
	Vector rectangles = new Vector();
	int init = 1;
	int defSelection = 0;
	Gadget pointedGadget;
	Group pointedGadgetGroup;
	int activeCommand = 0;
	int selectionMoved = 0;
	int selected = 1;
	int clickLock;
	Gadget actDown;
	Gadget actGadget;
	int controlMode;
	Gadget lastPointedGadget;
	Text tooltipText;
	Rectangle tooltipRect;
	Text textInput;
	String textInputString;

	public Osd()
	{
		this(1.0, 0.0, 10);
	}

	public Osd(float arg0, float arg1, int arg2)
	{
		this(new Viewport(arg2, 0.5, 0.5, arg0, arg1, (-(0.5)), (-(0.5))));
	}

	public Osd(float arg0, float arg1, float arg2, float arg3, float arg4, float arg5, int arg6)
	{
		this(new Viewport(arg6, arg2, arg3, arg0, arg1, arg4, arg5));
	}

	private Osd(Viewport arg0)
	{
		super();
		createNativeInstance();
		vp = arg0;
		setFlags(GameRef.WORLDTREEROOT);
		vpWidth = vp.getWidth();
		vpHeight = vp.getHeight();
		vpAspect = vp.getAspect();
		vpTop = vp.getTop();
		vpLeft = vp.getLeft();
		screenAspect = ((Config.video_x * vpWidth) / ((1.0 * Config.video_y) * vpHeight));
		groups.addElement(new Group(this));
	}

	public void finalize()
	{
		int local1 = (groups.size() - 1);
		while ((local1 >= 0))
		{
			groups.elementAt(local1).deactivate();
			local1--;
		}
		while ((!(hotkey.isEmpty())))
		{
			Hotkey local1_2 = hotkey.removeLastElement();
			local1_2.inactivate();
		}
		vp.destroy();
		super.finalize();
	}

	public void rebuild()
	{
		int local1 = (cam == null);
		if ((!(local1)))
		{
			hide();
		}
		vp.destroy();
		vp = new Viewport(10, vpTop, vpLeft, vpWidth, vpHeight);
		vpAspect = vp.getAspect();
		if ((!(local1)))
		{
			show();
		}
	}

	public Viewport getViewport()
	{
		return vp;
	}

	public float mapClickPosH(Vector3 arg0, float arg1, float arg2)
	{
		arg0.x = ((arg0.x - (((vpLeft + vpLeft) + vpWidth) - 1.0)) / vpWidth);
		arg0.y = ((arg0.y - (((vpTop + vpTop) + vpHeight) - 1.0)) / vpHeight);
		if ((arg0.x < arg1))
		{
			arg0.x = arg1;
		}
		if ((arg0.x > arg2))
		{
			arg0.x = arg2;
		}
		return ((arg0.x - arg1) / (arg2 - arg1));
	}

	public int idleFor(float arg0)
	{
		if (focusTracker)
		{
			if (isFocused())
			{
				return ((lastUserActivityTimeStamp + arg0) < System.currentTime());
			} else
			{
				focusTracker = 0;
			}
		} else
		{
			if (isFocused())
			{
				lastUserActivityTimeStamp = System.currentTime();
				focusTracker = 1;
			}
		}
		return 0;
	}

	public float getIdleTime()
	{
		return (System.currentTime() - lastUserActivityTimeStamp);
	}

	public void setIdleTime(float arg0)
	{
		lastUserActivityTimeStamp = (System.currentTime() - arg0);
	}

	public void deactivateText()
	{
		if (pointedGadget)
		{
			pointedGadget.unfocus();
			pointedGadget = null;
			if (tooltipText)
			{
				tooltipText.changeText(null);
				tooltipText = null;
				tooltipRect.finalize();
				tooltipRect = null;
			}
		}
	}

	public void activateText(Group arg0, Gadget arg1)
	{
		if (arg1)
		{
			lastUserActivityTimeStamp = System.currentTime();
			pointedGadget = arg1;
			lastPointedGadget = arg1;
			pointedGadgetGroup = arg0;
			activeCommand = arg1.cmd;
			arg1.focus();
			if ((actDown == pointedGadget))
			{
				pointedGadget.press();
			}
		}
	}

	public void changeSelection2(int arg0, int arg1)
	{
		Group local3;
		Group local4;
		Gadget local5;
		Gadget local6;
		int local7;
		int local8;
		if ((arg0 == (-(1))))
		{
			controlMode = 1;
		}
		if (((!(pointedGadget)) || (arg0 == (-(1)))))
		{
			local7 = 0;
			while ((local7 < groups.size()))
			{
				local3 = groups.elementAt(local7);
				if (local3.active)
				{
					local8 = 0;
					while ((local8 < local3.gadget.size()))
					{
						local5 = local3.gadget.elementAt(local8);
						if ((!((local5.disabled || local5.nofocus))))
						{
							local6 = local5;
							local4 = local3;
							break;
						}
						local5 = local3.gadget.elementAt(local8);
						local8++;
					}
				}
				if (local6)
				{
					break;
				}
				local7++;
			}
		} else
		{
			Vector3 local9 = pointedGadget.getHotSpot();
			float local10 = 100000;
			float local11;
			local7 = 0;
			while ((local7 < groups.size()))
			{
				local3 = groups.elementAt(local7);
				if (local3.active)
				{
					local8 = 0;
					while ((local8 < local3.gadget.size()))
					{
						local5 = local3.gadget.elementAt(local8);
						if (((!((local5.disabled || local5.nofocus))) && (local5 != pointedGadget)))
						{
							Vector3 local12 = local5.getHotSpot();
							local12.sub(local9);
							if (((((((arg0 == 0) && (local12.y > 0)) && ((local11 = ((local12.x * local12.x) + ((local12.y * local12.y) * 0.4))) < local10)) || (((arg0 == 1) && (local12.y < 0)) && ((local11 = ((local12.x * local12.x) + ((local12.y * local12.y) * 0.4))) < local10))) || (((arg0 == 2) && (local12.x < 0)) && ((local11 = (((local12.x * local12.x) * 0.14) + (local12.y * local12.y))) < local10))) || (((arg0 == 3) && (local12.x > 0)) && ((local11 = (((local12.x * local12.x) * 0.14) + (local12.y * local12.y))) < local10))))
							{
								local10 = local11;
								local6 = local5;
								local4 = local3;
							}
						}
						local8++;
					}
				}
				local7++;
			}
		}
		if (local6)
		{
			deactivateText();
			activateText(local4, local6);
			if (arg1)
			{
				new SfxRef(Frontend.SFX_MENU_MOVE).play();
			}
		}
	}

	public void changeSelection(int arg0, int arg1)
	{
		Group local3 = pointedGadgetGroup;
		Gadget local4 = pointedGadget;
		int local5;
		int local6;
		int local7;
		int local8;
		int local9;
		int local10;
		if (local3)
		{
			local5 = groups.indexOf(local3);
			if (local4)
			{
				local6 = local3.gadget.indexOf(local4);
			}
		} else
		{
			local3 = groups.elementAt(local5);
			pointedGadgetGroup = local3;
		}
		local6 += arg0;
		int local11;
		if ((arg0 < 0))
		{
			local11 = (-(1));
		} else
		{
			local11 = 1;
		}
		while (((!(local7)) && (!(local8))))
		{
			if ((((local6 < 0) || (local6 >= local3.gadget.size())) || (!(local3.active))))
			{
				local10 = 1;
			} else
			{
				if ((local3.gadget.elementAt(local6).disabled || local3.gadget.elementAt(local6).nofocus))
				{
					local6 += local11;
					continue;
				} else
				{
					local8 = 1;
				}
			}
			while (((local10 || (!(local3.active))) && (!(local7))))
			{
				local5 += local11;
				if ((local5 < 0))
				{
					local5 = (groups.size() - 1);
				} else
				{
					if ((local5 >= groups.size()))
					{
						local5 = 0;
					}
				}
				local3 = groups.elementAt(local5);
				if ((local3 == pointedGadgetGroup))
				{
					local7 = 1;
				}
				local9 = 1;
				local10 = 0;
			}
			if (local9)
			{
				if ((local11 < 0))
				{
					local6 = (local3.gadget.size() - 1);
				} else
				{
					local6 = 0;
				}
				local9 = 0;
			}
		}
		if ((!(local7)))
		{
			local4 = local3.gadget.elementAt(local6);
			deactivateText();
			activateText(local3, local4);
			if (arg1)
			{
				new SfxRef(Frontend.SFX_MENU_MOVE).play();
			}
		}
	}

	public void animate()
	{
		if (((((pointedGadget && (!(controlMode))) && pointedGadget.tooltip) && (!(tooltipText))) && idleFor(1)))
		{
			Vector3 local1 = Input.cursor.getPos();
			float local2 = 0.4;
			float local3 = Input.cursor.vp.getAspect();
			float local4 = Text.getHeight(Frontend.smallFont, Input.cursor.vp);
			float local5 = Text.getWidth(pointedGadget.tooltip, Frontend.smallFont, Input.cursor.vp);
			float local6 = (local4 * 0.6);
			local4 += local6;
			local5 += local6;
			local1.x += ((local5 * 0.5) + 0.049999997);
			local1.y += ((local4 * 0.5) + 0.1);
			float local7 = (local1.x + (0.5 * local5));
			if ((local7 > 1.0))
			{
				local1.x -= (local7 - 1.0);
			}
			float local8 = (local1.y + (0.5 * local4));
			if ((local8 > 1.0))
			{
				local1.y -= (local8 - 1.0);
			}
			tooltipText = new Text(
				Input.cursor,
				Frontend.smallFont,
				pointedGadget.tooltip,
				local1.x,
				(local1.y - (local2 * local4))
			);
			tooltipText.changeAlign(Text.ALIGN_CENTER);
			local1.x *= (Osd.SCALE_3D * local3);
			local1.y *= (-(Osd.SCALE_3D));
			tooltipRect = new Rectangle(
				Input.cursor,
				local1,
				new RectangleTemplate(((local5 * Osd.SCALE_FS) * local3), (local4 * Osd.SCALE_FS), Osd.RRT_TOOLTIP)
			);
		}
		if ((!(isFocused())))
		{
			deactivateText();
		}
		if ((Frontend.inputQueue.lastElement() == this))
		{
			controlCount++;
			int local1_2 = Input.lastKey();
			if ((controlMode && idleFor(10)))
			{
				deactivateText();
				controlMode = 0;
			}
			if (local1_2)
			{
				int local2_2;
				if (((prevKey == local1_2) && ((prevKeyCount + 1) == controlCount)))
				{
					local2_2 = 1;
				}
				prevKeyCount = controlCount;
				prevKey = local1_2;
			}
			if ((pointedGadget instanceof Slider))
			{
				if (pointedGadget.pressed)
				{
					Slider local2_3 = ((Slider) pointedGadget);
					Vector3 local3_2 = Input.cursor.getPos();
					float local4_2 = ((mapClickPosH(local3_2, local2_3.x0, local2_3.x1) * (local2_3.b - local2_3.a)) + local2_3.a);
					if ((local4_2 != local2_3.value))
					{
						lastUserActivityTimeStamp = System.currentTime();
						local2_3.setValue(local4_2);
						local2_3.sendMessage();
					}
				}
			}
			if (actGadget)
			{
				if ((actGadget instanceof StringInput))
				{
					int local2_4 = (local1_2 >> 16);
					if ((local2_4 > 0))
					{
						lastUserActivityTimeStamp = System.currentTime();
						if (actGadget.key(local2_4))
						{
							new SfxRef(Frontend.SFX_MENU_SELECT).play();
						}
					}
				}
			}
		}
	}

	public void handleEvent(Hotkey arg0)
	{
		lastUserActivityTimeStamp = System.currentTime();
		if ((arg0.command & Osd.PRIVATEEVENT))
		{
			int local2 = (arg0.command & (~(Osd.PRIVATEEVENT)));
			if (((Frontend.inputQueue.lastElement() == this) && (iLevel >= Osd.IL_MENU)))
			{
				if ((!(actGadget)))
				{
					if (((((!(controlMode)) && (((((local2 == Osd.CMD_MENU_UP) || (local2 == Osd.CMD_MENU_DN)) || (local2 == Osd.CMD_MENU_LF)) || (local2 == Osd.CMD_MENU_RG)) || (local2 == Osd.CMD_MENU_SEL))) && lastPointedGadget) && (!(pointedGadget))))
					{
						activateText(pointedGadgetGroup, lastPointedGadget);
						controlMode = 1;
					} else
					{
						if ((!(controlMode)))
						{
							controlMode = 1;
						}
						if ((local2 == Osd.CMD_MENU_UP))
						{
							changeSelection2(0, 1);
						} else
						{
							if ((local2 == Osd.CMD_MENU_DN))
							{
								changeSelection2(1, 1);
							} else
							{
								if ((local2 == Osd.CMD_MENU_LF))
								{
									changeSelection2(2, 1);
								} else
								{
									if ((local2 == Osd.CMD_MENU_RG))
									{
										changeSelection2(3, 1);
									} else
									{
										if ((local2 == Osd.CMD_MENU_SEL))
										{
											if ((pointedGadget && (!(pointedGadget.disabled))))
											{
												if (arg0.state)
												{
													new SfxRef(Frontend.SFX_MENU_SELECT).play();
													pointedGadget.press();
													actDown = pointedGadget;
												} else
												{
													if ((actDown == pointedGadget))
													{
														pointedGadget.release();
														pointedGadget.click();
													}
													actDown = null;
												}
											}
										} else
										{
											if ((local2 == Osd.CMD_MENU_INC))
											{
												if (pointedGadget)
												{
													pointedGadget.increase();
												}
											} else
											{
												if ((local2 == Osd.CMD_MENU_DEC))
												{
													if (pointedGadget)
													{
														pointedGadget.decrease();
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} else
		{
			if (arg0.handler)
			{
				arg0.handler.osdCommand(arg0.command);
			} else
			{
				if (alienMp)
				{
					alienMp.putMessage(new Event(arg0.command, arg0.state));
				}
			}
		}
	}

	public void event_handler(GameRef arg0, int arg1, String arg2)
	{
		int local4 = arg2.token(0).intValue();
		if ((local4 == GameType.EC_LCLICK))
		{
			int local5 = button.length;
			while (local5--)
			{
				if (button[local5])
				{
					if ((button[local5].id() == arg0.id()))
					{
						buttonHandler[local5].osdCommand(buttonCommand[local5]);
						break;
					}
				}
			}
		}
	}

	public void show()
	{
		show(1);
	}

	public void hide()
	{
		hide(1);
	}

	public void show(int arg0)
	{
		if (init)
		{
			init = 0;
			resetSelection();
			controlMode = 1;
		} else
		{
			changeSelection2((-(1)), 0);
		}
		lastUserActivityTimeStamp = System.currentTime();
		cam = new Camera(this, vp, 2, Osd.CAM_AOV, 0.1, 10.0);
		cam.setMatrix(new Vector3(0.0, 0.0, (Osd.CAM_OFFSET * vpHeight)), null);
		if (arg0)
		{
			vp.activate((Viewport.RENDERFLAG_CLEARDEPTH | Viewport.RENDERFLAG_CLEARTARGET));
			if ((iLevel > Osd.IL_NONE))
			{
				enableControl();
			}
		}
		if ((iLevel >= Osd.IL_MENU))
		{
			if ((!(menuKeysCreated)))
			{
				createMenuKeys();
			}
		}
	}

	public void hide(int arg0)
	{
		deactivateText();
		if (arg0)
		{
			if ((iLevel > Osd.IL_NONE))
			{
				disableControl();
			}
			vp.deactivate();
		}
		if (cam)
		{
			cam.destroy();
			cam = null;
		}
	}

	public void enableControl()
	{
		setEventMask((GameRef.EVENT_CURSOR | GameRef.EVENT_HOTKEY));
		enableAnimateHook();
		if ((iLevel > Osd.IL_TIPS))
		{
			setFocus(1);
		}
	}

	public void disableControl()
	{
		if ((iLevel > Osd.IL_TIPS))
		{
			setFocus(0);
		}
		disableAnimateHook();
		clearEventMask(GameRef.EVENT_ANY);
	}

	public Gadget findGadget(Object arg0, int arg1)
	{
		int local3 = groups.size();
		while (local3--)
		{
			Group local4 = groups.elementAt(local3);
			int local5 = local4.gadget.size();
			Gadget local6;
			while (local5--)
			{
				local6 = local4.gadget.elementAt(local5);
				if (((local6.handler == arg0) && (local6.cmd == arg1)))
				{
					return local6;
				}
			}
		}
		return null;
	}

	public Vector3 convertTextCoordinates(float arg0, float arg1, int arg2)
	{
		return new Vector3(
			(((arg0 * Osd.SCALE_3D) * vpAspect) * vpWidth),
			(((-(arg1)) * Osd.SCALE_3D) * vpHeight),
			(arg2 / 1000.0)
		);
	}

	public void addHook(GameRef arg0, GameRef arg1)
	{
		addHook(arg0, arg1, null);
	}

	public void addHook(GameRef arg0, GameRef arg1, String arg2)
	{
		arg1.addNotification(arg0, GameType.EVENT_CURSOR, GameType.EVENT_SAME, null, ("event_handler" + arg2));
	}

	public Menu createMenu(Style arg0, float arg1, float arg2, float arg3, int arg4)
	{
		return new Menu(this, arg0, arg1, arg2, arg3, arg4);
	}

	public Menu createMenu(Style arg0, float arg1, float arg2, float arg3)
	{
		return new Menu(this, arg0, arg1, arg2, arg3);
	}

	public Menu createMenu(Style arg0, float arg1, float arg2, float arg3, Style arg4, Style arg5)
	{
		Menu local7 = new Menu(this, arg0, arg1, arg2, arg3);
		local7.setSliderStyle(arg4, arg5);
		return local7;
	}

	public Text createHeader(String arg0)
	{
		if (Osd.RRT_HEADERBG)
		{
			createRectangle(0.0, (-(0.95000005)), 2.0, 0.1, (-(1)), Osd.RRT_HEADERBG);
		}
		Text local2 = createText(arg0, Frontend.largeFont, Text.ALIGN_LEFT, (-(0.95000005)), (-(0.98)));
		return local2;
	}

	public Rectangle createBG(ResourceRef arg0)
	{
		return createRectangle(0, 0, 2, 2, (-(2)), arg0);
	}

	public Rectangle createRectangle(float arg0, float arg1, float arg2, float arg3, int arg4, ResourceRef arg5)
	{
		Group local7 = groups.lastElement();
		Vector3 local8 = convertTextCoordinates(arg0, arg1, arg4);
		Rectangle local9 = new Rectangle(
			local7,
			local8,
			new RectangleTemplate((((arg2 * Osd.SCALE_FS) * vpAspect) * vpWidth), ((arg3 * Osd.SCALE_FS) * vpHeight), arg5)
		);
		rectangles.addElement(local9);
		return local9;
	}

	public Rectangle createRectangle(float arg0, float arg1, float arg2, float arg3, float arg4, float arg5, float arg6, int arg7, ResourceRef arg8)
	{
		Group local10 = groups.lastElement();
		arg0 += (((arg5 * arg2) * arg4) / vpAspect);
		arg1 += (arg6 * arg3);
		Vector3 local11 = convertTextCoordinates(arg0, arg1, arg7);
		Rectangle local12 = new Rectangle(
			local10,
			local11,
			new RectangleTemplate((((arg2 * Osd.SCALE_FS) * arg4) * vpWidth), ((arg3 * Osd.SCALE_FS) * vpHeight), arg8)
		);
		rectangles.addElement(local12);
		return local12;
	}

	public RenderRef createObject(int arg0, float arg1, float arg2, int arg3)
	{
		Vector3 local5 = convertTextCoordinates(arg1, arg2, arg3);
		return createObject(arg0, local5);
	}

	public RenderRef createObject(int arg0, float arg1, float arg2)
	{
		return createObject(arg0, arg1, arg2, 0);
	}

	public RenderRef createObject(int arg0, Vector3 arg1)
	{
		return createObject(arg0, arg1, null);
	}

	public RenderRef createObject(int arg0, Vector3 arg1, Ypr arg2)
	{
		GameRef local4 = groups.lastElement();
		RenderRef local5 = new RenderRef(local4, arg0, null);
		local5.setMatrix(arg1, arg2);
		object.addElement(local5);
		return local5;
	}

	public Text createText(String arg0, ResourceRef arg1, int arg2, float arg3, float arg4, int arg5)
	{
		Group local7 = groups.lastElement();
		Text local8 = new Text(local7, arg1, arg0, arg3, arg4, arg5, vp);
		local8.changeAlign(arg2);
		text.addElement(local8);
		return local8;
	}

	public Text createText(String arg0, ResourceRef arg1, int arg2, float arg3, float arg4)
	{
		return createText(arg0, arg1, arg2, arg3, arg4, 0);
	}

	public TextBox createTextBox(String arg0, ResourceRef arg1, int arg2, float arg3, float arg4, float arg5)
	{
		return createTextBox(arg0, arg1, arg2, arg3, arg4, arg5, null, 2000);
	}

	public TextBox createTextBox(String arg0, ResourceRef arg1, int arg2, float arg3, float arg4, float arg5, ResourceRef arg6)
	{
		return createTextBox(arg0, arg1, arg2, arg3, arg4, arg5, arg6, 2000);
	}

	public TextBox createTextBox(String arg0, ResourceRef arg1, int arg2, float arg3, float arg4, float arg5, ResourceRef arg6, int arg7)
	{
		Group local9 = groups.lastElement();
		TextBox local10 = new TextBox(local9, arg1, vp, arg2, arg0, arg3, arg4, arg5, arg6, arg7);
		textbox.addElement(local10);
		return local10;
	}

	public Button createButton(Style arg0, float arg1, float arg2, int arg3, String arg4)
	{
		Button local6 = createButton(arg0, arg1, arg2, 0, null, arg3);
		local6.setToolTip(arg4);
		local6.highliteMethod = Osd.HL_BACKLIGHT;
		return local6;
	}

	public Button createButton(Style arg0, float arg1, float arg2, String arg3, int arg4)
	{
		return createButton(arg0, arg1, arg2, 0, arg3, arg4);
	}

	public Button createButton(Style arg0, float arg1, float arg2, int arg3, String arg4, int arg5)
	{
		Group local7 = groups.lastElement();
		float local8 = ((((arg0.width / 2) * arg0.aspect) * arg0.aspect) / vpAspect);
		float local9;
		if ((arg0.align == Text.ALIGN_LEFT))
		{
			local9 = (arg1 + local8);
		} else
		{
			if ((arg0.align == Text.ALIGN_RIGHT))
			{
				local9 = (arg1 - local8);
			} else
			{
				local9 = arg1;
			}
		}
		Vector3 local10 = convertTextCoordinates(local9, arg2, arg3);
		Rectangle local11;
		if (arg0.rt)
		{
			local11 = new Rectangle(local7, local10, arg0.rt);
		}
		PhysicsRef local12 = new PhysicsRef();
		local12.createBox(local7, arg0.rWidth, arg0.rHeight, 0.0009999999, arg4);
		local12.setMatrix(local10, null);
		Button local13 = new Button(this, local11, local12, arg5, globalHandler);
		local7.gadget.addElement(local13);
		if (arg4)
		{
			if ((arg0.align == Text.ALIGN_LEFT))
			{
				arg1 += (arg0.width * 0.1);
			} else
			{
				if ((arg0.align == Text.ALIGN_RIGHT))
				{
					arg1 -= (arg0.width * 0.1);
				}
			}
			arg2 -= (Text.getFontSize(arg0.charset) / (Config.video_y * vp.getHeight()));
			Text local14 = createText(arg4, arg0.charset, arg0.align, arg1, arg2);
			local14.changeColor(Osd.COLOR_UNSELECTED);
			local13.setLabel(local14);
		}
		return local13;
	}

	public Slider createSlider(Style arg0, Style arg1, float arg2, float arg3, String arg4, int arg5, float arg6, float arg7, float arg8, int arg9)
	{
		Slider local11 = createSlider(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
		local11.setRange(arg7, arg8);
		local11.setTicks(arg9);
		local11.setValue(arg6);
		return local11;
	}

	public Slider createSlider(Style arg0, Style arg1, float arg2, float arg3, String arg4, int arg5, float arg6)
	{
		Group local8 = groups.lastElement();
		float local9 = ((((arg0.width / 2) * arg0.aspect) * arg0.aspect) / vpAspect);
		float local10 = ((((arg1.width / 2) * arg1.aspect) * arg1.aspect) / vpAspect);
		float local11;
		if ((arg0.align == Text.ALIGN_LEFT))
		{
			local11 = (arg2 + local9);
		} else
		{
			if ((arg0.align == Text.ALIGN_RIGHT))
			{
				local11 = (arg2 - local9);
			} else
			{
				local11 = arg2;
			}
		}
		Rectangle local12;
		local12 = new Rectangle(local8, null, arg1.rt);
		PhysicsRef local13 = new PhysicsRef();
		local13.createBox(local8, arg1.rWidth, arg1.rHeight, 0.0009999999, "knob");
		Slider local14 = new Slider(
			this,
			local12,
			local13,
			(local11 - ((local9 - local10) / vpWidth)),
			(local11 + ((local9 - local10) / vpWidth)),
			arg3,
			arg5,
			globalHandler
		);
		Button local15 = createButton(arg0, arg2, arg3, null, arg5);
		local15.parent = local14;
		local15.nofocus = 1;
		local14.button = local15;
		local14.setValue(arg6);
		arg3 -= (Text.getFontSize(arg0.charset) / (Config.video_y * vp.getHeight()));
		Text local16 = createText(
			arg4,
			arg0.charset,
			Text.ALIGN_RIGHT,
			(local11 - ((local9 * 1.1) / vpWidth)),
			arg3
		);
		local16.changeColor(Osd.COLOR_UNSELECTED);
		local14.setLabel(local16);
		local16 = createText(null, arg0.charset, Text.ALIGN_LEFT, (local11 + ((local9 * 1.1) / vpWidth)), arg3);
		local16.changeColor(Osd.COLOR_UNSELECTED);
		local14.setVLabel(local16);
		local8.gadget.addElement(local14);
		return local14;
	}

	public MultiChoice createMulti(Style arg0, float arg1, float arg2, String arg3, int arg4, int arg5, Vector arg6, int arg7)
	{
		MultiChoice local9 = createMulti(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
		local9.setOffset(arg7);
		local9.setValue(arg5);
		return local9;
	}

	public MultiChoice createMulti(Style arg0, float arg1, float arg2, String arg3, int arg4, int arg5, Vector arg6)
	{
		Group local8 = groups.lastElement();
		float local9 = ((((arg0.width / 2) * arg0.aspect) * arg0.aspect) / vpAspect);
		float local10;
		if ((arg0.align == Text.ALIGN_LEFT))
		{
			local10 = (arg1 + local9);
		} else
		{
			if ((arg0.align == Text.ALIGN_RIGHT))
			{
				local10 = (arg1 - local9);
			} else
			{
				local10 = arg1;
			}
		}
		Vector3 local11 = convertTextCoordinates(local10, arg2, 0);
		Rectangle local12;
		if (arg0.rt)
		{
			local12 = new Rectangle(local8, local11, arg0.rt);
		}
		PhysicsRef local13 = new PhysicsRef();
		local13.createBox(local8, arg0.rWidth, arg0.rHeight, 0.0009999999, arg3);
		local13.setMatrix(local11, null);
		MultiChoice local14 = new MultiChoice(this, local12, local13, arg6, arg4, globalHandler);
		arg2 -= (Text.getFontSize(arg0.charset) / (Config.video_y * vp.getHeight()));
		Text local15 = createText(arg3, arg0.charset, Text.ALIGN_RIGHT, (local10 - (local9 / vpWidth)), arg2);
		local15.changeColor(Osd.COLOR_UNSELECTED);
		local14.setLabel(local15);
		if ((arg0.align == Text.ALIGN_LEFT))
		{
			arg1 += (arg0.width * 0.1);
		} else
		{
			if ((arg0.align == Text.ALIGN_RIGHT))
			{
				arg1 -= (arg0.width * 0.1);
			}
		}
		local14.ccText = createText(null, arg0.charset, arg0.align, arg1, arg2);
		local14.ccText.changeColor(Osd.COLOR_UNSELECTED);
		local14.setValue(arg5);
		local8.gadget.addElement(local14);
		return local14;
	}

	public StringInput createStringInput(Style arg0, float arg1, float arg2, String arg3, int arg4, String arg5)
	{
		Group local7 = groups.lastElement();
		float local8;
		if ((arg0.align == Text.ALIGN_LEFT))
		{
			local8 = (arg1 + (arg0.width / 2.0));
		} else
		{
			if ((arg0.align == Text.ALIGN_RIGHT))
			{
				local8 = (arg1 - (arg0.width / 2.0));
			} else
			{
				local8 = arg1;
			}
		}
		Vector3 local9 = convertTextCoordinates(local8, arg2, 0);
		Rectangle local10;
		if (arg0.rt)
		{
			local10 = new Rectangle(local7, local9, arg0.rt);
		}
		PhysicsRef local11 = new PhysicsRef();
		local11.createBox(local7, arg0.rWidth, arg0.rHeight, 0.0009999999, arg3);
		local11.setMatrix(local9, null);
		StringInput local12 = new StringInput(this, local10, local11, arg4, globalHandler);
		arg2 -= (Text.getFontSize(arg0.charset) / (Config.video_y * vp.getHeight()));
		Text local13 = createText(arg3, arg0.charset, Text.ALIGN_RIGHT, (local8 - (arg0.width / 2.0)), arg2);
		local13.changeColor(Osd.COLOR_UNSELECTED);
		local12.setLabel(local13);
		if ((arg0.align == Text.ALIGN_LEFT))
		{
			arg1 += (arg0.width * 0.1);
		} else
		{
			if ((arg0.align == Text.ALIGN_RIGHT))
			{
				arg1 -= (arg0.width * 0.1);
			}
		}
		local12.inputText = createText(null, arg0.charset, arg0.align, arg1, arg2);
		local12.inputText.changeColor(Osd.COLOR_UNSELECTED);
		local12.setValue(arg5);
		local7.gadget.addElement(local12);
		return local12;
	}

	public GameRef createIcon(int arg0, float arg1, float arg2, GameRef arg3, String arg4)
	{
		GameRef local6 = groups.lastElement();
		GameRef local7 = new GameRef(
			local6,
			arg0,
			(convertTextCoordinates(arg1, arg2, 0).toString() + " 0.0, 0.0, 0.0"),
			null
		);
		addHook(local7, arg3, arg4);
		return local7;
	}

	public Button createButton(int arg0, float arg1, float arg2, String arg3, ResourceRef arg4, int arg5, Object arg6, int arg7)
	{
		GameRef local9 = createIcon(arg0, arg1, arg2, this, null);
		arg2 += ((0.1 - (Text.getFontSize(arg4) / (Config.video_y * vp.getHeight()))) / 2);
		Text local10 = createText(arg3, arg4, arg5, arg1, arg2);
		local10.changeColor(Osd.COLOR_UNSELECTED);
		textIndex[buttons] = (text.size() - 1);
		description[buttons] = null;
		button[buttons] = local9;
		buttonHandler[buttons] = arg6;
		buttonCommand[buttons++] = arg7;
		return local9;
	}

	public GameRef createButton(int arg0, float arg1, float arg2, String arg3, ResourceRef arg4, int arg5, GameRef arg6, String arg7)
	{
		GameRef local9 = createIcon(arg0, arg1, arg2, arg6, arg7);
		arg2 += ((0.1 - (Text.getFontSize(arg4) / (Config.video_y * vp.getHeight()))) / 2);
		Text local10 = createText(arg3, arg4, arg5, arg1, arg2);
		local10.changeColor(Osd.COLOR_UNSELECTED);
		textIndex[buttons] = (text.size() - 1);
		description[buttons] = null;
		button[buttons] = local9;
		buttonCommand[buttons++] = 0;
		return local9;
	}

	public GameRef createButton(int arg0, float arg1, float arg2, String arg3, ResourceRef arg4, int arg5, GameRef arg6, String arg7, int arg8)
	{
		GameRef local10 = createIcon(arg0, arg1, arg2, arg6, arg7);
		arg2 += ((0.1 - (Text.getFontSize(arg4) / (Config.video_y * vp.getHeight()))) / 2);
		Text local11 = createText(arg3, arg4, arg5, arg1, arg2);
		local11.changeColor(Osd.COLOR_UNSELECTED);
		textIndex[buttons] = (text.size() - 1);
		description[buttons] = null;
		button[buttons] = local10;
		buttonCommand[buttons++] = arg8;
		return local10;
	}

	public GameRef createButton(int arg0, float arg1, float arg2, String arg3, ResourceRef arg4, int arg5, GameRef arg6, String arg7, int arg8, String arg9)
	{
		GameRef local11 = createIcon(arg0, arg1, arg2, arg6, arg7);
		arg2 += ((0.1 - (Text.getFontSize(arg4) / (Config.video_y * vp.getHeight()))) / 2);
		Text local12 = createText(arg3, arg4, arg5, arg1, arg2);
		local12.changeColor(Osd.COLOR_UNSELECTED);
		textIndex[buttons] = (text.size() - 1);
		description[buttons] = arg9;
		button[buttons] = local11;
		buttonCommand[buttons++] = arg8;
		return local11;
	}

	public void createMenuKeys()
	{
		createHotkey(
			Input.AXIS_MENU_LEFT,
			(Input.VIRTUAL | Osd.HK_STATIC),
			(Osd.CMD_MENU_LF | Osd.PRIVATEEVENT),
			null
		);
		createHotkey(
			Input.AXIS_MENU_RIGHT,
			(Input.VIRTUAL | Osd.HK_STATIC),
			(Osd.CMD_MENU_RG | Osd.PRIVATEEVENT),
			null
		);
		createHotkey(
			Input.AXIS_MENU_UP,
			(Input.VIRTUAL | Osd.HK_STATIC),
			(Osd.CMD_MENU_UP | Osd.PRIVATEEVENT),
			null
		);
		createHotkey(
			Input.AXIS_MENU_DOWN,
			(Input.VIRTUAL | Osd.HK_STATIC),
			(Osd.CMD_MENU_DN | Osd.PRIVATEEVENT),
			null
		);
		createHotkey(
			Input.AXIS_SELECT,
			(Input.VIRTUAL | Osd.HK_STATIC),
			(Osd.CMD_MENU_SEL | Osd.PRIVATEEVENT),
			null,
			(Event.F_KEY_PRESS | Event.F_KEY_RELEASE)
		);
		menuKeysCreated = 1;
	}

	public Hotkey createHotkey(int arg0, int arg1, int arg2, GameRef arg3)
	{
		return createHotkey(arg0, arg1, arg2, arg3, Event.F_KEY_PRESS);
	}

	public Hotkey createHotkey(int arg0, int arg1, int arg2, GameRef arg3, int arg4)
	{
		Hotkey local6;
		if ((arg1 & Osd.HK_STATIC))
		{
			arg1 &= (~(Osd.HK_STATIC));
			local6 = new Hotkey(arg0, arg1, arg2, arg3, this, arg4);
			hotkey.addElement(local6);
			local6.activate();
		} else
		{
			Group local7 = groups.lastElement();
			local6 = new Hotkey(arg0, arg1, arg2, arg3, this, arg4);
			local7.hotkey.addElement(local6);
			if (local7.active)
			{
				local6.activate();
			}
		}
		return local6;
	}

	public Hotkey createHotkey(int arg0, int arg1, int arg2, int arg3)
	{
		Group local5 = groups.lastElement();
		Hotkey local6;
		if ((arg1 & Osd.HK_STATIC))
		{
			arg1 &= (~(Osd.HK_STATIC));
			local6 = new Hotkey(arg0, arg1, arg2, null, this, arg3);
			hotkey.addElement(local6);
			local6.activate();
		} else
		{
			local6 = new Hotkey(arg0, arg1, arg2, null, this, arg3);
			local5.hotkey.addElement(local6);
			if (local5.active)
			{
				local6.activate();
			}
		}
		return local6;
	}

	public int endGroup()
	{
		groups.addElement(new Group(this));
		return (groups.size() - 2);
	}

	public int beginGroup()
	{
		groups.addElement(new Group(this));
		return (groups.size() - 1);
	}

	public void hideGroup(int arg0)
	{
		Group local2 = groups.elementAt(arg0);
		local2.deactivate();
	}

	public void showGroup(int arg0)
	{
		Group local2 = groups.elementAt(arg0);
		local2.activate();
	}

	public void resetSelection()
	{
		deactivateText();
		pointedGadget = null;
		changeSelection(defSelection, 0);
		if (pointedGadget)
		{
			if ((pointedGadget instanceof StringInput))
			{
				pointedGadget.click();
			}
		}
	}

	public void setFocus(int arg0)
	{
		Input.flushKeys();
		if (arg0)
		{
			Frontend.inputQueue.addElement(this);
		} else
		{
			int local2 = Frontend.inputQueue.lastIndexOf(this);
			if ((local2 >= 0))
			{
				Frontend.inputQueue.removeElementAt(local2);
			}
		}
	}

	public int isFocused()
	{
		return ((iLevel <= Osd.IL_TIPS) || (Frontend.inputQueue.lastElement() == this));
	}

	public void setCommandText(int arg0, String arg1)
	{
		int local3 = 0;
		while ((local3 < buttons))
		{
			if ((arg0 == buttonCommand[local3]))
			{
				Text local4 = text.elementAt(textIndex[local3]);
				if (local4)
				{
					local4.changeText(arg1);
				}
				break;
			}
			local3++;
		}
	}

	public void pressEffect()
	{
	}

	public void pressEffectStop()
	{
	}

	public void hoverEffect()
	{
	}

	public void leaveEffect()
	{
	}

}
