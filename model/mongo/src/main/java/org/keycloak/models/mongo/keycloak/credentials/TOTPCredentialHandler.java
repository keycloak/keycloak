package org.keycloak.models.mongo.keycloak.credentials;

import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.query.NoSQLQuery;
import org.keycloak.models.mongo.keycloak.data.UserData;
import org.keycloak.models.mongo.keycloak.data.credentials.OTPData;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.util.TimeBasedOTP;

import java.util.Date;
import java.util.Map;

import static org.picketlink.common.util.StringUtil.isNullOrEmpty;
import static org.picketlink.idm.credential.util.TimeBasedOTP.*;

/**
 * Defacto forked from {@link org.picketlink.idm.credential.handler.TOTPCredentialHandler}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TOTPCredentialHandler extends PasswordCredentialHandler {

    public static final String ALGORITHM = "ALGORITHM";
    public static final String INTERVAL_SECONDS = "INTERVAL_SECONDS";
    public static final String NUMBER_DIGITS = "NUMBER_DIGITS";
    public static final String DELAY_WINDOW = "DELAY_WINDOW";
    public static final String DEFAULT_DEVICE = "DEFAULT_DEVICE";

    private TimeBasedOTP totp;

    public TOTPCredentialHandler(Map<String, Object> options) {
        super(options);
        setup(options);
    }

    private void setup(Map<String, Object> options) {
        String algorithm = getConfigurationProperty(options, ALGORITHM, DEFAULT_ALGORITHM);
        String intervalSeconds = getConfigurationProperty(options, INTERVAL_SECONDS, "" + DEFAULT_INTERVAL_SECONDS);
        String numberDigits = getConfigurationProperty(options, NUMBER_DIGITS, "" + DEFAULT_NUMBER_DIGITS);
        String delayWindow = getConfigurationProperty(options, DELAY_WINDOW, "" + DEFAULT_DELAY_WINDOW);

        this.totp = new TimeBasedOTP(algorithm, Integer.parseInt(numberDigits), Integer.valueOf(intervalSeconds), Integer.valueOf(delayWindow));
    }

    public Credentials.Status validate(NoSQL noSQL, UserData user, String passwordToValidate, String token, String device) {
        Credentials.Status status = super.validate(noSQL, user, passwordToValidate);

        if (Credentials.Status.VALID != status) {
            return status;
        }

        device = getDevice(device);

        user = noSQL.loadObject(UserData.class, user.getId());

        // If the user for the provided username cannot be found we fail validation
        if (user != null) {
            if (user.isEnabled()) {

                // Try to find OTP based on userId and device (For now assume that this is unique combination)
                NoSQLQuery query = noSQL.createQueryBuilder()
                        .andCondition("userId", user.getId())
                        .andCondition("device", device)
                        .build();
                OTPData otpData = noSQL.loadSingleObject(OTPData.class, query);

                // If the stored OTP is null we automatically fail validation
                if (otpData != null) {
                    // TODO: Status.INVALID should have bigger priority than Status.EXPIRED?
                    if (!PasswordCredentialHandler.isCredentialExpired(otpData.getExpiryDate())) {
                        boolean isValid = this.totp.validate(token, otpData.getSecretKey().getBytes());
                        if (!isValid) {
                            status = Credentials.Status.INVALID;
                        }
                    }  else {
                        status = Credentials.Status.EXPIRED;
                    }
                } else {
                    status = Credentials.Status.UNVALIDATED;
                }
            } else {
                status = Credentials.Status.ACCOUNT_DISABLED;
            }
        } else {
            status = Credentials.Status.INVALID;
        }

        return status;
    }

    public void update(NoSQL noSQL, UserData user, String secret, String device, Date effectiveDate, Date expiryDate) {
        device = getDevice(device);

        // Try to look if user already has otp (Right now, supports just one OTP per user)
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("userId", user.getId())
                .andCondition("device", device)
                .build();

        OTPData otpData = noSQL.loadSingleObject(OTPData.class, query);
        if (otpData == null) {
            otpData = new OTPData();
        }

        otpData.setSecretKey(secret);
        otpData.setDevice(device);

        if (effectiveDate != null) {
            otpData.setEffectiveDate(effectiveDate);
        }

        otpData.setExpiryDate(expiryDate);
        otpData.setUserId(user.getId());

        noSQL.saveObject(otpData);
    }

    private String getDevice(String device) {
        if (isNullOrEmpty(device)) {
            device = DEFAULT_DEVICE;
        }

        return device;
    }

    private String getConfigurationProperty(Map<String, Object> options, String key, String defaultValue) {
        Object value = options.get(key);

        if (value != null) {
            return String.valueOf(value);
        }

        return defaultValue;
    }
}
