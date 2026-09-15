package org.keycloak.crypto.hash;

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator.FixedBlockPool;
import org.jboss.logging.Logger;

public class Argon2PasswordHashProviderFactory implements PasswordHashProviderFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "argon2";
    public static final String TYPE_KEY = "type";
    public static final String VERSION_KEY = "version";
    public static final String HASH_LENGTH_KEY = "hashLength";
    public static final String MEMORY_KEY = "memory";
    public static final String ITERATIONS_KEY = "iterations";
    public static final String PARALLELISM_KEY = "parallelism";
    public static final String CPU_CORES_KEY = "cpuCores";

    private static final Logger logger = Logger.getLogger(Argon2PasswordHashProviderFactory.class);

    /**
     * The Argon2 password hashing is CPU bound, so it doesn't make sense to hash more values concurrently than there are cores on the machine.
     * When we run more, this only leads to an increased memory usage and to throttling of the process in containerized environments
     * when a CPU limit is imposed. The throttling would have a negative impact on other concurrent non-hashing activities of Keycloak.
     */
    private Semaphore cpuCoreSemaphore;
    private SoftBlockPool blockPoolManager;

    private String version;
    private String type;
    private int hashLength;
    private int memory;
    private int iterations;
    private int parallelism;

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Argon2PasswordHashProvider(version, type, hashLength, memory, iterations, parallelism, cpuCoreSemaphore, blockPoolManager);
    }

    @Override
    public void init(Config.Scope config) {
        version = config.get(VERSION_KEY, Argon2Parameters.DEFAULT_VERSION);
        type = config.get(TYPE_KEY, Argon2Parameters.DEFAULT_TYPE);
        hashLength = config.getInt(HASH_LENGTH_KEY, Argon2Parameters.DEFAULT_HASH_LENGTH);
        memory = config.getInt(MEMORY_KEY, Argon2Parameters.DEFAULT_MEMORY);
        iterations = config.getInt(ITERATIONS_KEY, Argon2Parameters.DEFAULT_ITERATIONS);
        parallelism = config.getInt(PARALLELISM_KEY, Argon2Parameters.DEFAULT_PARALLELISM);
        cpuCoreSemaphore = new Semaphore(config.getInt(CPU_CORES_KEY, Runtime.getRuntime().availableProcessors()));
        blockPoolManager = new SoftBlockPool(memory);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        builder.property()
                .name(VERSION_KEY)
                .type("string")
                .helpText("Version")
                .options(new LinkedList<>(Argon2Parameters.listVersions()))
                .defaultValue(Argon2Parameters.DEFAULT_VERSION)
                .add();

        builder.property()
                .name(TYPE_KEY)
                .type("string")
                .helpText("Type")
                .options(new LinkedList<>(Argon2Parameters.listTypes()))
                .defaultValue(Argon2Parameters.DEFAULT_TYPE)
                .add();

        builder.property()
                .name(HASH_LENGTH_KEY)
                .type("int")
                .helpText("Hash length")
                .defaultValue(Argon2Parameters.DEFAULT_HASH_LENGTH)
                .add();

        builder.property()
                .name(MEMORY_KEY)
                .type("int")
                .helpText("Memory size (KB)")
                .defaultValue(Argon2Parameters.DEFAULT_MEMORY)
                .add();

        builder.property()
                .name(ITERATIONS_KEY)
                .type("int")
                .helpText("Iterations")
                .defaultValue(Argon2Parameters.DEFAULT_ITERATIONS)
                .add();

        builder.property()
                .name(PARALLELISM_KEY)
                .type("int")
                .helpText("Parallelism")
                .defaultValue(Argon2Parameters.DEFAULT_PARALLELISM)
                .add();

        builder.property()
                .name(CPU_CORES_KEY)
                .type("int")
                .helpText("Maximum parallel CPU cores to use for hashing")
                .add();

        return builder.build();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return !Profile.isFeatureEnabled(Profile.Feature.FIPS);
    }

    @Override
    public int order() {
        return 300;
    }

    /**
     * Pool-of-pools for Argon2 memory blocks. Each hash operation acquires an exclusive
     * {@link FixedBlockPool}, uses it for all block allocations/deallocations, then releases it
     * back. Each individual pool is wrapped in a {@link SoftReference} so the JVM can reclaim them
     * independently under memory pressure (~7 MB per pool with default settings). Without pooling,
     * each hash allocates and discards ~7 MB of {@code long[]} arrays, creating significant GC
     * pressure under load.
     *
     * <p>{@link FixedBlockPool} is used instead of a custom {@link Argon2BytesGenerator.BlockPool}
     * because {@link Argon2BytesGenerator.Block#clear()} is private — only {@code FixedBlockPool},
     * as an inner class of {@code Argon2BytesGenerator}, can zero out block contents on
     * deallocate/allocate to prevent password-derived data from lingering in pooled memory.
     *
     * <p>All block pools will be of the configured memory size, although existing hashed passwords might use larger pools.
     * In those cases, the necessary blocks will be created on-the-fly, and then immediately garbage collected.
     * This is a limitation of the current implementation, and could be changed once we are able to create a custom
     * replacement for FixedBlockPool.
     *
     * <p>As the concurrency of the Argon2 password hashing is limited to the number of CPU cores,
     * this pool is also effectively bounded with the number of CPU cores.
     */
    static class SoftBlockPool {
        // FillBlock allocates 4 scratch blocks (R, Z, addressBlock, inputBlock) in addition to the primary memory blocks.
        private static final int SCRATCH_BLOCKS = 4;
        private final ConcurrentLinkedDeque<SoftReference<FixedBlockPool>> pools = new ConcurrentLinkedDeque<>();
        private final int maxBlocks;

        SoftBlockPool(int memoryInKB) {
            this.maxBlocks = memoryInKB + SCRATCH_BLOCKS;
        }

        FixedBlockPool acquire() {
            SoftReference<FixedBlockPool> ref;
            while ((ref = pools.pollLast()) != null) {
                FixedBlockPool pool = ref.get();
                if (pool != null) {
                    return pool;
                } else {
                    // Soft references may be cleared by the JVM under memory pressure.
                    // The retention policy is implementation- and configuration-specific
                    // (for example, HotSpot can be influenced by -XX:SoftRefLRUPolicyMSPerMB).
                    logger.debug("Soft reference was evicted");
                }
            }
            return new FixedBlockPool(maxBlocks);
        }

        void release(FixedBlockPool pool) {
            pools.offerLast(new SoftReference<>(pool));
        }
    }
}
