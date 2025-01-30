package ultrastorage.objects;

import necesse.engine.localization.Localization;
import necesse.engine.network.packet.PacketPlaceObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.levelEvent.SmokePuffCloudLevelEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.Inventory;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Ingredient;
import necesse.level.gameObject.furniture.InventoryObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.multiTile.MultiTile;
import ultrastorage.UltraStorage;
import ultrastorage.containers.VaultStorageContainer;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class VaultObject extends InventoryObject {
    public String stringId;
    public Ingredient ingredient;
    public int position;

    private VaultObject(String stringId, Color color, Ingredient ingredient, String textureName, int slots, boolean isWood) {
        super(textureName, slots + 1, new Rectangle(4, 4, 24, 24), isWood ? ToolType.ALL : ToolType.PICKAXE, color);
        this.stringId = stringId;
        this.ingredient = ingredient;
    }

    public void interact(Level level, int x, int y, PlayerMob player) {
        if (level.isServer()) {
            ObjectEntity ent = level.entityManager.getObjectEntity(x, y);
            if (ent != null && ent.implementsOEUsers() && ((OEUsers) ent).isInUse()) {
                player.getServerClient().sendChatMessage(Localization.translate("message", "cannotopenifitsopen"));
            } else {
                VaultStorageContainer.openAndSendContainer(UltraStorage.ULTRA_STORAGE_CONTAINER, player.getServerClient(), level, x, y);
            }
        }
    }

    public VaultObject getNextUltraChest() {
        if (position >= 0 && position + 1 < UltraStorage.vaultObjectList.size()) {
            return UltraStorage.vaultObjectList.get(position + 1);
        }
        return null;
    }

    public static int register(String storageID, Color color, String texture, Ingredient ingredient, int slots, float brokerValue, boolean isWood) {
        VaultObject vaultObject = new VaultObject(storageID, color, ingredient, texture, slots, isWood);
        vaultObject.position = UltraStorage.vaultObjectList.size();
        UltraStorage.vaultObjectList.add(vaultObject);
        return ObjectRegistry.registerObject(storageID, vaultObject, brokerValue, true);
    }

    public static int register(String storageID, Color color, Ingredient ingredient, int slots, float brokerValue, boolean isWood) {
        return register(storageID, color, storageID, ingredient, slots, brokerValue, isWood);
    }

    public static int register(String storageID, Color color, Ingredient ingredient, int slots, boolean isWood) {
        return register(storageID, color, ingredient, slots, -1F, isWood);
    }

    public static int register(String storageID, Color color, Ingredient ingredient, int slots, float brokerValue) {
        return register(storageID, color, ingredient, slots, brokerValue, false);
    }

    public static int register(String storageID, Color color, Ingredient ingredient, int slots) {
        return register(storageID, color, ingredient, slots, -1F);
    }

    public void performUpgrade(VaultObject newVaultObject, Level level, int tileX, int tileY, ServerClient serverClient, Inventory inventory) {
        int rotation = level.getObjectRotation(tileX, tileY);
        MultiTile multiTile = this.getMultiTile(level, 0, tileX, tileY);
        ArrayList<LevelObject> lastObjects = multiTile.streamObjects(tileX, tileY).filter((e) -> level.getObjectID(e.tileX, e.tileY) == e.value.getID()).map((e) -> level.getLevelObject(e.tileX, e.tileY)).collect(Collectors.toCollection(ArrayList::new));

        for (LevelObject lastObject : lastObjects) {
            level.setObject(lastObject.tileX, lastObject.tileY, 0);
        }

        newVaultObject.customPlaceObject(level, 0, tileX, tileY, rotation, true, inventory);
        level.getServer().network.sendToClientsWithTile(new PacketPlaceObject(level, null, 0, tileX, tileY, newVaultObject.getID(), rotation, true), level, tileX, tileY);
        Rectangle levelRectangle = newVaultObject.getMultiTile(rotation).getLevelRectangle(tileX, tileY);
        level.entityManager.addLevelEvent(new SmokePuffCloudLevelEvent(levelRectangle, newVaultObject.mapColor));

        for (LevelObject lastObject : lastObjects) {
            level.sendObjectUpdatePacket(lastObject.tileX, lastObject.tileY);
        }

    }

    public void customPlaceObject(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, Inventory inventory) {
        level.objectLayer.getObject(layerID, x, y).onPlacedOn(level, layerID, x, y, this);
        level.objectLayer.setObject(layerID, x, y, this.getID());
        level.objectLayer.setObjectRotation(layerID, x, y, (byte) rotation);
        level.objectLayer.setIsPlayerPlaced(layerID, x, y, byPlayer);
        if (layerID == 0 && level.isServer()) {
            InventoryObjectEntity objectEntity = (InventoryObjectEntity) this.getNewObjectEntity(level, x, y);
            if (objectEntity != null) {
                inventory.streamSlots().filter(slot -> slot != null && !slot.isSlotClear()).forEach(
                        slot -> objectEntity.inventory.setItem(slot.slot, slot.getItem())
                );
                level.entityManager.objectEntities.add(objectEntity);
            } else {
                inventory.streamSlots().filter(slot -> slot != null && !slot.isSlotClear()).forEach(
                        slot -> {
                            ItemPickupEntity itemPickup = new ItemPickupEntity(level, slot.getItem(), x * 32 + 16, y * 32 + 16, 0, 0);
                            level.entityManager.pickups.add(itemPickup);

                        }
                );
            }
        }

        MultiTile multiTile = this.getMultiTile(rotation);
        if (multiTile.isMaster) {
            multiTile.streamOtherObjects(x, y).forEach((e) -> e.value.placeObject(level, layerID, e.tileX, e.tileY, rotation, byPlayer));
        }

    }
}