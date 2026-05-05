<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayInfo=(realm.registrationAllowed && !registrationDisabled??); section>
<!-- template: webauthn-authenticate.ftl -->

    <#if section = "title">
     title
    <#elseif section = "header">
        ${msg("webauthn-login-title")}
    <#elseif section = "form">
        <div id="kc-form-webauthn" class="${properties.kcFormClass!}" >
            <form id="webauth" action="${url.loginAction}" method="post" hidden="hidden">
                <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                <input type="hidden" id="authenticatorData" name="authenticatorData"/>
                <input type="hidden" id="signature" name="signature"/>
                <input type="hidden" id="credentialId" name="credentialId"/>
                <input type="hidden" id="userHandle" name="userHandle"/>
                <input type="hidden" id="error" name="error"/>
            </form>

                <#if authenticators??>
                    <form id="authn_select" class="${properties.kcFormClass!}" hidden="hidden">
                        <#list authenticators.authenticators as authenticator>
                            <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
                        </#list>
                    </form>

                    <#if shouldDisplayAuthenticators?? && shouldDisplayAuthenticators>
                        <#if authenticators.authenticators?size gt 1>
                            <p class="${properties.kcSelectAuthListItemTitle!}">${msg("webauthn-available-authenticators")}</p>
                        </#if>

                        <ul class="${properties.kcSelectAuthListClass!}" role="list">
                            <#list authenticators.authenticators as authenticator>
                                <li class="${properties.kcSelectAuthListItemWrapperClass!}">
                                    <div id="kc-webauthn-authenticator-item-${authenticator?index}" class="${properties.kcSelectAuthListItemClass!}">
                                        <div class="${properties.kcSelectAuthListItemIconClass!}">
                                            <div class="${properties.kcWebAuthnDefaultIcon!}">
                                            <#if authenticator.iconLight?? || authenticator.iconDark??>
                                                <picture>
                                                    <#if authenticator.iconDark??>
                                                        <source srcset="${url.resourcesPath}/img/passkeys/${authenticator.iconDark}" media="(prefers-color-scheme: dark)">
                                                    </#if>
                                                    <img src="${url.resourcesPath}/img/passkeys/${authenticator.iconLight!authenticator.iconDark!''}" alt="" width="40" height="40">
                                                </picture>
                                            <#else>
                                                <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg"
                                                     viewBox="0 0 512 512">
                                                    <path d="M336 352a176 176 0 1 0-167.7-122.3L7 391a24 24 0 0 0-7 17v80a24 24 0 0 0 24 24h80a24 24 0 0 0 24-24v-40h40a24 24 0 0 0 24-24v-40h40a24 24 0 0 0 17-7l33.3-33.3c16.9 5.4 35 8.3 53.7 8.3zm40-256a40 40 0 1 1 0 80 40 40 0 1 1 0-80z"
                                                          fill="currentColor"/>
                                                </svg>
                                            </#if>
                                            </div>
                                        </div>
                                        <div class="${properties.kcSelectAuthListItemBodyClass!}">
                                            <div id="kc-webauthn-authenticator-label-${authenticator?index}"
                                                class="${properties.kcSelectAuthListItemHeadingClass!}">
                                                ${authenticator.label}
                                            </div>

                                            <#if authenticator.authenticatorProvider?? && authenticator.authenticatorProvider?has_content>
                                                <div id="kc-webauthn-authenticator-label-subtext-${authenticator?index}"
                                                    class="${properties.kcSelectAuthListItemSubtitleClass!}">
                                                    ${authenticator.authenticatorProvider}
                                                </div>
                                            </#if>

                                            <div class="${properties.kcSelectAuthListItemSubtitleClass!}">
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
                                </li>
                            </#list>
                        </ul>
                    </#if>
                </#if>

            <@buttons.actionGroup>
                <@buttons.button id="authenticateWebAuthnButton" label="webauthn-doAuthenticate" class=["kcButtonPrimaryClass","kcButtonBlockClass"] autofocus="autofocus"/>
            </@buttons.actionGroup>
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
                <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
