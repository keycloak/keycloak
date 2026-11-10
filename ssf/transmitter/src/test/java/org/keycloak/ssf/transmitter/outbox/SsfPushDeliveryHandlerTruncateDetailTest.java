package org.keycloak.ssf.transmitter.outbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SsfPushDeliveryHandlerTruncateDetailTest {

    @Test
    void keepsShortDetail() {
        assertNull(SsfPushDeliveryHandler.truncateDetail(null));
        String detail = "a".repeat(SsfPushDeliveryHandler.LAST_ERROR_DETAIL_MAX);
        assertSame(detail, SsfPushDeliveryHandler.truncateDetail(detail));
    }

    @Test
    void truncatesLongDetail() {
        String detail = "a".repeat(SsfPushDeliveryHandler.LAST_ERROR_DETAIL_MAX + 1);
        assertEquals("a".repeat(SsfPushDeliveryHandler.LAST_ERROR_DETAIL_MAX) + "...",
                SsfPushDeliveryHandler.truncateDetail(detail));
    }

    @Test
    void doesNotSplitSurrogatePair() {
        // the emoji's high surrogate is the last char that fits
        String detail = "a".repeat(SsfPushDeliveryHandler.LAST_ERROR_DETAIL_MAX - 1) + "😀" + "b";
        String truncated = SsfPushDeliveryHandler.truncateDetail(detail);
        assertEquals("a".repeat(SsfPushDeliveryHandler.LAST_ERROR_DETAIL_MAX - 1) + "...", truncated);
        assertFalse(Character.isHighSurrogate(truncated.charAt(truncated.length() - 4)));
    }
}
