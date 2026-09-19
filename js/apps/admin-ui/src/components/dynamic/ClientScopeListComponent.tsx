import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "./components";

const IS_PARAMETERIZED_SCOPE = "is.parameterized.scope";

export const ClientScopeListComponent = ({
  name,
  label,
  helpText,
  options,
  isDisabled = false,
  convertToName,
}: ComponentProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { control } = useFormContext();
  const { id } = useParams<{ id: string }>();
  const [open, setOpen] = useState(false);
  const [scopeNames, setScopeNames] = useState<string[]>([]);

  const filterParameterized = options?.includes(IS_PARAMETERIZED_SCOPE);

  useFetch(
    async () => {
      if (!id) return [];

      const [defaultScopes, optionalScopes, allRealmScopes] = await Promise.all(
        [
          adminClient.clients.listDefaultClientScopes({ id }),
          adminClient.clients.listOptionalClientScopes({ id }),
          adminClient.clientScopes.find(),
        ],
      );

      const assignedIds = new Set(
        [...defaultScopes, ...optionalScopes].map((s) => s.id),
      );

      return allRealmScopes
        .filter((s) => s.id && assignedIds.has(s.id))
        .filter(
          (s) =>
            !filterParameterized ||
            s.attributes?.[IS_PARAMETERIZED_SCOPE] === "true",
        )
        .map((s) => s.name!)
        .filter(Boolean)
        .sort();
    },
    (names) => setScopeNames(names),
    [id],
  );

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={label!} />}
      fieldId={name!}
    >
      <Controller
        name={convertToName(name!)}
        control={control}
        defaultValue=""
        render={({ field }) => (
          <KeycloakSelect
            toggleId={name}
            isDisabled={isDisabled || scopeNames.length === 0}
            onToggle={(toggle) => setOpen(toggle)}
            onSelect={(value) => {
              field.onChange(value as string);
              setOpen(false);
            }}
            selections={field.value || undefined}
            variant={SelectVariant.single}
            aria-label={t(label!)}
            isOpen={open}
          >
            {scopeNames.map((scopeName) => (
              <SelectOption
                selected={scopeName === field.value}
                key={scopeName}
                value={scopeName}
              >
                {scopeName}
              </SelectOption>
            ))}
          </KeycloakSelect>
        )}
      />
    </FormGroup>
  );
};
