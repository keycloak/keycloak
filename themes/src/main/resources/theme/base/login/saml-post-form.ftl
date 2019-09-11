<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${kcSanitize(msg("saml.post-form.title"))}
    <#elseif section = "form">
        <script>window.onload = function() {document.forms[0].submit()};</script>
        <p>${kcSanitize(msg("saml.post-form.message"))}</p>
        <form name="saml-post-binding" method="post" action="${samlPost.url}">
            <#if samlPost.SAMLRequest??>
                <input type="hidden" name="SAMLRequest" value="${samlPost.SAMLRequest}"/>
            </#if>
            <#if samlPost.SAMLResponse??>
                <input type="hidden" name="SAMLResponse" value="${samlPost.SAMLResponse}"/>
            </#if>
            <#if samlPost.relayState??>
                <input type="hidden" name="RelayState" value="${samlPost.relayState}"/>
            </#if>

            <noscript>
                <p>${kcSanitize(msg("saml.post-form.js-disabled"))}</p>
                <input type="submit" value="${kcSanitize(msg("doContinue"))}"/>
            </noscript>
        </form>
    </#if>
</@layout.registrationLayout>
