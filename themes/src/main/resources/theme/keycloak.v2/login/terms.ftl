<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout displayMessage=false; section>
<!-- template: terms.ftl -->

    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
    <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
        <div id="kc-terms-text">
            ${kcSanitize(msg("termsText"))?no_esc}
        </div>
        <@buttons.actionGroup horizontal=true>
            <@buttons.button name="accept" id="kc-accept" label="doAccept"/>
            <@buttons.button name="cancel" id="kc-decline" label="doDecline" type="secondary"/>
        </@buttons.actionGroup>
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
