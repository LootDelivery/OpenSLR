#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""
============================================================================
 SLRR Native Fonts - universal hook patcher (icon fix + anti-crash)
============================================================================
Применяет хук «нативных шрифтов» к exe игры Street Legal Racing: Redline
(мод native fonts v4.x gate и пересборки того же мода).
Работает БЕЗ pip-зависимостей: чистый Python 3 + stdlib.

Все точки патча ищутся ПО БАЙТОВЫМ СИГНАТУРАМ, а не по фиксированным
адресам, поэтому подходят и для изменённых exe той же игры: патчер сам
находит классификатор шрифтов, таблицу маршрутизации, fallback и свободное
место под пещеры.

Правки:
  [1] ANTI-CRASH : восстановление потерянного стаба в fallback-классификаторе
                   (je вёл в нули -> падение; теперь там xor eax,eax; ret)
  [2] ICON FIX   : шрифты с классом глифов 0 (иконки схемы кузова) больше
                   не уходят в TTF-бэкенд - рисуются штатным рендером игры.
                   Текст (класс 1/2) продолжает идти через TTF без изменений.
  [3] HOUSEKEEP  : VirtualSize секции кода хука (если пещеры за ним) и
                   пересчёт PE-контрольной суммы.

Использование:
  python slrr_font_hook.py <game.exe>           # пропатчить (с бэкапом)
  python slrr_font_hook.py <game.exe> --check   # только анализ, без записи
  python slrr_font_hook.py <game.exe> --out F   # записать в другой файл
  python slrr_font_hook.py <game.exe> --revert  # откат из <exe>.slrr-hook.bak

