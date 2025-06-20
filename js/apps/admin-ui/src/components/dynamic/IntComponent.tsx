import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { NumberComponentProps } from "./components";

export const IntComponent = ({
  name,
  label,
  helpText,
  readOnly,
  isDisabled = readOnly ?? false,
  convertToName,
  ...props
}: NumberComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      type="number"
      pattern="\d*"
      label={t(label!)}
      labelIcon={t(helpText!)}
      data-testid={name}
      isDisabled={isDisabled}
      {...props}
    />
  );
};
