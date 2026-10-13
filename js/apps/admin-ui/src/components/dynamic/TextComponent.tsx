import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeycloakTextArea, HelpItem } from "@keycloak/keycloak-ui-shared";
import type { ComponentProps } from "./components";

export const TextComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  required,
  isDisabled = false,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { register } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      required={required}
    >
      <KeycloakTextArea
        id={name!}
        data-testid={name}
        isDisabled={isDisabled}
        defaultValue={defaultValue?.toString()}
        {...register(convertToName(name!))}
      />
    </FormGroup>
  );
};
