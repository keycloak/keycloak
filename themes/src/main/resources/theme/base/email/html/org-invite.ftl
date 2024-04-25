<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("orgInviteBodyHtml", link, linkExpiration, realmName, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>
