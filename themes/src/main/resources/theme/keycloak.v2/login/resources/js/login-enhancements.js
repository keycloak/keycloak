// Progressive enhancements for the Fidar login screens: Caps Lock detection,
// button ripple, and a submit loading state. Everything here is additive — if
// this module fails to load the forms still work exactly as before.

const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

/**
 * Warn when Caps Lock is active while a password field has focus. The label is
 * rendered server-side into a data attribute so it stays translated.
 */
function setupCapsLockWarning() {
  const label = document.body.dataset.capslockText;

  if (!label) {
    return;
  }

  const passwordInputs = document.querySelectorAll('input[type="password"]');

  for (const input of passwordInputs) {
    // Sit the notice directly under the field, above the helper row.
    const anchor = input.closest(".pf-v5-c-input-group") ?? input.parentElement;

    if (!anchor?.parentElement) {
      continue;
    }

    const notice = document.createElement("div");
    notice.className = "kc-capslock-warning";
    notice.setAttribute("role", "status");
    notice.setAttribute("aria-live", "polite");
    notice.hidden = true;
    notice.innerHTML =
      '<i class="fas fa-exclamation-triangle" aria-hidden="true"></i><span></span>';
    notice.querySelector("span").textContent = label;

    anchor.parentElement.insertBefore(notice, anchor.nextSibling);

    const sync = (event) => {
      // getModifierState is unavailable on some synthetic events.
      if (typeof event.getModifierState !== "function") {
        return;
      }

      // Pressing Caps Lock itself reports the *pre-toggle* state during keydown
      // on several platforms, which leaves the notice stuck on after switching
      // it back off. Skip that event and let the matching keyup settle it.
      if (event.type === "keydown" && event.key === "CapsLock") {
        return;
      }

      notice.hidden = !event.getModifierState("CapsLock");
    };

    input.addEventListener("keydown", sync);
    input.addEventListener("keyup", sync);
    // Pointer events also carry modifier state, so clicking back into the field
    // re-syncs a notice that went stale while the input was unfocused.
    input.addEventListener("mousedown", sync);
    input.addEventListener("blur", () => {
      notice.hidden = true;
    });
  }
}

/** Material-style ripple originating from the pointer position. */
function setupRipple() {
  if (prefersReducedMotion.matches) {
    return;
  }

  const buttons = document.querySelectorAll(
    ".pf-v5-c-login__main .pf-v5-c-button.pf-m-primary",
  );

  for (const button of buttons) {
    button.addEventListener("click", (event) => {
      const rect = button.getBoundingClientRect();
      const size = Math.max(rect.width, rect.height);
      const ripple = document.createElement("span");

      ripple.className = "kc-ripple";
      ripple.style.width = `${size}px`;
      ripple.style.height = `${size}px`;
      ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
      ripple.style.top = `${event.clientY - rect.top - size / 2}px`;

      ripple.addEventListener("animationend", () => ripple.remove());
      button.appendChild(ripple);
    });
  }
}

/**
 * Show a spinner on submit. The login form already disables its button via an
 * inline onsubmit handler; this only adds the visual affordance.
 */
function setupSubmitState() {
  const forms = document.querySelectorAll(".pf-v5-c-login__main form");

  for (const form of forms) {
    form.addEventListener("submit", () => {
      const submitButton = form.querySelector(
        'button[type="submit"].pf-m-primary',
      );

      if (submitButton) {
        submitButton.classList.add("kc-loading");
      }
    });
  }
}

setupCapsLockWarning();
setupRipple();
setupSubmitState();
