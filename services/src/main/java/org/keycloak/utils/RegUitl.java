package org.keycloak.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则验证
 *
 * @author liubinwnag
 */
public class RegUitl {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789]\\d{9}$");

    /**
     * 验证是否为手机号码
     *
     * @param phone
     * @return
     */
    public static boolean isPhone(String phone) {
        if (phone != null) {
            Matcher m = MOBILE_PATTERN.matcher(phone);
            return m.matches();
        } else {
            return false;
        }
    }
}
