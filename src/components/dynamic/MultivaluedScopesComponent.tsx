import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";

import { HelpItem } from "../help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { AddScopeDialog } from "../../clients/scopes/AddScopeDialog";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import { useParams } from "react-router";
import type { EditClientPolicyConditionParams } from "../../realm-settings/routes/EditCondition";

export const MultivaluedScopesComponent = ({
  defaultValue,
  name,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();
  const { conditionName } = useParams<EditClientPolicyConditionParams>();
  const adminClient = useAdminClient();
  const [open, setOpen] = useState(false);
  const [clientScopes, setClientScopes] = useState<ClientScopeRepresentation[]>(
    []
  );

  useFetch(
    () => adminClient.clientScopes.find(),
    (clientScopes) => {
      setClientScopes(clientScopes);
    },
    []
  );

  const toggleModal = () => {
    setOpen(!open);
  };

  return (
    <FormGroup
      label={t("realm-settings:clientScopesCondition")}
      id="expected-scopes"
      labelIcon={
        <HelpItem
          helpText={t("realm-settings-help:clientScopesConditionTooltip")}
          fieldLabelId="realm-settings:clientScopesCondition"
        />
      }
      fieldId={name!}
    >
      <Controller
        name={`config.scopes`}
        control={control}
        defaultValue={[defaultValue]}
        rules={{ required: true }}
        render={({ onChange, value }) => {
          return (
            <>
              {open && (
                <AddScopeDialog
                  clientScopes={clientScopes.filter(
                    (scope) => !value.includes(scope.name!)
                  )}
                  isClientScopesConditionType
                  open={open}
                  toggleDialog={() => setOpen(!open)}
                  onAdd={(scopes) => {
                    onChange([
                      ...value,
                      ...scopes
                        .map((scope) => scope.scope)
                        .map((item) => item.name!),
                    ]);
                  }}
                />
              )}
              {value.length === 0 && !conditionName && (
                <TextInput
                  type="text"
                  id="kc-scopes"
                  value={value}
                  data-testid="client-scope-input"
                  name="config.client-scopes"
                  isDisabled
                />
              )}
              <ChipGroup
                className="kc-client-scopes-chip-group"
                isClosable
                onClick={() => {
                  onChange([]);
                }}
              >
                {value.map((currentChip: string) => (
                  <Chip
                    key={currentChip}
                    onClick={() => {
                      onChange(
                        value.filter((item: string) => item !== currentChip)
                      );
                    }}
                  >
                    {currentChip}
                  </Chip>
                ))}
              </ChipGroup>
              <Button
                data-testid="select-scope-button"
                variant="secondary"
                onClick={() => {
                  toggleModal();
                }}
              >
                {t("common:select")}
              </Button>
            </>
          );
        }}
      />
    </FormGroup>
  );
};
