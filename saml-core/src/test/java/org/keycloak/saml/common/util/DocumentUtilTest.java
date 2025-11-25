package org.keycloak.saml.common.util;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;

public class DocumentUtilTest {
  /**
   * Verifies that {@link DocumentUtil#getDocumentBuilder()} can be called from many threads
   * without triggering the race condition described in
   * <a href="https://github.com/keycloak/keycloak/issues/44438">issue #44438</a>.
   * <p>
   * The original implementation of {@code DocumentUtil#getDocumentBuilderFactory()} used a
   * non-thread-safe lazy initialization pattern which could throw a
   * {@link java.util.ConcurrentModificationException} when accessed concurrently.
   * This test starts {@code numThreads} threads that wait on a shared {@link CyclicBarrier}
   * and then all call {@link DocumentUtil#getDocumentBuilder()} at (roughly) the same time.
   * Any {@link ConcurrentModificationException} is captured in the {@code failure} reference
   * and will cause the final assertion to fail.
   * </p>
   *
   * <p>
   * The underlying bug is timing dependent, so this test is probabilistic: on the buggy
   * implementation it may still pass most of the time. To increase the likelihood of
   * reproducing the failure with the buggy code, you can temporarily inject a small,
   * randomized delay into {@code DocumentUtil#getDocumentBuilderFactory()} after the
   * {@code (documentBuilderFactory == null)} check, for example:
   * </p>
   *
   * <pre>{@code
   * try {
   *     Thread.sleep(new SecureRandom().nextInt(100));
   * } catch (InterruptedException e) {
   *     throw new RuntimeException(e);
   * }
   * }</pre>
   *
   * <p>
   * With this artificial delay in place, the buggy implementation is much more likely to
   * throw a {@link ConcurrentModificationException} in this test, demonstrating the race
   * condition. The fixed implementation should make this test pass reliably, even when
   * such a delay is present.
   * </p>
   */
  @Test
  public void testNoRaceConditionWhenCreatingDocumentBuilder() throws Throwable {
    // given
    AtomicReference<Throwable> failure = new AtomicReference<>();
    int numThreads = 100;


    // when
    List<Thread> threads = createThreads(numThreads, failure);
    joinThreads(numThreads, threads);

    // then
    if (failure.get() != null) {
        throw failure.get();
    }
  }

  private static void joinThreads(int numThreads, List<Thread> threads) {
    for (int i = 0; i < numThreads; i++) {
      try {
        threads.get(i).join();
      } catch (InterruptedException ignore) {
      }
    }
  }

  private static List<Thread> createThreads(int numThreads, AtomicReference<Throwable> failure) {
    CyclicBarrier barrier = new CyclicBarrier(numThreads);
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      Thread thread = new Thread(() -> {
        try {
          barrier.await();
          DocumentUtil.getDocumentBuilder();
        } catch (ParserConfigurationException | InterruptedException | BrokenBarrierException e) {
          throw new RuntimeException(e);
        } catch (ConcurrentModificationException e) {
          failure.set(e);
        }
      });
      threads.add(thread);
      thread.start();
    }
    return threads;
  }
}
