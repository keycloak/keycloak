<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("saml.post-form.title")}
    <#elseif section = "form">
        <script>window.onload = function() {document.forms[0].submit()};</script>
        <p>${msg("saml.post-form.message")}</p>
        <form class="${properties.kcFormClass!}" name="saml-post-binding" method="post" action="${samlPost.url}">
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
                <div class="${properties.kcFormClass!}">
                    <p>${msg("saml.post-form.js-disabled")}</p>
                    <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doContinue")}"/>
                    </div>
                </div>
            </noscript>
        </form>
    </#if>
</@layout.registrationLayout>
