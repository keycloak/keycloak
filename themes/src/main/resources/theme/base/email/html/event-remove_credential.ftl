<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventRemoveCredentialBodyHtml", event.getDetail("credential_type")!"unknown", event.date, event.ipAddress))?no_esc}
</@layout.emailLayout>
