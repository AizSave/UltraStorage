package ultrastorage.containers;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormTypingComponent;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.Container;
import necesse.inventory.container.ContainerAction;
import necesse.inventory.container.ContainerActionResult;
import necesse.inventory.container.slots.ContainerSlot;

import java.util.function.Supplier;

public class FormContainerVaultSlot extends FormContainerSlot {
    public FormContainerVaultSlot(Client client, Container container, int containerSlotIndex, int x, int y) {
        super(client, container, containerSlotIndex, x, y);
    }

    @Override
    protected void handleActionInputEvents(InputEvent event) {
        if (event.state && !event.isKeyboardEvent()) {
            GameWindow window = WindowManager.getWindow();
            SelectionFloatMenu menu;
            if (event.getID() == -100) {
                if (this.isMouseOver(event) && (!window.isKeyDown(340) || !FormTypingComponent.appendItemToTyping(this.getContainerSlot().getItem()))) {
                    if (Control.INV_QUICK_MOVE.isDown()) {
                        this.runAction(ContainerAction.QUICK_MOVE, event.shouldSubmitSound());
                    } else if (Control.INV_QUICK_TRASH.isDown() && this.canCurrentlyQuickTrash()) {
                        this.runAction(ContainerAction.QUICK_TRASH, event.shouldSubmitSound());
                    } else if (Control.INV_QUICK_DROP.isDown() && !this.getContainerSlot().isItemLocked()) {
                        this.runAction(ContainerAction.QUICK_DROP, event.shouldSubmitSound());
                    } else if (Control.INV_LOCK.isDown() && this.canCurrentlyLockItem()) {
                        this.runAction(ContainerAction.TOGGLE_LOCKED, event.shouldSubmitSound());
                    } else {
                        menu = new SelectionFloatMenu(this) {
                            public void draw(TickManager tickManager, PlayerMob perspective) {
                                if (!FormContainerVaultSlot.this.client.getPlayer().isInventoryExtended()) {
                                    this.remove();
                                }

                                super.draw(tickManager, perspective);
                            }
                        };
                        menu.setCreateEvent(event);
                        this.addLeftClickActions(menu);
                        if (!menu.isEmpty()) {
                            if (!this.getContainerSlot().isClear()) {
                                menu.add(Localization.translate("ui", "slottakefull"), () -> {
                                    this.runAction(ContainerAction.LEFT_CLICK, false);
                                    menu.remove();
                                });
                            }

                            this.getManager().openFloatMenu(menu);
                            this.playTickSound();
                        } else {
                            this.runAction(ContainerAction.LEFT_CLICK, event.shouldSubmitSound());
                        }
                    }
                }
            } else if (!(event.getID() != -99 && !event.isRepeatEvent(this)) && this.isMouseOver(event)) {
                InventoryItem invItem;
                if (Control.INV_QUICK_MOVE.isDown()) {
                    ContainerSlot containerSlot = this.getContainerSlot();
                    invItem = containerSlot.getItem();
                    int itemID = invItem == null ? -1 : invItem.item.getID();
                    if (event.getID() == -99 || event.isRepeatEvent(this, ContainerAction.TAKE_ONE, itemID)) {
                        if (itemID != -1) {
                            event.startRepeatEvents(this, ContainerAction.TAKE_ONE, itemID);
                        }

                        this.runAction(ContainerAction.TAKE_ONE, event.shouldSubmitSound());
                    }
                } else if (Control.INV_QUICK_TRASH.isDown() && this.canCurrentlyQuickTrash()) {
                    this.runAction(ContainerAction.QUICK_TRASH_ONE, event.shouldSubmitSound());
                } else if (Control.INV_QUICK_DROP.isDown() && !this.getContainerSlot().isItemLocked()) {
                    this.runAction(ContainerAction.QUICK_DROP_ONE, event.shouldSubmitSound());
                } else if (Control.INV_LOCK.isDown() && this.canCurrentlyLockItem()) {
                    this.runAction(ContainerAction.TOGGLE_LOCKED, event.shouldSubmitSound());
                } else {
                    menu = new SelectionFloatMenu(this) {
                        public void draw(TickManager tickManager, PlayerMob perspective) {
                            if (!FormContainerVaultSlot.this.client.getPlayer().isInventoryExtended()) {
                                this.remove();
                            }

                            super.draw(tickManager, perspective);
                        }
                    };
                    menu.setCreateEvent(event);
                    this.addRightClickActions(menu);
                    Supplier<ContainerActionResult> rAction;
                    if (!menu.isEmpty()) {
                        if (!this.getContainerSlot().isClear()) {
                            invItem = this.getContainerSlot().getItem();
                            rAction = invItem.item.getInventoryRightClickAction(this.getContainer(), invItem, this.getContainerSlot().getContainerIndex(), this.getContainerSlot());
                            if (rAction != null) {
                                String tip = invItem.item.getInventoryRightClickControlTip(this.getContainer(), invItem, this.getContainerSlot().getContainerIndex(), this.getContainerSlot());
                                if (tip != null) {
                                    menu.add(tip, () -> {
                                        this.runAction(ContainerAction.RIGHT_CLICK_ACTION, false);
                                        menu.remove();
                                    });
                                } else {
                                    menu.add(Localization.translate("ui", "slotuse"), () -> {
                                        this.runAction(ContainerAction.RIGHT_CLICK_ACTION, false);
                                        menu.remove();
                                    });
                                }
                            } else {
                                menu.add(Localization.translate("ui", "slotsplit"), () -> {
                                    this.runAction(ContainerAction.RIGHT_CLICK, false);
                                    menu.remove();
                                });
                            }
                        }

                        this.getManager().openFloatMenu(menu);
                        this.playTickSound();
                    } else {
                        invItem = this.getContainerSlot().getItem();
                        rAction = invItem == null ? null : invItem.item.getInventoryRightClickAction(this.getContainer(), invItem, this.getContainerSlot().getContainerIndex(), this.getContainerSlot());
                        if (rAction != null) {
                            this.runAction(ContainerAction.RIGHT_CLICK_ACTION, event.shouldSubmitSound());
                        } else {
                            this.runAction(ContainerAction.RIGHT_CLICK, event.shouldSubmitSound());
                        }
                    }
                }
            }

        }
    }

}
