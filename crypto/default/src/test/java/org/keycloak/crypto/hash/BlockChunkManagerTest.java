package org.keycloak.crypto.hash;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.junit.Test;

public class BlockChunkManagerTest {

    @Test
    public void hashWithVariousBlockCounts() {
        BlockChunkManager manager = new BlockChunkManager();

        // Exercises chunk boundary conditions: exact multiple, sub-chunk, non-aligned, high block count
        for (int[] config : new int[][]{{7168, 1}, {1, 1}, {8, 4}, {100, 3}, {256, 8}}) {
            try (BlockChunkManager.LeasedBlockPool pool = manager.lease()) {
                runArgon2(pool, config[0], config[1]);
            }
        }
    }

    @Test
    public void chunksReusedAcrossOperations() {
        BlockChunkManager manager = new BlockChunkManager();

        try (BlockChunkManager.LeasedBlockPool pool = manager.lease()) {
            runArgon2(pool, 7168, 1);
        }

        try (BlockChunkManager.LeasedBlockPool pool = manager.lease()) {
            runArgon2(pool, 7168, 1);
        }
    }

    @Test
    public void chunksSharedAcrossDifferentConfigurations() {
        BlockChunkManager manager = new BlockChunkManager();

        try (BlockChunkManager.LeasedBlockPool pool = manager.lease()) {
            runArgon2(pool, 7168, 1);
        }

        try (BlockChunkManager.LeasedBlockPool pool = manager.lease()) {
            runArgon2(pool, 1024, 1);
        }
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
