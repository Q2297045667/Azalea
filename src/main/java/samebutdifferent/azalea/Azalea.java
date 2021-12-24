package samebutdifferent.azalea;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import samebutdifferent.azalea.block.FloweringAzaleaLogBlock;
import samebutdifferent.azalea.block.ModWoodTypes;
import samebutdifferent.azalea.block.grower.ModAzaleaTreeGrower;
import samebutdifferent.azalea.registry.ModBlockEntities;
import samebutdifferent.azalea.registry.ModBlocks;
import samebutdifferent.azalea.registry.ModConfig;
import samebutdifferent.azalea.registry.ModItems;
import samebutdifferent.azalea.worldgen.ModFeatures;

@Mod(Azalea.MOD_ID)
public class Azalea
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "azalea";

    public Azalea() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG);

        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AxeItem.STRIPPABLES = new ImmutableMap.Builder<Block, Block>().putAll(AxeItem.STRIPPABLES)
                    .put(ModBlocks.AZALEA_LOG.get(), ModBlocks.STRIPPED_AZALEA_LOG.get())
                    .put(ModBlocks.FLOWERING_AZALEA_LOG.get(), ModBlocks.STRIPPED_AZALEA_LOG.get())
                    .put(ModBlocks.FLOWERING_AZALEA_WOOD.get(), ModBlocks.STRIPPED_AZALEA_WOOD.get())
                    .put(ModBlocks.AZALEA_WOOD.get(), ModBlocks.STRIPPED_AZALEA_WOOD.get()).build();
            ModFeatures.registerAzaleaTree();
            WoodType.register(ModWoodTypes.AZALEA);
            WoodType.register(ModWoodTypes.FLOWERING_AZALEA);
        });
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_FLOWER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_DOOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLOWERING_AZALEA_DOOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_TRAPDOOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLOWERING_AZALEA_TRAPDOOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.POTTED_AZALEA_FLOWER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.MOSS.get(), RenderType.cutoutMipped());
        event.enqueueWork(() -> {
            Sheets.addWoodType(ModWoodTypes.AZALEA);
            Sheets.addWoodType(ModWoodTypes.FLOWERING_AZALEA);
        });
    }

    @SubscribeEvent
    public void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SIGN_BLOCK_ENTITY.get(), SignRenderer::new);
    }

    @SubscribeEvent
    public void onBiomeLoad(BiomeLoadingEvent event) {
        if (ModConfig.GENERATE_TREE_NATURALLY.get()) {
            if (event.getCategory() == Biome.BiomeCategory.FOREST || event.getCategory() == Biome.BiomeCategory.PLAINS) {
                event.getGeneration().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION).add(() -> ModFeatures.MOD_AZALEA_TREE.placed(
                        PlacementUtils.countExtra(0, 0.05F * ModConfig.TREE_SPAWN_CHANCE_MULTIPLIER.get().floatValue(), 1),
                        InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(0), PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                        BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(ModBlocks.AZALEA_FLOWER.get().defaultBlockState(), BlockPos.ZERO)),
                        BiomeFilter.biome()));
            }
        }
    }

    @SubscribeEvent
    public void onBoneMeal(BonemealEvent event) {
        if (ModConfig.OVERRIDE_VANILLA_TREE.get()) {
            if (!event.getWorld().isClientSide) {
                if (event.getBlock().getBlock() == Blocks.AZALEA || event.getBlock().getBlock() == Blocks.FLOWERING_AZALEA) {
                    event.setResult(Event.Result.ALLOW);
                    ServerLevel world = (ServerLevel) event.getWorld();
                    ModAzaleaTreeGrower tree = new ModAzaleaTreeGrower();
                    tree.growTree(world, world.getChunkSource().getGenerator(), event.getPos(), event.getBlock(), world.random);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isClientSide) {
            Level world = event.getWorld();
            Player player = event.getPlayer();
            BlockPos pos = event.getPos();
            BlockState state = world.getBlockState(pos);
            ItemStack stack = event.getItemStack();
            InteractionHand hand = event.getHand();
            Direction direction = event.getHitVec().getDirection().getAxis() == Direction.Axis.Y ? event.getHitVec().getDirection().getOpposite() : event.getHitVec().getDirection();
            if (stack.is(Items.SHEARS)) {
                if (state.is(Blocks.FLOWERING_AZALEA)) {
                    FloweringAzaleaLogBlock.shearAzalea(world, player, pos, stack, hand, direction, Blocks.AZALEA.defaultBlockState());
                    player.swing(hand, true);
                }
                if (state.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
                    FloweringAzaleaLogBlock.shearAzalea(world, player, pos, stack, hand, direction, Blocks.AZALEA_LEAVES.defaultBlockState());
                    player.swing(hand, true);
                }
            }
        }
    }
}
