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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
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
                ? Component.text("Set your PIN — enter 4 digits", NamedTextColor.BLACK)
                : Component.text("Enter your PIN", NamedTextColor.BLACK);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack background = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, background);

        refreshDisplay(inv, new StringBuilder());

        inv.setItem(28, makeDigit("1")); inv.setItem(29, makeDigit("2")); inv.setItem(30, makeDigit("3"));
        inv.setItem(37, makeDigit("4")); inv.setItem(38, makeDigit("5")); inv.setItem(39, makeDigit("6"));
        inv.setItem(46, makeDigit("7")); inv.setItem(47, makeDigit("8")); inv.setItem(48, makeDigit("9"));
        inv.setItem(43, makeDigit("0"));

        inv.setItem(SLOT_DEL, makeItem(Material.RED_CONCRETE, "§c§l⌫  Delete"));
        inv.setItem(SLOT_OK, makeItem(Material.LIME_CONCRETE, "§a§l✔  Confirm"));

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

    private ItemStack makeDigit(String digit) {
        ItemStack item = new ItemStack(Material.WHITE_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§f§l" + digit).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
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
            player.sendMessage("§7Your PIN is: §f" + pin + " §7— keep it safe!");
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
                player.sendMessage("§e[VSP] ⚠ " + (TIMEOUT_SECS / 2) + " seconds remaining to enter your PIN!");
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
            getLogger().log(Level.SEVERE, "SHA-256 not available — storing PIN in plain text!", ex);
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
