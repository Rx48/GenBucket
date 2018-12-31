package codes.biscuit.simplegenbuckets.utils;

import codes.biscuit.simplegenbuckets.SimpleGenBuckets;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {

    private SimpleGenBuckets main;
    private List<Recipe> recipeList = new ArrayList<>();

    public Utils(SimpleGenBuckets main) {
        this.main = main;
    }

    public String matchBucket(ItemStack item) {
        if (main.getConfigValues().getBucketMaterialList().values().contains(item.getType()) && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Set<Map.Entry<String, Material>> materialList = main.getConfigValues().getBucketMaterialList().entrySet();
            for (Map.Entry<String, Material> material : materialList) {
                if (material.getValue().equals(item.getType())) {
                    if (main.getConfigValues().getBucketItemName(material.getKey()).equals(item.getItemMeta().getDisplayName())) {
                        return material.getKey();
                    }
                }
            }
        }
        return null;
    }

    public ItemStack getBucketItemStack(String bucket, int amount) {
        ItemStack item = main.getConfigValues().getBucketIngameItemStack(bucket, amount);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(main.getConfigValues().getBucketName(bucket));
        itemMeta.setLore(main.getConfigValues().getBucketItemLore(bucket));
        item.setItemMeta(itemMeta);
        if (main.getConfigValues().bucketItemShouldGlow(bucket)) {
            item = main.getUtils().addGlow(item);
        }
        return item;
    }

    public ItemStack addGlow(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public void registerRecipes() {
        if (main.getConfigValues().getRecipeBuckets() != null) {
            for (String bucket : main.getConfigValues().getRecipeBuckets()) {
                ItemStack item = getBucketItemStack(bucket, main.getConfigValues().getRecipeAmount(bucket));
                ShapedRecipe newRecipe = new ShapedRecipe(item);
                if (main.getConfigValues().getRecipeShape(bucket) != null) {
                    newRecipe.shape(main.getConfigValues().getRecipeShape(bucket).get(0), main.getConfigValues().getRecipeShape(bucket).get(1), main.getConfigValues().getRecipeShape(bucket).get(2));
                } else {
                    continue;
                }
                if (main.getConfigValues().getIngredients(bucket) != null) {
                    for (Map.Entry<Character, HashMap<Material, Short>> ingredient : main.getConfigValues().getIngredients(bucket).entrySet()) {
                        Material mat = null;
                        for (Material loopMat : ingredient.getValue().keySet()) mat = loopMat;
                        short data;
                        if (mat != null) {
                            data = ingredient.getValue().get(mat);
                        } else {
                            continue;
                        }
                        if (data != 1) {
                            newRecipe.setIngredient(ingredient.getKey(), new ItemStack(mat, 1, data).getData());
                        } else {
                            newRecipe.setIngredient(ingredient.getKey(), mat);
                        }
                    }
                } else {
                    continue;
                }
                main.getServer().addRecipe(newRecipe);
                recipeList.add(newRecipe);
            }
        }
    }

    public void reloadRecipes() {
        List<Recipe> oldRecipes = new ArrayList<>();
        {
            Iterator<Recipe> allRecipes = main.getServer().recipeIterator();
            while(allRecipes.hasNext()){
                Recipe recipe = allRecipes.next();
                if(!recipeList.contains(recipe)) {
                    oldRecipes.add(recipe);
                }
            }
        }
        main.getServer().clearRecipes();
        recipeList.clear();
        for (Recipe recipe : oldRecipes) main.getServer().addRecipe(recipe);
        registerRecipes();
    }

    public void checkUpdates(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://raw.githubusercontent.com/biscuut/"+main.getDescription().getName()+"/master/pom.xml");
                    URLConnection connection = url.openConnection();
                    connection.setReadTimeout(5000);
                    connection.addRequestProperty("User-Agent", "SimpleGenBuckets update checker");
                    connection.setDoOutput(true);
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String currentLine;
                    String newestVersion = "";
                    while ((currentLine = reader.readLine()) != null) {
                        if (currentLine.contains("<version>")) {
                            String[] newestVersionSplit = currentLine.split(Pattern.quote("<version>"));
                            newestVersionSplit = newestVersionSplit[1].split(Pattern.quote("</version>"));
                            newestVersion = newestVersionSplit[0];
                            break;
                        }
                    }
                    reader.close();
                    ArrayList<Integer> newestVersionNumbers = new ArrayList<>();
                    ArrayList<Integer> thisVersionNumbers = new ArrayList<>();
                    try {
                        for (String s : newestVersion.split(Pattern.quote("."))) {
                            newestVersionNumbers.add(Integer.parseInt(s));
                        }
                        for (String s : main.getDescription().getVersion().split(Pattern.quote("."))) {
                            thisVersionNumbers.add(Integer.parseInt(s));
                        }
                    } catch (Exception ex) {
                        return;
                    }
                    for (int i = 0; i < 3; i++) {
                        if (newestVersionNumbers.get(i) != null && thisVersionNumbers.get(i) != null) {
                            if (newestVersionNumbers.get(i) > thisVersionNumbers.get(i)) {
                                TextComponent newVersion = new TextComponent("A new version of "+main.getDescription().getName()+", " + newestVersion + " is available. Download it by clicking here.");
                                newVersion.setColor(ChatColor.RED);
                                newVersion.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/biscuut/"+main.getDescription().getName()+"/releases")); //TODO Change this to the spigot page when I post it.
                                p.spigot().sendMessage(newVersion);
                            } else if (thisVersionNumbers.get(i) > newestVersionNumbers.get(i)) {
                                p.sendMessage(ChatColor.RED + "You are running a development version of "+main.getDescription().getName()+", " + main.getDescription().getVersion() + ". The latest online version is " + newestVersion + ".");
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.runTask(main);
    }
}
