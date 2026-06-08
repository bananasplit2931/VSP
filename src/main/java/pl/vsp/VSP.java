package pl.vsp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;

public final class VSP extends JavaPlugin implements Listener {

    private static final int MAX_ATTEMPTS = 3;
    private static final int TIMEOUT_SECS = 30;

    private static final int[] DISPLAY_SLOTS = {10, 11, 12, 13};
    private static final Map<Integer, String> SLOT_TO_DIGIT = new LinkedHashMap<>();
    private static final int SLOT_DEL = 31;
    private static final int SLOT_OK = 49;
    
    private static final String SKULL_0  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMGViZTdlNTIxNTE2OWE2OTlhY2M2Y2VmYTdiNzNmZGIxMDhkYjg3YmI2ZGFlMjg0OWZiZTI0NzE0YjI3In19fQ==";
    private static final String SKULL_1  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFiYzJiY2ZiMmJkMzc1OWU2YjFlODZmYzdhNzk1ODVlMTEyN2RkMzU3ZmMyMDI4OTNmOWRlMjQxYmM5ZTUzMCJ9fX0=";
    private static final String SKULL_2  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNkOWVlZWU4ODM0Njg4ODFkODM4NDhhNDZiZjMwMTI0ODVjMjNmNzU3NTNiOGZiZTg0ODczNDE0MTk4NDcifX19";
    private static final String SKULL_3  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQ0ZWFlMTM5MzM4NjBhNmRmNWU4ZTk1NTY5M2I5NWE4YzNiMTVjMzZiOGI1ODc1MzJhYzA5OTZiYzM3ZTUifX19";
    private static final String SKULL_4  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJlNzhmYjIyNDI0MjMyZGMyN2I4MWZiY2I0N2ZkMjRjMWFjZjc2MDk4NzUzZjJkOWMyODU5ODI4N2RiNSJ9fX0=";
    private static final String SKULL_5  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ1N2UzYmM4OGE2NTczMGUzMWExNGUzZjQxZTAzOGE1ZWNmMDg5MWE2YzI0MzY0M2I4ZTU0NzZhZTIifX19";
    private static final String SKULL_6  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzM0YjM2ZGU3ZDY3OWI4YmJjNzI1NDk5YWRhZWYyNGRjNTE4ZjVhZTIzZTcxNjk4MWUxZGNjNmIyNzIwYWIifX19";
    private static final String SKULL_7  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmRiNmViMjVkMWZhYWJlMzBjZjQ0NGRjNjMzYjU4MzI0NzVlMzgwOTZiN2UyNDAyYTNlYzQ3NmRkN2I5In19fQ==";
    private static final String SKULL_8  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTkxOTQ5NzNhM2YxN2JkYTk5NzhlZDYyNzMzODM5OTcyMjI3NzRiNDU0Mzg2YzgzMTljMDRmMWY0Zjc0YzJiNSJ9fX0=";
    private static final String SKULL_9  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY3Y2FmNzU5MWIzOGUxMjVhODAxN2Q1OGNmYzY0MzNiZmFmODRjZDQ5OWQ3OTRmNDFkMTBiZmYyZTViODQwIn19fQ==";
    private static final String SKULL_OK  = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDk5ODBjMWQyMTE4MDlhOWI2NTY1MDg4ZjU2YTM4ZjJlZjQ5MTE1YzEwNTRmYTY2MjQ1MTIyZTllZWVkZWNjMiJ9fX0=";
    private static final String SKULL_DEL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjEyZjNlZmU4NGEwZjY2NDZhODBkNDVjZWZlNDE4ZTE5OWQ5NjE5ZjhjMWZiNWY1YzVjMDA4YzYwMzA1OWFjMyJ9fX0=";

    static {
        SLOT_TO_DIGIT.put(28, "1"); SLOT_TO_DIGIT.put(29, "2"); SLOT_TO_DIGIT.put(30, "3");
        SLOT_TO_DIGIT.put(37, "4"); SLOT_TO_DIGIT.put(38, "5"); SLOT_TO_DIGIT.put(39, "6");
        SLOT_TO_DIGIT.put(46, "7"); SLOT_TO_DIGIT.put(47, "8"); SLOT_TO_DIGIT.put(48, "9");
        SLOT_TO_DIGIT.put(43, "0");
    }

