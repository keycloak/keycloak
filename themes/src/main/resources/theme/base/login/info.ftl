<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${message.summary}
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message">
        <p class="instruction">${message.summary}</p>
        <#if requiredActions?has_content>
            <ol class="${properties.kcStepListClass!}" role="list">
                <#list requiredActions as reqActionItem>
                    <li>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}</li>
                </#list>
            </ol>
        </#if>
        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <p><@buttons.buttonLink id="backToApplication" href=pageRedirectUri label="backToApplication"/></p>
            <#elseif actionUri?has_content>
                <p><@buttons.buttonLink id="proceedWithAction" href=actionUri label="proceedWithAction"/></p>
            <#elseif (client.baseUrl)?has_content>
                <p><@buttons.buttonLink id="backToApplication" href=client.baseUrl label="backToApplication"/></p>
            </#if>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>
