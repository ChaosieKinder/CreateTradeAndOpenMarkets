package net.errantchaosie.tradeandopenmarkets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TAOMTags {

    public static class Blocks {

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, name));
        }
    }
    public static class Items {
        public static final TagKey<Item> CURRENCY_ITEMS = createTag("taom_currency_items");
        public static final TagKey<Item> ID_CARD_ITEMS = createTag("taom_id_card_items");
        public static final TagKey<Item> DEBIT_CARD_ITEMS = createTag("taom_debit_items");
        /**
         * These items can go inside a wallet
         */
        public static final TagKey<Item> WALLET_ITEMS = createTag("wallet_items");
        /**
         * These items are wallets that go in the curios slot
         */
        public static final TagKey<Item> WALLET_SLOT_ITEM = createTag("wallet");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(TradeAndOpenMarketsMod.MOD_ID, name));
        }
    }
}
