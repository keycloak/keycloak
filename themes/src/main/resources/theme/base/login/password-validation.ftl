<#-- Include <@validator.templates/> once before enabling client-side password validation. -->
<#macro templates>
    <template id="errorTemplate">
        <div class="${properties.kcInputHelperTextClass!}">
            <div class="${properties.kcInputHelperTextItemClass!} ${properties.kcError!}">
                <#if properties.kcInputHelperTextItemIconClass?has_content>
                    <span class="${properties.kcInputHelperTextItemIconClass}">
                        <i class="${properties.kcInputErrorIconClass!}" aria-hidden="true"></i>
                    </span>
                </#if>
                <ul class="${properties.kcInputErrorMessageClass!}">
                </ul>
            </div>
        </div>
    </template>
    <template id="errorItemTemplate">
        <li></li>
    </template>
</#macro>

<#-- Use <@validator.script field="password"/> to attach the configured password policy to that input. -->
<#macro script field="">
    <script type="module">
        <#outputformat "JavaScript">
        import { validatePassword } from "${url.resourcesPath}/js/password-policy.js";

        const activePolicies = [
            { name: "length", policy: { value: ${passwordPolicies.length!-1}, error: ${msg('invalidPasswordMinLengthMessage')?c} } },
            { name: "maxLength", policy: { value: ${passwordPolicies.maxLength!-1}, error: ${msg('invalidPasswordMaxLengthMessage')?c} } },
            { name: "lowerCase", policy: { value: ${passwordPolicies.lowerCase!-1}, error: ${msg('invalidPasswordMinLowerCaseCharsMessage')?c} } },
            { name: "upperCase", policy: { value: ${passwordPolicies.upperCase!-1}, error: ${msg('invalidPasswordMinUpperCaseCharsMessage')?c} } },
            { name: "digits", policy: { value: ${passwordPolicies.digits!-1}, error: ${msg('invalidPasswordMinDigitsMessage')?c} } },
            { name: "specialChars", policy: { value: ${passwordPolicies.specialChars!-1}, error: ${msg('invalidPasswordMinSpecialCharsMessage')?c} } }
        ].filter(p => p.policy.value !== -1);

        document.getElementById("${field}").addEventListener("change", (event) => {

            const errorContainer = document.getElementById("input-error-container-${field}");
            const template = document.querySelector("#errorTemplate").content.cloneNode(true);
            const errors = validatePassword(event.target.value, activePolicies);

            if (errors.length === 0) {
                errorContainer.replaceChildren();
                errorContainer.hidden = true;
                event.target.removeAttribute("aria-invalid");
                event.target.removeAttribute("aria-describedby");
                return;
            }

            const errorId = "input-error-${field}";
            const errorList = template.querySelector("ul");
            errorList.id = errorId;
            const htmlErrors = errors.forEach((e) => {
                const row = document.querySelector("#errorItemTemplate").content.cloneNode(true);
                const li = row.querySelector("li");
                li.textContent = e;
                errorList.appendChild(li);
            });
            errorContainer.replaceChildren(template);
            errorContainer.hidden = false;
            event.target.setAttribute("aria-invalid", "true");
            event.target.setAttribute("aria-describedby", errorId);
        });
    </#outputformat>
    </script>
</#macro>
