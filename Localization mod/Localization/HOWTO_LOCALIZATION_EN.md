# How to Add Translations to a Mod for SLRR Light Edition

This guide is for mod creators. You do not need to learn Java or Tufa. The process is based on copying ready-made snippets, replacing names and text, and saving the files.

This guide is for **SLRR Light Edition with the Localization Manager installed**. It will not work with the original game or with older Light Edition builds that do not include the Localization Manager.

---

## The important things to know first

- Translations are stored **inside the mod itself**, not in the game's shared files.
- A mod uses **one localization file for all languages**.
- The current game language selector offers English (EN), Russian (RU), and Polish (PL).
- Russian and Polish text can be typed **normally, using regular letters inside quotation marks**. You do not need a converter, HEX codes, or `\\xNN` sequences.
- Mods without localization will continue to work as before. You do not need to add files to them or change their source code.

When a translation for the selected language is missing, the game tries the English entry from the dictionary. If that is missing too, the original text already present in the mod is displayed.

## What can be translated

| Text | Where it is used |
|---|---|
| Vehicle name | The vehicle's name in the interface |
| Chassis name | The name of the vehicle's base chassis object |
| Chassis description | The vehicle description |
| Part name | The part name in inventory and information windows |
| Part description | The part description |
| Mod's own text | For example, a button label or a message in a custom window |

Light Edition provides ready-made methods for vehicle, chassis, and part names and descriptions. A separate short example for custom windows is included below.

---

# Part 1. Get ready

## What you need

- SLRR Light Edition with a working Localization Manager.
- The source folder for your mod.
- A plain-text editor, such as Notepad++, VS Code, or another editor you already use.
- A backup copy of your mod folder.

**Important:** You need the original `.java` source files. Do not edit `.class` files — those are already compiled game files. Renaming a `.class` file to `.java` will not restore the source code. If you only have `.class` files, ask the mod author for the sources.

## A typical car mod folder

For example:

```text
cars/
└── racers/
    └── My_Car_data/
        └── scripts/
            └── src/
                ├── My_Car.java
                └── ... other car source files
```

`My_Car.java` is the main vehicle source file in this example. Your mod's folder and file names will be different.

Make a copy of the mod folder before editing anything. If something goes wrong, you can restore the original files.

---

# Part 2. Create one translation file

In this example, we will add translations for a fictional car called `My Car`.

## Step 1. Create the file

In the `src` folder, next to the car's main source file, create a new file named:

```text
Localization_My_Car.java
```

You can choose a different name after `Localization_`. The file name and the class name inside the file must match. The example below already follows this rule. For example, if you name the file `Localization_Opel.java`, the class declaration inside it must be `public class Localization_Opel`, and the connection line must use `Localization_Opel.getDictionaries()`.

## Step 2. Check the package line

Open the car's main source file, `My_Car.java`. At the very top, find the line that starts with `package`. For example:

```java
package java.game.cars;
```

The first line in the new `Localization_My_Car.java` file must be **exactly the same**. If your car source has a different package line, copy that one instead of using the example.

## Step 3. Copy the ready-made code

Copy the entire example below into `Localization_My_Car.java`. Then replace the sample names and description with the text from your own mod.

```java
package java.game.cars;

import java.game.*;

public class Localization_My_Car
{
    static LocalizationMap cachedDictionaries;

    public static LocalizationMap getDictionaries()
    {
        if( cachedDictionaries != null )
            return cachedDictionaries;

        LocalizationMap all = new LocalizationMap();

        // ENGLISH
        LocalizationMap en = new LocalizationMap();
        en.put( "vehicle.name", "My Car" );
        en.put( "chassis.name", "My Car chassis" );
        en.put( "chassis.description", "English description of the car." );
        all.put( "en", en );

        // RUSSIAN
        LocalizationMap ru = new LocalizationMap();
        ru.put( "vehicle.name", "Моя машина" );
        ru.put( "chassis.name", "Шасси моей машины" );
        ru.put( "chassis.description", "Описание моей машины на русском языке." );
        all.put( "ru", ru );

        // POLISH
        LocalizationMap pl = new LocalizationMap();
        pl.put( "vehicle.name", "Mój samochód" );
        pl.put( "chassis.name", "Podwozie mojego samochodu" );
        pl.put( "chassis.description", "Opis mojego samochodu w języku polskim." );
        all.put( "pl", pl );

        cachedDictionaries = all;
        return cachedDictionaries;
    }
}
```

