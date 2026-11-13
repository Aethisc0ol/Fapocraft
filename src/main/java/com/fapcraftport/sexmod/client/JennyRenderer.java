package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.SexMod;
import com.fapcraftport.sexmod.entity.JennyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Jenny's model contains the partner's body (bone "steve" and children). Like the original,
 * that part is drawn with the partner's own skin, except the "cock" group, which uses the
 * mod's textures/player/steve.png.
 */
public class JennyRenderer extends GeoEntityRenderer<JennyEntity> {
    private static final ResourceLocation STEVE_EXTRAS = SexMod.id("textures/player/steve.png");

    private enum Region { GIRL, PLAYER_SKIN, PLAYER_EXTRAS }

    public JennyRenderer(EntityRendererProvider.Context context) {
        super(context, new JennyModel());
        this.shadowRadius = 0.4F;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, JennyEntity jenny, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        if (isReRender) {
            super.renderRecursively(poseStack, jenny, bone, renderType, bufferSource, buffer, true,
                    partialTick, packedLight, packedOverlay, colour);
            return;
        }
        RenderType type = switch (region(bone)) {
            case GIRL -> RenderType.entityCutoutNoCull(getTextureLocation(jenny));
            case PLAYER_SKIN -> RenderType.entityTranslucent(partnerSkin(jenny));
            case PLAYER_EXTRAS -> RenderType.entityTranslucent(STEVE_EXTRAS);
        };
        // Always re-fetch: switching render types can end the previous batch.
        VertexConsumer consumer = bufferSource.getBuffer(type);
        super.renderRecursively(poseStack, jenny, bone, type, bufferSource, consumer, false,
                partialTick, packedLight, packedOverlay, colour);
    }

    private static Region region(GeoBone bone) {
        for (GeoBone b = bone; b != null; b = b.getParent()) {
            String name = b.getName();
            if (name.equals("cock")) return Region.PLAYER_EXTRAS;
            if (name.equals("steve")) return Region.PLAYER_SKIN;
        }
        return Region.GIRL;
    }

    private static ResourceLocation partnerSkin(JennyEntity jenny) {
        Player partner = jenny.getPartner();
        if (partner instanceof AbstractClientPlayer acp) return acp.getSkin().texture();
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
