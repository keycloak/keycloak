import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { Select, SelectOption, SelectVariant } from "@patternfly/react-core";

import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type {
  Clients,
  PolicyQuery,
} from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

type Type = "resources" | "policies";

type ResourcesPolicySelectProps = {
  name: Type;
  clientId: string;
  permissionId?: string;
  variant?: SelectVariant;
  preSelected?: string;
  isRequired?: boolean;
};

type Policies = {
  id?: string;
  name?: string;
};

type TypeMapping = {
  [key in Type]: {
    searchFunction: keyof Pick<Clients, "listPolicies" | "listResources">;
    fetchFunction: keyof Pick<
      Clients,
      "getAssociatedPolicies" | "getAssociatedResources"
    >;
  };
};

const typeMapping: TypeMapping = {
  resources: {
    searchFunction: "listResources",
    fetchFunction: "getAssociatedResources",
  },
  policies: {
    searchFunction: "listPolicies",
    fetchFunction: "getAssociatedPolicies",
  },
};

export const ResourcesPolicySelect = ({
  name,
  clientId,
  permissionId,
  variant = SelectVariant.typeaheadMulti,
  preSelected,
  isRequired = false,
}: ResourcesPolicySelectProps) => {
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();

  const {
    control,
    formState: { errors },
  } = useFormContext<PolicyRepresentation>();
  const [items, setItems] = useState<Policies[]>([]);
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);

  const functions = typeMapping[name];

  const convert = (
    p: PolicyRepresentation | ResourceRepresentation
  ): Policies => ({
    id: "_id" in p ? p._id : "id" in p ? p.id : undefined,
    name: p.name,
  });

  useFetch(
    async () => {
      const params: PolicyQuery = Object.assign(
        { id: clientId, first: 0, max: 10, permission: "false" },
        search === "" ? null : { name: search }
      );
      return (
        await Promise.all([
          adminClient.clients[functions.searchFunction](params),
          permissionId
            ? adminClient.clients[functions.fetchFunction]({
                id: clientId,
                permissionId,
              })
            : Promise.resolve([]),
        ])
      )
        .flat()
        .filter(
          (r): r is PolicyRepresentation | ResourceRepresentation =>
            typeof r !== "string"
        )
        .map(convert)
        .filter(
          ({ id }, index, self) =>
            index === self.findIndex(({ id: otherId }) => id === otherId)
        );
    },
    setItems,
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
          typeAheadAriaLabel={t(name)}
        >
          {toSelectOptions()}
        </Select>
      )}
    />
  );
};
