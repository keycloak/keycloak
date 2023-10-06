import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import {
  Control,
  Controller,
  FieldPathByValue,
  FieldValues,
  PathValue,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";

export type RequiredActionMultiSelectProps<
  T extends FieldValues,
  P extends FieldPathByValue<T, string[] | undefined>,
> = {
  control: Control<T>;
  name: P;
  label: string;
  help: string;
};

export const RequiredActionMultiSelect = <
  T extends FieldValues,
  P extends FieldPathByValue<T, string[] | undefined>,
>({
  control,
  name,
  label,
  help,
}: RequiredActionMultiSelectProps<T, P>) => {
  const { t } = useTranslation();
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
        defaultValue={[] as PathValue<T, P>}
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
            selections={field.value as string[]}
            onSelect={(_, selectedValue) => {
              const value: string[] = field.value;
              field.onChange(
                value.find((item) => item === selectedValue)
                  ? value.filter((item) => item !== selectedValue)
                  : [...value, selectedValue],
              );
            }}
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
