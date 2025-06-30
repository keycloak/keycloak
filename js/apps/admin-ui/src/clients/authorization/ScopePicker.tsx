import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useFetch } from "../../utils/useFetch";

type Scope = {
  id: string;
  name: string;
};

export const ScopePicker = ({ clientId }: { clientId: string }) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { control } = useFormContext();

  const [open, setOpen] = useState(false);
  const [scopes, setScopes] = useState<ScopeRepresentation[]>();
  const [search, setSearch] = useState("");

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
    [search],
  );

  const renderScopes = (scopes: ScopeRepresentation[]) =>
    scopes.map((option) => (
      <SelectOption key={option.id} value={option}>
        {option.name}
      </SelectOption>
    ));

  if (!scopes) return <KeycloakSpinner />;
  return (
    <FormGroup
      label={t("authorizationScopes")}
      labelIcon={
        <HelpItem helpText={t("clientScopesHelp")} fieldLabelId="scopes" />
      }
      fieldId="scopes"
    >
      <Controller
        name="scopes"
        defaultValue={[]}
        control={control}
        render={({ field }) => (
          <KeycloakSelect
            toggleId="scopes"
            variant={SelectVariant.typeaheadMulti}
            chipGroupProps={{
              numChips: 3,
              expandedText: t("hide"),
              collapsedText: t("showRemaining"),
            }}
            onToggle={(val) => setOpen(val)}
            isOpen={open}
            selections={field.value.map((o: Scope) => o.name)}
            onFilter={(value) => {
              setSearch(value);
              return renderScopes(scopes);
            }}
            onSelect={(selectedValue) => {
              const option =
                typeof selectedValue === "string"
                  ? selectedValue
                  : (selectedValue as Scope).name;
              const changedValue = field.value.find(
                (o: Scope) => o.name === option,
              )
                ? field.value.filter((item: Scope) => item.name !== option)
                : [...field.value, selectedValue];
              field.onChange(changedValue);
            }}
            onClear={() => {
              setSearch("");
              field.onChange([]);
            }}
            typeAheadAriaLabel={t("authorizationScopes")}
          >
            {renderScopes(scopes)}
          </KeycloakSelect>
        )}
      />
    </FormGroup>
  );
};
