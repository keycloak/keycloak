<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventLoginErrorBodyHtml", "insightsupport@emeritus.org"))?no_esc}
</@layout.emailLayout>