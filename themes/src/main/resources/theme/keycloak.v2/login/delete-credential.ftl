<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout displayMessage=false; section>
<!-- template: delete-credential.ftl -->

    <#if section = "header">
        ${msg("deleteCredentialTitle", credentialLabel)}
    <#elseif section = "form">
        <div id="kc-delete-text" class="${properties.kcContentWrapperClass!}">
            ${msg("deleteCredentialMessage", credentialLabel)}
        </div>

        <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
            <@buttons.actionGroup>
                <@buttons.button name="accept" id="kc-accept" label="doConfirmDelete" class=["kcButtonPrimaryClass"]/>
                <@buttons.button name="cancel-aia" id="kc-decline" label="doDecline" class=["kcButtonSecondaryClass"]/>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
