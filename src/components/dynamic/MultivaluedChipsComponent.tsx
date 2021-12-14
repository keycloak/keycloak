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
import { GroupPickerDialog } from "../group/GroupPickerDialog";

export const MultivaluedChipsComponent = ({
  defaultValue,
  name,
  label,
  helpText,
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
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <Controller
        name={`config.${name}`}
        control={control}
        defaultValue={[defaultValue]}
        rules={{ required: true }}
        render={({ onChange, value }) => (
          <>
            {open && name === "scopes" && (
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
            {open && name === "groups" && (
              <GroupPickerDialog
                type="selectMany"
                text={{
                  title: "users:selectGroups",
                  ok: "users:join",
                }}
                onConfirm={(groups) => {
                  onChange([...value, ...groups.map((group) => group.name)]);
                  setOpen(false);
                }}
                onClose={() => {
                  setOpen(false);
                }}
                filterGroups={value}
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
        )}
      />
    </FormGroup>
  );
};
