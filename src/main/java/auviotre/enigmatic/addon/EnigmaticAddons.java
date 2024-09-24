package auviotre.enigmatic.addon;

import auviotre.enigmatic.addon.client.handlers.ClientEventHandler;
import auviotre.enigmatic.addon.contents.brewing.AstralBrewingRecipe;
import auviotre.enigmatic.addon.handlers.AddonEventHandler;
import auviotre.enigmatic.addon.handlers.AddonKeybindHandler;
import auviotre.enigmatic.addon.handlers.OmniconfigAddonHandler;
import auviotre.enigmatic.addon.helpers.PotionAddonHelper;
import auviotre.enigmatic.addon.packets.PacketCursedXPScrollKey;
import auviotre.enigmatic.addon.proxy.ClientProxy;
import auviotre.enigmatic.addon.proxy.CommonProxy;
import auviotre.enigmatic.addon.registries.*;
import com.aizistral.enigmaticlegacy.brewing.ValidationBrewingRecipe;
import com.aizistral.enigmaticlegacy.objects.LoggerWrapper;
import com.aizistral.enigmaticlegacy.registries.EnigmaticBlocks;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.aizistral.enigmaticlegacy.registries.EnigmaticTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import static auviotre.enigmatic.addon.registries.AbstractRegistry.loadClass;


