package Vfx.vfx.menu;

import Vfx.vfx.Vfx;
import Vfx.vfx.item.ShadowCollectorItem;
import net.minecraft.resources.ResourceLocation;
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
    private final ItemStack collectorStack;
    private final List<ResourceLocation> shadows;
    private final SimpleContainer container;
    private final int totalPages;
    private int page = 0;
    private int visibleRows = 1;

    public ShadowSelectionMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, ItemStack.EMPTY, readShadows(buffer));
    }

    public ShadowSelectionMenu(int id, Inventory inventory, ItemStack stack, List<ResourceLocation> shadows) {
        super(Vfx.SHADOW_SELECTION_MENU.get(), id);
        this.collectorStack = stack;
        this.shadows = new ArrayList<>(shadows);
        this.totalPages = Math.max(1, (int) Math.ceil((double) this.shadows.size() / SLOTS_PER_PAGE));
        this.container = new SimpleContainer(SLOTS_PER_PAGE);

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

        updateDisplayedStacks();
    }

    public static void writeShadows(FriendlyByteBuf buffer, List<ResourceLocation> shadows) {
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
        if (slotId >= 0 && slotId < SLOTS_PER_PAGE) {
            int globalIndex = this.page * SLOTS_PER_PAGE + slotId;
            if (globalIndex < shadows.size()) {
                ResourceLocation typeId = shadows.get(globalIndex);
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    if (ShadowCollectorItem.summonShadow(serverPlayer, typeId) && !collectorStack.isEmpty()) {
                        ShadowCollectorItem.consumeShadow(collectorStack, typeId);
                    }
                }
            }
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            } else {
                player.closeContainer();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
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