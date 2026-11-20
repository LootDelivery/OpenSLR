package java.game.parts.Work_VS_XX;

import java.render.osd.*;
import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.game.*;
import java.game.parts.rgearpart.reciprocatingrgearpart.*;
import java.game.parts.*;

public class Work_VS_XX_Rim_Lock extends Wheel
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

	String[] LockColorText =
	{
		"Anodized Black",
		"Anodized Blue",
		"Anodized Gold",
		"Anodized Green",
		"Anodized Metal",
		"Anodized Red"
	};

	int[] LockRenders =
	{
		parts.Work_VS_XX:0x00000040r, // Black
		parts.Work_VS_XX:0x00000041r, // Blue
		parts.Work_VS_XX:0x00000042r, // Gold
		parts.Work_VS_XX:0x00000043r, // Green
		parts.Work_VS_XX:0x00000044r, // Metal
		parts.Work_VS_XX:0x00000045r  // Red
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

	int lock_color, old_lock_color;

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
			lock_color = saveGame.readInt();

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
			saveGame.write( lock_color );
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
		old_lock_color = lock_color;

		m.addItem( "Offset", 1, f_offset, min_offset, max_offset, 1.1, null ).printValue( "   %1.1f" );
		m.addSeparator();
		m.addItem( "Rim Color", 2, rim_color, RimColorText, null );
		m.addItem( "Bolts Color", 3, bolts_color, BoltsColorText, null );
		m.addItem( "Lock Color", 4, lock_color, LockColorText, null );
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
			lock_color = old_lock_color;

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
			lock_color = ((MultiChoice)m.gadget).value;
			updateLockColor();
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
		updateLockColor();
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

	void updateLockColor()
	{
		updateRimPart( 4, parts.Work_VS_XX:0x00000074r, LockRenders[ lock_color ] );
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
