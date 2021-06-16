import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import _ from "lodash";
import {
  Button,
  ButtonVariant,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCheck,
  Divider,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  SelectOptionObject,
  Stack,
  StackItem,
  Text,
  TextContent,
} from "@patternfly/react-core";

import { JsonFileUpload } from "../components/json-file-upload/JsonFileUpload";

export type PartialImportProps = {
  open: boolean;
  toggleDialog: () => void;
};

// An imported JSON file can either be an array of realm objects
// or a single realm object.
type ImportedMultiRealm = [ImportedRealm?] | ImportedRealm;

// Realms in imported json can have a lot more properties,
// but these are the ones we care about.
type ImportedRealm = {
  id?: string;
  realm?: string;
  users?: [];
  clients?: [];
  groups?: [];
  identityProviders?: [];
  roles?: {
    realm?: [];
    client?: { [index: string]: [] };
  };
};

type NonRoleResource = "users" | "clients" | "groups" | "identityProviders";
type RoleResource = "realmRoles" | "clientRoles";
type Resource = NonRoleResource | RoleResource;

type CollisionOption = "FAIL" | "SKIP" | "OVERWRITE";

type ResourceChecked = { [k in Resource]: boolean };

export const PartialImportDialog = (props: PartialImportProps) => {
  const tRealm = useTranslation("realm-settings").t;
  const { t } = useTranslation("partial-import");

  const [isFileSelected, setIsFileSelected] = useState(false);
  const [isMultiRealm, setIsMultiRealm] = useState(false);
  const [importedFile, setImportedFile] = useState<ImportedMultiRealm>([]);
  const [isRealmSelectOpen, setIsRealmSelectOpen] = useState(false);
  const [isCollisionSelectOpen, setIsCollisionSelectOpen] = useState(false);
  const [collisionOption, setCollisionOption] = useState<CollisionOption>(
    "FAIL"
  );
  const [targetRealm, setTargetRealm] = useState<ImportedRealm>({});

  const allResourcesUnChecked: Readonly<ResourceChecked> = {
    users: false,
    clients: false,
    groups: false,
    identityProviders: false,
    realmRoles: false,
    clientRoles: false,
  };

  const [resourcesToImport, setResourcesToImport] = useState<ResourceChecked>(
    _.cloneDeep(allResourcesUnChecked)
  );

  const [isAnyResourceChecked, setIsAnyResourceChecked] = useState(false);

  const resetResourcesToImport = () => {
    setResourcesToImport(_.cloneDeep(allResourcesUnChecked));
    setIsAnyResourceChecked(false);
  };

  const resetInputState = () => {
    setIsMultiRealm(false);
    setImportedFile([]);
    setTargetRealm({});
    setCollisionOption("FAIL");
    resetResourcesToImport();
  };

  // when dialog opens or closes, clear state
  useEffect(() => {
    setIsFileSelected(false);
    resetInputState();
  }, [props.open]);

  const handleFileChange = (value: object) => {
    setIsFileSelected(!!value);
    resetInputState();

    setImportedFile(value);

    if (value instanceof Array && value.length > 0) {
      setIsMultiRealm(value.length > 1);
      setTargetRealm(value[0] || {});
    } else {
      setIsMultiRealm(false);
      setTargetRealm((value as ImportedRealm) || {});
    }
  };

  const handleRealmSelect = (
    event: React.ChangeEvent<Element> | React.MouseEvent<Element, MouseEvent>,
    realm: string | SelectOptionObject
  ) => {
    setTargetRealm(realm as ImportedRealm);
    setIsRealmSelectOpen(false);
    resetResourcesToImport();
  };

  const handleResourceCheckBox = (
    checked: boolean,
    event: React.FormEvent<HTMLInputElement>
  ) => {
    const resource: Resource = event.currentTarget.name as Resource;
    const copyOfResourcesToImport = _.cloneDeep(resourcesToImport);
    copyOfResourcesToImport[resource] = checked;
    setResourcesToImport(copyOfResourcesToImport);
    setIsAnyResourceChecked(resourcesChecked(copyOfResourcesToImport));
  };

  const realmSelectOptions = () => {
    if (!isMultiRealm) return [];

    const mapper = (realm: ImportedRealm) => {
      return (
        <SelectOption
          key={realm.id}
          value={realm}
          data-testid={realm.id + "-select-option"}
        >
          {realm.realm || realm.id}
        </SelectOption>
      );
    };

    return (importedFile as [ImportedRealm]).map(mapper);
  };

  const handleCollisionSelect = (
    event: React.ChangeEvent<Element> | React.MouseEvent<Element, MouseEvent>,
    option: string | SelectOptionObject
  ) => {
    setCollisionOption(option as CollisionOption);
    setIsCollisionSelectOpen(false);
  };

  const collisionOptions = () => {
    return [
      <SelectOption key="fail" value="FAIL">
        {t("FAIL")}
      </SelectOption>,
      <SelectOption key="skip" value="SKIP">
        {t("SKIP")}
      </SelectOption>,
      <SelectOption key="overwrite" value="OVERWRITE">
        {t("OVERWRITE")}
      </SelectOption>,
    ];
  };

  const targetHasResources = () => {
    return (
      targetHasResource("users") ||
      targetHasResource("groups") ||
      targetHasResource("clients") ||
      targetHasResource("identityProviders") ||
      targetHasRealmRoles() ||
      targetHasClientRoles()
    );
  };

  const targetHasResource = (resource: NonRoleResource) => {
    return (
      targetRealm &&
      targetRealm[resource] instanceof Array &&
      targetRealm[resource]!.length > 0
    );
  };

  const targetHasRoles = () => {
    return (
      targetRealm && Object.prototype.hasOwnProperty.call(targetRealm, "roles")
    );
  };

  const targetHasRealmRoles = () => {
    return (
      targetHasRoles() &&
      targetRealm.roles!.realm instanceof Array &&
      targetRealm.roles!.realm.length > 0
    );
  };

  const targetHasClientRoles = () => {
    return (
      targetHasRoles() &&
      Object.prototype.hasOwnProperty.call(targetRealm.roles, "client") &&
      Object.keys(targetRealm.roles!.client!).length > 0
    );
  };

  const itemCount = (resource: Resource) => {
    if (!isFileSelected) return 0;

    if (targetHasRealmRoles() && resource === "realmRoles")
      return targetRealm.roles!.realm!.length;

    if (targetHasClientRoles() && resource == "clientRoles")
      return clientRolesCount(targetRealm.roles!.client!);

    if (!targetRealm[resource as NonRoleResource]) return 0;

    return targetRealm[resource as NonRoleResource]!.length;
  };

  const clientRolesCount = (clientRoles: { [index: string]: [] }) => {
    let total = 0;
    for (const clientName in clientRoles) {
      total += clientRoles[clientName].length;
    }
    return total;
  };

  const resourcesChecked = (resources: ResourceChecked) => {
    let resource: Resource;
    for (resource in resources) {
      if (resources[resource]) return true;
    }

    return false;
  };

  const resourceDataListItem = (
    resource: Resource,
    resourceDisplayName: string
  ) => {
    return (
      <DataListItem aria-labelledby={`${resource}-list-item`}>
        <DataListItemRow>
          <DataListCheck
            aria-labelledby={`${resource}-checkbox`}
            name={resource}
            isChecked={resourcesToImport[resource]}
            onChange={handleResourceCheckBox}
            data-testid={resource + "-checkbox"}
          />
          <DataListItemCells
            dataListCells={[
              <DataListCell key={resource}>
                <span data-testid={resource + "-count"}>
                  {itemCount(resource)} {resourceDisplayName}
                </span>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={tRealm("partialImport")}
      isOpen={props.open}
      onClose={props.toggleDialog}
      actions={[
        <Button
          id="modal-import"
          data-testid="import-button"
          key="import"
          isDisabled={!isAnyResourceChecked}
          onClick={() => {
            props.toggleDialog();
          }}
        >
          {t("import")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel-button"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            props.toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Stack hasGutter>
        <StackItem>
          <TextContent>
            <Text>{t("partialImportHeaderText")}</Text>
          </TextContent>
        </StackItem>
        <StackItem>
          <JsonFileUpload
            id="partial-import-file"
            onChange={handleFileChange}
          />
        </StackItem>

        {isFileSelected && targetHasResources() && (
          <>
            <StackItem>
              <Divider />
            </StackItem>
            {isMultiRealm && (
              <StackItem>
                <Text>{t("selectRealm")}:</Text>
                <Select
                  toggleId="realm-selector"
                  isOpen={isRealmSelectOpen}
                  onToggle={() => setIsRealmSelectOpen(!isRealmSelectOpen)}
                  onSelect={handleRealmSelect}
                  placeholderText={targetRealm.realm || targetRealm.id}
                >
                  {realmSelectOptions()}
                </Select>
              </StackItem>
            )}
            <StackItem>
              <Text>{t("chooseResources")}:</Text>
              <DataList aria-label="Resources to import" isCompact>
                {targetHasResource("users") &&
                  resourceDataListItem("users", "users")}
                {targetHasResource("groups") &&
                  resourceDataListItem("groups", "groups")}
                {targetHasResource("clients") &&
                  resourceDataListItem("clients", "clients")}
                {targetHasResource("identityProviders") &&
                  resourceDataListItem(
                    "identityProviders",
                    "identity providers"
                  )}
                {targetHasRealmRoles() &&
                  resourceDataListItem("realmRoles", "realm roles")}
                {targetHasClientRoles() &&
                  resourceDataListItem("clientRoles", "client roles")}
              </DataList>
            </StackItem>
            <StackItem>
              <Text>{t("selectIfResourceExists")}:</Text>
              <Select
                isOpen={isCollisionSelectOpen}
                direction="up"
                onToggle={() => {
                  setIsCollisionSelectOpen(!isCollisionSelectOpen);
                }}
                onSelect={handleCollisionSelect}
                placeholderText={t(collisionOption)}
              >
                {collisionOptions()}
              </Select>
            </StackItem>
          </>
        )}
      </Stack>
    </Modal>
  );
};
