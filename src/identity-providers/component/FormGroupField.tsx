import React, { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";

export type FieldProps = { label: string; field: string; isReadOnly?: boolean };
export type FormGroupFieldProps = { label: string; children: ReactNode };

export const FormGroupField = ({ label, children }: FormGroupFieldProps) => {
  const { t } = useTranslation("identity-providers");
  return (
    <FormGroup
      label={t(label)}
      fieldId={label}
      labelIcon={
        <HelpItem
          helpText={`identity-providers-help:${label}`}
          forLabel={t(label)}
          forID={label}
        />
      }
    >
      {children}
    </FormGroup>
  );
};
