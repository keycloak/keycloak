<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventUpdateTotpBodyHtml",event.date, event.ipAddress))?no_esc}
</@layout.emailLayout>
