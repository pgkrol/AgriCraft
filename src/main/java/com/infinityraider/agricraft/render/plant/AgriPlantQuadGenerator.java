package com.infinityraider.agricraft.render.plant;

import com.agricraft.agricore.plant.AgriRenderType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.infinityraider.agricraft.api.v1.client.AgriPlantRenderType;
import com.infinityraider.agricraft.api.v1.client.IAgriPlantQuadGenerator;
import com.infinityraider.agricraft.api.v1.client.PlantQuadBakeEvent;
import com.infinityraider.agricraft.api.v1.crop.IAgriGrowthStage;
import com.infinityraider.agricraft.api.v1.plant.IAgriRenderable;
import com.infinityraider.infinitylib.render.IRenderUtilities;
import com.infinityraider.infinitylib.render.tessellation.ITessellator;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AgriPlantQuadGenerator implements IAgriPlantQuadGenerator, IRenderUtilities {
    private static final AgriPlantQuadGenerator INSTANCE = new AgriPlantQuadGenerator();

    public static AgriPlantQuadGenerator getInstance() {
        return INSTANCE;
    }

    private static final EnumMap<AgriRenderType, AgriPlantRenderType> CONVERSION_MAP;

    private final ThreadLocal<ITessellator> tess;

    private AgriPlantQuadGenerator() {
        this.tess = ThreadLocal.withInitial(this::getBakedQuadTessellator);
    }

    public ITessellator getTessellator() {
        return this.tess.get();
    }

    public List<BakedQuad> bakeQuads(IAgriRenderable plant, IAgriGrowthStage stage, @Nullable Direction direction,
                                     @Nonnull ResourceLocation texture, int yOffset, AgriRenderType jsonRenderType) {
        if(direction != null) {
            return Collections.emptyList();
        }
        TextureAtlasSprite sprite = this.getSprite(texture);
        AgriPlantRenderType type = CONVERSION_MAP.get(jsonRenderType);
        List<BakedQuad> quads = CONVERSION_MAP.get(jsonRenderType).bakedQuads(null, sprite, yOffset);
        PlantQuadBakeEvent event = new PlantQuadBakeEvent(this, plant, stage, null, texture, sprite, type, yOffset, quads);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getOutputQuads();
    }

    @SuppressWarnings("deprecation")
    public List<BakedQuad> fetchQuads(@Nonnull ResourceLocation location) {
        ModelLoader loader = ModelLoader.instance();
        if(loader == null) {
            return ImmutableList.of();
        }
        IBakedModel model = loader.getBakedModel(location, ModelRotation.X0_Y0, this::getSprite);
        return model == null ? ImmutableList.of() : model.getQuads(null, null, this.getRandom());
    }

    @Nonnull
    public List<BakedQuad> bakeQuadsForHashPattern(@Nonnull TextureAtlasSprite sprite, int yOffset) {
        ITessellator tessellator = this.getTessellator();

        tessellator.startDrawingQuads();
        tessellator.setFace((Direction) null);

        tessellator.pushMatrix();

        tessellator.translate(0, yOffset, 0);

        tessellator.drawScaledFaceDouble(0, 0, 16, 16, Direction.NORTH, sprite, 4);
        tessellator.drawScaledFaceDouble(0, 0, 16, 16, Direction.EAST, sprite, 4);
        tessellator.drawScaledFaceDouble(0, 0, 16, 16, Direction.NORTH, sprite, 12);
        tessellator.drawScaledFaceDouble(0, 0, 16, 16, Direction.EAST, sprite, 12);

        tessellator.popMatrix();

        List<BakedQuad> quads = tessellator.getQuads();
        tessellator.draw();
        return quads;
    }

    @Nonnull
    public List<BakedQuad> bakeQuadsForCrossPattern(@Nonnull TextureAtlasSprite sprite, int yOffset) {
        ITessellator tessellator = this.getTessellator();

        tessellator.startDrawingQuads();
        tessellator.setFace((Direction) null);

        tessellator.pushMatrix();

        tessellator.translate(0.5f, yOffset, 0.5f);
        tessellator.rotate(45, 0, 1, 0);
        tessellator.translate(-0.5f, 0, -0.5f);

        tessellator.drawScaledFaceDouble(0, 0, 16, 16, Direction.NORTH, sprite, 8);
        tessellator.drawScaledFaceDouble(0, 0, 16, 16, Direction.EAST, sprite, 8);

        tessellator.popMatrix();

        List<BakedQuad> quads = tessellator.getQuads();
        tessellator.draw();
        return quads;
    }

    @Nonnull
    public List<BakedQuad> bakeQuadsForPlusPattern(@Nonnull TextureAtlasSprite sprite, int yOffset) {
        ITessellator tessellator = this.getTessellator();

        tessellator.startDrawingQuads();
        tessellator.setFace((Direction) null);

        tessellator.pushMatrix();

        tessellator.translate(0, 12.0F*yOffset/16.0F, 0);

        tessellator.drawScaledFaceDouble(-2, 0, 10, 12, Direction.NORTH, sprite, 4.001F, 0, 0, 16, 16);
        tessellator.drawScaledFaceDouble(6, 0, 18, 12, Direction.NORTH, sprite, 3.999F, 0, 0, 16, 16);

        tessellator.drawScaledFaceDouble(-2, 0, 10, 12, Direction.EAST, sprite, 4.001F, 0, 0, 16, 16);
        tessellator.drawScaledFaceDouble(6, 0, 18, 12, Direction.EAST, sprite, 3.999F, 0, 0, 16, 16);

        tessellator.drawScaledFaceDouble(-2, 0, 10, 12, Direction.NORTH, sprite, 12.001F, 0, 0, 16, 16);
        tessellator.drawScaledFaceDouble(6, 0, 18, 12, Direction.NORTH, sprite, 11.999F, 0, 0, 16, 16);

        tessellator.drawScaledFaceDouble(-2, 0, 10, 12, Direction.EAST, sprite, 12.001F, 0, 0, 16, 16);
        tessellator.drawScaledFaceDouble(6, 0, 18, 12, Direction.EAST, sprite, 11.999F, 0, 0, 16, 16);

        tessellator.popMatrix();

        List<BakedQuad> quads = tessellator.getQuads();
        tessellator.draw();
        return quads;
    }

    @Nonnull
    public List<BakedQuad> bakeQuadsForRhombusPattern(@Nonnull TextureAtlasSprite sprite, int yOffset) {
        ITessellator tessellator = this.getTessellator();

        tessellator.startDrawingQuads();
        tessellator.setFace((Direction) null);

        float d = MathHelper.sqrt(128);

        tessellator.pushMatrix();
        tessellator.translate(0, d*yOffset/16.0F, 0.5f);
        tessellator.rotate(45, 0, 1, 0);

        tessellator.drawScaledFaceDouble(0, 0, d, d, Direction.NORTH, sprite, 0);
        tessellator.drawScaledFaceDouble(0, 0, d, d, Direction.NORTH, sprite, d);
        tessellator.drawScaledFaceDouble(0, 0, d, d, Direction.EAST, sprite, 0);
        tessellator.drawScaledFaceDouble(0, 0, d, d, Direction.EAST, sprite, d);

        tessellator.popMatrix();

        List<BakedQuad> quads = tessellator.getQuads();
        tessellator.draw();
        return quads;
    }

    @Nonnull
    @Override
    public List<BakedQuad> bakeQuadsForGourdPattern(@Nonnull TextureAtlasSprite sprite, int yOffset) {
        if(yOffset == 0) {
            return this.bakeQuadsForHashPattern(sprite, yOffset);
        } else if(yOffset == 1) {
            ITessellator tessellator = this.getTessellator();

            tessellator.startDrawingQuads();
            tessellator.setFace((Direction) null);

            tessellator.pushMatrix();

            tessellator.drawScaledPrism(7, 0, 2, 11, 4, 6, sprite);
            tessellator.drawScaledPrism(10, 0, 7, 14, 4, 11, sprite);
            tessellator.drawScaledPrism(5, 0, 10, 9, 4, 14, sprite);
            tessellator.drawScaledPrism(2, 0, 5, 6, 4, 9, sprite);

            tessellator.popMatrix();

            List<BakedQuad> quads = tessellator.getQuads();
            tessellator.draw();

            return quads;
        }
        return ImmutableList.of();
    }

    static {
        CONVERSION_MAP = Maps.newEnumMap(AgriRenderType.class);
        CONVERSION_MAP.put(AgriRenderType.HASH, AgriPlantRenderType.HASH);
        CONVERSION_MAP.put(AgriRenderType.CROSS, AgriPlantRenderType.CROSS);
        CONVERSION_MAP.put(AgriRenderType.PLUS, AgriPlantRenderType.PLUS);
        CONVERSION_MAP.put(AgriRenderType.RHOMBUS, AgriPlantRenderType.RHOMBUS);
        CONVERSION_MAP.put(AgriRenderType.GOURD, AgriPlantRenderType.GOURD);
    }
}
