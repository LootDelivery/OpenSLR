package java.render.osd.dialog;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.util.*;
import java.util.resource.*;

public class Dialog extends GameType
{
	static final int RID_DIALOGBG = frontend:0xer;
	static final float DEF_SIZE = 0.6;
	static final float DEF_ASPECT = (4.0 / 3.0);
	static final float DEF_BODYRATIO = 0.90000004;
	static final int DF_MODAL = 1;
	static final int DF_FULLSCREEN = 2;
	static final int DF_DEFAULTBG = 4;
	static final int DF_FREEZE = 8;
	static final int DF_LEAVEPOINTER = 16;
	static final int DF_LOWPRI = 32;
	static final int DF_HIGHPRI = 64;
	static final int DF_WIDE = 128;
	static final int DF_DARKEN = 256;
	static final int DF_SILENT = 512;
	static final int SIF_NOEMPTY = 4096;
	static final int FRF_SAVE = 4096;
	int priority = 22;
	Osd osd;
	int flags;
	String title;
	String buttonTexts;
	float timewarp;
	int result;
	int escapeCmd = (-(1));
	Controller controller;
	ControlSetState css;
	int prevPointerState;
	Osd backShieldOsd;
	float bodyRatio = Dialog.DEF_BODYRATIO;
	int shown;

	private Dialog()
	{
		super();
		System.log("private constructor access (Dialog)");
	}

	public Dialog(Controller arg0, int arg1, String arg2, String arg3)
	{
		this(arg0, arg1, arg2, arg3, Dialog.DEF_SIZE, Dialog.DEF_ASPECT, 0.5, 0.5, (-(0.5)), (-(0.5)));
	}

	public Dialog(Controller arg0, int arg1, String arg2, String arg3, float arg4, float arg5)
	{
		this(arg0, arg1, arg2, arg3, arg4, arg5, 0.5, 0.5, (-(0.5)), (-(0.5)));
	}

	public Dialog(Controller arg0, int arg1, String arg2, String arg3, float arg4, float arg5, float arg6, float arg7, float arg8, float arg9)
	{
		super();
		createNativeInstance();
		controller = arg0;
		flags = arg1;
		title = arg2;
		buttonTexts = arg3;
		if ((flags & Dialog.DF_HIGHPRI))
		{
			priority += 2;
		} else
		{
			if ((flags & Dialog.DF_LOWPRI))
			{
				priority -= 2;
			}
		}
		if ((flags & Dialog.DF_FULLSCREEN))
		{
			osd = new Osd(1.0, 0.0, priority);
		} else
		{
			if ((flags & Dialog.DF_WIDE))
			{
				osd = new Osd(0.90000004, (6.0 / 3.0), priority);
			} else
			{
				osd = new Osd(arg4, arg5, arg6, arg7, arg8, arg9, priority);
			}
		}
		if ((flags & Dialog.DF_DEFAULTBG))
		{
			osd.createBG(new ResourceRef(Dialog.RID_DIALOGBG));
		}
		if (title)
		{
			osd.createText(
				title,
				Frontend.mediumFont,
				Text.ALIGN_CENTER,
				0.0,
				((2 * ((1 - bodyRatio) / 4)) - 1)
			);
		}
		osd.globalHandler = this;
	}

