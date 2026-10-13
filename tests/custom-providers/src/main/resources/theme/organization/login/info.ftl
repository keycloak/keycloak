<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${message.summary}
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message">
        <#if org??>
            Sign-in to ${org.name} organization
            <#list org.attributes?keys as key>
                The ${key} is ${org.attributes[key]?join(", ")}
            </#list>
            <#if org.member>
                User is member of ${org.name}
            </#if>
        <#else>
            Sign-in to the realm
        </#if>
        <p class="instruction">${kcSanitize(message.summary)?no_esc}</p>
        <#if actionUri?has_content>
            <p><a href="${actionUri}">${msg("proceedWithAction")}</a></p>
        </#if>
    </div>
    </#if>
</@layout.registrationLayout>
