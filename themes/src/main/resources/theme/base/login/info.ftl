<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeaderKey?? && messageHeaderUsername?? && messageHeaderSentinel??>
            ${kcSanitize(msg(messageHeaderKey, "__KC_SENTINEL0_" + messageHeaderSentinel + "__", "__KC_SENTINEL1_" + messageHeaderSentinel + "__"))?replace("__KC_SENTINEL1_" + messageHeaderSentinel + "__", ((messageHeaderAlias!)?esc)?markup_string)?replace("__KC_SENTINEL0_" + messageHeaderSentinel + "__", ((messageHeaderUsername!)?esc)?markup_string)?no_esc}
        <#elseif messageHeader??>
            ${kcSanitize(msg(messageHeader))?no_esc}
        <#else>
            ${message.summary?esc}
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message">
        <p class="instruction">${message.summary?esc}<#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></b></#list><#else></#if></p>
        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <p><a href="${pageRedirectUri}">${msg("backToApplication")}</a></p>
            <#elseif actionUri?has_content>
                <p><a href="${actionUri}">${msg("proceedWithAction")}</a></p>
            <#elseif (client.baseUrl)?has_content>
                <p><a href="${client.baseUrl}">${msg("backToApplication")}</a></p>
            </#if>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>