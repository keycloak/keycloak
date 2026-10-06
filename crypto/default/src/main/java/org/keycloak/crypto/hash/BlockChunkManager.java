package org.keycloak.crypto.hash;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.Block;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.BlockPool;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.FixedBlockPool;
import org.jboss.logging.Logger;

/**
 * Pool-of-chunks for Argon2 memory blocks. All chunks are uniform ~1 MB ({@value #CHUNK_BLOCKS}
 * blocks of 1 KB each) and shared across all Argon2 configurations, so different memory settings
 * (e.g. 7 MB, 12 MB, 19 MB) draw from and return to the same pool of reusable chunks. Chunks are
 * acquired lazily as BouncyCastle calls {@link BlockPool#allocate()}, so there is no need to
 * mirror BC's internal block count calculation. Without pooling, each hash allocates and discards
 * ~7 MB of {@code long[]} arrays (with default settings), creating significant GC pressure
 * under load.
 *
 * <p>Each chunk is a {@link FixedBlockPool} wrapped in a {@link SoftReference} so the JVM can
 * reclaim them independently under memory pressure. The pool uses LIFO ordering: chunks are
 * returned to and acquired from the head, so the same hot working set is reused while idle
 * chunks at the tail age out and are reclaimed, allowing the pool to shrink naturally when
 * load drops.
 *
 * <p>{@link FixedBlockPool} is used for individual chunks because
 * {@link Argon2BytesGenerator.Block#clear()} is private — only {@code FixedBlockPool},
 * as an inner class of {@code Argon2BytesGenerator}, can zero out block contents on
 * deallocate/allocate to prevent password-derived data from lingering in pooled memory.
 * See <a href="https://github.com/bcgit/bc-java/issues/2452">bcgit/bc-java#2452</a>.
 *
 * <p>As the concurrency of the Argon2 password hashing is limited to the number of CPU cores,
 * the total number of chunks in use is also effectively bounded by the number of CPU cores
 * times the chunks-per-operation count.
 */
public class BlockChunkManager {

    private static final Logger logger = Logger.getLogger(BlockChunkManager.class);

    // 1025 instead of 1024: all OWASP-recommended Argon2id memory sizes (7/9/12/19/46 MiB) are
    // multiples of 1024 blocks, and BC adds 4 scratch blocks. 1025 absorbs the scratch blocks
    // in the last chunk, avoiding an almost-empty extra chunk per hash operation.
    static final int CHUNK_BLOCKS = 1025;

    final ConcurrentLinkedDeque<SoftReference<FixedBlockPool>> availableChunks = new ConcurrentLinkedDeque<>();

    public LeasedBlockPool lease() {
        return new LeasedBlockPool();
    }

    private FixedBlockPool acquireChunk() {
        SoftReference<FixedBlockPool> ref;
        while ((ref = availableChunks.pollFirst()) != null) {
            FixedBlockPool chunk = ref.get();
            if (chunk != null) {
                return chunk;
            } else {
                logger.debug("Soft reference was evicted");
            }
        }
        return new FixedBlockPool(CHUNK_BLOCKS);
    }

    private void releaseChunks(List<FixedBlockPool> chunks) {
        // Reverse order so full chunks land at the head; the partial tail chunk ends up at the tail where it ages out.
        for (int i = chunks.size() - 1; i >= 0; i--) {
            availableChunks.offerFirst(new SoftReference<>(chunks.get(i)));
        }
    }

    public class LeasedBlockPool implements BlockPool, AutoCloseable {
        private final List<FixedBlockPool> chunks = new ArrayList<>();
        private int blockCount;

        @Override
        public Block allocate() {
            if (blockCount % CHUNK_BLOCKS == 0) {
                chunks.add(acquireChunk());
            }
            Block block = chunks.get(blockCount / CHUNK_BLOCKS).allocate();
            blockCount++;
            return block;
        }

        @Override
        public void deallocate(Block block) {
            blockCount--;
            chunks.get(blockCount / CHUNK_BLOCKS).deallocate(block);
        }

        @Override
        public void close() {
            releaseChunks(chunks);
        }
    }
}
