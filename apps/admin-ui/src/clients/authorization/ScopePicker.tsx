import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { HelpItem } from "ui-shared";

type Scope = {
  id: string;
  name: string;
};

export const ScopePicker = ({ clientId }: { clientId: string }) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext();

  const [open, setOpen] = useState(false);
  const [scopes, setScopes] = useState<ScopeRepresentation[]>();
  const [search, setSearch] = useState("");

  const { adminClient } = useAdminClient();

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
    setScopes,
    [search]
  );

  const renderScopes = (scopes?: ScopeRepresentation[]) =>
    scopes?.map((option) => (
      <SelectOption key={option.id} value={option}>
        {option.name}
      </SelectOption>
    ));

  return (
    <FormGroup
      label={t("authorizationScopes")}
      labelIcon={
        <HelpItem
          helpText={t("clients-help:scopes")}
          fieldLabelId="clients:scopes"
        />
      }
      fieldId="scopes"
    >
      <Controller
        name="scopes"
        defaultValue={[]}
        control={control}
        render={({ field }) => (
          <Select
            toggleId="scopes"
            variant={SelectVariant.typeaheadMulti}
            chipGroupProps={{
              numChips: 3,
              expandedText: t("common:hide"),
              collapsedText: t("common:showRemaining"),
            }}
            onToggle={setOpen}
            isOpen={open}
            selections={field.value.map((o: Scope) => o.name)}
            onFilter={(_, value) => {
              setSearch(value);
              return renderScopes(scopes);
            }}
            onSelect={(_, selectedValue) => {
              const option =
                typeof selectedValue === "string"
                  ? selectedValue
                  : (selectedValue as Scope).name;
              const changedValue = field.value.find(
                (o: Scope) => o.name === option
              )
                ? field.value.filter((item: Scope) => item.name !== option)
                : [...field.value, selectedValue];
              field.onChange(changedValue);
            }}
            onClear={(event) => {
              event.stopPropagation();
              setSearch("");
              field.onChange([]);
            }}
            typeAheadAriaLabel={t("authorizationScopes")}
          >
            {renderScopes(scopes)}
          </Select>
        )}
      />
    </FormGroup>
  );
};
