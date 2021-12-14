import React from "react";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";

export const MultiValuedStringComponent = ({
  name,
  label,
  helpText,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <MultiLineInput
        name={`config.${name}`}
        aria-label={name}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: t(label!).toLowerCase(),
        })}
      />
    </FormGroup>
  );
};
