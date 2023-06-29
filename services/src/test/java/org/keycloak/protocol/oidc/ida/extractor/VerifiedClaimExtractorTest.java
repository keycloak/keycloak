package org.keycloak.protocol.oidc.ida.extractor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.protocol.oidc.ida.mappers.extractor.VerifiedClaimExtractor;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public class VerifiedClaimExtractorTest {
    static VerifiedClaimExtractor extractor;

    private static final OffsetDateTime CURRENT_TIME =
            OffsetDateTime.of(2022, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    static Map<String, Object> userClaim;

    @Before
    public void setUpBeforeClass() throws IOException {
        extractor = new VerifiedClaimExtractor(CURRENT_TIME);
        InputStream stream = getClass().getResourceAsStream("/org/keycloak/test/oidc/userClaims.json");
        userClaim = (Map<String, Object>)JsonSerialization.readValue(stream, Map.class).get("verified_claims");
    }

    @Test
    public void testBasicClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertBasicClaims(filteredClaims);
    }

    @Test
    public void testEmptyMap() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":{}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertBasicClaims(filteredClaims);
    }

    @Test
    public void testValueMatchUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":{" +
                        "\"value\": \"uk_tfida\"}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertBasicClaims(filteredClaims);
    }

    @Test
    public void testValueMatchUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":{" +
                        "\"value\": \"Sarah\"}" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertBasicClaims(filteredClaims);
    }

    private void assertBasicClaims(Map<String, Object> filteredClaims) {
        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName.equals("Sarah"));

        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName == null);

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testValueNotMatchUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":{" +
                        "\"value\": \"Unknown\"}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertTrue(filteredClaims == null);
    }

    @Test
    public void testValueNotMatchUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":{" +
                        "\"value\":\"Unknown\"" +
                        "}," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName == null);

        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName.equals("Meredyth"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testValuesMatchUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":{" +
                        "\"values\":[\"Unknown0\",\"uk_tfida\"]" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertValuesMatch(filteredClaims);
    }

    @Test
    public void testValuesMatchUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":{" +
                        "\"values\":[\"Unknown0\",\"Sarah\"]" +
                        "}," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertValuesMatch(filteredClaims);
    }

    private void assertValuesMatch(Map<String, Object> filteredClaims) {
        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName.equals("Sarah"));

        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName.equals("Meredyth"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testValuesNotMatchUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":{" +
                        "\"values\":[\"Unknown0\",\"Unknown1\"]" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertTrue(filteredClaims == null);
    }

    @Test
    public void testValuesNotMatchUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":{" +
                        "\"values\":[\"Unknown0\",\"Unknown1\"]" +
                        "}," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName == null);

        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName.equals("Meredyth"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testMaxAgeSatisfiedUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"time\":{" +
                        "\"max_age\": 2000000000" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName.equals("Sarah"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));

        String time = (String) verification.get("time");
        Assert.assertTrue(time.equals("2021-05-11T14:29Z"));
    }

    @Test
    public void testMaxAgeSatisfiedUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"birthdate\":{" + // The actual value of birthdate is 1976-03-11".
                        "\"max_age\": 2000000000" +
                        "}," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String birthdate = (String) claims.get("birthdate");
        Assert.assertTrue(birthdate.equals("1976-03-11"));

        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName.equals("Meredyth"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testMaxAgeNotSatisfiedUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"time\":{" +
                        "\"max_age\": 100" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertTrue(filteredClaims == null);
    }

    @Test
    public void testMaxAgeNotSatisfiedUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"birthdate\":{" + // The actual value of birthdate is 1976-03-11".
                        "\"max_age\": 100" +
                        "}," +
                        "\"family_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String birthdate = (String) claims.get("birthdate");
        Assert.assertTrue(birthdate == null);

        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName.equals("Meredyth"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testDateTime() throws IOException {
        Map<String, Object> userClaim = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":\"my_framework\"" +
                        "}," +
                        "\"claims\":{" +
                        "\"datetime_0\":\"2022-04-22\"," +

                        "\"datetime_1\":\"2022-04-22T12:34:56+0900\"," +
                        "\"datetime_2\":\"2022-04-22T12:34:56+09:00\"," +
                        "\"datetime_3\":\"2022-04-22T12:34:56+09\"," +
                        "\"datetime_4\":\"2022-04-22T12:34:56Z\"," +

                        "\"datetime_5\":\"2022-04-22T12:34\"," +
                        "\"datetime_6\":\"2022-04-22T12:34+0900\"," +
                        "\"datetime_7\":\"2022-04-22T12:34+09:00\"," +
                        "\"datetime_8\":\"2022-04-22T12:34+09\"," +
                        "\"datetime_9\":\"2022-04-22T12:34Z\"" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"datetime_0\":{\"max_age\":2000000000}," +

                        "\"datetime_1\":{\"max_age\":2000000000}," +
                        "\"datetime_2\":{\"max_age\":2000000000}," +
                        "\"datetime_3\":{\"max_age\":2000000000}," +
                        "\"datetime_4\":{\"max_age\":2000000000}," +

                        "\"datetime_5\":{\"max_age\":2000000000}," +
                        "\"datetime_6\":{\"max_age\":2000000000}," +
                        "\"datetime_7\":{\"max_age\":2000000000}," +
                        "\"datetime_8\":{\"max_age\":2000000000}," +
                        "\"datetime_9\":{\"max_age\":2000000000}" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String datetime_0 = (String) claims.get("datetime_0");
        Assert.assertTrue(datetime_0.equals("2022-04-22"));

        String datetime_1 = (String) claims.get("datetime_1");
        Assert.assertTrue(datetime_1.equals("2022-04-22T12:34:56+0900"));

        String datetime_2 = (String) claims.get("datetime_2");
        Assert.assertTrue(datetime_2.equals("2022-04-22T12:34:56+09:00"));

        String datetime_3 = (String) claims.get("datetime_3");
        Assert.assertTrue(datetime_3.equals("2022-04-22T12:34:56+09"));

        String datetime_4 = (String) claims.get("datetime_4");
        Assert.assertTrue(datetime_4.equals("2022-04-22T12:34:56Z"));

        String datetime_5 = (String) claims.get("datetime_5");
        Assert.assertTrue(datetime_5.equals("2022-04-22T12:34"));

        String datetime_6 = (String) claims.get("datetime_6");
        Assert.assertTrue(datetime_6.equals("2022-04-22T12:34+0900"));

        String datetime_7 = (String) claims.get("datetime_7");
        Assert.assertTrue(datetime_7.equals("2022-04-22T12:34+09:00"));

        String datetime_8 = (String) claims.get("datetime_8");
        Assert.assertTrue(datetime_8.equals("2022-04-22T12:34+09"));

        String datetime_9 = (String) claims.get("datetime_9");
        Assert.assertTrue(datetime_9.equals("2022-04-22T12:34Z"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("my_framework"));
    }

    @Test
    public void testBasicClaimsNested() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"address\":{" +
                        "\"locality\":null" +
                        "}" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertBasicNestedClaim(filteredClaims);
    }

    private void assertBasicNestedClaim(Map<String, Object> filteredClaims) {
        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        Map<String, Object> address = (Map<String, Object>) claims.get("address");
        Assert.assertNotNull(address);

        String locality = (String) address.get("locality");
        Assert.assertTrue(locality.equals("Edinburgh"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testValueNestedUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"assurance_process\":{" +
                        "\"policy\":{" +
                        "\"value\":\"gpg45\"" +
                        "}" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertNotNull(givenName.equals("Sarah"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        Map<String, Object> assuranceProcess = (Map<String, Object>) verification.get("assurance_process");
        Assert.assertNotNull(assuranceProcess);

        String policy = (String) assuranceProcess.get("policy");
        Assert.assertTrue(policy.equals("gpg45"));
    }

    @Test
    public void testValueNotMatchNestedUnderVerification() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"assurance_process\":{" +
                        "\"policy\":{" +
                        "\"value\":\"Unknown0\"" +
                        "}" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);
        Assert.assertTrue(filteredClaims == null);
    }

    @Test
    public void testValueMatchNestedUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"address\":{" +
                        "\"locality\":{" +
                        "\"value\":\"Edinburgh\"" +
                        "}" +
                        "}" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        assertBasicNestedClaim(filteredClaims);
    }

    @Test
    public void testValueNotMatchNestedUnderClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"address\":{" +
                        "\"locality\":{" +
                        "\"value\":\"Unknown0\"" +
                        "}" +
                        "}" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        Map<String, Object> address = (Map<String, Object>) claims.get("address");
        Assert.assertTrue(address.isEmpty());


        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));
    }

    @Test
    public void testAssuranceDetails() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"assurance_process\":{" +
                        "\"assurance_details\":null" +
                        "}" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName.equals("Sarah"));

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));

        Map<String, Object> assuranceProcess = (Map<String, Object>) verification.get("assurance_process");
        Assert.assertNotNull(assuranceProcess);

        List assuranceDetails = (List) assuranceProcess.get("assurance_details");
        Assert.assertTrue(assuranceDetails.size() == 3);
    }

    @Test
    public void testArraySingleMatchSingle() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"evidence\":[" +
                        "{" +
                        "\"type\":{" +
                        "\"value\":\"electronic_record\"" +
                        "}," +
                        "\"check_details\":[" +
                        "{" +
                        "\"check_method\":null," +
                        "\"organization\":{" +
                        "\"value\":\"TheCreditBureau\"" +
                        "}," +
                        "\"txn\":null" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        List evidenceList = (List) verification.get("evidence");
        Assert.assertTrue(evidenceList.size() == 1);

        Map<String, Object> evidenceValue = (Map<String, Object>)evidenceList.get(0);

        String type = (String)evidenceValue.get("type");
        Assert.assertTrue(type.equals("electronic_record"));

        List checkDetails = (List) evidenceValue.get("check_details");
        Assert.assertTrue(checkDetails.size() == 1);

        Map<String, Object> checkDetail = (Map<String, Object>)checkDetails.get(0);

        String checkMethod = (String)checkDetail.get("check_method");
        Assert.assertTrue(checkMethod.equals("kbv"));

        String organization = (String)checkDetail.get("organization");
        Assert.assertTrue(organization.equals("TheCreditBureau"));

        String txn = (String)checkDetail.get("txn");
        Assert.assertTrue(txn.equals("kbv1-hf934hn09234ng03jj3"));
    }

    @Test
    public void testArraySingleNotMatch() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"evidence\":[" +
                        "{" +
                        "\"type\":{" +
                        "\"value\":\"unknown\"" +
                        "}," +
                        "\"check_details\":[" +
                        "{" +
                        "\"check_method\":null," +
                        "\"organization\":{" +
                        "\"value\":\"TheCreditBureau\"" +
                        "}," +
                        "\"txn\":null" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertTrue(filteredClaims == null);
    }

    @Test
    public void testArraySingleMatchMulti() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"evidence\":[" +
                        "{" +
                        "\"type\":{" +
                        "\"value\":\"electronic_record\"" +
                        "}," +
                        "\"check_details\":[" +
                        "{" +
                        "\"check_method\":null," +
                        "\"organization\":null," +
                        "\"txn\":null" +
                        "}" +
                        "]" +
                        "}" +
                        "]" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        List evidenceList = (List) verification.get("evidence");
        Assert.assertTrue(evidenceList.size() == 5);

        for(int i = 0; i < evidenceList.size() ; i++){
            Map<String, Object> evidence = (Map<String, Object>)evidenceList.get(i);
            String type = (String)evidence.get("type");
            Assert.assertTrue(type.equals("electronic_record"));

            List checkDetails = (List) evidence.get("check_details");
            Assert.assertNotNull(checkDetails);
        }
    }

    @Test
    public void testArrayMultipleMatch() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null," +
                        "\"evidence\":[" +
                        "{" +
                        "\"type\":{" +
                        "\"value\":\"electronic_record\"" +
                        "}," +
                        "\"check_details\":[" +
                        "{" +
                        "\"check_method\":null," +
                        "\"organization\":{" +
                        "\"value\":\"TheCreditBureau\"" +
                        "}," +
                        "\"txn\":null" +
                        "}" +
                        "]" +
                        "}," +
                        "{" +
                        "\"type\":{" +
                        "\"value\":\"document\"" +
                        "}" +
                        "}" +
                        "]" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Assert.assertNotNull(filteredClaims);

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        List evidenceList = (List) verification.get("evidence");
        Assert.assertTrue(evidenceList.size() == 2);

        for(int i = 0; i < evidenceList.size() ; i++){
            Map<String, Object> evidence = (Map<String, Object>)evidenceList.get(i);
            String type = (String)evidence.get("type");
            Assert.assertTrue(type.equals("electronic_record") || type.equals("document"));
        }
    }

    @Test
    public void testRequestInvalidClaims() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_Framework\":null" +
                        "}," +
                        "\"claims\":{" +
                        "\"given_name\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);
        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName.equals("Sarah"));
    }

    @Test
    public void testOnlyVerifications() throws IOException {
        Map<String, Object> request = JsonSerialization.readValue(
                "{" +
                        "\"verification\":{" +
                        "\"trust_framework\":null" +
                        "}" +
                        "}"
                , Map.class);

        Map<String, Object> filteredClaims = extractor.getFilteredClaims(request, userClaim);

        Map<String, Object> verification = (Map<String, Object>) filteredClaims.get("verification");
        Assert.assertNotNull(verification);

        String trustFramework = (String) verification.get("trust_framework");
        Assert.assertTrue(trustFramework.equals("uk_tfida"));

        Map<String, Object> claims = (Map<String, Object>) filteredClaims.get("claims");
        Assert.assertNotNull(claims);

        String givenName = (String) claims.get("given_name");
        Assert.assertTrue(givenName.equals("Sarah"));
        String familyName = (String) claims.get("family_name");
        Assert.assertTrue(familyName.equals("Meredyth"));
        String birthdate = (String) claims.get("birthdate");
        Assert.assertTrue(birthdate.equals("1976-03-11"));
        String country = ((Map<String, String>) claims.get("place_of_birth")).get("country");
        Assert.assertTrue(country.equals("UK"));
        Map<String, String> address = (Map<String, String>) claims.get("address");
        String locality = (String) address.get("locality");
        Assert.assertTrue(locality.equals("Edinburgh"));
        String postalCode = (String) address.get("postal_code");
        Assert.assertTrue(postalCode.equals("EH1 9GP"));
        String addressCountry = (String) address.get("country");
        Assert.assertTrue(addressCountry.equals("UK"));
        String streetAddress = (String) address.get("street_address");
        Assert.assertTrue(streetAddress.equals("122 Burns Crescent"));
    }
}