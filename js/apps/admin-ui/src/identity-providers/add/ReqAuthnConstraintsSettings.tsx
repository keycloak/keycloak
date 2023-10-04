import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";

const comparisonValues = ["exact", "minimum", "maximum", "better"];

export const ReqAuthnConstraints = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const [comparisonOpen, setComparisonOpen] = useState(false);
  return (
    <>
      <FormGroup
        label={t("comparison")}
        labelIcon={
          <HelpItem helpText={t("comparisonHelp")} fieldLabelId="comparison" />
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
            helpText={t("authnContextClassRefsHelp")}
            fieldLabelId="authnContextClassRefs"
          />
        }
      >
        <MultiLineInput
          name="config.authnContextClassRefs"
          aria-label={t("identify-providers:authnContextClassRefs")}
          addButtonLabel="addAuthnContextClassRef"
          data-testid="classref-field"
        />
      </FormGroup>
      <FormGroup
        label={t("authnContextDeclRefs")}
        fieldId="kc-authnContextDeclRefs"
        labelIcon={
          <HelpItem
            helpText={t("authnContextDeclRefsHelp")}
            fieldLabelId="authnContextDeclRefs"
          />
        }
      >
        <MultiLineInput
          name="config.authnContextDeclRefs"
          aria-label={t("identify-providers:authnContextDeclRefs")}
          addButtonLabel="addAuthnContextDeclRef"
          data-testid="declref-field"
        />
      </FormGroup>
    </>
  );
};
