import { FormGroup, TextArea } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { convertToName } from "./DynamicComponents";
import type { ComponentProps } from "./components";

export const TextComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { register } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
    >
      <TextArea
        id={name!}
        data-testid={name}
        isDisabled={isDisabled}
        defaultValue={defaultValue?.toString()}
        {...register(convertToName(name!))}
      />
    </FormGroup>
  );
};
