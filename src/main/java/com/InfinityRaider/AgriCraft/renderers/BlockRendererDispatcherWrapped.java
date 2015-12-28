package com.InfinityRaider.AgriCraft.renderers;

import com.InfinityRaider.AgriCraft.utility.LogHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ReportedException;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class BlockRendererDispatcherWrapped extends BlockRendererDispatcher implements IRenderingRegistry {
    private static BlockRendererDispatcherWrapped INSTANCE;

    private final BlockRendererDispatcher prevDispatcher;
    private Map<Block, ISimpleBlockRenderingHandler> renderers;

    public static void init() {
        BlockRendererDispatcher prevDispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        INSTANCE = new BlockRendererDispatcherWrapped(prevDispatcher, getGameSettings(prevDispatcher));
        applyDispatcher();
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(INSTANCE);
        RenderItemWrapped.init();
    }

    public static BlockRendererDispatcherWrapped getInstance() {
        return INSTANCE;
    }

    private BlockRendererDispatcherWrapped(BlockRendererDispatcher prevDispatcher, GameSettings settings) {
        super(prevDispatcher.getBlockModelShapes(), settings);
        this.prevDispatcher = prevDispatcher;
        renderers = new HashMap<>();
    }

    @Override
    public void registerRenderingHandler(Block block, ISimpleBlockRenderingHandler renderer) {
        renderers.put(block, renderer);
    }

    @Override
    public ISimpleBlockRenderingHandler getRenderingHandler(Block block) {
        return renderers.get(block);
    }

    @Override
    public boolean hasRenderingHandler(Block block) {
        return renderers.containsKey(block);
    }

    @Override
    public void renderBlockDamage(IBlockState state, BlockPos pos, TextureAtlasSprite texture, IBlockAccess blockAccess) {
        prevDispatcher.renderBlockDamage(state, pos, texture, blockAccess);
    }

    @Override
    public boolean renderBlock(IBlockState state, BlockPos pos, IBlockAccess world, WorldRenderer worldRendererIn) {
        Block block = state.getBlock();
        if(renderers.containsKey(block)) {
            try {
                return renderers.get(block).renderWorldBlock(world, pos.getX(), pos.getY(), pos.getZ(), pos, block, state, worldRendererIn);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block in world");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being tesselated");
                CrashReportCategory.addBlockInfo(crashreportcategory, pos, state.getBlock(), state.getBlock().getMetaFromState(state));
                throw new ReportedException(crashreport);
            }
        }
        return prevDispatcher.renderBlock(state, pos, world, worldRendererIn);
    }

    @Override
    public BlockModelRenderer getBlockModelRenderer() {
        return prevDispatcher.getBlockModelRenderer();
    }

    @Override
    public IBakedModel getModelFromBlockState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return prevDispatcher.getModelFromBlockState(state, worldIn, pos);
    }

    @Override
    public void renderBlockBrightness(IBlockState state, float brightness) {
        prevDispatcher.renderBlockBrightness(state, brightness);
    }

    @Override
    public boolean isRenderTypeChest(Block block, int meta) {
        return prevDispatcher.isRenderTypeChest(block, meta);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        prevDispatcher.onResourceManagerReload(resourceManager);
    }

    private static GameSettings getGameSettings(BlockRendererDispatcher dispatcher) {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        Minecraft mc = Minecraft.getMinecraft();
        for (Field field : mc.getClass().getDeclaredFields()) {
            if(field.getType() == GameSettings.class) {
                field.setAccessible(true);
                try {
                    settings = (GameSettings) field.get(dispatcher);
                } catch (Exception e) {
                    LogHelper.printStackTrace(e);
                }
                field.setAccessible(false);
                break;
            }
        }
        return settings;
    }

    private static void applyDispatcher() {
        Minecraft mc = Minecraft.getMinecraft();
        for(Field field:mc.getClass().getDeclaredFields()) {
            if(field.getType() == BlockRendererDispatcher.class) {
                field.setAccessible(true);
                try {
                    field.set(mc, INSTANCE);
                } catch (IllegalAccessException e) {
                    LogHelper.printStackTrace(e);
                }
                field.setAccessible(false);
                break;
            }
        }
    }
}