import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Label,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useFetch, useAdminClient } from "../context/auth/AdminClient";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { CaretDownIcon, FilterIcon } from "@patternfly/react-icons";
import _ from "lodash";
import type { RealmRoleParams } from "./routes/RealmRole";

type Role = RoleRepresentation & {
  clientId?: string;
};

export type AssociatedRolesModalProps = {
  open: boolean;
  toggleDialog: () => void;
  onConfirm: (newReps: RoleRepresentation[]) => void;
  existingCompositeRoles?: RoleRepresentation[];
  allRoles?: RoleRepresentation[];
  omitComposites?: boolean;
  isRadio?: boolean;
  isMapperId?: boolean;
};

export const AssociatedRolesModal = (props: AssociatedRolesModalProps) => {
  const { t } = useTranslation("roles");
  const [name, setName] = useState("");
  const adminClient = useAdminClient();
  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);

  const [isFilterDropdownOpen, setIsFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState("roles");
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const { id } = useParams<RealmRoleParams>();

  const alphabetize = (rolesList: RoleRepresentation[]) => {
    return _.sortBy(rolesList, (role) => role.name?.toUpperCase());
  };

  const loader = async () => {
    const roles = await adminClient.roles.find();

    if (!props.omitComposites) {
      const existingAdditionalRoles = await adminClient.roles.getCompositeRoles(
        {
          id,
        }
      );
      const allRoles = [...roles, ...existingAdditionalRoles];

      const filterDupes: Role[] = allRoles.filter(
        (thing, index, self) =>
          index === self.findIndex((t) => t.name === thing.name)
      );

      const clients = await adminClient.clients.find();
      filterDupes
        .filter((role) => role.clientRole)
        .map(
          (role) =>
            (role.clientId = clients.find(
              (client) => client.id === role.containerId
            )!.clientId!)
        );

      return alphabetize(filterDupes).filter((role: RoleRepresentation) => {
        return (
          props.existingCompositeRoles?.find(
            (existing: RoleRepresentation) => existing.name === role.name
          ) === undefined && role.name !== name
        );
      });
    }
    return alphabetize(roles);
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

  const clientRolesLoader = async () => {
    const clients = await adminClient.clients.find();

    const clientIdArray = clients.map((client) => client.id);

    let rolesList: Role[] = [];
    for (const id of clientIdArray) {
      const clientRolesList = await adminClient.clients.listRoles({
        id: id as string,
      });
      rolesList = [...rolesList, ...clientRolesList];
    }

    rolesList
      .filter((role) => role.clientRole)
      .map(
        (role) =>
          (role.clientId = clients.find(
            (client) => client.id === role.containerId
          )!.clientId!)
      );

    if (!props.omitComposites) {
      const existingAdditionalRoles = await adminClient.roles.getCompositeRoles(
        {
          id,
        }
      );

      return alphabetize(rolesList).filter((role: RoleRepresentation) => {
        return (
          existingAdditionalRoles.find(
            (existing: RoleRepresentation) => existing.name === role.name
          ) === undefined && role.name !== name
        );
      });
    }

    return alphabetize(rolesList);
  };

  useEffect(() => {
    refresh();
  }, [filterType]);

  useFetch(
    () =>
      !props.isMapperId
        ? adminClient.roles.findOneById({ id })
        : Promise.resolve(null),
    (role) => setName(role ? role.name! : t("createRole")),
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

  return (
    <Modal
      data-testid="addAssociatedRole"
      title={t("roles:associatedRolesModalTitle", { name })}
      isOpen={props.open}
      onClose={props.toggleDialog}
      variant={ModalVariant.large}
      actions={[
        <Button
          key="add"
          data-testid="add-associated-roles-button"
          variant="primary"
          isDisabled={!selectedRows.length}
          onClick={() => {
            props.toggleDialog();
            props.onConfirm(selectedRows);
          }}
        >
          {t("common:add")}
        </Button>,
        <Button
          key="cancel"
          variant="link"
          onClick={() => {
            props.toggleDialog();
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
        isRadio={props.isRadio}
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
          setSelectedRows([...rows]);
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
