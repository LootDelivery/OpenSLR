package java.game;

import java.util.*;

// Small map implementation for the SLRR/Tufa script runtime.
// Tufa does not provide java.util.Hashtable, so this class exposes the
// familiar put/get map API while using the already-supported Vector class.
public class LocalizationMap
{
	Vector mapKeys;
	Vector mapValues;

	public LocalizationMap()
	{
		mapKeys = new Vector();
		mapValues = new Vector();
	}

	public void put( String key, Object value )
	{
		if( key == null || value == null )
			return;

		for( int i=0; i<mapKeys.size(); i++ )
		{
			String currentKey = (String)mapKeys.elementAt(i);
			if( key.equals( currentKey ) )
			{
				mapValues.setElementAt( value, i );
				return;
			}
		}

		mapKeys.addElement( key );
		mapValues.addElement( value );
	}

	public Object get( String key )
	{
		if( key == null )
			return null;

		for( int i=0; i<mapKeys.size(); i++ )
		{
			String currentKey = (String)mapKeys.elementAt(i);
			if( key.equals( currentKey ) )
				return mapValues.elementAt(i);
		}

		return null;
	}

	public int size()
	{
		return mapKeys.size();
	}

	public String keyAt( int index )
	{
		if( index < 0 || index >= mapKeys.size() )
			return null;
		return (String)mapKeys.elementAt(index);
	}

	public Object valueAt( int index )
	{
		if( index < 0 || index >= mapValues.size() )
			return null;
		return mapValues.elementAt(index);
	}
}
