import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, Controller } from "react-hook-form";
import { FormGroup, Button, Checkbox } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import {
  TableComposable,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from "@patternfly/react-table";

import { Row, ServiceRole } from "../../../components/role-mapping/RoleMapping";
import type { RequiredIdValue } from "./ClientScope";
import { HelpItem } from "ui-shared";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { AddRoleMappingModal } from "../../../components/role-mapping/AddRoleMappingModal";

export const Role = () => {
  const { t } = useTranslation("clients");
  const {
    control,
    getValues,
    setValue,
    formState: { errors },
  } = useFormContext<{
    roles?: RequiredIdValue[];
  }>();
  const values = getValues("roles");

  const [open, setOpen] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<Row[]>([]);

  const { adminClient } = useAdminClient();

  useFetch(
    async () => {
      if (values && values.length > 0) {
        const roles = await Promise.all(
          values.map((r) => adminClient.roles.findOneById({ id: r.id }))
        );
        return Promise.all(
          roles.map(async (role) => ({
            role: role!,
            client: role!.clientRole
              ? await adminClient.clients.findOne({
                  id: role?.containerId!,
                })
              : undefined,
          }))
        );
      }
      return Promise.resolve([]);
    },
    setSelectedRoles,
    []
  );

  return (
    <FormGroup
      label={t("roles")}
      labelIcon={
        <HelpItem
          helpText={t("clients-help:policyRoles")}
          fieldLabelId="clients:roles"
        />
      }
      fieldId="roles"
      helperTextInvalid={t("requiredRoles")}
      validated={errors.roles ? "error" : "default"}
      isRequired
    >
      <Controller
        name="roles"
        control={control}
        defaultValue={[]}
        rules={{
          validate: (value?: RequiredIdValue[]) =>
            value && value.filter((c) => c.id).length > 0,
        }}
        render={({ field }) => (
          <>
            {open && (
              <AddRoleMappingModal
                id="role"
                type="roles"
                onAssign={(rows) => {
                  field.onChange([
                    ...(field.value || []),
                    ...rows.map((row) => ({ id: row.role.id })),
                  ]);
                  setSelectedRoles([...selectedRoles, ...rows]);
                  setOpen(false);
                }}
                onClose={() => {
                  setOpen(false);
                }}
                isLDAPmapper
              />
            )}
            <Button
              data-testid="select-role-button"
              variant="secondary"
              onClick={() => {
                setOpen(true);
              }}
            >
              {t("addRoles")}
            </Button>
          </>
        )}
      />
      {selectedRoles.length > 0 && (
        <TableComposable variant="compact">
          <Thead>
            <Tr>
              <Th>{t("roles")}</Th>
              <Th>{t("required")}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {selectedRoles.map((row, index) => (
              <Tr key={row.role.id}>
                <Td>
                  <ServiceRole role={row.role} client={row.client} />
                </Td>
                <Td>
                  <Controller
                    name={`roles.${index}.required`}
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
                      setValue("roles", [
                        ...(values || []).filter((s) => s.id !== row.role.id),
                      ]);
                      setSelectedRoles([
                        ...selectedRoles.filter(
                          (s) => s.role.id !== row.role.id
                        ),
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
