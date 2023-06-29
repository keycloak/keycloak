package org.keycloak.protocol.oidc.ida.mappers.extractor;

import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.ZoneOffset.UTC;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.DATETIME_FORMATTERS;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.DATE_LENGTH;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.KEY_ARRAY_MAX_AGE;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.KEY_CLAIMS;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.KEY_FILTER_MAX_AGE;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.KEY_FILTER_VALUE;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.KEY_FILTER_VALUES;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.KEY_VERIFICATION;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.WARN_MESSAGE_CANNOT_PARSE_DATETIME;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.WARN_MESSAGE_REQUESTED_CLAIM_IS_NOT_IN_VERIFIED_CLAIMS;
import static org.keycloak.protocol.oidc.ida.mappers.extractor.ExtractorConstants.arrayKeys;

/**
 * Class that implements the process of retrieving a request claim from a user's Verified Claims
 */
public class VerifiedClaimExtractor {
    private static final Logger logger = Logger.getLogger(VerifiedClaimExtractor.class);
    private final OffsetDateTime currentTime;
    private boolean isVerificationFlag = false;

    public VerifiedClaimExtractor(OffsetDateTime currentTime) {
        this.currentTime = currentTime;
    }

    /**
     * Retrieving a request claim from a user's Verified Claims
     *
     * @param requestClaims request Claims
     * @param userAllVerifiedClaims user's Verified Claims
     * @return request claim from a user's Verified Claims
     */
    public Map<String, Object> getFilteredClaims(Map<String, Object> requestClaims, Map<String, Object> userAllVerifiedClaims) {
        Map<String, Object> filteredClaims = new HashMap<>();
        boolean isAllOmit = extractClaims(requestClaims, userAllVerifiedClaims, filteredClaims);
        if (filteredClaims.get(KEY_VERIFICATION) != null && requestClaims.get(KEY_CLAIMS) == null) {
            // Keycloak proprietary specifications not written in the IDA specification
            // Returns all claims of the user if claims element is not specified for requestClaims and
            // one or more verification elements can be obtained
            Object userAllClaims = userAllVerifiedClaims.get(KEY_CLAIMS);
            if(userAllClaims != null) {
                filteredClaims.put(KEY_CLAIMS, userAllClaims);
            }
        }
        return (isAllOmit ? null : filteredClaims);
    }

    /**
     * Parse the same JSON depth data in requestClaims and userAllClaims to set the results to responseClaims
     * This method is called recursively for each JSON depth
     *
     * @param requestClaims
     * @param userAllClaims
     * @param filteredClaims
     * @return if "value", "Values", "max_age" are under "verified_claims/verification" and do not match, return true, else return false
     */
    private boolean extractClaims(Map<String, Object> requestClaims, Map<String, Object> userAllClaims, Map<String, Object> filteredClaims) {
        for (Map.Entry<String, Object> requestClaim : requestClaims.entrySet()) {
            String requestKey = requestClaim.getKey();
            Object requestValue = requestClaim.getValue();
            Object userClaimValue = userAllClaims.get(requestKey);
            if (userClaimValue == null) {
                // Ignore if the requested claim is not in Verified Claims
                logger.warnf(WARN_MESSAGE_REQUESTED_CLAIM_IS_NOT_IN_VERIFIED_CLAIMS, requestKey);
                continue;
            }

            boolean isVerificationFlagBackup = isVerificationFlag;
            if (requestKey.equals(KEY_VERIFICATION)) {
                // Returns null if "value", "Values", "max_age" are under "verified_claims/verification" and do not match
                // https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html#section-6.5.2-1
                isVerificationFlag = true;
            }

            if (requestValue == null || isEmptyMap(requestValue)) {
                // Returns userClaimValue if requestValue is a null or empty map (if there is no filter condition)
                // There is no description of the empty map({}) in the specification of the IDA.
                // Therefore, in Keycloak, it is the specification that the empty map is treated the same as null.
                if (userClaimValue != null) {
                    filteredClaims.put(requestKey, userClaimValue);
                }
            } else if (requestValue instanceof Map) {
                Map<String, Object> requestMap = (Map) requestValue;
                Object value = requestMap.get(KEY_FILTER_VALUE);
                List values = requestMap.get(KEY_FILTER_VALUES) instanceof List ? (List) requestMap.get(KEY_FILTER_VALUES) : null;
                Long maxAge = requestMap.get(KEY_FILTER_MAX_AGE) instanceof Number ? Long.valueOf(((Number) requestMap.get(KEY_FILTER_MAX_AGE)).longValue()) : null;
                if (value != null || values != null || maxAge != null) {
                    // If a filtering attribute is provided
                    // https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html#name-defining-further-constraint
                    if((value != null && value.equals(userClaimValue)) ||
                            (values != null && values.stream().anyMatch(v -> userClaimValue.equals(v))) ||
                            (maxAge != null && checkUserClaimValueWithMaxAge(maxAge, userClaimValue))){
                        // If you meet one of the filtering condition
                        filteredClaims.put(requestKey, userClaimValue);
                    } else {
                        // If none of the filtering conditions are met
                        if(isVerificationFlag) {
                            // Returns null if under verified_claims/verification
                            return true;
                        }
                    }
                } else {
                    // If the requestValue is not a filter, make sure that userAllClaims is also a map and the recursive call
                    if (userClaimValue instanceof Map) {
                        Map<String, Object> subClaim = new HashMap<>();
                        if (extractClaims(requestMap, (Map) userClaimValue, subClaim)) {
                            return true;
                        }
                        filteredClaims.put(requestKey, subClaim);
                    }
                }
            } else if (arrayKeys.contains(requestKey) && requestValue instanceof List) {
                // If requestKey is the key of the array and the requestValue is a list,
                // make sure that userClaimValue is also a list and all the combinations in the list are recursive calls
                if (userClaimValue instanceof List) {
                    if (extractAllList(requestKey, (List) requestValue, (List) userClaimValue, filteredClaims)) {
                        return true;
                    }
                }
            }
            isVerificationFlag = isVerificationFlagBackup;
        }
        return false;
    }

