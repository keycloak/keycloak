import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
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
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

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
          helpText={t("clients-help:resetActions")}
          fieldLabelId="resetActions"
        />
      }
      fieldId="actions"
    >
      <Controller
        name="actions"
        defaultValue={[]}
        control={control}
        render={({ field }) => (
          <Select
            maxHeight={375}
            toggleId="actions"
            variant={SelectVariant.typeaheadMulti}
            chipGroupProps={{
              numChips: 3,
            }}
            menuAppendTo="parent"
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={field.value}
            onSelect={(_, selectedValue) =>
              field.onChange(
                field.value.find((o: string) => o === selectedValue)
                  ? field.value.filter((item: string) => item !== selectedValue)
                  : [...field.value, selectedValue]
              )
            }
            onClear={(event) => {
              event.stopPropagation();
              field.onChange([]);
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
