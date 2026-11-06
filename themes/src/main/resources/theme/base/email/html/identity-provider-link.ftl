<#import "template.ftl" as layout>
<@layout.emailLayout>
    <div style="white-space: pre-line">${msg("identityProviderLinkBodyBeforeLink", identityProviderDisplayName, realmName, identityProviderContext.username!, "", linkExpiration, linkExpirationFormatter(linkExpiration))}<a href="${link}">${link}</a>${msg("identityProviderLinkBodyAfterLink", identityProviderDisplayName, realmName, identityProviderContext.username!, "", linkExpiration, linkExpirationFormatter(linkExpiration))}</div>
</@layout.emailLayout>
