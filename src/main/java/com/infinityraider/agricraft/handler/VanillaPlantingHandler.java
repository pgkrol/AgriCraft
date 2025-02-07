package com.infinityraider.agricraft.handler;

import com.google.common.collect.Sets;
import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.api.v1.AgriApi;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

public class VanillaPlantingHandler {
    private static final VanillaPlantingHandler INSTANCE = new VanillaPlantingHandler();

    public static VanillaPlantingHandler getInstance() {
        return INSTANCE;
    }

    private final Set<Item> exceptions;

    private VanillaPlantingHandler() {
        this.exceptions = Sets.newConcurrentHashSet();
    }

    public void registerException(Item item) {
        this.exceptions.add(item);
    }

    public boolean isException(ItemStack stack) {
        return this.exceptions.contains(stack.getItem());
    }

    @SuppressWarnings("unused")
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void vanillaSeedPlanting(PlayerInteractEvent.RightClickBlock event) {
        // If overriding is disabled, don't bother.
        if (!AgriCraft.instance.getConfig().overrideVanillaFarming()) {
            return;
        }

        // Fetch the event item stack.
        final ItemStack stack = event.getItemStack();

        // If the item is an exception, cancel
        if(this.isException(event.getItemStack())) {
            return;
        }

        // If clicking crop tile, the crop will handle the logic
        if (AgriApi.getCrop(event.getWorld(), event.getPos()).isPresent()) {
            return;
        }

        // Pass the stack through the adapterizer
        boolean success = AgriApi.getGenomeAdapterizer().valueOf(stack).map(seed -> {
            // Fetch world information.
            BlockPos pos = event.getFace() == null ? event.getPos() : event.getPos().offset(event.getFace());
            World world = event.getWorld();
            PlayerEntity player = event.getPlayer();

            // The player is attempting to plant a seed,
            // convert it to an agricraft crop
            return AgriApi.getSoil(event.getWorld(), pos.down()).map(soil -> {
                BlockState newState = AgriCraft.instance.getModBlockRegistry().crop_plant.getStateForPlacement(world, pos);
                if (newState != null && world.setBlockState(pos, newState, 11)) {
                    boolean planted = AgriApi.getCrop(world, pos).map(crop -> crop.plantGenome(seed)).orElse(false);
                    if (planted) {
                        if (player == null || !player.isCreative()) {
                            stack.shrink(1);
                        }
                        return true;
                    } else {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
                return false;
            }).orElse(false);
        }).orElse(false);

        if(success) {
            // Cancel the event
            event.setUseItem(Event.Result.DENY);
            event.setCanceled(true);
        }
    }

    /*
     * Event handler to deny bonemeal while sneaking on crops that are not
     * allowed to have bonemeal applied to them
     */
    @SuppressWarnings("unused")
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void denyBonemeal(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntityLiving().isSneaking()) {
            return;
        }
        ItemStack heldItem = event.getEntityLiving().getActiveItemStack();
        if (!heldItem.isEmpty() && heldItem.getItem() == Items.BONE_MEAL) {
            AgriApi.getCrop(event.getWorld(), event.getPos()).ifPresent(crop -> event.setUseItem(Event.Result.DENY));
        }
    }
}
