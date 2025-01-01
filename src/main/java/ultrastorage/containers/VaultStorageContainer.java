package ultrastorage.containers;

import necesse.engine.GameTileRange;
import necesse.engine.Settings;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketOEInventoryNameUpdate;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.SlotIndexRange;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.inventory.container.customAction.PointCustomAction;
import necesse.inventory.container.customAction.StringCustomAction;
import necesse.inventory.container.settlement.SettlementContainerObjectStatusManager;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.container.slots.OEInventoryContainerSlot;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.Level;
import ultrastorage.objects.VaultObject;

import java.util.*;

public class VaultStorageContainer extends SettlementDependantContainer {
    public StringCustomAction renameButton;
    public EmptyCustomAction quickStackButton;
    public EmptyCustomAction transferAll;
    public EmptyCustomAction restockButton;
    public final OEInventory oeInventory;
    public final ObjectEntity objectEntity;
    public final OEUsers oeUsers;
    public int INVENTORY_START = -1;
    public int INVENTORY_END = -1;
    public SettlementContainerObjectStatusManager settlementObjectManager;

    public PointCustomAction moveItem;
    public IntCustomAction lootSlot;

    public EmptyCustomAction sortAction;
    public final EmptyCustomAction upgradeUltraChest;

    private final LinkedHashSet<Inventory> nearbyInventories = new LinkedHashSet();

    public VaultStorageContainer(final NetworkClient client, int uniqueSeed, final OEInventory oeInventory, PacketReader reader) {
        super(client, uniqueSeed);
        this.oeInventory = oeInventory;
        this.objectEntity = (ObjectEntity) oeInventory;

        VaultObject vaultObject = (VaultObject) objectEntity.getObject();

        oeInventory.triggerInteracted();
        this.oeUsers = this.objectEntity instanceof OEUsers ? (OEUsers) this.objectEntity : null;
        if (client.isServer() & this.oeUsers != null) {
            this.oeUsers.startUser(client.playerMob);
        }

        this.nearbyInventories.addAll(this.craftInventories);
        this.nearbyInventories.add(this.oeInventory.getInventory());

        for (InventoryRange inventoryRange : this.getNearbyInventories(getLevel(), objectEntity.getX(), objectEntity.getY(), new GameTileRange(9, vaultObject.getMultiTile(0).getTileRectangle(0, 0)), OEInventory::canUseForNearbyCrafting)) {
            this.nearbyInventories.add(inventoryRange.inventory);
        }

        this.settlementObjectManager = new SettlementContainerObjectStatusManager(this, this.objectEntity.getLevel(), this.objectEntity.getX(), this.objectEntity.getY(), reader);
        InventoryRange inventoryRange = this.getOEInventoryRange();

        for (int i = inventoryRange.startSlot; i <= inventoryRange.endSlot; ++i) {
            int index = this.addSlot(this.getOEContainerSlot(oeInventory, i));
            if (this.INVENTORY_START == -1) {
                this.INVENTORY_START = index;
            }

            if (this.INVENTORY_END == -1) {
                this.INVENTORY_END = index;
            }

            this.INVENTORY_START = Math.min(this.INVENTORY_START, index);
            this.INVENTORY_END = Math.max(this.INVENTORY_END, index);
        }

        this.addInventoryQuickTransfer(this.INVENTORY_START, this.INVENTORY_END);
        this.renameButton = this.registerAction(new StringCustomAction() {
            protected void run(String value) {
                if (oeInventory.canSetInventoryName()) {
                    oeInventory.setInventoryName(value);
                    if (client.isServer()) {
                        client.getServerClient().getServer().network.sendToClientsWithEntity(new PacketOEInventoryNameUpdate(oeInventory, value), VaultStorageContainer.this.objectEntity);
                    }
                }

            }
        });
        this.quickStackButton = this.registerAction(new EmptyCustomAction() {
            protected void run() {
                if (oeInventory.canQuickStackInventory()) {
                    ArrayList<InventoryRange> targets = new ArrayList(Collections.singleton(VaultStorageContainer.this.getOEInventoryRange()));
                    VaultStorageContainer.this.quickStackToInventories(targets, client.playerMob.getInv().main);
                }

            }
        });
        this.transferAll = this.registerAction(new EmptyCustomAction() {
            protected void run() {
                for (int i = VaultStorageContainer.this.CLIENT_INVENTORY_START; i <= VaultStorageContainer.this.CLIENT_INVENTORY_END; ++i) {
                    if (!VaultStorageContainer.this.getSlot(i).isItemLocked()) {
                        VaultStorageContainer.this.transferToSlots(VaultStorageContainer.this.getSlot(i), VaultStorageContainer.this.INVENTORY_START, VaultStorageContainer.this.INVENTORY_END, "transferall");
                    }
                }
            }
        });
        this.restockButton = this.registerAction(new EmptyCustomAction() {
            protected void run() {
                ArrayList<InventoryRange> targets = new ArrayList(Collections.singleton(VaultStorageContainer.this.getOEInventoryRange()));
                VaultStorageContainer.this.restockFromInventories(targets, client.playerMob.getInv().main);
            }
        });

        this.lootSlot = this.registerAction(new IntCustomAction() {
            protected void run(int i) {
                if (!VaultStorageContainer.this.getSlot(i).isItemLocked()) {
                    VaultStorageContainer.this.transferToSlots(VaultStorageContainer.this.getSlot(i), Arrays.asList(new SlotIndexRange(VaultStorageContainer.this.CLIENT_HOTBAR_START, VaultStorageContainer.this.CLIENT_HOTBAR_END), new SlotIndexRange(VaultStorageContainer.this.CLIENT_INVENTORY_START, VaultStorageContainer.this.CLIENT_INVENTORY_END)), "lootall");
                }
            }
        });

        this.moveItem = this.registerAction(new PointCustomAction() {
            protected void run(int from, int to) {
                VaultStorageContainer.this.getSlot(to).setItem(VaultStorageContainer.this.getSlot(from).getItem());
                VaultStorageContainer.this.getSlot(from).setItem(null);
            }
        });

        this.sortAction = this.registerAction(new EmptyCustomAction() {
            protected void run() {
                if (oeInventory.canSortInventory()) {
                    InventoryRange range = VaultStorageContainer.this.getOEInventoryRange();
                    range.inventory.sortItems(client.playerMob.getLevel(), client.playerMob, range.startSlot + 1, range.endSlot);
                }

            }
        });

        this.upgradeUltraChest = this.registerAction(new EmptyCustomAction() {
            protected void run() {
                VaultObject vaultObject = (VaultObject) objectEntity.getObject();
                VaultObject nextVaultObject = vaultObject.getNextUltraChest();

                if (nextVaultObject != null) {
                    Collection<Inventory> craftInventories = VaultStorageContainer.this.getCraftInventories();
                    if (VaultStorageContainer.this.canCraftRecipe(new Ingredient[]{nextVaultObject.ingredient}, craftInventories, true).canCraft()) {
                        if (client.isServer()) {
                            vaultObject.performUpgrade(nextVaultObject, objectEntity.getLevelObject().level, objectEntity.getLevelObject().tileX, objectEntity.getLevelObject().tileY, client.getServerClient(), oeInventory.getInventory());
                        }

                        Recipe.craft(new Ingredient[]{nextVaultObject.ingredient}, client.playerMob.getLevel(), client.playerMob, craftInventories);
                    }

                    VaultStorageContainer.this.close();
                }
            }
        });

    }

