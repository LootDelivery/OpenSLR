package java.game.cars;

import java.game.*;

// One file for all Opel Manta 400 translations (EN/RU/PL).
// Keys are short and are automatically prefixed with "opel_manta_400".
public class Localization_Opel_Manta_400
{
	static LocalizationMap cachedDictionaries;

	public static LocalizationMap getDictionaries()
	{
		if( cachedDictionaries != null )
			return cachedDictionaries;

		LocalizationMap all = new LocalizationMap();
		LocalizationMap en = new LocalizationMap();
		en.put( "vehicle.name", "Opel Manta 400" );
		en.put( "chassis.name", "Opel Manta 400 chassis" );
		en.put( "chassis.description", "In 1979, Opel collaborated with German tuner Irmscher and Cosworth in UK to create the Opel Manta 400. Irmscher designed the exterior and interior of the Manta 400. Cosworth built a 2.4-litre engine that was mounted with the 16-valve head to provide massive output, and the opportunity to make several tune-ups for the rally drivers." );
		all.put( "en", en );

		LocalizationMap ru = new LocalizationMap();
		ru.put( "vehicle.name", "Opel Manta 400" );
		ru.put( "chassis.name", "Шасси Opel Manta 400" );
		ru.put( "chassis.description", "В 1979 году Opel совместно с немецким тюнером Irmscher и британской Cosworth создал Opel Manta 400. Экстерьер и интерьер Manta 400 разработала студия Irmscher. Cosworth построила 2,4-литровый двигатель с 16-клапанной головкой, который обеспечивал огромную отдачу и открывал возможности для многочисленных доработок раллийных версий." );
		all.put( "ru", ru );

		LocalizationMap pl = new LocalizationMap();
		pl.put( "vehicle.name", "Opel Manta 400" );
		pl.put( "chassis.name", "Podwozie Opel Manta 400" );
		pl.put( "chassis.description", "W 1979 roku Opel we wspуіpracy z niemieckim tunerem Irmscher oraz brytyjsk№ firm№ Cosworth stworzyі model Opel Manta 400. Nadwozie i wnкtrze Mantity 400 zaprojektowaіa firma Irmscher. Cosworth zbudowaіa 2,4-litrowy silnik z 16-zaworow№ gіowic№, ktуry zapewniaі ogromn№ moc i moїliwoњж wielu modyfikacji dla rajdowcуw." );
		all.put( "pl", pl );

		cachedDictionaries = all;
		return cachedDictionaries;
	}
}
