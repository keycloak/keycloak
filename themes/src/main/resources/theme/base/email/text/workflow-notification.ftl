${kcSanitize(msg(subjectKey, daysRemaining, reason))?no_esc}

<#if messageKey == "customMessage">
${kcSanitize(customMessage)?no_esc}
<#else>
Dear ${user.firstName!user.username},

${kcSanitize(msg(messageKey, daysRemaining, reason))?no_esc}

<#if daysRemaining gt 0>
    Time remaining: ${daysRemaining} day<#if daysRemaining != 1>s</#if>
</#if>

If you have questions, please contact your ${realmName} administrator.

Best regards,
${realmName} Administration
</#if>