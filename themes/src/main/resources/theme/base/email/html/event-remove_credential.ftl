<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventRemoveCredentialBodyHtml", event.details.credential_type!"unknown", event.date, event.ipAddress))?no_esc}
</@layout.emailLayout>
