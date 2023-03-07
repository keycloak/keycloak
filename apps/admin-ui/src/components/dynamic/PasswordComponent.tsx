import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { PasswordInput } from "../password-input/PasswordInput";
import type { ComponentProps } from "./components";
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
        defaultValue={defaultValue?.toString()}
        {...register(convertToName(name!))}
      />
    </FormGroup>
  );
};
