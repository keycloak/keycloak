import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  KeycloakSelect,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { SelectOption } from "@patternfly/react-core";
import { useEffect, useMemo, useRef, useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
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
    setValue,
    formState: { errors },
  } = useFormContext();

  const [scopes, setScopes] = useState<ScopeRepresentation[]>([]);
  const [selectedScopes, setSelectedScopes] = useState<ScopeRepresentation[]>(
    [],
  );
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const scopesById = useRef(new Map<string, ScopeRepresentation>());
  const previousResourceId = useRef(resourceId);

  // Avoid passing a defaultValue to useWatch, otherwise it can mask the form value during initialization.
  const watchedValues = useWatch({ control, name: "scopes" });
  const values: string[] = useMemo(
    () => watchedValues ?? (preSelected ? [preSelected] : []),
    [watchedValues, preSelected],
  );

  const cacheScopes = (scopes: ScopeRepresentation[]) =>
    scopes.forEach(
      (scope) => scope.id && scopesById.current.set(scope.id, scope),
    );

  const toSelectOptions = (scopes: ScopeRepresentation[]) =>
    scopes.map((scope) => (
      <SelectOption key={scope.id} value={scope}>
        {scope.name}
      </SelectOption>
    ));

  // Changing the resource invalidates the current scope selection.
  useEffect(() => {
    if (previousResourceId.current === resourceId) {
      return;
    }

    previousResourceId.current = resourceId;

    if (resourceId) {
      setValue("scopes", []);
    }
  }, [resourceId]);

  useFetch(
    async (): Promise<ScopeRepresentation[]> => {
      if (!resourceId) {
        return adminClient.clients.listAllScopes(
          Object.assign(
            { id: clientId, deep: false, max: 1000 },
            search === "" ? null : { name: search },
          ),
        );
      }
      return adminClient.clients.listScopesByResource({
        id: clientId,
        resourceName: resourceId,
      });
    },
    (scopes) => {
      cacheScopes(scopes);
      setScopes(scopes);
    },
    [resourceId, search],
  );

  // Selected scopes may not exist in the currently loaded page, so resolve them independently by id.
  useFetch(
    async (): Promise<ScopeRepresentation[]> => {
      const unknown = values.filter((id) => !scopesById.current.has(id));

      const fetched = await Promise.all(
        unknown.map((scopeId) =>
          adminClient.clients
            .getAuthorizationScope({ id: clientId, scopeId })
            .catch(() => undefined),
        ),
      );

      cacheScopes(fetched.filter((scope) => scope !== undefined));

      return values
        .map((id) => scopesById.current.get(id))
        .filter((scope) => scope !== undefined);
    },
    setSelectedScopes,
    [values],
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

            cacheScopes(changedValue);
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
