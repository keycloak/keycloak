import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { NumberComponentProps } from "./components";

export const NumberComponent = ({
  name,
  label,
  helpText,
  convertToName,
  ...props
}: NumberComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      type="number"
      label={t(label!)}
      labelIcon={t(helpText!)}
      data-testid={name}
      {...props}
    />
  );
};
