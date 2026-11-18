package java.util;

import java.io.*;
import java.sound.*;

public class Config extends GameType
{
	static final int SHIGH = 3;
	static final int HIGH = 2;
	static final int MID = 1;
	static final int LOW = 0;
	static final int OFF = (-(1));
	static String version = "v2.2.1";
	static int acceptBuild = 601;
	static int video_windowed = 0;
	static int video_x = 800;
	static int video_y = 600;
	static int video_depth = 32;
	static float video_gamma = 1.0;
	static int SysCursor = 1;
	static int Sound_3D_HW = 2;
	static int Sound_Mix_HW = 2;
	static int loadConfigEnabled = 1;
	static int majomParade = 0;
	static int init = sl:0x40r;
	static int texture_size = 1;
	static float texture_save_quality = 1.0;
	static int texture_format = 3;
	static int shadow_size = 256;
	static float shadow_detail = 0.25;
	static int shadows = 1;
	static int mirror = 0;
	static int flares = 1;
	static int headlight_rays = 1;
	static int BgScene = 1;
	static float object_detail = 0.030000001;
	static float object_detail_amp = 13.0;
	static float particle_density = 0.5;
	static int skidmark_max = 2000;
	static float trafficDensity = 0.2;
	static float pedestrianDensity = 0.1;
	static float cameras_viewrange = 400.0;
	static float camera_int_clipplane = 0.1;
	static float camera_int_viewrange = 400.0;
	static float camera_ext_clipplane = 0.2;
	static float camera_ext_viewrange = 400.0;
	static float camera_mirror_viewrange = 50;
	static float mirror_object_detail = 0.030000001;
	static float mirror_object_detail_amp = 13.0;
	static float camera_AOV = 15.0;
	static float camera_torque = 0.02;
	static float camera_force = 2.0;
	static float camera_damping = 1.0;
	static float camera_perpdamping = 0.7;
	static float camera_max_force = 20.0;
	static float camera_look_ahead = 0.0;
	static float camera_go_behind = 0.0;
	static float camera_go_above = 0.0;
	static float camera_distance = 5.0;
	static float camera_FOV = (1.25 * 0.5);
	static float head_move_steer = 1.0;
	static float head_move_vel = 1.0;
	static float head_move_acc = 1.0;
	static int camera_render = 0;
	static int camera_nocollision = 0;
	static int metricSystem = 1;
	static int gpsMode;
	static int player_transmission = 1;
	static float player_steeringhelp = 0.666;
	static float player_abs = 0.0;
	static float player_asr = 0.0;
	static float player_esp = 0.0;
	static float deformation = 1.6;
	static float external_damage = 0.25;
	static float internal_damage = 1.0;
	static float player_damage_multiplier = 0.8;
	static float player_change_distance = 10.0;
	static float player_change_velocity = 2.0;
	static int player_display = 0;
	static int MouseHelp = 0;
	static float wheel_gndfeedback_factor = 7.0;
	static float wheel_brake_factor = 6.0;
	static float engine_inertia_factor = 6.0;
	static float target_angle_weight = 0.5;
	static float controller_deadzone = 0.0;
	static float mouseSensitivity = 0.8;
	static int ForceFeedBack = 1;
	static float FFB_strength = 1.0;
	static float FFB_strength_emulated = 0.0333;
	static float gravity = 9.81;
	static float ground_friction = 1.0;
	static int ground_precache = 1;
	static int resource_refreshrate = 2;
	static int resource_loadrate = 10;
	static int mem_vertex_max = ((80 * 1024) * 1024);
	static int mem_vertex_min = ((16 * 1024) * 1024);
	static int mem_texture_max = ((80 * 1024) * 1024);
	static int mem_texture_min = ((40 * 1024) * 1024);
	static int mem_instance_max = 5500;
	static int mem_instance_min = 4000;
	static int mem_sound_max = ((66 * 1024) * 1024);
	static int mem_sound_min = ((12 * 1024) * 1024);
	static int mp_arrows_on = 1;
	static float deadZoneAccelerate = 0.0;
	static float deadZoneBrake = 0.0;
	static float deadZoneSteering = 0.0;
	static float deadZoneClutch = 0.0;
	static float throttleSpeed = 0.02;
	static float brakeSpeed = 0.02;
	static float steerSpeed = 0.01;
	static float steerhelp_turn = 1.0;
	static float throttlePower = 2.5;
	static float brakePower = 2.5;
	static float steeringPower = 1.5;
	static float clutchPower = 2.5;
	static final int SAVEFILEID = -19088744;
	static final int SAVEFILEVERSION = 12;
	public int FILD = 1;

	public Config(int arg0)
	{
		super();
		if ((!(loadConfig("save/game/options"))))
		{
			Sound.setVolume(Sound.CHANNEL_EFFECTS, 1.0);
			Sound.setVolume(Sound.CHANNEL_MUSIC, 0.5);
			Sound.setVolume(Sound.CHANNEL_ENGINE, 1.0);
		}
	}

	public void finalize()
	{
		saveConfig("save/game/options");
		super.finalize();
	}

