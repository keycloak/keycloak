import { CallbackError, ReadCallback, ResourceKey } from "i18next";
import HttpBackend from "i18next-http-backend";

import { adminClient } from "../admin-client";
import { DEFAULT_LOCALE, KEY_SEPARATOR } from "./i18n";

/** A custom backend that merges the overrides the static labels with those defined by the user in the console. */
export class OverridesBackend extends HttpBackend {
  #overridesCache = new Map<string, Promise<Record<string, string>>>();

  async loadUrl(
    url: string,
    callback: ReadCallback,
    languages?: string | string[],
    namespaces?: string | string[],
  ) {
    try {
      const [data, overrides] = await Promise.all([
        this.#loadUrlPromisified(url, languages, namespaces),
        this.#loadOverrides(languages),
      ]);

      const namespace = this.#determineNamespace(namespaces);

      // Bail out on applying overrides if the namespace could not be determined.
      if (!namespace) {
        return callback(null, data);
      }

      callback(null, this.#applyOverrides(data, overrides));
    } catch (error) {
      callback(error as CallbackError, null);
    }
  }

  #applyOverrides(data: ResourceKey, overrides: Record<string, string>) {
    if (typeof data === "string") {
      return data;
    }

    // Ensure we are operating on a cloned data structure to prevent in-place mutations.
    const target = structuredClone(data);

    for (const [path, value] of Object.entries(overrides)) {
      this.#applyOverride(target, path, value);
    }

    return target;
  }

  /** Applies an override by converting path segments denoted with a key separator as nested objects and merging the result. */
  #applyOverride(target: Record<string, any>, path: string, value: string) {
    const trail = path.split(KEY_SEPARATOR);
    let pointer = target;

    trail.forEach((segment, index) => {
      const isLast = index === trail.length - 1;
      pointer = pointer[segment] = isLast ? value : pointer[segment] ?? {};
    });
  }

  #loadOverrides(languages?: string | string[]) {
    const locale = this.#determineLocale(languages);
    const cachedOverrides = this.#overridesCache.get(locale);

    if (cachedOverrides) {
      return cachedOverrides;
    }

    const overrides = adminClient.realms.getRealmLocalizationTexts({
      realm: adminClient.realmName,
      selectedLocale: locale,
    });

    this.#overridesCache.set(locale, overrides);

    // Evict cached request on failure.
    overrides.catch((error) => {
      this.#overridesCache.delete(locale);
      return Promise.reject(error);
    });

    return overrides;
  }

  #determineLocale(languages?: string | string[]) {
    if (typeof languages === "string") {
      return languages;
    }

    return languages?.[0] ?? DEFAULT_LOCALE;
  }

  #determineNamespace(namespaces?: string | string[]) {
    if (typeof namespaces === "string") {
      return namespaces;
    }

    return namespaces?.[0];
  }

  #loadUrlPromisified(
    url: string,
    languages?: string | string[],
    namespaces?: string | string[],
  ) {
    return new Promise<ResourceKey>((resolve, reject) => {
      const callback: ReadCallback = (error, data) => {
        if (error) {
          return reject(error);
        }

        if (typeof data !== "object" || data === null) {
          return reject(
            new Error(
              "Unable to load URL, data returned is of an unsupported type.",
              { cause: error },
            ),
          );
        }

        resolve(data);
      };

      super.loadUrl(url, callback, languages, namespaces);
    });
  }
}
