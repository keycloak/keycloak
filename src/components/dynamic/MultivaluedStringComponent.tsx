import React from "react";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { convertToHyphens } from "../../util";

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
        <HelpItem helpText={t(helpText!)} forLabel={t(label!)} forID={name!} />
      }
      fieldId={name!}
    >
      <MultiLineInput
        name={`config.${convertToHyphens(name!)}`}
        aria-label={name}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: t(label!).toLowerCase(),
        })}
      />
    </FormGroup>
  );
};
