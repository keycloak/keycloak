import { FormProvider } from "react-hook-form";
import { SelectControl } from "../controls/SelectControl";
import { UserProfileFieldProps } from "./UserProfileFields";

const localeToDisplayName = (locale: string) => {
  try {
    return new Intl.DisplayNames([locale], { type: "language" }).of(locale);
  } catch (error) {
    return locale;
  }
};

type LocaleSelectorProps = Omit<UserProfileFieldProps, "inputType"> & {
  supportedLocales: string[];
};

export const LocaleSelector = ({
  t,
  form,
  supportedLocales,
}: LocaleSelectorProps) => {
  const locales = supportedLocales.map((locale) => ({
    key: locale,
    value: localeToDisplayName(locale) || "",
  }));
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
        options={locales}
      />
    </FormProvider>
  );
};
