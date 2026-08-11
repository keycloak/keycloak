<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("verifiableCredentialOfferBodyHtml", link, linkExpiration, realmName, credentialScopeDisplayName, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>