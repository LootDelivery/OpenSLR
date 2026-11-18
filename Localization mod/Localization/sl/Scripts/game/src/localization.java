package java.game;

import java.io.*;

// Central localization manager for SLRR Light Edition.
// Dictionary structure: locale code -> LocalizationMap(key -> translated text).
// LocalizationMap provides map semantics without relying on Hashtable, which
// is not available in the Tufa script runtime.
public class localization
{
	public final static int LANG_EN = 0;
	public final static int LANG_RU = 1;
	public final static int LANG_PL = 2;
	public final static int LANG_COUNT = 3;

	final static String SETTINGS_FILE = "save/game/localization";

	static int currentLanguage = -1;
	static LocalizationMap dictionariesByLanguage;
	static LocalizationMap registeredMods;

	static void ensureInitialized()
	{
		if( dictionariesByLanguage != null )
			return;

		dictionariesByLanguage = new LocalizationMap();
		mergeDictionaries( LocalizationData.getDictionaries(), null );
	}

	static String getLanguageCode( int lang )
	{
		if( lang == LANG_RU )
			return "ru";
		if( lang == LANG_PL )
			return "pl";
		return "en";
	}

	public static String[] getLanguageNames()
	{
		return LocalizationData.getLanguageNames();
	}

	public static int getLanguage()
	{
		if( currentLanguage < 0 )
			currentLanguage = loadLanguage();

		return currentLanguage;
	}

	public static void setLanguage( int lang )
	{
		if( lang < 0 || lang >= LANG_COUNT )
			lang = LANG_EN;

		if( lang == getLanguage() )
			return;

		currentLanguage = lang;
		saveLanguage();
	}

	public static int toggleLanguage()
	{
		int next = getLanguage() + 1;
		if( next >= LANG_COUNT )
			next = LANG_EN;
		setLanguage( next );
		return currentLanguage;
	}

	// Lookup for built-in Light Edition strings.
	public static String get( String key, String fallback )
	{
		return getByKey( key, fallback );
	}

	// Lookup helper for mod-owned UI strings.
	public static String getMod( String modId, String key, String fallback )
	{
		if( modId == null || modId.length() == 0 || key == null || key.length() == 0 )
			return fallback;

		return getByKey( modId + "." + key, fallback );
	}

	static String getByKey( String key, String fallback )
	{
		ensureInitialized();
		if( key == null )
			return fallback;

		String languageCode = getLanguageCode( getLanguage() );
		String translated = getFromLanguage( languageCode, key );

		// Missing selected-language entries fall back to the English entry.
		if( translated == null && !languageCode.equals( "en" ) )
			translated = getFromLanguage( "en", key );

		if( translated == null || translated.length() == 0 )
			return fallback;

		return translated;
	}

	static String getFromLanguage( String languageCode, String key )
	{
		LocalizationMap dictionary = (LocalizationMap)dictionariesByLanguage.get( languageCode );
		if( dictionary == null )
			return null;

		Object value = dictionary.get( key );
		if( value != null && value instanceof String )
		{
			String text = (String)value;
			if( text.length() > 0 )
				return text;
		}

		return null;
	}

	// Register one complete dictionary bundle for a mod.
	// Bundle structure: locale code -> LocalizationMap(short key -> translation).
	// The manager adds modId as a namespace to every key.
	public static void registerMod( String modId, LocalizationMap dictionaries )
	{
		if( modId == null || modId.length() == 0 || dictionaries == null )
			return;

		ensureInitialized();
		if( registeredMods == null )
			registeredMods = new LocalizationMap();

		if( registeredMods.get( modId ) != null )
			return;

		mergeDictionaries( dictionaries, modId );
		registeredMods.put( modId, "registered" );
	}

	static void mergeDictionaries( LocalizationMap source, String namespace )
	{
		if( source == null )
			return;

		for( int languageIndex=0; languageIndex<source.size(); languageIndex++ )
		{
			String languageCode = source.keyAt( languageIndex );
			LocalizationMap sourceEntries = (LocalizationMap)source.valueAt( languageIndex );
			if( languageCode == null || sourceEntries == null )
				continue;

			LocalizationMap targetEntries = (LocalizationMap)dictionariesByLanguage.get( languageCode );
			if( targetEntries == null )
			{
				targetEntries = new LocalizationMap();
				dictionariesByLanguage.put( languageCode, targetEntries );
			}

			for( int keyIndex=0; keyIndex<sourceEntries.size(); keyIndex++ )
			{
				String shortKey = sourceEntries.keyAt( keyIndex );
				Object valueObject = sourceEntries.valueAt( keyIndex );
				if( shortKey == null || valueObject == null || !(valueObject instanceof String) )
					continue;

				String fullKey = shortKey;
				if( namespace != null && namespace.length() > 0 )
					fullKey = namespace + "." + shortKey;

				String value = (String)valueObject;
				if( fullKey.length() > 0 && value.length() > 0 )
					targetEntries.put( fullKey, value );
			}
		}
	}

	// The save format stays unchanged: one integer language ID.
	static int loadLanguage()
	{
		File f = new File( SETTINGS_FILE );
		if( !f.open( File.MODE_READ ) )
			return LANG_EN;

		int value = f.readInt();
		f.close();

		if( value >= 0 && value < LANG_COUNT )
			return value;

		return LANG_EN;
	}

	static void saveLanguage()
	{
		File.delete( SETTINGS_FILE );

		File f = new File( SETTINGS_FILE );
		if( f.open( File.MODE_WRITE ) )
		{
			f.write( currentLanguage );
			f.close();
		}
	}
}
