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
import { asyncStateFetch, useAdminClient } from "../context/auth/AdminClient";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { CaretDownIcon, FilterIcon } from "@patternfly/react-icons";
import _ from "lodash";
import { useErrorHandler } from "react-error-boundary";

type Role = RoleRepresentation & {
  clientId?: string;
};

export type AssociatedRolesModalProps = {
  open: boolean;
  toggleDialog: () => void;
  onConfirm: (newReps: RoleRepresentation[]) => void;
  existingCompositeRoles: RoleRepresentation[];
};

export const AssociatedRolesModal = (props: AssociatedRolesModalProps) => {
  const { t } = useTranslation("roles");
  const [name, setName] = useState("");
  const adminClient = useAdminClient();
  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);
  const errorHandler = useErrorHandler();

  const [isFilterDropdownOpen, setIsFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState("roles");
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const { id } = useParams<{ id: string }>();

  const alphabetize = (rolesList: RoleRepresentation[]) => {
    return _.sortBy(rolesList, (role) => role.name?.toUpperCase());
  };

  const loader = async () => {
    const roles = await adminClient.roles.find();
    const existingAdditionalRoles = await adminClient.roles.getCompositeRoles({
      id,
    });
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
        props.existingCompositeRoles.find(
          (existing: RoleRepresentation) => existing.name === role.name
        ) === undefined && role.name !== name
      );
    });
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
    const existingAdditionalRoles = await adminClient.roles.getCompositeRoles({
      id,
    });

    rolesList
      .filter((role) => role.clientRole)
      .map(
        (role) =>
          (role.clientId = clients.find(
            (client) => client.id === role.containerId
          )!.clientId!)
      );

    return alphabetize(rolesList).filter((role: RoleRepresentation) => {
      return (
        existingAdditionalRoles.find(
          (existing: RoleRepresentation) => existing.name === role.name
        ) === undefined && role.name !== name
      );
    });
  };

  useEffect(() => {
    refresh();
  }, [filterType]);

  useEffect(() => {
    if (id) {
      return asyncStateFetch(
        () => adminClient.roles.findOneById({ id }),
        (fetchedRole) => setName(fetchedRole.name!),
        errorHandler
      );
    } else {
      setName(t("createRole"));
    }
  }, []);

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
      title={t("roles:associatedRolesModalTitle", { name })}
      isOpen={props.open}
      onClose={props.toggleDialog}
      variant={ModalVariant.large}
      actions={[
        <Button
          key="add"
          data-testid="add-associated-roles-button"
          variant="primary"
          isDisabled={!selectedRows?.length}
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
