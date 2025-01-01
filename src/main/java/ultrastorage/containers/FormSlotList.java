package ultrastorage.containers;

import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.forms.components.lists.FormGeneralGridList;
import necesse.gfx.forms.components.lists.FormListGridElement;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.slots.ContainerSlot;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

abstract public class FormSlotList extends FormGeneralGridList<FormSlotList.SlotGrid> {
    public Client client;
    protected VaultStorageContainer container;

    public FormSlotList(int x, int y, int width, int height, Client client, VaultStorageContainer container) {
        super(x, y, width, height, 36, 36);
        this.client = client;
        this.container = container;
    }

    public void setSlots(Collection<ContainerSlot> containerSlots) {
        this.elements = new ArrayList<>();
        containerSlots.stream().filter(containerSlot -> containerSlot.getItem() != null && containerSlot.getItem().item != null)
                .forEach(containerSlot -> this.elements.add(new SlotGrid(containerSlot.getContainerIndex())));

        this.limitMaxScroll();
    }

    abstract public void onSlotClicked(int slotIndex, InputEvent event);

    public GameMessage getEmptyMessage() {
        return new LocalMessage("ui", "storageempty");
    }

    public class SlotGrid extends FormListGridElement<FormSlotList> {
        public final int slotIndex;

        public SlotGrid(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        protected void draw(FormSlotList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {
            ContainerSlot containerSlot = container.getSlot(slotIndex);
            InventoryItem item = containerSlot.getItem();
            Color color = Settings.UI.activeElementColor;
            if (this.isMouseOver(parent)) {
                color = Settings.UI.highlightElementColor;
                if (item != null && item.item != null) {
                    GameWindow window = WindowManager.getWindow();
                    if (!window.isKeyDown(-100) && !window.isKeyDown(-99)) {
                        ListGameTooltips tooltips = item.item.getTooltips(item, perspective, new GameBlackboard());
                        GameTooltipManager.addTooltip(tooltips, GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
                    }
                }
            }
            GameTexture slotTexture = this.isMouseOver(parent) ? Settings.UI.inventoryslot_small.highlighted : Settings.UI.inventoryslot_small.active;
            slotTexture.initDraw().color(color).draw(2, 2);
            if (item != null && item.item != null) {
                item.draw(perspective, 2, 2);
            }
        }

        protected void onClick(FormSlotList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
            FormSlotList.this.onSlotClicked(slotIndex, event);
        }

        protected void onControllerEvent(FormSlotList parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
            if (event.getState() == ControllerInput.MENU_SELECT) {
                FormSlotList.this.onSlotClicked(slotIndex, InputEvent.ControllerButtonEvent(event, tickManager));
                event.use();
            }
        }
    }


}
