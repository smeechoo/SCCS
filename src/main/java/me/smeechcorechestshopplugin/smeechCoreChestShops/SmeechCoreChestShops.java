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
        getLogger().info("Smeechcorechestshop Plugin Enabled");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Smeechcorechestshop Plugin Disabled");
    }

    // Event for creating the shop chest using a sign
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines(); // Use array to handle all lines at once

        // Check if the sign contains [PRICE]
        if ("[PRICE]".equalsIgnoreCase(lines[0])) {
            Player player = event.getPlayer();
            try {
                // Parse the price from line 2
                int price = Integer.parseInt(lines[1]);
                if (price <= 0 || price > 64) {
                    player.sendMessage("Invalid price. Price must be between 1 and 64.");
                    event.setCancelled(true);
                    return;
                }

                // Get the item cost from line 3
                Material costMaterial = Material.matchMaterial(lines[2]);
                if (costMaterial == null) {
                    player.sendMessage("Invalid item specified on line 3.");
                    event.setCancelled(true);
                    return;
                }

                // Set up the sign
                event.setLine(0, "[PRICE]");  // Set the first line to [PRICE] without color code
                event.setLine(3, player.getName());  // Set the shop owner
                player.sendMessage("Shop chest successfully created!");
            } catch (NumberFormatException e) {
                event.getPlayer().sendMessage("Invalid number for price. Please enter a valid number.");
                event.setCancelled(true);
            }
        }
    }

    // Event for interacting with the shop chest
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return; // Ensure block is not null

        // Ensure the clicked block is a chest
        if (clickedBlock.getType() == Material.CHEST) {
            // Check all sides of the chest for a sign
            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.UP || face == BlockFace.DOWN) continue; // Skip top and bottom
                Block relativeBlock = clickedBlock.getRelative(face);
                if (relativeBlock.getState() instanceof Sign sign) {
                    handleShopInteraction(event, clickedBlock, sign);
                    return;
                }
            }
        }
    }

    private void handleShopInteraction(PlayerInteractEvent event, Block chestBlock, Sign sign) {
        String[] lines = sign.getLines(); // Get sign lines as an array

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
                handlePurchase(player, chestInventory, costMaterial, price, event);
            }
        }
    }

    private int parsePrice(String priceString, Player player) {
        try {
            return Integer.parseInt(priceString);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid price on the sign.");
            return -1; // Return -1 to indicate failure
        }
    }

    private void handlePurchase(Player buyer, Inventory chestInventory, Material costMaterial, int price, PlayerInteractEvent event) {
        Inventory buyerInventory = buyer.getInventory();
        ItemStack costItem = new ItemStack(costMaterial, price);

        // Block the buyer from taking an item without purchasing
        event.setCancelled(true); // Cancel the default chest interaction

        if (!buyerInventory.containsAtLeast(costItem, price)) {
            buyer.sendMessage("You do not have enough " + costMaterial.name() + " to make this purchase.");
            return;
        }

        // Check if the item is available in the shop
        ItemStack purchasedItem = chestInventory.getItem(0); // Get the first available item
        if (purchasedItem == null || purchasedItem.getAmount() < 1) {
            buyer.sendMessage("The shop is out of stock.");
            return;
        }

        // Perform the transaction
        buyerInventory.removeItem(costItem); // Remove cost items from buyer
        chestInventory.removeItem(purchasedItem); // Remove purchased item from chest
        buyer.getInventory().addItem(purchasedItem); // Add purchased item to buyer
        buyer.sendMessage("You successfully purchased " + purchasedItem.getType().name() + " for " + price + " " + costMaterial.name() + "!");
    }
}
