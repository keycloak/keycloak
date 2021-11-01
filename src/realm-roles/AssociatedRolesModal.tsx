import React, { useEffect, useState } from "react";
import { useHistory, useParams, useRouteMatch } from "react-router-dom";
import {
  AlertVariant,
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Label,
  Modal,
  ModalVariant,
  Spinner,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useFetch, useAdminClient } from "../context/auth/AdminClient";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { CaretDownIcon, FilterIcon } from "@patternfly/react-icons";
import { omit, sortBy } from "lodash";
import { RealmRoleParams, toRealmRole } from "./routes/RealmRole";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import {
  ClientRoleParams,
  ClientRoleRoute,
  toClientRole,
} from "./routes/ClientRole";

type Role = RoleRepresentation & {
  clientId?: string;
};

export type AssociatedRolesModalProps = {
  toggleDialog: () => void;
  onConfirm?: (newReps: RoleRepresentation[]) => void;
  omitComposites?: boolean;
  isRadio?: boolean;
  isMapperId?: boolean;
};

type FilterType = "roles" | "clients";

export const AssociatedRolesModal = ({
  toggleDialog,
  onConfirm,
  omitComposites,
  isRadio,
  isMapperId,
}: AssociatedRolesModalProps) => {
  const { t } = useTranslation("roles");
  const [name, setName] = useState("");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);
  const [compositeRoles, setCompositeRoles] = useState<RoleRepresentation[]>();

  const [isFilterDropdownOpen, setIsFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState<FilterType>("roles");
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const { id } = useParams<RealmRoleParams>();
  const clientRoleRouteMatch = useRouteMatch<ClientRoleParams>(
    ClientRoleRoute.path
  );

  const history = useHistory();

  const alphabetize = (rolesList: RoleRepresentation[]) => {
    return sortBy(rolesList, (role) => role.name?.toUpperCase());
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };

    const searchParam = search || "";

    if (searchParam) {
      params.search = searchParam;
    }

    return adminClient.roles.find(params);
  };

  const AliasRenderer = ({ id, name, clientId }: Role) => {
    return (
      <>
        {clientId && (
          <Label color="blue" key={`label-${id}`}>
            {clientId}
          </Label>
        )}{" "}
        {name}
      </>
    );
  };

  /* this is still pretty expensive querying all client and then all roles */
  const clientRolesLoader = async () => {
    const clients = await adminClient.clients.find();
    const clientRoles = await Promise.all(
      clients.map(async (client) => {
        const roles = await adminClient.clients.listRoles({ id: client.id! });

        return roles.map<Role>((role) => ({
          ...role,
          clientId: client.clientId,
        }));
      })
    );

    return alphabetize(clientRoles.flat());
  };

  const addComposites = async (composites: RoleRepresentation[]) => {
    const compositeArray = composites;

    const to = clientRoleRouteMatch
      ? toClientRole({ ...clientRoleRouteMatch.params, tab: "AssociateRoles" })
      : toRealmRole({
          realm,
          id,
          tab: "AssociatedRoles",
        });

    try {
      await adminClient.roles.createComposite(
        { roleId: id, realm },
        compositeArray
      );
      history.push(to);
      addAlert(t("addAssociatedRolesSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roles:addAssociatedRolesError", error);
    }
  };

  useEffect(() => {
    refresh();
  }, [filterType]);

  useFetch(
    async () => {
      const [role, compositeRoles] = await Promise.all([
        !isMapperId ? adminClient.roles.findOneById({ id }) : undefined,
        !omitComposites ? adminClient.roles.getCompositeRoles({ id }) : [],
      ]);

      return { role, compositeRoles };
    },
    ({ role, compositeRoles }) => {
      setName(role ? role.name! : t("createRole"));
      setCompositeRoles(compositeRoles);
    },
    []
  );

  const onFilterDropdownToggle = () => {
    setIsFilterDropdownOpen(!isFilterDropdownOpen);
  };

  const onFilterDropdownSelect = (filterType: string) => {
    if (filterType === "roles") {
      setFilterType("clients");
    }
    if (filterType === "clients") {
      setFilterType("roles");
    }
    setIsFilterDropdownOpen(!isFilterDropdownOpen);
  };

  if (!compositeRoles) {
    return (
      <div className="pf-u-text-align-center">
        <Spinner />
      </div>
    );
  }

  return (
    <Modal
      data-testid="addAssociatedRole"
      title={t("roles:associatedRolesModalTitle", { name })}
      isOpen
      onClose={toggleDialog}
      variant={ModalVariant.large}
      actions={[
        <Button
          key="add"
          data-testid="add-associated-roles-button"
          variant="primary"
          isDisabled={!selectedRows.length}
          onClick={() => {
            toggleDialog();
            if (onConfirm) {
              onConfirm(selectedRows);
            } else {
              addComposites(selectedRows);
            }
          }}
        >
          {t("common:add")}
        </Button>,
        <Button
          key="cancel"
          variant="link"
          onClick={() => {
            toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        key={key}
        loader={filterType === "roles" ? loader : clientRolesLoader}
        ariaLabelKey="roles:roleList"
        searchPlaceholderKey="roles:searchFor"
        isRadio={isRadio}
        isPaginated={filterType === "roles"}
        isRowDisabled={(r) => compositeRoles.some((o) => o.name === r.name)}
        searchTypeComponent={
          <Dropdown
            onSelect={() => onFilterDropdownSelect(filterType)}
            data-testid="filter-type-dropdown"
            toggle={
              <DropdownToggle
                id="toggle-id-9"
                onToggle={onFilterDropdownToggle}
                toggleIndicator={CaretDownIcon}
                icon={<FilterIcon />}
              >
                Filter by {filterType}
              </DropdownToggle>
            }
            isOpen={isFilterDropdownOpen}
            dropdownItems={[
              <DropdownItem
                data-testid="filter-type-dropdown-item"
                key="filter-type"
              >
                {filterType === "roles"
                  ? t("filterByClients")
                  : t("filterByRoles")}{" "}
              </DropdownItem>,
            ]}
          />
        }
        canSelectAll
        onSelect={(rows) => {
          setSelectedRows(rows.map((r) => omit(r, "clientId")));
        }}
        columns={[
          {
            name: "name",
            displayKey: "roles:roleName",
            cellRenderer: AliasRenderer,
          },
          {
            name: "description",
            displayKey: "common:description",
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t("noRoles")}
            instructions={t("noRolesInstructions")}
            primaryActionText={t("createRole")}
          />
        }
      />
    </Modal>
  );
};
