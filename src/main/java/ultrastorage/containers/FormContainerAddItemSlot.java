package ultrastorage.containers;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.inventory.container.slots.ContainerSlot;

public class FormContainerAddItemSlot extends FormContainerSlot {
    VaultStorageContainer container;
    VaultStorageContainerForm containerForm;

    public FormContainerAddItemSlot(Client client, VaultStorageContainer container, VaultStorageContainerForm containerForm, int containerSlotIndex, int x, int y) {
        super(client, container, containerSlotIndex, x, y);
        this.container = container;
        this.containerForm = containerForm;
    }

    @Override
    protected void handleActionControllerEvents(ControllerEvent event) {
        super.handleActionControllerEvents(event);
        moveItem();
    }

    @Override
    public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
        super.handleControllerEvent(event, tickManager, perspective);
        moveItem();
    }

    @Override
    public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
        super.handleInputEvent(event, tickManager, perspective);
        moveItem();
    }

    @Override
    protected void handleActionInputEvents(InputEvent event) {
        super.handleActionInputEvents(event);
        moveItem();
    }

    public final void moveItem() {
        if (this.getContainerSlot().getItem() != null) {
            ContainerSlot containerSlot = container.getAllSlots().stream().filter(ContainerSlot::isClear).findFirst().orElse(null);
            if (containerSlot != null) {
                container.moveItem.runAndSend(this.containerSlotIndex, containerSlot.getContainerIndex());
                container.sortAction.runAndSend();
                containerForm.updateFilter();
            }
        }
    }
}
