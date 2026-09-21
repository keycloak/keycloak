package org.keycloak.crypto.hash;

import java.util.ArrayDeque;
import java.util.Deque;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.Block;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.BlockPool;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BlockChunkManagerTest {

    /**
     * A {@link BlockPool} that pre-allocates exactly {@code capacity} blocks, throws
     * {@link AssertionError} if BouncyCastle requests more (under-allocation), and tracks
     * the high-water mark so tests can verify no blocks were wasted (over-allocation).
     */
    static class StrictBlockPool implements BlockPool {
        private final Deque<Block> available = new ArrayDeque<>();
        private final int capacity;
        private int outstanding;
        private int highWaterMark;

        StrictBlockPool(int capacity) {
            this.capacity = capacity;
            for (int i = 0; i < capacity; i++) {
                available.push(new Block());
            }
        }

        @Override
        public Block allocate() {
            Block block = available.poll();
            if (block == null) {
                throw new AssertionError(
                        "Pool exhausted: all " + capacity + " blocks are in use — computeMaxBlocks() is undersized");
            }
            outstanding++;
            highWaterMark = Math.max(highWaterMark, outstanding);
            return block;
        }

        @Override
        public void deallocate(Block block) {
            outstanding--;
            available.push(block);
        }

        int highWaterMark() {
            return highWaterMark;
        }
    }

    @Test
    public void defaultParameters() {
        assertPoolSizeExact(7168, 1);
    }

    @Test
    public void minimumMemory() {
        assertPoolSizeExact(1, 1);
    }

    @Test
    public void memoryBelowMinimumForParallelism() {
        assertPoolSizeExact(8, 4);
    }

    @Test
    public void nonAlignedMultiLane() {
        assertPoolSizeExact(100, 3);
    }

    @Test
    public void highParallelism() {
        assertPoolSizeExact(256, 8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parallelismZeroRejected() {
        BlockChunkManager.computeMaxBlocks(7168, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parallelismNegativeRejected() {
        BlockChunkManager.computeMaxBlocks(7168, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parallelismExceedsMaxRejected() {
        BlockChunkManager.computeMaxBlocks(7168, (1 << 24));
    }

    @Test
    public void computeMaxBlocksValues() {
        // default: 7168 blocks, 1 lane — no rounding needed
        assertEquals(7168 + 4, BlockChunkManager.computeMaxBlocks(7168, 1));

        // memory < 8*parallelism: floor kicks in at 2*4*parallelism = 32
        assertEquals(32 + 4, BlockChunkManager.computeMaxBlocks(8, 4));

        // non-aligned: 100/(4*3)=8 segments, laneLength=32, total=3*32=96
        assertEquals(96 + 4, BlockChunkManager.computeMaxBlocks(100, 3));
    }

    @Test
    public void chunkedPoolWorksWithBouncyCastle() {
        BlockChunkManager manager = new BlockChunkManager();

        try (BlockChunkManager.LeasedBlockPool pool = manager.lease(7168, 1)) {
            Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(new byte[16])
                    .withParallelism(1)
                    .withMemoryAsKB(7168)
                    .withIterations(1)
                    .withBlockPool(pool)
                    .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);

            byte[] result = new byte[32];
            generator.generateBytes("test".toCharArray(), result);
        }
    }

    @Test
    public void chunkedPoolReusesChunksAcrossOperations() {
        BlockChunkManager manager = new BlockChunkManager();

        // First hash
        try (BlockChunkManager.LeasedBlockPool pool = manager.lease(7168, 1)) {
            runArgon2(pool, 7168, 1);
        }

        // Second hash — should reuse chunks from the first
        try (BlockChunkManager.LeasedBlockPool pool = manager.lease(7168, 1)) {
            runArgon2(pool, 7168, 1);
        }
    }

    @Test
    public void chunkedPoolWorksAcrossDifferentConfigurations() {
        BlockChunkManager manager = new BlockChunkManager();

        try (BlockChunkManager.LeasedBlockPool pool = manager.lease(7168, 1)) {
            runArgon2(pool, 7168, 1);
        }

        // Different memory config — reuses chunks from the pool
        try (BlockChunkManager.LeasedBlockPool pool = manager.lease(1024, 1)) {
            runArgon2(pool, 1024, 1);
        }
    }

    private void assertPoolSizeExact(int memoryInKB, int parallelism) {
        int maxBlocks = BlockChunkManager.computeMaxBlocks(memoryInKB, parallelism);
        StrictBlockPool pool = new StrictBlockPool(maxBlocks);

        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(new byte[16])
                .withParallelism(parallelism)
                .withMemoryAsKB(memoryInKB)
                .withIterations(1)
                .withBlockPool(pool)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);

        byte[] result = new byte[32];
        generator.generateBytes("test".toCharArray(), result);

        assertEquals("computeMaxBlocks() over-allocates for m=" + memoryInKB + ", p=" + parallelism,
                maxBlocks, pool.highWaterMark());
    }

    private void runArgon2(BlockChunkManager.LeasedBlockPool pool, int memoryInKB, int parallelism) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(new byte[16])
                .withParallelism(parallelism)
                .withMemoryAsKB(memoryInKB)
                .withIterations(1)
                .withBlockPool(pool)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);

        byte[] result = new byte[32];
        generator.generateBytes("test".toCharArray(), result);
    }
}
