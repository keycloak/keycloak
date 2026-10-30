<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("saml.post-form.title")}
    <#elseif section = "form">
        <script>window.onload = function() {document.forms[0].submit()};</script>
        <p>${msg("saml.post-form.message")}</p>
        <form name="saml-post-binding" class="${properties.kcFormClass!}" method="post" action="${samlPost.url}">
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
                    <@buttons.actionGroup id="kc-form-buttons">
                        <@buttons.button label="doContinue" class=["kcButtonLargeClass"] value=msg("doContinue") />
                    </@buttons.actionGroup>
                </div>
            </noscript>
        </form>
    </#if>
</@layout.registrationLayout>
