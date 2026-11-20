package java.game;

import java.game.parts.*;
import java.game.parts.bodypart.*;
import java.io.*;
import java.render.*;
import java.render.osd.*;
import java.render.osd.dialog.*;
import java.util.*;
import java.util.resource.*;

public class Mechanic extends GameType
{
	static final int RID_REPAIR_PART = frontend:0x12dr;
	static final int RID_REPAIR_CAR = frontend:0x12er;
	static final int RID_SELL_PART = frontend:0x12fr;
	static final int RID_FILTER_ENGINE = frontend:0x11ar;
	static final int RID_FILTER_BODY = frontend:0x11br;
	static final int RID_FILTER_RGEAR = frontend:0x11cr;
	static final int RID_BACKGROUND = frontend:0xc0r;
	static final float BACKGROUND_HEIGHT = 0.4;
	static final float PARTS_VP_TOP = 0.815;
	static final float PARTS_VP_LEFT = 0.226;
	static final float PARTS_VP_WIDTH = 0.696;
	static final float PARTS_VP_HEIGHT = 0.178;
	static final int CMD_SCROLL_UP = 0;
	static final int CMD_SCROLL_DOWN = 1;
	static final int CMD_REPAIR = 2;
	static final int CMD_REPAIR_ALL = 3;
	static final int CMD_SELL = 4;
	static final int CMD_ENGINE = 119;
	static final int CMD_BODY = 120;
	static final int CMD_RUNNING_GEAR = 121;
	static final int RID_BTN_L = frontend:0x98r;
	static final int RID_BTN_M = frontend:0x99r;
	static final int RID_BTN_R = frontend:0x9ar;
	static final SfxRef SfxWRENCH = new SfxRef(GameLogic.SFX_WRENCH);
	static final int MF_FREE_REPAIRS = 1;
	static int flags;
	Player player;
	Osd osd;
	int osdGroup;
	int handleControls;
	ControlSetState css;
	VisualInventory inventory;
	Text invLineTxt;
	Text infoline;
	Text moneyTxt;
	InventoryPanel actualPanel;
	int actualPanelChanged;
	int overVehicle;
	GameRef lastLookDestination;
	GameRef camera;
	GameRef map;
	int mode;
	int filterEngine;
	int filterBody;
	int filterRGear;
	Thread spinnerThread;

