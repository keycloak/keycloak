<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("eventUserDisabledByTemporaryLockoutHtml", event.date))?no_esc}
</@layout.emailLayout>
