<#import "field.ftl" as field>
<#-- Use <@registerCommons.termsAcceptance/> inside registration forms to render required terms and errors. -->
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
        <@field.checkbox name="termsAccepted" label=msg("acceptTerms") required=true errorId="input-error-terms-accepted" />
    </#if>
</#macro>
