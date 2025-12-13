package org.keycloak.services.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for RequestIdUtil.
 * Tests the utility methods for accessing RequestId from MDC and session.
 *
 * @author Keycloak Team
 */
public class RequestIdUtilTest {

    @Before
    public void setUp() {
        // Clean MDC before each test
        MDC.clear();
    }

    @After
    public void tearDown() {
        // Clean MDC after each test
        MDC.clear();
    }

    @Test
    public void testGetCurrentRequestId_FromMDC() {
        // Arrange
        String expectedRequestId = "test-request-id-123";
        MDC.put(RequestIdUtil.MDC_REQUEST_ID_KEY, expectedRequestId);

        // Act
        String actualRequestId = RequestIdUtil.getCurrentRequestId();

        // Assert
        assertThat(actualRequestId, is(expectedRequestId));
    }

    @Test
    public void testGetCurrentRequestId_ReturnsNull_WhenNotAvailable() {
        // Arrange - no value in MDC, no session available

        // Act
        String requestId = RequestIdUtil.getCurrentRequestId();

        // Assert
        assertThat(requestId, is(nullValue()));
    }

    @Test
    public void testHasCurrentRequestId_ReturnsTrueWhenAvailable() {
        // Arrange
        String requestId = "test-request-id-456";
        MDC.put(RequestIdUtil.MDC_REQUEST_ID_KEY, requestId);

        // Act
        boolean hasRequestId = RequestIdUtil.hasCurrentRequestId();

        // Assert
        assertThat(hasRequestId, is(true));
    }

    @Test
    public void testHasCurrentRequestId_ReturnsFalseWhenNull() {
        // Arrange - no value set

        // Act
        boolean hasRequestId = RequestIdUtil.hasCurrentRequestId();

        // Assert
        assertThat(hasRequestId, is(false));
    }

    @Test
    public void testHasCurrentRequestId_ReturnsFalseWhenEmptyString() {
        // Arrange
        MDC.put(RequestIdUtil.MDC_REQUEST_ID_KEY, "");

        // Act
        boolean hasRequestId = RequestIdUtil.hasCurrentRequestId();

        // Assert
        assertThat(hasRequestId, is(false));
    }

    @Test
    public void testHasCurrentRequestId_ReturnsFalseWhenWhitespaceString() {
        // Arrange
        MDC.put(RequestIdUtil.MDC_REQUEST_ID_KEY, "   ");

        // Act
        boolean hasRequestId = RequestIdUtil.hasCurrentRequestId();

        // Assert
        assertThat(hasRequestId, is(false));
    }

    @Test
    public void testMdcKeyConstant() {
        // Verify the MDC key constant matches expected format
        assertThat(RequestIdUtil.MDC_REQUEST_ID_KEY, is("kc.requestId"));
    }

    @Test
    public void testSessionAttributeKeyConstant() {
        // Verify the session attribute key constant
        assertThat(RequestIdUtil.SESSION_REQUEST_ID_KEY, is("requestId"));
    }

    @Test
    public void testRequestIdHeaderConstant() {
        // Verify the HTTP header constant
        assertThat(RequestIdUtil.REQUEST_ID_HEADER, is("X-Request-ID"));
    }

    @Test
    public void testConcurrentMdcAccess() {
        // Test that MDC access is thread-safe for this thread
        // (MDC is thread-local, so each thread has its own context)

        // Arrange
        String requestId1 = "concurrent-test-1";
        String requestId2 = "concurrent-test-2";

        // Act & Assert
        MDC.put(RequestIdUtil.MDC_REQUEST_ID_KEY, requestId1);
        assertThat(RequestIdUtil.getCurrentRequestId(), is(requestId1));

        MDC.put(RequestIdUtil.MDC_REQUEST_ID_KEY, requestId2);
        assertThat(RequestIdUtil.getCurrentRequestId(), is(requestId2));

        // Verify the value changed
        assertThat(RequestIdUtil.getCurrentRequestId(), is(not(requestId1)));
    }
}
