package java.game;

import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class TrackSelect extends Scene implements GameState
{
	static final int CMD_BACK = 0;
	static final int CMD_OK = 1;
	static final int CMD_ABOUT = 2;
	static final int CMD_UP = 3;
	static final int CMD_DOWN = 4;
	static final int CMD_MAP0 = 10;
	static final int CMD_MAP1 = 11;
	static final int CMD_MAP2 = 12;
	static final int CMD_MAP3 = 13;
	static final int CMD_MAP4 = 14;
	static final int CMD_MAP5 = 15;
	static final int CMD_MAP6 = 16;
	static final int CMD_MAP7 = 17;
	static final int CMD_MAP8 = 18;
	static final int CMD_MAP9 = 19;
	static final int Mode;
	static final int BackGroundVideo;
	static final int MapsAmount;
	static final int CurrentPage;
	static final int CurrentName;
	static final int CurrentMapNumber;
	static final int TrackSelected;
	static final String MapName;
	static final String MapAuthor;
	static final String MapDescription;
	static final ResourceRef MapImage;
	Rectangle CurrentTrackImage;
	Button OkButton;
	Button UpButton;
	Button DownButton;
	Button Map0_Button;
	Button Map1_Button;
	Button Map2_Button;
	Button Map3_Button;
	Button Map4_Button;
	Button Map5_Button;
	Button Map6_Button;
	Button Map7_Button;
	Button Map8_Button;
	Button Map9_Button;
	Style Menu_Button;
	Style bsm;
	Style Menu_Left;
	Style Menu_Right;
	Text CurrentTrackNameText;
	Text FoundTrackText;
	Text CurrentTrackAuthorText;
	Text CurrentPageText;
	TextBox CurrentTrackDescriptionText;
	Menu m;
	GameState prev_state;
	Osd osd;
	GameState parentState;
	Bot bot;
	Player player;
	Vehicle Vehicle;

	public static final float Check(float arg0)
	{
		return (Math.sqrt(arg0) + 3.75);
	}

	public TrackSelect(int arg0)
	{
		super();
		float local2 = (Math.random() * 100);
		if ((MainMenu.Check(local2) != (Math.sqrt(local2) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		TrackSelect.Mode = arg0;
		createNativeInstance();
		TrackSelect.MapsAmount = GameLogic.MapBox.size();
	}

	public TrackSelect()
	{
		super();
		float local1 = (Math.random() * 100);
		if ((MainMenu.Check(local1) != (Math.sqrt(local1) + 3.75)))
		{
			Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();
		}
		TrackSelect.Mode = 0;
		createNativeInstance();
		TrackSelect.MapsAmount = GameLogic.MapBox.size();
	}

	public void enter(GameState arg0)
	{
		Frontend.loadingScreen.show();
		osd = new Osd();
		osd.globalHandler = this;
		setEventMask(GameRef.EVENT_TIME);
		createOSDObjects();
		osd.show();
		Input.cursor.enable(1);
		setEventMask(GameRef.EVENT_CURSOR);
		if ((!(parentState)))
		{
			parentState = arg0;
		}
		if (((!(arg0)) instanceof Garage))
		{
			new SfxRef(GameLogic.SFX_LEAVEGARAGE).play();
		}
		GfxEngine.closeVideo();
		if ((TrackSelect.BackGroundVideo = (GfxEngine.openVideo("Data\\Fmv\\Prime.avi", 1, 1) == 0)))
		{
		}
		Frontend.loadingScreen.display();
	}

	public void exit(GameState arg0)
	{
		Frontend.loadingScreen.show();
		clearEventMask(GameRef.EVENT_ANY);
		removeAllTimers();
		Input.cursor.enable(0);
		osd.hide();
		osd.finalize();
		if (((!(arg0)) instanceof Garage))
		{
			new SfxRef(GameLogic.SFX_LEAVEGARAGE).play();
		}
		if (TrackSelect.BackGroundVideo)
		{
			GfxEngine.closeVideo();
		}
		Frontend.loadingScreen.display();
	}

	public void createOSDObjects()
	{
		Menu_Button = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_CENTER, null);
		bsm = new Style(0.7, 0.13, Frontend.largeFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
		Menu_Left = new Style(0.6, 0.13, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST);
		Menu_Right = new Style(0.6, 0.13, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST);
		osd.createHeader("TRACK SELECTOR");
		osd.createBG(new ResourceRef(frontend:0x16r));
		CurrentPageText = osd.createText("", Frontend.largeFont, Text.ALIGN_LEFT, (-(0.95000005)), (-(0.86)));
		CurrentPageText.changeColor(-4144960);
		if ((TrackSelect.MapsAmount > 10))
		{
			CurrentPageText.changeText(("Current Page: " + (TrackSelect.CurrentPage + 1)));
		}
		CurrentTrackNameText = osd.createText(
			GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapName,
			Frontend.largeFont,
			Text.ALIGN_CENTER,
			0.55,
			(-(0.03))
		);
		CurrentTrackNameText.changeColor(-4144960);
		CurrentTrackAuthorText = osd.createText(
			GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapAuthor,
			Frontend.largeFont,
			Text.ALIGN_CENTER,
			0.55,
			0.68
		);
		CurrentTrackAuthorText.changeColor(-4144960);
		FoundTrackText = osd.createText(
			("Found Maps: " + TrackSelect.MapsAmount),
			Frontend.largeFont,
			Text.ALIGN_CENTER,
			0.0,
			0.82
		);
		FoundTrackText.changeColor(-4144960);
		CurrentTrackImage = osd.createRectangle(
			0.52,
			(-(0.35000002)),
			0.90000004,
			0.6,
			(-(1)),
			new ResourceRef(GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapImage)
		);
		osd.createRectangle(0.52, 0.35000002, 0.90000004, 0.6, (-(1)), new ResourceRef(MW_Mod:0x30r));
		CurrentTrackDescriptionText = osd.createTextBox(
			GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapDescription,
			Frontend.smallFont,
			Text.ALIGN_CENTER,
			0.1,
			0.1,
			0.85
		);
		m = osd.createMenu(Menu_Left, (-(0.98)), 0.85, 0);
		m.addItem("BACK", TrackSelect.CMD_BACK);
		osd.createHotkey(Input.AXIS_CANCEL, (Input.VIRTUAL | Osd.HK_STATIC), TrackSelect.CMD_BACK, this);
		m = osd.createMenu(Menu_Right, 0.98, (-(0.83)), 0);
		m.addItem("ABOUT", TrackSelect.CMD_ABOUT);
		m = osd.createMenu(Menu_Right, 0.98, 0.85, 0);
		OkButton = m.addItem("TAKE IT", TrackSelect.CMD_OK);
		OkButton.enable();
		if ((TrackSelect.Mode == 1))
		{
			OkButton.changeLabelText("");
			OkButton.disable();
		}
		if ((TrackSelect.MapsAmount > 10))
		{
			m = osd.createMenu(Menu_Button, 0.0, (-(0.6)), 0, Osd.MD_VERTICAL);
			UpButton = m.addItem(new ResourceRef(frontend:0x4cr), TrackSelect.CMD_UP, "Prev Page");
			m = osd.createMenu(Menu_Button, 0.0, 0.6, 0, Osd.MD_VERTICAL);
			DownButton = m.addItem(new ResourceRef(frontend:0x84r), TrackSelect.CMD_DOWN, "Next Page");
			if ((TrackSelect.CurrentPage == 0))
			{
				UpButton.changeTexture(new ResourceRef(frontend:0xafr));
				UpButton.disable();
			}
			TrackSelect.CurrentName = (TrackSelect.CurrentPage * 10);
			if ((TrackSelect.MapsAmount < (TrackSelect.CurrentName + 10)))
			{
				DownButton.changeTexture(new ResourceRef(frontend:0xafr));
				DownButton.disable();
			}
		}
		TrackSelect.CurrentName = (TrackSelect.CurrentPage * 10);
		m = osd.createMenu(Menu_Left, (-(0.98)), (-(0.6)), 0);
		if (((TrackSelect.CurrentName + 0) <= (TrackSelect.MapsAmount - 1)))
		{
			Map0_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 0)].MapName,
				TrackSelect.CMD_MAP0
			);
		} else
		{
			Map0_Button = m.addItem("", TrackSelect.CMD_MAP0);
			Map0_Button.disable();
		}
		if (((TrackSelect.CurrentName + 1) <= (TrackSelect.MapsAmount - 1)))
		{
			Map1_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 1)].MapName,
				TrackSelect.CMD_MAP1
			);
		} else
		{
			Map1_Button = m.addItem("", TrackSelect.CMD_MAP1);
			Map1_Button.disable();
		}
		if (((TrackSelect.CurrentName + 2) <= (TrackSelect.MapsAmount - 1)))
		{
			Map2_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 2)].MapName,
				TrackSelect.CMD_MAP2
			);
		} else
		{
			Map2_Button = m.addItem("", TrackSelect.CMD_MAP2);
			Map2_Button.disable();
		}
		if (((TrackSelect.CurrentName + 3) <= (TrackSelect.MapsAmount - 1)))
		{
			Map3_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 3)].MapName,
				TrackSelect.CMD_MAP3
			);
		} else
		{
			Map3_Button = m.addItem("", TrackSelect.CMD_MAP3);
			Map3_Button.disable();
		}
		if (((TrackSelect.CurrentName + 4) <= (TrackSelect.MapsAmount - 1)))
		{
			Map4_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 4)].MapName,
				TrackSelect.CMD_MAP4
			);
		} else
		{
			Map4_Button = m.addItem("", TrackSelect.CMD_MAP4);
			Map4_Button.disable();
		}
		if (((TrackSelect.CurrentName + 5) <= (TrackSelect.MapsAmount - 1)))
		{
			Map5_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 5)].MapName,
				TrackSelect.CMD_MAP5
			);
		} else
		{
			Map5_Button = m.addItem("", TrackSelect.CMD_MAP5);
			Map5_Button.disable();
		}
		if (((TrackSelect.CurrentName + 6) <= (TrackSelect.MapsAmount - 1)))
		{
			Map6_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 6)].MapName,
				TrackSelect.CMD_MAP6
			);
		} else
		{
			Map6_Button = m.addItem("", TrackSelect.CMD_MAP6);
			Map6_Button.disable();
		}
		if (((TrackSelect.CurrentName + 7) <= (TrackSelect.MapsAmount - 1)))
		{
			Map7_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 7)].MapName,
				TrackSelect.CMD_MAP7
			);
		} else
		{
			Map7_Button = m.addItem("", TrackSelect.CMD_MAP7);
			Map7_Button.disable();
		}
		if (((TrackSelect.CurrentName + 8) <= (TrackSelect.MapsAmount - 1)))
		{
			Map8_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 8)].MapName,
				TrackSelect.CMD_MAP8
			);
		} else
		{
			Map8_Button = m.addItem("", TrackSelect.CMD_MAP8);
			Map8_Button.disable();
		}
		if (((TrackSelect.CurrentName + 9) <= (TrackSelect.MapsAmount - 1)))
		{
			Map9_Button = m.addItem(
				GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 9)].MapName,
				TrackSelect.CMD_MAP9
			);
		} else
		{
			Map9_Button = m.addItem("", TrackSelect.CMD_MAP9);
			Map9_Button.disable();
		}
		Refresh();
		TrackSelect.TrackSelected = 1;
	}

	public void Refresh()
	{
		CurrentTrackNameText.changeText(GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapName);
		CurrentTrackDescriptionText.changeText(GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapDescription);
		CurrentTrackAuthorText.changeText(GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapAuthor);
		CurrentTrackImage.changeTexture(new ResourceRef(GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapImage));
		TrackSelect.MapName = GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapName;
		TrackSelect.MapDescription = GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapDescription;
		TrackSelect.MapAuthor = GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapAuthor;
		TrackSelect.MapImage = new ResourceRef(GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].MapImage);
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == TrackSelect.CMD_BACK))
		{
			GameLogic.changeActiveSection(parentState);
		} else
		{
			if ((arg0 == TrackSelect.CMD_OK))
			{
				if (GameLogic.player.car)
				{
					GameLogic.MapBox.elementData[TrackSelect.CurrentMapNumber].StartMap();
				} else
				{
					new WarningDialog(
						GameLogic.player.controller,
						(Dialog.DF_MODAL | Dialog.DF_DEFAULTBG),
						"WARNING",
						"You need a car to do this! \n Buy a car or get one from the car lot.").display(
					);
				}
			} else
			{
				if ((arg0 == TrackSelect.CMD_ABOUT))
				{
					new WarningDialog(
						GameLogic.player.controller,
						((Dialog.DF_MODAL | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
						"TRACK SELECTOR v1.1",
						" \n Now Map Addon is easy as Car Addon! \n This Selector looking for addon maps and add it to maps list. \n Amount of new maps isn't limited! \n \n If you want know how create Addon Map take a look at ForModders.rar \n \n By Miran").display(
					);
				} else
				{
					if ((arg0 == TrackSelect.CMD_UP))
					{
						if ((TrackSelect.CurrentPage > 0))
						{
							TrackSelect.CurrentPage -= 1;
						}
						TrackSelect.CurrentName = (TrackSelect.CurrentPage * 10);
						CurrentPageText.changeText(("Current Page: " + (TrackSelect.CurrentPage + 1)));
						DownButton.changeTexture(new ResourceRef(frontend:0x84r));
						DownButton.enable();
						if ((TrackSelect.CurrentPage == 0))
						{
							UpButton.changeTexture(new ResourceRef(frontend:0xafr));
							UpButton.disable();
						}
						if (((TrackSelect.CurrentName + 0) <= (TrackSelect.MapsAmount - 1)))
						{
							Map0_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 0)].MapName);
							Map0_Button.enable();
						} else
						{
							Map0_Button.changeLabelText("");
							Map0_Button.disable();
						}
						if (((TrackSelect.CurrentName + 1) <= (TrackSelect.MapsAmount - 1)))
						{
							Map1_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 1)].MapName);
							Map1_Button.enable();
						} else
						{
							Map1_Button.changeLabelText("");
							Map1_Button.disable();
						}
						if (((TrackSelect.CurrentName + 2) <= (TrackSelect.MapsAmount - 1)))
						{
							Map2_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 2)].MapName);
							Map2_Button.enable();
						} else
						{
							Map2_Button.changeLabelText("");
							Map2_Button.disable();
						}
						if (((TrackSelect.CurrentName + 3) <= (TrackSelect.MapsAmount - 1)))
						{
							Map3_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 3)].MapName);
							Map3_Button.enable();
						} else
						{
							Map3_Button.changeLabelText("");
							Map3_Button.disable();
						}
						if (((TrackSelect.CurrentName + 4) <= (TrackSelect.MapsAmount - 1)))
						{
							Map4_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 4)].MapName);
							Map4_Button.enable();
						} else
						{
							Map4_Button.changeLabelText("");
							Map4_Button.disable();
						}
						if (((TrackSelect.CurrentName + 5) <= (TrackSelect.MapsAmount - 1)))
						{
							Map5_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 5)].MapName);
							Map5_Button.enable();
						} else
						{
							Map5_Button.changeLabelText("");
							Map5_Button.disable();
						}
						if (((TrackSelect.CurrentName + 6) <= (TrackSelect.MapsAmount - 1)))
						{
							Map6_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 6)].MapName);
							Map6_Button.enable();
						} else
						{
							Map6_Button.changeLabelText("");
							Map6_Button.disable();
						}
						if (((TrackSelect.CurrentName + 7) <= (TrackSelect.MapsAmount - 1)))
						{
							Map7_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 7)].MapName);
							Map7_Button.enable();
						} else
						{
							Map7_Button.changeLabelText("");
							Map7_Button.disable();
						}
						if (((TrackSelect.CurrentName + 8) <= (TrackSelect.MapsAmount - 1)))
						{
							Map8_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 8)].MapName);
							Map8_Button.enable();
						} else
						{
							Map8_Button.changeLabelText("");
							Map8_Button.disable();
						}
						if (((TrackSelect.CurrentName + 9) <= (TrackSelect.MapsAmount - 1)))
						{
							Map9_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 9)].MapName);
							Map9_Button.enable();
						} else
						{
							Map9_Button.changeLabelText("");
							Map9_Button.disable();
						}
					} else
					{
						if ((arg0 == TrackSelect.CMD_DOWN))
						{
							if ((TrackSelect.MapsAmount >= (TrackSelect.CurrentName + 10)))
							{
								TrackSelect.CurrentPage += 1;
							}
							TrackSelect.CurrentName = (TrackSelect.CurrentPage * 10);
							CurrentPageText.changeText(("Current Page: " + (TrackSelect.CurrentPage + 1)));
							UpButton.changeTexture(new ResourceRef(frontend:0x4cr));
							UpButton.enable();
							if ((TrackSelect.MapsAmount < (TrackSelect.CurrentName + 10)))
							{
								DownButton.changeTexture(new ResourceRef(frontend:0xafr));
								DownButton.disable();
							}
							if (((TrackSelect.CurrentName + 0) <= (TrackSelect.MapsAmount - 1)))
							{
								Map0_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 0)].MapName);
								Map0_Button.enable();
							} else
							{
								Map0_Button.changeLabelText("");
								Map0_Button.disable();
							}
							if (((TrackSelect.CurrentName + 1) <= (TrackSelect.MapsAmount - 1)))
							{
								Map1_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 1)].MapName);
								Map1_Button.enable();
							} else
							{
								Map1_Button.changeLabelText("");
								Map1_Button.disable();
							}
							if (((TrackSelect.CurrentName + 2) <= (TrackSelect.MapsAmount - 1)))
							{
								Map2_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 2)].MapName);
								Map2_Button.enable();
							} else
							{
								Map2_Button.changeLabelText("");
								Map2_Button.disable();
							}
							if (((TrackSelect.CurrentName + 3) <= (TrackSelect.MapsAmount - 1)))
							{
								Map3_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 3)].MapName);
								Map3_Button.enable();
							} else
							{
								Map3_Button.changeLabelText("");
								Map3_Button.disable();
							}
							if (((TrackSelect.CurrentName + 4) <= (TrackSelect.MapsAmount - 1)))
							{
								Map4_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 4)].MapName);
								Map4_Button.enable();
							} else
							{
								Map4_Button.changeLabelText("");
								Map4_Button.disable();
							}
							if (((TrackSelect.CurrentName + 5) <= (TrackSelect.MapsAmount - 1)))
							{
								Map5_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 5)].MapName);
								Map5_Button.enable();
							} else
							{
								Map5_Button.changeLabelText("");
								Map5_Button.disable();
							}
							if (((TrackSelect.CurrentName + 6) <= (TrackSelect.MapsAmount - 1)))
							{
								Map6_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 6)].MapName);
								Map6_Button.enable();
							} else
							{
								Map6_Button.changeLabelText("");
								Map6_Button.disable();
							}
							if (((TrackSelect.CurrentName + 7) <= (TrackSelect.MapsAmount - 1)))
							{
								Map7_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 7)].MapName);
								Map7_Button.enable();
							} else
							{
								Map7_Button.changeLabelText("");
								Map7_Button.disable();
							}
							if (((TrackSelect.CurrentName + 8) <= (TrackSelect.MapsAmount - 1)))
							{
								Map8_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 8)].MapName);
								Map8_Button.enable();
							} else
							{
								Map8_Button.changeLabelText("");
								Map8_Button.disable();
							}
							if (((TrackSelect.CurrentName + 9) <= (TrackSelect.MapsAmount - 1)))
							{
								Map9_Button.changeLabelText(GameLogic.MapBox.elementData[(TrackSelect.CurrentName + 9)].MapName);
								Map9_Button.enable();
							} else
							{
								Map9_Button.changeLabelText("");
								Map9_Button.disable();
							}
						} else
						{
							if ((arg0 == TrackSelect.CMD_MAP0))
							{
								TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 0);
								Refresh();
							} else
							{
								if ((arg0 == TrackSelect.CMD_MAP1))
								{
									TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 1);
									Refresh();
								} else
								{
									if ((arg0 == TrackSelect.CMD_MAP2))
									{
										TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 2);
										Refresh();
									} else
									{
										if ((arg0 == TrackSelect.CMD_MAP3))
										{
											TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 3);
											Refresh();
										} else
										{
											if ((arg0 == TrackSelect.CMD_MAP4))
											{
												TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 4);
												Refresh();
											} else
											{
												if ((arg0 == TrackSelect.CMD_MAP5))
												{
													TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 5);
													Refresh();
												} else
												{
													if ((arg0 == TrackSelect.CMD_MAP6))
													{
														TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 6);
														Refresh();
													} else
													{
														if ((arg0 == TrackSelect.CMD_MAP7))
														{
															TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 7);
															Refresh();
														} else
														{
															if ((arg0 == TrackSelect.CMD_MAP8))
															{
																TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 8);
																Refresh();
															} else
															{
																if ((arg0 == TrackSelect.CMD_MAP9))
																{
																	TrackSelect.CurrentMapNumber = (TrackSelect.CurrentName + 9);
																	Refresh();
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
					}
				}
			}
		}
	}

	public void show()
	{
		super.show();
		osd.show();
	}

	public void hide()
	{
		osd.hide();
		super.hide();
	}

}
