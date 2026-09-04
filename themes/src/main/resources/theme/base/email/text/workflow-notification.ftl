${kcSanitize(msg(subjectKey, daysRemaining, reason))?no_esc}

<#if messageKey == "customMessage">
${kcSanitize(customMessage)?no_esc}
<#else>
${kcSanitize(msg("accountNotificationGreeting", user.firstName!user.username))?no_esc}

${kcSanitize(msg(messageKey, daysRemaining, reason))?no_esc}

<#if daysRemaining gt 0>
    ${kcSanitize(msg("accountNotificationTimeRemaining", daysRemaining))?no_esc}
</#if>

${kcSanitize(msg("accountNotificationQuestions", realmName))?no_esc}

${kcSanitize(msg("accountNotificationSignature"))?no_esc}
${kcSanitize(msg("accountNotificationSignatureFrom", realmName))?no_esc}
</#if>