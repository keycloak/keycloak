<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("linkIdpActionTitle", idpDisplayName)}
    <#elseif section = "form">
    <div id="kc-link-text">
        ${msg("linkIdpActionMessage", idpDisplayName)}
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="continue" id="kc-continue" type="submit" value="${msg("doContinue")}"/>
        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel-aia" value="${msg("doCancel")}" id="kc-cancel" type="submit" />
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
