<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderDisplayName, realmName, "{0}", link, linkExpiration, linkExpirationFormatter(linkExpiration)))?replace("{0}", ((identityProviderContext.username!)?esc)?markup_string)?no_esc}
</@layout.emailLayout>
