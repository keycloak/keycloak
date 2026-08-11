<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<#import "password-commons.ftl" as passwordCommons>
<#import "qr-code.ftl" as qr>
<@layout.registrationLayout displayRequiredFields=false displayMessage=!messagesPerField.existsError('credentialOffer'); section>
<!-- template: oid4vc-credential-offer.ftl -->

    <#if section = "header">
        ${msg("credentialOfferTitle", credentialDisplayName)}
    <#elseif section = "form">
        <ol id="kc-cred-offer-settings" class="pf-v5-c-list pf-v5-u-mb-md">
            <li>
                <p>${msg("credentialOfferStep1", credentialDisplayName)}</p>
                <@qr.qrCode id="kc-credential-offer-qr-code" content=credentialOffer.qrCode alt="QR code to claim a credential with a wallet">
                    <span id="kc-credential-offer-uri"><a href="${credentialOffer.uri}" id="credential-offer-uri-link">${msg("credentialOfferUri")}</a></span>
                </@qr.qrCode>
            </li>
            <li>
                <p>${msg("credentialOfferStep2", credentialDisplayName)}</p>
            </li>
        </ol>

        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-cred-offer-settings-form" method="post" novalidate="novalidate">
            <@buttons.actionGroup horizontal=true>
                <@buttons.button id="continue-vc-offer" label="doContinue" />
                <#if !skipCancelButton??>
                    <@buttons.button id="cancel-vc-offer" name="cancel-aia" label="doCancel" type="secondary" value="true" />
                </#if>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
