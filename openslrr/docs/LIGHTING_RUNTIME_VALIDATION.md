# LIGHTING_RUNTIME_VALIDATION — L2: трассировка связей световых ресурсов

Статусы: `[Ф-код]` — подтверждено кодом; `[Ф-runtime]` — наблюдалось при
исполнении host-кода на реальных данных; `[Г]` — гипотеза, совместимая с
наблюдениями; `[?]` — неизвестно.

Базовая ревизия: `31078ac` (`L1.1a`). Документ ничего не меняет в рендере: все
выводы получены чтением кода, запуском host-сборки и отдельного headless-стенда,
который вызывает те же функции разбора и разрешения ресурсов на реальных данных
установки.

## L2.0. Область

Закрывается блокер №1 §L1.12 — «связь объект/деталь → световой ресурс». Числовые
параметры (`range`, `attenuation`, `theta/phi`, `attH/attV`, `fake*`) не
исследуются, реализация Point/Spot не начиналась.

Главный результат L2, кратко:

1. Ссылки уличных объектов (`objects.rpk`, 9 штук) **разбираются** host-кодом и
   **отбрасываются** на этапе фильтра геометрии: световой ресурс не имеет меша.
2. Ссылки автомобильных деталей **существуют в данных** (310 ссылок в 501
   рыхлом CFG, 11 световых ресурсов) и тоже **отбрасываются** в
   `part_runtime.cpp:add()` по той же причине.
3. Ни одна ссылка — ни объектная, ни автомобильная — не превращается в световой
   узел, не попадает в `g_lights` и не доходит до шейдеров.
4. `RenderRef.create` в runtime вызывает `apply_directional_light()`, который
   для Point/Spot выходит до регистрации. В наблюдавшихся сессиях туда не
   пришёл ни один Point/Spot-ресурс.

## L2.1. Инструментарий

### L2.1.1. Существующие механизмы

- Логи — `std::printf`/`fprintf(stderr, ...)` с префиксами `[rpk]`, `[part]`,
  `[scene]`, `[boot]`, `[section]`, `[refl]`, `[jvm]`. Отдельной подсистемы
  логов в проекте нет.
- Диагностические переключатели — переменные окружения `SLRR_*`
  (`src/boot/boot.cpp:252-264`, `src/render/bgfx_lib_backend.cpp:150-1912`),
  плюс CLI-флаги `--bench-*`, `--spawn-car`, `--probe*` (`src/main.cpp:120-240`).
  Нового механизма логирования не вводилось: добавлен один env-переключатель в
  том же стиле.

### L2.1.2. Добавленная трассировка

`SLRR_LIGHT_TRACE=1` — выключена по умолчанию (`gameref_light_trace()`,
`src/resources/gameref.cpp:1003`). Вызовы трассы стоят только вокруг печати и не
участвуют в решениях:

| Место | Что печатает |
|---|---|
| `gameref_register_light` (`gameref.cpp:1010`) | `type_id`, `live_id`, `set`, `has_direction` на каждую регистрацию |
| `apply_directional_light` (`natives_renderref.cpp:100`) | ранний выход для light-ресурса (`directional=0`) и успешную регистрацию с `live_id` и направлением |
| `part_runtime.cpp:437` `add()` | `render 0x… typeof=13 flags=0x…` → «нет модели, узел не создан» (только для световых ресурсов или ненулевых флагов) |
| `part_lamp_lit` (`part_runtime.cpp:1165`) | смену состояния лампы по детали (не каждый кадр): `flags`, `preview`, `lit` |
| `scene_instances_add_ground_props` (`scene_instances.cpp:2427`) | `render`-строку ICFG, указывающую на `typeof 13` и стёртую фильтром; непрочитанные строки `light …` |

Трасса печатает события: регистрация, отброс при создании узла, смена состояния
и разбор карты. Покадровых записей нет.

### L2.1.3. Headless-стенд

Рендерер для проверки разбора не нужен: `parse_part_config()`,
`RpkIndex::resolve()` и `RpkIndex::resolve_embedded_id()` — обычные функции без
JVM. Стенд (вне репозитория, `%TEMP%/slrrdec/l2`) компилирует
`part_config.cpp` + `rpk_index.cpp` из этого же дерева и вызывает их на данных
установки. Конверт `ground_prop_config()` и предикат `ground_prop_is_mesh()`
воспроизведены дословно из `scene_instances.cpp:2253-2270` / `:2360-2371`,
чтобы получить те же решения; сами парсер и индексатор — настоящие.

