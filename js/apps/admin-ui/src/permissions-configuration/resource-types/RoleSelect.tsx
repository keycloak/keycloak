import { useState } from "react";
import { Button, Checkbox, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import {
  FormErrorText,
  HelpItem,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { AddRoleMappingModal } from "../../components/role-mapping/AddRoleMappingModal";
import { ServiceRole } from "../../components/role-mapping/RoleMapping";

type RoleSelectorProps = {
  name: string;
};

export const RoleSelect = ({ name }: RoleSelectorProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const {
    control,
    getValues,
    setValue,
    formState: { errors },
  } = useFormContext<{ [key: string]: { id: string; required?: boolean }[] }>();

  const values = getValues(name);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<
    { role: any; client?: any }[]
  >([]);

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
              ? await adminClient.clients.findOne({ id: role?.containerId! })
              : undefined,
          })),
        );
      }
      return [];
    },
    setSelectedRoles,
    [],
  );

  return (
    <FormGroup
      label={t("roles")}
      labelIcon={
        <HelpItem helpText={t("policyRolesHelp")} fieldLabelId="roles" />
      }
      fieldId={name}
      isRequired
    >
      {isModalOpen && (
        <AddRoleMappingModal
          id="role"
          type="roles"
          onAssign={(rows) => {
            setValue(name, [
              ...(values || []),
              ...rows
                .filter((row) => row.role.id !== undefined)
                .map((row) => ({ id: row.role.id! })),
            ]);

            setSelectedRoles([...selectedRoles, ...rows]);
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
          isLDAPmapper
        />
      )}

      <Button
        data-testid="select-role-button"
        variant="secondary"
        onClick={() => setIsModalOpen(true)}
      >
        {t("addRoles")}
      </Button>

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
                    name={`${name}.${index}.required`}
                    control={control}
                    defaultValue={false}
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
                      setValue(
                        name,
                        (values || []).filter((s) => s.id !== row.role.id),
                      );
                      setSelectedRoles(
                        selectedRoles.filter((s) => s.role.id !== row.role.id),
                      );
                    }}
                  />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}

      {errors[name] && <FormErrorText message={t("requiredRoles")} />}
    </FormGroup>
  );
};
