import { useState } from "react";
import { Button, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import {
  FormErrorText,
  HelpItem,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { AddRoleMappingModal } from "../../components/role-mapping/AddRoleMappingModal";
import { Row, ServiceRole } from "../../components/role-mapping/RoleMapping";
import { PermissionsConfigurationTabsParams } from "../routes/PermissionsConfigurationTabs";
import { useParams } from "react-router-dom";

type RoleSelectorProps = {
  name: string;
};

export const RoleSelect = ({ name }: RoleSelectorProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const {
    getValues,
    setValue,
    formState: { errors },
  } = useFormContext<{ [key: string]: string[] }>();
  const values = getValues(name) || [];
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<Row[]>([]);
  const { tab } = useParams<PermissionsConfigurationTabsParams>();

  useFetch(
    async () => {
      if (values.length > 0) {
        const roles = await Promise.all(
          values.map((id) => adminClient.roles.findOneById({ id })),
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
      label={tab !== "evaluation" ? t("roles") : t("role")}
      labelIcon={
        <HelpItem
          helpText={
            tab !== "evaluation" ? t("policyRolesHelp") : t("selectRole")
          }
          fieldLabelId="roles"
        />
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
              ...values,
              ...rows
                .filter((row) => row.role.id !== undefined)
                .map((row) => row.role.id!),
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
        {tab !== "evaluation" ? t("addRoles") : t("selectRole")}
      </Button>
      {selectedRoles.length > 0 && (
        <Table variant="compact">
          <Thead>
            <Tr>
              <Th>{t("roles")}</Th>
              <Th aria-hidden="true" />
            </Tr>
          </Thead>
          <Tbody>
            {selectedRoles.map((row) => (
              <Tr key={row.role.id}>
                <Td>
                  <ServiceRole role={row.role} client={row.client} />
                </Td>
                <Td>
                  <Button
                    variant="link"
                    className="keycloak__client-authorization__policy-row-remove"
                    icon={<MinusCircleIcon />}
                    onClick={() => {
                      setValue(
                        name,
                        values.filter((id) => id !== row.role.id),
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