	public Mechanic(Player arg0, Osd arg1, Text arg2, Text arg3, int arg4)
	{
		super();
		createNativeInstance();
		player = arg0;
		osd = arg1;
		moneyTxt = arg2;
		infoline = arg3;
		handleControls = arg4;
		inventory = new VisualInventory(
			player,
			Mechanic.PARTS_VP_LEFT,
			Mechanic.PARTS_VP_TOP,
			Mechanic.PARTS_VP_WIDTH,
			Mechanic.PARTS_VP_HEIGHT
		);
		osd.globalHandler = this;
		Style local6 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWUP)
		);
		osd.createButton(local6, 0.94000006, 0.68, Mechanic.CMD_SCROLL_UP, "Scroll up");
		Style local7 = new Style(
			0.1,
			0.1,
			1.0,
			Frontend.mediumFont,
			Text.ALIGN_CENTER,
			new ResourceRef(Osd.RID_ARROWDN)
		);
		osd.createButton(local7, 0.94000006, 0.92, Mechanic.CMD_SCROLL_DOWN, "Scroll down");
		invLineTxt = osd.createText("1", Frontend.largeFont, Text.ALIGN_CENTER, 0.95000005, 0.76);
		osd.createRectangle(
			0.0,
			(1.0 - (Mechanic.BACKGROUND_HEIGHT / 2)),
			2.0,
			Mechanic.BACKGROUND_HEIGHT,
			(-(1)),
			new ResourceRef(Mechanic.RID_BACKGROUND)
		);
		Style local8 = new Style(0.120000005, 0.120000005, Frontend.mediumFont, Text.ALIGN_LEFT, null);
		Menu local9 = osd.createMenu(local8, (-(0.98)), 0.71999997, 0.120000005, Osd.MD_HORIZONTAL);
		local9.addItem(
			new ResourceRef(Mechanic.RID_REPAIR_PART),
			Mechanic.CMD_REPAIR,
			"Repair Part").enableDrop(
		);
		local9.addItem(new ResourceRef(Mechanic.RID_REPAIR_CAR), Mechanic.CMD_REPAIR_ALL, "Repair all the car");
		local9.addItem(new ResourceRef(Mechanic.RID_SELL_PART), Mechanic.CMD_SELL, "Sell Part").enableDrop();
		local9 = osd.createMenu(local8, (-(0.98)), 0.88, 0.120000005, Osd.MD_HORIZONTAL);
		local9.addItem(
			new ResourceRef(Mechanic.RID_FILTER_ENGINE),
			Mechanic.CMD_ENGINE,
			"Hide/Unhide ENGINE Parts"
		);
		local9.addItem(new ResourceRef(Mechanic.RID_FILTER_BODY), Mechanic.CMD_BODY, "Hide/Unhide BODY Parts");
		local9.addItem(
			new ResourceRef(Mechanic.RID_FILTER_RGEAR),
			Mechanic.CMD_RUNNING_GEAR,
			"Hide/Unhide RUNNING GEAR Parts"
		);
		osd.hideGroup((osdGroup = osd.endGroup()));
	}

	public void show()
	{
		osd.showGroup(osdGroup);
		inventory.show();
		spinnerThread = new Thread(this, "Mechanic inventory part spinner");
		spinnerThread.start();
		Input.cursor.addHandler(this);
		setEventMask(GameRef.EVENT_CURSOR);
		if (handleControls)
		{
			Input.cursor.setPointer(Frontend.pointers, "G");
			Input.cursor.enable(1);
			Input.getAxis(1, (-(1)));
			css = player.controller.reset();
			player.controller.activateState(ControlSet.CAMTURNSET);
		}
	}

	public void hide()
	{
		clearEventMask(GameRef.EVENT_ANY);
		Input.cursor.remHandler(this);
		if (handleControls)
		{
			Input.cursor.setPointer(Frontend.pointers, "J");
			Input.cursor.enable(0);
			player.controller.reset(css);
		}
		if (spinnerThread)
		{
			spinnerThread.stop();
			spinnerThread = null;
		}
		infoline.changeText(null);
		inventory.hide();
		osd.hideGroup(osdGroup);
	}

	public void run()
	{
		while (true)
		{
			int local1;
			InventoryPanel local2 = actualPanel;
			if ((actualPanelChanged || inventory.visualsUpdated))
			{
				actualPanelChanged = 0;
				inventory.visualsUpdated = 0;
				if (local2)
				{
					if (local2.invItem)
					{
						infoline.changeText(local2.invItem.getInfo());
					} else
					{
						infoline.changeText("empty slot");
					}
				} else
				{
					infoline.changeText(null);
				}
			}
			if (local2)
			{
				local2.focusHook();
			}
			spinnerThread.sleep(20);
		}
	}

	public void handleEvent(GameRef arg0, int arg1, String arg2)
	{
		int local4 = (-(1));
		if ((arg1 == GameRef.EVENT_CURSOR))
		{
			int local5 = arg2.token(++local4).intValue();
			int local6 = arg2.token(++local4).intValue();
			if ((local5 == GameType.EC_LCLICK))
			{
				int local7 = arg2.token(++local4).intValue();
				Object local8 = arg0.getScriptInstance();
				Vector3 local9 = new Vector3(
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue()
				);
				Vector3 local10 = new Vector3(
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue(),
					arg2.token(++local4).floatValue()
				);
				if ((local8 instanceof Part))
				{
					if ((mode == 0))
					{
						if ((!((local8 instanceof Chassis))))
						{
							int local11 = local8.getInfo(GameType.GII_GETOUT_OK);
							if ((local11 != (-(1))))
							{
								if (lastLookDestination)
								{
									if ((lastLookDestination.id() == local8.id()))
									{
										camera.command((((("look " + map.id()) + " ") + local8.getPos().toString()) + " 0,0,0"));
										lastLookDestination = null;
									}
								}
								SfxWRENCH.play(local10, 0.0, 1.0, 1.0, 0);
								inventory.scrollTo(inventory.addItem(local8));
								invLineTxt.changeText(Integer.toString((inventory.currentLine() + 1)));
								player.car.wakeUp();
								GameLogic.spendTime(GameLogic.mechTime(local8, 0));
							}
						}
					} else
					{
						if (local8.isTuneable())
						{
							Dialog local11_2 = new Dialog(
								player.controller,
								(((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG) | Dialog.DF_WIDE),
								"TUNE PART",
								"OK;Cancel"
							);
							Style local12 = new Style(
								0.45,
								0.08,
								Frontend.smallFont,
								Text.ALIGN_CENTER,
								Osd.RRT_TEST
							);
							Style local13 = new Style(
								0.45,
								0.06,
								Frontend.smallFont,
								Text.ALIGN_CENTER,
								new ResourceRef(Osd.RID_SLD_BACK)
							);
							Style local14 = new Style(
								0.04,
								0.049999997,
								Frontend.smallFont,
								Text.ALIGN_RIGHT,
								new ResourceRef(Osd.RID_SLD_KNOB)
							);
							local11_2.osd.globalHandler = local8;
							Menu local15 = local11_2.osd.createMenu(
								local12,
								0.0,
								(-(0.5)),
								0,
								local13,
								local14
							);
							local8.buildTuningMenu(local15);
							local8.enterAsyncMode_Script();
							local11_2.osd.globalHandler = local11_2;
							int local16 = local11_2.display();
							local8.leaveAsyncMode_Script();
							local8.endTuningSession(local16);
						} else
						{
							local8.command("flap_toggle");
							player.car.wakeUp();
						}
					}
				}
			} else
			{
				if ((local5 == GameType.EC_HOVER))
				{
					GameRef local7_2 = new GameRef(arg2.token(++local4).intValue());
					int local8_2 = arg2.token(++local4).intValue();
					InventoryPanel local9_2;
					if ((local9_2 = inventory.getPanelbyButtonPhyId(local8_2)))
					{
						actualPanel = local9_2;
						actualPanelChanged = 1;
					} else
					{
						if (actualPanel)
						{
							actualPanel = null;
							actualPanelChanged = 1;
						}
					}
					Object local10_2 = local7_2.getScriptInstance();
					if ((local10_2 instanceof Part))
					{
						Part local11_3 = local10_2;
						int local12_2 = (local11_3.getConditionNoAttach() * 100);
						String local13_2;
						if (local11_3.isComplex())
						{
							local13_2 = (local13_2 + "+");
						}
						if (local11_3.isTuneable())
						{
							local13_2 = (local13_2 + "T");
						}
						if ((!(local11_3.isStreetLegal())))
						{
							local13_2 = (local13_2 + "!");
						}
						if (local13_2)
						{
							local13_2 = ((" [" + local13_2) + "]");
						}
						infoline.changeText(((((local11_3.name + local13_2) + " (") + local12_2) + "%)"));
						overVehicle = 1;
					}
				} else
				{
					if ((local5 == GameType.EC_LEAVE))
					{
						GameRef local7_3;
						GameRef local8_3 = new GameRef(arg2.token(++local4).intValue());
						Object local9_3 = local8_3.getScriptInstance();
						if (((local9_3 instanceof Part) && overVehicle))
						{
							infoline.changeText(null);
							overVehicle = 0;
						}
					} else
					{
						if ((local5 == GameType.EC_RDRAGBEGIN))
						{
							player.controller.user_Del(Input.AXIS_CURSOR_X, ControlSet.MOUSE, 0);
							player.controller.user_Del(Input.AXIS_CURSOR_Y, ControlSet.MOUSE, 1);
							Input.cursor.cursor.queueEvent(null, GameType.EVENT_COMMAND, "lock");
						} else
						{
							if ((local5 == GameType.EC_RDRAGEND))
							{
								player.controller.user_Add(
									Input.AXIS_CURSOR_X,
									ControlSet.MOUSE,
									0,
									(-(1.0)),
									1.0,
									(-(1.0)),
									1.0
								);
								player.controller.user_Add(
									Input.AXIS_CURSOR_Y,
									ControlSet.MOUSE,
									1,
									(-(1.0)),
									1.0,
									(-(1.0)),
									1.0
								);
								Input.cursor.cursor.queueEvent(null, GameType.EVENT_COMMAND, "unlock");
							}
						}
					}
				}
			}
		}
	}

	public void refreshMoneyString()
	{
		if (moneyTxt)
		{
			new SfxRef(Frontend.SFX_MONEY).play();
			moneyTxt.changeText(("$" + Integer.toString(player.money)));
		}
	}

	public void osdCommand(int arg0)
	{
		if ((arg0 == Mechanic.CMD_ENGINE))
		{
			filterEngine = (2 - filterEngine);
			if (player.car)
			{
				player.car.command(("filter 1 " + filterEngine));
				player.car.wakeUp();
				player.car.command("suspend");
			}
			filterInventory(filterEngine, filterBody, filterRGear);
		} else
		{
			if ((arg0 == Mechanic.CMD_BODY))
			{
				filterBody = (2 - filterBody);
				if (player.car)
				{
					player.car.command(("filter 2 " + filterBody));
					player.car.wakeUp();
					player.car.command("suspend");
				}
				filterInventory(filterEngine, filterBody, filterRGear);
			} else
			{
				if ((arg0 == Mechanic.CMD_RUNNING_GEAR))
				{
					filterRGear = (2 - filterRGear);
					if (player.car)
					{
						player.car.command(("filter 3 " + filterRGear));
						player.car.wakeUp();
						player.car.command("suspend");
					}
					filterInventory(filterEngine, filterBody, filterRGear);
				} else
				{
					if ((arg0 == Mechanic.CMD_SCROLL_UP))
					{
						inventory.upScroll();
						invLineTxt.changeText(Integer.toString((inventory.currentLine() + 1)));
					} else
					{
						if ((arg0 == Mechanic.CMD_SCROLL_DOWN))
						{
							inventory.downScroll();
							invLineTxt.changeText(Integer.toString((inventory.currentLine() + 1)));
						} else
						{
							if ((arg0 == Mechanic.CMD_REPAIR))
							{
								if ((osd.dropObject || osd.dropGadget))
								{
									Part local2;
									if (osd.dropGadget)
									{
										InventoryItem local3;
										if ((local3 = inventory.getItembyButton(osd.dropGadget)))
										{
											local2 = local3.getPart();
										}
									} else
									{
										local2 = osd.dropObject.getScriptInstance();
										if (local2)
										{
											if ((local2 instanceof Chassis))
											{
												int local3_2 = local2.getInfo(GameType.GII_GETOUT_OK);
												if ((local3_2 != 0))
												{
													String local4;
													if ((local3_2 != (-(1))))
													{
														GameRef local5 = new GameRef(local3_2);
														Part local6 = local5.getScriptInstance();
														local4 = (("A " + local6.name) + " is");
													} else
													{
														local4 = "Some parts are";
													}
													new WarningDialog(
														player.controller,
														((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
														"REPAIR CHASSIS",
														(local4 + " attached to the chassis. \n All parts have to be disassembled from the chassis before it is going to be repaired.")).display(
													);
													local2 = null;
												}
											} else
											{
												local2 = null;
											}
										}
									}
									if (local2)
									{
										if (local2.id())
										{
											if ((local2.getCondition() < 1.0))
											{
												if (local2.isRepairable())
												{
													float local3_3 = local2.repairCost();
													if ((local3_3 > 0))
													{
														int local4_2 = local3_3;
														if ((!(local4_2)))
														{
															local4_2 = 1;
														}
														if ((Mechanic.flags & Mechanic.MF_FREE_REPAIRS))
														{
															local4_2 = 0;
														}
														if ((local4_2 <= player.money))
														{
															Dialog local5_2;
															if (local4_2)
															{
																local5_2 = new RepairDialog(
																	player.controller,
																	((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
																	local4_2,
																	local2
																);
															} else
															{
																local5_2 = new WarningDialog(
																	player.controller,
																	((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
																	"INFO",
																	(local2.name + " has been repaired for free.")
																);
															}
															if ((local5_2.display() == 0))
															{
																player.money -= local4_2;
																local2.repair();
																refreshMoneyString();
																GameLogic.spendTime((local4_2 * 10));
															}
														} else
														{
															int local5_3 = (local4_2 - player.money);
															new WarningDialog(
																player.controller,
																((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
																"REPAIR PART",
																(("You need $" + local5_3) + " more to do this!")).display(
															);
														}
													} else
													{
														new WarningDialog(
															player.controller,
															((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
															"REPAIR PART",
															"This part is already repaired as far as possible!").display(
														);
													}
												} else
												{
													new WarningDialog(
														player.controller,
														((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
														"REPAIR PART",
														"This part is so damaged it cannot be repaired.").display(
													);
												}
											} else
											{
												new WarningDialog(
													player.controller,
													((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
													"REPAIR PART",
													"This part's in perfect condition!").display(
												);
											}
										}
									}
								} else
								{
									new WarningDialog(
										player.controller,
										((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
										"INFO",
										"Drop parts here from the Parts Bin to repair them! \n \n You can also drop the chassis here directly from the garage.").display(
									);
								}
							} else
							{
								if ((arg0 == Mechanic.CMD_SELL))
								{
									if (osd.dropGadget)
									{
										InventoryItem local2_2;
										if ((local2_2 = inventory.getItembyButton(osd.dropGadget)))
										{
											int local3_4 = local2_2.getPrice();
											Dialog local4_3 = new ScrapDialog(
												player.controller,
												((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
												local2_2,
												local3_4
											);
											if ((local4_3.display() == 0))
											{
												player.money += local3_4;
												refreshMoneyString();
												inventory.removeItem(local2_2);
											}
										}
									} else
									{
										if ((inventory.items.size() > 1))
										{
											int local2_3 = 0;
											int local3_5 = (inventory.items.size() - 1);
											while ((local3_5 >= 0))
											{
												InventoryItem local4_4 = inventory.items.elementData[1];
												local2_3 += local4_4.getPrice();
												local3_5--;
											}
											String local3_6;
											int local4_5 = 0;
											if ((((filterEngine == 0) && (filterBody == 0)) && (filterRGear == 0)))
											{
												local3_6 = "";
											} else
											{
												if ((filterEngine == 0))
												{
													local4_5++;
												}
												if ((filterBody == 0))
												{
													local4_5++;
												}
												if ((filterRGear == 0))
												{
													local4_5++;
												}
												if ((local4_5 == 2))
												{
													String local5_4;
													String local6_2;
													if ((filterEngine == 0))
													{
														local5_4 = "engine";
													} else
													{
														if ((filterBody == 0))
														{
															local5_4 = "body";
														}
													}
													if ((filterRGear == 0))
													{
														local6_2 = "running gear";
													} else
													{
														if ((filterBody == 0))
														{
															local6_2 = "body";
														}
													}
													local3_6 = ((local5_4 + " and ") + local6_2);
												} else
												{
													if ((filterEngine == 0))
													{
														local3_6 = "engine";
													}
													if ((filterBody == 0))
													{
														local3_6 = "body";
													}
													if ((filterRGear == 0))
													{
														local3_6 = "running gear";
													}
												}
											}
											if ((0 == new YesNoDialog(player.controller, ((Dialog.DF_LOWPRI | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG), "SELL ALL PARTS", (((("Do you want to sell ALL " + local3_6) + " parts from Part Bin for total price $") + local2_3) + "?")).display()))
											{
												inventory.items.removeAllElements();
												inventory.update();
												player.money += local2_3;
												refreshMoneyString();
											}
										} else
										{
											new WarningDialog(
												player.controller,
												((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
												"INFO",
												"Click here to sell all parts from your Parts Bin, or drop parts here to sell them. \n \n To sell you entire car go to the Used Car Dealer.").display(
											);
										}
									}
								} else
								{
									if ((arg0 == Mechanic.CMD_REPAIR_ALL))
									{
										if ((player.car && player.car.chassis))
										{
											Part local2_4 = player.car.chassis;
											if ((local2_4.getCondition() < 1.0))
											{
												if (local2_4.isRepairable())
												{
													int local3_7 = local2_4.repairCost();
													if ((local3_7 > 0))
													{
														int local4_6 = local3_7;
														if ((!(local4_6)))
														{
															local4_6 = 1;
														}
														if ((Mechanic.flags & Mechanic.MF_FREE_REPAIRS))
														{
															local4_6 = 0;
														}
														if ((local4_6 <= player.money))
														{
															Dialog local5_5;
															if (local4_6)
															{
																local5_5 = new RepairAllDialog(
																	player.controller,
																	((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
																	local4_6
																);
															} else
															{
																local5_5 = new WarningDialog(
																	player.controller,
																	((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
																	"INFO",
																	"Your car has been repaired for free."
																);
															}
															if ((local5_5.display() == 0))
															{
																player.money -= local4_6;
																player.car.repair();
																refreshMoneyString();
																GameLogic.spendTime((local4_6 * 10));
															}
														} else
														{
															new WarningDialog(
																player.controller,
																((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
																"REPAIR PART",
																(("You need $" + (local4_6 - player.money)) + " more to do this!")).display(
															);
														}
													} else
													{
														new WarningDialog(
															player.controller,
															((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
															"REPAIR CAR",
															"This car is already repaired as far as possible!").display(
														);
													}
												} else
												{
													new WarningDialog(
														player.controller,
														((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
														"REPAIR CAR",
														"This car is so damaged it can't be repaired!").display(
													);
												}
											} else
											{
												new WarningDialog(
													player.controller,
													((Dialog.DF_FREEZE | Dialog.DF_MODAL) | Dialog.DF_DEFAULTBG),
													"REPAIR CAR",
													"This car's in perfect condition!").display(
												);
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

	public void flushInventory()
	{
		int local1 = inventory.size();
		while (local1--)
		{
			inventory.moveToInventory(0, player.parts);
		}
	}

	public void filterInventory(int arg0, int arg1, int arg2)
	{
		int local4;
		if (player.car)
		{
			local4 = (player.car.getInfo(GameType.GII_TYPE) >> 16);
		}
		InventoryItem local5;
		Part local6;
		int local7 = 0;
		while ((local7 < player.parts.size()))
		{
			local5 = player.parts.items.elementAt(local7);
			if ((local5 instanceof InventoryItem_Part))
			{
				local6 = local5.getPart();
				if (((local6.carCategory == Part.COMMON) || ((local6.getInfo(GameType.GII_TYPE) >> 16) == local4)))
				{
					int local8 = local6.getInfo(GameType.GII_PART_CATEGORY);
					if ((local8 == 1))
					{
						if ((!(arg0)))
						{
							player.parts.moveToInventory(local7--, inventory);
						}
					} else
					{
						if ((local8 == 2))
						{
							if ((!(arg1)))
							{
								player.parts.moveToInventory(local7--, inventory);
							}
						} else
						{
							if ((local8 == 3))
							{
								if ((!(arg2)))
								{
									player.parts.moveToInventory(local7--, inventory);
								}
							}
						}
					}
					if ((local8 == 0))
					{
						player.parts.moveToInventory(local7--, inventory);
					}
				}
			} else
			{
				player.parts.moveToInventory(local7--, inventory);
			}
			local7++;
		}
		int local7_2 = 0;
		while ((local7_2 < inventory.size()))
		{
			local5 = inventory.items.elementAt(local7_2);
			if ((local5 instanceof InventoryItem_Part))
			{
				local6 = local5.getPart();
				if (((local6.carCategory == Part.COMMON) || ((local6.getInfo(GameType.GII_TYPE) >> 16) == local4)))
				{
					int local8_2 = local6.getInfo(GameType.GII_PART_CATEGORY);
					if ((local8_2 == 1))
					{
						if (arg0)
						{
							inventory.moveToInventory(local7_2--, player.parts);
						}
					} else
					{
						if ((local8_2 == 2))
						{
							if (arg1)
							{
								inventory.moveToInventory(local7_2--, player.parts);
							}
						} else
						{
							if ((local8_2 == 3))
							{
								if (arg2)
								{
									inventory.moveToInventory(local7_2--, player.parts);
								}
							}
						}
					}
				} else
				{
					inventory.moveToInventory(local7_2--, player.parts);
				}
			}
			local7_2++;
		}
		inventory.update();
	}

}

public class ScrapDialog extends YesNoDialog
{
	public ScrapDialog(Controller arg0, int arg1, InventoryItem arg2, int arg3)
	{
		super(
			arg0,
			arg1,
			"SELL PART",
			(((("Do you want to sell this " + arg2.getName()) + " for $") + arg3) + " ?")
		);
	}

}

public class RepairDialog extends NoYesDialog
{
	public RepairDialog(Controller arg0, int arg1, int arg2, Part arg3)
	{
		super(
			arg0,
			arg1,
			"REPAIR PART",
			(((("It will cost $" + arg2) + " to repair this ") + arg3.name) + ". Do you want to repair it?")
		);
	}

}

public class RepairAllDialog extends NoYesDialog
{
	public RepairAllDialog(Controller arg0, int arg1, int arg2)
	{
		super(
			arg0,
			arg1,
			"REPAIR CAR",
			(("It will cost $" + arg2) + " to repair this car. Do you want to repair it?")
		);
	}

}
