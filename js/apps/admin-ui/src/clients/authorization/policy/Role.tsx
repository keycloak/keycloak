import { HelpItem, useFetch } from "@keycloak/keycloak-ui-shared";
import { Button, Checkbox, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { DefaultSwitchControl } from "../../../components/SwitchControl";
import {
  AddRoleButton,
  AddRoleMappingModal,
  FilterType,
} from "../../../components/role-mapping/AddRoleMappingModal";
import { Row, ServiceRole } from "../../../components/role-mapping/RoleMapping";
import type { RequiredIdValue } from "./ClientScope";

export const Role = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { control, getValues, setValue } = useFormContext<{
    roles?: RequiredIdValue[];
    fetchRoles?: boolean;
  }>();
  const values = getValues("roles");

  const [open, setOpen] = useState(false);
  const [filterType, setFilterType] = useState<FilterType>("clients");

  const [selectedRoles, setSelectedRoles] = useState<Row[]>([]);

  useFetch(
    async () => {
      if (values && values.length > 0) {
        const roles = await Promise.all(
          values.map((r) => adminClient.roles.findOneById({ id: r.id })),
        );
        return Promise.all(
          roles.map(async (role) => ({
            role: role!,
            client: role!.clientRole
              ? await adminClient.clients.findOne({
                  id: role?.containerId!,
                })
              : undefined,
          })),
        );
      }
      return Promise.resolve([]);
    },
    setSelectedRoles,
    [],
  );

  return (
    <>
      <FormGroup
        label={t("roles")}
        labelIcon={
          <HelpItem helpText={t("policyRolesHelp")} fieldLabelId="roles" />
        }
        fieldId="roles"
      >
        <Controller
          name="roles"
          control={control}
          defaultValue={[]}
          render={({ field }) => (
            <>
              {open && (
                <AddRoleMappingModal
                  id="role"
                  type="roles"
                  title={t("assignRole")}
                  filterType={filterType}
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
                />
              )}
              <AddRoleButton
                data-testid="select-role-button"
                variant="secondary"
                onFilerTypeChange={(type) => {
                  setFilterType(type);
                  setOpen(true);
                }}
              />
            </>
          )}
        />
        {selectedRoles.length > 0 && (
          <Table variant="compact">
            <Thead>
              <Tr>
                <Th>{t("roles")}</Th>
                <Th>{t("required")}</Th>
                <Th aria-hidden="true" />
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
                            (s) => s.role.id !== row.role.id,
                          ),
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
      <DefaultSwitchControl
        name="fetchRoles"
        label={t("fetchRoles")}
        labelIcon={t("fetchRolesHelp")}
      />
    </>
  );
};
