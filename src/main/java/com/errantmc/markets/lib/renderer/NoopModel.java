package com.errantmc.markets.lib.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** A baked model that renders nothing but still behaves like a normal item model. */
public final class NoopModel extends BakedModelWrapper<BakedModel> {
    public NoopModel() {
        // Delegate everything (particle, overrides, transforms, flags) to the missing model
        super(Minecraft.getInstance().getModelManager().getMissingModel());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state,
                                    @Nullable Direction side,
                                    RandomSource rand,
                                    ModelData extraData,
                                    @Nullable RenderType renderType) {
        // Draw nothing
        return Collections.emptyList();
    }
}