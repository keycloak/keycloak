<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout displayMessage=false; section>
<!-- template: link-idp-action.ftl -->

    <#if section = "header">
        ${msg("linkIdpActionTitle", idpDisplayName)}
    <#elseif section = "form">
        <div id="kc-link-text" class="${properties.kcContentWrapperClass!}">
            ${msg("linkIdpActionMessage", idpDisplayName)}
        </div>

        <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
            <@buttons.actionGroup>
                <@buttons.button name="continue" id="kc-continue" label="doContinue" class=["kcButtonPrimaryClass"]/>
                <@buttons.button name="cancel-aia" id="kc-cancel" label="doCancel" class=["kcButtonSecondaryClass"]/>
            </@buttons.actionGroup>
        </form
    </#if>
</@layout.registrationLayout>
