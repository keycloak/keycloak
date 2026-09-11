<#import "template.ftl" as layout>
<@layout.emailLayout>
<#assign _kcSentinel = "__KC_SENTINEL_" + identityProviderUsernameSentinel + "__">${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderDisplayName, realmName, _kcSentinel, link, linkExpiration, linkExpirationFormatter(linkExpiration)))?replace(_kcSentinel, ((identityProviderContext.username!)?esc)?markup_string)?no_esc}
</@layout.emailLayout>
