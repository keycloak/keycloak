<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("emailTestBodyHtml",realmName))?no_esc}
</@layout.emailLayout>
