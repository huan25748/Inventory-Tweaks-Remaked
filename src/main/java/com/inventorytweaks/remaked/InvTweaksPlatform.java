package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.List;
import java.util.Map;

/**
 * Minecraft 1.21.1 platform layer, replacing the 1.3-1.12 obfuscation layer of the original mod.
 *
 * @author Jimeo Wan
 */
public class InvTweaksPlatform {

    public final Minecraft mc;

    public InvTweaksPlatform(Minecraft mc_) {
        mc = mc_;
    }

    // Minecraft members

    public static String getNamespacedID(String id) {
        if (id == null) {
            return null;
        } else if (id.indexOf(':') == -1) {
            return "minecraft:" + id;
        }
        return id;
    }

    public static boolean areItemStacksEqual(ItemStack itemStack1, ItemStack itemStack2) {
        return ItemStack.isSameItemSameTags(itemStack1, itemStack2) && itemStack1.getCount() == itemStack2.getCount();
    }

    public static ItemStack getSlotStack(AbstractContainerMenu menu, int i) {
        Slot slot = menu.slots.get(i);
        return (slot == null) ? ItemStack.EMPTY : slot.getItem();
    }

    public static int getSlotNumber(Slot slot) {
        return slot.index;
    }

    public static Slot getSlotAtMousePosition(AbstractContainerScreen<?> guiContainer) {
        if (guiContainer == null) {
            return null;
        }
        int mouseX = (int) getScaledMouseX();
        int mouseY = (int) getScaledMouseY();
        return getSlotAtPosition(guiContainer, mouseX, mouseY);
    }

    public static Slot getSlotAtPosition(AbstractContainerScreen<?> guiContainer, int mouseX, int mouseY) {
        AbstractContainerMenu menu = guiContainer.getMenu();
        int x = mouseX - ScreenAccessor.getGuiLeft(guiContainer);
        int y = mouseY - ScreenAccessor.getGuiTop(guiContainer);
        for (int k = 0; k < menu.slots.size(); k++) {
            Slot slot = menu.slots.get(k);
            if (!slot.isActive()) {
                continue;
            }
            if (x >= slot.x - 1 && x < slot.x + 16 + 1 && y >= slot.y - 1 && y < slot.y + 16 + 1) {
                return slot;
            }
        }
        return null;
    }

    public static double getScaledMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    public static double getScaledMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    public static int getSpecialChestRowSize(AbstractContainerMenu menu) {
        return SlotMaps.getRowSize(menu);
    }

    public static boolean isValidChest(AbstractContainerMenu menu) {
        if (menu == null) {
            return false;
        }
        Map<ContainerSection, List<Slot>> map = SlotMaps.getSlotMap(menu);
        List<Slot> chest = map.get(ContainerSection.CHEST);
        return chest != null && !chest.isEmpty();
    }

    /**
     * The "large chest" layout (buttons stacked outside the GUI's right edge) is only used for containers that
     * explicitly ask for it, exactly like 1.12: the vanilla double chest was <b>not</b> flagged as large chest, so it
     * uses the normal horizontal button row.
     */
    public static boolean isLargeChest(AbstractContainerMenu menu) {
        if (menu == null) {
            return false;
        }
        ChestContainer annotation = menu.getClass().getAnnotation(ChestContainer.class);
        return annotation != null && annotation.isLargeChest();
    }

    public static boolean isValidInventory(AbstractContainerMenu menu) {
        return menu instanceof net.minecraft.world.inventory.InventoryMenu;
    }

    public static boolean showButtons(AbstractContainerMenu menu) {
        return menu != null;
    }

    public static Map<ContainerSection, List<Slot>> getContainerSlotMap(AbstractContainerMenu menu) {
        return SlotMaps.getSlotMap(menu);
    }

    public static boolean isGuiContainer(Object o) {
        return o instanceof AbstractContainerScreen;
    }

    public static boolean isGuiInventoryCreative(Object o) {
        return o != null && o.getClass().equals(CreativeModeInventoryScreen.class);
    }

    public static boolean isGuiInventory(Object o) {
        return o != null && o.getClass().equals(InventoryScreen.class);
    }

    public static boolean isGuiEditSign(Object o) {
        return o instanceof net.minecraft.client.gui.screens.inventory.SignEditScreen;
    }

    public static boolean isItemArmor(Object o) {
        return o instanceof ArmorItem;
    }

    public static boolean isBasicSlot(Object o) {
        // Only vanilla slots are used as intermediate slots.
        return o != null && o.getClass().equals(Slot.class);
    }

    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
    }

    public static AbstractContainerMenu getCurrentContainer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        AbstractContainerMenu currentContainer = mc.player.inventoryMenu;
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            currentContainer = containerScreen.getMenu();
        }
        return currentContainer;
    }

    // Slot members

    public static boolean areSameItemType(ItemStack itemStack1, ItemStack itemStack2) {
        return !itemStack1.isEmpty() && !itemStack2.isEmpty()
                && (ItemStack.isSameItem(itemStack1, itemStack2) && (itemStack1.isDamageableItem() || ItemStack.isSameItemSameTags(itemStack1, itemStack2)));
    }

    public static boolean areItemsStackable(ItemStack itemStack1, ItemStack itemStack2) {
        return !itemStack1.isEmpty() && !itemStack2.isEmpty()
                && ItemStack.isSameItemSameTags(itemStack1, itemStack2)
                && itemStack1.isStackable()
                && itemStack1.isStackable() == itemStack2.isStackable();
    }

    public void addChatMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(Component.literal(message));
        }
    }

    public LocalPlayer getThePlayer() {
        return mc.player;
    }

    public Screen getCurrentScreen() {
        return mc.screen;
    }

    public void displayGuiScreen(Screen parentScreen) {
        mc.setScreen(parentScreen);
    }

    public int getKeyBindingForwardKeyCode() {
        return Keys.getCode("W");
    }

    public int getKeyBindingBackKeyCode() {
        return Keys.getCode("S");
    }

    public NonNullList<ItemStack> getMainInventory() {
        if (mc.player == null) {
            return NonNullList.create();
        }
        return mc.player.getInventory().items;
    }

    public ItemStack getHeldStack() {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        }
        return mc.player.containerMenu.getCarried();
    }

    public ItemStack getFocusedStack() {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        }
        return mc.player.getInventory().getSelected();
    }

    public int getFocusedSlot() {
        if (mc.player == null) {
            return -1;
        }
        return mc.player.getInventory().selected;
    }

    public boolean hasTexture(ResourceLocation texture) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public ItemStack getOffhandStack() {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        }
        return mc.player.getOffhandItem();
    }

    public void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static net.minecraft.world.item.Item getItem(String id) {
        if (id == null) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(key);
    }
}
