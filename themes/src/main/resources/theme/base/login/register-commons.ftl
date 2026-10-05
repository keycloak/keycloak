<#import "field.ftl" as field>
<#macro termsAcceptance>
    <#if termsAcceptanceRequired??>
        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcInputWrapperClass!}">
                ${msg("termsTitle")}
                <div id="kc-registration-terms-text">
                    ${kcSanitize(msg("termsText"))?no_esc}
                </div>
            </div>
        </div>
        <@field.checkbox name="termsAccepted" label=msg("acceptTerms") required=false />
        <#if messagesPerField.existsError('termsAccepted')>
            <div class="${properties.kcFormHelperTextClass!}" aria-live="polite">
                <span id="input-error-terms-accepted" class="${properties.kcInputErrorMessageClass!}">
                    ${messagesPerField.get('termsAccepted')}
                </span>
            </div>
        </#if>
    </#if>
</#macro>
