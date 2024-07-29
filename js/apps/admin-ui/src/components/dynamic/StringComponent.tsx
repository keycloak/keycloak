import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { convertToName } from "./DynamicComponents";
import type { ComponentProps } from "./components";

export const StringComponent = ({
  name,
  label,
  helpText,
  ...props
}: ComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      label={t(label!)}
      helperText={t(helpText!)}
      data-testid={name}
      {...props}
    />
  );
};
