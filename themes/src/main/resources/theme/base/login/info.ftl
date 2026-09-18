<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeaderKey?? && messageHeaderUsername?? && messageHeaderSentinel??>
            <#assign _m0 = "__KC_SENTINEL0_" + messageHeaderSentinel + "__">
            <#assign _m1 = "__KC_SENTINEL1_" + messageHeaderSentinel + "__">
            ${kcSanitize(msg(messageHeaderKey, _m0, _m1))?replace(_m1, ((messageHeaderAlias!)?esc)?markup_string)?replace(_m0, ((messageHeaderUsername!)?esc)?markup_string)?no_esc}
        <#elseif messageHeader??>
            ${kcSanitize(msg(messageHeader))?no_esc}
        <#else>
            ${message.summary?esc}
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message">
        <p class="instruction"><#if messageBodyKey?? && messageBodySentinel??><#assign _m0 = "__KC_SENTINEL0_" + messageBodySentinel + "__"><#assign _m1 = "__KC_SENTINEL1_" + messageBodySentinel + "__"><#assign _param0 = messageBodyParam0!(messageBodyUsername!)><#assign _param1 = messageBodyParam1!(messageBodyAlias!)>${kcSanitize(msg(messageBodyKey, _m0, _m1))?replace(_m1, ((_param1!)?esc)?markup_string)?replace(_m0, ((_param0!)?esc)?markup_string)?no_esc}<#else>${message.summary?esc}</#if><#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></b></#list><#else></#if></p>
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
