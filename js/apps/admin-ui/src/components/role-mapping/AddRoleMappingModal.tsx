import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Modal,
  ModalVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAccess } from "../../context/access/Access";
import useLocaleSort from "../../utils/useLocaleSort";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { ResourcesKey, Row, ServiceRole } from "./RoleMapping";
import { getAvailableRoles } from "./queries";
import { getAvailableClientRoles } from "./resource";

type AddRoleMappingModalProps = {
  id: string;
  type: ResourcesKey;
  name?: string;
  isRadio?: boolean;
  onAssign: (rows: Row[]) => void;
  onClose: () => void;
  isLDAPmapper?: boolean;
};

type FilterType = "roles" | "clients";

export const AddRoleMappingModal = ({
  id,
  name,
  type,
  isRadio = false,
  isLDAPmapper,
  onAssign,
  onClose,
}: AddRoleMappingModalProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { hasAccess } = useAccess();
  const canViewRealmRoles = hasAccess("view-realm") || hasAccess("query-users");

  const [searchToggle, setSearchToggle] = useState(false);

  const [filterType, setFilterType] = useState<FilterType>("clients");
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

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

    const roles = await getAvailableRoles(adminClient, type, { ...params, id });
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

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        isLDAPmapper ? t("assignRole") : t("assignRolesTo", { client: name })
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
          {t("assign")}
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
        key={key}
        onSelect={(rows) => setSelectedRows([...rows])}
        searchPlaceholderKey="searchByRoleName"
        isPaginated={!(filterType === "roles" && type !== "roles")}
        searchTypeComponent={
          canViewRealmRoles && (
            <ToolbarItem>
              <Dropdown
                onOpenChange={(isOpen) => setSearchToggle(isOpen)}
                onSelect={() => {
                  setFilterType(filterType === "roles" ? "clients" : "roles");
                  setSearchToggle(false);
                  refresh();
                }}
                toggle={(ref) => (
                  <MenuToggle
                    data-testid="filter-type-dropdown"
                    ref={ref}
                    onClick={() => setSearchToggle(!searchToggle)}
                    icon={<FilterIcon />}
                  >
                    {filterType === "roles"
                      ? t("filterByRoles")
                      : t("filterByClients")}
                  </MenuToggle>
                )}
                isOpen={searchToggle}
              >
                <DropdownList>
                  <DropdownItem key="filter-type" data-testid={filterType}>
                    {filterType === "roles"
                      ? t("filterByClients")
                      : t("filterByRoles")}
                  </DropdownItem>
                </DropdownList>
              </Dropdown>
            </ToolbarItem>
          )
        }
        canSelectAll
        isRadio={isRadio}
        loader={filterType === "roles" ? loader : clientRolesLoader}
        ariaLabelKey="roles"
        columns={[
          {
            name: "name",
            cellRenderer: ServiceRole,
          },
          {
            name: "role.description",
            displayKey: "description",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noRoles")}
            instructions={t("noRealmRolesToAssign")}
            secondaryActions={[
              {
                text: t("filterByClients"),
                onClick: () => {
                  setFilterType("clients");
                  refresh();
                },
              },
            ]}
          />
        }
      />
    </Modal>
  );
};
