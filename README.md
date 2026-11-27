# Inventory Tweaks Remaked — NeoForge 26.3

A faithful remake of the classic **Inventory Tweaks** for Minecraft 26.3 on NeoForge.
Chest and inventory sorting, container buttons, click shortcuts, auto-refill and fully editable sorting rules — rebuilt for modern Minecraft.

![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)
![Loader](https://img.shields.io/badge/Loader-NeoForge-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## Features

- **R-key sorting that follows the cursor** — sorting works without opening the inventory; where the pointer rests decides whether the chest, the inventory or both are sorted.
- **Middle-click sorting** with three algorithms (default / vertical / horizontal); on crafting slots it splits stacks evenly instead of sorting.
- **Container buttons** — default sort, sort by column, sort by row and the settings button, drawn with the original pixel art.
- **Item transfer buttons** — move matching items into the container or pull the whole container back with `Shift`.
- **Click shortcuts** — `Ctrl` moves one item, `Ctrl+Shift` all of the same type, `Shift` a stack, `Space` everything, `Alt` drops, `1–9` sends to a hotbar slot, `↑`/`↓` move between halves.
- **Auto-refill** — tools and stacks are replaced automatically when they run out or are about to break.
- **Editable rules and item tree** — define what goes where, lock or freeze slots, and keep several profiles that can be switched in game.
- **Client-side only** — nothing is required on the server and no extra packets are sent.
- **Localized** — the interface ships with 26 languages, and the shortcut help page follows the game language.

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Loader | NeoForge 26.3.0.117 or newer |
| Java | 25 |
| Side | Client only |
| Dependencies | NeoForge only (no extra libraries) |

## Installation

1. Install NeoForge for Minecraft 26.3 and start the game once so the loader folders exist.
2. Drop `inventorytweaksremaked-neoforge-26.3-1.1.0.jar` into the `mods` folder of your instance.
3. Launch the game and open your inventory — a **“…”** button appears at the right end of the title row. If it is there, the mod loaded correctly.

The first launch creates `config/InvTweaks/` with the default configuration. Nothing is printed in chat; only one line is written to `logs/latest.log`.

## Usage

| Action | How |
|---|---|
| Sort the area under the cursor | `R` (works anywhere, no need to open the inventory) |
| Sort and cycle algorithms | Middle click |
| Sort by column / by row | Container buttons next to the title row |
| Move one item / all of a type / a stack | `Ctrl` / `Ctrl+Shift` / `Shift` + left click |
| Move everything in the area | `Space` + left click |
| Send to a hotbar slot | `1`–`9` + left click |
| Move to the upper / lower half | `↑` / `↓` + left click |
| Drop | `Alt` + left click |
| Switch sorting profile | Hold `R` for about a second, or `1`–`9` + `R` |

In creative mode the item panel only keeps the settings button; `R` and the middle click never sort the creative item list.

## Configuration

Everything lives in `config/InvTweaks/`:

| File | Purpose |
|---|---|
| `InvTweaks.cfg` | Switches and key bindings (properties format) |
| `InvTweaksRules.txt` | Sorting rules and profiles |
| `InvTweaksTree.txt` | Item category tree (XML content) |
| `trees/*.tree` | Extra tree fragments merged at launch |
| `InvTweaksCompatibility.xml` | Optional container compatibility declarations |

Rules are one constraint per line, `<target> <keyword>`:

```text
Default:

D1 pickaxe
D2 axe
D3 shovel
D4 sword
D5 edibleFood
D9 torch

A1-C5v stuff
A9-C6v blocks
```

- Columns are `1`–`9` left to right, rows are `a`–`d` top to bottom (`d` is the hotbar).
- `D1` is a single slot, `D` a whole row, `1` a whole column, `A5-C9` a rectangle.
- Modifiers: `r` reverses the direction, `v` fills a rectangle vertically first.
- `LOCKED` keeps other items out of the area, `FROZEN` keeps the items inside from moving at all.
- Start a profile with a single word and a colon (`Mining:`), then switch with `R`.

Custom categories go directly under `<stuff>` in `InvTweaksTree.txt`; the element name becomes the keyword used in rules.

## Documentation

- English: <https://huan25748.github.io/Inventory-Tweaks-Remaked/>
- 简体中文: <https://huan25748.github.io/Inventory-Tweaks-Remaked/zh.html>
- 日本語 / Français / Русский: `ja.html`, `fr.html`, `ru.html` on the same site

## Building from source

```bash
# JDK 21 (the build uses a Java 25 toolchain)
./gradlew build
```

The finished jar is written to `build/libs/inventorytweaksremaked-neoforge-26.3-<version>.jar`.

## License and credits

Released under the **MIT License**. Based on the original *Inventory Tweaks* (1.12.2) by **Jimeo Wan** and **kobata**, with thanks to the Inventory Tweaks contributors. Ported and maintained by **33460** and **huan25748**.

---

### 中文简介

Inventory Tweaks Remaked 是经典模组 Inventory Tweaks（1.12.2）的现代移植版，支持箱子与背包排序、容器按钮、点击快捷操作、自动补货，以及可完全自定义的排序规则与物品树。

- 版本：Minecraft 26.3 / NeoForge
- 依赖：NeoForge only (no extra libraries)
- 安装：把 `inventorytweaksremaked-neoforge-26.3-1.1.0.jar` 放进 `mods` 文件夹，进游戏打开背包，标题行右侧出现 **“…”** 按钮即加载成功
- 文档：<https://huan25748.github.io/Inventory-Tweaks-Remaked/zh.html>

> Building on 26.x requires Gradle 9.2+ and a JDK 25 installation; if Gradle cannot find it, pass `-Porg.gradle.java.installations.paths=<your jdk-25 path>`.