    private boolean useNearbyInventories() {
        return this.client.isServer() ? this.client.craftingUsesNearbyInventories : (Boolean) Settings.craftingUseNearby.get();
    }

    public Collection<Inventory> getCraftInventories() {
        return this.useNearbyInventories() ? this.nearbyInventories : super.getCraftInventories();
    }

    public ArrayList<ContainerSlot> getAllSlots() {
        ArrayList<ContainerSlot> containerSlots = new ArrayList<>();
        for (int i = VaultStorageContainer.this.INVENTORY_START + 1; i <= VaultStorageContainer.this.INVENTORY_END; ++i) {
            containerSlots.add(VaultStorageContainer.this.getSlot(i));
        }
        return containerSlots;
    }

    public ContainerSlot getOEContainerSlot(OEInventory oeInventory, int slot) {
        return new OEInventoryContainerSlot(oeInventory, slot);
    }

    public void quickStackControlPressed() {
        this.quickStackButton.runAndSend();
    }

    public void tick() {
        super.tick();
        if (this.client.isServer() & this.oeUsers != null) {
            this.oeUsers.startUser(this.client.playerMob);
        }

    }

    protected Level getLevel() {
        return this.objectEntity.getLevel();
    }

    public boolean isValid(ServerClient client) {
        if (!super.isValid(client)) {
            return false;
        } else {
            Level level = client.getLevel();
            return !this.objectEntity.removed() && level.getObject(this.objectEntity.getX(), this.objectEntity.getY()).inInteractRange(level, this.objectEntity.getX(), this.objectEntity.getY(), client.playerMob);
        }
    }

    public OEInventory getOEInventory() {
        return this.oeInventory;
    }

    public InventoryRange getOEInventoryRange() {
        return new InventoryRange(this.oeInventory.getInventory());
    }

    public void onClose() {
        super.onClose();
        if (this.client.isServer() & this.oeUsers != null) {
            this.oeUsers.stopUser(this.client.playerMob);
        }

    }

    public static void openAndSendContainer(int containerID, ServerClient client, Level level, int tileX, int tileY, Packet extraContent) {
        if (!level.isServer()) {
            throw new IllegalStateException("Level must be a server level");
        } else {
            Packet packet = new Packet();
            PacketWriter writer = new PacketWriter(packet);
            SettlementContainerObjectStatusManager.writeContent(client, level, tileX, tileY, writer);
            if (extraContent != null) {
                writer.putNextContentPacket(extraContent);
            }

            ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
            PacketOpenContainer p = PacketOpenContainer.ObjectEntity(containerID, objectEntity, packet);
            ContainerRegistry.openAndSendContainer(client, p);
        }
    }

    public static void openAndSendContainer(int containerID, ServerClient client, Level level, int tileX, int tileY) {
        openAndSendContainer(containerID, client, level, tileX, tileY, (Packet) null);
    }
}