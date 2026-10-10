<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout displayMessage=false; section>
<!-- template: delete-credential.ftl -->

    <#if section = "header">
        ${msg("deleteCredentialTitle", credentialLabel)}
    <#elseif section = "form">
        <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
            <p id="kc-delete-text">
                ${msg("deleteCredentialMessage", credentialLabel)}
            </p>
            <@buttons.actionGroup horizontal=true>
                <@buttons.button name="accept" id="kc-accept" label="doConfirmDelete"/>
                <@buttons.button name="cancel-aia" id="kc-decline" label="doDecline" type="secondary"/>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
