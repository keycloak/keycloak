<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=(realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "title">
     title
    <#elseif section = "header">
        ${msg("webauthn-login-title")}
    <#elseif section = "form">
        <div id="kc-form-webauthn" class="${properties.kcFormClass!}">
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
                            <p class="${properties.kcSelectAuthListItemTitle!}">${msg("webauthn-available-authenticators")}</p>
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
                                            ${authenticator.label}
                                        </div>

                                        <#if authenticator.transports?? && authenticator.transports.displayNameProperties?has_content>
                                            <div id="kc-webauthn-authenticator-transport-${authenticator?index}"
                                                 class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                                <#list authenticator.transports.displayNameProperties as nameProperty>
                                                    <span>${msg(nameProperty)}</span>
                                                    <#if nameProperty?has_next>
                                                        <span>, </span>
                                                    </#if>
                                                </#list>
                                            </div>
                                        </#if>

                                        <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                            <span id="kc-webauthn-authenticator-createdlabel-${authenticator?index}">
                                                ${msg('webauthn-createdAt-label')}
                                            </span>
                                            <span id="kc-webauthn-authenticator-created-${authenticator?index}">
                                                ${authenticator.createdAt}
                                            </span>
                                        </div>
                                    </div>
                                    <div class="${properties.kcSelectAuthListItemFillClass!}"></div>
                                </div>
                            </#list>
                        </div>
                    </#if>
                </#if>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input id="authenticateWebAuthnButton" type="button" autofocus="autofocus"
                           value="${msg("webauthn-doAuthenticate")}"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"/>
                </div>
            </div>
        </div>

    <script type="module">
        <#outputformat "JavaScript">
        import { authenticateByWebAuthn } from "${url.resourcesPath}/js/webauthnAuthenticate.js";
        const authButton = document.getElementById('authenticateWebAuthnButton');
        authButton.addEventListener("click", function() {
            const input = {
                isUserIdentified : ${isUserIdentified},
                challenge : ${challenge?c},
                userVerification : ${userVerification?c},
                rpId : ${rpId?c},
                createTimeout : ${createTimeout?c},
                errmsg : ${msg("webauthn-unsupported-browser-text")?c}
            };
            authenticateByWebAuthn(input);
        }, { once: true });
        </#outputformat>
    </script>

    <#elseif section = "info">
        <#if realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
