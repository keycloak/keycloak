<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("passwordResetBodyHtml", link, linkExpiration, realmName, linkExpirationFormatter(linkExpiration), "insightsupport@emeritus.org"))?no_esc}
</@layout.emailLayout>
