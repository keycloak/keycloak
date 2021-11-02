<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
    <div id="kc-terms-text">
        ${kcSanitize(msg("termsText"))?no_esc}
    </div>
    <form class="pf-c-form" action="${url.loginAction}" method="POST">
        <div class="pf-c-form__group pf-m-action">
            <div class="pf-c-form__actions">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}"/>
                <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
            </div>
        </div>
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
