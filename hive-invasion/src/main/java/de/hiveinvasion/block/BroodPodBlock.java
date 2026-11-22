package de.hiveinvasion.block;

import de.hiveinvasion.entity.Alien;
import de.hiveinvasion.entity.ClingerEntity;
import de.hiveinvasion.registry.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

/**
 * Brutkapsel. Geschlossen wartet sie; kommt ein Wirt näher als 4 Blöcke,
 * öffnet sie sich und entlässt einen Klammerer. Offene Kapseln wachsen
 * mit der Zeit wieder zu.
 */
public class BroodPodBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.of("open");
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 14, 14);
    private static final double TRIGGER_RANGE = 4.0;

    public BroodPodBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !state.get(OPEN)) world.scheduleBlockTick(pos, this, 20);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(OPEN)) return;
        if (!tryHatch(state, world, pos)) world.scheduleBlockTick(pos, this, 10);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(OPEN)) {
            // Langsam nachwachsen (Nester erneuern sich).
            if (random.nextInt(30) == 0) world.setBlockState(pos, state.with(OPEN, false));
        } else if (!world.getBlockTickScheduler().isQueued(pos, this)) {
            // Kapseln aus der Weltgenerierung "aufwecken".
            world.scheduleBlockTick(pos, this, 10);
        }
    }

    private boolean tryHatch(BlockState state, ServerWorld world, BlockPos pos) {
        Box area = new Box(pos).expand(TRIGGER_RANGE);
        List<LivingEntity> hosts = world.getEntitiesByClass(LivingEntity.class, area,
                e -> Alien.canHost(e) && (e instanceof PlayerEntity || e instanceof net.minecraft.entity.passive.MerchantEntity));
        if (hosts.isEmpty()) return false;

        world.setBlockState(pos, state.with(OPEN, true));
        ClingerEntity clinger = ModEntities.CLINGER.create(world);
        if (clinger != null) {
            clinger.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    world.random.nextFloat() * 360f, 0f);
            clinger.initialize(world, world.getLocalDifficulty(pos), SpawnReason.SPAWNER, null);
            clinger.setTarget(hosts.get(0));
            clinger.setVelocity(0, 0.45, 0);
            world.spawnEntity(clinger);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_SNIFFER_EGG_CRACK, SoundCategory.BLOCKS, 1.0f, 0.6f);
        world.spawnParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                12, 0.25, 0.1, 0.25, 0.05);
        return true;
    }
}