	public int saveConfig(String arg0)
	{
		File.delete(arg0);
		File local2 = new File(arg0);
		if ((local2.open(File.MODE_WRITE) && Config.loadConfigEnabled))
		{
			local2.write(Config.SAVEFILEID);
			local2.write(Config.SAVEFILEVERSION);
			local2.write(Config.video_windowed);
			local2.write(Config.video_x);
			local2.write(Config.video_y);
			local2.write(Config.video_depth);
			local2.write(Config.video_gamma);
			local2.write(Config.texture_size);
			local2.write(Config.shadow_size);
			local2.write(Config.shadow_detail);
			local2.write(Config.shadows);
			local2.write(Config.cameras_viewrange);
			local2.write(Config.camera_mirror_viewrange);
			local2.write(Config.mirror);
			local2.write(Config.flares);
			local2.write(Config.headlight_rays);
			local2.write(Config.BgScene);
			local2.write(Config.particle_density);
			local2.write(Config.skidmark_max);
			local2.write(Config.resource_refreshrate);
			local2.write(Config.resource_loadrate);
			local2.write(Config.object_detail);
			local2.write(Config.object_detail_amp);
			local2.write(Config.player_transmission);
			local2.write(Config.player_steeringhelp);
			local2.write(Config.player_asr);
			local2.write(Config.player_abs);
			local2.write(Config.player_damage_multiplier);
			local2.write(Config.deformation);
			local2.write(Config.internal_damage);
			local2.write(Config.trafficDensity);
			local2.write(Config.pedestrianDensity);
			local2.write(Config.mouseSensitivity);
			local2.write(Sound.getVolume(Sound.CHANNEL_EFFECTS));
			local2.write(Sound.getVolume(Sound.CHANNEL_MUSIC));
			local2.write(Sound.getVolume(Sound.CHANNEL_ENGINE));
			local2.write(Config.metricSystem);
			local2.write(Config.gpsMode);
			local2.write(Config.FFB_strength);
			local2.write(Config.FFB_strength_emulated);
			local2.write(Config.Sound_3D_HW);
			local2.write(Config.Sound_Mix_HW);
			local2.write(Config.head_move_steer);
			local2.write(Config.head_move_vel);
			local2.write(Config.head_move_acc);
			local2.close();
			return 1;
		}
		return 0;
	}

	public int loadConfig(String arg0)
	{
		int local2;
		File local3 = new File(arg0);
		if (Config.loadConfigEnabled)
		{
			if (local3.open(File.MODE_READ))
			{
				if ((local3.readInt() == Config.SAVEFILEID))
				{
					if ((local3.readInt() == Config.SAVEFILEVERSION))
					{
						Config.video_windowed = local3.readInt();
						Config.video_x = local3.readInt();
						Config.video_y = local3.readInt();
						Config.video_depth = local3.readInt();
						Config.video_gamma = local3.readFloat();
						Config.texture_size = local3.readInt();
						Config.shadow_size = local3.readInt();
						Config.shadow_detail = local3.readFloat();
						Config.shadows = local3.readInt();
						Config.cameras_viewrange = local3.readFloat();
						Config.camera_ext_viewrange = Config.cameras_viewrange;
						Config.camera_int_viewrange = Config.cameras_viewrange;
						Config.camera_mirror_viewrange = local3.readFloat();
						Config.mirror = local3.readInt();
						Config.flares = local3.readInt();
						Config.headlight_rays = local3.readInt();
						Config.BgScene = local3.readFloat();
						Config.particle_density = local3.readFloat();
						Config.skidmark_max = local3.readInt();
						Config.resource_refreshrate = local3.readInt();
						Config.resource_loadrate = local3.readInt();
						Config.object_detail = local3.readFloat();
						Config.object_detail_amp = local3.readFloat();
						Config.player_transmission = local3.readInt();
						Config.player_steeringhelp = local3.readFloat();
						Config.player_asr = local3.readFloat();
						Config.player_abs = local3.readFloat();
						Config.player_damage_multiplier = local3.readFloat();
						Config.deformation = local3.readFloat();
						Config.internal_damage = local3.readFloat();
						Config.trafficDensity = local3.readFloat();
						Config.pedestrianDensity = local3.readFloat();
						Config.mouseSensitivity = local3.readFloat();
						Sound.setVolume(Sound.CHANNEL_EFFECTS, local3.readFloat());
						Sound.setVolume(Sound.CHANNEL_MUSIC, local3.readFloat());
						Sound.setVolume(Sound.CHANNEL_ENGINE, local3.readFloat());
						Config.metricSystem = local3.readInt();
						Config.gpsMode = local3.readInt();
						Config.FFB_strength = local3.readFloat();
						Config.FFB_strength_emulated = local3.readFloat();
						Config.Sound_3D_HW = local3.readInt();
						Config.Sound_Mix_HW = local3.readInt();
						Config.head_move_steer = local3.readFloat();
						Config.head_move_vel = local3.readFloat();
						Config.head_move_acc = local3.readFloat();
						local2 = 1;
					}
				}
				local3.close();
			}
		}
		if ((Config.metricSystem == 0))
		{
			System.setMeasure(1600);
		} else
		{
			System.setMeasure(1000);
		}
		return local2;
	}

}

public class VideoMode
{
	float width;
	float height;
	int depth;
	int windowed;

	public VideoMode(float arg0, float arg1, int arg2, int arg3)
	{
		super();
		width = arg0;
		height = arg1;
		depth = arg2;
		windowed = arg3;
	}

}
