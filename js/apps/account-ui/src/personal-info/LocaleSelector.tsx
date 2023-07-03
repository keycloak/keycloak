import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { Option } from "ui-shared";
import { SelectControl } from "ui-shared";
import { supportedLocales } from "../api/methods";
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
  const [locales, setLocales] = useState<Option[]>([]);

  usePromise(
    (signal) => supportedLocales({ signal }),
    (locales) =>
      setLocales(
        locales.map<Option>(
          (l) =>
            ({
              key: l,
              value: localeToDisplayName(l),
            })
        )
      )
  );

  return (
    <SelectControl
      id="locale-select"
      name="attributes.locale"
      label={t("selectALocale")}
      controller={{ defaultValue: "" }}
      options={locales}
    />
  );
};