Это `[Ф-runtime]` для шагов «разбор и разрешение», но **не** для шагов
«реестр/`SceneInstance`/шейдер»: там нужен запуск игры (см. §L2.8).

## L2.2. L2-A: уличные объекты и ICFG

### L2.2.1. Точки наблюдения (код)

| Шаг | Функция | Строки |
|---|---|---|
| Загрузка ICFG | `ground_prop_config()` — проверяет `ICFG`, подтип `object`/`trlight`/`building`, отдаёт текст в парсер | `scene_instances.cpp:2253` |
| Разбор строк | `parse_part_config()`: `render` → `PartConfig::renders`, `flags` → `renders.back().flags` | `part_config.cpp:303-312`, `:332-333` |
| Строка `light` | команды `light` в парсере нет — попадает в `PartConfig::unknown` | `part_config.cpp:412-413` |
| Разрешение id | `RpkIndex::resolve_embedded_id()`: старшая половина — **слот зависимости** пакета, не host-`pack_id` | `rpk_index.cpp:444-460` |
| Фильтр геометрии | `is_visible()` → `ground_prop_is_mesh()`: только `typeof 14` или `typeof 5` | `scene_instances.cpp:2360` |
| Создание узлов | пропы запекаются как статические меши (`bake_static_mesh`), отдельных световых узлов нет | `scene_instances.cpp:216`, `:2406` |

### L2.2.2. Эксперименты по обязательным ресурсам

Стенд, `open_all=82`, `objects.rpk pack_id=68` (0x44).

| Поле | `lamp_base` | `lamp_road` | `kislampa` | `lamp_lane0_hold` |
|---|---|---|---|---|
| Сценарий / карта / машина | headless, пакет `objects.rpk`, тип `0x18e` / `0x6cd` / `0x18c` / `0x4ab` | | | |
| Исходный ресурс и ID | `objects.rpk:0x1c7` (Point, `street`) | тот же | тот же | тот же |
| Исходная строка | `render 0x000001c7 0.0 7.8 2.85` + `flags 4` | `… 0.0 7.8 1.85` | `… 0.0 4.3 0.0` | `… -1.15 7.8 0` |
| Функция-потребитель | `parse_part_config` → `PartRender{resource=0x004401c7, flags=0x4}` → `is_visible` | то же | то же | то же |
| Созданный runtime-объект | **нет** — запись стёрта до создания | нет | нет | нет |
| Регистрация в light registry | нет | нет | нет | нет |
| `SceneInstance` | нет | нет | нет | нет |
| Draw submission / shader | нет | нет | нет | нет |
| Итог | разобрано и отброшено | то же | то же | то же |
| Лог / строки | `scene_instances.cpp:2427-2447`, `:2360-2362` | | | |

| Поле | `lamp_a` | `lamp_b` | `lamp_c` | `pozna` |
|---|---|---|---|---|
| Исходный ресурс и ID | `objects.rpk:0x16` (`type fake point`, `street_fake_proba`) | тот же | тот же | тот же |
| Исходная строка | `render 0x00000016 0.0 5.48 -2.7` + `flags 4` | `… -1.7 5.48 -1` | `… 1.7 5.48 -1` | `… -0.5 11.95 0` |
| Функция-потребитель | `parse_part_config` → `PartRender{resource=0x00440016, flags=0x4}` → `is_visible` | то же | то же | то же |
| Созданный runtime-объект | **нет** | нет | нет | нет |
| Регистрация / `SceneInstance` / draw | нет | нет | нет | нет |
| Итог | разобрано и отброшено | то же | то же | то же |

| Поле | `trafficlamp` |
|---|---|
| Исходный ресурс и ID | `objects.rpk:0x1c6` (fakespot, `traffic`) |
| Исходная строка | `light 0x000001c6 1 0 0` |
| Функция-потребитель | нет: в `parse_part_config` команды `light` не существует |
| Созданный runtime-объект | **нет** |
| Регистрация / `SceneInstance` / draw | нет |
| Итог | **не подтверждено**: строка не разбирается, попадает в `PartConfig::unknown` |
| Лог / строки | стенд: `unknown-token light 0x000001c6 1 0 0`; `part_config.cpp:412-413` |

