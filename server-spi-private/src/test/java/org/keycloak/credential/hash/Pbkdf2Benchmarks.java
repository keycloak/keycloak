package org.keycloak.credential.hash;

import com.fasterxml.jackson.databind.deser.Deserializers;
import org.keycloak.common.util.SystemEnvProperties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

/**
 * Created by findyr-akaplan on 3/22/17.
 */
public class Pbkdf2Benchmarks {

    private SecureRandom random;
    private int sha1Iterations;
    private int sha256Iterations;
    private int sha512Iterations;
    private Pbkdf2PasswordHashProvider sha1Provider;
    private Pbkdf2Sha256PasswordHashProvider sha256Provider;
    private Pbkdf2Sha512PasswordHashProvider sha512Provider;

    private Pbkdf2Benchmarks(int sha1Iterations, int sha256Iterations, int sha512Iterations) {
        this.sha1Iterations = sha1Iterations;
        this.sha256Iterations = sha256Iterations;
        this.sha512Iterations = sha512Iterations;
        this.random = new SecureRandom();
        this.sha1Provider = new Pbkdf2PasswordHashProvider();
        this.sha256Provider = new Pbkdf2Sha256PasswordHashProvider();
        this.sha512Provider = new Pbkdf2Sha512PasswordHashProvider();
    }

    private void runBenchmark(int samples, File csv) {
        System.out.print("Benchmarking pbkdf2 hash algorithms...");
        Collection<BenchmarkResult> allResults = new ArrayList<>(samples);
        int progress = samples < 100 ? 1 : samples / 100;
        long start = System.currentTimeMillis();
        for (int i = 0; i < samples; i++) {
            allResults.add(benchmarkAlgorithms());
            if (i % progress == 0) {
                System.out.print(".");
            }
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Done.");
        System.out.println(String.format("Completed sampling in %d seconds.", duration / 1000));
        exportToCsv(allResults, csv);
    }

    private BenchmarkResult benchmarkAlgorithms() {
        byte[] salt = new byte[4];
        random.nextBytes(salt);
        byte[] randomPassword = new byte[12];
        random.nextBytes(randomPassword);
        String testPassword = Base64.getEncoder().encodeToString(randomPassword);
        BenchmarkResult result = new BenchmarkResult();
        result.sha1Duration = benchmarkProvider(salt, testPassword, sha1Iterations, sha1Provider);
        result.sha256Duration = benchmarkProvider(salt, testPassword, sha256Iterations, sha256Provider);
        result.sha512Duration = benchmarkProvider(salt, testPassword, sha512Iterations, sha512Provider);
        result.password = testPassword;
        result.salt = Base64.getEncoder().encodeToString(salt);
        return result;
    }

    private long benchmarkProvider(byte[] salt, String testPassword, int iterations, APbkdf2PasswordHashProvider provider) {
        long start = System.currentTimeMillis();
        provider.encode(testPassword, iterations, salt);
        return System.currentTimeMillis() - start;
    }

    private void exportToCsv(Collection<BenchmarkResult> results, File file) {
        System.out.print("Exporting to csv...");
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("\"Salt\",\"Password\",\"Sha1Duration\",\"Sha256Duration\",\"Sha512Duration\"\n");
            double sha1Sum = 0;
            double sha1SqSum = 0;
            double sha256Sum = 0;
            double sha256SqSum = 0;
            double sha512Sum = 0;
            double sha512SqSum = 0;
            for (BenchmarkResult result : results) {
                sha1Sum += result.sha1Duration;
                sha1SqSum += (result.sha1Duration * result.sha1Duration);
                sha256Sum += result.sha256Duration;
                sha256SqSum += (result.sha256Duration * result.sha256Duration);
                sha512Sum += result.sha512Duration;
                sha512SqSum += (result.sha512Duration * result.sha512Duration);
                writer.append(String.format("\"%1$s\",\"%2$s\",%3$d,%4$d,%5$d\n", result.salt, result.password,
                        result.sha1Duration, result.sha256Duration, result.sha512Duration));
            }
            writer.flush();
            System.out.println("Done.");
            int n = results.size();
            double sha1Avg = sha1Sum / n;
            double sha1sd = Math.sqrt(sha1SqSum / n - (sha1Avg * sha1Avg));
            double sha256Avg = sha256Sum / n;
            double sha256sd = Math.sqrt(sha256SqSum / n - (sha256Avg * sha256Avg));
            double sha512Avg = sha512Sum / n;
            double sha512sd = Math.sqrt(sha512SqSum / n - (sha512Avg * sha512Avg));
            // Multiply sd by 2 to get 95% confidence interval
            System.out.println(String.format("Average hash duration - sha1: %1$.2f +/-%2$.2f, sha256: %3$.2f +/-%4$.2f, sha512: %5$.2f +/-%6$.2f", sha1Avg,
                    sha1sd * 2, sha256Avg, sha256sd * 2, sha512Avg, sha512sd * 2));
        } catch (IOException e) {
            System.out.println("Failed to export to CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int sha1Iterations = Integer.parseInt(System.getProperty("sha1Iterations", "20000"));
        int sha256Iterations = Integer.parseInt(System.getProperty("sha256Iterations", "28000"));
        int sha512Iterations = Integer.parseInt(System.getProperty("sha512Iterations", "18000"));
        Pbkdf2Benchmarks test = new Pbkdf2Benchmarks(sha1Iterations, sha256Iterations, sha512Iterations);
        File exportFile = new File(System.getProperty("output", "benchmarks.csv"));
        int samples = Integer.parseInt(System.getProperty("samples", "500"));
        test.runBenchmark(samples, exportFile);
    }

    private static class BenchmarkResult {
        String salt;
        String password;
        long sha1Duration;
        long sha256Duration;
        long sha512Duration;
    }
}