**Russian and Polish text above is written normally, using regular letters.** Type your translations directly between the quotation marks in the same way. Do not convert the text into special sequences.

### What should you change, and what should you leave alone?

In this line:

```java
en.put( "vehicle.name", "My Car" );
```

- `vehicle.name` is the key. It is the short identifier for this particular text. **Do not change it** unless you also change the same key wherever the translation is connected.
- `My Car` is the actual text shown to the player. Replace it with your vehicle's name.

The keys on the left must be identical in every language. The text on the right is different:

```java
en.put( "vehicle.name", "My Car" );
ru.put( "vehicle.name", "Моя машина" );
pl.put( "vehicle.name", "Mój samochód" );
```

If you do not have a translation for one of the languages yet, you can leave out that entry. For example, if you do not have a Polish translation, simply do not add the Polish entry. When PL is selected, the game will try the English translation, and if that is missing, it will use the mod's original text.

Do not remove the technical lines or rename `Localization_My_Car`, `getDictionaries`, `LocalizationMap`, `en`, `ru`, or `pl`.

---

# Part 3. Register the localization class in the mod's RPK

This step is required. Creating `Localization_My_Car.java` is not enough: the game must also know that the compiled `Localization_My_Car.class` is a resource belonging to your mod. Add a resource entry to the car's RPK for this.

**Do not worry about the `.res` and `.rsd` names.** These are not two separate Windows files you need to create. They are the two parts of one entry inside the text representation of an RPK.

## Step 1. Find your mod's RPK

A car usually has an RPK next to its data folder. For example:

```text
cars/
└── racers/
    ├── My_Car.rpk
    └── My_Car_data/
        └── scripts/
            └── src/
                ├── My_Car.java
                └── Localization_My_Car.java
```

Make a backup copy of `My_Car.rpk` before editing it.

## Step 2. Open the RPK as a text resource database

You cannot edit an RPK directly in a normal text editor. First, decode it into a text RDB using `resdecode` or a compatible Resource Tools GUI. Open the resulting `.rdb` file in Notepad++, VS Code, or another text editor.

After editing the RDB, convert it back into an RPK using `resconvert` or the matching command in Resource Tools GUI.

## Step 3. Add the localization resource entry

Add the following block to the end of the RDB. This example is for `My_Car`; adjust the path and IDs for your own mod.

```text
<FILE 00000347.res >
typeof 8
superid 0x00000104
typeid 0x0000A390
alias localization
isparentcompatible 1.00
</FILE>
<FILE 00000347.rsd >
script cars\racers\My_Car_data\scripts\Localization_My_Car.class
lod_amp 3.000
</FILE>
```

Important rules:

- `00000347` is the entry number in the RPK. It must be unused in this RDB. Choose another number if it is already present. The number in the `.res` and `.rsd` blocks **must match**.
- `typeid 0x0000A390` is another resource identifier. It must be unique among the `typeid` values in this RPK. Choose a different unused value if necessary.
- Keep `superid 0x00000104` exactly as shown. It identifies the resource type/category; it is not an ID you invent separately for every mod.
- You can leave `alias localization` unchanged.
- The path after `script` must point to the **compiled `.class` file**, not the `.java` source. In this example, the source is in `scripts/src`, but the RPK path points to `scripts` — without the `src` folder.
- The path must match the file's actual location inside the game folder. Replace `My_Car` and the file name with the names used by your mod.

`typeof 8` is the resource type used for script resources. These RPK entries are used for `.class` files and related game scripts.

## What about `isparentcompatible` and `lod_amp`?

You do not need to work out or change these values. Leave both lines exactly as they are in the tested example:

```text
isparentcompatible 1.00
lod_amp 3.000
```

`isparentcompatible` appears to be related to compatibility with a parent resource type. Its exact internal behavior is not explained in the available documentation; RPK guides generally advise leaving it alone. Keep it at `1.00` for this entry.

