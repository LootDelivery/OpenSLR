package java.game.parts;

public class DynoData extends Native
{
	static int flagsXX_inoperable = -2147483648;
	static int flagsXX_has_flywheel = 1073741824;
	static int flagsXX_has_port_nos = 536870912;
	static int flagsXX_has_wet_nos = 268435456;
	static int flagsXX_has_dry_nos = 134217728;
	static int flagsI4_has_SC_intake_tube = 1;
	static int flagsI4_has_turbo_intake_tube = 2;
	static int flagsI4_has_stock_intake_tube = 4;
	static int flagsI4_has_air_filter_for_SC_intake = 8;
	static int flagsI4_has_air_filter_for_turbo_intake = 16;
	static int flagsI4_has_air_filter_for_stock_intake = 32;
	static int flagsV6_has_SC_intake_tube = 1;
	static int flagsV6_has_L_turbo_intake_tube = 2;
	static int flagsV6_has_R_turbo_intake_tube = 4;
	static int flagsV6_has_biturbo_intake_tube = 8;
	static int flagsV6_has_stock_intake_tube = 16;
	static int flagsV6_has_air_filter_for_SC_intake = 32;
	static int flagsV6_has_air_filter_for_L_turbo_intake = 64;
	static int flagsV6_has_air_filter_for_R_turbo_intake = 128;
	static int flagsV6_has_air_filter_for_stock_intake = 256;
	float cylinders = 4;
	float bore = 0.081999995;
	float stroke = 0.089999996;
	float Vmin = 4.9999995E-05;
	float in_min = 0.005;
	float out_min = 0.005;
	float in_max = 0.8;
	float out_max = 0.8;
	float time_in_open = 0.0009999999;
	float time_in_close = 0.25;
	float time_out_open = 0.75;
	float time_out_close = 0.99;
	float time_spark_min = 0.48000002;
	float time_spark_inc = (-(0.049999997));
	float time_spark_RPM0 = 2000.0;
	float time_spark_RPM1 = 5000.0;
	float time_burn = 0.0029999998;
	float RPM_limit = 7000.0;
	float T_loss = 95.0;
	float rpm_turbo_mul = 16.0;
	float rpm_turbo_opt = 85000.0;
	float rpm_turbo_range = 20000.0;
	float P_turbo_max = 0.0;
	float P_turbo_waste = 10.0;
	float nitro_H = 1.0;
	float nitro_cooling = 1.0;
	float nitro_consumption = 0.0;
	float mixture_ratio = 14.0;
	float mixture_H = 1000000.0;
	float max_fuel_consumption = 0.0;
	float max_air_consumption = 0.0;
	float torque = 1.0;
	float torque2 = 1.0;
	float maxRPM = 7000.0;
	float Displacement = 0;
	float Compression = 0;
	float minCompression = 0;
	float maxCompression = 0;
	String fuelType = "";
	float maxTorque = 0;
	float maxHP = 0;
	float RPM_maxTorque = 0;
	float RPM_maxHP = 0;
	int flags = 0;

	public native void newNative();

	public native void deleteNative();

	DynoData()
	{
		super();
		newNative();
	}

	public void finalize()
	{
		deleteNative();
	}

	public void clear()
	{
		cylinders = 0;
		bore = 0.01;
		stroke = 0.01;
		Vmin = 100000.0;
		in_min = 1.0;
		out_min = 1.0;
		in_max = 1.0;
		out_max = 1.0;
		time_in_open = 0.0009999999;
		time_in_close = 0.0019999999;
		time_out_open = 0.0029999998;
		time_out_close = 0.0039999997;
		time_spark_min = 0.5;
		time_spark_inc = 0.0;
		time_spark_RPM0 = 0.0;
		time_spark_RPM1 = 10000.0;
		time_burn = 100000.0;
		RPM_limit = 10000.0;
		T_loss = 100.0;
		rpm_turbo_mul = 0.0;
		rpm_turbo_opt = 0.0;
		rpm_turbo_range = 0.0;
		P_turbo_max = 0.0;
		P_turbo_waste = 0.0;
		nitro_H = 1.0;
		nitro_cooling = 1.0;
		nitro_consumption = 0.0;
		mixture_ratio = 14.0;
		mixture_H = 1000000.0;
		max_fuel_consumption = 0.0;
		max_air_consumption = 0.0;
		torque = 0.0;
		torque2 = 0.0;
		maxRPM = 0.0;
		flags = 0;
	}

