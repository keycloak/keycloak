<#import "template.ftl" as layout>
<@layout.emailLayout>
<h2>${kcSanitize(msg(subjectKey, daysRemaining, reason))?no_esc}</h2>

<p>Dear ${user.firstName!user.username},</p>

<#if messageKey == "customMessage">
<p>${kcSanitize(customMessage)?no_esc}</p>
<#else>
<p>${kcSanitize(msg(messageKey, daysRemaining, reason))?no_esc}</p>
</#if>

<#if daysRemaining gt 0>
<p><strong>Time remaining: ${daysRemaining} day<#if daysRemaining != 1>s</#if></strong></p>
</#if>

<p>If you have questions, please contact your ${realmName} administrator.</p>

<p>
Best regards,<br>
${realmName} Administration
</p>
</@layout.emailLayout>