@Mod(EnigmaticAddons.MODID)
public class EnigmaticAddons {
    public static final String MODID = "enigmaticaddons";
    public static final LoggerWrapper LOGGER = new LoggerWrapper("Enigmatic Addons");
    public static SimpleChannel packetInstance;
    public static final CommonProxy PROXY = DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);

    public EnigmaticAddons() {
        LOGGER.info("Constructing mod instance...");
        OmniconfigAddonHandler.initialize();
        loadClass(EnigmaticAddonItems.class);
        loadClass(EnigmaticAddonMenus.class);
        loadClass(EnigmaticAddonEffects.class);
        loadClass(EnigmaticAddonRecipes.class);
        loadClass(EnigmaticAddonEntities.class);
        loadClass(EnigmaticAddonParticles.class);
        loadClass(EnigmaticAddonEnchantments.class);
        loadClass(EnigmaticAddonLootModifier.class);
        if (OmniconfigAddonHandler.FutureItemDisplay.getValue())
            loadClass(FutureItems.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientRegistries);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        FMLJavaModLoadingContext.get().getModEventBus().register(PROXY);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PROXY);
        MinecraftForge.EVENT_BUS.register(new AddonEventHandler());
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new AddonKeybindHandler());
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        LOGGER.info("Mod instance constructed successfully.");
    }

    private void setup(FMLCommonSetupEvent event) {
        LOGGER.info("Initializing common setup phase...");
        event.enqueueWork(() -> loadClass(EnigmaticAddonDamageTypes.class));
        event.enqueueWork(() -> loadClass(EnigmaticAddonPotions.class));
        LOGGER.info("Registering packets...");
        packetInstance = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MODID, "main")).networkProtocolVersion(() -> "1").clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
        packetInstance.registerMessage(0, PacketCursedXPScrollKey.class, PacketCursedXPScrollKey::encode, PacketCursedXPScrollKey::decode, PacketCursedXPScrollKey::handle);
        LOGGER.info("Common setup phase finished successfully.");
    }

    public void onLoadComplete(FMLLoadCompleteEvent event) {
        LOGGER.info("Initializing load completion phase...");
        EnigmaticItems.SPELLSTONES.add(EnigmaticAddonItems.FORGOTTEN_ICE);
        EnigmaticItems.SPELLSTONES.add(EnigmaticAddonItems.REVIVAL_LEAF);
        EnigmaticItems.SPELLSTONES.add(EnigmaticAddonItems.LOST_ENGINE);
        LOGGER.info("Registering brewing recipes...");
        if (OmniconfigAddonHandler.isItemEnabled(EnigmaticAddonItems.COMMON_POTION)) {
            PotionAddonHelper.registerCommonPotions();
        }
        if (OmniconfigAddonHandler.isItemEnabled(EnigmaticAddonItems.ULTIMATE_POTION)) {
            PotionAddonHelper.registerUltimatePotions();
        }
        if (OmniconfigAddonHandler.isItemEnabled(EnigmaticAddonItems.ASTRAL_POTION)) {
            BrewingRecipeRegistry.addRecipe(new AstralBrewingRecipe(new ResourceLocation(MODID, "astral_potion")));
        }
        PotionAddonHelper.registerDispenserBehavior();
        BrewingRecipeRegistry.addRecipe(new ValidationBrewingRecipe(PotionAddonHelper.SPECIAL_POTIONS, null));
        LOGGER.info("Load completion phase finished successfully");
    }

    private void clientRegistries(FMLClientSetupEvent event) {
        LOGGER.info("Initializing client setup phase...");
        PROXY.clientInit();
        LOGGER.info("Client setup phase finished successfully.");
    }

    private void onServerStart(ServerAboutToStartEvent event) {
        this.performCleanup();
    }

    public void performCleanup() {
        AddonEventHandler.NIGHT_SCROLL_BOXES.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void buildCreativeModeTabContents(@NotNull BuildCreativeModeTabContentsEvent event) {
        MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> entries = event.getEntries();
        if (event.getTab() == EnigmaticTabs.MAIN && OmniconfigAddonHandler.TabResorted.getValue()) {
            putAfter(entries, EnigmaticItems.ENIGMATIC_ITEM, EnigmaticItems.THE_ACKNOWLEDGMENT);
            entries.putAfter(EnigmaticItems.ENIGMATIC_AMULET.getCreativeTabStacks().get(6), new ItemStack(EnigmaticItems.IRON_RING), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            putAfter(entries, EnigmaticItems.IRON_RING, EnigmaticItems.GOLDEN_RING);
            putAfter(entries, EnigmaticItems.GOLDEN_RING, EnigmaticItems.ENDER_RING);
            putAfter(entries, EnigmaticItems.ENDER_RING, EnigmaticAddonItems.QUARTZ_RING);
            putAfter(entries, EnigmaticAddonItems.QUARTZ_RING, EnigmaticItems.MAGNET_RING);
            putAfter(entries, EnigmaticItems.MAGNET_RING, EnigmaticItems.SUPER_MAGNET_RING);
            putAfter(entries, EnigmaticItems.SUPER_MAGNET_RING, EnigmaticItems.ANIMAL_GUIDEBOOK);
            putAfter(entries, EnigmaticItems.ANIMAL_GUIDEBOOK, EnigmaticItems.HUNTER_GUIDEBOOK);
            putAfter(entries, EnigmaticItems.HUNTER_GUIDEBOOK, EnigmaticAddonItems.LIVING_ODE);
            putAfter(entries, EnigmaticAddonItems.LIVING_ODE, EnigmaticItems.ENIGMATIC_EYE);
            putAfter(entries, EnigmaticItems.ENIGMATIC_EYE, EnigmaticItems.TATTERED_TOME);
            putAfter(entries, EnigmaticItems.TATTERED_TOME, EnigmaticItems.WITHERED_TOME);
            putAfter(entries, EnigmaticItems.WITHERED_TOME, EnigmaticItems.CORRUPTED_TOME);
            putAfter(entries, EnigmaticItems.CORRUPTED_TOME, EnigmaticItems.THICC_SCROLL);
            putAfter(entries, EnigmaticItems.THICC_SCROLL, EnigmaticItems.XP_SCROLL);
            putAfter(entries, EnigmaticItems.XP_SCROLL, EnigmaticItems.ESCAPE_SCROLL);
            putAfter(entries, EnigmaticItems.ESCAPE_SCROLL, EnigmaticItems.HEAVEN_SCROLL);
            putAfter(entries, EnigmaticItems.HEAVEN_SCROLL, EnigmaticItems.FABULOUS_SCROLL);
            putAfter(entries, EnigmaticItems.FABULOUS_SCROLL, EnigmaticItems.EARTH_HEART);
            putAfter(entries, EnigmaticItems.EARTH_HEART, EnigmaticAddonItems.EARTH_HEART_FRAGMENT);
            putAfter(entries, EnigmaticAddonItems.EARTH_HEART_FRAGMENT, EnigmaticItems.EXTRADIMENSIONAL_EYE);
            putAfter(entries, EnigmaticItems.EXTRADIMENSIONAL_EYE, EnigmaticItems.MINING_CHARM);
            putAfter(entries, EnigmaticItems.MINING_CHARM, EnigmaticItems.MONSTER_CHARM);
            putAfter(entries, EnigmaticItems.MONSTER_CHARM, EnigmaticAddonItems.ADVENTURE_CHARM);
            putAfter(entries, EnigmaticAddonItems.ADVENTURE_CHARM, EnigmaticItems.ENCHANTMENT_TRANSPOSER);
            putAfter(entries, EnigmaticItems.ENCHANTMENT_TRANSPOSER, EnigmaticItems.INFINIMEAL);
            putAfter(entries, EnigmaticItems.INFINIMEAL, EnigmaticItems.RECALL_POTION);
            putAfter(entries, EnigmaticItems.ANGEL_BLESSING, EnigmaticAddonItems.FORGOTTEN_ICE);
            putAfter(entries, EnigmaticAddonItems.FORGOTTEN_ICE, EnigmaticAddonItems.REVIVAL_LEAF);
            putAfter(entries, EnigmaticAddonItems.REVIVAL_LEAF, EnigmaticAddonItems.LOST_ENGINE);
            putAfter(entries, EnigmaticAddonItems.LOST_ENGINE, EnigmaticItems.LORE_INSCRIBER);
            putAfter(entries, EnigmaticItems.LORE_INSCRIBER, EnigmaticItems.LORE_FRAGMENT);
            putAfter(entries, EnigmaticItems.LORE_FRAGMENT, EnigmaticItems.VOID_STONE);
            putAfter(entries, EnigmaticItems.VOID_STONE, EnigmaticItems.UNHOLY_GRAIL);
            putAfter(entries, EnigmaticItems.UNHOLY_GRAIL, EnigmaticAddonItems.ANTIQUE_BAG);
            putAfter(entries, EnigmaticAddonItems.ANTIQUE_BAG, EnigmaticAddonItems.VOID_TOME);
            putAfter(entries, EnigmaticAddonItems.VOID_TOME, EnigmaticAddonItems.HELL_BLADE_CHARM);
            putAfter(entries, EnigmaticAddonItems.HELL_BLADE_CHARM, EnigmaticItems.MEGA_SPONGE);
            putAfter(entries, EnigmaticItems.MEGA_SPONGE, EnigmaticItems.FORBIDDEN_AXE);
            putAfter(entries, EnigmaticItems.FORBIDDEN_AXE, EnigmaticAddonItems.ICHOR_DROPLET);
            putAfter(entries, EnigmaticAddonItems.ICHOR_DROPLET, EnigmaticAddonItems.ICHOROOT);
            putAfter(entries, EnigmaticAddonItems.ICHOROOT, EnigmaticItems.ASTRAL_DUST);
            putAfter(entries, EnigmaticItems.ASTRAL_DUST, EnigmaticBlocks.ASTRAL_BLOCK);
            putAfter(entries, EnigmaticBlocks.ASTRAL_BLOCK, EnigmaticItems.ENDER_ROD);
            putAfter(entries, EnigmaticItems.ENDER_ROD, EnigmaticItems.MENDING_MIXTURE);
            putAfter(entries, EnigmaticItems.MENDING_MIXTURE, EnigmaticItems.ASTRAL_POTATO);
            putAfter(entries, EnigmaticItems.ASTRAL_POTATO, EnigmaticAddonItems.ASTRAL_POTION);
            putAfter(entries, EnigmaticAddonItems.ASTRAL_POTION, EnigmaticAddonItems.DRAGON_BOW);
            putAfter(entries, EnigmaticAddonItems.DRAGON_BOW, EnigmaticItems.COSMIC_HEART);
            putAfter(entries, EnigmaticItems.COSMIC_HEART, EnigmaticItems.INSIGNIA);
            putAfter(entries, EnigmaticItems.INSIGNIA, EnigmaticBlocks.COSMIC_CAKE);
            putAfter(entries, EnigmaticBlocks.COSMIC_CAKE, EnigmaticItems.UNWITNESSED_AMULET);
            putAfter(entries, EnigmaticItems.UNWITNESSED_AMULET, EnigmaticItems.ASCENSION_AMULET);
            putAfter(entries, EnigmaticItems.ASCENSION_AMULET, EnigmaticItems.ICHOR_BOTTLE);
            putAfter(entries, EnigmaticItems.ICHOR_BOTTLE, EnigmaticItems.FORBIDDEN_FRUIT);
            putAfter(entries, EnigmaticItems.FORBIDDEN_FRUIT, EnigmaticAddonItems.PRIMEVAL_CUBE);
            putAfter(entries, EnigmaticAddonItems.PRIMEVAL_CUBE, EnigmaticItems.THE_CUBE);
            putAfter(entries, EnigmaticItems.THE_CUBE, EnigmaticBlocks.BIG_LAMP);
            putAfter(entries, EnigmaticBlocks.BIG_LAMP, EnigmaticBlocks.MASSIVE_LAMP);
            putAfter(entries, EnigmaticBlocks.MASSIVE_LAMP, EnigmaticBlocks.BIG_REDSTONELAMP);
            putAfter(entries, EnigmaticBlocks.BIG_REDSTONELAMP, EnigmaticBlocks.MASSIVE_REDSTONELAMP);
            putAfter(entries, EnigmaticBlocks.MASSIVE_REDSTONELAMP, EnigmaticBlocks.BIG_SHROOMLAMP);
            putAfter(entries, EnigmaticBlocks.BIG_SHROOMLAMP, EnigmaticBlocks.MASSIVE_SHROOMLAMP);
            putAfter(entries, EnigmaticItems.ETHERIUM_SCRAPS, EnigmaticBlocks.ETHERIUM_BLOCK);
            putAfter(entries, EnigmaticItems.ASTRAL_BREAKER, EnigmaticBlocks.END_ANCHOR);
            putAfter(entries, EnigmaticBlocks.END_ANCHOR, EnigmaticItems.ENIGMATIC_ELYTRA);
            putAfter(entries, EnigmaticItems.ENIGMATIC_ELYTRA, EnigmaticItems.CURSED_RING);
            putAfter(entries, EnigmaticItems.CURSED_RING, EnigmaticItems.TWISTED_HEART);
            putAfter(entries, EnigmaticItems.TWISTED_HEART, EnigmaticItems.TWISTED_POTION);
            putAfter(entries, EnigmaticItems.TWISTED_POTION, EnigmaticItems.TWISTED_MIRROR);
            putAfter(entries, EnigmaticItems.TWISTED_MIRROR, EnigmaticItems.SOUL_COMPASS);
            putAfter(entries, EnigmaticItems.SOUL_COMPASS, EnigmaticItems.BERSERK_CHARM);
            putAfter(entries, EnigmaticItems.BERSERK_CHARM, EnigmaticItems.INFERNAL_SHIELD);
            putAfter(entries, EnigmaticItems.INFERNAL_SHIELD, EnigmaticItems.ASTRAL_FRUIT);
            putAfter(entries, EnigmaticItems.ASTRAL_FRUIT, EnigmaticItems.GUARDIAN_HEART);
            putAfter(entries, EnigmaticItems.GUARDIAN_HEART, EnigmaticItems.EVIL_ESSENCE);
            putAfter(entries, EnigmaticItems.EVIL_ESSENCE, EnigmaticItems.EVIL_INGOT);
            putAfter(entries, EnigmaticItems.EVIL_INGOT, EnigmaticItems.THE_TWIST);
            putAfter(entries, EnigmaticItems.THE_TWIST, EnigmaticItems.ENDER_SLAYER);
            putAfter(entries, EnigmaticItems.ENDER_SLAYER, EnigmaticItems.CURSE_TRANSPOSER);
            putAfter(entries, EnigmaticItems.CURSE_TRANSPOSER, EnigmaticItems.ENCHANTER_PEARL);
            putAfter(entries, EnigmaticItems.ENCHANTER_PEARL, EnigmaticAddonItems.SANGUINARY_HANDBOOK);
            putAfter(entries, EnigmaticAddonItems.SANGUINARY_HANDBOOK, EnigmaticAddonItems.FALSE_JUSTICE);
            putAfter(entries, EnigmaticAddonItems.FALSE_JUSTICE, EnigmaticItems.CURSED_STONE);
            putAfter(entries, EnigmaticItems.CURSED_STONE, EnigmaticAddonItems.PURE_HEART);
            putAfter(entries, EnigmaticAddonItems.PURE_HEART, EnigmaticAddonItems.BLESS_AMPLIFIER);
            putAfter(entries, EnigmaticAddonItems.BLESS_AMPLIFIER, EnigmaticAddonItems.EARTH_PROMISE);
            putAfter(entries, EnigmaticAddonItems.EARTH_PROMISE, EnigmaticAddonItems.BLESS_RING);
            putAfter(entries, EnigmaticAddonItems.BLESS_RING, EnigmaticItems.DARKEST_SCROLL);
            putAfter(entries, EnigmaticItems.DARKEST_SCROLL, EnigmaticAddonItems.CURSED_XP_SCROLL);
            putAfter(entries, EnigmaticAddonItems.CURSED_XP_SCROLL, EnigmaticAddonItems.NIGHT_SCROLL);
            putAfter(entries, EnigmaticAddonItems.NIGHT_SCROLL, EnigmaticItems.AVARICE_SCROLL);
            putAfter(entries, EnigmaticItems.AVARICE_SCROLL, EnigmaticItems.CURSED_SCROLL);
            putAfter(entries, EnigmaticItems.CURSED_SCROLL, EnigmaticItems.ABYSSAL_HEART);
            putAfter(entries, EnigmaticItems.ABYSSAL_HEART, EnigmaticItems.THE_INFINITUM);
            putAfter(entries, EnigmaticItems.THE_INFINITUM, EnigmaticItems.DESOLATION_RING);
            putAfter(entries, EnigmaticItems.DESOLATION_RING, EnigmaticItems.ELDRITCH_AMULET);
            putAfter(entries, EnigmaticItems.ELDRITCH_AMULET, EnigmaticItems.ELDRITCH_PAN);
            putAfter(entries, EnigmaticItems.ELDRITCH_PAN, EnigmaticItems.LOOT_GENERATOR);
            putAfter(entries, EnigmaticItems.LOOT_GENERATOR, EnigmaticItems.REDEMPTION_POTION);
            putAfter(entries, EnigmaticItems.REDEMPTION_POTION, EnigmaticItems.SOUL_CRYSTAL);
            putAfter(entries, EnigmaticItems.SOUL_CRYSTAL, EnigmaticItems.QUOTE_PLAYER);
            putAfter(entries, EnigmaticItems.QUOTE_PLAYER, EnigmaticItems.DECEPTION_AMULET);
            putAfter(entries, EnigmaticItems.DECEPTION_AMULET, EnigmaticItems.SOUL_DUST);
            putAfter(entries, EnigmaticItems.SOUL_DUST, EnigmaticItems.THE_JUDGEMENT);
            putAfter(entries, EnigmaticItems.THE_JUDGEMENT, EnigmaticBlocks.STAR_FABRIC);

            if (OmniconfigAddonHandler.FutureItemDisplay.getValue()) {
                putAfter(entries, EnigmaticBlocks.STAR_FABRIC, FutureItems.DARK_HELMET);
                putAfter(entries, FutureItems.DARK_HELMET, FutureItems.DARK_CHESTPLATE);
                putAfter(entries, FutureItems.DARK_CHESTPLATE, FutureItems.DARK_LEGGINGS);
                putAfter(entries, FutureItems.DARK_LEGGINGS, FutureItems.DARK_BOOTS);
                putAfter(entries, FutureItems.DARK_BOOTS, FutureItems.LIVING_FLAME);
                putAfter(entries, FutureItems.LIVING_FLAME, FutureItems.WORMHOLE_POTION);
                putAfter(entries, FutureItems.WORMHOLE_POTION, FutureItems.GEM_OF_BINDING);
            }
        }
    }

    private void putAfter(MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> entries, ItemLike after, ItemLike item) {
        entries.putAfter(new ItemStack(after), new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
