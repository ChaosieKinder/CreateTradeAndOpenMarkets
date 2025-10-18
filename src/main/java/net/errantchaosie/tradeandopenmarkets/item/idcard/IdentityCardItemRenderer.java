package net.errantchaosie.tradeandopenmarkets.item.idcard;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.component.IdentityCardComponent;
import net.errantchaosie.tradeandopenmarkets.component.IdentityCardData;
import net.errantchaosie.tradeandopenmarkets.item.idcard.client.IdentityCardClientCache;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.phys.Vec3;
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
                    poseStack.translate(0.20f, 0.25f, 0.45f);
                    // Draw player picture
                    renderFlatFaceQuad(poseStack, displayContext, buffer, data.toProfile(), 0.5f, 0.5f, packedLight, packedOverlay);
                }
                default -> { // Anything in-world
                    poseStack.translate(0.05f, 0.30f, 0.45f); //y=0.3 is too low
                    // Draw player picture
                    renderFlatFaceQuad(poseStack, displayContext, buffer, data.toProfile(), 0.3f, 0.3f, packedLight, packedOverlay);
                }
            }
            if(displayContext != ItemDisplayContext.GUI) {
                _drawIdCardText(poseStack, buffer, data, packedLight, packedOverlay);
            }
            poseStack.popPose();
        });
    }

    private static int getY() {
        return 120;
    }

    private void _drawIdCardText(PoseStack poseStack, MultiBufferSource buffer, IdentityCardData data, int packedLight, int packedOverlay) {
        IdentityCardPlayerSnapshot snapshot = IdentityCardClientCache.get(data.playerId()).orElse(null);

        int fontColor = 0x000000;

        if (snapshot != null) {
            // Player Xp Bar
            poseStack.pushPose();
            poseStack.translate(0.150f, 0.31f, 0); // was 0.150f, 0.3f, 0, which put us directly under the head
            renderXpBarBillboard(buffer, poseStack, 0, 0, 0, snapshot.experienceProgress(), 0.3f, 0.14f, packedLight);
            poseStack.popPose();

            // Player Level
            poseStack.pushPose();
            poseStack.scale(0.005f, 0.005f, 1f);
            poseStack.translate(0.150f, 0.34f, -0.004);
            String levelText = String.valueOf(snapshot.level());
            int levelTextWidth = textRenderer.width(levelText);

            textRenderer.drawInBatch(levelText,
                30f - levelTextWidth/2.0f,
                60f,
                0xFF22CC22, // xp green = 0xFF55FF55
                false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        // Player Name
        poseStack.pushPose();
        poseStack.scale(0.01f, 0.01f, 1f);
        textRenderer.drawInBatch(data.playerName(),
                0f,
                34f, // 38 is too low, 32 is too high
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
                fontColor, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);

        // Dynamic player information is available here for rendering when needed.
        // It may not be available and should be skipped if it is not.
        if (snapshot != null) {
            textRenderer.drawInBatch("Age: " + snapshot.timePlayedDays() + "d " + (Math.round(snapshot.timePlayedHours() * 2400) / 100) + "h",
                    80,
                    10,
                    fontColor, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);

            //Heart and health
//            drawHeart(poseStack, buffer, 0, 50, -1, 16, 16, packedLight);
//            textRenderer.drawInBatch(snapshot.health() + "/" + snapshot.maxHealth(),
//                    80,
//                    20,
//                    fontColor, false, poseStack.last().pose(), buffer,
//                    Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);

            var spawnLocation = snapshot.spawnLocation();
            var spawnLocationText = spawnLocation.isPresent()
                    ? spawnLocation.get().x() + ", " + spawnLocation.get().y() +  ", " + spawnLocation.get().z()
                    : "world spawn";
            textRenderer.drawInBatch(spawnLocationText,
                    80,
                    30,
                    fontColor, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);
            textRenderer.drawInBatch(snapshot.spawnDimension(),
                    80,
                    40,
                    fontColor, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);

            textRenderer.drawInBatch("height: " + snapshot.height(),
                    80,
                    74,
                    fontColor, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, /*light*/ 0xF000F0);
        } else {
            // perhaps some of the random cycling text can be put in its place?
        }

        // Player Id
        textRenderer.drawInBatch("ID: " + data.playerId().toString(),
                -4,
                120,
                fontColor, false, poseStack.last().pose(), buffer,
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

    private static void renderXpBarBillboard(MultiBufferSource buffers,PoseStack ps,
                                             double x, double y, double z,
                                             float fill01,
                                             float width, float height, int packedLight) {
        // Position relative to camera
        ps.pushPose();

        // Slight forward nudge to avoid z-fighting with the surface behind
        ps.translate(0, 0, -0.001f);

        // World-space size → scale the unit quad
        // We’ll render a unit quad from (−0.5..+0.5) in X, (0..1) in Y, then scale to width/height
        ps.scale(width, height, 1f);

        // Fullbright + no overlay (so it’s readable regardless of light)
        int overlay = OverlayTexture.NO_OVERLAY;

        // Choose a GUI-ish render type that respects transparency & ignores lighting
        VertexConsumer vc = buffers.getBuffer(RenderType.gui());

        // Colors (ARGB)
        int bg = 0xAA000000;    // semi-opaque black
        int border = 0xFF2A2A2A;
        int fill = 0xFF55FF55;  // XP green

        // Draw order: border -> background -> fill
        rect(vc, ps, -0.52f, -0.04f, 0, 1.04f, 0.18f, border, packedLight, overlay);  // border slightly larger
        rect(vc, ps, -0.5f,   0.00f, -0.0001f,  1.00f, 0.12f, bg,     packedLight, overlay);  // background
        rect(vc, ps, -0.5f,   0.00f, -0.0002f,  fill01, 0.12f, fill,  packedLight, overlay);  // filled portion

        ps.popPose();
    }
    private static void rect(VertexConsumer vc, PoseStack ps,
                             float x, float y, float z, float w, float h,
                             int argb, int light, int overlay) {
        float x0 = x,    y0 = y;
        float x1 = x + w, y1 = y + h;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>>  8) & 0xFF;
        int b = (argb       ) & 0xFF;

        Matrix4f mat = ps.last().pose();

        vc.addVertex(mat, x0, y0, z).setColor(r,g,b,a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x0, y1, z).setColor(r,g,b,a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1, y1, z).setColor(r,g,b,a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1, y0, z).setColor(r,g,b,a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0,0,1);

    }
    public static void drawHeart(PoseStack ps, MultiBufferSource buffers,
                                 float x, float y, float z, float width, float height,
                                 int light) {

        var mc = Minecraft.getInstance();
        var guiSprites = mc.getGuiSprites();
        TextureAtlasSprite cont  = guiSprites.getSprite(ResourceLocation.withDefaultNamespace("hud/heart_container"));
        TextureAtlasSprite fill  = guiSprites.getSprite(ResourceLocation.withDefaultNamespace("hud/heart_full"));
        RenderType rtCont  = RenderType.entityTranslucent(cont.atlasLocation());
        RenderType rtFill  = RenderType.entityTranslucent(fill.atlasLocation());

        int overlay = OverlayTexture.NO_OVERLAY;

        ps.pushPose();
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        ps.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        ps.mulPose(Axis.YP.rotationDegrees(-cam.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(cam.getXRot()));
        ps.translate(0.0f, 0.0f, -0.001f); // avoid z-fight
        ps.scale(width, height, 1.0f);

        drawSpriteQuad(buffers.getBuffer(rtCont),  ps, cont,  -0.5f, 0.0f, 1.0f, 1.0f, 0xFFFFFFFF, light, overlay);
        drawSpriteQuad(buffers.getBuffer(rtFill),  ps, fill,  -0.5f, 0.0f, 1.0f, 1.0f, 0xFFFFFFFF, light, overlay);
        ps.popPose();

    }
    private static void drawSpriteQuad(VertexConsumer vc, PoseStack ps,
                                       TextureAtlasSprite sprite,
                                       float x, float y, float w, float h,
                                       int argb, int light, int overlay) {
        float x0 = x,     y0 = y;
        float x1 = x + w, y1 = y + h;

        float u0 = sprite.getU0(), v0 = sprite.getV0();
        float u1 = sprite.getU1(), v1 = sprite.getV1();

        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>>  8) & 0xFF;
        int b = (argb       ) & 0xFF;

        Matrix4f mat = ps.last().pose();

        // two triangles (clockwise), fluent-style
        vc.addVertex(mat, x0, y0, 0).setColor(r,g,b,a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x0, y1, 0).setColor(r,g,b,a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1, y1, 0).setColor(r,g,b,a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1, y0, 0).setColor(r,g,b,a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0,0,1);
    }
}