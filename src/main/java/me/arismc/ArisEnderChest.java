package me.arismc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ArisEnderChest extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ec").setExecutor(new ECCommand());
    }

    public String format(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private class ECCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) return true;

            if (!player.hasPermission("arisenderchest.ec")) {
                player.sendMessage(format(getConfig().getString("messages.no-permission")));
                return true;
            }

            openCustomEnderChest(player);
            return true;
        }
    }

    private void openCustomEnderChest(Player player) {
        // Quyền arisenderchest.3.9 -> 6 hàng (54 ô), mặc định -> 3 hàng (27 ô)
        int rows = player.hasPermission("arisenderchest.3.9") ? 6 : 3;
        int size = rows * 9;

        String title = format(getConfig().getString("inventory-title"));
        Inventory customEc = Bukkit.createInventory(player, size, title);

        // Copy item từ EnderChest gốc của hệ thống Minecraft
        ItemStack[] currentItems = player.getEnderChest().getContents();
        for (int i = 0; i < Math.min(size, currentItems.length); i++) {
            customEc.setItem(i, currentItems[i]);
        }

        player.openInventory(customEc);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = format(getConfig().getString("inventory-title"));
        if (event.getView().getTitle().equals(title)) {
            ItemStack[] newContents = event.getInventory().getContents();

            // Thực thi trên Region của người chơi (Tối ưu Ryzen 9 / Folia)
            Bukkit.getRegionScheduler().execute(this, player.getLocation(), () -> {
                player.getEnderChest().setContents(newContents);
                player.saveData(); // Ghi dữ liệu vào file .dat ngay lập tức để chống dupe
            });
        }
    }

    @EventHandler
    public void onOpenEnderBlock(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            Player player = (Player) event.getPlayer();
            // Nếu có quyền nâng cấp, thay thế giao diện mặc định bằng rương mở rộng
            if (player.hasPermission("arisenderchest.3.9")) {
                event.setCancelled(true);
                Bukkit.getRegionScheduler().execute(this, player.getLocation(), () -> openCustomEnderChest(player));
            }
        }
    }
}
