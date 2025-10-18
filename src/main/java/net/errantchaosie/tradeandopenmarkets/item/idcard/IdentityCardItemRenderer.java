package net.errantchaosie.tradeandopenmarkets.item.idcard;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.component.IdentityCardComponent;
import net.errantchaosie.tradeandopenmarkets.component.IdentityCardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@OnlyIn(Dist.CLIENT)
public class IdentityCardItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final Logger LOG = LogUtils.getLogger();
    public static final ModelResourceLocation CARD_MODEL_LOCATION = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, "item/id_card_base"));
    private final Minecraft minecraft;
    private final ItemRenderer itemRenderer;
    private final Font textRenderer;
    // face + hat (8x8 each) from a 64x64 skin
    private static final int TEX_W = 64, TEX_H = 64;
    private static final int FACE_U = 8,  FACE_V = 8,  FACE_SIZE = 8;
    private static final int HAT_U  = 40, HAT_V  = 8,  HAT_SIZE  = 8;

    public IdentityCardItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.minecraft = Minecraft.getInstance();
        this.itemRenderer = minecraft.getItemRenderer();
        this.textRenderer = minecraft.font;
    }

    @Override
    public void renderByItem(ItemStack itemStack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = minecraft.getModelManager().getModel(CARD_MODEL_LOCATION);

        poseStack.pushPose();
            poseStack.mulPose(Axis.XN.rotationDegrees(180.0f));
            //poseStack.mulPose(Axis.ZN.rotationDegrees(90.0f));
            poseStack.translate(0f, -1f, 0f);
            switch (displayContext) {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND,
                     THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND-> {
                    //poseStack.scale(0.5f, 0.5f, 0.5f);
                    //poseStack.translate(1.0F, 1.5F, 0.0F);
                   // poseStack.mulPose(Axis.XN.rotationDegrees(90.0f));
                }
                default -> { // GUI + everything else
                    //poseStack.translate(0.15F, 0.0F, 0.4F);
                    //.scale(0.35F, 0.35F, 0.35F);
                }
            }

            for (RenderType renderType : model.getRenderTypes(itemStack, false)) {
                //LOG.info("Rendering renderType:{}, CardModelLocation: {}", renderType, CARD_MODEL_LOCATION);
                itemRenderer.renderModelLists(
                        model,
                        itemStack,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        ItemRenderer.getFoilBufferDirect(buffer, renderType, true, itemStack.hasFoil())
                );
            }

        _drawIdCardContents(itemStack, displayContext, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();


    }

    /**
     * Renders the contents of an ID card.
     */
    private void _drawIdCardContents(ItemStack itemStack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        IdentityCardComponent.get(itemStack).ifPresent(data -> {
            poseStack.pushPose();
            // Initial alignment
            switch (displayContext) {
                case GUI -> {
                    poseStack.translate(0.15f, 0.15f, 0.45f);
                    // Draw player picture
                    renderFlatFaceQuad(poseStack, displayContext, buffer, data.toProfile(), 0.7f, 0.7f, packedLight, packedOverlay);
                }
                default -> { // Anything in-world
                    poseStack.translate(0.1f, 0.2f, 0.45f);
                    // Draw player picture
                    renderFlatFaceQuad(poseStack, displayContext, buffer, data.toProfile(), 0.3f, 0.3f, packedLight, packedOverlay);
                }
            }
            if(displayContext != ItemDisplayContext.GUI) {
                _drawIdCardText(poseStack, buffer, data);
            }
            poseStack.popPose();
        });
    }

    private void _drawIdCardText(PoseStack poseStack, MultiBufferSource buffer, IdentityCardData data) {
        int fontColor = 0x000000;
        // Player Name
        poseStack.pushPose();
        poseStack.scale(0.01f, 0.01f, 1f);
        textRenderer.drawInBatch(data.playerName(),
                0f,
                32f,
                fontColor, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);
        poseStack.popPose();
        // Creation Date
        poseStack.pushPose();
        poseStack.scale(0.004f, 0.004f, 1f);

        Date date = new Date(data.createdAtMillis());
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateFormatted = formatter.format(date);

        textRenderer.drawInBatch("Issued: " + dateFormatted,
                80, // 72 is too far right
                0,
                fontColor, true, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);
        // Player Id
        textRenderer.drawInBatch("ID: " + data.playerId().toString(),
                -16,
                140,
                fontColor, true, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);
        poseStack.popPose();
    }

    private void _renderHead3D(IdentityCardData data, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        itemRenderer.renderStatic(data.createHeadStack(), ItemDisplayContext.GUI, packedLight, packedOverlay, poseStack, buffer, minecraft.level, data.playerId().hashCode());
        poseStack.popPose();
    }

    public void drawFlatFace(GuiGraphics gg, ResolvableProfile profile, int x, int y, int outSize) {
        Minecraft mc = Minecraft.getInstance();

        // 1) Get a skin (sync "insecure" is fine; or use getOrLoad(profile.gameProfile()) for async)
        var skin = mc.getSkinManager().getInsecureSkin(profile.gameProfile()); // may return default Steve/Alex
        ResourceLocation tex = skin.texture(); // in 1.21.x this is the full skin sheet (64x64)

        // 2) Draw base face
        gg.blit(
                tex,
                x, y,                               // screen position
                0,                                  // z
                FACE_U, FACE_V,                     // u, v (source origin)
                FACE_SIZE, FACE_SIZE,               // width, height (source slice)
                TEX_W, TEX_H                        // full texture size
        );

        // Scale up to outSize if you want (draw it scaled via blit with width/height)
        if (outSize != FACE_SIZE) {
            // Quick scale: draw to a temp RenderTarget, or simply repeat blit with width/height params:
            gg.blit(tex, x, y, 0, FACE_U, FACE_V, outSize, outSize, TEX_W, TEX_H);
        }

        // 3) Draw the hat/overlay on top (alpha respected)
        gg.blit(tex, x, y, 0, HAT_U, HAT_V, outSize, outSize, TEX_W, TEX_H);
    }

    public static void renderFlatFaceQuad(PoseStack ps, ItemDisplayContext displayContext,  MultiBufferSource buffers, GameProfile profile,
                                          float width, float height, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        var skin = mc.getSkinManager().getInsecureSkin(profile);
        ResourceLocation tex = skin.texture();

        // Quad in local space: (0,0) to (width,height). Z = 0 plane.
        // Convert sub-UVs to 0..1:
        float u0 = 8f / 64f, v0 = 8f / 64f, u1 = 16f / 64f, v1 = 16f / 64f;       // face
        float hu0 = 40f/64f, hv0 = 8f/64f,  hu1 = 48f/64f, hv1 = 16f/64f;         // hat

        // Choose an entity cutout layer (respects alpha)
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(tex));
        Matrix4f mat = ps.last().pose();

        ps.pushPose();
        switch (displayContext) {
            case GUI -> {
                ps.scale(0.6f, 0.6f, 0.6f);
                //ps.translate(0.5f, 0.6f, 0.70f);
                //poseStack.scale(0.8f, 0.8f, 0.8f);
            }
//            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
//                poseStack.translate(0.25F, 0.5f, 0f);
//                  poseStack.scale(0.8F, 0.8F, 0.8F);
//            }
//            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
//                poseStack.translate(0.25F, 0.5f, 0f);
//                poseStack.scale(0.8F, 0.8F, 0.8F);
//            }
            default -> {
                //ps.scale(0.2f, 0.2f, 0.2f);
                //ps.mulPose(Axis.YN.rotationDegrees(180.0f));
                //ps.translate(0.2f, 0.6f, 0.70f);
                //poseStack.translate(0.5f, 0.5f, 0f);
                //ps.scale(0.8f, 0.8f, 0.8f);
            }
        }
        // Draw base face quad
        putQuad(vc, mat, 0, 0, width, height, u0, v0, u1, v1, light, overlay);

        // Draw hat overlay on top (same quad, different UVs)
        VertexConsumer vcHat = buffers.getBuffer(RenderType.entityTranslucent(tex));
        putQuad(vcHat, mat, 0, 0, width, height, hu0, hv0, hu1, hv1, light, overlay);
        ps.popPose();
    }

    private static void putQuad(VertexConsumer vc, Matrix4f mat,
                                float x, float y, float w, float h,
                                float u0, float v0, float u1, float v1,
                                int light, int overlay) {
        // clockwise two triangles
        vc.addVertex(mat, x,     y,     0).setColor(255,255,255,255).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x,     y + h, 0).setColor(255,255,255,255).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x + w, y + h, 0).setColor(255,255,255,255).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x + w, y,     0).setColor(255,255,255,255).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0,0,1);
    }
}