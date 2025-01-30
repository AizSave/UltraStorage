package ultrastorage;

import necesse.engine.Settings;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.network.PacketReader;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.gameDamageType.DamageType;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.ui.ButtonIcon;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.item.toolItem.miscToolItem.NetToolItem;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import ultrastorage.containers.VaultStorageContainer;
import ultrastorage.containers.VaultStorageContainerForm;
import ultrastorage.objects.VaultObject;

import java.awt.*;
import java.util.ArrayList;

@ModEntry
public class UltraStorage {
    public static int ULTRA_STORAGE_CONTAINER;

    public static ArrayList<VaultObject> vaultObjectList = new ArrayList<>();

    public static String[] anyChest = new String[] {
            "storagebox", "demonchest", "oakchest", "sprucechest", "pinechest", "palmchest",
            "maplechest", "birchchest", "dungeonchest", "bonechest", "deadwoodchest"
    };

    public void init() {

        // Containers
        ULTRA_STORAGE_CONTAINER = ContainerRegistry.registerOEContainer((client, uniqueSeed, oe, content) -> new VaultStorageContainerForm(client, new VaultStorageContainer(client.getClient(), uniqueSeed, (OEInventory) oe, new PacketReader(content))), (client, uniqueSeed, oe, content, serverObject) -> new VaultStorageContainer(client, uniqueSeed, (OEInventory) oe, new PacketReader(content)));

        // Ultra Chests
        VaultObject.register("woodvault", new Color(97, 39, 3), null, 80, true);
        VaultObject.register("coppervault", new Color(191, 90, 62), new Ingredient("copperbar", 4), 100);
        VaultObject.register("ironvault", new Color(130, 139, 152), new Ingredient("ironbar", 3), 125);
        VaultObject.register("goldvault", new Color(233, 134, 39	), new Ingredient("goldbar", 2), 150);
        VaultObject.register("demonicvault", new Color(62, 59, 88), new Ingredient("demonicbar", 5), 175);
        VaultObject.register("ivyvault", new Color(57, 77, 60), new Ingredient("ivybar", 2), 200);
        VaultObject.register("quartzvault", new Color(174, 161, 137	), new Ingredient("quartz", 5), 240);
        VaultObject.register("tungstenvault", new Color(67, 69, 73	), new Ingredient("tungstenbar", 2), 280);
        VaultObject.register("glacialvault", new Color(50, 140, 167), new Ingredient("glacialbar", 2), 420);
        VaultObject.register("myceliumvault", new Color(182, 77, 70), new Ingredient("myceliumbar", 2), 460);
        VaultObject.register("ancientvault", new Color(85, 55, 27), new Ingredient("ancientfossilbar", 2), 500);

        // Any Chest Ingredient
        for (String chest : anyChest) {
            ItemRegistry.getItem(chest).addGlobalIngredient("anychest");
        }
    }

    public void postInit() {

        // Recipes Registry
        for (int i = 0; i < vaultObjectList.size(); i++) {
            VaultObject vaultObject = vaultObjectList.get(i);
            String antRecipe = i == 0 ? null : vaultObjectList.get(i - 1).stringId;

            ArrayList<Ingredient> ingredients = new ArrayList<>();
            ingredients.add(new Ingredient("anychest", 5));
            if(vaultObject.ingredient != null) {
                ingredients.add(vaultObject.ingredient);
            }
            Recipes.registerModRecipe(
                    new Recipe(vaultObject.stringId, RecipeTechRegistry.WORKSTATION, ingredients.toArray(new Ingredient[]{}))
            );
            if(antRecipe != null) {
                ingredients.remove(0);
                ingredients.add(new Ingredient(antRecipe, 1));
                Recipes.registerModRecipe(
                        new Recipe(vaultObject.stringId, RecipeTechRegistry.WORKSTATION, ingredients.toArray(new Ingredient[]{}))
                );
            }
        }

    }

    public static ButtonIcon ALL_ITEMS_FILTER;
    public static ButtonIcon MAT_ITEMS_FILTER;
    public static ButtonIcon MELEE_WEAPONS_FILTER;
    public static ButtonIcon RANGE_WEAPONS_FILTER;
    public static ButtonIcon MAGIC_WEAPONS_FILTER;
    public static ButtonIcon SUMMON_WEAPONS_FILTER;
    public static ButtonIcon OTHER_WEAPONS_FILTER;
    public static ButtonIcon TOOL_ITEMS_FILTER;
    public static ButtonIcon ARMOR_ITEMS_FILTER;
    public static ButtonIcon COSMETIC_ITEMS_FILTER;
    public static ButtonIcon TRINKET_ITEMS_FILTER;
    public static ButtonIcon MOUNT_ITEMS_FILTER;
    public static ButtonIcon ARROW_ITEMS_FILTER;
    public static ButtonIcon BULLET_ITEMS_FILTER;
    public static ButtonIcon SEED_ITEMS_FILTER;
    public static ButtonIcon BAIT_ITEMS_FILTER;
    public static ButtonIcon FOOD_ITEMS_FILTER;
    public static ButtonIcon CONSUMABLE_ITEMS_FILTER;
    public static ButtonIcon QUEST_ITEMS_FILTER;
    public static ButtonIcon MISC_ITEMS_FILTER;
    public static ButtonIcon OBJECT_ITEMS_FILTER;
    public static ButtonIcon TILE_ITEMS_FILTER;
    public static ButtonIcon VINYL_ITEMS_FILTER;

