package com.errantmc.markets.component;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Data component payload stored on ID cards.
 */
public record IdentityCardData(UUID playerId, String playerName, long createdAtMillis) {

    public static final Codec<IdentityCardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString)
                    .fieldOf("playerId")
                    .forGetter(IdentityCardData::playerId),
            Codec.STRING
                    .fieldOf("playerName")
                    .forGetter(IdentityCardData::playerName),
            Codec.LONG
                    .fieldOf("createdAtMillis")
                    .forGetter(IdentityCardData::createdAtMillis)
    ).apply(instance, IdentityCardData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityCardData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public IdentityCardData decode(RegistryFriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            long created = buf.readVarLong();
            return new IdentityCardData(id, name, created);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, IdentityCardData value) {
            buf.writeUUID(value.playerId());
            buf.writeUtf(value.playerName());
            buf.writeVarLong(value.createdAtMillis());
        }
    };

    public static IdentityCardData of(Player player) {
        Objects.requireNonNull(player, "player");
        return new IdentityCardData(player.getUUID(), player.getGameProfile().getName(), Instant.now().toEpochMilli());
    }

    public GameProfile toProfile() {
        return new GameProfile(playerId, playerName);
    }


    public ItemStack createHeadStack() {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(toProfile()));
        return stack;
    }

}