| Поле | Контроль: `objects.rpk:0x1cd` (Point, `testtrack`) |
|---|---|
| Ссылки в конфигах | не найдено ни одной (см. L1.1a и повторный скан L2) |
| Что делает host | ничего: нет строки — нет записи в `renders` |
| Итог | **не подтверждено** (ожидаемо) |

`[Ф-runtime]` Итог по §L2-A: **8 из 9** объектных ссылок проходят разбор и
стёрты фильтром `ground_prop_is_mesh` (световой ресурс `typeof 13` не является
мешем); **1** (`light 0x000001c6`) не разбирается вообще. Ни одна не доходит до
реестра.

### L2.2.3. Почему это не «загружено и отфильтровано позже»

`[Ф-код]` Фильтр стоит **до** любого создания узла:

```cpp
std::erase_if(renders, [&](const PartRender& render) {
  return render.variants.empty() ? !is_visible(render.resource)
      : std::none_of(...);
});
```

`is_visible` → `ground_prop_is_mesh` → `const auto* entry = rpk.resolve(resource);
if (!entry || (entry->typeof_ != 14 && entry->typeof_ != 5)) return false;`
(`scene_instances.cpp:2360-2362`). Для `typeof 13` возвращается `false`, запись
удаляется из `PartConfig::renders`, и в последующем запекании статических мешей
её уже нет. Заодно теряется и `flags 4`, потому что он принадлежал именно этой
записи.

## L2.3. Проверка `flags 4` (все восемь записей)

`[Ф-рес]` Проверены все восемь `render`, указывающих на световой ресурс в
`objects.rpk`. В каждом случае строка `flags 4` — следующая непустая строка
после `render`, и парсер присваивает её именно этой записи
(`renders.back().flags |= 4`).

| Объект | `render`-строка | Следующая строка | Флаги у записи |
|---|---|---|---|
| `lamp_base` (0x18e) | `render 0x000001c7 0.0 7.8 2.85` | `flags 4` | `0x4` |
| `lamp_road` (0x6cd) | `render 0x000001c7 0.0 7.8 1.85` | `flags 4` | `0x4` |
| `kislampa` (0x18c) | `render 0x000001c7 0.0 4.3 0.0` | `flags 4` | `0x4` |
| `lamp_lane0_hold` (0x4ab) | `render 0x000001c7 -1.15 7.8 0` | `flags 4` | `0x4` |
| `lamp_a` (0x18f) | `render 0x00000016 0.0 5.48 -2.7` | `flags 4` | `0x4` |
| `lamp_b` (0x191) | `render 0x00000016 -1.7 5.48 -1` | `flags 4` | `0x4` |
| `lamp_c` (0x192) | `render 0x00000016 1.7 5.48 -1` | `flags 4` | `0x4` |
| `pozna` (0x1cc) | `render 0x00000016 -0.5 11.95 0` | `flags 4` | `0x4` |

Исключений нет: последовательность универсальна для всех восьми. Утверждение
L1.1a подтверждено полностью.

`[Ф-код]` При этом `flags 4` в ICFG объекта и `PartConfig::renders[].flags`
(`light_flags`) — **одно и то же поле** в разборе (`part_config.cpp:332`), но
не одно и то же следствие: у деталей флаг доходит до `add(..., light_flags)` и
создаёт узел лампового меша, а у объектных пропов запись стёрта фильтром, и
флаг умирает вместе с ней.

## L2.4. L2-B: ссылки автомобильных деталей — они существуют

Это главная поправка к L1. Скан L1 смотрел **только текстовые payload'ы внутри
`.rpk`** и поэтому не видел рыхлых CFG. В установке детали читаются из рыхлых
файлов: `configuration(resource)` → `load_part_config()` открывает путь из
свойства `native` (`part_config.cpp:441-467`), а не payload пакета.

`[Ф-runtime]` Стенд: 501 рыхлый CFG в `*_data/scripts`, 1029 строк `render`,
**310 ссылок на световые ресурсы** (`typeof 13`), 11 уникальных ресурсов,
19 автомобильных пакетов.

