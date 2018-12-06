package codes.biscuit.simplegenbuckets.commands;

import codes.biscuit.simplegenbuckets.SimpleGenBuckets;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GenBucketAdminCommand implements CommandExecutor {

    private SimpleGenBuckets main;

    public GenBucketAdminCommand(SimpleGenBuckets main) {
        this.main = main;
    }

    public final TabCompleter TAB_COMPLETER = (sender, cmd, alias, args) -> {
        if (args.length == 1) {
            List<String> arguments = new ArrayList<>(Arrays.asList("give", "reload", "bypass"));
            for (String arg : Arrays.asList("give", "reload")) {
                if (!arg.startsWith(args[0].toLowerCase())) {
                    arguments.remove(arg);
                }
            }
            return arguments;
        } else if (args.length == 3) {
            List<String> arguments = new ArrayList<>(main.getConfigValues().getBucketList());
            for (String arg : main.getConfigValues().getBucketList()) {
                if (!arg.startsWith(args[2].toLowerCase())) {
                    arguments.remove(arg);
                }
            }
            return arguments;
        }
        return null;
    };

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "give":
                    if (sender.hasPermission("simplegenbuckets.give")) {
                        if (args.length > 1) {
                            Player p = Bukkit.getPlayerExact(args[1]);
                            if (p != null) {
                                if (args.length > 2) {
                                    String bucket = args[2];
                                    if (main.getConfigValues().bucketExists(bucket)) {
                                        int giveAmount = 1;
                                        if (args.length > 3) {
                                            try {
                                                giveAmount = Integer.parseInt(args[3]);
                                            } catch (NumberFormatException ex) {
                                                sender.sendMessage(ChatColor.RED + "This isn't a valid give amount!");
                                                return false;
                                            }
                                        }
                                        ItemStack item = main.getConfigValues().getBucketIngameItemStack(bucket, giveAmount);
                                        ItemMeta itemMeta = item.getItemMeta();
                                        itemMeta.setDisplayName(main.getConfigValues().getBucketName(bucket));
                                        itemMeta.setLore(main.getConfigValues().getBucketItemLore(bucket));
                                        item.setItemMeta(itemMeta);
                                        if (main.getConfigValues().bucketItemShouldGlow(bucket)) {
                                            item = main.getUtils().addGlow(item);
                                        }
                                        HashMap excessItems;
                                        if (!main.getConfigValues().giveShouldDropItem()) {
                                            if (giveAmount < 65) {
                                                if (p.getInventory().firstEmpty() == -1) {
                                                    sender.sendMessage(ChatColor.RED + "This player doesn't have any empty slots in their inventory!");
                                                    return true;
                                                }
                                            } else {
                                                sender.sendMessage(ChatColor.RED + "You can only give 64 at a time!");
                                                return true;
                                            }
                                        }
                                        excessItems = p.getInventory().addItem(item);
                                        for (Object excessItem : excessItems.values()) {
                                            int itemCount = ((ItemStack) excessItem).getAmount();
                                            while (itemCount > 64) {
                                                ((ItemStack) excessItem).setAmount(64);
                                                p.getWorld().dropItemNaturally(p.getLocation(), (ItemStack) excessItem);
                                                itemCount = itemCount - 64;
                                            }
                                            if (itemCount > 0) {
                                                ((ItemStack) excessItem).setAmount(itemCount);
                                                p.getWorld().dropItemNaturally(p.getLocation(), (ItemStack) excessItem);
                                            }
                                        }
                                        if (!main.getConfigValues().getGiveMessage(p, giveAmount).equals("")) {
                                            sender.sendMessage(main.getConfigValues().getGiveMessage(p, giveAmount));
                                        }
                                        if (!main.getConfigValues().getReceiveMessage(giveAmount).equals("")) {
                                            p.sendMessage(main.getConfigValues().getReceiveMessage(giveAmount));
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "This bucket does not exist!");
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Please specify a bucket!");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "This player is not online!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Please specify a player!");
                        }
                    } else {
                        if (!main.getConfigValues().getNoPermissionCommandMessage().equals("")) {
                            sender.sendMessage(main.getConfigValues().getNoPermissionCommandMessage());
                        }
                    }
                    break;
                case "reload":
                    if (sender.hasPermission("simplegenbuckets.reload")) {
                        main.reloadConfig();
                        sender.sendMessage(ChatColor.GREEN + "Successfully reloaded the config. Most values have been instantly updated.");
                    } else {
                        if (!main.getConfigValues().getNoPermissionCommandMessage().equals("")) {
                            sender.sendMessage(main.getConfigValues().getNoPermissionCommandMessage());
                        }
                    }
                    break;
                case "bypass":
                    if (sender instanceof Player) {
                        if (sender.hasPermission("simplegenbuckets.bypass")) {
                            if (main.getHookUtils().getBypassPlayers().contains(((Player)sender).getUniqueId())) {
                                sender.sendMessage(ChatColor.RED + "You can no longer place genbuckets anywhere infinitely.");
                                main.getHookUtils().getBypassPlayers().remove(((Player)sender).getUniqueId());
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "You can now place genbuckets anywhere infinitely.");
                                main.getHookUtils().getBypassPlayers().add(((Player)sender).getUniqueId());
                            }
                        } else {
                            if (!main.getConfigValues().getNoPermissionCommandMessage().equals("")) {
                                sender.sendMessage(main.getConfigValues().getNoPermissionCommandMessage());
                            }
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You can only use this command in-game!");
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Invalid argument!");
            }
        } else {
            sender.sendMessage(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------" + ChatColor.GRAY +"[" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + " SimpleGenBuckets " + ChatColor.GRAY + "]" + ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "● /gba give <player> <bucket> [amount] " + ChatColor.GRAY + "- Give a player (a) genbucket(s)");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "● /gba reload " + ChatColor.GRAY + "- Reload the config");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "● /gba bypass " + ChatColor.GRAY + "- Place genbuckets anywhere infinitely!");
            sender.sendMessage(ChatColor.GRAY.toString() + ChatColor.ITALIC + "v" + main.getDescription().getVersion() + " by Biscut");
            sender.sendMessage(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------------------------------");
        }
        return false;
    }
}
