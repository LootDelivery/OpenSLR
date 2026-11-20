package java.game;

import java.render.*;
import java.util.resource.*;

public class Scene extends GameType
{
	GroundRef map;
	RenderRef skydome;
	RenderRef sun;
	RenderRef suntype;
	float lastSelectionTime;
	float lastSelectionSeed;
	int lastConfig = (-(1));
	int internalScene;
	GameRef renderer;
	ResourceRef SkydomeTexture;
	int MotionB = 0;
	RenderRef Temp_Skydome;
	MotionBlur MB_Animator;

	public void CreateMBskydome()
	{
		if (skydome)
		{
			skydome.destroy();
		}
		Temp_Skydome = new RenderRef();
		Temp_Skydome.duplicate(new ResourceRef(MW_Mod:0x79r));
		Temp_Skydome.changeResource(new ResourceRef(MW_Mod:0x35r), SkydomeTexture);
		skydome = new RenderRef(map, Temp_Skydome, "MB Skydome Render");
		if ((!(MB_Animator)))
		{
			MB_Animator = new MotionBlur(renderer, skydome);
		} else
		{
			MB_Animator.changeSky(skydome);
		}
	}

	public void addSceneElements(float arg0)
	{
		addSceneElements(time2Config(arg0));
	}

	public void addSceneElements(int arg0)
	{
		if ((arg0 != lastConfig))
		{
			if ((!(internalScene)))
			{
				suntype = new RenderRef();
				suntype.duplicate(new RenderRef(maps.skydome:0x124r));
			}
			if ((arg0 == 0))
			{
				if ((!(internalScene)))
				{
					map.setFog(463390, 20.0, 150.0);
					suntype.setLight(4612741, 463390, 4612741);
					GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x35r));
				}
				if ((!(MotionB)))
				{
					skydome = new RenderRef(map, maps.skydome:0x23r, "egbolt-night_02");
				} else
				{
					SkydomeTexture = new ResourceRef(maps.skydome:0x22r);
					CreateMBskydome();
				}
			} else
			{
				if ((arg0 == 1))
				{
					if ((!(internalScene)))
					{
						map.setFog(1512726, 50.0, 200.0);
						suntype.setLight(6850982, 2040109, 6850982);
						GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x30r));
					}
					if ((!(MotionB)))
					{
						skydome = new RenderRef(map, maps.skydome:0x1dr, "egbolt-dusk_03");
					} else
					{
						SkydomeTexture = new ResourceRef(maps.skydome:0x1ar);
						CreateMBskydome();
					}
				}
			}
			if ((arg0 == 2))
			{
				if ((!(internalScene)))
				{
					map.setFog(1118483, 10.0, 160.0);
					suntype.setLight(6118775, 1512495, 6118775);
					GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x31r));
				}
				if ((!(MotionB)))
				{
					skydome = new RenderRef(map, maps.skydome:0x1er, "egbolt-dusk_04");
				} else
				{
					SkydomeTexture = new ResourceRef(maps.skydome:0x1br);
					CreateMBskydome();
				}
			} else
			{
				if ((arg0 == 3))
				{
					if ((!(internalScene)))
					{
						map.setFog(3225163, 50.0, 200.0);
						suntype.setLight(13351338, 2764871, 13351338);
						GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x2er));
					}
					if ((!(MotionB)))
					{
						skydome = new RenderRef(map, maps.skydome:0x15r, "egbolt-dusk_01");
					} else
					{
						SkydomeTexture = new ResourceRef(maps.skydome:0xer);
						CreateMBskydome();
					}
				} else
				{
					if ((arg0 == 4))
					{
						if ((!(internalScene)))
						{
							map.setFog(6905955, 30.0, 200.0);
							suntype.setLight(12696725, 5332829, 12696725);
							GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x2fr));
						}
						if ((!(MotionB)))
						{
							skydome = new RenderRef(map, maps.skydome:0x16r, "egbolt-dusk_02");
						} else
						{
							SkydomeTexture = new ResourceRef(maps.skydome:0xdr);
							CreateMBskydome();
						}
					} else
					{
						if ((arg0 == 5))
						{
							if ((!(internalScene)))
							{
								map.setFog(526344, 50.0, 200.0);
								suntype.setLight(5459890, 1314349, 5459890);
								GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x32r));
							}
							if ((!(MotionB)))
							{
								skydome = new RenderRef(map, maps.skydome:0x1fr, "egbolt-dusk_05");
							} else
							{
								SkydomeTexture = new ResourceRef(maps.skydome:0x1cr);
								CreateMBskydome();
							}
						} else
						{
							if ((arg0 == 6))
							{
								if ((!(internalScene)))
								{
									map.setFog(11582652, 70.0, 400.0);
									suntype.setLight(12902371, 3030870, 12902371);
									suntype.setFlare(
										new ResourceRef(maps.skydome:0x100r),
										-454761217,
										1.0,
										10.0,
										15,
										8
									);
									GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x33r));
								}
								if ((!(MotionB)))
								{
									skydome = new RenderRef(map, maps.skydome:0x21r, "egbolt-morning_01");
								} else
								{
									SkydomeTexture = new ResourceRef(maps.skydome:0x20r);
									CreateMBskydome();
								}
							} else
							{
								if ((arg0 == 7))
								{
									if ((!(internalScene)))
									{
										map.setFog(6191506, 70.0, 400.0);
										suntype.setLight(14538925, 4939375, 14538925);
										suntype.setFlare(
											new ResourceRef(maps.skydome:0x100r),
											-454761217,
											1.0,
											10.0,
											15,
											8
										);
										GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x26r));
									}
									if ((!(MotionB)))
									{
										skydome = new RenderRef(map, maps.skydome:0x2r, "egbolt-am_01");
									} else
									{
										SkydomeTexture = new ResourceRef(maps.skydome:0x5r);
										CreateMBskydome();
									}
								} else
								{
									if ((arg0 == 8))
									{
										if ((!(internalScene)))
										{
											map.setFog(10597827, 70.0, 400.0);
											suntype.setLight(14604988, 3161176, 14604988);
											suntype.setFlare(
												new ResourceRef(maps.skydome:0x100r),
												-454761217,
												1.0,
												10.0,
												15,
												8
											);
											GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x27r));
										}
										if ((!(MotionB)))
										{
											skydome = new RenderRef(map, maps.skydome:0xfr, "egbolt-am_02");
										} else
										{
											SkydomeTexture = new ResourceRef(maps.skydome:0x6r);
											CreateMBskydome();
										}
									}
								}
							}
						}
					}
				}
			}
			if ((arg0 == 9))
			{
				if ((!(internalScene)))
				{
					map.setFog(8821677, 70.0, 400.0);
					suntype.setLight(14210224, 3625072, 14210224);
					suntype.setFlare(new ResourceRef(maps.skydome:0x100r), -454761217, 1.0, 10.0, 15, 8);
					GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x28r));
				}
				if ((!(MotionB)))
				{
					skydome = new RenderRef(map, maps.skydome:0x10r, "egbolt-am_03");
				} else
				{
					SkydomeTexture = new ResourceRef(maps.skydome:0x7r);
					CreateMBskydome();
				}
			}
			if ((arg0 == 10))
			{
				if ((!(internalScene)))
				{
					map.setFog(10789280, 70.0, 400.0);
					suntype.setLight(10594227, 5791851, 10594227);
					suntype.setFlare(new ResourceRef(maps.skydome:0x100r), -724249345, 1.0, 3.0, 15, 8);
					GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x29r));
				}
				if ((!(MotionB)))
				{
					skydome = new RenderRef(map, maps.skydome:0x19r, "egbolt-cloudyday_01");
				} else
				{
					SkydomeTexture = new ResourceRef(maps.skydome:0x18r);
					CreateMBskydome();
				}
			} else
			{
				if ((arg0 == 11))
				{
					if ((!(internalScene)))
					{
						map.setFog(3885144, 60.0, 300.0);
						suntype.setLight(12368819, 1711663, 12368819);
						GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x2br));
					}
					if ((!(MotionB)))
					{
						skydome = new RenderRef(map, maps.skydome:0x13r, "egbolt-dawn_02");
					} else
					{
						SkydomeTexture = new ResourceRef(maps.skydome:0x9r);
						CreateMBskydome();
					}
				} else
				{
					if ((arg0 == 12))
					{
						if ((!(internalScene)))
						{
							map.setFog(1443586, 60.0, 300.0);
							suntype.setLight(13673069, 985103, 13673069);
							GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x2ar));
						}
						if ((!(MotionB)))
						{
							skydome = new RenderRef(map, maps.skydome:0x12r, "egbolt-dawn_01");
						} else
						{
							SkydomeTexture = new ResourceRef(maps.skydome:0x8r);
							CreateMBskydome();
						}
					} else
					{
						if ((arg0 == 13))
						{
							if ((!(internalScene)))
							{
								map.setFog(0, 60.0, 300.0);
								suntype.setLight(9669244, 1514794, 9669244);
								GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x2cr));
							}
							if ((!(MotionB)))
							{
								skydome = new RenderRef(map, maps.skydome:0x11r, "egbolt-dawn_03");
							} else
							{
								SkydomeTexture = new ResourceRef(maps.skydome:0xar);
								CreateMBskydome();
							}
						} else
						{
							if ((arg0 == 14))
							{
								if ((!(internalScene)))
								{
									map.setFog(197381, 60.0, 300.0);
									suntype.setLight(6242373, 855059, 6242373);
									GfxEngine.setGlobalEnvmap(new ResourceRef(maps.skydome:0x2dr));
								}
								if ((!(MotionB)))
								{
									skydome = new RenderRef(map, maps.skydome:0x14r, "egbolt-dawn_04");
								} else
								{
									SkydomeTexture = new ResourceRef(maps.skydome:0xbr);
									CreateMBskydome();
								}
							}
						}
					}
				}
			}
			if ((!(internalScene)))
			{
				sun = new RenderRef(map, suntype, "sunny");
			}
			lastConfig = arg0;
		}
	}

	public int time2Config(float arg0)
	{
		float local2 = ((arg0 - lastSelectionTime) / 3600);
		if (((local2 >= 1.0) || (local2 <= (-(1.0)))))
		{
			lastSelectionTime = arg0;
			lastSelectionSeed = Math.random();
		}
		float local3 = (arg0 / 3600);
		float local4 = lastSelectionSeed;
		int local5;
		if (((local3 < 3) || (local3 >= 22)))
		{
			local5 = 0;
		} else
		{
			if ((local3 < 4))
			{
				if ((local4 < 0.5))
				{
					local5 = 1;
				} else
				{
					local5 = 2;
				}
			} else
			{
				if ((local3 < 8))
				{
					if ((local4 < 0.33))
					{
						local5 = 3;
					} else
					{
						if ((local4 < 0.66))
						{
							local5 = 4;
						} else
						{
							local5 = 5;
						}
					}
				} else
				{
					if ((local3 < 10))
					{
						local5 = 6;
					} else
					{
						if ((local3 < 19))
						{
							if ((local4 < 0.25))
							{
								local5 = 7;
							} else
							{
								if ((local4 < 0.5))
								{
									local5 = 8;
								} else
								{
									if ((local4 < 0.75))
									{
										local5 = 9;
									} else
									{
										local5 = 10;
									}
								}
							}
						} else
						{
							if ((local3 < 20.5))
							{
								local5 = 11;
							} else
							{
								if ((local3 < 22))
								{
									if ((local4 < 0.33))
									{
										local5 = 12;
									} else
									{
										if ((local4 < 0.66))
										{
											local5 = 13;
										} else
										{
											local5 = 14;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return local5;
	}

	public void remSceneElements()
	{
		if (skydome)
		{
			skydome.destroy();
		}
		if ((!(internalScene)))
		{
			sun.destroy();
			suntype.destroy();
		}
		lastConfig = (-(1));
	}

	public void refresh(float arg0)
	{
		int local2 = time2Config(arg0);
		if ((local2 != lastConfig))
		{
			remSceneElements();
			addSceneElements(local2);
		}
	}

}
