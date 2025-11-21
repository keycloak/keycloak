<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayInfo=(realm.registrationAllowed && !registrationDisabled??); section>
<!-- template: webauthn-authenticate.ftl -->

    <#if section = "title">
     title
    <#elseif section = "header">
        ${kcSanitize(msg("webauthn-login-title"))?no_esc}
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
                            <p class="${properties.kcSelectAuthListItemTitle!}">${kcSanitize(msg("webauthn-available-authenticators"))?no_esc}</p>
                        </#if>

                        <ul class="${properties.kcSelectAuthListClass!}" role="list">
                            <#list authenticators.authenticators as authenticator>
                                <li class="${properties.kcSelectAuthListItemWrapperClass!}">
                                    <div id="kc-webauthn-authenticator-item-${authenticator?index}" class="${properties.kcSelectAuthListItemClass!}">
                                        <div class="${properties.kcSelectAuthListItemIconClass!}">
                                            <div class="${properties.kcWebAuthnDefaultIcon!}">
                                            <#switch authenticator.transports.iconClass>
                                                <#case "kcWebAuthnBLE">
                                                    <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg"
                                                         viewBox="0 0 217.499 217.499" xml:space="preserve"><path
                                                                d="m123.264 108.749 45.597-44.488a8.997 8.997 0 0 0 0-12.882l-50.038-48.82A9 9 0 0 0 103.538 9v80.504l-42.331-41.3a9 9 0 1 0-12.57 12.883l48.851 47.663-48.851 47.663a9 9 0 1 0 12.57 12.883l42.331-41.3V208.5a9 9 0 0 0 15.285 6.441l50.038-48.82a8.997 8.997 0 0 0 0-12.882l-45.597-44.49zm-1.725-78.395 28.15 27.465-28.15 27.465v-54.93zm0 156.789v-54.93l28.15 27.465-28.15 27.465z"
                                                                fill="currentColor"/></svg>
                                                    <#break>
                                                <#case "kcWebAuthnNFC">
                                                    <svg aria-hidden="true" viewBox="0 0 24 24"
                                                         xmlns="http://www.w3.org/2000/svg">
                                                        <path d="M12 19a1 1 0 1 0 0 2v-2Zm.01 2a1 1 0 1 0 0-2v2Zm2.68-3.96a1 1 0 0 0 1.347-1.48l-1.346 1.48Zm3.364-3.7a1 1 0 0 0 1.346-1.48l-1.346 1.48Zm-10.09 2.22a1 1 0 0 0 1.346 1.48l-1.346-1.48ZM4.6 11.86a1 1 0 1 0 1.345 1.48l-1.345-1.48ZM12 21h.01v-2H12v2Zm0-5c1.036 0 1.979.393 2.69 1.04l1.345-1.48A5.982 5.982 0 0 0 12 14v2Zm0-5c2.331 0 4.454.886 6.053 2.34l1.346-1.48A10.964 10.964 0 0 0 12 9v2ZM9.31 17.04A3.982 3.982 0 0 1 12 16v-2a5.982 5.982 0 0 0-4.036 1.56l1.346 1.48Zm-3.364-3.7A8.964 8.964 0 0 1 12 11V9a10.964 10.964 0 0 0-7.4 2.86l1.346 1.48Z"
                                                              fill="currentColor"/>
                                                    </svg>
                                                    <#break>
                                                <#case "kcWebAuthnUSB">
                                                    <svg aria-hidden="true" viewBox="0 0 24 24"
                                                         xmlns="http://www.w3.org/2000/svg">
                                                        <path d="M12 1.25a.75.75 0 0 1 .624.334l2 3a.75.75 0 1 1-1.248.832l-.626-.939v10.515c.121-.062.248-.115.38-.16l3.265-1.088c.51-.17.855-.647.855-1.185v-1.85a1.195 1.195 0 0 1-.634-.325 1.239 1.239 0 0 1-.341-.735 4.845 4.845 0 0 1-.025-.615v-.068c0-.206 0-.427.025-.615.03-.219.105-.5.341-.735.236-.236.516-.311.735-.341.188-.025.41-.025.615-.025h.069c.205 0 .426 0 .614.025.219.03.5.105.735.341.236.236.311.516.341.735.025.188.025.41.025.615v.068c0 .206 0 .427-.025.615-.03.219-.105.5-.341.735-.2.2-.434.285-.634.324v1.85a2.75 2.75 0 0 1-1.88 2.61l-3.265 1.088a1.25 1.25 0 0 0-.855 1.186v.703a2 2 0 1 1-1.5 0v-3.704a1.25 1.25 0 0 0-.855-1.185L7.13 12.167a2.75 2.75 0 0 1-1.88-2.609V7.582a1.75 1.75 0 1 1 1.5 0v1.976c0 .539.344 1.016.855 1.186l3.265 1.089c.132.044.259.097.38.159V4.477l-.626.939a.75.75 0 1 1-1.248-.832l2-3A.75.75 0 0 1 12 1.25Z"
                                                              fill="currentColor"/>
                                                    </svg>
                                                    <#break>
                                                <#default>
                                                    <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg"
                                                         viewBox="0 0 512 512">
                                                        <path d="M336 352a176 176 0 1 0-167.7-122.3L7 391a24 24 0 0 0-7 17v80a24 24 0 0 0 24 24h80a24 24 0 0 0 24-24v-40h40a24 24 0 0 0 24-24v-40h40a24 24 0 0 0 17-7l33.3-33.3c16.9 5.4 35 8.3 53.7 8.3zm40-256a40 40 0 1 1 0 80 40 40 0 1 1 0-80z"
                                                              fill="currentColor"/>
                                                    </svg>
                                                    <#break>
                                            </#switch>
                                            </div>
                                        </div>
                                        <div class="${properties.kcSelectAuthListItemBodyClass!}">
                                            <div id="kc-webauthn-authenticator-label-${authenticator?index}"
                                                class="${properties.kcSelectAuthListItemHeadingClass!}">
                                                ${kcSanitize(msg('${authenticator.label}'))?no_esc}
                                            </div>

                                            <#if authenticator.transports?? && authenticator.transports.displayNameProperties?has_content>
                                                <div id="kc-webauthn-authenticator-transport-${authenticator?index}">
                                                    <#list authenticator.transports.displayNameProperties as nameProperty>
                                                        <span>${kcSanitize(msg('${nameProperty!}'))?no_esc}</span>
                                                        <#if nameProperty?has_next>
                                                            <span>, </span>
                                                        </#if>
                                                    </#list>
                                                </div>
                                            </#if>

                                            <span id="kc-webauthn-authenticator-createdlabel-${authenticator?index}">
                                                <i>${kcSanitize(msg('webauthn-createdAt-label'))?no_esc}</i>
                                            </span>
                                            <span id="kc-webauthn-authenticator-created-${authenticator?index}">
                                                <i>${kcSanitize(authenticator.createdAt)?no_esc}</i>
                                            </span>
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
        import { authenticateByWebAuthn } from "${url.resourcesPath}/js/webauthnAuthenticate.js";
        const authButton = document.getElementById('authenticateWebAuthnButton');
        authButton.addEventListener("click", function() {
            const input = {
                isUserIdentified : ${isUserIdentified},
                challenge : '${challenge}',
                userVerification : '${userVerification}',
                rpId : '${rpId}',
                createTimeout : ${createTimeout?c},
                errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}"
            };
            authenticateByWebAuthn(input);
        }, { once: true });
    </script>

    <#elseif section = "info">
        <#if realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
