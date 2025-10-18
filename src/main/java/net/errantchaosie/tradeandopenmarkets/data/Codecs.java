package net.errantchaosie.tradeandopenmarkets.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class Codecs {
    public final class NetCodecs {

        public static final StreamCodec<FriendlyByteBuf, java.util.UUID> UUID_STREAM =
                StreamCodec.of(
                        (FriendlyByteBuf buf, UUID id) -> buf.writeUUID(id),
                        (FriendlyByteBuf buf) -> buf.readUUID()
        );


    }
}
