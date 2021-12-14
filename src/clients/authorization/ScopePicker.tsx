import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { HelpItem } from "../../components/help-enabler/HelpItem";

type Scope = {
  id: string;
  name: string;
};

export const ScopePicker = ({ clientId }: { clientId: string }) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext();

  const [open, setOpen] = useState(false);
  const [scopes, setScopes] = useState<JSX.Element[]>();
  const [search, setSearch] = useState("");

  const adminClient = useAdminClient();

  useFetch(
    () => {
      const params = {
        id: clientId,
        first: 0,
        max: 20,
        deep: false,
        name: search,
      };
      return adminClient.clients.listAllScopes(params);
    },
    (scopes) =>
      setScopes(
        scopes.map((option) => (
          <SelectOption key={option.id} value={option}>
            {option.name}
          </SelectOption>
        ))
      ),
    [search]
  );

  return (
    <FormGroup
      label={t("authorizationScopes")}
      labelIcon={
        <HelpItem
          helpText="clients-help:scopes"
          fieldLabelId="clients:scopes"
        />
      }
      fieldId="scopes"
    >
      <Controller
        name="scopes"
        defaultValue={[]}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId="scopes"
            variant={SelectVariant.typeaheadMulti}
            chipGroupProps={{
              numChips: 3,
              expandedText: t("common:hide"),
              collapsedText: t("common:showRemaining"),
            }}
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={value.map((o: Scope) => o.name)}
            onFilter={(_, value) => {
              setSearch(value);
              return scopes;
            }}
            onSelect={(_, selectedValue) => {
              const option =
                typeof selectedValue === "string"
                  ? selectedValue
                  : (selectedValue as Scope).name;
              const changedValue = value.find((o: Scope) => o.name === option)
                ? value.filter((item: Scope) => item.name !== option)
                : [...value, selectedValue];
              onChange(changedValue);
            }}
            onClear={(event) => {
              event.stopPropagation();
              setSearch("");
              onChange([]);
            }}
            aria-label={t("authorizationScopes")}
          >
            {scopes}
          </Select>
        )}
      />
    </FormGroup>
  );
};
