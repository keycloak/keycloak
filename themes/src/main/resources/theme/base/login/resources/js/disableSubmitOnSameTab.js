const NEW_TAB_MODIFIERS = ["shiftKey", "ctrlKey", "metaKey"];

function hasNewTabModifiers(event) {
    return NEW_TAB_MODIFIERS.some((modifier) => event[modifier]);
}

function opensInNewTab(form, event) {
    if (form.target && form.target !== "_self") {
        return true;
    }

    const submitter = event.submitter;
    return submitter != null && hasNewTabModifiers(submitter);
}

export function bindDisableSubmitOnSameTab(form) {
    const buttonName = form.dataset.kcDisableSubmit;
    if (!buttonName) {
        return;
    }

    let keyboardNewTabSubmit = false;

    form.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && hasNewTabModifiers(event)) {
            keyboardNewTabSubmit = true;
        }
    }, true);

    form.addEventListener("submit", (event) => {
        if (keyboardNewTabSubmit || opensInNewTab(form, event)) {
            keyboardNewTabSubmit = false;
            return;
        }

        keyboardNewTabSubmit = false;

        const button = event.submitter ?? form.elements.namedItem(buttonName);
        if (button) {
            button.disabled = true;
        }
    });
}

document.querySelectorAll("form[data-kc-disable-submit]").forEach(bindDisableSubmitOnSameTab);