    private boolean extractAllList(String requestKey, List requestList, List userClaimList, Map<String, Object> filteredClaims) {
        if (requestKey.equals(KEY_ARRAY_MAX_AGE)) {
            // For assurance_details, all the sub elements of the property are returned.
            // https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html#section-6.2-12
            filteredClaims.put(requestKey, userClaimList);
        } else {
            List<Map<String, Object>> subClaimList = new ArrayList<>();
            for (Object userClaim : userClaimList) {
                if (!(userClaim instanceof Map)) {
                    // The verified claim list element is always a Map, so if it is not a Map, ignore it.
                    continue;
                }
                for (Object requestClaim : requestList) {
                    if (!(requestClaim instanceof Map)) {
                        // The request Claim list element is always a Map, so if it is not a Map, ignore it.
                        continue;
                    }
                    Map<String, Object> filteredSubClaims = new HashMap<>();
                    boolean isOmitList = extractClaims((Map<String, Object>) requestClaim, (Map<String, Object>) userClaim, filteredSubClaims);
                    if (!isOmitList && filteredSubClaims != null && !filteredSubClaims.isEmpty()) {
                        subClaimList.add(filteredSubClaims);
                        break;
                    }
                }
            }
            if (subClaimList.isEmpty()){
               return true;
            } else  {
                filteredClaims.put(requestKey, subClaimList);
            }
        }
        return false;
    }

    private boolean checkUserClaimValueWithMaxAge(Long maxAge, Object userClaimValue) {
        if (userClaimValue instanceof String) {
            String userClaimString = (String) userClaimValue;
            OffsetDateTime userClaimDateTime = parseDateTime(userClaimString);
            if (userClaimDateTime == null) {
                return false;
            }
            return currentTime.isBefore(userClaimDateTime.plusSeconds(maxAge));
        }
        return false;
    }

    /**
     * Parse YYYY-MM-DD、YYYY-MM-DDThh:mm[:ss]TZD in iso8601 format
     * https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html#section-5.1-12
     *
     * @param dateTime
     * @return OffsetDateTime. if can't parse dateTime, return null.
     */
    private OffsetDateTime parseDateTime(String dateTime) {
        if (dateTime.length() < DATE_LENGTH) {
            logger.warnf(WARN_MESSAGE_CANNOT_PARSE_DATETIME, dateTime);
            return null;
        }

        if (dateTime.length() == DATE_LENGTH) {
            try {
                // Parsing YYYY-MM-DD format
                return LocalDate.parse(dateTime).atTime(0, 0).atOffset(UTC);
            } catch (DateTimeParseException e) {
                logger.warnf(WARN_MESSAGE_CANNOT_PARSE_DATETIME, dateTime);
                return null;
            }
        }

        try {
            // Parsing YYYY-MM-DDThh:mm[:ss] format
            return LocalDateTime.parse(dateTime).atOffset(UTC);
        } catch (DateTimeParseException e) {
            // Offset may have been set, so check offset
        }

        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                // Parsing YYYY-MM-DDThh:mm[:ss]TZD format
                return OffsetDateTime.parse(dateTime, formatter);
            } catch (DateTimeParseException e) {
                logger.warnf(WARN_MESSAGE_CANNOT_PARSE_DATETIME, dateTime);
            }
        }
        return null;
    }

    private boolean isEmptyMap(Object requestValue) {
        return requestValue instanceof Map && ((Map) requestValue).isEmpty();
    }
}
