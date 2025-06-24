<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventUpdatePasswordBodyHtml", "insightsupport@emeritus.org"))?no_esc}
</@layout.emailLayout>
