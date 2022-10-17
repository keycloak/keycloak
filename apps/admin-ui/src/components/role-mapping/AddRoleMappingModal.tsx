import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Modal,
  ModalVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";

import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../../context/auth/AdminClient";
import useLocaleSort from "../../utils/useLocaleSort";
import { ResourcesKey, Row, ServiceRole } from "./RoleMapping";
import { getAvailableRoles } from "./queries";
import { getAvailableClientRoles } from "./resource";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";

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
  const { t } = useTranslation(type);
  const { adminClient } = useAdminClient();

  const [searchToggle, setSearchToggle] = useState(false);

  const [filterType, setFilterType] = useState<FilterType>("roles");
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const localeSort = useLocaleSort();
  const compareRow = ({ role: { name } }: Row) => name?.toUpperCase();

  const loader = async (
    first?: number,
    max?: number,
    search?: string
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
    search?: string
  ): Promise<Row[]> => {
    const roles = await getAvailableClientRoles({
      adminClient,
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
      compareRow
    );
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        isLDAPmapper
          ? t("common:assignRole")
          : t("common:assignRolesTo", { client: name })
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
          {t("common:assign")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        key={key}
        onSelect={(rows) => setSelectedRows([...rows])}
        searchPlaceholderKey="clients:searchByRoleName"
        isPaginated
        searchTypeComponent={
          <ToolbarItem>
            <Dropdown
              onSelect={() => {
                setFilterType(filterType === "roles" ? "clients" : "roles");
                setSearchToggle(false);
                refresh();
              }}
              data-testid="filter-type-dropdown"
              toggle={
                <DropdownToggle
                  onToggle={setSearchToggle}
                  icon={<FilterIcon />}
                >
                  {filterType === "roles"
                    ? t("common:filterByRoles")
                    : t("common:filterByClients")}
                </DropdownToggle>
              }
              isOpen={searchToggle}
              dropdownItems={[
                <DropdownItem key="filter-type" data-testid={filterType}>
                  {filterType === "roles"
                    ? t("common:filterByClients")
                    : t("common:filterByRoles")}
                </DropdownItem>,
              ]}
            />
          </ToolbarItem>
        }
        canSelectAll
        isRadio={isRadio}
        loader={filterType === "roles" ? loader : clientRolesLoader}
        ariaLabelKey="clients:roles"
        columns={[
          {
            name: "name",
            cellRenderer: ServiceRole,
          },
          {
            name: "role.description",
            displayKey: "common:description",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noRoles")}
            instructions={t("common:noRealmRolesToAssign")}
            secondaryActions={[
              {
                text: t("common:filterByClients"),
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
