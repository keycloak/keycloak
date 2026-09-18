<#import "template.ftl" as layout>
<@layout.emailLayout>
<#assign _m0 = "__KC_SENTINEL_" + identityProviderUsernameSentinel + "__">
${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderDisplayName, realmName, _m0, link, linkExpiration, linkExpirationFormatter(linkExpiration)))?replace(_m0, ((identityProviderContext.username!)?esc)?markup_string)?no_esc}
</@layout.emailLayout>
