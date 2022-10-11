import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";

export const CredentialsResetActionMultiSelect = () => {
  const { t } = useTranslation("users");
  const { adminClient } = useAdminClient();
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);
  const [requiredActions, setRequiredActions] = useState<
    RequiredActionProviderRepresentation[]
  >([]);

  useFetch(
    () => adminClient.authenticationManagement.getRequiredActions(),
    (actions) => {
      setRequiredActions(actions);
    },
    []
  );

  return (
    <FormGroup
      label={t("resetActions")}
      labelIcon={
        <HelpItem
          helpText="clients-help:resetActions"
          fieldLabelId="resetActions"
        />
      }
      fieldId="actions"
    >
      <Controller
        name="actions"
        defaultValue={[]}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId="actions"
            variant={SelectVariant.typeaheadMulti}
            chipGroupProps={{
              numChips: 3,
            }}
            menuAppendTo="parent"
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={value}
            onSelect={(_, selectedValue) =>
              onChange(
                value.find((o: string) => o === selectedValue)
                  ? value.filter((item: string) => item !== selectedValue)
                  : [...value, selectedValue]
              )
            }
            onClear={(event) => {
              event.stopPropagation();
              onChange([]);
            }}
            typeAheadAriaLabel={t("resetActions")}
          >
            {requiredActions.map(({ alias, name }) => (
              <SelectOption
                key={alias}
                value={alias}
                data-testid={`${alias}-option`}
              >
                {name}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
