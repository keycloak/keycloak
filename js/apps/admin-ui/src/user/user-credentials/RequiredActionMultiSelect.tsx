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

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";

type RequiredActionMultiSelectProps = {
  name: string;
  label: string;
  help: string;
};

export const RequiredActionMultiSelect = ({
  name,
  label,
  help,
}: RequiredActionMultiSelectProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);
  const [requiredActions, setRequiredActions] = useState<
    RequiredActionProviderRepresentation[]
  >([]);

  useFetch(
    () => adminClient.authenticationManagement.getRequiredActions(),
    (actions) => {
      const enabledUserActions = actions.filter((action) => {
        return action.enabled;
      });
      setRequiredActions(enabledUserActions);
    },
    [],
  );

  return (
    <FormGroup
      label={t(label)}
      labelIcon={<HelpItem helpText={t(help)} fieldLabelId="resetAction" />}
      fieldId="actions"
    >
      <Controller
        name={name}
        defaultValue={[]}
        control={control}
        render={({ field }) => (
          <Select
            maxHeight={375}
            toggleId={`${name}-actions`}
            variant={SelectVariant.typeaheadMulti}
            chipGroupProps={{
              numChips: 3,
            }}
            placeholderText={t("requiredActionPlaceholder")}
            menuAppendTo="parent"
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={field.value}
            onSelect={(_, selectedValue) =>
              field.onChange(
                field.value.find((o: string) => o === selectedValue)
                  ? field.value.filter((item: string) => item !== selectedValue)
                  : [...field.value, selectedValue],
              )
            }
            onClear={(event) => {
              event.stopPropagation();
              field.onChange([]);
            }}
            typeAheadAriaLabel={t("resetAction")}
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
