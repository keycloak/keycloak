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
import type { Groups } from "@keycloak/keycloak-admin-client/lib/resources/groups";
import { Row } from "./RoleMapping";
import {
  getAvailableClientRoles,
  getAvailableRealmRoles,
  RoleMappingType,
} from "./queries";

type AddRoleMappingModalProps = {
  id: string;
  type: RoleMappingType;
  filterType: FilterType;
  name?: string;
  isRadio?: boolean;
  onAssign: (rows: Row[]) => void;
  onClose: () => void;
  title?: string;
  actionLabel?: string;
  groupsResource?: Groups;
};

export type FilterType = "roles" | "clients";

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
  type?: RoleMappingType;
  onFilerTypeChange: (type: FilterType) => void;
};

const useCanViewRealmRoles = (type?: RoleMappingType) => {
  const { hasAccess } = useAccess();
  switch (type) {
    case "users":
    case "groups":
      return hasAccess("view-realm") || hasAccess("query-users");
    case "clients":
    case "clientScopes":
      return hasAccess("view-realm") || hasAccess("query-clients");
    default:
      return hasAccess("view-realm");
  }
};

export const AddRoleButton = ({
  label,
  variant,
  isDisabled,
  type,
  onFilerTypeChange,
  ...rest
}: AddRoleButtonProps) => {
  const { t } = useTranslation();
  const [open, toggle] = useToggle();
  const canViewRealmRoles = useCanViewRealmRoles(type);

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

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<Row[]> => {
    const params = { id, first, max, ...(search ? { search } : {}) };
    const roles =
      filterType === "roles"
        ? await getAvailableRealmRoles(
            adminClient,
            type,
            params,
            groupsResource,
          )
        : await getAvailableClientRoles(
            adminClient,
            type,
            params,
            groupsResource,
          );

    return localeSort(roles, ({ client, role }) =>
      `${client?.clientId ?? ""}${role.name}`.toUpperCase(),
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

  if (filterType === "roles") {
    columns.splice(1, 1);
  }

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        title ||
        t("assignRolesTo", {
          type: filterType === "roles" ? t("realm") : t("client"),
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
          filterType === "roles" ? "searchByRoleName" : "search"
        }
        isPaginated={!(filterType === "roles" && type !== "realms")}
        canSelectAll
        isRadio={isRadio}
        loader={loader}
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
