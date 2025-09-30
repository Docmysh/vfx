package Vfx.vfx.menu;

import Vfx.vfx.Vfx;
import Vfx.vfx.item.ShadowCollectorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ShadowSelectionMenu extends AbstractContainerMenu {
    private static final int MAX_ROWS = 6;
    private static final int SLOTS_PER_ROW = 9;
    private static final int SLOTS_PER_PAGE = MAX_ROWS * SLOTS_PER_ROW;
    private static final int SLOT_BUTTON_OFFSET = 2;
    private final List<ResourceLocation> shadows;
    private final SimpleContainer container;
    private int totalPages;
    private int page = 0;
    private int visibleRows = 1;
    private final InteractionHand hand;

    public ShadowSelectionMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readShadows(buffer), buffer.readEnum(InteractionHand.class));
    }

    public ShadowSelectionMenu(int id, Inventory inventory, List<ResourceLocation> shadows, InteractionHand hand) {
        super(Vfx.SHADOW_SELECTION_MENU.get(), id);
        this.shadows = new ArrayList<>(shadows);
        this.container = new SimpleContainer(SLOTS_PER_PAGE);
        this.hand = hand;

        for (int row = 0; row < MAX_ROWS; row++) {
            for (int column = 0; column < SLOTS_PER_ROW; column++) {
                int slotIndex = row * SLOTS_PER_ROW + column;
                this.addSlot(new Slot(container, slotIndex, 8 + column * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }

        refreshContents();
    }

    public static void writeData(FriendlyByteBuf buffer, List<ResourceLocation> shadows, InteractionHand hand) {
        writeShadows(buffer, shadows);
        buffer.writeEnum(hand);
    }

    private static void writeShadows(FriendlyByteBuf buffer, List<ResourceLocation> shadows) {
        buffer.writeVarInt(shadows.size());
        for (ResourceLocation id : shadows) {
            buffer.writeResourceLocation(id);
        }
    }

    private static List<ResourceLocation> readShadows(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ResourceLocation> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buffer.readResourceLocation());
        }
        return list;
    }

    private ItemStack createDisplayStack(ResourceLocation id) {
        ItemStack stack = new ItemStack(Items.ECHO_SHARD);
        if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType != null) {
                SpawnEggItem eggItem = SpawnEggItem.byId(entityType);
                if (eggItem != null) {
                    stack = new ItemStack(eggItem);
                }
                Component entityName = entityType.getDescription();
                stack.setHoverName(Component.translatable("menu.vfx.shadow_collector.entry", entityName));
                return stack;
            }
        }
        stack.setHoverName(Component.translatable("menu.vfx.shadow_collector.entry", Component.literal(id.toString())));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!handleSummonClick(player, slotId)) {
            super.clicked(slotId, button, clickType, player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void clientChangePage(int delta) {
        changePage(delta);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            changePage(-1);
            return true;
        }
        if (id == 1) {
            changePage(1);
            return true;
        }
        if (id >= SLOT_BUTTON_OFFSET && id < SLOT_BUTTON_OFFSET + SLOTS_PER_PAGE) {
            return handleSummonClick(player, id - SLOT_BUTTON_OFFSET);
        }
        return super.clickMenuButton(player, id);
    }

    private void changePage(int delta) {
        if (totalPages <= 1) {
            return;
        }
        int newPage = Mth.clamp(this.page + delta, 0, totalPages - 1);
        if (newPage != this.page) {
            this.page = newPage;
            updateDisplayedStacks();
            this.broadcastChanges();
        }
    }

    public boolean isShadowSlot(Slot slot) {
        return slot != null && slot.container == this.container;
    }

    public int getButtonIdForSlot(int slotIndex) {
        return SLOT_BUTTON_OFFSET + slotIndex;
    }

    public int getSlotIndex(Slot slot) {
        return this.slots.indexOf(slot);
    }

    private boolean handleSummonClick(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOTS_PER_PAGE) {
            return false;
        }
        int globalIndex = this.page * SLOTS_PER_PAGE + slotIndex;
        if (globalIndex >= shadows.size()) {
            closeMenu(player);
            return true;
        }

        ResourceLocation typeId = shadows.get(globalIndex);
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack collector = player.getItemInHand(hand);
            if (collector.getItem() instanceof ShadowCollectorItem) {
                ShadowCollectorItem.ShadowBehavior behavior = ShadowCollectorItem.getBehavior(collector);
                if (ShadowCollectorItem.summonShadow(serverPlayer, typeId, behavior) && !collector.isEmpty()) {
                    ShadowCollectorItem.consumeShadow(collector, typeId);
                    shadows.remove(globalIndex);
                    refreshContents();
                    this.broadcastChanges();
                }
            }
        }

        closeMenu(player);
        return true;
    }

    private void closeMenu(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.closeContainer();
        } else {
            player.closeContainer();
        }
    }

    private void refreshContents() {
        this.totalPages = Math.max(1, (int) Math.ceil((double) this.shadows.size() / SLOTS_PER_PAGE));
        this.page = Mth.clamp(this.page, 0, this.totalPages - 1);
        updateDisplayedStacks();
    }

    private void updateDisplayedStacks() {
        int startIndex = this.page * SLOTS_PER_PAGE;
        int displayed = 0;
        for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
            int shadowIndex = startIndex + slot;
            if (shadowIndex < shadows.size()) {
                container.setItem(slot, createDisplayStack(shadows.get(shadowIndex)));
                displayed++;
            } else {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        this.visibleRows = Math.max(1, Math.min(MAX_ROWS, (int) Math.ceil(displayed / (double) SLOTS_PER_ROW)));
    }
}