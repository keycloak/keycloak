<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventUserDisabledByPermanentLockoutHtml", event.date))?no_esc}
</@layout.emailLayout>
