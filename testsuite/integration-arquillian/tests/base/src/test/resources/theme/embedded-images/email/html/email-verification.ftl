<html>
<body>
<img src="embed:logo.png">
<br>
${kcSanitize(msg("emailVerificationBodyHtml",link, linkExpiration, realmName, linkExpirationFormatter(linkExpiration)))?no_esc}
</body>
</html>