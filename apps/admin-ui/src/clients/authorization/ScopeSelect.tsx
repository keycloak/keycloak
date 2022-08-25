import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { Select, SelectOption, SelectVariant } from "@patternfly/react-core";

import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

type ScopeSelectProps = {
  clientId: string;
  resourceId?: string;
  preSelected?: string;
};

export const ScopeSelect = ({
  clientId,
  resourceId,
  preSelected,
}: ScopeSelectProps) => {
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();

  const {
    control,
    setValue,
    formState: { errors },
  } = useFormContext();

  const [scopes, setScopes] = useState<ScopeRepresentation[]>([]);
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const firstUpdate = useRef(true);

  const toSelectOptions = (scopes: ScopeRepresentation[]) =>
    scopes.map((scope) => (
      <SelectOption key={scope.id} value={scope.id}>
        {scope.name}
      </SelectOption>
    ));

  useFetch(
    async () => {
      if (!resourceId) {
        return adminClient.clients.listAllScopes(
          Object.assign(
            { id: clientId, first: 0, max: 10, deep: false },
            search === "" ? null : { name: search }
          )
        );
      }

      if (resourceId && !firstUpdate.current) {
        setValue("scopes", []);
      }

      firstUpdate.current = false;
      return adminClient.clients.listScopesByResource({
        id: clientId,
        resourceName: resourceId,
      });
    },
    setScopes,
    [resourceId, search]
  );

  return (
    <Controller
      name="scopes"
      defaultValue={preSelected ? [preSelected] : []}
      control={control}
      rules={{ validate: (value) => value.length > 0 }}
      render={({ onChange, value }) => (
        <Select
          toggleId="scopes"
          variant={SelectVariant.typeaheadMulti}
          onToggle={setOpen}
          onFilter={(_, filter) => {
            setSearch(filter);
            return toSelectOptions(scopes);
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
          aria-labelledby={t("scopes")}
          validated={errors.scopes ? "error" : "default"}
          isDisabled={!!preSelected}
          typeAheadAriaLabel={t("scopes")}
        >
          {toSelectOptions(scopes)}
        </Select>
      )}
    />
  );
};
