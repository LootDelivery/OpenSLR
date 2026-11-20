package java.game.parts.Work_VS_XX;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.parts.rgearpart.reciprocatingrgearpart.*;
import java.game.parts.*;

public class Work_VS_XX_Rim extends Wheel
{
	// region Constants

	String[] RimColorText =
	{
		"Black with Chrome Barrel",
		"Blue with Chrome Barrel",
		"Brown with Chrome Barrel",
		"Gold with Chrome Barrel",
		"Green with Chrome Barrel",
		"Metal with Chrome Barrel",
		"Red with Chrome Barrel",
		"White with Chrome Barrel"
	};

	int[] RimRenders =
	{
		parts.Work_VS_XX:0x00000000r, // Black
		parts.Work_VS_XX:0x00000000r, // Blue
		parts.Work_VS_XX:0x00000000r, // Brown
		parts.Work_VS_XX:0x00000000r, // Gold
		parts.Work_VS_XX:0x00000000r, // Green
		parts.Work_VS_XX:0x00000000r, // Metal
		parts.Work_VS_XX:0x00000000r, // Red
		parts.Work_VS_XX:0x00000000r  // White
	};

	String[] BoltsColorText =
	{
		"Anodized Black",
		"Anodized Blue",
		"Anodized Gold",
		"Anodized Green",
		"Anodized Metal",
		"Anodized Red"
	};

	int[] BoltsRenders =
	{
		parts.Work_VS_XX:0x00000000r, // Black
		parts.Work_VS_XX:0x00000000r, // Blue
		parts.Work_VS_XX:0x00000000r, // Gold
		parts.Work_VS_XX:0x00000000r, // Green
		parts.Work_VS_XX:0x00000000r, // Metal
		parts.Work_VS_XX:0x00000000r  // Red
	};

	String[] CapTypeText =
	{
		"Work VS Silver",
		"Work VS Gold on Red",
		"Work Wheels Silver on Red",
		"Work VS Gold on Black"
	};

	int[] CapRenders =
	{
		parts.Work_VS_XX:0x0000003Cr, // Type 1
		parts.Work_VS_XX:0x0000003Dr, // Type 2
		parts.Work_VS_XX:0x0000003Er, // Type 3
		parts.Work_VS_XX:0x0000003Fr  // Type 4
	};

	String[] NutsTypeText =
	{
		"Leggdura Racing RL53",
		"Leggdura Racing CL53"
	};

	String[] NutsColorText =
	{
		"Anodized Black",
		"Anodized Blue",
		"Anodized Gold",
		"Anodized Green",
		"Anodized Metal",
		"Anodized Red"
	};

	int[] NutsRenders =
	{
		parts.Work_VS_XX:0x00000046r, // Type 1 Black
		parts.Work_VS_XX:0x00000047r, // Type 1 Blue
		parts.Work_VS_XX:0x00000048r, // Type 1 Gold
		parts.Work_VS_XX:0x00000049r, // Type 1 Green
		parts.Work_VS_XX:0x0000004Ar, // Type 1 Metal
		parts.Work_VS_XX:0x0000004Br, // Type 1 Red
		parts.Work_VS_XX:0x0000004Cr, // Type 2 Black
		parts.Work_VS_XX:0x0000004Dr, // Type 2 Blue
		parts.Work_VS_XX:0x0000004Er, // Type 2 Gold
		parts.Work_VS_XX:0x0000004Fr, // Type 2 Green
		parts.Work_VS_XX:0x00000050r, // Type 2 Metal
		parts.Work_VS_XX:0x00000051r  // Type 2 Red
	};

	// endregion


	// region Variables

	float f_offset, old_offset;

	float min_offset = -65.0;

	float max_offset = 65.0;

	float default_offset = 0.0;

	float diameter = 19.0;

	float width = 11.0;

	int rim_color, old_rim_color;

	int bolts_color = 2, old_bolts_color;

	int cap_type = 3, old_cap_type;

	int nuts_type, old_nuts_type;

	int nuts_color, old_nuts_color;

	// endregion


	// region Common Public Methods

	public void load( File saveGame )
	{
		super.load( saveGame );

		int	save_ver = saveGame.readInt();

		if (save_ver >= 1)
		{
			f_offset = saveGame.readFloat();
			rim_color = saveGame.readInt();
			bolts_color = saveGame.readInt();
			cap_type = saveGame.readInt();
			nuts_type = saveGame.readInt();
			nuts_color = saveGame.readInt();

			updateAll();
		}
	}

	public void save( File saveGame )
	{
		super.save( saveGame );

		int	save_ver = 1;

		saveGame.write( save_ver );

		if (save_ver >= 1)
		{
			saveGame.write( f_offset );
			saveGame.write( rim_color );
			saveGame.write( bolts_color );
			saveGame.write( cap_type );
			saveGame.write( nuts_type );
			saveGame.write( nuts_color );
		}
	}

