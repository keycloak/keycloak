import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  KeycloakSelect,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { SelectOption } from "@patternfly/react-core";
import { useRef, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const {
    control,
    getValues,
    setValue,
    formState: { errors },
  } = useFormContext();

  const [scopes, setScopes] = useState<ScopeRepresentation[]>([]);
  const [selectedScopes, setSelectedScopes] = useState<ScopeRepresentation[]>(
    [],
  );
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const firstUpdate = useRef(true);

  const values: string[] | undefined = getValues("scopes");

  const toSelectOptions = (scopes: ScopeRepresentation[]) =>
    scopes.map((scope) => (
      <SelectOption key={scope.id} value={scope}>
        {scope.name}
      </SelectOption>
    ));

  useFetch(
    async (): Promise<ScopeRepresentation[]> => {
      if (!resourceId) {
        return adminClient.clients.listAllScopes(
          Object.assign(
            { id: clientId, deep: false },
            search === "" ? null : { name: search },
          ),
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
    (scopes) => {
      setScopes(scopes);
      if (!search)
        setSelectedScopes(
          scopes.filter((s: ScopeRepresentation) => values?.includes(s.id!)),
        );
    },
    [resourceId, search],
  );

  return (
    <Controller
      name="scopes"
      defaultValue={preSelected ? [preSelected] : []}
      control={control}
      rules={{ validate: (value) => value.length > 0 }}
      render={({ field }) => (
        <KeycloakSelect
          toggleId="scopes"
          variant={SelectVariant.typeaheadMulti}
          onToggle={(val) => setOpen(val)}
          onFilter={(filter) => {
            setSearch(filter);
            return toSelectOptions(scopes);
          }}
          onClear={() => {
            field.onChange([]);
            setSearch("");
          }}
          selections={selectedScopes.map((s) => s.name!)}
          onSelect={(selectedValue) => {
            const option =
              typeof selectedValue === "string"
                ? selectedScopes.find((s) => s.name === selectedValue)!
                : (selectedValue as ScopeRepresentation);
            const changedValue = selectedScopes.find((p) => p.id === option.id)
              ? selectedScopes.filter((p) => p.id !== option.id)
              : [...selectedScopes, option];

            field.onChange(changedValue.map((s) => s.id));
            setSelectedScopes(changedValue);
            setSearch("");
          }}
          isOpen={open}
          aria-labelledby={t("scopes")}
          validated={errors.scopes ? "error" : "default"}
          isDisabled={!!preSelected}
          typeAheadAriaLabel={t("scopes")}
        >
          {toSelectOptions(scopes)}
        </KeycloakSelect>
      )}
    />
  );
};
