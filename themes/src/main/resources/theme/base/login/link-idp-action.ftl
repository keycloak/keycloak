<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("linkIdpActionTitle", idpDisplayName)}
    <#elseif section = "form">
    <div id="kc-link-text">
        ${msg("linkIdpActionMessage", idpDisplayName)}
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <@buttons.button name="continue" id="kc-continue" label="doContinue" fullWidth=false class=["kcButtonLargeClass"] value=msg("doContinue") />
        <@buttons.button name="cancel-aia" id="kc-cancel" label="doCancel" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value=msg("doCancel") />
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
