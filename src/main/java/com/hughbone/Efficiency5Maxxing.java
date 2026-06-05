package com.hughbone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (capEfficiency(inventory.getItem(i))) {
				capped++;
			}
		}
		if (capped > 0) {
			LOGGER.info("Capped efficiency on {} item(s) in {}'s inventory.",
				capped, player.getGameProfile().name());
		}
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
