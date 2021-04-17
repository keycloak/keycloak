<html>
<body>
${kcSanitize(msg("emailUpdateConfirmationBodyHtml",link, newEmail, realmName, linkExpirationFormatter(linkExpiration)))?no_esc}
</body>
</html>