    public void initResources() {
        ALL_ITEMS_FILTER = new ButtonIcon(Settings.UI, "allitems");
        MAT_ITEMS_FILTER = new ButtonIcon(Settings.UI, "matitems");
        MELEE_WEAPONS_FILTER = new ButtonIcon(Settings.UI, "meleeweapons");
        RANGE_WEAPONS_FILTER = new ButtonIcon(Settings.UI, "rangeweapons");
        MAGIC_WEAPONS_FILTER = new ButtonIcon(Settings.UI, "magicweapons");
        SUMMON_WEAPONS_FILTER = new ButtonIcon(Settings.UI, "summonweapons");
        OTHER_WEAPONS_FILTER = new ButtonIcon(Settings.UI, "otherweapons");
        TOOL_ITEMS_FILTER = new ButtonIcon(Settings.UI, "toolitems");
        ARMOR_ITEMS_FILTER = new ButtonIcon(Settings.UI, "armoritems");
        COSMETIC_ITEMS_FILTER = new ButtonIcon(Settings.UI, "cosmeticitems");
        TRINKET_ITEMS_FILTER = new ButtonIcon(Settings.UI, "trinketitems");
        MOUNT_ITEMS_FILTER = new ButtonIcon(Settings.UI, "mountitems");
        ARROW_ITEMS_FILTER = new ButtonIcon(Settings.UI, "arrowitems");
        BULLET_ITEMS_FILTER = new ButtonIcon(Settings.UI, "bulletitems");
        SEED_ITEMS_FILTER = new ButtonIcon(Settings.UI, "seeditems");
        BAIT_ITEMS_FILTER = new ButtonIcon(Settings.UI, "baititems");
        FOOD_ITEMS_FILTER = new ButtonIcon(Settings.UI, "fooditems");
        CONSUMABLE_ITEMS_FILTER = new ButtonIcon(Settings.UI, "consumableitems");
        QUEST_ITEMS_FILTER = new ButtonIcon(Settings.UI, "questitems");
        MISC_ITEMS_FILTER = new ButtonIcon(Settings.UI, "miscitems");
        OBJECT_ITEMS_FILTER = new ButtonIcon(Settings.UI, "objectitems");
        TILE_ITEMS_FILTER = new ButtonIcon(Settings.UI, "tileitems");
        VINYL_ITEMS_FILTER = new ButtonIcon(Settings.UI, "vinylitems");
    }

    public static boolean isRequiredToolType(String requiredType, Item item, PlayerMob perspective) {
        return isRequiredToolType(requiredType, item.getDefaultItem(perspective, 1));
    }

    public static boolean isRequiredToolType(String requiredType, InventoryItem item) {
        if (!(item.item instanceof ToolItem)) return false;
        return getToolType((ToolItem) item.item, item).equals(requiredType);
    }

    public static String getToolType(ToolItem toolItem, InventoryItem item) {
        if (toolItem instanceof ToolDamageItem || toolItem instanceof NetToolItem) {
            return "tool";
        } else {
            DamageType damageType = toolItem.getDamageType(item);
            if (damageType == DamageTypeRegistry.MELEE) {
                return "weapon-melee";
            } else if (damageType == DamageTypeRegistry.RANGED) {
                return "weapon-range";
            } else if (damageType == DamageTypeRegistry.MAGIC) {
                return "weapon-magic";
            } else if (damageType == DamageTypeRegistry.SUMMON) {
                return "weapon-summon";
            } else {
                return "weapon-other";
            }
        }
    }

    public static boolean isCosmetic(ArmorItem armorItem, PlayerMob perspective) {
        return isCosmetic(armorItem, armorItem.getDefaultItem(perspective, 1));
    }

    public static boolean isCosmetic(ArmorItem armorItem, InventoryItem item) {
        return armorItem.getFlatArmorValue(item) == 0;
    }

    public static String[] miscSeparatedItems = new String[]{"misc", "consumable", "object", "tile", "vinyl"};
}
