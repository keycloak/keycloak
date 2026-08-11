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

/**
 * Light/dark toggle. The initial scheme is resolved by an inline script in the
 * document head so there is no flash of the wrong theme; this only handles the
 * click and records the choice.
 */
function setupThemeToggle() {
  const button = document.getElementById("kc-theme-toggle");
  const darkClass = document.body.dataset.darkClass;

  if (!button || !darkClass) {
    return;
  }

  const sync = (isDark) => {
    button.setAttribute("aria-pressed", String(isDark));
  };

  sync(document.documentElement.classList.contains(darkClass));

  button.addEventListener("click", () => {
    const isDark = document.documentElement.classList.toggle(darkClass);

    try {
      localStorage.setItem("fidar-color-scheme", isDark ? "dark" : "light");
    } catch (error) {
      // Storage unavailable (blocked cookies); the toggle still applies for
      // this page view, it just will not be remembered.
    }

    sync(isDark);
  });
}

/**
 * Tenant logo shown inside the sign-in card, above the page title.
 *
 * Mirrors the behaviour of the Keycloakify build's useRealmBranding hook:
 * resolve `{logoLightUrl, logoDarkUrl}` for the realm from the branding API,
 * cache per realm in sessionStorage so subsequent pages in the same flow
 * (password, OTP, reset) don't refetch, and fall back silently to no logo.
 *
 * The Fidar wordmark on the brand panel is unaffected — this is additive.
 */
// This login page is served per-tenant at {tenant}.auth.fidar.io (or bare
// auth.fidar.io on the shared/legacy host) — the branding API for that same
// tenant lives at {tenant}.sdk.fidar.io / sdk.fidar.io respectively, never
// at a single fixed production host. Swap the "auth" label for "sdk" in the
// page's own hostname instead of hardcoding one tenant's domain.
const BRANDING_API = `https://${window.location.hostname.replace(/(^|\.)auth\./, "$1sdk.")}/fidar/sdk/api`;
const BRANDING_CACHE_PREFIX = "fidar_branding_";

function readBrandingCache(realm) {
  try {
    const raw = sessionStorage.getItem(BRANDING_CACHE_PREFIX + realm);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function writeBrandingCache(realm, branding) {
  try {
    sessionStorage.setItem(
      BRANDING_CACHE_PREFIX + realm,
      JSON.stringify(branding),
    );
  } catch (error) {
    // sessionStorage unavailable — the logo still renders, just refetches.
  }
}

function setupRealmLogo() {
  const container = document.getElementById("kc-realm-logo");
  const image = document.getElementById("kc-realm-logo-img");
  const realm = document.body.dataset.realm;
  const darkClass = document.body.dataset.darkClass;

  // `master` is the admin realm and has no tenant branding of its own.
  if (!container || !image || !realm || realm === "master") {
    return;
  }

  const apply = (branding) => {
    if (!branding) {
      return;
    }

    const pick = () => {
      const isDark =
        !!darkClass && document.documentElement.classList.contains(darkClass);
      // Either variant stands in for a missing counterpart.
      return isDark
        ? branding.logoDarkUrl || branding.logoLightUrl
        : branding.logoLightUrl || branding.logoDarkUrl;
    };

    const url = pick();

    if (!url) {
      return;
    }

    // Only reveal once the image has actually decoded, so a broken or slow
    // URL never leaves an empty box above the title.
    image.addEventListener("load", () => container.removeAttribute("hidden"), {
      once: true,
    });
    image.addEventListener("error", () => container.setAttribute("hidden", ""), {
      once: true,
    });
    image.src = url;

    // Keep the variant in step with the theme toggle.
    const toggle = document.getElementById("kc-theme-toggle");

    if (toggle) {
      toggle.addEventListener("click", () => {
        const next = pick();

        if (next) {
          image.src = next;
        }
      });
    }
  };

  const cached = readBrandingCache(realm);

  if (cached) {
    apply(cached);
    return;
  }

  fetch(`${BRANDING_API}/public/tenant-branding/${encodeURIComponent(realm)}`)
    .then((response) => (response.ok ? response.json() : null))
    .then((json) => {
      if (!json) {
        return;
      }

      const branding = {
        logoLightUrl: json.logoLightUrl ?? null,
        logoDarkUrl: json.logoDarkUrl ?? null,
      };

      // Cached even when both URLs are null. Realms with no logo of their own
      // are the common case, and skipping the write meant every screen in the
      // flow — passkey, password, OTP, reset — refetched the same empty answer.
      // apply() and pick() both no-op on a logo-less realm.
      writeBrandingCache(realm, branding);
      apply(branding);
    })
    .catch(() => {
      // Branding is decorative; a failed lookup must never block sign-in.
    });
}

/**
 * OAuth 2.0 device grant: bounce back into the native app once the browser leg
 * has completed.
 *
 * The markup is only emitted on the device-verification-complete info page (see
 * template.ftl), so the presence of the container *is* the condition — nothing
 * is sniffed from the page text here, unlike the Keycloakify build this
 * replaces.
 *
 * The short pause lets the success message register before the app takes the
 * foreground back; without it the tab appears to flicker and close. If no app
 * has claimed the scheme the navigation is a no-op and the user stays put with
 * the link that is already on the page.
 */
const DEVICE_CALLBACK_DELAY_MS = 1200;

function setupDeviceCallback() {
  const container = document.getElementById("kc-device-callback");
  const uri = container?.dataset.callbackUri;

  if (!uri) {
    return;
  }

  window.setTimeout(() => {
    window.location.href = uri;
  }, DEVICE_CALLBACK_DELAY_MS);
}

setupCapsLockWarning();
setupRipple();
setupSubmitState();
setupThemeToggle();
setupRealmLogo();
setupDeviceCallback();
