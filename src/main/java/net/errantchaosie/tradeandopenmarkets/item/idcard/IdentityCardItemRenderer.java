package net.errantchaosie.tradeandopenmarkets.item.idcard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.component.IdentityCardComponent;
import net.errantchaosie.tradeandopenmarkets.component.IdentityCardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class IdentityCardItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final Logger LOG = LogUtils.getLogger();
    private static final ModelResourceLocation MODEL_LOCATION = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "id_card"));
    private final Minecraft minecraft;
    private final ItemRenderer itemRenderer;

    public IdentityCardItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.minecraft = Minecraft.getInstance();
        this.itemRenderer = minecraft.getItemRenderer();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        LOG.info("tried to render item...");
        BakedModel model = minecraft.getModelManager().getModel(MODEL_LOCATION);
        itemRenderer.render(stack, displayContext, false, poseStack, buffer, packedLight, packedOverlay, model);

        IdentityCardComponent.get(stack).ifPresent(data -> renderHeadOverlay(data, displayContext, poseStack, buffer, packedLight, packedOverlay));
    }

    private void renderHeadOverlay(IdentityCardData data, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        LOG.info("tried to render head...");
        ItemStack headStack = data.createHeadStack();
        Level level = minecraft.level;

        poseStack.pushPose();
        switch (displayContext) {
            case GUI -> {
                poseStack.translate(0.125F, 0.125F, 0.001F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.15F, 0.05F, 0.55F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.15F, -0.1F, 0.5F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
            }
            default -> {
                poseStack.translate(0.15F, 0.0F, 0.4F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
            }
        }
        //poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        itemRenderer.renderStatic(headStack, ItemDisplayContext.GUI, packedLight, packedOverlay, poseStack, buffer, level, data.playerId().hashCode());
        poseStack.popPose();
    }
}