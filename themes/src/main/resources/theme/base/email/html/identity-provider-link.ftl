<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderAlias, realmName, identityProviderContext.username, link, linkExpiration, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>
