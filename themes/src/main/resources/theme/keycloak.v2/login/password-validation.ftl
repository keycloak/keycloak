<#macro templates>
    <template id="errorTemplate">
        <div class="${properties.kcFormHelperTextClass}" aria-live="polite">
            <div class="${properties.kcInputHelperTextClass}">
                <div class="${properties.kcInputHelperTextItemClass} ${properties.kcError}">
                    <ul class="${properties.kcInputErrorMessageClass}">
                    </ul>
                </div>
            </div>
        </div>
    </template>
    <template id="errorItemTemplate">
        <li></li>
    </template>
</#macro>

<#macro script field="">
    <script type="module">
        import { validatePassword } from "${url.resourcesPath}/js/password-policy.js";

        const activePolicies = [
            { name: "length", policy: { value: ${passwordPolicies.length!-1}, error: "${msg('invalidPasswordMinLengthMessage')}"} },
            { name: "maxLength", policy: { value: ${passwordPolicies.maxLength!-1}, error: "${msg('invalidPasswordMaxLengthMessage')}"} },
            { name: "lowerCase", policy: { value: ${passwordPolicies.lowerCase!-1}, error: "${msg('invalidPasswordMinLowerCaseCharsMessage')}"} },
            { name: "upperCase", policy: { value: ${passwordPolicies.upperCase!-1}, error: "${msg('invalidPasswordMinUpperCaseCharsMessage')}"} },
            { name: "digits", policy: { value: ${passwordPolicies.digits!-1}, error: "${msg('invalidPasswordMinDigitsMessage')}"} },
            { name: "specialChars", policy: { value: ${passwordPolicies.specialChars!-1}, error: "${msg('invalidPasswordMinSpecialCharsMessage')}"} }
        ].filter(p => p.policy.value !== -1);

        document.getElementById("${field}").addEventListener("change", (event) => {

            const errorContainer = document.getElementById("input-error-container-${field}");
            const template = document.querySelector("#errorTemplate").content.cloneNode(true);
            const errors = validatePassword(event.target.value, activePolicies);

            if (errors.length === 0) {
                errorContainer.replaceChildren();
                return;
            }

            const errorList = template.querySelector("ul");
            const htmlErrors = errors.forEach((e) => {
                const row = document.querySelector("#errorItemTemplate").content.cloneNode(true);
                const li = row.querySelector("li");
                li.textContent = e;
                errorList.appendChild(li);
            });
            errorContainer.replaceChildren(template);
        });
    </script>
</#macro>