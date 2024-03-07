import { useTranslation } from "react-i18next";
import { TextControl } from "ui-shared";

import { convertToName } from "./DynamicComponents";
import type { ComponentProps } from "./components";

export const StringComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  required,
}: ComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      label={t(label!)}
      labelIcon={t(helpText!)}
      defaultValue={defaultValue?.toString()}
      rules={{
        required: required ? t("required") : undefined,
      }}
      isDisabled={isDisabled}
    />
  );
};