| Ресурс | alias | Вид (L1) | Ссылок | Флаги |
|---|---|---|---:|---|
| `cars/racers.rpk:0x0008` | `headlights` | **spot** | 40 | `0x4` ×40 |
| `cars/racers.rpk:0x000d` | `headlights` | **spot** | **0** | — |
| `cars/racers.rpk:0x000f` | `taillights` | fakespot | 25 | `0x1` ×21, `0x10` ×2, `0x20` ×2 |
| `cars/racers.rpk:0x0011` | `taillights` | fakespot | 20 | `0x4` ×20 |
| `cars/racers.rpk:0x0013` | `taillights` | fakespot | 19 | `0x2` ×19 |
| `cars/racers.rpk:0x0026` | `headlights` | fakespot | 52 | `0x10` ×26, `0x20` ×26 |
| `particles.rpk:0x001f` | `nos_light` | fakespot | 18 | `0x100` ×18 |
| `particles.rpk:0x0020` | `red_light` | fakespot | 46 | `0x1` ×19, `0x2` ×1, `0x4` ×24, `0x5` ×2 |
| `particles.rpk:0x0021` | `white_light` | **spot** | 39 | `0x2` ×15, `0x4` ×24 |
| `particles.rpk:0x0022` | `yellow_light` | fakespot | 36 | `0x10` ×18, `0x20` ×18 |
| `parts.rpk:0xd362` | `headlights` | **spot** | 3 | `0x4` ×3 |
| `parts.rpk:0xd364` | `taillights` | fakespot | 12 | `0x4` ×12 |

Раскладка флагов совпадает с объявленной в `gameref.hpp:181`: `1` brake,
`2` reverse, `4` city, `0x10`/`0x20` — левый/правый указатель; `0x100`
встречается только у `nos_light` (в маску `0x13f` попадает, в `part_lamp_lit`
не проверяется).

`[Ф-runtime]` Пример исходной строки (`cars/racers/Prime_data/scripts/L_headlights.cfg`):

```
render		0x000000E4	0.000 0.000 0.000	0.000 0.000 0.000	; CITY
flags		4
render		0x00030008	0.000 0.000 -0.100	0.087 0.000 0.000	; CITY
flags		4
```

`0x00030008` — это `0x0003` (слот зависимости `cars/racers.rpk` в пакете машины)
+ локальный `0x0008`; `resolve_embedded_id()` даёт `0x00130008`, то есть
`cars/racers.rpk:0x0008` (`rpk_index.cpp:444-460`).

## L2.5. L2-B: что host делает с автомобильной ссылкой

`[Ф-код]` Цепочка:

1. `configuration(resource)` → `load_part_config()` читает рыхлый CFG.
2. `parse_part_config()` создаёт `PartRender{resource=0x00130008, flags=0x4}`.
3. `render_part()` третий цикл: `if (r.wheel >= 0 || (r.flags & 0x40u) ||
   (r.flags & 0x13fu) == 0) continue;` → вызывает
   `add(r.resource, …, r.flags)` (`part_runtime.cpp:499-503`).
4. `add()` требует модель: `index->model_for_resource(pack, local).ok`
   (`part_runtime.cpp:437-450`). Световой ресурс модели не имеет →
   **ранний `return`**.
5. Узел не создаётся: нет `RenderRef`, нет `SceneInstance` (`light_flags`
   существует только на созданном узле), нет регистрации света.

`[Ф-runtime]` Та же цепочка видна в логе host-сборки при сборке машины в
гараже (см. §L2.8):

```
[light-trace] part render 0x00130008 typeof=13 flags=0x4 -> no model, node not created
[light-trace] part render 0x00130026 typeof=13 flags=0x10 -> no model, node not created
[light-trace] part render 0x00130026 typeof=13 flags=0x20 -> no model, node not created
[light-trace] part render 0x00130011 typeof=13 flags=0x4 -> no model, node not created
[light-trace] part render 0x0013000f typeof=13 flags=0x1 -> no model, node not created
[light-trace] part render 0x00130013 typeof=13 flags=0x2 -> no model, node not created
```

| Поле | Результат |
|---|---|
| Сценарий / карта / машина | `--game --window --no-wait`, гаража: сборка `Prime` (в каталоге), без карты |
| Исходный ресурс и ID | `cars/racers.rpk:0x0008`, `0x000f`, `0x0011`, `0x0013`, `0x0026` |
| Исходная строка / событие | `render 0x00030008 … ` + `flags 4` в рыхлом CFG детали |
| Функция-потребитель | `parse_part_config` → `render_part` → `add()` |
| Созданный runtime-объект | **нет** (ранний `return` до `jvm.new_object`) |
| Регистрация в light registry | нет |
| `SceneInstance` | нет (узла нет, `light_flags` некуда положить) |
| Draw submission / shader | нет |
| Итог | **частично**: связь в данных есть, host её читает и отбрасывает |
| Лог / исходный код / строки | `part_runtime.cpp:437-455`, `:499-503`; `SLRR_LIGHT_TRACE=1` |

