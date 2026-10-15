<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "header">
        <#if client.attributes.logoUri??>
            <img src="${client.attributes.logoUri}"/>
        </#if>
        <p>
        <#if client.name?has_content>
            ${msg("oauthGrantTitle",advancedMsg(client.name))}
        <#else>
            ${msg("oauthGrantTitle",client.clientId)}
        </#if>
        </p>
    <#elseif section = "form">
        <div id="kc-oauth" class="content-area">
            <h3>${msg("oauthGrantRequest")}</h3>
            <ul>
                <#if oauth.clientScopesRequested??>
                    <#list oauth.clientScopesRequested as clientScope>
                        <li>
                            <span><#if !clientScope.parameterizedScopeParameter??>
                                        ${advancedMsg(clientScope.consentScreenText)}
                                    <#else>
                                        ${advancedMsg(clientScope.consentScreenText)}: <b>${clientScope.parameterizedScopeParameter}</b>
                                </#if>
                            </span>
                        </li>
                    </#list>
                </#if>
            </ul>
            <#if client.attributes.policyUri?? || client.attributes.tosUri??>
                <h3>
                    <#if client.name?has_content>
                        ${msg("oauthGrantInformation",advancedMsg(client.name))}
                    <#else>
                        ${msg("oauthGrantInformation",client.clientId)}
                    </#if>
                    <#if client.attributes.tosUri??>
                        ${msg("oauthGrantReview")}
                        <a href="${client.attributes.tosUri}" target="_blank">${msg("oauthGrantTos")}</a>
                    </#if>
                    <#if client.attributes.policyUri??>
                        ${msg("oauthGrantReview")}
                        <a href="${client.attributes.policyUri}" target="_blank">${msg("oauthGrantPolicy")}</a>
                    </#if>
                </h3>
            </#if>

            <form class="form-actions" action="${url.oauthAction}" method="POST">
                <input type="hidden" name="code" value="${oauth.code}">
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-options">
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                        </div>
                    </div>

                    <div id="kc-form-buttons">
                        <div class="${properties.kcFormButtonsWrapperClass!}">
                            <@buttons.button name="accept" id="kc-login" label="doYes" fullWidth=false class=["kcButtonLargeClass"] value=msg("doYes") />
                            <@buttons.button name="cancel" id="kc-cancel" label="doNo" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value=msg("doNo") />
                        </div>
                    </div>
                </div>
            </form>
            <div class="clearfix"></div>
        </div>
    </#if>
</@layout.registrationLayout>
