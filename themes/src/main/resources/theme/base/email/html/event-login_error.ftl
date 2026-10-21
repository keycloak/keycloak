<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventLoginErrorBodyHtml",event.date,event.ipAddress))?no_esc}
</@layout.emailLayout>
