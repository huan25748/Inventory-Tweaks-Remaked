package com.inventorytweaks.remaked;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Self-contained translations (English + Chinese) so the mod does not need a lang asset folder.
 */
public final class InvTweaksText {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> ZH = new HashMap<>();

    static {
        EN.put("invtweaks.button.chest1.tooltip", "Default sorting");
        ZH.put("invtweaks.button.chest1.tooltip", "默认排序方式");
        EN.put("invtweaks.button.chest2.tooltip", "Sort in columns");
        ZH.put("invtweaks.button.chest2.tooltip", "按列整理");
        EN.put("invtweaks.button.chest3.tooltip", "Sort in rows");
        ZH.put("invtweaks.button.chest3.tooltip", "按行整理");
        EN.put("invtweaks.button.settings.tooltip", "Inventory settings");
        ZH.put("invtweaks.button.settings.tooltip", "物品栏设置");
        EN.put("invtweaks.button.transfer_up.tooltip", "Move items to container");
        ZH.put("invtweaks.button.transfer_up.tooltip", "转移物品至容器");
        EN.put("invtweaks.button.transfer_down.tooltip", "Move items to inventory");
        ZH.put("invtweaks.button.transfer_down.tooltip", "转移物品至物品栏");
        EN.put("invtweaks.extract.create.error", "[16] The mod won't work, because %s creation failed!");
        ZH.put("invtweaks.extract.create.error", "[16] Mod无法工作，因为%s创建失败！");
        EN.put("invtweaks.extract.find.error", "[15] The mod won't work, because %s could not be found!");
        ZH.put("invtweaks.extract.find.error", "[15] Mod无法工作，因为找不到%s！");
        EN.put("invtweaks.help.bugsorting", "Not working?");
        ZH.put("invtweaks.help.bugsorting", "不起作用？");
        EN.put("invtweaks.help.bugsorting.pt1", "If your server seems to 'cancel' what this mod does,");
        ZH.put("invtweaks.help.bugsorting.pt1", "如果服务器似乎“取消”了此Mod的操作，");
        EN.put("invtweaks.help.bugsorting.pt2", "it's probably caused by it using the NoCheatPlus plugin.");
        ZH.put("invtweaks.help.bugsorting.pt2", "那么可能是因为它使用了NoCheatPlus插件。");
        EN.put("invtweaks.help.bugsorting.pt3", "Ask your admin to disable the following options:");
        ZH.put("invtweaks.help.bugsorting.pt3", "请求你的服务器管理员取消掉以下选项：");
        EN.put("invtweaks.help.bugsorting.pt4", "\"inventory.fastclick\" and \"combined.improbable\"");
        ZH.put("invtweaks.help.bugsorting.pt4", "\"inventory.fastclick\" 和 \"combined.improbable\"");
        EN.put("invtweaks.help.shortcuts.allitems", "Move all items of same type");
        ZH.put("invtweaks.help.shortcuts.allitems", "移动所有同类型物品");
        EN.put("invtweaks.help.shortcuts.backward", "(Backwards)");
        ZH.put("invtweaks.help.shortcuts.backward", "(后退)");
        EN.put("invtweaks.help.shortcuts.click", "Click");
        ZH.put("invtweaks.help.shortcuts.click", "点击");
        EN.put("invtweaks.help.shortcuts.craftall", "Craft all to inventory");
        ZH.put("invtweaks.help.shortcuts.craftall", "全部合成到物品栏");
        EN.put("invtweaks.help.shortcuts.craftone", "Craft to inventory");
        ZH.put("invtweaks.help.shortcuts.craftone", "合成到物品栏");
        EN.put("invtweaks.help.shortcuts.drop", "Drop");
        ZH.put("invtweaks.help.shortcuts.drop", "丢弃");
        EN.put("invtweaks.help.shortcuts.emptyslot", "Move to empty slot");
        ZH.put("invtweaks.help.shortcuts.emptyslot", "移动至空栏位");
        EN.put("invtweaks.help.shortcuts.everything", "Move everything");
        ZH.put("invtweaks.help.shortcuts.everything", "移动所有物品");
        EN.put("invtweaks.help.shortcuts.forward", "(Forward)");
        ZH.put("invtweaks.help.shortcuts.forward", "(前进)");
        EN.put("invtweaks.help.shortcuts.hotbar", "Move to hotbar");
        ZH.put("invtweaks.help.shortcuts.hotbar", "移动至快捷栏");
        EN.put("invtweaks.help.shortcuts.oneitem", "Move one item");
        ZH.put("invtweaks.help.shortcuts.oneitem", "移动一个物品");
        EN.put("invtweaks.help.shortcuts.onestack", "Move one stack");
        ZH.put("invtweaks.help.shortcuts.onestack", "移动一叠物品");
        EN.put("invtweaks.help.shortcuts.or", "or");
        ZH.put("invtweaks.help.shortcuts.or", "或");
        EN.put("invtweaks.help.shortcuts.rightclick", "(Any shortcut) + Right instead of left click");
        ZH.put("invtweaks.help.shortcuts.rightclick", "(任意快捷键) + 鼠标右键");
        EN.put("invtweaks.help.shortcuts.selectconfig", "Select sorting configuration");
        ZH.put("invtweaks.help.shortcuts.selectconfig", "选择整理方案");
        EN.put("invtweaks.help.shortcuts.title", "Shortcuts help");
        ZH.put("invtweaks.help.shortcuts.title", "快捷键帮助");
        EN.put("invtweaks.key.category", "Inventory Tweaks");
        ZH.put("invtweaks.key.category", "背包一键整理");
        EN.put("invtweaks.key.sort", "Inventory Sorting");
        ZH.put("invtweaks.key.sort", "物品栏整理");
        EN.put("invtweaks.keyboardfix.error", "[12] I'm having troubles with the keyboard");
        ZH.put("invtweaks.keyboardfix.error", "[12] 我的键盘有问题");
        EN.put("invtweaks.keyboardfix.recover", "Ok it's repaired, sorry about that.");
        ZH.put("invtweaks.keyboardfix.recover", "问题已修复，抱歉");
        EN.put("invtweaks.loadconfig.done", "Configuration loaded.");
        ZH.put("invtweaks.loadconfig.done", "配置文件已载入");
        EN.put("invtweaks.loadconfig.enabled", "'%s' enabled");
        ZH.put("invtweaks.loadconfig.enabled", "'%s'已启用");
        EN.put("invtweaks.loadconfig.filemissing", "missing, creating default one.");
        ZH.put("invtweaks.loadconfig.filemissing", "缺失，正在创建一个默认配置");
        EN.put("invtweaks.loadconfig.invalidkeywords", "Invalid keywords found");
        ZH.put("invtweaks.loadconfig.invalidkeywords", "关键词无效");
        EN.put("invtweaks.loadconfig.refresh.error", "[1] Failed to refresh properties from file");
        ZH.put("invtweaks.loadconfig.refresh.error", "[1] 无法从文件中更新配置");
        EN.put("invtweaks.modcompat.ci.error", "[13] Failed to manage 'Convenient Inventory' compatibility");
        ZH.put("invtweaks.modcompat.ci.error", "[13] 无法与'Convenient Inventory'达成兼容");
        EN.put("invtweaks.pickup.error", "[7] Failed to move picked up stack");
        ZH.put("invtweaks.pickup.error", "[7] 无法移动拾取的堆叠");
        EN.put("invtweaks.propsfile.error", "[14] Failed to create the mod properties file");
        ZH.put("invtweaks.propsfile.error", "[14] 无法创建Mod配置文件");
        EN.put("invtweaks.propsfile.loaded", "Mod properties loaded");
        ZH.put("invtweaks.propsfile.loaded", "Mod配置文件已载入");
        EN.put("invtweaks.settings.advanced.autoequip", "Auto-equip armor");
        ZH.put("invtweaks.settings.advanced.autoequip", "自动装备盔甲");
        EN.put("invtweaks.settings.advanced.autoequip.tooltip", "Equips the best available\\narmor when sorting");
        ZH.put("invtweaks.settings.advanced.autoequip.tooltip", "整理时自动装备上最好的盔甲");
        EN.put("invtweaks.settings.advanced.mappingsfile", "Open the shortcuts mappings file...");
        ZH.put("invtweaks.settings.advanced.mappingsfile", "打开快捷键映射文件");
        EN.put("invtweaks.settings.advanced.mappingsfile.error", "[11] Failed to open mappings file");
        ZH.put("invtweaks.settings.advanced.mappingsfile.error", "[11] 无法打开映像文件");
        EN.put("invtweaks.settings.advanced.serverassist", "Server Item Movement");
        ZH.put("invtweaks.settings.advanced.serverassist", "服务端物品移动");
        EN.put("invtweaks.settings.advanced.serverassist.tooltip", "Enable using server assistance for item movement while sorting.");
        ZH.put("invtweaks.settings.advanced.serverassist.tooltip", "使用服务端的物品移动协助功能");
        EN.put("invtweaks.settings.advanced.sortonpickup", "Sort on pickup");
        ZH.put("invtweaks.settings.advanced.sortonpickup", "拾取物品时自动整理");
        EN.put("invtweaks.settings.advanced.sortonpickup.soloonly", "SP only");
        ZH.put("invtweaks.settings.advanced.sortonpickup.soloonly", "仅限单人游戏");
        EN.put("invtweaks.settings.advanced.sortonpickup.tooltip", "Avoids to fill the hotbar with\\neverything that you pickup");
        ZH.put("invtweaks.settings.advanced.sortonpickup.tooltip", "避免用你拾到的物品来填充快捷栏");
        EN.put("invtweaks.settings.advanced.sortonpickup.tooltip.soloonly", "Only in single player mode");
        ZH.put("invtweaks.settings.advanced.sortonpickup.tooltip.soloonly", "仅限单人游戏模式");
        EN.put("invtweaks.settings.advanced.sounds", "Enable sound");
        ZH.put("invtweaks.settings.advanced.sounds", "开启音效");
        EN.put("invtweaks.settings.advanced.sounds.tooltip", "Play sounds when sorting\\nor when auto-refill is triggered");
        ZH.put("invtweaks.settings.advanced.sounds.tooltip", "在整理或自动装填时播放音效");
        EN.put("invtweaks.settings.autorefill", "Auto-refill");
        ZH.put("invtweaks.settings.autorefill", "自动装填");
        EN.put("invtweaks.settings.autorefill.tooltip", "Replaces broken tools & emptied stacks\\nwith others from your inventory");
        ZH.put("invtweaks.settings.autorefill.tooltip", "自动替换损坏的工具和用尽的堆叠");
        EN.put("invtweaks.settings.beforebreak", "Repair-friendly refill");
        ZH.put("invtweaks.settings.beforebreak", "保留式重装");
        EN.put("invtweaks.settings.beforebreak.tooltip", "Replaces tools just before they break");
        ZH.put("invtweaks.settings.beforebreak.tooltip", "在工具坏掉前的一瞬间替换它们");
        EN.put("invtweaks.settings.chestbuttons", "Chest buttons");
        ZH.put("invtweaks.settings.chestbuttons", "箱子按钮");
        EN.put("invtweaks.settings.chestbuttons.tooltip", "Adds three buttons\\non chests to sort them");
        ZH.put("invtweaks.settings.chestbuttons.tooltip", "在箱子上添加三个按钮来排序整理");
        EN.put("invtweaks.settings.disableci", "Disable CI");
        ZH.put("invtweaks.settings.disableci", "禁用CI");
        EN.put("invtweaks.settings.disableci.tooltip", "In conflict with Convenient Inventory");
        ZH.put("invtweaks.settings.disableci.tooltip", "与Convenient Inventory冲突");
        EN.put("invtweaks.settings.displaytooltip", "Display Path Tooltip");
        EN.put("invtweaks.settings.displaytooltip.tooltip", "Show the Item Tree XPaths\\nthat apply to the item in\\nthe tool tip.\\n(Handy for JEI filtering.)");
        EN.put("invtweaks.settings.exit", "Done");
        ZH.put("invtweaks.settings.exit", "完成");
        EN.put("invtweaks.settings.key", "Sort key:");
        ZH.put("invtweaks.settings.key", "整理键：");
        EN.put("invtweaks.settings.middleclick", "Middle click");
        ZH.put("invtweaks.settings.middleclick", "鼠标中键");
        EN.put("invtweaks.settings.middleclick.tooltip", "Allows to sort using\\nthe middle click");
        ZH.put("invtweaks.settings.middleclick.tooltip", "使用鼠标中键来排序整理");
        EN.put("invtweaks.settings.moddedtreefile", "Browse modded tree files");
        EN.put("invtweaks.settings.moddedtreefile.error", "[17] Failed to open Modded Tree Repository");
        EN.put("invtweaks.settings.moreoptions", "More options...");
        ZH.put("invtweaks.settings.moreoptions", "更多选项...");
        EN.put("invtweaks.settings.moreoptions.tooltip", "Even more cool stuff");
        ZH.put("invtweaks.settings.moreoptions.tooltip", "更多炫酷的东西");
        EN.put("invtweaks.settings.off", "OFF");
        ZH.put("invtweaks.settings.off", "关闭");
        EN.put("invtweaks.settings.on", "ON");
        ZH.put("invtweaks.settings.on", "开启");
        EN.put("invtweaks.settings.onlinehelp", "Open help in browser...");
        ZH.put("invtweaks.settings.onlinehelp", "在浏览器中打开帮助...");
        EN.put("invtweaks.settings.onlinehelp.error", "[10] Failed to open help");
        ZH.put("invtweaks.settings.onlinehelp.error", "[10] 无法打开帮助文件");
        EN.put("invtweaks.settings.pvpwarning.pt1", "Note: On PvP servers, the auto-refill & auto-equip armor");
        ZH.put("invtweaks.settings.pvpwarning.pt1", "提示：在PVP服务器上，自动填装和自动装备盔甲");
        EN.put("invtweaks.settings.pvpwarning.pt2", "options are often considered cheating!");
        ZH.put("invtweaks.settings.pvpwarning.pt2", "的选项通常被视作为作弊！");
        EN.put("invtweaks.settings.rulesfile", "Open the sorting rules file...");
        ZH.put("invtweaks.settings.rulesfile", "打开排序规则文件...");
        EN.put("invtweaks.settings.rulesfile.error", "[8] Failed to open rules file");
        ZH.put("invtweaks.settings.rulesfile.error", "[8] 无法打开规则文件");
        EN.put("invtweaks.settings.shortcuts", "Shortcuts");
        ZH.put("invtweaks.settings.shortcuts", "快捷键");
        EN.put("invtweaks.settings.shortcuts.tooltip", "Enables various shortcuts\\nto move items around");
        ZH.put("invtweaks.settings.shortcuts.tooltip", "启用各种快捷键来移动物品");
        EN.put("invtweaks.settings.title", "Inventory and chests settings");
        ZH.put("invtweaks.settings.title", "物品栏和箱子设置");
        EN.put("invtweaks.settings.treefile", "Open the item tree file...");
        ZH.put("invtweaks.settings.treefile", "打开物品树文件...");
        EN.put("invtweaks.settings.treefile.error", "[9] Failed to open tree file");
        ZH.put("invtweaks.settings.treefile.error", "[9] 无法打开树文件");
        EN.put("invtweaks.shortcut.error", "[6] Failed to trigger shortcut");
        ZH.put("invtweaks.shortcut.error", "[6] 无法触发快捷键");
        EN.put("invtweaks.sort.autorefill.error", "[4] Failed to trigger auto-refill");
        ZH.put("invtweaks.sort.autorefill.error", "[4] 无法触发自动装填");
        EN.put("invtweaks.sort.chest.error", "[3] Failed to sort container");
        ZH.put("invtweaks.sort.chest.error", "[3] 整理容器失败");
        EN.put("invtweaks.sort.inventory.error", "[2] Failed to sort inventory");
        ZH.put("invtweaks.sort.inventory.error", "[2] 整理物品栏失败");
        EN.put("invtweaks.sort.releaseitem.error", "[5] Failed to release item");
        ZH.put("invtweaks.sort.releaseitem.error", "[5] 无法放下物品");
        EN.put("key.categories.inventory", "Inventory");
        ZH.put("key.categories.inventory", "背包");
        EN.put("key.inventorytweaks.sort", "Sort inventory");
        ZH.put("key.inventorytweaks.sort", "整理背包");
    }

    private InvTweaksText() {
    }

    public static boolean isChinese() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getLanguageManager() == null) {
            return false;
        }
        String code = mc.getLanguageManager().getSelected();
        return code != null && code.toLowerCase().startsWith("zh");
    }

    public static String get(String key) {
        Map<String, String> map = isChinese() ? ZH : EN;
        String value = map.get(key);
        if (value == null) {
            value = EN.get(key);
        }
        return value == null ? key : value;
    }

    public static MutableComponent text(String key) {
        return Component.literal(get(key));
    }

    public static MutableComponent text(String key, Object... args) {
        return Component.literal(String.format(get(key), args));
    }
}
