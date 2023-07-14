<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if msg("customTermsTitle") != "customTermsTitle">
            ${msg("customTermsTitle")}
        <#else>
            ${msg("termsTitle")}
        </#if>
    <#elseif section = "form">
    <div id="kc-terms-text">
        <#if msg("customTermsText") != "customTermsText">
            ${kcSanitize(msg("customTermsText"))?no_esc}
        <#else>
            ${kcSanitize(msg("termsText"))?no_esc}
        </#if>
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}"/>
        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
