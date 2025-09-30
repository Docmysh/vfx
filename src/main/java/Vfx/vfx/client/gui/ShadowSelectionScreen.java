package Vfx.vfx.client.gui;

import Vfx.vfx.menu.ShadowSelectionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ShadowSelectionScreen extends AbstractContainerScreen<ShadowSelectionMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    private Button previousButton;
    private Button nextButton;
    private Button returnAllButton;

    public ShadowSelectionScreen(ShadowSelectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + menu.getVisibleRows() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.returnAllButton = Button.builder(Component.translatable("screen.vfx.shadow_collector.return_all"), button -> returnAllShadows())
                .bounds(x + 8, y - 22, 110, 20)
                .build();
        this.previousButton = Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(x + this.imageWidth - 50, y - 22, 20, 20)
                .build();
        this.nextButton = Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(x + this.imageWidth - 25, y - 22, 20, 20)
                .build();
        this.addRenderableWidget(returnAllButton);
        this.addRenderableWidget(previousButton);
        this.addRenderableWidget(nextButton);
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLayout();
        updateButtons();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int rows = this.menu.getVisibleRows();
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, rows * 18 + 17);
        guiGraphics.blit(TEXTURE, x, y + rows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        if (this.menu.getTotalPages() > 1) {
            Component pageText = Component.translatable("screen.vfx.shadow_collector.page", this.menu.getPage() + 1, this.menu.getTotalPages());
            guiGraphics.drawString(this.font, pageText, this.imageWidth - 8 - this.font.width(pageText), 6, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            Slot hovered = this.hoveredSlot;
            if (hovered != null && hovered.hasItem() && this.menu.isShadowSlot(hovered)) {
                int slotIndex = this.menu.getSlotIndex(hovered);
                if (slotIndex >= 0) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                            this.menu.getButtonIdForSlot(slotIndex));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateLayout() {
        int rows = this.menu.getVisibleRows();
        int desiredHeight = 114 + rows * 18;
        if (desiredHeight != this.imageHeight) {
            this.imageHeight = desiredHeight;
            this.inventoryLabelY = this.imageHeight - 94;
        }
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (this.returnAllButton != null) {
            this.returnAllButton.setPosition(x + 8, y - 22);
        }
        if (this.previousButton != null && this.nextButton != null) {
            this.previousButton.setPosition(x + this.imageWidth - 50, y - 22);
            this.nextButton.setPosition(x + this.imageWidth - 25, y - 22);
        }
    }

    private void updateButtons() {
        boolean multiple = this.menu.getTotalPages() > 1;
        if (this.returnAllButton != null) {
            this.returnAllButton.visible = true;
            this.returnAllButton.active = this.minecraft != null && this.minecraft.gameMode != null;
        }
        if (this.previousButton != null) {
            this.previousButton.visible = multiple;
            this.previousButton.active = multiple && this.menu.getPage() > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.visible = multiple;
            this.nextButton.active = multiple && this.menu.getPage() < this.menu.getTotalPages() - 1;
        }
    }

    private void changePage(int delta) {
        if ((delta < 0 && this.menu.getPage() <= 0) || (delta > 0 && this.menu.getPage() >= this.menu.getTotalPages() - 1)) {
            return;
        }
        this.menu.clientChangePage(delta);
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, delta < 0 ? 0 : 1);
        }
    }

    private void returnAllShadows() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.menu.getReturnAllButtonId());
        }
    }
}