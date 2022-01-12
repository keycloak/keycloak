import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { Select, SelectOption, SelectVariant } from "@patternfly/react-core";

import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type { Clients } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

type ResourcesPolicySelectProps = {
  name: string;
  clientId: string;
  searchFunction: keyof Pick<Clients, "listPolicies" | "listResources">;
};

export const ResourcesPolicySelect = ({
  name,
  searchFunction,
  clientId,
}: ResourcesPolicySelectProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();

  const { control } = useFormContext<PolicyRepresentation>();
  const [items, setItems] = useState<JSX.Element[]>([]);
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
    (policies) =>
      setItems(
        policies.map((p) => (
          <SelectOption key={p.id} value={p.id}>
            {p.name}
          </SelectOption>
        ))
      ),
    [search]
  );

  return (
    <Controller
      name={name}
      defaultValue={[]}
      control={control}
      render={({ onChange, value }) => (
        <Select
          toggleId={name}
          variant={SelectVariant.typeaheadMulti}
          onToggle={setOpen}
          onFilter={(_, filter) => {
            setSearch(filter);
            return items;
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
          aria-labelledby={t("policies")}
        >
          {items}
        </Select>
      )}
    />
  );
};
