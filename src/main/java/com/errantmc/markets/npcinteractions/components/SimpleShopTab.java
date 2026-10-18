package com.errantmc.markets.npcinteractions.components;

import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic implementation for item based tabs like buying or selling.
 */
public abstract class SimpleShopTab extends InteractionTab {

    private final Component title;
    private final List<ShopEntry> entries;

    public SimpleShopTab(ResourceLocation type, Component title, List<ShopEntry> entries) {
        super(type);
        this.title = title;
        this.entries = new ArrayList<>(entries);
    }

    public SimpleShopTab(ResourceLocation type, RegistryFriendlyByteBuf buf) {
        super(type, buf);
        this.title = ComponentSerialization.STREAM_CODEC.decode(buf);
        int entryCount = buf.readVarInt();
        this.entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            this.entries.add(new ShopEntry(buf));
        }
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf buffer) {
        super.writeToStream(buffer);
        ComponentSerialization.STREAM_CODEC.encode(buffer, this.title);
        buffer.writeVarInt(this.entries.size());
        for (ShopEntry entry : this.entries) {
            entry.writeToStream(buffer);
        }
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
