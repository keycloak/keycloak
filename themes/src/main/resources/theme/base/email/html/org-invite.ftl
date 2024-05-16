<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("orgInviteBodyHtml", link, linkExpiration, realmName, organization.name, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>
