import RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  DropdownProps,
  MenuToggle,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { cellWidth, TableText } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAccess } from "../../context/access/Access";
import { translationFormatter } from "../../utils/translationFormatter";
import useLocaleSort from "../../utils/useLocaleSort";
import useToggle from "../../utils/useToggle";
import { ResourcesKey, Row } from "./RoleMapping";
import { getAvailableRoles } from "./queries";
import { getAvailableClientRoles } from "./resource";

type AddRoleMappingModalProps = {
  id: string;
  type: ResourcesKey;
  filterType: FilterType;
  name?: string;
  isRadio?: boolean;
  onAssign: (rows: Row[]) => void;
  onClose: () => void;
  title?: string;
  actionLabel?: string;
  groupsResource?: any;
};

export type FilterType = "roles" | "clients" | "orgRoles";

const RoleDescription = ({ role }: { role: RoleRepresentation }) => {
  const { t } = useTranslation();
  return (
    <TableText wrapModifier="truncate">
      {translationFormatter(t)(role.description) as string}
    </TableText>
  );
};

type AddRoleButtonProps = Omit<
  DropdownProps,
  "children" | "toggle" | "isOpen" | "onOpenChange"
> & {
  label?: string;
  variant?: "default" | "plain" | "primary" | "plainText" | "secondary";
  isDisabled?: boolean;
  onFilerTypeChange: (type: FilterType) => void;
};

export const AddRoleButton = ({
  label,
  variant,
  isDisabled,
  onFilerTypeChange,
  ...rest
}: AddRoleButtonProps) => {
  const { t } = useTranslation();
  const [open, toggle] = useToggle();

  const { hasAccess } = useAccess();
  const canViewRealmRoles = hasAccess("view-realm") || hasAccess("query-users");
  const canViewOrganizationRoles = hasAccess("manage-organizations");

  return (
    <Dropdown
      onOpenChange={toggle}
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          onClick={toggle}
          variant={variant || "primary"}
          isDisabled={isDisabled}
          data-testid="add-role-mapping-button"
        >
          {t(label || "assignRole")}
        </MenuToggle>
      )}
      isOpen={open}
      {...rest}
    >
      <DropdownList>
        <DropdownItem
          data-testid="client-role"
          component="button"
          onClick={() => {
            onFilerTypeChange("clients");
          }}
        >
          {t("clientRoles")}
        </DropdownItem>
        {canViewRealmRoles && (
          <DropdownItem
            data-testid="roles-role"
            component="button"
            onClick={() => {
              onFilerTypeChange("roles");
            }}
          >
            {t("realmRoles")}
          </DropdownItem>
        )}
        {canViewOrganizationRoles && (
          <DropdownItem
            data-testid="org-roles"
            component="button"
            onClick={() => {
              onFilerTypeChange("orgRoles");
            }}
          >
            {t("organizationRoles")}
          </DropdownItem>
        )}
      </DropdownList>
    </Dropdown>
  );
};

export const AddRoleMappingModal = ({
  id,
  name,
  type,
  isRadio,
  filterType,
  onAssign,
  onClose,
  title,
  actionLabel,
  groupsResource,
}: AddRoleMappingModalProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);

  const localeSort = useLocaleSort();
  const compareRow = ({ role: { name } }: Row) => name?.toUpperCase();

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<Row[]> => {
    const params: Record<string, string | number> = {
      first: first!,
      max: max!,
    };

    if (search) {
      params.search = search;
    }

    const roles = await getAvailableRoles(
      adminClient,
      type,
      { ...params, id },
      groupsResource,
    );
    const sorted = localeSort(roles, compareRow);
    return sorted.map((row) => {
      return {
        role: row.role,
        id: row.role.id,
      };
    });
  };

  const clientRolesLoader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<Row[]> => {
    const roles = await getAvailableClientRoles(adminClient, {
      id,
      type,
      first: first || 0,
      max: max || 10,
      search,
    });

    return localeSort(
      roles.map((e) => ({
        client: { clientId: e.client, id: e.clientId },
        role: { id: e.id, name: e.role, description: e.description },
        id: e.id,
      })),
      ({ client: { clientId }, role: { name } }) => `${clientId}${name}`,
    );
  };

  const orgRolesLoader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<Row[]> => {
    if (type !== "groups" || !groupsResource) {
      return [];
    }

    const roles = await (
      groupsResource as any
    ).listAvailableOrganizationRoleMappings({
      id,
    });

    const filtered = search
      ? roles.filter(
          (role: RoleRepresentation) =>
            role.name?.toLowerCase().includes(search.toLowerCase()) ||
            role.description?.toLowerCase().includes(search.toLowerCase()),
        )
      : roles;

    return localeSort(
      filtered.map((role: RoleRepresentation) => ({
        role: { ...role, isOrgRole: true },
        id: role.id,
      })),
      compareRow,
    );
  };

  const columns = [
    {
      name: "role.name",
      displayKey: "name",
      transforms: [cellWidth(30)],
    },
    {
      name: "client.clientId",
      displayKey: "clientId",
    },
    {
      name: "role.description",
      displayKey: "description",
      cellRenderer: RoleDescription,
    },
  ];

  if (filterType === "roles" || filterType === "orgRoles") {
    columns.splice(1, 1);
  }

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        title ||
        t("assignRolesTo", {
          type:
            filterType === "roles"
              ? t("realm")
              : filterType === "orgRoles"
                ? t("organization")
                : t("client"),
          client: name,
        })
      }
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="assign"
          key="confirm"
          isDisabled={selectedRows.length === 0}
          variant="primary"
          onClick={() => {
            onAssign(selectedRows);
            onClose();
          }}
        >
          {actionLabel || t("assign")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        onSelect={(rows) => setSelectedRows([...rows])}
        searchPlaceholderKey={
          filterType === "roles" || filterType === "orgRoles"
            ? "searchByRoleName"
            : "search"
        }
        isPaginated={
          !(filterType === "roles" && type !== "roles") &&
          filterType !== "orgRoles"
        }
        canSelectAll
        isRadio={isRadio}
        loader={
          filterType === "roles"
            ? loader
            : filterType === "orgRoles"
              ? orgRolesLoader
              : clientRolesLoader
        }
        ariaLabelKey="associatedRolesText"
        columns={columns}
        emptyState={
          <ListEmptyState
            message={t("noRoles")}
            instructions={t("noRealmRolesToAssign")}
          />
        }
      />
    </Modal>
  );
};
