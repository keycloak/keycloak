<#-- Render a login feedback alert with <@alert.alert message=message/>. -->
<#macro alert message>
    <div class="${properties.kcAlertClass!} ${properties['kcAlert' + message.type?cap_first + 'Class']!}">
        <div class="${properties.kcAlertIconClass!}">
            <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
            <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
            <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
            <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
        </div>
        <span class="${properties.kcAlertTitleClass!} kc-feedback-text">${message.summary}</span>
    </div>
</#macro>
