<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=(realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "title">
     title
    <#elseif section = "header">
        ${kcSanitize(msg("passkey-login-title"))?no_esc}
    <#elseif section = "form">
        <form id="webauth" action="${url.loginAction}" method="post">
            <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
            <input type="hidden" id="authenticatorData" name="authenticatorData"/>
            <input type="hidden" id="signature" name="signature"/>
            <input type="hidden" id="credentialId" name="credentialId"/>
            <input type="hidden" id="userHandle" name="userHandle"/>
            <input type="hidden" id="error" name="error"/>
        </form>

        <div class="${properties.kcFormGroupClass!} no-bottom-margin">
            <#if authenticators??>
                <form id="authn_select" class="${properties.kcFormClass!}">
                    <#list authenticators.authenticators as authenticator>
                        <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
                    </#list>
                </form>

                <#if shouldDisplayAuthenticators?? && shouldDisplayAuthenticators>
                    <#if authenticators.authenticators?size gt 1>
                        <p class="${properties.kcSelectAuthListItemTitle!}">${kcSanitize(msg("passkey-available-authenticators"))?no_esc}</p>
                    </#if>

                    <div class="${properties.kcFormClass!}">
                        <#list authenticators.authenticators as authenticator>
                            <div id="kc-webauthn-authenticator-item-${authenticator?index}" class="${properties.kcSelectAuthListItemClass!}">
                                <div class="${properties.kcSelectAuthListItemIconClass!}">
                                    <i class="${(properties['${authenticator.transports.iconClass}'])!'${properties.kcWebAuthnDefaultIcon!}'} ${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                                </div>
                                <div class="${properties.kcSelectAuthListItemBodyClass!}">
                                    <div id="kc-webauthn-authenticator-label-${authenticator?index}"
                                         class="${properties.kcSelectAuthListItemHeadingClass!}">
                                        ${kcSanitize(msg('${authenticator.label}'))?no_esc}
                                    </div>

                                    <#if authenticator.transports?? && authenticator.transports.displayNameProperties?has_content>
                                        <div id="kc-webauthn-authenticator-transport-${authenticator?index}"
                                            class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                            <#list authenticator.transports.displayNameProperties as nameProperty>
                                                <span>${kcSanitize(msg('${nameProperty!}'))?no_esc}</span>
                                                <#if nameProperty?has_next>
                                                    <span>, </span>
                                                </#if>
                                            </#list>
                                        </div>
                                    </#if>

                                    <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                        <span id="kc-webauthn-authenticator-createdlabel-${authenticator?index}">
                                            ${kcSanitize(msg('passkey-createdAt-label'))?no_esc}
                                        </span>
                                        <span id="kc-webauthn-authenticator-created-${authenticator?index}">
                                            ${kcSanitize(authenticator.createdAt)?no_esc}
                                        </span>
                                    </div>
                                </div>
                                <div class="${properties.kcSelectAuthListItemFillClass!}"></div>
                            </div>
                        </#list>
                    </div>
                </#if>
            </#if>

            <div id="kc-form">
                <div id="kc-form-wrapper">
                    <#if realm.password>
                        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" style="display:none">
                            <#if !usernameHidden??>
                                <div class="${properties.kcFormGroupClass!}">
                                    <label for="username" class="${properties.kcLabelClass!}">${msg("passkey-autofill-select")}</label>
                                    <input tabindex="1" id="username"
                                        aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                        class="${properties.kcInputClass!}" name="username"
                                        value="${(login.username!'')}"
                                        autocomplete="username webauthn"
                                        type="text" autofocus autocomplete="off"
                                        dir="ltr"/>
                                    <#if messagesPerField.existsError('username')>
                                        <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                            ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                        </span>
                                    </#if>
                                </div>
                            </#if>
                        </form>
                    </#if>
                    <div id="kc-form-passkey-button" class="${properties.kcFormButtonsClass!}" style="display:none">
                        <input id="authenticateWebAuthnButton" type="button" onclick="doAuthenticate([], "${rpId}", "${challenge}", ${isUserIdentified}, ${createTimeout}, "${userVerification}", "${msg("passkey-unsupported-browser-text")?no_esc}")" autofocus="autofocus"
                            value="${kcSanitize(msg("passkey-doAuthenticate"))}"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"/>
                    </div>
                    <div id="kc-form-passkey-button" class="${properties.kcFormButtonsClass!}" style="display:none">
                        <input id="authenticateWebAuthnButton" type="button" autofocus="autofocus"
                            value="${kcSanitize(msg("passkey-doAuthenticate"))}"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"/>
                    </div>
                </div>
            </div>
        </div>

        <script type="module">
            import { authenticateByWebAuthn } from "${url.resourcesPath}/js/webauthnAuthenticate.js";
            import { initAuthenticate } from "${url.resourcesPath}/js/passkeysConditionalAuth.js";

            const authButton = document.getElementById('authenticateWebAuthnButton');
            const input = {
                isUserIdentified : ${isUserIdentified},
                challenge : '${challenge}',
                userVerification : '${userVerification}',
                rpId : '${rpId}',
                createTimeout : ${createTimeout},
                errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}"
            };
            authButton.addEventListener("click", () => {
                authenticateByWebAuthn(input);
            });

            const args = {
                isUserIdentified : ${isUserIdentified},
                challenge : '${challenge}',
                userVerification : '${userVerification}',
                rpId : '${rpId}',
                createTimeout : ${createTimeout},
                errmsg : "${msg("passkey-unsupported-browser-text")?no_esc}"
            };

            document.addEventListener("DOMContentLoaded", (event) => initAuthenticate(args));
        </script>

    <#elseif section = "info">
        <#if realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
