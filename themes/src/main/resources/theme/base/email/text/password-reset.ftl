<#ftl output_format="plainText">
<#assign moreAccountsText><#if moreAccounts??>${msg("passwordResetMoreAccounts", user.username, moreAccounts)}</#if></#assign>
${msg("passwordResetBody",link, linkExpiration, realmName, linkExpirationFormatter(linkExpiration), moreAccountsText)}