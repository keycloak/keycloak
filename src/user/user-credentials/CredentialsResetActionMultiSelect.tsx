import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import { HelpItem } from "../../components/help-enabler/HelpItem";

export const CredentialsResetActionMultiSelect = () => {
  const { t } = useTranslation("users");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

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
            {Object.values(RequiredActionAlias).map((action, index) => (
              <SelectOption
                key={index}
                value={action}
                data-testid={`${action}-option`}
              >
                {t(action)}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
