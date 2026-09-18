<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeaderKey?? && messageHeaderUsername??>
            ${kcSanitize(msg(messageHeaderKey, "{0}", "{1}"))?replace("{0}", ((messageHeaderUsername!)?esc)?markup_string)?replace("{1}", ((messageHeaderAlias!)?esc)?markup_string)?no_esc}
        <#elseif messageHeader??>
            ${kcSanitize(msg(messageHeader))?no_esc}
        <#else>
            ${message.summary?esc}
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message">
        <p class="instruction"><#if messageBodyKey??><#assign _param0 = messageBodyParam0!(messageBodyUsername!)><#assign _param1 = messageBodyParam1!(messageBodyAlias!)>${kcSanitize(msg(messageBodyKey, "{0}", "{1}"))?replace("{0}", ((_param0!)?esc)?markup_string)?replace("{1}", ((_param1!)?esc)?markup_string)?no_esc}<#else>${message.summary?esc}</#if><#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></b></#list><#else></#if></p>
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
