<#import "template.ftl" as layout>
<@layout.emailLayout>
<h2>${kcSanitize(msg(subjectKey, daysRemaining, reason))?no_esc}</h2>

<#if messageKey == "customMessage">
    <p>${kcSanitize(customMessage)?no_esc}</p>
<#else>
    <p>${kcSanitize(msg("accountNotificationGreeting", user.firstName!user.username))?no_esc}</p>

    <p>${kcSanitize(msg(messageKey, daysRemaining, reason))?no_esc}</p>

    <#if daysRemaining gt 0>
        <p><strong>${kcSanitize(msg("accountNotificationTimeRemaining", daysRemaining))?no_esc}</strong></p>
    </#if>

    <p>${kcSanitize(msg("accountNotificationQuestions", realmName))?no_esc}</p>

    <p>${kcSanitize(msg("accountNotificationSignature"))?no_esc}<br>
       ${kcSanitize(msg("accountNotificationSignatureFrom", realmName))?no_esc}</p>
</#if>

</@layout.emailLayout>