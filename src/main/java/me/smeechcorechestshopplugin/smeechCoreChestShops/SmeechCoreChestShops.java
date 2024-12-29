package me.smeechcorechestshopplugin.smeechCoreChestShops;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmeechCoreChestShops extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Load or create the configuration file
        getLogger().info("SmeechCoreChestShops Plugin Enabled");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("SmeechCoreChestShops Plugin Disabled");
    }

    // Event for creating the shop chest using a sign
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();

        if ("[PRICE]".equalsIgnoreCase(lines[0])) {
            Player player = event.getPlayer();
            try {
                int price = Integer.parseInt(lines[1]); // Parse price from line 2
                if (price <= 0 || price > 64) {
                    player.sendMessage(getConfig().getString("messages.invalid-sign", "Invalid price. Price must be between 1 and 64."));
                    event.setCancelled(true);
                    return;
                }

                Material costMaterial = Material.matchMaterial(lines[2]); // Parse material from line 3
                if (costMaterial == null) {
                    player.sendMessage(getConfig().getString("messages.invalid-sign", "Invalid item specified on line 3."));
                    event.setCancelled(true);
                    return;
                }

                event.setLine(0, "[PRICE]"); // Set the first line to [PRICE] without color code
                event.setLine(3, player.getName()); // Set the shop owner
                player.sendMessage(getConfig().getString("messages.shop-created", "Shop chest successfully created!"));
            } catch (NumberFormatException e) {
                event.getPlayer().sendMessage(getConfig().getString("messages.invalid-sign", "Invalid number for price. Please enter a valid number."));
                event.setCancelled(true);
            }
        }
    }

    // Event for interacting with the shop chest
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.CHEST) return;

        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.UP || face == BlockFace.DOWN) continue; // Skip top and bottom
            Block relativeBlock = clickedBlock.getRelative(face);
            if (relativeBlock.getState() instanceof Sign sign) {
                handleShopInteraction(event, clickedBlock, sign);
                return;
            }
        }
    }

    private void handleShopInteraction(PlayerInteractEvent event, Block chestBlock, Sign sign) {
        String[] lines = sign.getLines();
        if ("[PRICE]".equalsIgnoreCase(lines[0])) {
            String owner = lines[3];
            int price = parsePrice(lines[1], event.getPlayer());
            if (price == -1) return;

            Material costMaterial = Material.matchMaterial(lines[2]);
            if (costMaterial == null) {
                event.getPlayer().sendMessage("Invalid item in the shop.");
                return;
            }

            Inventory chestInventory = ((Chest) chestBlock.getState()).getInventory();

            Player player = event.getPlayer();
            if (player.getName().equals(owner)) {
                player.sendMessage("You are the shop owner. You can access the chest.");
                player.openInventory(chestInventory);
            } else {
                handlePurchase(player, chestInventory, costMaterial, price);
            }
        }
    }

    private int parsePrice(String priceString, Player player) {
        try {
            return Integer.parseInt(priceString);
        } catch (NumberFormatException e) {
            player.sendMessage(getConfig().getString("messages.invalid-sign", "Invalid price on the sign."));
            return -1; // Return -1 to indicate failure
        }
    }

    private void handlePurchase(Player buyer, Inventory chestInventory, Material costMaterial, int price) {
        Inventory buyerInventory = buyer.getInventory();
        ItemStack costItem = new ItemStack(costMaterial, price);

        if (!buyerInventory.containsAtLeast(costItem, price)) {
            buyer.sendMessage(getConfig().getString("messages.not-enough-currency", "You do not have enough " + costMaterial.name() + " to make this purchase.").replace("%currency%", costMaterial.name()));
            return;
        }

        ItemStack purchasedItem = chestInventory.getItem(0); // Get the first available item
        if (purchasedItem == null || purchasedItem.getAmount() < 1) {
            buyer.sendMessage(getConfig().getString("messages.shop-out-of-stock", "This shop is out of stock."));
            return;
        }

        // Perform the transaction
        buyerInventory.removeItem(costItem); // Remove cost items from buyer
        chestInventory.removeItem(purchasedItem); // Remove purchased item from chest
        buyer.getInventory().addItem(purchasedItem); // Add purchased item to buyer
        buyer.sendMessage(getConfig().getString("messages.purchase-successful", "You successfully purchased %item% for %price% %currency%!")
                .replace("%item%", purchasedItem.getType().name())
                .replace("%price%", String.valueOf(price))
                .replace("%currency%", costMaterial.name()));
    }
}
