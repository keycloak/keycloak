import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  SelectControl,
  SelectControlOption,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useContext, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../admin-client";
import { GroupsResourceContext } from "../../context/group-resource/GroupResourceContext";
import type { ComponentProps } from "./components";

export const OrganizationRoleComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  required,
  isDisabled = false,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const organizationId = useContext(GroupsResourceContext)?.getOrgId();
  const [roles, setRoles] = useState<RoleRepresentation[]>([]);
  const [selectedRoles, setSelectedRoles] = useState<SelectControlOption[]>();
  const [search, setSearch] = useState("");
  const { control } = useFormContext();
  const fieldName = convertToName(name!);
  const value = useWatch({
    control,
    name: fieldName,
    defaultValue,
  });

  useFetch(
    () =>
      organizationId
        ? adminClient.organizations.listRoles({
            orgId: organizationId,
            max: 20,
            search: search || undefined,
          })
        : Promise.resolve([]),
    setRoles,
    [organizationId, search],
  );

  useFetch(
    () =>
      organizationId && value
        ? adminClient.organizations.findRole({
            orgId: organizationId,
            roleId: value,
          })
        : Promise.resolve(null),
    (role) =>
      setSelectedRoles(
        role?.id && role.name ? [{ key: role.id, value: role.name }] : [],
      ),
    [organizationId, value],
  );

  return (
    <SelectControl
      name={fieldName}
      label={t(label!)}
      labelIcon={t(helpText!)}
      controller={{
        defaultValue: defaultValue || "",
        rules: {
          required: {
            value: required || false,
            message: t("required"),
          },
        },
      }}
      onFilter={setSearch}
      variant="typeahead"
      isDisabled={isDisabled || !organizationId}
      selectedOptions={selectedRoles}
      options={roles
        .filter((role) => role.id && role.name)
        .map((role) => ({ key: role.id!, value: role.name! }))}
    />
  );
};
