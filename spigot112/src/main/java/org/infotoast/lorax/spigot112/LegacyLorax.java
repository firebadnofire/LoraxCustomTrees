package org.infotoast.lorax.spigot112;

import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.java.JavaPlugin;
import org.infotoast.lorax.common.Catalog;
import org.infotoast.lorax.common.Generation;
import org.infotoast.lorax.common.LegacyBlocks;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.common.Structure;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

public final class LegacyLorax extends JavaPlugin implements Listener {
    private Map<String, Structure> catalog = Collections.emptyMap();
    public Map<String, Structure> getStructures() { return catalog; }

    @Override public void onEnable() {
        saveDefaultConfig();
        try {
            catalog = Catalog.load(getClassLoader());
            for (Structure structure : catalog.values()) for (Structure.Entry entry : structure.entries) {
                for (org.infotoast.lorax.common.BlockState state : entry.blocks) for (int rotation = 0; rotation < 4; rotation++) LegacyBlocks.map(state.rotate(rotation));
            }
        } catch (Exception error) {
            getLogger().log(java.util.logging.Level.SEVERE, "Lorax legacy asset initialization failed", error);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Lorax Engine loaded successfully! Loaded " + catalog.size() + " bundled structures.");
    }

    @EventHandler public void worldInit(WorldInitEvent event) {
        final World world = event.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL || !getConfig().getBoolean("enable-populator")) return;
        if (getConfig().getBoolean("select-worlds") && !getConfig().getStringList("enabled-worlds").contains(world.getName())) return;
        world.getPopulators().add(new BlockPopulator() {
            @Override public void populate(World source, Random random, Chunk chunk) {
                Generation.populate(catalog, new LegacyWorldAccess(source, chunk.getX(), chunk.getZ(), false), chunk.getX(), chunk.getZ());
            }
        });
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("/lorax spawn <structure> [rotation]");
            sender.sendMessage("/lorax export is not available in this legacy adapter.");
            return true;
        }
        if (args[0].equalsIgnoreCase("export")) {
            sender.sendMessage("Legacy WorldEdit export is not implemented; use the modern Paper export command.");
            return true;
        }
        if (!args[0].equalsIgnoreCase("spawn")) return false;
        if (!(sender instanceof Player)) { sender.sendMessage("This command requires a player."); return true; }
        if (!sender.hasPermission("lorax.spawn")) { sender.sendMessage("You do not have permission."); return true; }
        if (args.length < 2 || args.length > 3) { sender.sendMessage("/lorax spawn <structure> [rotation]"); return true; }
        Player player = (Player) sender;
        Structure structure = catalog.get(args[1]);
        if (structure == null) { sender.sendMessage("Unknown structure: " + args[1]); return true; }
        Position position = new Position(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        try {
            int rotation = args.length == 3 ? Integer.parseInt(args[2]) : new Random(java.util.Objects.hash(player.getWorld().getSeed(), (long) position.x, position.y, position.z)).nextInt(4);
            if (rotation < 0 || rotation > 3) throw new IllegalArgumentException("Rotation must be 0..3");
            Placement.Result result = Placement.place(structure, position, rotation, new LegacyWorldAccess(player.getWorld(), position.x >> 4, position.z >> 4, true), true);
            sender.sendMessage("Lorax placement: " + result);
        } catch (IllegalArgumentException error) { sender.sendMessage(error.getMessage()); }
        return true;
    }
}
