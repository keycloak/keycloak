import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { SelectControlOption } from "ui-shared";
import { SelectControl } from "ui-shared";
import { getSupportedLocales } from "../api/methods";
import { usePromise } from "../utils/usePromise";

const localeToDisplayName = (locale: string) => {
  try {
    return new Intl.DisplayNames([locale], { type: "language" }).of(locale);
  } catch (error) {
    return locale;
  }
};

export const LocaleSelector = () => {
  const { t } = useTranslation();
  const [locales, setLocales] = useState<SelectControlOption[]>([]);

  usePromise(
    (signal) => getSupportedLocales({ signal }),
    (locales) =>
      setLocales(
        locales.map<SelectControlOption>((locale) => ({
          key: locale,
          value: localeToDisplayName(locale) || "",
        })),
      ),
  );

  return (
    <SelectControl
      data-testid="locale-select"
      name="attributes.locale"
      label={t("selectALocale")}
      controller={{ defaultValue: "" }}
      options={locales}
    />
  );
};
