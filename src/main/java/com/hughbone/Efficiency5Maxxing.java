package com.hughbone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Efficiency5Maxxing implements ModInitializer {
	public static final String MOD_ID = "efficiency-5-maxxing";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// The level we cap efficiency to.
	private static final int MAX_LEVEL = 5;

	@Override
	public void onInitialize() {
		// When a player joins, walk their inventory and cap any over-leveled
		// efficiency enchantment. This avoids scanning/rewriting .dat files on disk:
		// the only place an over-leveled tool can re-enter play is in a player's
		// inventory, and that's checked the moment they log in.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			capPlayerInventory(handler.getPlayer()));
	}

	private void capPlayerInventory(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		int capped = 0;
		int restored = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (capEfficiency(stack)) {
				capped++;
			}
			if (resetToolComponent(stack)) {
				restored++;
			}
		}
		if (capped > 0) {
			LOGGER.info("Capped efficiency on {} item(s) in {}'s inventory.",
				capped, player.getGameProfile().name());
		}
		if (restored > 0) {
			LOGGER.info("Reset mining-speed (tool component) on {} item(s) in {}'s inventory.",
				restored, player.getGameProfile().name());
		}
	}

	// Undo the "NetheriteInstantMining" datapack, which rewrites each tool's
	// minecraft:tool component (via set_components) to give pickaxe/axe-mineable
	// blocks an absurd mining speed. We can't tell a legit custom tool component
	// from the datapack's, so we simply revert the component to the item's
	// default. Returns true if the stack was modified.
	private boolean resetToolComponent(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		// The item's prototype (default) tool component, if it has one.
		Tool defaultTool = stack.getItem().components().get(DataComponents.TOOL);
		Tool currentTool = stack.get(DataComponents.TOOL);

		// Nothing to do if the component already matches the default.
		if (java.util.Objects.equals(currentTool, defaultTool)) {
			return false;
		}

		if (defaultTool != null) {
			// Setting the prototype value clears the per-stack override,
			// restoring vanilla mining behavior.
			stack.set(DataComponents.TOOL, defaultTool);
		} else {
			// Non-tool item the datapack added a tool component to: strip it.
			stack.remove(DataComponents.TOOL);
		}
		return true;
	}

	// Cap the efficiency enchantment on a single stack to MAX_LEVEL.
	// Returns true if the stack was modified.
	private boolean capEfficiency(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		ItemEnchantments enchantments = stack.getEnchantments();
		boolean overLeveled = false;
		for (Holder<Enchantment> ench : enchantments.keySet()) {
			if (ench.is(Enchantments.EFFICIENCY) && enchantments.getLevel(ench) > MAX_LEVEL) {
				overLeveled = true;
				break;
			}
		}
		if (!overLeveled) {
			return false;
		}

		EnchantmentHelper.updateEnchantments(stack, mutable -> {
			for (Holder<Enchantment> ench : mutable.keySet()) {
				if (ench.is(Enchantments.EFFICIENCY) && mutable.getLevel(ench) > MAX_LEVEL) {
					mutable.set(ench, MAX_LEVEL);
				}
			}
		});
		return true;
	}
}
