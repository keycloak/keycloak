<#import "template.ftl" as layout>
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

            <#-- Group scopes by required vs optional -->
            <#assign requiredScopes = []>
            <#assign optionalScopes = []>
            <#if oauth.clientScopesRequested??>
                <#list oauth.clientScopesRequested as clientScope>
                    <#if clientScope.required>
                        <#assign requiredScopes = requiredScopes + [clientScope]>
                    <#else>
                        <#assign optionalScopes = optionalScopes + [clientScope]>
                    </#if>
                </#list>
            </#if>

            <#-- Display scopes with categories only when user can deselect optional scopes -->
            <#if oauth.allowUserDeselectOptionalScopes>
                <#-- Display required scopes -->
                <#if requiredScopes?has_content>
                    <h4>${msg("consentRequiredScopes", "Required permissions")}</h4>
                    <ul>
                        <#list requiredScopes as clientScope>
                            <li>
                                <input type="checkbox" id="scope_${clientScope?index}_required" checked disabled />
                                <label for="scope_${clientScope?index}_required">
                                    <#if !clientScope.dynamicScopeParameter??>
                                        ${advancedMsg(clientScope.consentScreenText)}
                                    <#else>
                                        ${advancedMsg(clientScope.consentScreenText)}: <b>${clientScope.dynamicScopeParameter}</b>
                                    </#if>
                                </label>
                            </li>
                        </#list>
                    </ul>
                </#if>

                <#-- Display optional scopes -->
                <#if optionalScopes?has_content>
                    <h4>${msg("consentOptionalScopes", "Optional permissions")}</h4>
                    <ul>
                        <#list optionalScopes as clientScope>
                            <li>
                                <input type="checkbox" name="scope_${clientScope.id}" id="scope_${clientScope_index}_optional" value="${clientScope.id}" checked />
                                <label for="scope_${clientScope_index}_optional">
                                    <#if !clientScope.dynamicScopeParameter??>
                                        ${advancedMsg(clientScope.consentScreenText)}
                                    <#else>
                                        ${advancedMsg(clientScope.consentScreenText)}: <b>${clientScope.dynamicScopeParameter}</b>
                                    </#if>
                                </label>
                            </li>
                        </#list>
                    </ul>
                </#if>
            <#else>
                <#-- When feature is disabled, show simple list without "Required" label -->
                <ul>
                    <#if oauth.clientScopesRequested??>
                        <#list oauth.clientScopesRequested as clientScope>
                            <li>
                                <span><#if !clientScope.dynamicScopeParameter??>
                                            ${advancedMsg(clientScope.consentScreenText)}
                                        <#else>
                                            ${advancedMsg(clientScope.consentScreenText)}: <b>${clientScope.dynamicScopeParameter}</b>
                                    </#if>
                                </span>
                            </li>
                        </#list>
                    </#if>
                </ul>
            </#if>

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
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-login" type="submit" value="${msg("doYes")}"/>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doNo")}"/>
                        </div>
                    </div>
                </div>
            </form>
            <div class="clearfix"></div>
        </div>
    </#if>
</@layout.registrationLayout>
