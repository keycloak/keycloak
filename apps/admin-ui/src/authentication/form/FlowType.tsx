import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
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

const TYPES = ["basic-flow", "client-flow"] as const;

export const FlowType = () => {
  const { t } = useTranslation("authentication");
  const { control } = useFormContext<AuthenticationFlowRepresentation>();
  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t("flowType")}
      labelIcon={
        <HelpItem
          helpText="authentication-help:topLevelFlowType"
          fieldLabelId="authentication:flowType"
        />
      }
      fieldId="flowType"
    >
      <Controller
        name="providerId"
        defaultValue={TYPES[0]}
        control={control}
        render={({ field }) => (
          <Select
            toggleId="flowType"
            onToggle={setOpen}
            onSelect={(_, value) => {
              field.onChange(value.toString());
              setOpen(false);
            }}
            selections={t(`top-level-flow-type.${field.value}`)}
            variant={SelectVariant.single}
            aria-label={t("flowType")}
            isOpen={open}
          >
            {TYPES.map((type) => (
              <SelectOption
                key={type}
                selected={type === field.value}
                value={type}
              >
                {t(`top-level-flow-type.${type}`)}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
