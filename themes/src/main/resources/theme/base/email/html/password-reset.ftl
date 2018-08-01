<#outputformat "plainText">
<#assign moreAccountsText><#if moreAccounts??>${msg("passwordResetMoreAccountsHtml", user.username, moreAccounts)}</#if></#assign>
</#outputformat>

<html>
<body>
${msg("passwordResetBodyHtml",link, linkExpiration, realmName, linkExpirationFormatter(linkExpiration), moreAccountsText)?no_esc}
</body>
</html>