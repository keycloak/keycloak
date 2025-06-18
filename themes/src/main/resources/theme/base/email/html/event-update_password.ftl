<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventUpdatePasswordBodyHtml",event.date, event.ipAddress))?no_esc}
</@layout.emailLayout>
