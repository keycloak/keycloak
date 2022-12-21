import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { MultiLineInput } from "../../components/multi-line-input/hook-form-v7/MultiLineInput";

const comparisonValues = ["exact", "minimum", "maximum", "better"];

export const ReqAuthnConstraints = () => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();
  const [comparisonOpen, setComparisonOpen] = useState(false);
  return (
    <>
      <FormGroup
        label={t("comparison")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:comparison"
            fieldLabelId="identity-providers:comparison"
          />
        }
        fieldId="comparison"
      >
        <Controller
          name="config.authnContextComparisonType"
          defaultValue={comparisonValues[0]}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="comparison"
              required
              direction="up"
              onToggle={(isExpanded) => setComparisonOpen(isExpanded)}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                setComparisonOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              aria-label={t("comparison")}
              isOpen={comparisonOpen}
            >
              {comparisonValues.map((option) => (
                <SelectOption
                  selected={option === field.value}
                  key={option}
                  value={option}
                >
                  {t(option)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("authnContextClassRefs")}
        fieldId="kc-authnContextClassRefs"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:authnContextClassRefs"
            fieldLabelId="authnContextClassRefs"
          />
        }
      >
        <MultiLineInput
          name="config.authnContextClassRefs"
          aria-label={t("identify-providers:authnContextClassRefs")}
          addButtonLabel="identity-providers:addAuthnContextClassRef"
          data-testid="classref-field"
        />
      </FormGroup>
      <FormGroup
        label={t("authnContextDeclRefs")}
        fieldId="kc-authnContextDeclRefs"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:authnContextDeclRefs"
            fieldLabelId="authnContextDeclRefs"
          />
        }
      >
        <MultiLineInput
          name="config.authnContextDeclRefs"
          aria-label={t("identify-providers:authnContextDeclRefs")}
          addButtonLabel="identity-providers:addAuthnContextDeclRef"
          data-testid="declref-field"
        />
      </FormGroup>
    </>
  );
};
