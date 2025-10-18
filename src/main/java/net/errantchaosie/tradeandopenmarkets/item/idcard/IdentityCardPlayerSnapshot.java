package net.errantchaosie.tradeandopenmarkets.item.idcard;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Snapshot of player state that can be displayed on an identity card.
 */
public record IdentityCardPlayerSnapshot(
        UUID playerId,
        Component spawnDimension,
        Optional<Position> spawnLocation,
        Component currentDimension,
        Position currentLocation,
        float health,
        float maxHealth,
        int level,
        float experienceProgress,
        int armor,
        float armorToughness,
        float height,
        int timePlayedDays,
        float timePlayedHours
) {

    public static IdentityCardPlayerSnapshot from(ServerPlayer player) {
        Component spawnDimension = dimensionName(player.getRespawnDimension());
        Optional<Position> spawnLocation = Optional.ofNullable(player.getRespawnPosition())
                .map(Position::fromBlockPos);

        Level level = player.level();
        Component currentDimension = dimensionName(level.dimension());
        Position currentLocation = Position.fromVec3(player.position());

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int experienceLevel = player.experienceLevel;
        float experienceProgress = player.experienceProgress;
        int armor = player.getArmorValue();
        float armorToughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float height = player.getDimensions(player.getPose()).height();
        int timePlayedDays = 0;
        float timePlayedHours = 0.0f;

        ServerStatsCounter statsCounter = player.getStats();
        if (statsCounter != null) {
            Stat<ResourceLocation> playTimeStat = Stats.CUSTOM.get(Stats.PLAY_TIME);
            int ticksPlayed = statsCounter.getValue(playTimeStat);
            double totalHours = ticksPlayed / 72000.0d;
            timePlayedDays = (int) (totalHours / 24.0d);
            timePlayedHours = (float) (totalHours - (timePlayedDays * 24.0d));
        }

        return new IdentityCardPlayerSnapshot(
                player.getUUID(),
                spawnDimension,
                spawnLocation,
                currentDimension,
                currentLocation,
                health,
                maxHealth,
                experienceLevel,
                experienceProgress,
                armor,
                armorToughness,
                height,
                timePlayedDays,
                timePlayedHours
        );
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        ComponentSerialization.STREAM_CODEC.encode(buf, spawnDimension);
        buf.writeBoolean(spawnLocation.isPresent());
        spawnLocation.ifPresent(position -> position.encode(buf));
        ComponentSerialization.STREAM_CODEC.encode(buf, currentDimension);
        currentLocation.encode(buf);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeVarInt(level);
        buf.writeFloat(experienceProgress);
        buf.writeVarInt(armor);
        buf.writeFloat(armorToughness);
        buf.writeFloat(height);
        buf.writeVarInt(timePlayedDays);
        buf.writeFloat(timePlayedHours);
    }

    public static IdentityCardPlayerSnapshot decode(RegistryFriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        Component spawnDimension = ComponentSerialization.STREAM_CODEC.decode(buf);
        Optional<Position> spawnLocation = buf.readBoolean()
                ? Optional.of(Position.decode(buf))
                : Optional.empty();
        Component currentDimension = ComponentSerialization.STREAM_CODEC.decode(buf);
        Position currentLocation = Position.decode(buf);
        float health = buf.readFloat();
        float maxHealth = buf.readFloat();
        int level = buf.readVarInt();
        float experienceProgress = buf.readFloat();
        int armor = buf.readVarInt();
        float armorToughness = buf.readFloat();
        float height = buf.readFloat();
        int timePlayedDays = buf.readVarInt();
        float timePlayedHours = buf.readFloat();

        return new IdentityCardPlayerSnapshot(
                playerId,
                spawnDimension,
                spawnLocation,
                currentDimension,
                currentLocation,
                health,
                maxHealth,
                level,
                experienceProgress,
                armor,
                armorToughness,
                height,
                timePlayedDays,
                timePlayedHours
        );
    }

    private static Component dimensionName(ResourceKey<Level> dimensionKey) {
        ResourceLocation location = dimensionKey.location();
        return Component.translatable(Util.makeDescriptionId("dimension", location));
    }

    /**
     * Simple XYZ container for serialisation.
     */
    public record Position(double x, double y, double z) {
        private static Position fromBlockPos(BlockPos pos) {
            return new Position(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d);
        }

        private static Position fromVec3(Vec3 vec) {
            return new Position(vec.x, vec.y, vec.z);
        }

        private void encode(FriendlyByteBuf buf) {
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
        }

        private static Position decode(FriendlyByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            return new Position(x, y, z);
        }
    }
}