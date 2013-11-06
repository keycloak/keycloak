package org.keycloak.models.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * TOTP: Time-based One-time Password Algorithm Based on http://tools.ietf.org/html/draft-mraihi-totp-timebased-06
 *
 * @author anil saldhana
 * @since Sep 20, 2010
 */
public class TimeBasedOTP {

    public static final String HMAC_SHA1 = "HmacSHA1";
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String HMAC_SHA512 = "HmacSHA512";

    public static final String DEFAULT_ALGORITHM = HMAC_SHA1;
    public static final int DEFAULT_NUMBER_DIGITS = 6;
    public static final int DEFAULT_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_DELAY_WINDOW = 1;

    // 0 1 2 3 4 5 6 7 8
    private static final int[] DIGITS_POWER = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    private Clock clock;
    private final String algorithm;
    private final int numberDigits;
    private final int delayWindow;

    public TimeBasedOTP() {
        this(DEFAULT_ALGORITHM, DEFAULT_NUMBER_DIGITS, DEFAULT_INTERVAL_SECONDS, DEFAULT_DELAY_WINDOW);
    }

    /**
     * @param algorithm the encryption algorithm
     * @param numberDigits the number of digits for tokens
     * @param timeIntervalInSeconds the number of seconds a token is valid
     * @param delayWindow the number of previous intervals that should be used to validate tokens.
     */
    public TimeBasedOTP(String algorithm, int numberDigits, int timeIntervalInSeconds, int delayWindow) {
        this.algorithm = algorithm;
        this.numberDigits = numberDigits;
        this.clock = new Clock(timeIntervalInSeconds);
        this.delayWindow = delayWindow;
    }

    /**
     * <p>Generates a token.</p>
     *
     * @param secretKey the secret key to derive the token from.
     */
    public String generate(String secretKey) {
        long T = this.clock.getCurrentInterval();

        String steps = Long.toHexString(T).toUpperCase();

        // Just get a 16 digit string
        while (steps.length() < 16)
            steps = "0" + steps;

        return generateTOTP(secretKey, steps, this.numberDigits, this.algorithm);
    }

    /**
     * This method generates an TOTP value for the given set of parameters.
     *
     * @param key          the shared secret, HEX encoded
     * @param time         a value that reflects a time
     * @param returnDigits number of digits to return
     * @param crypto       the crypto function to use
     * @return A numeric String in base 10 that includes {@link truncationDigits} digits
     * @throws java.security.GeneralSecurityException
     *
     */
    public String generateTOTP(String key, String time, int returnDigits, String crypto) {
        String result = null;
        byte[] hash;

        // Using the counter
        // First 8 bytes are for the movingFactor
        // Complaint with base RFC 4226 (HOTP)
        while (time.length() < 16)
            time = "0" + time;

        // Get the HEX in a Byte[]
        byte[] msg = hexStr2Bytes(time);

        // Adding one byte to get the right conversion
        // byte[] k = hexStr2Bytes(key);
        byte[] k = key.getBytes();

        hash = hmac_sha1(crypto, k, msg);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16) | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[returnDigits];

        result = Integer.toString(otp);

        while (result.length() < returnDigits) {
            result = "0" + result;
        }
        return result;
    }

    /**
     * <p>Validates a token using a secret key.</p>
     *
     * @param token  OTP string to validate
     * @param secret Shared secret
     * @return
     */
    public boolean validate(String token, byte[] secret) {
        long currentInterval = this.clock.getCurrentInterval();

        for (int i = this.delayWindow; i >= 0; --i) {
            String steps = Long.toHexString(currentInterval - i).toUpperCase();

            // Just get a 16 digit string
            while (steps.length() < 16)
                steps = "0" + steps;

            String candidate = generateTOTP(new String(secret), steps, this.numberDigits, this.algorithm);

            if (candidate.equals(token)) {
                return true;
            }
        }

        return false;
    }

    public void setCalendar(Calendar calendar) {
        this.clock.setCalendar(calendar);
    }

    /**
     * This method uses the JCE to provide the crypto algorithm. HMAC computes a Hashed Message Authentication Code with the
     * crypto hash algorithm as a parameter.
     *
     * @param crypto   the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes the bytes to use for the HMAC key
     * @param text     the message or text to be authenticated.
     * @throws java.security.NoSuchAlgorithmException
     *
     * @throws java.security.InvalidKeyException
     *
     */
    private byte[] hmac_sha1(String crypto, byte[] keyBytes, byte[] text) {
        byte[] value;

        try {
            Mac hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");

            hmac.init(macKey);

            value = hmac.doFinal(text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return value;
    }

    /**
     * This method converts HEX string to Byte[]
     *
     * @param hex the HEX string
     * @return A byte array
     */
    private byte[] hexStr2Bytes(String hex) {
        // Adding one byte to get the right conversion
        // values starting with "0" can be converted
        byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

        // Copy all the REAL bytes, not the "first"
        byte[] ret = new byte[bArray.length - 1];
        for (int i = 0; i < ret.length; i++)
            ret[i] = bArray[i + 1];
        return ret;
    }

    private class Clock {

        private final int interval;
        private Calendar calendar;

        public Clock(int interval) {
            this.interval = interval;
        }

        public long getCurrentInterval() {
            Calendar currentCalendar = this.calendar;

            if (currentCalendar == null) {
                currentCalendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            }

            return (currentCalendar.getTimeInMillis() / 1000) / this.interval;
        }

        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }
    }
}