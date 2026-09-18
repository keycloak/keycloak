package org.keycloak.crypto.hash;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.Block;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.BlockPool;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.FixedBlockPool;

public class BlockChunkManager {

    public static final int CHUNK_BLOCKS = 1024; // 1 MB chunk (1024 blocks of 1 KB)

    private static final int ARGON2_SYNC_POINTS = 4;
    // FillBlock allocates 4 scratch blocks (R, Z, addressBlock, inputBlock) in
    // addition to the primary memory blocks.
    private static final int SCRATCH_BLOCKS = 4;

    private final Deque<SoftReference<FixedBlockPool>> availableChunks = new ArrayDeque<>();

    public BlockChunkManager() {
    }

    static int computeMaxBlocks(int memoryInKB, int parallelism) {
        if (parallelism < 1 || parallelism > ((1 << 24) - 1)) {
            throw new IllegalArgumentException("parallelism must be between 1 and " + ((1 << 24) - 1));
        }
        // Mirror BouncyCastle's effective block count calculation:
        // memoryBlocks = max(memory, 2 * SYNC_POINTS * lanes), then rounded to a
        // multiple of 4 * lanes.
        int memoryBlocks = Math.max(memoryInKB, 2 * ARGON2_SYNC_POINTS * parallelism);
        int segmentLength = memoryBlocks / (ARGON2_SYNC_POINTS * parallelism);
        int laneLength = segmentLength * ARGON2_SYNC_POINTS;
        return parallelism * laneLength + SCRATCH_BLOCKS;
    }

    public LeasedBlockPool lease(int memoryKb, int parallelism) {
        int maxBlocks = computeMaxBlocks(memoryKb, parallelism);
        int chunksNeeded = maxBlocks / CHUNK_BLOCKS + 1;
        List<FixedBlockPool> acquiredChunks = new ArrayList<>(chunksNeeded);

        synchronized (availableChunks) {
            while (acquiredChunks.size() < chunksNeeded && !availableChunks.isEmpty()) {
                SoftReference<FixedBlockPool> ref = availableChunks.pop();
                FixedBlockPool chunk = ref.get();
                if (chunk != null) {
                    acquiredChunks.add(chunk);
                }
            }
        }

        while (acquiredChunks.size() < chunksNeeded) {
            acquiredChunks.add(new FixedBlockPool(CHUNK_BLOCKS));
        }

        return new LeasedBlockPool(acquiredChunks);
    }

    private void release(List<FixedBlockPool> chunks) {
        synchronized (availableChunks) {
            for (FixedBlockPool chunk : chunks) {
                availableChunks.push(new SoftReference<>(chunk));
            }
        }
    }

    public class LeasedBlockPool implements BlockPool, AutoCloseable {
        private final List<FixedBlockPool> chunks;
        private int allocatedCount;
        private int deallocatedCount;

        private LeasedBlockPool(List<FixedBlockPool> chunks) {
            this.chunks = chunks;
        }

        @Override
        public Block allocate() {
            Block result = chunks.get((allocatedCount / CHUNK_BLOCKS) % chunks.size()).allocate();
            allocatedCount++;
            return result;
        }

        @Override
        public void deallocate(Block block) {
            chunks.get((deallocatedCount / CHUNK_BLOCKS) % chunks.size()).deallocate(block);
            deallocatedCount++;
        }

        @Override
        public void close() {
            BlockChunkManager.this.release(chunks);
        }
    }
}
