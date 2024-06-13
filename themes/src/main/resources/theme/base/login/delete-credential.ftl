<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("deleteCredentialTitle", credentialLabel)}
    <#elseif section = "form">
    <div id="kc-delete-text">
        ${msg("deleteCredentialMessage", credentialLabel)}
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-accept" type="submit" value="${msg("doConfirmDelete")}"/>
        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel-aia" value="${msg("doCancel")}" id="kc-decline" type="submit" />
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
