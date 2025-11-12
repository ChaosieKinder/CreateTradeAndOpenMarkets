package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic implementation for item based tabs like buying or selling.
 */
public class SimpleShopTab implements ShopTab {
    private final ResourceLocation type;
    private final Component title;
    private final List<ShopEntry> entries;

    public SimpleShopTab(ResourceLocation type, Component title, List<ShopEntry> entries) {
        this.type = type;
        this.title = title;
        this.entries = new ArrayList<>(entries);
    }

    @Override
    public ResourceLocation type() {
        return this.type;
    }

    @Override
    public Component title() {
        return this.title;
    }

    public List<ShopEntry> entries() {
        return Collections.unmodifiableList(this.entries);
    }

    protected List<ShopEntry> mutableEntries() {
        return this.entries;
    }
}
