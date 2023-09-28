import { SelectControl } from "../controls/SelectControl";
import { UserProfileFieldsProps } from "./UserProfileGroup";

const localeToDisplayName = (locale: string) => {
  try {
    return new Intl.DisplayNames([locale], { type: "language" }).of(locale);
  } catch (error) {
    return locale;
  }
};

type LocaleSelectorProps = UserProfileFieldsProps & {
  supportedLocales: string[];
};

export const LocaleSelector = ({
  t,
  supportedLocales,
}: LocaleSelectorProps) => {
  const locales = supportedLocales.map((locale) => ({
    key: locale,
    value: localeToDisplayName(locale) || "",
  }));

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
