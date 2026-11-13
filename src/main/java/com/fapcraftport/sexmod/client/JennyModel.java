package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.SexMod;
import com.fapcraftport.sexmod.entity.JennyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class JennyModel extends GeoModel<JennyEntity> {
    private static final ResourceLocation DRESSED = SexMod.id("geo/jenny/jennydressed.geo.json");
    private static final ResourceLocation NUDE = SexMod.id("geo/jenny/jennynude.geo.json");
    private static final ResourceLocation TEXTURE = SexMod.id("textures/entity/jenny/jenny.png");
    private static final ResourceLocation ANIMATIONS = SexMod.id("animations/jenny/jenny.animation.json");

    @Override
    public ResourceLocation getModelResource(JennyEntity jenny) {
        return jenny.isNude() ? NUDE : DRESSED;
    }

    @Override
    public ResourceLocation getTextureResource(JennyEntity jenny) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(JennyEntity jenny) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(JennyEntity jenny, long instanceId, AnimationState<JennyEntity> animationState) {
        super.setCustomAnimations(jenny, instanceId, animationState);

        boolean showPlayer = jenny.getState().isLocked() && jenny.getPartnerId().isPresent();
        boolean slim = false;
        if (showPlayer && Minecraft.getInstance().level != null) {
            Player partner = jenny.getPartner();
            slim = partner instanceof AbstractClientPlayer acp && acp.getSkin().model() == PlayerSkin.Model.SLIM;
        }

        for (GeoBone bone : getAnimationProcessor().getRegisteredBones()) {
            String name = bone.getName();
            if (name.equals("steve")) {
                bone.setHidden(!showPlayer);
            } else if (name.startsWith("armor") || name.startsWith("custom")
                    || name.equals("weapon") || name.equals("offhand")
                    || name.equals("items") || name.equals("itemRenderer")) {
                // Armour, held items and custom-head slots aren't ported yet.
                bone.setHidden(true);
            } else if (name.endsWith("Alex")) {
                bone.setHidden(!slim);
            } else if (name.endsWith("Steve") && !name.equals("steve")) {
                bone.setHidden(slim);
            }
        }
    }
}
