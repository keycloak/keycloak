<#import "template.ftl" as layout>
<@layout.emailLayout>
<#assign _m0 = "__KC_SENTINEL_" + identityProviderUsernameSentinel + "__">
${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderDisplayName, realmName, _m0, link, linkExpiration, linkExpirationFormatter(linkExpiration)), _m0, identityProviderContext.username!)?no_esc}
</@layout.emailLayout>
