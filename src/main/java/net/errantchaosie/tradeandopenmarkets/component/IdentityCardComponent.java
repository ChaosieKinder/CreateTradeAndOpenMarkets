package net.errantchaosie.tradeandopenmarkets.component;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public final class IdentityCardComponent {
    private IdentityCardComponent() {}

    private static @Nullable DataComponentType<IdentityCardData> typeOrNull() {
        return TAOMComponents.IDENTITY_CARD_DATA.isBound() ? TAOMComponents.IDENTITY_CARD_DATA.get() : null;
    }

    public static Optional<IdentityCardData> get(ItemStack stack) {
        var type = typeOrNull();
        if (type == null) {
            return Optional.empty();
        }
        IdentityCardData data = stack.get(type);
        return Optional.ofNullable(data);
    }

    public static boolean hasIdentity(ItemStack stack) {
        return get(stack).isPresent();
    }

    public static void set(ItemStack stack, IdentityCardData data) {
        var type = typeOrNull();
        if (type == null) {
            return;
        }
        stack.set(type, data);
    }
}