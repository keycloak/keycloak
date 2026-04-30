import i18next from "i18next";
import { environment } from "./environment.js";
import { joinPath } from "./utils/join-path.js";

const DEFAULT_LOCALE = "en";

export const i18n = i18next.createInstance();

/**
 * Fetch translations from the Keycloak server
 * @param {string} locale
 * @returns {Promise<Record<string, string>>}
 */
/**
 * Convert Java MessageFormat placeholders ({0}, {1}) to i18next format ({{0}}, {{1}})
 * @param {string} value
 * @returns {string}
 */
function convertPlaceholders(value) {
  return value.replace(/\{(\d+)\}/g, "{{$1}}");
}

async function fetchTranslations(locale) {
  const url = joinPath(
    environment.serverBaseUrl,
    `resources/${environment.realm}/account/${locale}`,
  );

  try {
    const response = await fetch(url);
    if (!response.ok) {
      console.warn(
        `Failed to fetch translations for locale ${locale}: ${response.status}`,
      );
      return {};
    }

    const data = await response.json();

    if (Array.isArray(data)) {
      return Object.fromEntries(
        data.map(({ key, value }) => [key, convertPlaceholders(value)]),
      );
    }

    if (typeof data === "object" && data !== null) {
      return Object.fromEntries(
        Object.entries(data).map(([key, value]) => [
          key,
          convertPlaceholders(value),
        ]),
      );
    }

    console.warn(`Unexpected translation format for locale ${locale}`);
    return {};
  } catch (error) {
    console.error(`Error fetching translations for locale ${locale}:`, error);
    return {};
  }
}

/**
 * Check if a string is a Keycloak bundle key (e.g., "${myKey}")
 * @param {unknown} displayName
 * @returns {boolean}
 */
export function isBundleKey(displayName) {
  return displayName && typeof displayName === "string"
    ? displayName.includes("${")
    : false;
}

/**
 * Unwrap a bundle key by removing ${ and }
 * @param {string} key
 * @returns {string}
 */
export function unWrap(key) {
  return key.substring(2, key.length - 1);
}

/**
 * Initialize i18n with translations fetched from the server
 * @returns {Promise<typeof i18n>}
 */
export async function initI18n() {
  const locale = environment.locale || DEFAULT_LOCALE;

  const [translations, fallbackTranslations] = await Promise.all([
    fetchTranslations(locale),
    locale !== DEFAULT_LOCALE
      ? fetchTranslations(DEFAULT_LOCALE)
      : Promise.resolve({}),
  ]);

  const resources = {
    [locale]: { translation: translations },
  };

  if (
    locale !== DEFAULT_LOCALE &&
    Object.keys(fallbackTranslations).length > 0
  ) {
    resources[DEFAULT_LOCALE] = { translation: fallbackTranslations };
  }

  await i18n.init({
    lng: locale,
    fallbackLng: DEFAULT_LOCALE,
    interpolation: {
      escapeValue: false,
    },
    resources,
  });

  return i18n;
}

/**
 * Translate a key, automatically handling Keycloak bundle keys
 * @param {string} key
 * @param {Record<string, unknown>} [params]
 * @returns {string}
 */
export function t(key, params) {
  if (isBundleKey(key)) {
    return i18n.t(unWrap(key), params);
  }
  return i18n.t(key, params);
}

/**
 * Get label for display - handles both bundle keys and plain strings
 * @param {string | undefined} label - The label or bundle key
 * @param {string} [fallback] - Fallback key if label is not provided
 * @returns {string}
 */
export function label(label, fallback) {
  if (!label && fallback) {
    return t(fallback);
  }
  if (!label) {
    return "";
  }
  if (isBundleKey(label)) {
    return t(label);
  }
  return label;
}
