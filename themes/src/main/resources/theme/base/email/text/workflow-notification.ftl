${kcSanitize(msg(subjectKey, daysRemaining, reason))?no_esc}

Dear ${user.firstName!user.username},

<#if messageKey == "customMessage">
${kcSanitize(customMessage)?no_esc}
<#else>
${kcSanitize(msg(messageKey, daysRemaining, reason))?no_esc}
</#if>

<#if daysRemaining gt 0>
Time remaining: ${daysRemaining} day<#if daysRemaining != 1>s</#if>
</#if>

If you have questions, please contact your ${realmName} administrator.

Best regards,
${realmName} Administration