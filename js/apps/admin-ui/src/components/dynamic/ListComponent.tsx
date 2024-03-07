import { SelectVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { SelectControl } from "ui-shared";
import { convertToName } from "./DynamicComponents";
import type { ComponentProps } from "./components";

export const ListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
  required,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation();

  return (
    <SelectControl
      name={convertToName(name!)}
      label={t(label!)}
      labelIcon={t(helpText!)}
      controller={{
        defaultValue: defaultValue || options?.[0] || "",
        rules: {
          required: required ? t("required") : undefined,
        },
      }}
      isDisabled={isDisabled}
      variant={SelectVariant.single}
      options={options ?? []}
    />
  );
};
