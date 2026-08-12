<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderDisplayName, realmName, "__KC_USERNAME__", link, linkExpiration, linkExpirationFormatter(linkExpiration)))?replace("__KC_USERNAME__", ((identityProviderContext.username!)?esc)?markup_string)?no_esc}
</@layout.emailLayout>
