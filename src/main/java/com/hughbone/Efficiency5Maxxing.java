package com.hughbone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public class Efficiency5Maxxing implements ModInitializer {
	public static final String MOD_ID = "efficiency-5-maxxing";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// The enchantment id we cap, and the level we cap it to.
	private static final String EFFICIENCY_ID = "minecraft:efficiency";
	private static final int MAX_LEVEL = 5;

	@Override
	public void onInitialize() {
		// Run once, after the server (and its world storage) is fully started. At this
		// point no players are connected yet, so every player's data lives on disk and
		// can be edited safely.
		ServerLifecycleEvents.SERVER_STARTED.register(this::processAllPlayerData);
	}

	private void processAllPlayerData(MinecraftServer server) {
		Path playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
		if (!Files.isDirectory(playerDataDir)) {
			LOGGER.info("No playerdata directory found at {}, nothing to do.", playerDataDir);
			return;
		}

		int scanned = 0;
		int changed = 0;
		try (Stream<Path> files = Files.list(playerDataDir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".dat")).toList()) {
				scanned++;
				try {
					if (processPlayerFile(file)) {
						changed++;
						LOGGER.info("Capped efficiency in {}", file.getFileName());
					}
				} catch (IOException e) {
					LOGGER.error("Failed to process {}", file.getFileName(), e);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to list player data directory {}", playerDataDir, e);
			return;
		}

		LOGGER.info("efficiency-5-maxxing done: scanned {} player file(s), modified {}.", scanned, changed);
	}

	private boolean processPlayerFile(Path file) throws IOException {
		CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		if (!capEfficiency(root)) {
			return false;
		}
		NbtIo.writeCompressed(root, file);
		return true;
	}

	// Recursively walk the NBT tree and cap any "minecraft:efficiency" entry above
	// MAX_LEVEL. This catches the enchantment wherever it appears (held items, the
	// main inventory, ender chest, equipment, stored book enchantments, ...) without
	// depending on the exact inventory layout. Returns true if anything changed.
	private boolean capEfficiency(Tag tag) {
		boolean changed = false;
		if (tag instanceof CompoundTag compound) {
			for (String key : new ArrayList<>(compound.keySet())) {
				Tag child = compound.get(key);
				if (EFFICIENCY_ID.equals(key) && child instanceof IntTag intTag) {
					if (intTag.intValue() > MAX_LEVEL) {
						compound.put(key, IntTag.valueOf(MAX_LEVEL));
						changed = true;
					}
				} else {
					changed |= capEfficiency(child);
				}
			}
		} else if (tag instanceof ListTag list) {
			for (Tag child : list) {
				changed |= capEfficiency(child);
			}
		}
		return changed;
	}
}