	public void show()
	{
		new SfxRef(Frontend.SFX_MENU_MOVE).precache();
		new SfxRef(Frontend.SFX_MENU_SELECT).precache();
		new SfxRef(Frontend.SFX_QUESTION).precache();
		shown = 1;
		if ((flags & Dialog.DF_FREEZE))
		{
			timewarp = System.timeWarp(0.0);
		}
		Osd local1;
		if ((flags & Dialog.DF_MODAL))
		{
			backShieldOsd = new Osd(1.0, 0.0, (priority - 1));
			backShieldOsd.iLevel = Osd.IL_NONE;
			backShieldOsd.show();
			local1 = backShieldOsd;
			osd.flags |= Osd.OF_MODAL;
		} else
		{
			local1 = osd;
		}
		ResourceRef local2;
		if ((flags & Dialog.DF_DARKEN))
		{
			local2 = Osd.RRT_DARKEN;
		}
		Style local3 = new Style(2.0, 2.0, local1.vpAspect, Frontend.mediumFont, Text.ALIGN_CENTER, local2);
		local1.createButton(local3, 0.0, 0.0, (-(2)), " ", 9999999).nofocus = 1;
		float local4;
		float local5;
		StringTokenizer local6 = new StringTokenizer(buttonTexts, ";");
		int local7 = local6.countTokens();
		if (local7)
		{
			Style local8 = new Style(
				0.3,
				0.1,
				Frontend.mediumFont,
				Text.ALIGN_CENTER,
				new ResourceRef(frontend:0xd8r)
			);
			float local9 = (2.0 / (local7 + 1));
			local5 = ((2 * (bodyRatio + (((1 - bodyRatio) / 4) * 3))) - 1);
			local4 = (-(1));
			int local10 = 0;
			while ((local10 < local7))
			{
				local4 += local9;
				osd.createButton(local8, local4, local5, local6.nextToken(), local10);
				local10++;
			}
			if ((escapeCmd < 0))
			{
				escapeCmd = osd.defSelection;
			}
			osd.orientation = 1;
			osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, escapeCmd, this);
		}
		if (((!(flags)) & Dialog.DF_SILENT))
		{
			new SfxRef(Frontend.SFX_QUESTION).play();
		}
		osd.show();
		if (controller)
		{
			css = controller.reset();
			controller.activateState(ControlSet.MENUSET);
		}
		if ((!((flags & Dialog.DF_LEAVEPOINTER))))
		{
			prevPointerState = Input.cursor.enable(1);
		}
		setEventMask((GameRef.EVENT_CURSOR | GameRef.EVENT_COMMAND));
	}

	public void hide()
	{
		shown = 0;
		clearEventMask(GameRef.EVENT_ANY);
		if ((!((flags & Dialog.DF_LEAVEPOINTER))))
		{
			Input.cursor.enable(prevPointerState);
		}
		if (controller)
		{
			controller.reset(css);
		}
		osd.hide();
		if (backShieldOsd)
		{
			backShieldOsd.hide();
			backShieldOsd = null;
		}
		if ((flags & Dialog.DF_FREEZE))
		{
			System.timeWarp(timewarp);
		}
	}

	public void osdCommand(int arg0)
	{
		result = arg0;
		notify();
	}

	public int display()
	{
		if ((!(shown)))
		{
			show();
		}
		wait();
		hide();
		return result;
	}

}

public class YesNoDialog extends TextDialog
{
	public YesNoDialog(Controller arg0, int arg1, String arg2, String arg3)
	{
		super(arg0, arg1, arg2, "YES;NO", arg3);
		escapeCmd = 1;
	}

	public YesNoDialog(Controller arg0, int arg1, String arg2, String arg3, float arg4, float arg5)
	{
		super(arg0, arg1, arg2, "YES;NO", arg3, arg4, arg5);
		escapeCmd = 1;
	}

}

public class NoYesDialog extends TextDialog
{
	public NoYesDialog(Controller arg0, int arg1, String arg2, String arg3)
	{
		super(arg0, arg1, arg2, "YES;NO", arg3);
		osd.defSelection = 1;
	}

	public NoYesDialog(Controller arg0, int arg1, String arg2, String arg3, float arg4, float arg5)
	{
		super(arg0, arg1, arg2, "YES;NO", arg3, arg4, arg5);
		osd.defSelection = 1;
	}

}

public class OKCancelDialog extends TextDialog
{
	public OKCancelDialog(Controller arg0, int arg1, String arg2, String arg3)
	{
		super(arg0, arg1, arg2, "OK;Cancel", arg3);
	}

	public OKCancelDialog(Controller arg0, int arg1, String arg2, String arg3, float arg4, float arg5)
	{
		super(arg0, arg1, arg2, "OK;Cancel", arg3, arg4, arg5);
	}

}

public class WarningDialog extends TextDialog
{
	public WarningDialog(Controller arg0, int arg1, String arg2, String arg3)
	{
		super(arg0, arg1, arg2, "OK", arg3);
	}

