import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import { HelpItem, useFetch } from "@keycloak/keycloak-ui-shared";
import { Button, Checkbox, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import useLocaleSort, { mapByKey } from "../../../utils/useLocaleSort";
import { AddScopeDialog } from "../../scopes/AddScopeDialog";

export type RequiredIdValue = {
  id: string;
  required: boolean;
};

export const ClientScope = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { control, getValues, setValue } = useFormContext<{
    clientScopes: RequiredIdValue[];
  }>();

  const [open, setOpen] = useState(false);
  const [scopes, setScopes] = useState<ClientScopeRepresentation[]>([]);
  const [selectedScopes, setSelectedScopes] = useState<
    ClientScopeRepresentation[]
  >([]);

  const localeSort = useLocaleSort();

  useFetch(
    () => adminClient.clientScopes.find(),
    (scopes = []) => {
      const clientScopes = getValues("clientScopes") || [];
      setSelectedScopes(
        clientScopes.map((s) => scopes.find((c) => c.id === s.id)!),
      );
      setScopes(localeSort(scopes, mapByKey("name")));
    },
    [],
  );

  return (
    <FormGroup
      label={t("clientScopes")}
      labelIcon={
        <HelpItem
          helpText={t("clientsClientScopesHelp")}
          fieldLabelId="clientScopes"
        />
      }
      fieldId="clientScopes"
    >
      <Controller
        name="clientScopes"
        control={control}
        defaultValue={[]}
        render={({ field }) => (
          <>
            {open && (
              <AddScopeDialog
                clientScopes={scopes.filter(
                  (scope) =>
                    !field.value
                      .map((c: RequiredIdValue) => c.id)
                      .includes(scope.id!),
                )}
                isClientScopesConditionType
                open={open}
                toggleDialog={() => setOpen(!open)}
                onAdd={(scopes) => {
                  setSelectedScopes([
                    ...selectedScopes,
                    ...scopes.map((s) => s.scope),
                  ]);
                  field.onChange([
                    ...field.value,
                    ...scopes
                      .map((scope) => scope.scope)
                      .map((item) => ({ id: item.id!, required: false })),
                  ]);
                }}
              />
            )}
            <Button
              data-testid="select-scope-button"
              variant="secondary"
              onClick={() => {
                setOpen(true);
              }}
            >
              {t("addClientScopes")}
            </Button>
          </>
        )}
      />
      {selectedScopes.length > 0 && (
        <Table variant="compact">
          <Thead>
            <Tr>
              <Th>{t("clientScopeTitle")}</Th>
              <Th>{t("required")}</Th>
              <Th aria-hidden="true" />
            </Tr>
          </Thead>
          <Tbody>
            {selectedScopes.map((scope, index) => (
              <Tr key={scope.id}>
                <Td>{scope.name}</Td>
                <Td>
                  <Controller
                    name={`clientScopes.${index}.required`}
                    defaultValue={false}
                    control={control}
                    render={({ field }) => (
                      <Checkbox
                        id="required"
                        data-testid="standard"
                        name="required"
                        isChecked={field.value}
                        onChange={field.onChange}
                      />
                    )}
                  />
                </Td>
                <Td>
                  <Button
                    variant="link"
                    className="keycloak__client-authorization__policy-row-remove"
                    icon={<MinusCircleIcon />}
                    onClick={() => {
                      setValue("clientScopes", [
                        ...getValues("clientScopes").filter(
                          (s) => s.id !== scope.id,
                        ),
                      ]);
                      setSelectedScopes([
                        ...selectedScopes.filter((s) => s.id !== scope.id),
                      ]);
                    }}
                  />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
    </FormGroup>
  );
};