    private final Set<UUID> authenticated = new HashSet<>();
    private final Map<UUID, Integer> attempts = new HashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new HashMap<>();
    private final Map<UUID, StringBuilder> pinBuffers = new HashMap<>();

    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "pins.yml");
        getDataFolder().mkdirs();

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Could not create pins.yml", ex);
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VSP enabled.");
    }

    @Override
    public void onDisable() {
        timeoutTasks.values().forEach(BukkitTask::cancel);
        timeoutTasks.clear();
        Bukkit.getOnlinePlayers().forEach(this::unlock);
        getLogger().info("VSP disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        lock(player);
        attempts.put(player.getUniqueId(), 0);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            startTimeout(player);
            openPinGui(player);
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cleanup(player);
        unlock(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isLocked(player)) return;
        if (!pinBuffers.containsKey(player.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && isLocked(player)) openPinGui(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!pinBuffers.containsKey(player.getUniqueId())) {
            if (isLocked(player)) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getRawSlot();
        StringBuilder buffer = pinBuffers.get(player.getUniqueId());
        if (buffer == null) return;

        if (SLOT_TO_DIGIT.containsKey(slot)) {
            if (buffer.length() < 4) {
                buffer.append(SLOT_TO_DIGIT.get(slot));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                refreshDisplay(event.getView().getTopInventory(), buffer);
            }
        } else if (slot == SLOT_DEL) {
            if (!buffer.isEmpty()) {
                buffer.deleteCharAt(buffer.length() - 1);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                refreshDisplay(event.getView().getTopInventory(), buffer);
            }
        } else if (slot == SLOT_OK) {
            if (buffer.length() != 4) {
                player.sendMessage("§c[VSP] Enter exactly 4 digits!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                return;
            }
            String pin = buffer.toString();
            pinBuffers.remove(player.getUniqueId());
            player.closeInventory();
            handlePin(player, pin);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isLocked(event.getPlayer())) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isLocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isLocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isLocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (isLocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isLocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (isLocked(event.getPlayer())) event.setCancelled(true);
    }

    private void openPinGui(Player player) {
        pinBuffers.put(player.getUniqueId(), new StringBuilder());

        boolean isFirstJoin = !hasPin(player);
        Component title = isFirstJoin
                ? Component.text("Set your PIN. Enter 4 digits", NamedTextColor.BLACK)
                : Component.text("Enter your PIN", NamedTextColor.BLACK);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack background = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, background);

        refreshDisplay(inv, new StringBuilder());

        inv.setItem(28, makeSkullDigit("1", SKULL_1));
        inv.setItem(29, makeSkullDigit("2", SKULL_2));
        inv.setItem(30, makeSkullDigit("3", SKULL_3));
        inv.setItem(37, makeSkullDigit("4", SKULL_4));
        inv.setItem(38, makeSkullDigit("5", SKULL_5));
        inv.setItem(39, makeSkullDigit("6", SKULL_6));
        inv.setItem(46, makeSkullDigit("7", SKULL_7));
        inv.setItem(47, makeSkullDigit("8", SKULL_8));
        inv.setItem(48, makeSkullDigit("9", SKULL_9));
        inv.setItem(43, makeSkullDigit("0", SKULL_0));

        inv.setItem(SLOT_DEL, makeSkullButton("§c§l⌫  Delete", SKULL_DEL));
        inv.setItem(SLOT_OK,  makeSkullButton("§a§l✔  Confirm", SKULL_OK));

        player.openInventory(inv);
    }

    private void refreshDisplay(Inventory inv, StringBuilder buffer) {
        for (int i = 0; i < 4; i++) {
            boolean filled = i < buffer.length();
            inv.setItem(DISPLAY_SLOTS[i], makeItem(
                    filled ? Material.PURPLE_CONCRETE : Material.GRAY_CONCRETE,
                    filled ? "§5§l●" : "§8○"
            ));
        }
    }
    
    private ItemStack makeSkullDigit(String digit, String textureValue) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        applySkullTexture(skull, textureValue);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.displayName(Component.text("§f§l" + digit).decoration(TextDecoration.ITALIC, false));
        skull.setItemMeta(meta);
        return skull;
    }
    
    private ItemStack makeSkullButton(String name, String textureValue) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        applySkullTexture(skull, textureValue);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        skull.setItemMeta(meta);
        return skull;
    }
    
    private void applySkullTexture(ItemStack skull, String textureValue) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return;
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "VSP");
            PlayerTextures textures = profile.getTextures();
            String decoded = new String(Base64.getDecoder().decode(textureValue), StandardCharsets.UTF_8);
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd   = decoded.indexOf("\"", urlStart);
            String skinUrl = decoded.substring(urlStart, urlEnd);
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to apply skull texture: " + textureValue, ex);
        }
        skull.setItemMeta(meta);
    }

    private ItemStack makeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private void handlePin(Player player, String pin) {
        if (!hasPin(player)) {
            savePin(player, pin);
            login(player);
            player.sendMessage("§a[VSP] ✔ PIN set! You are now logged in.");
            player.sendMessage("§7Your PIN is: §f" + pin + " §7 keep it safe!");
        } else {
            if (checkPin(player, pin)) {
                login(player);
                player.sendMessage("§a[VSP] ✔ Successfully logged in!");
            } else {
                int tries = attempts.getOrDefault(player.getUniqueId(), 0) + 1;
                attempts.put(player.getUniqueId(), tries);
                int remaining = MAX_ATTEMPTS - tries;

                if (tries >= MAX_ATTEMPTS) {
                    cancelTimeout(player.getUniqueId());
                    unlock(player);
                    player.kickPlayer("§cToo many incorrect PIN attempts.\n§7Please reconnect and try again.");
                } else {
                    player.sendMessage("§c[VSP] ✘ Wrong PIN! Attempts remaining: §f" + remaining);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (player.isOnline() && isLocked(player)) openPinGui(player);
                    }, 5L);
                }
            }
        }
    }

    private void startTimeout(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTimeout(uuid);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && isLocked(player)) {
                player.sendMessage("§e[VSP] ⚠ " + (TIMEOUT_SECS / 2) + " seconds remaining to enter your PIN.");
            }
        }, (long) (TIMEOUT_SECS / 2) * 20L);

        BukkitTask kickTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            if (isLocked(player)) {
                pinBuffers.remove(uuid);
                unlock(player);
                player.kickPlayer("§cLogin timed out.\n§7Please reconnect and try again.");
            }
        }, (long) TIMEOUT_SECS * 20L);

        timeoutTasks.put(uuid, kickTask);
    }

    private void cancelTimeout(UUID uuid) {
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vspadmin")) return false;

        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("reset")) {
            sender.sendMessage("§eUsage: /vspadmin reset <player>");
            return true;
        }

        String targetName = args[1];
        Player onlineTarget = Bukkit.getPlayer(targetName);
        UUID targetUuid = null;

        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            var offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget.hasPlayedBefore()) targetUuid = offlineTarget.getUniqueId();
        }

        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        data.set(targetUuid.toString(), null);
        saveData();
        authenticated.remove(targetUuid);
        attempts.remove(targetUuid);
        pinBuffers.remove(targetUuid);
        cancelTimeout(targetUuid);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            lock(onlineTarget);
            attempts.put(targetUuid, 0);
            startTimeout(onlineTarget);
            openPinGui(onlineTarget);
        }

        sender.sendMessage("§a[VSP] PIN for §f" + targetName + " §ahas been reset.");
        return true;
    }

    private boolean isLocked(Player player) {
        return !authenticated.contains(player.getUniqueId());
    }

    private void lock(Player player) {
        player.setWalkSpeed(0f);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 10, false, false));
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    private void unlock(Player player) {
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        if (player.getGameMode() == GameMode.ADVENTURE) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void login(Player player) {
        cancelTimeout(player.getUniqueId());
        attempts.remove(player.getUniqueId());
        pinBuffers.remove(player.getUniqueId());
        authenticated.add(player.getUniqueId());
        unlock(player);
    }

    private void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTimeout(uuid);
        attempts.remove(uuid);
        pinBuffers.remove(uuid);
        authenticated.remove(uuid);
    }

    private boolean hasPin(Player player) {
        return data.contains(player.getUniqueId().toString());
    }

    private void savePin(Player player, String pin) {
        data.set(player.getUniqueId().toString(), sha256(pin));
        saveData();
    }

    private boolean checkPin(Player player, String pin) {
        String stored = data.getString(player.getUniqueId().toString());
        return stored != null && stored.equals(sha256(pin));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(
                    digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            getLogger().log(Level.SEVERE, "SHA-256 not available, storing PIN in plain text!", ex);
            return input;
        }
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save pins.yml", ex);
        }
    }
}
