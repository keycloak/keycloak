package org.keycloak.crypto.hash;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
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

public class Argon2PasswordHashProviderFactory implements PasswordHashProviderFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "argon2";
    public static final String TYPE_KEY = "type";
    public static final String VERSION_KEY = "version";
    public static final String HASH_LENGTH_KEY = "hashLength";
    public static final String MEMORY_KEY = "memory";
    public static final String ITERATIONS_KEY = "iterations";
    public static final String PARALLELISM_KEY = "parallelism";
    public static final String CPU_CORES_KEY = "cpuCores";

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
        blockPoolManager = new SoftBlockPool();
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
     * {@link SimpleBlockPool} (no synchronization during hashing), uses it for all block
     * allocations/deallocations, then releases it back. Each individual pool is wrapped in a
     * {@link SoftReference} so the JVM can reclaim them independently under memory pressure
     * (~7 MB per pool with default settings). Without pooling, each hash allocates and discards
     * ~7 MB of {@code long[]} arrays, creating significant GC pressure under load.
     */
    static class SoftBlockPool {
        private final ConcurrentLinkedDeque<SoftReference<SimpleBlockPool>> pools = new ConcurrentLinkedDeque<>();

        Argon2BytesGenerator.BlockPool acquire() {
            SoftReference<SimpleBlockPool> ref;
            while ((ref = pools.pollLast()) != null) {
                SimpleBlockPool pool = ref.get();
                if (pool != null) {
                    return pool;
                }
            }
            return new SimpleBlockPool();
        }

        void release(Argon2BytesGenerator.BlockPool pool) {
            pools.offerLast(new SoftReference<>((SimpleBlockPool) pool));
        }
    }

    private static class SimpleBlockPool implements Argon2BytesGenerator.BlockPool {
        private final ArrayDeque<Argon2BytesGenerator.Block> blocks = new ArrayDeque<>();

        @Override
        public Argon2BytesGenerator.Block allocate() {
            Argon2BytesGenerator.Block block = blocks.pollLast();
            return block != null ? block : new Argon2BytesGenerator.Block();
        }

        @Override
        public void deallocate(Argon2BytesGenerator.Block block) {
            blocks.addLast(block);
        }
    }
}