Подробности - в README.md рядом с этим скриптом.
"""
import struct, sys, os, hashlib, argparse

HOOK_VERSION = "v4.3"

# ---------------------------------------------------------------------------
# Сигнатуры. Формат: hex-строка, '??' = любой байт.
# Описывают КОД мода, а не данные, поэтому выживают при пересборке мода
# с другими адресами.
# ---------------------------------------------------------------------------

# Голова классификатора шрифтов:
#   xor eax,eax; add ecx,-7; cmp ecx,0x26; ja <fallback>;
#   push edi; push esi; mov esi,1; jmp dword [ecx*4 + <jump table>]
SIG_HEAD = ("31 C0 83 C1 F9 83 F9 26 0F 87 ?? ?? ?? ??"
            " 57 56 BE 01 00 00 00 FF 24 8D ?? ?? ?? ??")

# Кейс px=15 в jump-таблице классификатора (12 байт):
#   mov edi,8; mov esi,3; jmp <эпилог с записью out>
SIG_PX15 = "BF 08 00 00 00 BE 03 00 00 00 EB ??"
# наш перенаправитель: E9 rel32 + NOP*7, сразу за ним - кейс px=20 (mov edi,6).
# Контекст нужен, чтобы не ловить обычные jmp с nop-паддингом в коде мода.
SIG_PX15_HOOKED = "E9 ?? ?? ?? ?? 90 90 90 90 90 90 90 BF 06 00 00 00"

# Ссылка на таблицу маршрутизации в коде сканирования классификатора:
#   mov edx,<table>; mov edi,<2*entries>
SIG_TABLE = "BA ?? ?? ?? ?? BF ?? ?? ?? ??"

# Тело fallback-классификатора (36 байт; ?? = disp8 у je):
#   mov ecx,[esp+4]; cmp [ecx+0x20],0; je <crash site>;
#   mov eax,[esp+8]; mov [eax],3; mov [eax+4],8; mov eax,1; ret;
#   xor eax,eax; ret
SIG_FALLBACK = ("8B 4C 24 04 83 79 20 00 74 ?? 8B 44 24 08"
                " C7 00 03 00 00 00 C7 40 04 08 00 00 00"
                " B8 01 00 00 00 C3 31 C0 C3")

# Эпилог классификатора (общий pop/test/ret для всех кейсов):
SIG_EPILOGUE = "5E 5F 85 C0 74 01 C3"

STRIDE = 0x38          # размер записи таблицы маршрутизации (захардкожен в моде)
CAVE2_GAP = 0x60       # расстояние между пещерами (cave1 ~0x4B байт)
OUT_SLOT = 3           # out[0]: TTF-слот для px=15 (как в оригинале)
OUT_PROFILE = 8        # out[4]: TTF-профиль для px=15 (как в оригинале)


def parse_sig(s):
    out = []
    for tok in s.split():
        out.append(None if tok == "??" else int(tok, 16))
    return out


def find_all(data, sig, start=0, end=None):
    """Все вхождения сигнатуры (None = любой байт)."""
    if end is None:
        end = len(data)
    n = len(sig)
    first = next(i for i, v in enumerate(sig) if v is not None)
    first_b = sig[first]
    hits = []
    for i in range(max(start, 0), end - n + 1):
        if data[i + first] != first_b:
            continue
        for j in range(first + 1, n):
            if sig[j] is not None and data[i + j] != sig[j]:
                break
        else:
            hits.append(i)
    return hits


def matches(data, off, sig):
    """Точное совпадение сигнатуры в позиции off (None = любой байт)."""
    return all(v is None or data[off + j] == v for j, v in enumerate(sig))


def fmt_bytes(bs):
    return " ".join("%02X" % c for c in bs)


def set_rel8(buf, je_off, target_off):
    """disp8 для короткого перехода: target = je_off + 2 + disp."""
    d = target_off - (je_off + 2)
    assert -128 <= d <= 127, "rel8 overflow: %d" % d
    buf[je_off + 1] = d & 0xFF


def set_rel32(buf, at, src_va, tgt_va):
    """disp32 для E9/E0F87 в позиции at (сразу после опкода), src_va - VA опкода."""
    insn_len = 5 if buf[at - 1] == 0xE9 else 6
    struct.pack_into("<i", buf, at, tgt_va - (src_va + insn_len))


# ===========================================================================
# Мини-PE-парсер (чистый stdlib)
# ===========================================================================
class PE(object):
    def __init__(self, buf):
        self.buf = bytearray(buf)
        b = self.buf
        if len(b) < 0x40 or b[:2] != b"MZ":
            raise ValueError("это не PE-файл (нет MZ-заголовка)")
        e_lfanew = struct.unpack_from("<I", b, 0x3C)[0]
        if b[e_lfanew:e_lfanew + 4] != b"PE\x00\x00":
            raise ValueError("это не PE-файл (нет PE-сигнатуры)")
        coff = e_lfanew + 4
        self.machine, nsec = struct.unpack_from("<HH", b, coff)
        size_opt = struct.unpack_from("<H", b, coff + 16)[0]
        opt = coff + 20
        magic = struct.unpack_from("<H", b, opt)[0]
        if magic != 0x10B:
            raise ValueError("нужен 32-битный PE32 (magic 0x10B), а не 0x%X" % magic)
        self.image_base = struct.unpack_from("<I", b, opt + 28)[0]
        self.size_of_image = struct.unpack_from("<I", b, opt + 56)[0]
        self.checksum_off = opt + 64
        self.sections = []
        sh = opt + size_opt
        for i in range(nsec):
            o = sh + 40 * i
            name = bytes(b[o:o + 8]).rstrip(b"\x00").decode("ascii", "replace")
            vs, va, rawsz, rawptr = struct.unpack_from("<IIII", b, o + 8)
            self.sections.append(dict(name=name, vs=vs, va=va, rawsz=rawsz,
                                      rawptr=rawptr, hdr_off=o))

    def sec_by_name(self, name):
        for s in self.sections:
            if s["name"] == name:
                return s
        return None

    def fix_checksum(self):
        """PE-контрольная сумма по спецификации: поле обнуляется, свёртка
        суммы 16-битных слов с переносом, + размер файла."""
        b = self.buf
        struct.pack_into("<I", b, self.checksum_off, 0)
        n = len(b)
        total = 0
        for i in range(n // 2):
            total += b[2 * i] | (b[2 * i + 1] << 8)
            total = (total & 0xFFFF) + (total >> 16)
        if n & 1:
            total += b[-1]
            total = (total & 0xFFFF) + (total >> 16)
        total = (total & 0xFFFF) + (total >> 16)
        ck = (total + n) & 0xFFFFFFFF
        struct.pack_into("<I", b, self.checksum_off, ck)
        return ck


# ===========================================================================
# Генерация кода пещер
# ===========================================================================
def scan_loop(code, entries, table_va, pre_pushed):
    """Общий скан таблицы маршрутизации (как в оригинальном классификаторе).
    pre_pushed=False: в стеке [esp+0xC] descriptor (после наших push edi/esi)
    pre_pushed=True : в стеке [esp+0xC] descriptor (после push'ей классификатора).
    Возвращает (код_скана, метки)."""
    c = code
    if not pre_pushed:
        c += b"\x57\x56"                                # push edi; push esi (наши)
    c += b"\x8b\x4c\x24\x0c"                            # mov ecx,[esp+0xC] ; descriptor
    c += b"\xba" + struct.pack("<I", table_va)          # mov edx,<table>
    c += b"\xbe" + struct.pack("<I", entries)           # mov esi,<entries>
    scan = len(c)
    c += b"\x83\x7a\xf0\x00"                            # cmp dword [edx-0x10],0
    j_n = len(c); c += b"\x74\x00"                      # je n
    c += b"\x39\x4a\xe4"                                # cmp [edx-0x1C],ecx
    j_f1 = len(c); c += b"\x74\x00"                     # je found_a
    n_lbl = len(c)
    c += b"\x83\x7a\x0c\x00"                            # cmp dword [edx+0xC],0
    j_adv = len(c); c += b"\x74\x00"                    # je adv
    c += b"\x39\x0a"                                    # cmp [edx],ecx
    j_f2 = len(c); c += b"\x74\x00"                     # je found_b
    adv = len(c)
    c += b"\x83\xc2\x38"                                # add edx,0x38
    c += b"\x4e"                                        # dec esi
    j_scan = len(c); c += b"\x75\x00"                   # jne scan
    return dict(code=c, scan=scan, j_n=j_n, j_f1=j_f1, n=n_lbl, j_adv=j_adv,
                j_f2=j_f2, adv=adv, j_scan=j_scan)


def build_cave1(table_va, fallback_va, entries):
    """ПЕЩЕРА 1 - вход из range-fail (ja), ДО push'ей классификатора.
    Стек при входе: [esp]=ret, [esp+4]=descriptor, [esp+8]=out.
      класс 0   -> xor eax,eax; ret  (стоковый рендер игры)
      класс!=0  -> jmp fallback     (оригинальное поведение мода)
      не найдена-> jmp fallback     (оригинальное поведение мода)"""
    m = scan_loop(bytearray(), entries, table_va, pre_pushed=False)
    c = m["code"]
    set_rel8(c, m["j_n"], m["n"])
    set_rel8(c, m["j_adv"], m["adv"])
    set_rel8(c, m["j_scan"], m["scan"])
    c += b"\x5e\x5f"                                    # pop esi; pop edi (наши)
    jmp_nf = len(c) + 1
    c += b"\xe9\x00\x00\x00\x00"                        # jmp fallback (не найдена)
    found_a = len(c)
    c += b"\x83\xea\x1c"                                # sub edx,0x1C -> entry
    found_b = len(c)
    c += b"\x8b\x4a\x08"                                # mov ecx,[edx+8] ; class
    c += b"\x5e\x5f"                                    # pop esi; pop edi (наши)
    c += b"\x85\xc9"                                    # test ecx,ecx
    j_stock = len(c); c += b"\x74\x00"                  # jz stock
    jmp_cl = len(c) + 1
    c += b"\xe9\x00\x00\x00\x00"                        # jmp fallback (класс!=0)
    stock = len(c)
    c += b"\x31\xc0\xc3"                                # xor eax,eax; ret
    # back-patch
    set_rel8(c, m["j_f1"], found_a)
    set_rel8(c, m["j_f2"], found_b)
    set_rel8(c, j_stock, stock)
    return c, (jmp_nf, jmp_cl)


def build_cave2(table_va, entries, out_slot, out_profile):
    """ПЕЩЕРА 2 - вход из кейса px=15, ПОСЛЕ push'ей классификатора.
    Стек при входе: [esp+0xC]=descriptor, [esp+0x10]=out.
      класс 0   -> pop'ы классификатора; xor eax,eax; ret (сток)
      класс!=0  -> записать out={slot,profile} ДЕТЕРМИНИРОВАННО (как
                   оригинальный эпилог классификатора), pop'ы, ret с eax=1
      не найдена-> как оригинал: out={slot,profile}, return 1"""
    m = scan_loop(bytearray(), entries, table_va, pre_pushed=True)
    c = m["code"]
    set_rel8(c, m["j_n"], m["n"])
    set_rel8(c, m["j_adv"], m["adv"])
    set_rel8(c, m["j_scan"], m["scan"])
    jmp_ttf = len(c)
    c += b"\xeb\x00"                                    # jmp ttf (не найдена -> как оригинал)
    found_a = len(c)
    c += b"\x83\xea\x1c"                                # sub edx,0x1C -> entry
    found_b = len(c)
    c += b"\x8b\x4a\x08"                                # mov ecx,[edx+8] ; class
    c += b"\x85\xc9"                                    # test ecx,ecx
    j_stock = len(c); c += b"\x74\x00"                  # jz stock
    ttf = len(c)
    c += b"\xbe" + struct.pack("<I", out_slot)          # mov esi,<slot>
    c += b"\xbf" + struct.pack("<I", out_profile)       # mov edi,<profile>
    c += b"\x8b\x44\x24\x10"                            # mov eax,[esp+0x10] ; out
    c += b"\x89\x30"                                    # mov [eax],esi
    c += b"\x89\x78\x04"                                # mov [eax+4],edi
    c += b"\xb8\x01\x00\x00\x00"                        # mov eax,1
    c += b"\x5e\x5f"                                    # pop esi; pop edi (классификатора)
    c += b"\xc3"                                        # ret
    stock = len(c)
    c += b"\x5e\x5f\x31\xc0\xc3"                        # pop esi; pop edi; xor eax,eax; ret
    set_rel8(c, m["j_f1"], found_a)
    set_rel8(c, m["j_f2"], found_b)
    set_rel8(c, j_stock, stock)
    set_rel8(c, jmp_ttf, ttf)
    return c


# ===========================================================================
# Анализ exe
# ===========================================================================
def analyze(path):
    pe = PE(open(path, "rb").read())
    info = {"path": path, "size": len(pe.buf),
            "sha256": hashlib.sha256(bytes(pe.buf)).hexdigest(),
            "image_base": pe.image_base, "size_of_image": pe.size_of_image,
            "warnings": [], "errors": []}

    # --- секция с кодом мода ---
    sec = pe.sec_by_name(".ttf")
    if sec is not None:
        raw = bytes(pe.buf[sec["rawptr"]:sec["rawptr"] + sec["rawsz"]])
        if not find_all(raw, parse_sig(SIG_HEAD)):
            info["warnings"].append("в секции '.ttf' нет сигнатуры классификатора - ищем по всем секциям")
            sec = None
    if sec is None:
        for s in pe.sections:
            r = bytes(pe.buf[s["rawptr"]:s["rawptr"] + s["rawsz"]])
            if find_all(r, parse_sig(SIG_HEAD)):
                sec = s
                info["warnings"].append("секция '.ttf' не подошла, код мода найден в секции '%s'" % s["name"])
                break
    if sec is None:
        info["errors"].append("не найдена секция с кодом мода (сигнатура классификатора шрифтов) - "
                              "проверьте, что это exe мода native fonts")
        return pe, None, info

    sec_va = pe.image_base + sec["va"]
    raw = bytes(pe.buf[sec["rawptr"]:sec["rawptr"] + sec["rawsz"]])
    info.update(section=sec, section_va=sec_va, raw=raw, virtualsize=sec["vs"])

    # --- классификатор + адрес fallback из ja ---
    heads = find_all(raw, parse_sig(SIG_HEAD))
    if len(heads) != 1:
        info["errors"].append("сигнатура классификатора: %d вхождений (нужно ровно 1)" % len(heads))
        return pe, sec, info
    head_off = heads[0]
    site_a_off = head_off + 8                       # 0F 87 rel32
    site_a_va = sec_va + site_a_off
    fallback_va = site_a_va + 6 + struct.unpack_from("<i", raw, site_a_off + 2)[0]
    info.update(classify_va=sec_va + head_off, site_a_va=site_a_va, fallback_va=fallback_va)

    # --- кейс px=15 ---
    px = find_all(raw, parse_sig(SIG_PX15))
    if len(px) != 1:
        hooked = find_all(raw, parse_sig(SIG_PX15_HOOKED))
        if len(hooked) == 1:
            info["errors"].append("exe уже пропатчен этим хуком (кейс px=15 перенаправлен в пещеру). "
                                  "Повторная установка не нужна; для перепатчивания возьмите исходный exe.")
            return pe, sec, info
        info["errors"].append("сигнатура кейса px=15: %d вхождений (нужно ровно 1)" % len(px))
        return pe, sec, info
    site_b_off = px[0]
    info["site_b_va"] = sec_va + site_b_off

    # --- таблица маршрутизации ---
    table_va = entries = None
    for t in find_all(raw, parse_sig(SIG_TABLE), head_off, head_off + 0x80):
        cand_va = struct.unpack_from("<I", raw, t + 1)[0]     # imm32 у mov edx (t+1..t+4)
        cand_cnt = struct.unpack_from("<I", raw, t + 6)[0]    # imm32 у mov edi (BF на t+5)
        cnt = cand_cnt // 2
        if sec_va <= cand_va < sec_va + sec["rawsz"] and 1 <= cnt <= 64:
            table_va, entries = cand_va, cnt
            break
    if table_va is None:
        info["errors"].append("рядом с классификатором не найдена ссылка на таблицу маршрутизации "
                              "(mov edx,<table>; mov edi,<2*entries>)")
        return pe, sec, info
    info.update(table_va=table_va, table_entries=entries)

    # --- fallback: тело + crash-сайт (anti-crash правка) ---
    fb_off = fallback_va - sec_va
    fb_ok = 0 <= fb_off and fb_off + len(parse_sig(SIG_FALLBACK)) <= len(raw) \
        and matches(raw, fb_off, parse_sig(SIG_FALLBACK))
    info["fallback_ok"] = fb_ok
    if not fb_ok:
        info["crash_state"] = "unknown"
        info["crash_va"] = None
        info["warnings"].append("тело fallback-классификатора не совпало с сигнатурой - "
                                "anti-crash правка будет пропущена (иконный фикс применён будет)")
    else:
        crash_off = fb_off + 10 + raw[fb_off + 9]   # je rel8 -> crash site
        crash_va = sec_va + crash_off
        info["crash_va"] = crash_va
        tgt3 = raw[crash_off:crash_off + 3]
        if tgt3 == b"\x00\x00\x00":
            info["crash_state"] = "needed"
        elif tgt3 == b"\x31\xc0\xc3":
            info["crash_state"] = "already"
        else:
            info["crash_state"] = "occupied"
            info["warnings"].append("на месте crash-сайта 0x%08X стоит посторонний код (%s) - "
                                    "anti-crash правка пропущена" % (crash_va, fmt_bytes(tgt3)))

    # --- эпилог (информационно) ---
    ep_off = fb_off - 7
    info["epilogue_ok"] = 0 <= ep_off and matches(raw, ep_off, parse_sig(SIG_EPILOGUE))

    # --- пещера: нулевой хвост секции, не пересекающийся с данными, на которые есть ссылки ---
    lnz = max((i for i, b in enumerate(raw) if b), default=-1)
    cave_min = ((lnz + 1 + 15) // 16) * 16 if lnz >= 0 else 0
    ref_vals = set()
    for i in range(len(raw) - 3):
        v = struct.unpack_from("<I", raw, i)[0]
        if sec_va <= v < sec_va + sec["rawsz"]:
            ref_vals.add(v)
    info["max_ref_va"] = (max(ref_vals) if ref_vals else sec_va)
    need = CAVE2_GAP + 0x70                          # cave1 (~0x4B) + gap + cave2 (~0x5B)
    cave1_off = None
    cand = cave_min
    while cand + need <= len(raw):
        lo = sec_va + max(0, cand - 0x100)
        hi = sec_va + cand + need + 0x100
        if not any(lo <= v < hi for v in ref_vals) and not any(raw[cand:cand + need]):
            cave1_off = cand
            break
        cand += 0x10
    if cave1_off is None:
        info["errors"].append("в секции '%s' нет свободного нулевого места под пещеры (нужно %d байт)"
                              % (sec["name"], need))
        return pe, sec, info
    cave1_va = sec_va + cave1_off
    info.update(cave1_va=cave1_va, cave2_va=cave1_va + CAVE2_GAP, cave_need=need)

    # --- VirtualSize ---
    info["vs_bump_needed"] = info["cave2_va"] + 0x70 > sec_va + sec["vs"]
    if info["vs_bump_needed"]:
        sec_end_rva = sec["va"] + sec["rawsz"]
        if (sec_end_rva + 0xFFF) // 0x1000 * 0x1000 > pe.size_of_image:
            info["errors"].append("SizeOfImage (0x%X) не покрывает расширение секции до SizeOfRawData - "
                                  "патч невозможен для этой сборки" % pe.size_of_image)
    return pe, sec, info


# ===========================================================================
# Патч
# ===========================================================================
def do_patch(pe, info, report):
    b = pe.buf
    sec = info["section"]
    sec_va = info["section_va"]
    rawptr = sec["rawptr"]

    def wr(va, bs):
        o = rawptr + (va - sec_va)
        b[o:o + len(bs)] = bs

    def rd(va, n):
        o = rawptr + (va - sec_va)
        return bytes(b[o:o + n])

    site_a_va, site_b_va = info["site_a_va"], info["site_b_va"]
    fallback_va = info["fallback_va"]
    cave1_va, cave2_va = info["cave1_va"], info["cave2_va"]
    table_va, entries = info["table_va"], info["table_entries"]

    # [1] anti-crash
    if info["crash_state"] == "needed":
        old = rd(info["crash_va"], 3)
        wr(info["crash_va"], b"\x31\xc0\xc3")
        report.append("[1] ANTI-CRASH 0x%08X: %s -> 31 C0 C3 (xor eax,eax; ret вместо нулей)"
                      % (info["crash_va"], fmt_bytes(old)))
    elif info["crash_state"] == "already":
        report.append("[1] ANTI-CRASH 0x%08X: стаб уже стоит, пропуск" % info["crash_va"])
    else:
        report.append("[1] ANTI-CRASH: пропуск (%s)" % info["crash_state"])

    # [2] пещеры
    c1, c1_jmps = build_cave1(table_va, fallback_va, entries)
    c2 = build_cave2(table_va, entries, OUT_SLOT, OUT_PROFILE)
    for jpos in c1_jmps:
        set_rel32(c1, jpos, cave1_va + jpos - 1, fallback_va)
    assert len(c1) <= CAVE2_GAP and CAVE2_GAP + len(c2) <= info["cave_need"]
    wr(cave1_va, bytes(c1))
    wr(cave2_va, bytes(c2))
    report.append("[2] CAVE1 0x%08X (%2d байт): класс глифов 0 -> return 0 (сток); иначе -> fallback 0x%08X"
                  % (cave1_va, len(c1), fallback_va))
    report.append("    CAVE2 0x%08X (%2d байт): класс 0 -> return 0 (сток); класс!=0 -> out={%d,%d} + return 1"
                  % (cave2_va, len(c2), OUT_SLOT, OUT_PROFILE))

    # [3] перенаправления
    old_a = rd(site_a_va, 6)
    b_a = bytearray(b"\x0f\x87\x00\x00\x00\x00")
    struct.pack_into("<i", b_a, 2, cave1_va - (site_a_va + 6))
    wr(site_a_va, bytes(b_a))
    report.append("[3] SITE-A 0x%08X (range-fail ja): %s -> 0F 87 -> 0x%08X"
                  % (site_a_va, fmt_bytes(old_a), cave1_va))
    old_b = rd(site_b_va, 12)
    b_b = bytearray(b"\xe9\x00\x00\x00\x00" + b"\x90" * 7)
    struct.pack_into("<i", b_b, 1, cave2_va - (site_b_va + 5))
    wr(site_b_va, bytes(b_b))
    report.append("[4] SITE-B 0x%08X (кейс px=15): %s -> E9 -> 0x%08X + NOP*7"
                  % (site_b_va, fmt_bytes(old_b), cave2_va))

    # [5] VirtualSize
    if info["vs_bump_needed"]:
        o = sec["hdr_off"] + 8
        old_vs = struct.unpack_from("<I", b, o)[0]
        struct.pack_into("<I", b, o, sec["rawsz"])
        report.append("[5] VirtualSize '%s': 0x%X -> 0x%X (пещеры выходили за виртуальный размер)"
                      % (sec["name"], old_vs, sec["rawsz"]))
    else:
        report.append("[5] VirtualSize без изменений (пещеры внутри 0x%X)" % sec["vs"])

    # [6] checksum
    ck = pe.fix_checksum()
    report.append("[6] PE checksum: 0x%08X" % ck)


# ===========================================================================
# CLI
# ===========================================================================
def main():
    ap = argparse.ArgumentParser(
        description="SLRR native fonts hook patcher %s (icon fix + anti-crash)" % HOOK_VERSION)
    ap.add_argument("exe", help="exe игры (сборка мода native fonts)")
    ap.add_argument("--check", action="store_true", help="только анализ, без записи")
    ap.add_argument("--out", help="записать результат в другой файл (по умолчанию поверх исходного)")
    ap.add_argument("--no-backup", action="store_true", help="не создавать бэкап при записи поверх исходного")
    ap.add_argument("--revert", action="store_true", help="восстановить exe из бэкапа <exe>.slrr-hook.bak")
    ap.add_argument("--no-report", action="store_true", help="не сохранять <exe>.HookReport.txt")
    args = ap.parse_args()

    path = os.path.abspath(args.exe)
    if args.revert:
        bak = path + ".slrr-hook.bak"
        if not os.path.exists(bak):
            print("ОШИБКА: бэкап не найден: %s" % bak)
            return 2
        data = open(bak, "rb").read()
        open(path, "wb").write(data)
        print("Восстановлено из бэкапа: %s -> %s (%d байт)" % (bak, path, len(data)))
        return 0

    try:
        pe, sec, info = analyze(path)
    except ValueError as e:
        print("ОШИБКА: %s" % e)
        return 2

    rep = []
    rep.append("SLRR native fonts hook patcher %s" % HOOK_VERSION)
    rep.append("=" * 74)
    rep.append("Файл          : %s" % info["path"])
    rep.append("Размер        : %d байт" % info["size"])
    rep.append("sha256 (исх.) : %s" % info["sha256"])
    rep.append("ImageBase 0x%08X, SizeOfImage 0x%08X" % (info["image_base"], info["size_of_image"]))
    if sec is not None:
        rep.append("Секция мода   : '%s' VA=0x%08X VirtualSize=0x%X RawSize=0x%X"
                   % (sec["name"], info["section_va"], info["virtualsize"], sec["rawsz"]))
    for w in info.get("warnings", []):
        rep.append("ВНИМАНИЕ: " + w)

    if info.get("errors"):
        rep.append("-" * 74)
        for e in info["errors"]:
            rep.append("ОШИБКА: " + e)
        print("\n".join(rep))
        print("\nПатч НЕ применён.")
        return 2

    rep.append("-" * 74)
    rep.append("Найденные точки (все - по сигнатурам, адреса могут отличаться от исходной сборки):")
    rep.append("  классификатор шрифтов : 0x%08X" % info["classify_va"])
    rep.append("  SITE-A (range-fail)   : 0x%08X" % info["site_a_va"])
    rep.append("  SITE-B (кейс px=15)   : 0x%08X" % info["site_b_va"])
    rep.append("  fallback-классификатор: 0x%08X (тело: %s, эпилог: %s)"
               % (info["fallback_va"], "OK" if info["fallback_ok"] else "НЕ СОВПАЛ",
                  "OK" if info["epilogue_ok"] else "НЕ СОВПАЛ"))
    rep.append("  crash-сайт            : %s (состояние: %s)"
               % ("0x%08X" % info["crash_va"] if info["crash_va"] else "-", info["crash_state"]))
    rep.append("  таблица маршрутизации : 0x%08X, %d записей" % (info["table_va"], info["table_entries"]))
    rep.append("  пещеры                : cave1=0x%08X, cave2=0x%08X" % (info["cave1_va"], info["cave2_va"]))

    if args.check:
        rep.append("-" * 74)
        rep.append("РЕЖИМ --check: ничего не записано. Хук применим.")
        print("\n".join(rep))
        return 0

    rep.append("-" * 74)
    do_patch(pe, info, rep)

    out = os.path.abspath(args.out) if args.out else path
    if out == path and not args.no_backup:
        bak = path + ".slrr-hook.bak"
        open(bak, "wb").write(open(path, "rb").read())
        rep.append("Бэкап         : %s" % bak)
    open(out, "wb").write(bytes(pe.buf))
    rep.append("Записано      : %s" % out)
    rep.append("sha256 (новый): %s" % hashlib.sha256(bytes(pe.buf)).hexdigest())
    rep.append("")
    rep.append("Проверка в игре: схема кузова раскладывается по деталям; текст и остальное - без изменений.")
    rep.append("Откат: python slrr_font_hook.py <game.exe> --revert")
    print("\n".join(rep))

    if not args.no_report:
        rp = out + ".HookReport.txt"
        open(rp, "w", encoding="utf-8").write("\n".join(rep) + "\n")
        print("(отчёт: %s)" % rp)
    return 0


if __name__ == "__main__":
    sys.exit(main())