	public void beAverageOf(DynoData arg0, DynoData arg1)
	{
		bore = ((arg0.bore + arg1.bore) * 0.5);
		stroke = ((arg0.stroke + arg1.stroke) * 0.5);
		Vmin = ((arg0.Vmin + arg1.Vmin) * 0.5);
		in_min = ((arg0.in_min + arg1.in_min) * 0.5);
		out_min = ((arg0.out_min + arg1.out_min) * 0.5);
		in_max = ((arg0.in_max + arg1.in_max) * 0.5);
		out_max = ((arg0.out_max + arg1.out_max) * 0.5);
		time_in_open = ((arg0.time_in_open + arg1.time_in_open) * 0.5);
		time_in_close = ((arg0.time_in_close + arg1.time_in_close) * 0.5);
		time_out_open = ((arg0.time_out_open + arg1.time_out_open) * 0.5);
		time_out_close = ((arg0.time_out_close + arg1.time_out_close) * 0.5);
		time_spark_min = ((arg0.time_spark_min + arg1.time_spark_min) * 0.5);
		time_spark_inc = ((arg0.time_spark_inc + arg1.time_spark_inc) * 0.5);
		time_spark_RPM0 = ((arg0.time_spark_RPM0 + arg1.time_spark_RPM0) * 0.5);
		time_spark_RPM1 = ((arg0.time_spark_RPM1 + arg1.time_spark_RPM1) * 0.5);
		time_burn = ((arg0.time_burn + arg1.time_burn) * 0.5);
		RPM_limit = ((arg0.RPM_limit + arg1.RPM_limit) * 0.5);
		T_loss = ((arg0.T_loss + arg1.T_loss) * 0.5);
		rpm_turbo_mul = ((arg0.rpm_turbo_mul + arg1.rpm_turbo_mul) * 0.5);
		rpm_turbo_opt = ((arg0.rpm_turbo_opt + arg1.rpm_turbo_opt) * 0.5);
		rpm_turbo_range = ((arg0.rpm_turbo_range + arg1.rpm_turbo_range) * 0.5);
		P_turbo_max = ((arg0.P_turbo_max + arg1.P_turbo_max) * 0.5);
		P_turbo_waste = ((arg0.P_turbo_waste + arg1.P_turbo_waste) * 0.5);
		nitro_H = ((arg0.nitro_H + arg1.nitro_H) * 0.5);
		nitro_cooling = ((arg0.nitro_cooling + arg1.nitro_cooling) * 0.5);
		nitro_consumption = ((arg0.nitro_consumption + arg1.nitro_consumption) * 0.5);
		mixture_ratio = ((arg0.mixture_ratio + arg1.mixture_ratio) * 0.5);
		mixture_H = ((arg0.mixture_H + arg1.mixture_H) * 0.5);
		max_fuel_consumption = ((arg0.max_fuel_consumption + arg1.max_fuel_consumption) * 0.5);
		max_air_consumption = ((arg0.max_air_consumption + arg1.max_air_consumption) * 0.5);
		maxRPM = ((arg0.maxRPM + arg1.maxRPM) * 0.5);
	}

	public DynoData clone()
	{
		DynoData local1 = new DynoData();
		local1.cylinders = cylinders;
		local1.bore = bore;
		local1.stroke = stroke;
		local1.Vmin = Vmin;
		local1.in_min = in_min;
		local1.out_min = out_min;
		local1.in_max = in_max;
		local1.out_max = out_max;
		local1.time_in_open = time_in_open;
		local1.time_in_close = time_in_close;
		local1.time_out_open = time_out_open;
		local1.time_out_close = time_out_close;
		local1.time_spark_min = time_spark_min;
		local1.time_spark_inc = time_spark_inc;
		local1.time_spark_RPM0 = time_spark_RPM0;
		local1.time_spark_RPM1 = time_spark_RPM1;
		local1.time_burn = time_burn;
		local1.RPM_limit = RPM_limit;
		local1.T_loss = T_loss;
		local1.rpm_turbo_mul = rpm_turbo_mul;
		local1.rpm_turbo_opt = rpm_turbo_opt;
		local1.rpm_turbo_range = rpm_turbo_range;
		local1.P_turbo_max = P_turbo_max;
		local1.P_turbo_waste = P_turbo_waste;
		local1.nitro_H = nitro_H;
		local1.nitro_cooling = nitro_cooling;
		local1.nitro_consumption = nitro_consumption;
		local1.mixture_ratio = mixture_ratio;
		local1.mixture_H = mixture_H;
		local1.max_fuel_consumption = max_fuel_consumption;
		local1.max_air_consumption = max_air_consumption;
		local1.torque = torque;
		local1.torque2 = torque2;
		local1.maxRPM = maxRPM;
		local1.Displacement = Displacement;
		local1.Compression = Compression;
		local1.minCompression = minCompression;
		local1.maxCompression = maxCompression;
		local1.fuelType = fuelType;
		local1.maxTorque = maxTorque;
		local1.maxHP = maxHP;
		local1.RPM_maxTorque = RPM_maxTorque;
		local1.RPM_maxHP = RPM_maxHP;
		local1.flags = flags;
		return local1;
	}

	public native float calcDyno(float arg0);

	public native float getTorque(float arg0, float arg1);

	public native float getHP(float arg0, float arg1);

}
