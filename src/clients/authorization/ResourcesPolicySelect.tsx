import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { Select, SelectOption, SelectVariant } from "@patternfly/react-core";

import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type { Clients } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

type ResourcesPolicySelectProps = {
  name: keyof PolicyRepresentation;
  clientId: string;
  searchFunction: keyof Pick<Clients, "listPolicies" | "listResources">;
  variant?: SelectVariant;
  preSelected?: string;
  isRequired?: boolean;
};

type Policies = {
  id?: string;
  name?: string;
};

export const ResourcesPolicySelect = ({
  name,
  searchFunction,
  clientId,
  variant = SelectVariant.typeaheadMulti,
  preSelected,
  isRequired = false,
}: ResourcesPolicySelectProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();

  const { control, errors } = useFormContext<PolicyRepresentation>();
  const [items, setItems] = useState<Policies[]>([]);
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);

  useFetch(
    async () =>
      (
        await adminClient.clients[searchFunction](
          Object.assign(
            { id: clientId, first: 0, max: 10 },
            search === "" ? null : { name: search }
          )
        )
      ).map((p) => ({
        id: "_id" in p ? p._id : "id" in p ? p.id : undefined,
        name: p.name,
      })),
    (policies) => setItems(policies),
    [search]
  );

  const toSelectOptions = () =>
    items.map((p) => (
      <SelectOption key={p.id} value={p.id}>
        {p.name}
      </SelectOption>
    ));

  return (
    <Controller
      name={name}
      defaultValue={preSelected ? [preSelected] : []}
      control={control}
      rules={{ validate: (value) => !isRequired || value.length > 0 }}
      render={({ onChange, value }) => (
        <Select
          toggleId={name}
          variant={variant}
          onToggle={setOpen}
          onFilter={(_, filter) => {
            setSearch(filter);
            return toSelectOptions();
          }}
          onClear={() => {
            onChange([]);
            setSearch("");
          }}
          selections={value}
          onSelect={(_, selectedValue) => {
            const option = selectedValue.toString();
            const changedValue = value.find((p: string) => p === option)
              ? value.filter((p: string) => p !== option)
              : [...value, option];
            onChange(changedValue);
            setSearch("");
          }}
          isOpen={open}
          aria-labelledby={t(name)}
          isDisabled={!!preSelected}
          validated={errors[name] ? "error" : "default"}
        >
          {toSelectOptions()}
        </Select>
      )}
    />
  );
};