	// endregion


	// region Common Protected Methods

	void setupCustomRim( String name, float diameter, float width )
	{
		this.name = name;

		rim_type = rtFACTORY;
		this.diameter = diameter;
		this.width = width;
		f_offset = default_offset;
		SetupWheel( diameter, width, default_offset );

		description = "";

		updateAll();
	}

	// endregion


	// region Tuning Public Methods

	public int isTuneable()
	{
		return 1;
	}

	public void buildTuningMenu( Menu m )
	{
		old_offset = f_offset;
		old_rim_color = rim_color;
		old_bolts_color = bolts_color;
		old_cap_type = cap_type;
		old_nuts_type = nuts_type;
		old_nuts_color = nuts_color;

		m.addItem( "Offset", 1, f_offset, min_offset, max_offset, 1.1, null ).printValue( "   %1.1f" );
		m.addSeparator();
		m.addItem( "Rim Color", 2, rim_color, RimColorText, null );
		m.addItem( "Bolts Color", 3, bolts_color, BoltsColorText, null );
		m.addItem( "Hub Cap", 4, cap_type, CapTypeText, null );
		m.addItem( "Lug Nuts", 5, nuts_type, NutsTypeText, null );
		m.addItem( "Lug Nuts Color", 6, nuts_color, NutsColorText, null );
		m.addSeparator();
		m.addItem( "Reset to factory defaults", 0 );

		GameLogic.player.car.wakeUp();
	}

	public void endTuningSession( int cancelled )
	{
		if( cancelled )
		{
			f_offset = old_offset;
			rim_color = old_rim_color;
			bolts_color = old_bolts_color;
			cap_type = old_cap_type;
			nuts_type = old_nuts_type;
			nuts_color = old_nuts_color;

			updateAll();
		}
		else
		{
			if (f_offset != old_offset)
			{
				GameLogic.spendTime(5*60);
			}

			getCar_LocalVersion();

			if (the_car)
			{
				the_car.forceUpdate();
			}
		}
	}

	public void handleMessage( Event m )
	{
		if( m.cmd == 0 )
		{
			f_offset = default_offset;
			m.gadget.osd.findGadget( this, 1 ).setValue( default_offset );
			updateRimOffset();
		}
		else if( m.cmd == 1 )
		{
			f_offset = ((Slider)m.gadget).value;
			((Slider)m.gadget).changeVLabelText( Float.toString(f_offset, "   %1.1f"));
			updateRimOffset();
		}
		else if( m.cmd == 2 )
		{
			rim_color = ((MultiChoice)m.gadget).value;
			updateRimColor();
		}
		else if( m.cmd == 3 )
		{
			bolts_color = ((MultiChoice)m.gadget).value;
			updateBoltsColor();
		}
		else if( m.cmd == 4 )
		{
			cap_type = ((MultiChoice)m.gadget).value;
			updateCapType();
		}
		else if( m.cmd == 5 )
		{
			nuts_type = ((MultiChoice)m.gadget).value;
			updateNutsType();
		}
		else if( m.cmd == 6 )
		{
			nuts_color = ((MultiChoice)m.gadget).value;
			updateNutsType();
		}
	}

	public void updatevariables()
	{
		super.updatevariables();
		updateAll();
	}

	// endregion


	// region Tuning Protected Methods

	void updateAll()
	{
		updateRimOffset();
		updateRimColor();
		updateBoltsColor();
		updateCapType();
		updateNutsType();
	}

	void updateRimOffset()
	{
		setSlotPos( 1, new Vector3( f_offset/1000.0, 0, 0 ), null );
	}

	void updateRimColor()
	{
		setRenderType( RimRenders[ rim_color ] );
	}

	void updateBoltsColor()
	{
		updateRimPart( 3, parts.Work_VS_XX:0x00000072r, BoltsRenders[ bolts_color ] );
	}

	void updateCapType()
	{
		updateRimPart( 4, parts.Work_VS_XX:0x00000073r, CapRenders[ cap_type ] );
	}

	void updateNutsType()
	{
		updateRimPart( 5, parts.Work_VS_XX:0x00000075r, NutsRenders[ nuts_type * NutsColorText.length + nuts_color ] );
	}

	// endregion


	// region Tuning Private Methods

	private void updateRimPart( int slotId, int partId, int renderType )
	{
		Part part = partOnSlot( slotId );
		if( part == null )
		{
			disableSlot( slotId, 0 );
			part = addPart( partId, null );
			disableSlot( slotId, 1 );
		}
		if( part )
		{
			part.setRenderType( renderType );
		}
	}

	// endregion
}
