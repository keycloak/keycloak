package org.keycloak.protocol.oidc.ida.mappers.extractor;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ExtractorConstants {
    static final int DATE_LENGTH = 10;

    static final String KEY_FILTER_VALUE = "value";
    static final String KEY_FILTER_VALUES = "values";
    static final String KEY_FILTER_MAX_AGE = "max_age";

    static final String KEY_ARRAY_MAX_AGE = "assurance_details";

    static final String KEY_VERIFICATION = "verification";

    static final String KEY_CLAIMS = "claims";

    static final List<String> arrayKeys = new ArrayList<>();
    static {
        arrayKeys.add("check_details");
        arrayKeys.add("attachments");
        arrayKeys.add("assurance_details");
        arrayKeys.add("evidence");
        arrayKeys.add("evidence_ref");
    }

    static final List<DateTimeFormatter> DATETIME_FORMATTERS = Arrays.asList(
            // Offset corresponds to ±hh,±hh:mm format
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            // Offset corresponds to ±hh,±hhmm format
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"),
            // Offset corresponds to ±hh,±hhmm format
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
    );

    static final String WARN_MESSAGE_CANNOT_PARSE_DATETIME = "Can't parse dateTime(%s).";
    static final String WARN_MESSAGE_REQUESTED_CLAIM_IS_NOT_IN_VERIFIED_CLAIMS = "Ignore if the requested claim(%s) is not in Verified Claims";
}