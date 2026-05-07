<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "password-commons.ftl" as passwordCommons>
<@layout.registrationLayout displayRequiredFields=false displayMessage=!messagesPerField.existsError('credentialOffer'); section>
<!-- template: oid4vc-credential-offer.ftl -->

    <#if section = "header">
        ${msg("credentialOfferTitle", credentialDisplayName)}
    <#elseif section = "form">
        <ol id="kc-cred-offer-settings" class="pf-v5-c-list pf-v5-u-mb-md">
            <li>
                <p>${msg("credentialOfferStep1", credentialDisplayName)}</p>
                <img id="kc-credential-offer-qr-code" src="data:image/png;base64, ${credentialOffer.qrCode}" alt="Figure: Barcode"><br/>
                <p><span id="kc-credential-offer-uri"><a href="${credentialOffer.uri}" id="credential-offer-uri-link">${msg("credentialOfferUri")}</a></span></p>
            </li>
            <li>
                <p>${msg("credentialOfferStep2", credentialDisplayName)}</p>
            </li>
        </ol>

        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-cred-offer-settings-form" method="post" novalidate="novalidate">
            <div class="pf-v5-c-form__group pf-m-action">
                <div class="pf-v5-c-form__actions">
                    <#if isAppInitiatedAction??>
                        <input type="submit"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                            id="continue-vc-offer" value="${msg("doContinue")}"
                        />
                        <button type="submit"
                                class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} ${properties.kcButtonLargeClass!}"
                                id="cancel-vc-offer" name="cancel-aia" value="true">${msg("doCancel")}
                        </button>
                    <#else>
                        <input type="submit"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                            id="continue-vc-offer" value="${msg("doContinue")}"
                        />
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
