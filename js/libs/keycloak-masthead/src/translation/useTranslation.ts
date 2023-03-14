import { useCallback, useMemo } from "react";

import { Translations } from "./translations";
import { useTranslations } from "./TranslationsContext";

export type TranslateFunction = (
  key: keyof Translations,
  args?: Record<string, string>
) => Translations[typeof key];

export const useTranslation = () => {
  const translations = useTranslations();
  const translate = useCallback<TranslateFunction>(
    (key, args) => {
      const translation = translations[key];

      if (!args) {
        return translation;
      }

      return Object.entries(args).reduce(
        (formatted, [key, value]) => formatted.replaceAll(`{{${key}}}`, value),
        translation
      );
    },
    [translations]
  );

  return useMemo(() => ({ t: translate }), [translate]);
};
