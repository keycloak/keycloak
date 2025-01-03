public static boolean isValidEmail(String value) {
    return isValidEmail(value, Config.scope("user-profile-declarative-user-profile")
                              .getInt(MAX_EMAIL_LOCAL_PART_LENGTH, MAX_LOCAL_PART_LENGTH));
}

public static boolean isValidEmail(String value, int maxEmailLocalPartLength) {
    if (value == null || value.trim().isEmpty()) {
        return false;
    }

    String trimmedValue = value.trim();
    int splitPosition = trimmedValue.lastIndexOf('@');

    if (splitPosition < 0) {
        return false;
    }

    String localPart = trimmedValue.substring(0, splitPosition);
    String domainPart = trimmedValue.substring(splitPosition + 1).trim();

    return isValidEmailLocalPart(localPart, maxEmailLocalPartLength) &&
           isValidEmailDomainAddress(domainPart);
}

private static boolean isValidEmailLocalPart(String localPart, int maxEmailLocalPartLength) {
    if (localPart.length() > maxEmailLocalPartLength) {
        return false;
    }
    Matcher matcher = LOCAL_PART_PATTERN.matcher(localPart);
    return matcher.matches();
}

private static boolean isValidEmailDomainAddress(String domain) {
    if (domain.endsWith(".")) {
        return false;
    }

    String asciiString;
    try {
        asciiString = IDN.toASCII(domain);
    } catch (IllegalArgumentException e) {
        return false;
    }

    if (asciiString.length() > MAX_DOMAIN_PART_LENGTH) {
        return false;
    }

    Matcher matcher = EMAIL_DOMAIN_PATTERN.matcher(domain);
    return matcher.matches();
}
