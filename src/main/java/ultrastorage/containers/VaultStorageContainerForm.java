package ultrastorage.containers;

import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.network.client.Client;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.GameBackground;
import necesse.gfx.GameColor;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.fairType.parsers.TypeParser;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.*;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.ContainerFormSwitcher;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementObjectStatusFormManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.*;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.miscItem.VinylItem;
import necesse.inventory.item.placeableItem.StonePlaceableItem;
import necesse.inventory.item.placeableItem.consumableItem.ConsumableItem;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.inventory.recipe.CanCraft;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import ultrastorage.UltraStorage;
import ultrastorage.objects.VaultObject;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VaultStorageContainerForm extends ContainerFormSwitcher<VaultStorageContainer> {
    public static int width = 550;

    public Form inventoryForm = this.addComponent(new Form(width, 300), (form, active) -> {
        if (!active) {
            this.label.setTyping(false);
            this.runEditUpdate();
        }
    });
    public SettlementObjectStatusFormManager settlementObjectFormManager;
    public FormLabelEdit label;
    public FormContentIconButton edit;
    public FormContentBox slotsBox;
    public FormContainerSlot[] slots;
    public LocalMessage renameTip;

    public FormLocalLabel labelSlots;

    public FormTextInput searchFilter;
    public FormTextInput searchModFilter;

    public FormFairTypeButton upgradeButton;

    public static SlotsFilterButton[] filterButtons = new SlotsFilterButton[]{
            new SlotsFilterButton("all", UltraStorage.ALL_ITEMS_FILTER),
            new SlotsFilterButton("mat", UltraStorage.MAT_ITEMS_FILTER),
            new SlotsFilterButton("weapon-melee", UltraStorage.MELEE_WEAPONS_FILTER),
            new SlotsFilterButton("weapon-range", UltraStorage.RANGE_WEAPONS_FILTER),
            new SlotsFilterButton("weapon-magic", UltraStorage.MAGIC_WEAPONS_FILTER),
            new SlotsFilterButton("weapon-summon", UltraStorage.SUMMON_WEAPONS_FILTER),
            new SlotsFilterButton("weapon-other", UltraStorage.OTHER_WEAPONS_FILTER),
            new SlotsFilterButton("tool", UltraStorage.TOOL_ITEMS_FILTER),
            new SlotsFilterButton("armor", UltraStorage.ARMOR_ITEMS_FILTER),
            new SlotsFilterButton("cosmetic", UltraStorage.COSMETIC_ITEMS_FILTER),
            new SlotsFilterButton("trinket", UltraStorage.TRINKET_ITEMS_FILTER),
            new SlotsFilterButton("mount", UltraStorage.MOUNT_ITEMS_FILTER),
            new SlotsFilterButton("arrow", UltraStorage.ARROW_ITEMS_FILTER),
            new SlotsFilterButton("bullet", UltraStorage.BULLET_ITEMS_FILTER),
            new SlotsFilterButton("seed", UltraStorage.SEED_ITEMS_FILTER),
            new SlotsFilterButton("bait", UltraStorage.BAIT_ITEMS_FILTER),
            new SlotsFilterButton("food", UltraStorage.FOOD_ITEMS_FILTER),
            new SlotsFilterButton("consumable", UltraStorage.CONSUMABLE_ITEMS_FILTER),
            new SlotsFilterButton("quest", UltraStorage.QUEST_ITEMS_FILTER),
            new SlotsFilterButton("misc", UltraStorage.MISC_ITEMS_FILTER),
            new SlotsFilterButton("object", UltraStorage.OBJECT_ITEMS_FILTER),
            new SlotsFilterButton("tile", UltraStorage.TILE_ITEMS_FILTER),
            new SlotsFilterButton("vinyl", UltraStorage.VINYL_ITEMS_FILTER)
    };

    public Map<String, FormContentIconButton> itemsFilterButtons = new HashMap<>();
    public String selectedButtonFilter = "all";

    public static int addedHeightForSearch = 86;

    public static final int buttonsPerColumn = 8;
    public static int maxColumns = (int) Math.ceil((float) filterButtons.length / buttonsPerColumn) - 1;
    public static int addedWidthForButtonFilters = maxColumns * 40 + 40 + 10;

    public static TypeParser<?>[] getParsers(FontOptions fontOptions) {
        return new TypeParser[]{TypeParsers.GAME_COLOR, TypeParsers.REMOVE_URL, TypeParsers.URL_OPEN, TypeParsers.ItemIcon(fontOptions.getSize()), TypeParsers.MobIcon(fontOptions.getSize()), TypeParsers.InputIcon(fontOptions)};
    }

    public VaultStorageContainerForm(Client client, VaultStorageContainer container) {
        super(client, container);
        OEInventory oeInventory = container.getOEInventory();
        FontOptions labelOptions = new FontOptions(20);
        this.label = this.inventoryForm.addComponent(new FormLabelEdit("", labelOptions, Settings.UI.activeTextColor, 4, 4, this.inventoryForm.getWidth() - 8, 50), -1000);
        this.label.onMouseChangedTyping((e) -> this.runEditUpdate());
        this.label.onSubmit((e) -> this.runEditUpdate());
        this.label.allowCaretSetTyping = oeInventory.canSetInventoryName();
        this.label.allowItemAppend = true;
        this.label.setParsers(getParsers(labelOptions));
        this.label.setText(oeInventory.getInventoryName().translate());
        FormFlow iconFlow = new FormFlow(this.inventoryForm.getWidth() - 4 - 20);
        this.renameTip = new LocalMessage("ui", "renamebutton");
        if (oeInventory.canSetInventoryName()) {
            this.edit = this.inventoryForm.addComponent(new FormContentIconButton(iconFlow.next(-26) - 24, 4, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.container_rename, this.renameTip));
            this.edit.onClicked((e) -> {
                this.label.setTyping(!this.label.isTyping());
                this.runEditUpdate();
            });
        }


        FormContentIconButton actionButton;

        // Quickstack Out
        actionButton = this.inventoryForm.addComponent(new FormContentIconButton(iconFlow.next(-26) - 24, 4, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.inventory_quickstack_out, new GameMessage[0]) {
            public GameTooltips getTooltips(PlayerMob perspective) {
                StringTooltips tooltips = new StringTooltips(Localization.translate("ui", "inventoryquickstack"));
                GameWindow window = WindowManager.getWindow();
                if (!window.isKeyDown(340) && !window.isKeyDown(344)) {
                    tooltips.add(Localization.translate("ui", "shiftmoreinfo"), GameColor.LIGHT_GRAY);
                } else {
                    tooltips.add(Localization.translate("ui", "inventoryquickstackinfo", "key", TypeParsers.getInputParseString(Control.INV_LOCK) + "+" + TypeParsers.getInputParseString(-100)), GameColor.LIGHT_GRAY, 400);
                }

                return tooltips;
            }
        });
        actionButton.onClicked((e) -> {
            container.quickStackButton.runAndSend();
            this.updateFilter();
        });
        actionButton.setCooldown(500);

        // Transfer All
        class TransferAllButton extends FormContentIconButton {
            TransferAllButton(int x, int y, FormInputSize size, ButtonColor color, ButtonTexture icon, GameMessage... tooltips) {
                super(x, y, size, color, icon, tooltips);
            }

            public GameTooltips getTooltips(PlayerMob perspective) {
                StringTooltips tooltips = new StringTooltips(Localization.translate("ui", "inventorytransferall"));
                GameWindow window = WindowManager.getWindow();
                if (!window.isKeyDown(340) && !window.isKeyDown(344)) {
                    tooltips.add(Localization.translate("ui", "shiftmoreinfo"), GameColor.LIGHT_GRAY);
                } else {
                    tooltips.add(Localization.translate("ui", "inventorytransferallinfo", "key", TypeParsers.getInputParseString(Control.INV_LOCK) + "+" + TypeParsers.getInputParseString(-100)), GameColor.LIGHT_GRAY, 400);
                }

                return tooltips;
            }
        }
        actionButton = this.inventoryForm.addComponent(new TransferAllButton(iconFlow.next(-26) - 24, 4, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.container_loot_all)).mirrorY();
        actionButton.onClicked((e) -> {
            container.transferAll.runAndSend();
            this.updateFilter();
        });
        actionButton.setCooldown(500);


        // Quickstack In
        actionButton = this.inventoryForm.addComponent(new FormContentIconButton(iconFlow.next(-26) - 24, 4, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.inventory_quickstack_in, new LocalMessage("ui", "inventoryrestock")));
        actionButton.onClicked((e) -> {
            container.restockButton.runAndSend();
            this.updateFilter();
        });
        actionButton.setCooldown(500);

        // Loot All
        actionButton = this.inventoryForm.addComponent(new FormContentIconButton(iconFlow.next(-26) - 24, 4, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.container_loot_all, new GameMessage[]{new LocalMessage("ui", "inventorylootall")}));
        actionButton.onClicked((e) -> {
            container.lootButton.runAndSend();
        });

        // Sort
        actionButton = this.inventoryForm.addComponent(new FormContentIconButton(iconFlow.next(-26) - 24, 4, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.inventory_sort, new GameMessage[]{new LocalMessage("ui", "inventorysort")}));
        actionButton.onClicked((e) -> {
            container.sortButton.runAndSend();
        });
        actionButton.setCooldown(500);

        this.settlementObjectFormManager = container.settlementObjectManager.getFormManager(this, this.inventoryForm, client);
        this.settlementObjectFormManager.addConfigButtonRow(this.inventoryForm, iconFlow, 4, -1);

        VaultObject vaultObject = (VaultObject) container.objectEntity.getObject();
        VaultObject nextVaultObject = vaultObject.getNextUltraChest();
        if (nextVaultObject != null) {
            int upgradeButtonWidth = 200;
            final InventoryItem upgradeItem = new InventoryItem(nextVaultObject.getObjectItem());
            LocalMessage upgradeMessage = new LocalMessage("ui", "upgradeto", "upgrade", TypeParsers.getItemParseString(upgradeItem));
            this.upgradeButton = this.inventoryForm.addComponent(new FormFairTypeButton(upgradeMessage, iconFlow.next() - upgradeButtonWidth, 4, 200, FormInputSize.SIZE_24, ButtonColor.GREEN) {
                protected void addTooltips(PlayerMob perspective) {
                    super.addTooltips(perspective);
                    ListGameTooltips tooltips = new ListGameTooltips();
                    FairType upgradeToType = new FairType();
                    FontOptions fontOptions = (new FontOptions(Settings.tooltipTextSize)).outline();
                    GameColor upgradeToColor = upgradeItem.item.getRarityColor(upgradeItem);
                    upgradeToType.append(fontOptions, Localization.translate("ui", "upgradeto", "upgrade", upgradeToColor.getColorCode() + nextVaultObject.getDisplayName()));
                    upgradeToType.applyParsers(TypeParsers.GAME_COLOR);
                    tooltips.add(new FairTypeTooltip(upgradeToType));
                    tooltips.add(new SpacerGameTooltip(10));
                    tooltips.add(Localization.translate("misc", "recipecostsing"));

                    Ingredient ingredient = nextVaultObject.ingredient;

                    CanCraft canCraft = Recipe.canCraft(new Ingredient[]{ingredient}, client.getLevel(), client.getPlayer(), container.getCraftInventories(), true);
                    tooltips.add(ingredient.getTooltips(canCraft.canCraft() ? ingredient.getIngredientAmount() : canCraft.haveIngredients[0], true));

                    GameTooltipManager.addTooltip(tooltips, GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
                }
            });
            this.upgradeButton.setParsers(TypeParsers.ItemIcon(12));
            this.upgradeButton.onClicked((e) -> {
                container.upgradeUltraChest.runAndSend();
            });
        }

        this.label.setWidth(iconFlow.next() - 8);

        this.inventoryForm.addComponent(this.slotsBox = new FormContentBox(6, 34, this.inventoryForm.getWidth() - 12 - 23, 260 - addedHeightForSearch));

        this.searchFilter = this.inventoryForm.addComponent(new FormTextInput(26, this.inventoryForm.getHeight() - 86 + 12, FormInputSize.SIZE_32_TO_40, this.inventoryForm.getWidth() - 52, this.inventoryForm.getHeight() - 20));
        this.searchFilter.placeHolder = new LocalMessage("ui", "searchtip");
        this.searchFilter.rightClickToClear = true;
        this.searchFilter.onChange((event) -> this.updateFilter());

        this.labelSlots = this.inventoryForm.addComponent(new FormLocalLabel(new StaticMessage(""), new FontOptions(10), 0, searchFilter.getX() + (this.inventoryForm.getWidth() - 52) / 2, searchFilter.getY() - 12));

        this.searchModFilter = this.inventoryForm.addComponent(new FormTextInput(26, this.inventoryForm.getHeight() - 46 + 6, FormInputSize.SIZE_32_TO_40, this.inventoryForm.getWidth() - 52, this.inventoryForm.getHeight() - 20));
        this.searchModFilter.placeHolder = new LocalMessage("ui", "searchmodtip");
        this.searchModFilter.rightClickToClear = true;
        this.searchModFilter.onChange((event) -> this.updateFilter());

        FormBreakLine breakLine = this.inventoryForm.addComponent(new FormBreakLine(FormBreakLine.ALIGN_MID, this.inventoryForm.getWidth() - 10, this.inventoryForm.getHeight() / 2, this.inventoryForm.getHeight() - 20, false));
        breakLine.color = new Color(0, 0, 0);

        this.inventoryForm.setWidth(width + addedWidthForButtonFilters);

        FormFlow buttonsIconFlow = new FormFlow(0);
        AtomicInteger inColumn = new AtomicInteger();
        AtomicInteger column = new AtomicInteger();
        Arrays.stream(filterButtons).forEach(itemFilterButtons -> {
            int actualColumn = column.get();
            int actualInRow = inColumn.get();
            if (inColumn.get() >= buttonsPerColumn) {
                actualColumn += 1;
                column.set(actualColumn);
                actualInRow = 1;
                inColumn.set(actualInRow);
                buttonsIconFlow.next(-buttonsIconFlow.next());
            } else {
                actualInRow += 1;
                inColumn.set(actualInRow);
            }

            itemsFilterButtons.put(itemFilterButtons.id, (FormContentIconButton) this.inventoryForm.addComponent(new FormContentIconButton(inventoryForm.getWidth() - addedWidthForButtonFilters + actualColumn * 40 + 6, buttonsIconFlow.next(32 + 2) + 10, FormInputSize.SIZE_32, Objects.equals(itemFilterButtons.id, "all") ? ButtonColor.GREEN : ButtonColor.BASE, itemFilterButtons.icon, new GameMessage[0]) {
                public GameTooltips getTooltips(PlayerMob perspective) {
                    return new StringTooltips(Localization.translate("ui", itemFilterButtons.id + "itemsfilter"));
                }
            }).onClicked(button -> {
                itemsFilterButtons.get(selectedButtonFilter).color = ButtonColor.BASE;
                selectedButtonFilter = itemFilterButtons.id;
                itemsFilterButtons.get(selectedButtonFilter).color = ButtonColor.GREEN;
                this.updateFilter();
            }));

        });

        this.updateFilter(true);

        this.makeCurrent(this.inventoryForm);
    }

    public void onWindowResized(GameWindow window) {
        super.onWindowResized(window);
        ContainerComponent.setPosFocus(this.inventoryForm);
        this.settlementObjectFormManager.onWindowResized();
    }

    public boolean shouldOpenInventory() {
        return true;
    }


    public void updateFilter() {
        this.updateFilter(false);
    }

    public void updateFilter(boolean firstTime) {
        PlayerMob perspective = client.getPlayer();
        ArrayList<ContainerSlot> allSlots = container.getAllSlots();


        if(firstTime) {
            labelSlots.setText(allSlots.size() + " slots");
        } else {
            container.sortButton.runAndSend();
        }

        String searchText = this.searchFilter.getText().toLowerCase();
        String searchModText = this.searchModFilter.getText().toLowerCase();

        Stream<ContainerSlot> selectedSlots = allSlots.stream().filter((containerSlot) -> {
            InventoryItem inventoryItem = containerSlot.getItem();
            if (inventoryItem == null) {
                return true;
            }
            Item item = inventoryItem.item;
            if (item == null) {
                return true;
            }
            LoadedMod mod = ItemRegistry.getItemMod(item.getID());
            if (mod == null) {
                if (!"vanilla".contains(searchModText)) {
                    return false;
                }
            } else {
                if (!mod.id.contains(searchModText)) {
                    return false;
                }
            }

            if (!Objects.equals(selectedButtonFilter, "all")) {
                if (item instanceof StonePlaceableItem) {
                    if (!Objects.equals(selectedButtonFilter, "mat")) {
                        return false;
                    }
                } else if (Objects.equals(selectedButtonFilter, "tool") || selectedButtonFilter.startsWith("weapon-")) {
                    if (!UltraStorage.isRequiredToolType(selectedButtonFilter, item, perspective)) {
                        return false;
                    }
                } else if (Objects.equals(selectedButtonFilter, "armor") || Objects.equals(selectedButtonFilter, "cosmetic")) {
                    if (!(item instanceof ArmorItem)) {
                        return false;
                    }
                    if (UltraStorage.isCosmetic((ArmorItem) item, perspective) != Objects.equals(selectedButtonFilter, "cosmetic")) {
                        return false;
                    }
                } else if (Arrays.asList(UltraStorage.miscSeparatedItems).contains(selectedButtonFilter)) {
                    if (item.type != Item.Type.MISC) {
                        return false;
                    }
                    if (item instanceof ConsumableItem) {
                        if (!Objects.equals(selectedButtonFilter, "consumable")) {
                            return false;
                        }
                    } else if (item instanceof ObjectItem) {
                        if (!Objects.equals(selectedButtonFilter, "object")) {
                            return false;
                        }
                    } else if (item instanceof TileItem) {
                        if (!Objects.equals(selectedButtonFilter, "tile")) {
                            return false;
                        }
                    } else if (item instanceof VinylItem) {
                        if (!Objects.equals(selectedButtonFilter, "vinyl")) {
                            return false;
                        }
                    } else {
                        if (!Objects.equals(selectedButtonFilter, "misc")) {
                            return false;
                        }
                    }
                } else if (item.type != Item.Type.valueOf(selectedButtonFilter.toUpperCase())) {
                    return false;
                }
            }

            return item.getStringID().toLowerCase().contains(searchText) || item.getDisplayName(item.getDefaultItem(client.getPlayer(), 1)).toLowerCase().contains(searchText);
        });

        this.slotsBox.clearComponents();
        FormFlow flow = new FormFlow(34);
        this.addSlots(flow, selectedSlots.collect(Collectors.toList()));
        this.slotsBox.setContentBox(new Rectangle(6, 34, slotsBox.getWidth(), flow.next()));
    }

    public static class SlotsFilterButton {
        String id;
        ButtonTexture icon;

        public SlotsFilterButton(String id, ButtonTexture icon) {
            this.id = id;
            this.icon = icon;
        }
    }

    private void runEditUpdate() {
        OEInventory oeInventory = this.container.getOEInventory();
        if (oeInventory.canSetInventoryName()) {
            if (this.label.isTyping()) {
                this.edit.setIcon(Settings.UI.container_rename_save);
                this.renameTip = new LocalMessage("ui", "savebutton");
            } else {
                if (!this.label.getText().equals(oeInventory.getInventoryName().translate())) {
                    oeInventory.setInventoryName(this.label.getText());
                    this.container.renameButton.runAndSend(this.label.getText());
                }

                this.edit.setIcon(Settings.UI.container_rename);
                this.renameTip = new LocalMessage("ui", "renamebutton");
                this.label.setText(oeInventory.getInventoryName().translate());
            }

            this.edit.setTooltips(this.renameTip);
        }
    }

    public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
        this.settlementObjectFormManager.updateButtons();
        super.draw(tickManager, perspective, renderBox);
    }

    protected void addSlots(FormFlow flow, List<ContainerSlot> itemSlots) {
        this.slots = new FormContainerSlot[itemSlots.size()];
        int currentY = flow.next();

        for (int i = 0; i < itemSlots.size(); i++) {
            ContainerSlot containerSlot = itemSlots.get(i);

            int x = i % 12;
            if (x == 0) {
                currentY = flow.next(40);
            }

            this.slots[i] = this.slotsBox.addComponent(new FormContainerVaultSlot(this.client, this.container, containerSlot.getContainerIndex(), 28 + x * 40, currentY));
        }
    }
}