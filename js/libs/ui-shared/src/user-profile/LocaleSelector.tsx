import { useMemo } from "react";
import { FormProvider } from "react-hook-form";
import { SelectControl } from "../controls/select-control/SelectControl";
import { UserProfileFieldProps } from "./UserProfileFields";

const localeToDisplayName = (locale: string) => {
  try {
    return new Intl.DisplayNames([locale], { type: "language" }).of(locale);
  } catch {
    return locale;
  }
};

type LocaleSelectorProps = Omit<UserProfileFieldProps, "inputType"> & {
  supportedLocales: string[];
  currentLocale: string;
};

export const LocaleSelector = ({
  t,
  form,
  supportedLocales,
  currentLocale,
}: LocaleSelectorProps) => {
  const locales = useMemo(
    () =>
      supportedLocales
        .map((locale) => ({
          key: locale,
          value: t(`locale_${locale}`, localeToDisplayName(locale) ?? locale),
        }))
        .sort((a, b) => a.value.localeCompare(b.value, currentLocale)),
    [supportedLocales, currentLocale, t],
  );

  if (!locales.length) {
    return null;
  }
  return (
    <FormProvider {...form}>
      <SelectControl
        data-testid="locale-select"
        name="attributes.locale"
        label={t("selectALocale")}
        controller={{ defaultValue: "" }}
        options={[{ key: "", value: t("defaultLocale") }, ...locales]}
        variant={locales.length >= 10 ? "typeahead" : "single"}
      />
    </FormProvider>
  );
};
