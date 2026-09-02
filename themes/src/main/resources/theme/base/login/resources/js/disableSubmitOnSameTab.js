const NEW_TAB_MODIFIERS = ["shiftKey", "ctrlKey", "metaKey"];

function hasNewTabModifiers(event) {
    return NEW_TAB_MODIFIERS.some((modifier) => event[modifier]);
}

function opensInNewTab(form) {
    return form.target && form.target !== "_self";
}

function isImplicitSubmitControl(element) {
    if (!element) {
        return false;
    }

    const tagName = element.tagName;
    if (tagName === "TEXTAREA" || tagName === "SELECT") {
        return true;
    }

    if (tagName !== "INPUT") {
        return false;
    }

    const type = (element.type || "text").toLowerCase();
    return type !== "button" && type !== "submit" && type !== "reset" && type !== "checkbox" && type !== "radio";
}

export function bindDisableSubmitOnSameTab(form) {
    const buttonName = form.dataset.kcDisableSubmit;
    if (!buttonName) {
        return;
    }

    let modifierNewTabSubmit = false;

    form.addEventListener("click", (event) => {
        const submitButton = event.target.closest('button[type="submit"], input[type="submit"]');
        if (submitButton && form.contains(submitButton) && hasNewTabModifiers(event)) {
            modifierNewTabSubmit = true;
            queueMicrotask(() => {
                modifierNewTabSubmit = false;
            });
        }
    }, true);

    form.addEventListener("keydown", (event) => {
        if (event.key !== "Enter" || !hasNewTabModifiers(event) || !isImplicitSubmitControl(event.target)) {
            return;
        }

        modifierNewTabSubmit = true;
        queueMicrotask(() => {
            modifierNewTabSubmit = false;
        });
    }, true);

    form.addEventListener("submit", (event) => {
        const newTabSubmit = modifierNewTabSubmit || opensInNewTab(form);
        modifierNewTabSubmit = false;

        if (newTabSubmit) {
            return;
        }

        const button = form.elements.namedItem(buttonName);
        if (button) {
            button.disabled = true;
        }
    });
}

document.querySelectorAll("form[data-kc-disable-submit]").forEach(bindDisableSubmitOnSameTab);
