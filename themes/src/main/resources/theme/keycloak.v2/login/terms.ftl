<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout displayMessage=false; section>
<!-- template: terms.ftl -->

    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
    <div class="${properties.kcContentWrapperClass}">
        ${kcSanitize(msg("termsText"))?no_esc}
    </div>
    <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
        <@buttons.actionGroup horizontal=true>
            <@buttons.button name="accept" id="kc-accept" label="doAccept" class=["kcButtonPrimaryClass"]/>
            <@buttons.button name="cancel" id="kc-decline" label="doDecline" class=["kcButtonSecondaryClass"]/>
        </@buttons.actionGroup>
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
