package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillagerInteractionGameTests {
    private VillagerInteractionGameTests() {}

    /** Right-clicking a villager opens the mod's interaction menu on the server, in place of vanilla's. */
    @GameTest(template = ARENA)
    public static void rightClickingAVillagerOpensTheInteractionMenu(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(4.5, 1, 4.5));
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
        ServerPlayer player = survivalPlayer(helper);

        player.interactOn(villager, InteractionHand.MAIN_HAND);

        assertTrue(helper, "the interaction menu to be open, but it is " + player.containerMenu.getClass().getSimpleName(),
                player.containerMenu instanceof VillagerInteractionMenu);
        helper.succeed();
    }
}
