<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
    <div id="kc-terms-text">
        ${kcSanitize(msg("termsText"))?no_esc}
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <@buttons.button name="accept" id="kc-accept" label="doAccept" fullWidth=false class=["kcButtonLargeClass"] value=msg("doAccept") />
        <@buttons.button name="cancel" id="kc-decline" label="doDecline" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value=msg("doDecline") />
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
