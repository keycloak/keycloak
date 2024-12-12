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
    <template id="passwordStrengthTemplate">
        <div class="${properties.kcProgress} kc-password-strength-bar" id="bar">
            <div class="${properties.kcProgressBar}" role="progressbar">
                <div class="${properties.kcProgressBarIndicator}" id="indicator" style="width:0%"></div>
            </div>
        </div>
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
            { name: "specialChars", policy: { value: ${passwordPolicies.specialChars!-1}, error: "${msg('invalidPasswordMinSpecialCharsMessage')}"} },
            { name: "passwordStrength", policy: { value: ${passwordPolicies.passwordStrength?then(0, -1) }, error: "passwordStrength"} }
        ].filter(p => p.policy.value !== -1);

        if (activePolicies.filter(p => p.name === "passwordStrength").length !== 0) {
            document.getElementById("input-error-container-${field}").appendChild(document.querySelector("#passwordStrengthTemplate").content.cloneNode(true));
        }

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
                if (typeof e !== "string") {
                    const passwordStrengthTemplate = document.querySelector("#passwordStrengthTemplate").content.cloneNode(true);
                    const bar = passwordStrengthTemplate.querySelector("#bar");
                    const percentage = (e.score + 1) * 20;
                    passwordStrengthTemplate.getElementById("indicator").style.width = percentage + "%";
                    bar.classList.remove("pf-m-danger");
                    bar.classList.remove("pf-m-success");
                    if (percentage < 40) {
                        bar.classList.add("pf-m-danger");
                    }
                    if (percentage >= 80) {
                        bar.classList.add("pf-m-success");
                    }
                    template.prepend(bar);
                    return;
                }
                const row = document.querySelector("#errorItemTemplate").content.cloneNode(true);
                const li = row.querySelector("li");
                li.textContent = e;
                errorList.appendChild(li);
            });
            errorContainer.replaceChildren(template);
        });
    </script>
</#macro>