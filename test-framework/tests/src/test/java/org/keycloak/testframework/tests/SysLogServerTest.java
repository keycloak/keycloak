package org.keycloak.testframework.tests;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSysLogServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.SysLog;
import org.keycloak.testframework.events.SysLogListener;
import org.keycloak.testframework.events.SysLogServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for {@link SysLogServer}.
 * <p>
 * Keycloak enables RFC 6587 counting framing by default when the syslog protocol is TCP
 * ({@code log-syslog-counting-framing=protocol-dependent}), which prefixes every message with its
 * octet count. {@link SysLog#parse(String)} expects a bare RFC 5424 message, so the extra leading
 * token shifts every header field by one and the timestamp parse throws. The result is that no
 * record ever reaches a listener.
 * <p>
 * {@code SysLogServerSupplier} therefore pins {@code log-syslog-counting-framing=false}. This test
 * fails if that is removed or if the default changes again.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/39893">#39893</a>
 * @see <a href="https://github.com/keycloak/keycloak/issues/40683">#40683</a>
 * @see <a href="https://github.com/keycloak/keycloak/issues/51550">#51550</a>
 */
@KeycloakIntegrationTest(config = SysLogServerTest.SysLogServerConfig.class)
public class SysLogServerTest {

  private static final long TIMEOUT_SECONDS = 30;

  @InjectSysLogServer
  SysLogServer sysLogServer;

  @InjectRealm
  ManagedRealm realm;

  @Test
  public void logMessagesAreReceivedAndParsed() throws InterruptedException {
    CollectingListener listener = new CollectingListener();
    sysLogServer.addListener(listener);
    try {
      triggerServerSideLogging();

      assertTrue(listener.awaitFirst(TIMEOUT_SECONDS, SECONDS),
          """
          No syslog record arrived within %d seconds. The server is most \
          likely emitting RFC 6587 counting framing (octet-count prefix), \
          which SysLog.parse cannot read.""".formatted(TIMEOUT_SECONDS));

      SysLog record = listener.all().get(0);

      // These are the fields that misalign when the octet count is present:
      // with framing on, timestamp parsing fails.
      assertNotNull(record.getTimestamp(), "Parsed record has no timestamp");
      assertNotNull(record.getAppName(), "Parsed record has no app-name");
      assertNotNull(record.getMessage(), "Parsed record has no message");
    } finally {
      sysLogServer.removeListener(listener);
    }
  }

  /**
   * Produces server-side log output.
   */
  private void triggerServerSideLogging() {
    UserRepresentation user = UserBuilder.create()
        .username("syslog-framing-user")
        .build();
    realm.admin().users().create(user).close();
  }

  /**
   * Intentionally empty. Its only purpose is to force a restart of the managed Keycloak instance so
   * that the syslog options from {@code SysLogServerSupplier} take effect - otherwise a server
   * started by an earlier test class is reused without a syslog handler. No options are set here on
   * purpose, since the test verifies that the defaults work.
   */
  public static class SysLogServerConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
      return config;
    }
  }

  private static final class CollectingListener implements SysLogListener {

    private final List<SysLog> records = new CopyOnWriteArrayList<>();
    private final CountDownLatch first = new CountDownLatch(1);

    @Override
    public void onLog(SysLog sysLog) {
      records.add(sysLog);
      first.countDown();
    }

    boolean awaitFirst(long timeout, TimeUnit unit) throws InterruptedException {
      return first.await(timeout, unit);
    }

    List<SysLog> all() {
      return List.copyOf(records);
    }
  }
}
