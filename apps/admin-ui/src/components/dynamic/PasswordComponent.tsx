import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "../help-enabler/HelpItem";
import { PasswordInput } from "../password-input/PasswordInput";
import { convertToName } from "./DynamicComponents";

export const PasswordComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { register } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <PasswordInput
        id={name!}
        data-testid={name}
        isDisabled={isDisabled}
        ref={register()}
        name={convertToName(name!)}
        defaultValue={defaultValue?.toString()}
      />
    </FormGroup>
  );
};
