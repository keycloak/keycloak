import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, Controller } from "react-hook-form";
import { FormGroup, Button, Checkbox } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { HelpItem } from "ui-shared";
import { AddScopeDialog } from "../../scopes/AddScopeDialog";
import useLocaleSort, { mapByKey } from "../../../utils/useLocaleSort";

export type RequiredIdValue = {
  id: string;
  required: boolean;
};

export const ClientScope = () => {
  const { t } = useTranslation("clients");
  const {
    control,
    getValues,
    setValue,
    formState: { errors },
  } = useFormContext<{
    clientScopes: RequiredIdValue[];
  }>();

  const [open, setOpen] = useState(false);
  const [scopes, setScopes] = useState<ClientScopeRepresentation[]>([]);
  const [selectedScopes, setSelectedScopes] = useState<
    ClientScopeRepresentation[]
  >([]);

  const { adminClient } = useAdminClient();
  const localeSort = useLocaleSort();

  useFetch(
    () => adminClient.clientScopes.find(),
    (scopes) => {
      setSelectedScopes(
        getValues("clientScopes").map((s) => scopes.find((c) => c.id === s.id)!)
      );
      setScopes(localeSort(scopes, mapByKey("name")));
    },
    []
  );

  return (
    <FormGroup
      label={t("clientScopes")}
      labelIcon={
        <HelpItem
          helpText={t("clients-help:clientScopes")}
          fieldLabelId="clients:clientScopes"
        />
      }
      fieldId="clientScopes"
      helperTextInvalid={t("requiredClientScope")}
      validated={errors.clientScopes ? "error" : "default"}
      isRequired
    >
      <Controller
        name="clientScopes"
        control={control}
        defaultValue={[]}
        rules={{
          validate: (value: RequiredIdValue[]) =>
            value.filter((c) => c.id).length > 0,
        }}
        render={({ field }) => (
          <>
            {open && (
              <AddScopeDialog
                clientScopes={scopes.filter(
                  (scope) =>
                    !field.value
                      .map((c: RequiredIdValue) => c.id)
                      .includes(scope.id!)
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
        <TableComposable variant="compact">
          <Thead>
            <Tr>
              <Th>{t("clientScope")}</Th>
              <Th>{t("required")}</Th>
              <Th />
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
                          (s) => s.id !== scope.id
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
        </TableComposable>
      )}
    </FormGroup>
  );
};