	public WarningDialog(Controller arg0, int arg1, String arg2, String arg3, float arg4, float arg5)
	{
		super(arg0, arg1, arg2, "OK", arg3, arg4, arg5);
	}

}

public class StringRequesterDialog extends OKCancelDialog
{
	String input;

	public StringRequesterDialog(Controller arg0, int arg1, String arg2, String arg3)
	{
		super(arg0, arg1, arg2, "OK;Cancel", null);
		input = arg3;
	}

	public void show()
	{
		Style local1 = new Style(
			0.75,
			0.120000005,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(frontend:0xd8r)
		);
		osd.createStringInput(local1, 0.0, 0.0, null, 12345, input);
		super.show();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == 12345))
		{
			input = osd.stringValue;
			if ((!(((flags & Dialog.SIF_NOEMPTY) && (input.length() == 0)))))
			{
				if ((!(osd.actGadget)))
				{
					osdCommand(0);
				}
			}
		} else
		{
			if ((arg0 == 0))
			{
				if (((flags & Dialog.SIF_NOEMPTY) && (input.length() == 0)))
				{
					return;
				}
			}
			super.osdCommand(arg0);
		}
	}

}

public class FileRequesterDialog extends OKCancelDialog
{
	static final int MAX_ENTRIES = 5;
	static final int CMD_ENTRIES = 100;
	String path;
	String mask;
	Button[] fileButtons;
	String fileName;
	Vector fileNameList = new Vector();
	int index;

	public FileRequesterDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, String arg5)
	{
		this(arg0, arg1, arg2, arg3, arg4, arg5, FindFile.FILES_ONLY);
	}

	public FileRequesterDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, String arg5, int arg6)
	{
		super(arg0, arg1, arg2, (arg3 + ";Cancel"), null);
		path = arg4;
		mask = arg5;
		FindFile local8 = new FindFile();
		String local9 = local8.first((arg4 + arg5), arg6);
		while (local9)
		{
			if (validator((arg4 + local9)))
			{
				fileNameList.addElement(local9);
			}
			local9 = local8.next();
		}
		local8.close();
	}

	public int validator(String arg0)
	{
		return 1;
	}

	public void show()
	{
		Style local1;
		Menu local2;
		local1 = new Style(0.5, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_NONE);
		local2 = osd.createMenu(local1, 0.0, (-(0.55)), 0.11);
		fileButtons = new Button[FileRequesterDialog.MAX_ENTRIES];
		int local3 = 0;
		while ((local3 < FileRequesterDialog.MAX_ENTRIES))
		{
			fileButtons[local3] = local2.addItem("", (FileRequesterDialog.CMD_ENTRIES + local3));
			local3++;
		}
		local1 = new Style(0.5, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_NONE);
		osd.createStringInput(local1, 0.0, 0.54, null, 200, "");
		osd.createText("FileName:", Frontend.mediumFont, Text.ALIGN_RIGHT, (-(0.5)), 0.52);
		local1 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWUP)
		);
		osd.createButton(local1, 0.55, (-(0.55)), 201, null);
		local1 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWDN)
		);
		osd.createButton(local1, 0.55, 0.25, 202, null);
		setButtonNames();
		super.show();
	}

	public void setButtonNames()
	{
		int local1 = 0;
		while ((local1 < FileRequesterDialog.MAX_ENTRIES))
		{
			if ((fileNameList.size() > (index + local1)))
			{
				fileButtons[local1].changeLabelText(fileNameList.elementAt((index + local1)));
			} else
			{
				fileButtons[local1].changeLabelText(null);
			}
			local1++;
		}
	}

	public void osdCommand(int arg0)
	{
		if (((arg0 >= FileRequesterDialog.CMD_ENTRIES) && (arg0 < (FileRequesterDialog.CMD_ENTRIES + FileRequesterDialog.MAX_ENTRIES))))
		{
			arg0 -= FileRequesterDialog.CMD_ENTRIES;
			fileName = fileButtons[arg0].getLabelText();
			osdCommand(0);
		} else
		{
			if ((arg0 == 200))
			{
				fileName = osd.stringValue;
				if ((!(osd.actGadget)))
				{
					osdCommand(0);
				}
			} else
			{
				if ((arg0 == 201))
				{
					if (index)
					{
						index -= FileRequesterDialog.MAX_ENTRIES;
						setButtonNames();
					}
				} else
				{
					if ((arg0 == 202))
					{
						if (((index + FileRequesterDialog.MAX_ENTRIES) < fileNameList.size()))
						{
							index += FileRequesterDialog.MAX_ENTRIES;
							setButtonNames();
						}
					} else
					{
						if ((arg0 == 0))
						{
							if (((!(fileName)) || (fileName.length() == 0)))
							{
								return;
							}
							if ((!(validator((path + fileName)))))
							{
								if ((!((flags & Dialog.FRF_SAVE))))
								{
									new WarningDialog(
										controller,
										((Dialog.DF_HIGHPRI | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
										title,
										"File system error.").display(
									);
									return;
								}
							} else
							{
								if ((flags & Dialog.FRF_SAVE))
								{
									if (new NoYesDialog(controller, ((Dialog.DF_HIGHPRI | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG), title, "Overwrite existing file?").display())
									{
										return;
									}
								}
							}
						}
						super.osdCommand(arg0);
					}
				}
			}
		}
	}

}

public class MenuDialog extends Dialog
{
	String info;
	String[] labels;

	public MenuDialog(Controller arg0, int arg1, String arg2, String arg3, String[] arg4)
	{
		super(arg0, (arg1 | Dialog.DF_DARKEN), arg2, null);
		labels = arg4;
		info = arg3;
	}

	public void show()
	{
		float local1 = (-(0.5));
		if (info)
		{
			osd.createText(info, Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, local1);
			local1 += 0.2;
		}
		Style local2 = new Style(
			0.45,
			0.1,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(frontend:0xd8r)
		);
		Menu local3 = osd.createMenu(local2, 0.0, local1, 0);
		int local4 = 0;
		while ((local4 < labels.length))
		{
			local3.addItem(labels[local4], (local4 + 1));
			local4++;
		}
		local3.addSeparator();
		local2 = new Style(
			0.25,
			0.120000005,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(frontend:0xd8r)
		);
		local3.setStyle(local2);
		local3.addItem("CANCEL", 0);
		super.show();
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 < (labels.length + 1)))
		{
			result = arg0;
			notify();
		}
	}

}

