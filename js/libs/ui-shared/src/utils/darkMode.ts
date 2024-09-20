const DARK_MODE_CLASS = "pf-v5-theme-dark";
const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

function updateDarkMode(isEnabled: boolean) {
  const { classList } = document.documentElement;

  if (isEnabled) {
    classList.add(DARK_MODE_CLASS);
  } else {
    classList.remove(DARK_MODE_CLASS);
  }
}

export function initializeDarkMode() {
  updateDarkMode(mediaQuery.matches);
  mediaQuery.addEventListener("change", (event) =>
    updateDarkMode(event.matches),
  );
}