`lod_amp` appears in RPK records related to LOD (Level of Detail), including some script records and resources that use models. Its exact effect on a record that points only to a localization class is not reliably documented. Use the tested value `3.000` and do not try to tune it. We have also not verified whether this line can be safely omitted.

## Step 4. Build the RPK again

Save the RDB and convert it back into `My_Car.rpk` using `resconvert` or Resource Tools GUI. Make sure the rebuilt RPK replaces the old copy in your mod folder.

After the source is compiled, this file should exist:

```text
cars/racers/My_Car_data/scripts/Localization_My_Car.class
```

That is the file referenced by the `script` line in the RPK. Do not point the entry to `src/Localization_My_Car.java`.

**Important:** if your mod does not use localization, you do not need to add this RPK entry. Mods without localization will continue to work as before.

---

# Part 4. Connect the translations to the vehicle

The dictionary file is ready. Now you need to tell the vehicle which entries from that file it should use.

## Step 1. Open the vehicle source file

Open `My_Car.java` and find the constructor. It usually starts approximately like this:

```java
public My_Car( int id )
{
    super( id );
```

Inside the constructor, find the original English values for `vehicleName`, `name`, and `description`. For example:

```java
vehicleName = "My Car";
name = "My Car chassis";
description = "English description of the car.";
```

Your car will have different text. Do not delete the vehicle's existing settings or specifications.

## Step 2. Add the connection lines

Immediately **after the original assignments** to `vehicleName`, `name`, and `description`, insert:

```java
localization.registerMod( "my_car_mod", Localization_My_Car.getDictionaries() );
setLocalizedTexts(
    "my_car_mod", "chassis.name", "My Car chassis",
    "chassis.description", "English description of the car."
);
setLocalizedVehicleName( "my_car_mod", "vehicle.name", "My Car" );
```

Replace these parts of the example:

- `my_car_mod` — with a short, unique ID for your mod, for example `opel_manta_400`. Use exactly the same ID in every connection line. The manager adds this ID to each key automatically: for example, `vehicle.name` becomes `opel_manta_400.vehicle.name`.
- `Localization_My_Car` — with the name of your dictionary class, if you named it differently.
- `My Car chassis` — with the original English chassis name from your source file.
- `English description of the car.` — with the original English description from your source file.
- `My Car` in the last line — with the original English vehicle name.

The keys `chassis.name`, `chassis.description`, and `vehicle.name` must match the keys in the translation file. You can choose different keys, but they must be identical in the dictionary and in the connection lines.

### Do I need to add an import?

The main vehicle source file usually already contains:

```java
import java.game.*;
```

This is needed to access the localization manager. If the line is missing, add it below the `package ...;` line.

If the dictionary file is in the same package (meaning it has the same `package` line), you do not need a separate `import` for it.

## Why do the connection lines go after the original names?

The mod first assigns the original English text. The localization methods then replace it with the translation. If you put the connection lines earlier, later assignments could overwrite the translation with English text again.

---

# Part 5. Translate parts

Use the same dictionary file for parts. Do not create a separate file for each language or a separate dictionary for every part.

## Step 1. Find the part source file

Open the `.java` source file for the part. Find its constructor and the original assignments to `name` and `description`.

For example:

```java
name = "My Turbo Kit";
description = "A bolt-on turbocharger kit.";
```

## Step 2. Add the translations to the shared dictionary

In `Localization_My_Car.java`, add new keys to the relevant language sections:

```java
en.put( "turbo.name", "My Turbo Kit" );
en.put( "turbo.description", "A bolt-on turbocharger kit." );

ru.put( "turbo.name", "Турбокомплект" );
ru.put( "turbo.description", "Комплект турбонаддува для двигателя." );

pl.put( "turbo.name", "Zestaw turbo" );
pl.put( "turbo.description", "Zestaw turbosprężarki do silnika." );
```

The keys `turbo.name` and `turbo.description` are examples. Choose clear keys for other parts. Do not reuse the same key for different texts within one mod.

## Step 3. Connect the translation to the part

After the original `name` and `description` assignments, add:

```java
localization.registerMod( "my_car_mod", Localization_My_Car.getDictionaries() );
setLocalizedTexts(
    "my_car_mod", "turbo.name", "My Turbo Kit",
    "turbo.description", "A bolt-on turbocharger kit."
);
```

Use the same `my_car_mod` ID and the same `Localization_My_Car` class as you used for the vehicle.

**Important:** if the vehicle and parts belong to one mod, use one shared dictionary and one `modId`. That dictionary must contain the translations for the vehicle and all its parts. Do not create separate dictionaries using the same `modId`: the manager registers one translation bundle per ID.

If the part is in a different package from the dictionary file, add an `import` with the dictionary class's full name to the part source. For example, if the dictionary says `package java.game.cars;`, the import is:

```java
import java.game.cars.Localization_My_Car;
```

If you are not sure which package is used in the part source, do not guess — ask someone to help identify it.

Calling `registerMod()` more than once with an already registered ID is safe: the manager will not load the same bundle again. You can therefore add it to the vehicle constructor and to individual part constructors if those parts can be opened in the game independently.

---

# Part 6. Text in your own windows and menus

If your mod has its own window, button, or message, you can request a translation directly.

For example, add the `dialog.install` key to the language dictionaries:

```java
en.put( "dialog.install", "Install turbo kit" );
ru.put( "dialog.install", "Установить турбокомплект" );
pl.put( "dialog.install", "Zainstaluj zestaw turbo" );
```

Before the first call to `localization.getMod()`, the dictionary must have been registered with `localization.registerMod()`. In a simple setup, this already happens in the vehicle or part constructor. If your custom window can open before that, register the dictionary in the code that starts the window.

Then use this in the source file for the window:

```java
String buttonText = localization.getMod(
    "my_car_mod",
    "dialog.install",
    "Install turbo kit"
);
```

The last parameter is the original English text to use when a translation cannot be found.

This example is only for the mod's own interface elements. For normal vehicle, chassis, and part names, use the ready-made methods described above.

---

# Part 7. Test the result

1. Save the modified `.java` files.
2. Launch the game using the same process you normally use to test changes to your scripts.
3. Select RU in the game settings.
4. Open the vehicle or part and check its name and description.
5. Repeat the check for EN and PL.
6. Check at least one entry for which you intentionally did not add a translation. The game should display the English entry or the original text.

Do not remove the original English assignments to `vehicleName`, `name`, and `description` from the source files. They are needed as fallbacks.

---

# Frequently asked questions

### Do I have to add localization to every mod?

No. It is optional. A mod without localization files and without calls to the Localization Manager will continue to work as before.

### Do I need a separate file for RU, EN, and PL?

No. All languages for one mod are stored in one file: `Localization_ModName.java`.

### Can I type Cyrillic and Polish letters directly into the file?

Yes. Type normal text inside the quotation marks, just like in the examples. Do not use converters, HEX codes, or special `\\xNN` sequences.

### What happens if a translation is not ready?

The game tries the English entry. If that is missing, it displays the original English text from the mod. You can start with one language and add the others later.

### Can one dictionary be used for several vehicles and parts?

Yes. This is the recommended approach for one mod. Add separate keys to the shared dictionary, such as `manta.vehicle.name`, `kadett.vehicle.name`, `turbo.name`, and `turbo.description`.

### I only have `.class` files, not `.java` sources. What should I do?

You cannot connect localization yourself using this guide. Ask the mod author for the source files, or ask someone who can make the changes and recompile the scripts.

### Where can I see a working localization example?

Look at `Opel_Manta_400.java` and `Localization_Opel_Manta_400.java` in the SLRR Light Edition source files. They show how the dictionary is registered and how the vehicle name, chassis name, and description are connected to it.

---

## Quick checklist

- [ ] One localization file has been created for the mod.
- [ ] The required EN, RU, and/or PL translations have been added.
- [ ] The same keys are used in every language.
- [ ] The dictionary is registered with `localization.registerMod()`.
- [ ] The vehicle/part name and description are connected with `setLocalizedTexts()`.
- [ ] The vehicle name is connected with `setLocalizedVehicleName()`.
- [ ] The original English text has been kept.
- [ ] The translations have been tested in the game.