public class TextDialog extends Dialog
{
	String text;

	public TextDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4)
	{
		super(arg0, (arg1 | Dialog.DF_DARKEN), arg2, arg3);
		text = arg4;
	}

	public TextDialog(Controller arg0, int arg1, String arg2, String arg3, String arg4, float arg5, float arg6)
	{
		super(arg0, (arg1 | Dialog.DF_DARKEN), arg2, arg3, arg5, arg6);
		text = arg4;
	}

	public void show()
	{
		if (text)
		{
			float local1 = (-(0.85));
			float local2 = (-(0.7));
			float local3 = 1.7;
			osd.createTextBox(text, Frontend.mediumFont, Text.ALIGN_CENTER, local1, local2, local3);
		}
		super.show();
	}

}

public class CsDialog extends SimpleLoadingDialog
{
	public CsDialog(Controller arg0, int arg1, ResourceRef arg2)
	{
		super((arg1 | Dialog.DF_FULLSCREEN), arg2);
		controller = arg0;
	}

}

public class CongratsDialog extends Dialog
{
	GameRef buttonOk;
	int bgID;

	public CongratsDialog(Controller arg0, int arg1, int arg2)
	{
		super(arg0, (arg1 | Dialog.DF_DARKEN), null, null);
		bgID = arg2;
	}

	public void show()
	{
		osd.createBG(new ResourceRef(bgID));
		Style local1 = new Style(
			0.45,
			0.120000005,
			Frontend.mediumFont,
			Text.ALIGN_RIGHT,
			new ResourceRef(frontend:0xd8r)
		);
		osd.createButton(local1, 1.0, 0.88, "OK", 0);
		osd.createHotkey(Input.AXIS_CANCEL, Input.VIRTUAL, osd.defSelection, this);
		super.show();
	}

}