### L2.5.1. Что при этом работает

`[Ф-runtime]` Рядом с каждой световой строкой в CFG стоит **меш** того же
состояния (`…_CITY`, `…_BRAKE`, `…_REVERSING`, `…_INDICATOR`), и его
собственный `flags 4` создаёт узел с `light_flags=4` — он и скрывается/показывается
через `part_lamp_lit()`. То есть видимая часть лампы реализована, а источник
света — нет:

```
[part] create id=0x7000007c type=0x1100b9 class=…Prime_L_taillights slots=1 renders=5
[light-trace] lamp part=… flags=0x4 preview=0 lit=0 (first)
[light-trace] lamp part=… flags=0x100 preview=0 lit=0 (first)
```

Последняя строка — `nos_light` (`0x100`): `part_lamp_lit` проверяет только бит
`4`, поэтому лампа с флагом `0x100` не загорается никогда. Это наблюдение
подтверждает правило «только бит 4» и не является новым блокером.

## L2.6. `RenderRef.create` и `apply_directional_light` в runtime

`[Ф-runtime]` За наблюдавшиеся сессии `apply_directional_light()` вызван только
для directional-ресурса:

```
[light-trace] register key=… type_id=0x00390024 live_id=0x0 set=1 has_dir=0
[light-trace] register key=… type_id=0x00390024 live_id=0x70000025 set=1 has_dir=0
[light-trace] apply id=0x00390024 directional live_id=0x70000025 parent=1 dir=(1.000 -1.500 0.500)
[light-trace] apply id=0x00390024 directional live_id=0x70000078 parent=1 dir=(1.000 -1.500 0.500)
```

`0x00390024` — `maps/skydome.rpk:0x24` (`sun`), направление `(1, −1.5, 0.5)` из
файла. Регистрация происходит дважды (splash и меню), `live_id` растёт
(`0x70000025` → `0x70000078`), `has_dir=0` в момент регистрации и `has_direction`
выставляется после — это ровно тот порядок, что описан в §L1.2.

`[Ф-runtime]` Ни один Point/Spot-ресурс в `apply_directional_light()` не попал:
в трассе нет ни `0x00130008`, ни `0x004fd362`, ни `0x00450021`, ни
`objects.rpk:0x1c7`. Причина не в данных, а в том, что этот путь вызывается
только из `RenderRef.create`/`setLight` (`natives_renderref.cpp:253-276`,
`:532-552`), а скрипты для фар его не используют: их связь идёт через CFG.

## L2.7. Где цепочка обрывается

| Цепочка | Последний выполненный шаг | Точка обрыва | Следствие |
|---|---|---|---|
| Объект ICFG `render` → свет | `parse_part_config` создал `PartRender` с `flags` | `scene_instances.cpp:2427-2447` → `ground_prop_is_mesh` (`:2360`) | запись стёрта; ни узла, ни света |
| Объект ICFG `light` → свет | — | `part_config.cpp:412-413` (`unknown`) | строка не разбирается |
| Деталь CFG `render` → свет | `render_part` передал `light_flags` в `add()` | `part_runtime.cpp:437-455` (`model_for_resource` не ok) | узел не создан |
| `RenderRef.create` → свет | `apply_directional_light` прочитал payload | `natives_renderref.cpp:157-171` (`!directional`) | регистрации нет |
| `setLight(III)` → свет | `gameref_register_light` | `gameref.cpp:1023-1030` (`directional_light.set`) | узел в `g_lights`, но не в списке directional |
| Лампа: видимость меша | `part_lamp_lit` | — | работает (бит `4` + время + двигатель) |

## L2.8. Что запускалось, а что нет

### Запускалось

| Команда | Результат |
|---|---|
| `slrr_host.exe --probe` | не использовался (не грузит карту) |
| `slrr_host.exe L2Probe --game --window --no-wait` | доходит до `MainMenu`, собирает машину каталога, вызывает `part_lamp_lit` для ламповых мешей; завершается штатно |
| `SLRR_LIGHT_TRACE=1 slrr_host.exe L2Probe --game --window --no-wait` | та же сессия с трассой: 2 `apply`, 4 `register`, 32 отброшенных световых `render`, 8 переходов лампы |
| `--spawn-car cars/racers/Baiern.rpk:0x6` (и `Maz3_2005.rpk:0x6`), с `--bench-map ValoCity` и без | **падает** `0xC0000005` в `sections.change_active_section(sections.garage())` до входа в цикл; см. ниже |
| Headless-стенд `harness.exe` / `scan.exe` / `scan2.exe` | полный разбор 9 объектных ссылок и 501 автомобильного CFG |

