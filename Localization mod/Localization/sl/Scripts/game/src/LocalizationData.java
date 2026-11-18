package java.game;

// Built-in Light Edition translations. All supported languages are in this file.
// Structure: locale code -> LocalizationMap(key -> translated text).
public class LocalizationData
{
	public static LocalizationMap getDictionaries()
	{
		LocalizationMap all = new LocalizationMap();
		LocalizationMap en = new LocalizationMap();
		en.put( "MAINMENU_TITLE", "MAIN MENU" );
		en.put( "MAINMENU_NEW_CAREER", "NEW CAREER" );
		en.put( "MAINMENU_LOAD_CAREER", "LOAD CAREER" );
		en.put( "MAINMENU_LOAD_CAR", "LOAD CAR" );
		en.put( "MAINMENU_SAVE_CAR", "SAVE CAR" );
		en.put( "MAINMENU_OPTIONS", "OPTIONS" );
		en.put( "MAINMENU_EXTRAS", "EXTRAS" );
		en.put( "MAINMENU_EXIT", "EXIT" );
		en.put( "MAINMENU_BACK_TO_GARAGE", "BACK TO GARAGE" );
		en.put( "MAINMENU_LANGUAGE", "LANGUAGE" );
		en.put( "MAINMENU_LANGUAGE_HINT", "Switch interface language" );
		en.put( "DIALOG_EXIT_TITLE", "EXIT" );
		en.put( "DIALOG_EXIT_BODY", "Do you really want to Exit?" );
		en.put( "DIALOG_EXIT_BODY_CAREER", "Do you really want to Exit? \n \n Note that your current career will be automatically saved." );
		en.put( "DIALOG_DELETE_CAREER_TITLE", "Delete career" );
		en.put( "DIALOG_DELETE_CAREER_BODY", "Do you really want to delete your ongoing career?" );
		en.put( "DIALOG_DELETE_CAR_TITLE", "Delete car" );
		en.put( "DIALOG_DELETE_CAR_BODY", "Do you really want to delete your car?" );
		en.put( "DIALOG_DELETE_PAINT_TITLE", "Delete paintjob" );
		en.put( "DIALOG_DELETE_PAINT_BODY", "Do you really want to delete your paintjob?" );
		en.put( "DIALOG_DELETE_CONTINUE", "Do you really want to continue?" );
		all.put( "en", en );

		LocalizationMap ru = new LocalizationMap();
		ru.put( "MAINMENU_TITLE", "ГЛАВНОЕ МЕНЮ" );
		ru.put( "MAINMENU_NEW_CAREER", "НОВАЯ КАРЬЕРА" );
		ru.put( "MAINMENU_LOAD_CAREER", "ЗАГРУЗИТЬ КАРЬЕРУ" );
		ru.put( "MAINMENU_LOAD_CAR", "ЗАГРУЗИТЬ МАШИНУ" );
		ru.put( "MAINMENU_SAVE_CAR", "СОХРАНИТЬ МАШИНУ" );
		ru.put( "MAINMENU_OPTIONS", "НАСТРОЙКИ" );
		ru.put( "MAINMENU_EXTRAS", "ДОПОЛНЕНИЯ" );
		ru.put( "MAINMENU_EXIT", "ВЫХОД" );
		ru.put( "MAINMENU_BACK_TO_GARAGE", "ОБРАТНО В ГАРАЖ" );
		ru.put( "MAINMENU_LANGUAGE", "ЯЗЫК" );
		ru.put( "MAINMENU_LANGUAGE_HINT", "Смена языка интерфейса (главное меню)" );
		ru.put( "DIALOG_EXIT_TITLE", "ВЫХОД" );
		ru.put( "DIALOG_EXIT_BODY", "Действительно выйти из игры?" );
		ru.put( "DIALOG_EXIT_BODY_CAREER", "Действительно выйти? \n \n Обратите внимание: текущая карьера будет автоматически сохранена." );
		ru.put( "DIALOG_DELETE_CAREER_TITLE", "Удаление карьеры" );
		ru.put( "DIALOG_DELETE_CAREER_BODY", "Действительно удалить текущую карьеру?" );
		ru.put( "DIALOG_DELETE_CAR_TITLE", "Удаление машины" );
		ru.put( "DIALOG_DELETE_CAR_BODY", "Действительно удалить эту машину?" );
		ru.put( "DIALOG_DELETE_PAINT_TITLE", "Удаление покраски" );
		ru.put( "DIALOG_DELETE_PAINT_BODY", "Действительно удалить эту покраску?" );
		ru.put( "DIALOG_DELETE_CONTINUE", "Действительно продолжить?" );
		all.put( "ru", ru );

		LocalizationMap pl = new LocalizationMap();
		pl.put( "MAINMENU_TITLE", "MENU GЈУWNE" );
		pl.put( "MAINMENU_NEW_CAREER", "NOWA KARIERA" );
		pl.put( "MAINMENU_LOAD_CAREER", "WCZYTAJ KARIERК" );
		pl.put( "MAINMENU_LOAD_CAR", "WCZYTAJ SAMOCHУD" );
		pl.put( "MAINMENU_SAVE_CAR", "ZAPISZ SAMOCHУD" );
		pl.put( "MAINMENU_OPTIONS", "OPCJE" );
		pl.put( "MAINMENU_EXTRAS", "DODATKI" );
		pl.put( "MAINMENU_EXIT", "WYJЊCIE" );
		pl.put( "MAINMENU_BACK_TO_GARAGE", "POWRУT DO GARAЇU" );
		pl.put( "MAINMENU_LANGUAGE", "JКZYK" );
		pl.put( "MAINMENU_LANGUAGE_HINT", "Zmiana jкzyka interfejsu" );
		pl.put( "DIALOG_EXIT_TITLE", "WYJЊCIE" );
		pl.put( "DIALOG_EXIT_BODY", "Czy na pewno chcesz wyjњж?" );
		pl.put( "DIALOG_EXIT_BODY_CAREER", "Czy na pewno chcesz wyjњж? \n \n Uwaga: Twoja obecna kariera zostanie automatycznie zapisana." );
		pl.put( "DIALOG_DELETE_CAREER_TITLE", "Usuс karierк" );
		pl.put( "DIALOG_DELETE_CAREER_BODY", "Czy na pewno chcesz usun№ж trwaj№c№ karierк?" );
		pl.put( "DIALOG_DELETE_CAR_TITLE", "Usuс samochуd" );
		pl.put( "DIALOG_DELETE_CAR_BODY", "Czy na pewno chcesz usun№ж swуj samochуd?" );
		pl.put( "DIALOG_DELETE_PAINT_TITLE", "Usuс lakier" );
		pl.put( "DIALOG_DELETE_PAINT_BODY", "Czy na pewno chcesz usun№ж swуj lakier?" );
		pl.put( "DIALOG_DELETE_CONTINUE", "Czy na pewno chcesz kontynuowaж?" );
		all.put( "pl", pl );

		return all;
	}

	// Captions are intentionally kept in their own native language.
	public static String[] getLanguageNames()
	{
		String[] names = new String[3];
		names[0] = "ENGLISH";
		names[1] = "РУССКИЙ";
		names[2] = "POLSKI";
		return names;
	}
}
