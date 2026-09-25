<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("deleteCredentialTitle", credentialLabel)}
    <#elseif section = "form">
    <div id="kc-delete-text">
        ${msg("deleteCredentialMessage", credentialLabel)}
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <@buttons.button name="accept" id="kc-accept" label="doConfirmDelete" fullWidth=false class=["kcButtonLargeClass"] value=msg("doConfirmDelete") />
        <@buttons.button name="cancel-aia" id="kc-decline" label="doCancel" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value=msg("doCancel") />
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
