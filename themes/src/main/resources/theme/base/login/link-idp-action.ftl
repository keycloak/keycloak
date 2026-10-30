<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout displayMessage=false; section>
<!-- template: link-idp-action.ftl -->

    <#if section = "header">
        ${msg("linkIdpActionTitle", idpDisplayName)}
    <#elseif section = "form">
        <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
            <p id="kc-link-text">
                ${msg("linkIdpActionMessage", idpDisplayName)}
            </p>
            <@buttons.actionGroup horizontal=true>
                <@buttons.button name="continue" id="kc-continue" label="doContinue"/>
                <@buttons.button name="cancel-aia" id="kc-cancel" label="doCancel" type="secondary"/>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