### Не запускалось (и почему)

- **Карта (`ValoCity`, город) и поездка**: недостижимы — падение на входе в
  гараж блокирует и `--bench-map`, и `--auto-cmd street`. Воспроизводится
  детерминированно на текущем HEAD с обоими автомобилями, без трассы и с ней:

  ```
  [boot] Init actualState=java.game.MainMenu
  [jvm] invoke miss java.game.MainMenu.removeAllTimers ()V
  [crash] code=0xC0000005
  ```

  Это не связано со светом: падение происходит до первой карты и до любого
  светового ресурса. Дополнительно `--auto-new` упирается в
  `java.render.osd.dialog.StringRequesterDialog` и не доходит до гаража.
  Поэтому состояния «двигатель включён», «тормоз», «задний ход», «указатели» на
  живой машине **не наблюдались** — они остаются `[?]` (см. §L2.10).

- **`SceneInstance`/шейдеры для светового ресурса**: наблюдать нечего, потому
  что до них ничего не доходит (это результат, а не пробел).

## L2.9. Поправки к L1

`[Ф-runtime]` Три утверждения L1/L1.1a оказались неверными и исправлены в
`LIGHTING_AUDIT.md`:

1. **«Ни один Spot-ресурс не упомянут в конфигах»** — неверно. В рыхлых CFG
   деталей 310 ссылок на 11 световых ресурсов, включая три Spot:
   `cars/racers.rpk:0x8` (40), `parts.rpk:0xd362` (3),
   `particles.rpk:0x21` (39). L1 сканировал только payload'ы `.rpk` и не видел
   рыхлых файлов.
2. **`particles.rpk:0x21` (`white_light`) не имеет автомобильного контекста** —
   неверно: 39 ссылок, флаги `0x2` (reverse) и `0x4` (city).
3. **Четвёртый Spot `cars/racers.rpk:0xd`** — действительно не упоминается ни в
   одном конфиге; здесь L1 был прав.

Ссылки объектов (`objects.rpk`, 9 штук) L1 описал верно.

## L2.10. Что осталось

`[Г]` Совместимые интерпретации:

- Световые `render`-строки в CFG — это «источник света для этой лампы»:
  позиция совпадает с лампой (`0.0 7.8 2.85` у фонаря, `0.087 0.0 0.0` сдвиг
  фары), флаги совпадают с состоянием меша. Что оригинальный движок создавал по
  ним Point/Spot — правдоподобно, но не доказано.
- `0x00030008` в CFG — слот зависимости, а не host-`pack_id`; это верно для
  любых `0x0000xxxx`-id в CFG (`rpk_index.cpp:444-460`).

`[?]` Неизвестно:

- Как оригинал превращал такую строку в источник (создавал узел типа 0x8/0xd
  или использовал ресурс напрямую).
- Что делает `flags 0x100` (`nos_light`) в оригинале.
- Ведёт ли себя `part_lamp_lit` в оригинале так же (бит `4` + время + двигатель).
- Состояния лампы на живой машине (двигатель, тормоз, задний ход, указатели) —
  требуют запуска карты, который в этой среде заблокирован падением гаража.

## L2.11. Как повторить

1. Собрать host: `cmake --build build --config Release --target slrr_host`.
2. Включить трассу: `SLRR_LIGHT_TRACE=1`.
3. Запустить игру так, чтобы дошло до карты (нужен рабочий вход в гараж):
   `slrr_host.exe Player --game --window --bench-map ValoCity --bench-drive 40 --bench-time 22`.
4. Искать в выводе `[light-trace]`: `apply` (кто вызвал `RenderRef.create`),
   `part render … typeof=13` (отброшенные световые строки CFG),
   `prop … erased` (объектные ICFG-ссылки), `lamp part=` (переходы состояния).
5. Выключить трассу: без переменной окружения печати нет вообще.

Стенд для разбора без рендерера (если нужно перепроверить данные) собирается из
`part_config.cpp` + `rpk_index.cpp` и `include/`; он не входит в репозиторий.
