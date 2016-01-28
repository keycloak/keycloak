package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClaimMask {
    public static final long NAME = 0x01L;
    public static final long USERNAME = 0x02L;
    public static final long PROFILE = 0x04L;
    public static final long PICTURE = 0x08L;
    public static final long WEBSITE = 0x10L;
    public static final long EMAIL = 0x20L;
    public static final long GENDER = 0x40L;
    public static final long LOCALE = 0x80L;
    public static final long ADDRESS = 0x100L;
    public static final long PHONE = 0x200L;

    public static final long ALL = NAME | USERNAME | PROFILE | PICTURE | WEBSITE | EMAIL | GENDER | LOCALE | ADDRESS | PHONE;

    public static boolean hasName(long mask) {
        return (mask & NAME) > 0;
    }
    public static boolean hasUsername(long mask) {
        return (mask & USERNAME) > 0;
    }
    public static boolean hasProfile(long mask) {
        return (mask & PROFILE) > 0;
    }
    public static boolean hasPicture(long mask) {
        return (mask & PICTURE) > 0;
    }
    public static boolean hasWebsite(long mask) {
        return (mask & WEBSITE) > 0;
    }
    public static boolean hasEmail(long mask) {
        return (mask & EMAIL) > 0;
    }
    public static boolean hasGender(long mask) {
        return (mask & GENDER) > 0;
    }
    public static boolean hasLocale(long mask) {
        return (mask & LOCALE) > 0;
    }
    public static boolean hasAddress(long mask) {
        return (mask & ADDRESS) > 0;
    }
    public static boolean hasPhone(long mask) {
        return (mask & PHONE) > 0;
    }



}
