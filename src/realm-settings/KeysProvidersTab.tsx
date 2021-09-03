import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  DataList,
  DataListAction,
  DataListCell,
  DataListControl,
  DataListDragButton,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  DropdownToggle,
  InputGroup,
  KebabToggle,
  PageSection,
  TextInput,
  Toolbar,
  ToolbarGroup,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";

import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

import "./RealmSettingsSection.css";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { Link, useRouteMatch } from "react-router-dom";
import { AESGeneratedModal } from "./key-providers/aes-generated/AESGeneratedModal";
import { JavaKeystoreModal } from "./key-providers/java-keystore/JavaKeystoreModal";
import { HMACGeneratedModal } from "./key-providers/hmac-generated/HMACGeneratedModal";
import { ECDSAGeneratedModal } from "./key-providers/ecdsa-generated/ECDSAGeneratedModal";
import { RSAModal } from "./RSAModal";
import { RSAGeneratedModal } from "./key-providers/rsa-generated/RSAGeneratedModal";
import { KEY_PROVIDER_TYPE } from "../util";

type ComponentData = KeyMetadataRepresentation & {
  id?: string;
  providerDescription?: string;
  name?: string;
  toggleHidden?: boolean;
  config?: any;
  parentId?: string;
};

type KeysTabInnerProps = {
  components: ComponentData[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
  refresh: () => void;
};

export const KeysTabInner = ({ components, refresh }: KeysTabInnerProps) => {
  const { t } = useTranslation("realm-settings");
  const { addAlert, addError } = useAlerts();
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { url } = useRouteMatch();

  const [id, setId] = useState("");

  const [searchVal, setSearchVal] = useState("");
  const [filteredComponents, setFilteredComponents] = useState<ComponentData[]>(
    []
  );
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const serverInfo = useServerInfo();
  const providerTypes = (
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? []
  ).map((item) => item.id);

  const [itemOrder, setItemOrder] = useState<string[]>([]);
  const [providerDropdownOpen, setProviderDropdownOpen] = useState(false);

  const [defaultConsoleDisplayName, setDefaultConsoleDisplayName] =
    useState("");

  const [selectedComponent, setSelectedComponent] =
    useState<ComponentRepresentation>();

  const [liveText, setLiveText] = useState("");

  useEffect(() => {
    const itemIds = components.map((component) => component.id!);
    setItemOrder(["data", ...itemIds]);
  }, [components, searchVal]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "realm-settings:deleteProviderTitle",
    messageKey: t("deleteProviderConfirm") + selectedComponent?.name + "?",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          id: selectedComponent!.id!,
          realm: realm,
        });

        refresh();

        addAlert(t("deleteProviderSuccess"), AlertVariant.success);
      } catch (error) {
        addError("realm-settings:deleteProviderError", error);
      }
    },
  });

  const onDragStart = async (id: string) => {
    setLiveText(t("common:onDragStart", { item: id }));
    setId(id);
  };

  const onDragMove = () => {
    setLiveText(t("common:onDragMove", { item: id }));
  };

  const onDragCancel = () => {
    setLiveText(t("common:onDragCancel"));
  };

  const onDragFinish = async (itemOrder: string[]) => {
    setItemOrder(itemOrder);
    setLiveText(t("common:onDragFinish"));
    const updateAll = components.map((component: ComponentData) => {
      const componentToSave = { ...component };
      delete componentToSave.providerDescription;

      return adminClient.components.update(
        { id: component.id! },
        {
          ...componentToSave,
          config: {
            priority: [
              (
                itemOrder.length -
                itemOrder.indexOf(component.id!) +
                100
              ).toString(),
            ],
          },
        }
      );
    });

    try {
      await Promise.all(updateAll);
      refresh();
      addAlert(
        t("realm-settings:saveProviderListSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("realm-settings:saveProviderError", error);
    }
  };

  const onSearch = () => {
    if (searchVal !== "") {
      setSearchVal(searchVal);
      const x = components.filter((v) => {
        return v.name?.includes(searchVal) || v.providerId?.includes(searchVal);
      });
      setFilteredComponents(x);
    } else {
      setSearchVal("");
      setFilteredComponents(components);
    }
  };

  const handleKeyDown = (e: any) => {
    if (e.key === "Enter") {
      onSearch();
    }
  };

  const handleInputChange = (value: string) => {
    setSearchVal(value);
  };

  const handleModalToggle = () => {
    setIsCreateModalOpen(!isCreateModalOpen);
  };

  const [actionListOpen, setActionListOpen] = useState<boolean[]>(
    components.map(() => false)
  );
  const toggleActionList = (index: number) => {
    actionListOpen[index] = !actionListOpen[index];
    setActionListOpen([...actionListOpen]);
  };

  return (
    <>
      {defaultConsoleDisplayName === "aes-generated" && (
        <AESGeneratedModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )}
      {defaultConsoleDisplayName === "ecdsa-generated" && (
        <ECDSAGeneratedModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )}
      {defaultConsoleDisplayName === "hmac-generated" && (
        <HMACGeneratedModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )}
      {defaultConsoleDisplayName === "java-keystore" && (
        <JavaKeystoreModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )}
      {defaultConsoleDisplayName === "rsa" && (
        <RSAModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )}
      {defaultConsoleDisplayName === "rsa-generated" && (
        <RSAGeneratedModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )}
      <DeleteConfirm />
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <Toolbar>
          <ToolbarGroup className="providers-toolbar">
            <ToolbarItem>
              <InputGroup>
                <TextInput
                  name={"inputGroupName"}
                  id={"inputGroupName"}
                  type="search"
                  aria-label={t("common:search")}
                  placeholder={t("common:search")}
                  onChange={handleInputChange}
                  onKeyDown={handleKeyDown}
                />
                <Button
                  variant={ButtonVariant.control}
                  aria-label={t("common:search")}
                >
                  <SearchIcon />
                </Button>
              </InputGroup>
            </ToolbarItem>
            <ToolbarItem>
              <Dropdown
                data-testid="addProviderDropdown"
                className="add-provider-dropdown"
                isOpen={providerDropdownOpen}
                toggle={
                  <DropdownToggle
                    onToggle={(val) => setProviderDropdownOpen(val)}
                    isPrimary
                  >
                    {t("realm-settings:addProvider")}
                  </DropdownToggle>
                }
                dropdownItems={[
                  providerTypes.map((item) => (
                    <DropdownItem
                      onClick={() => {
                        handleModalToggle();

                        setProviderDropdownOpen(false);
                        setDefaultConsoleDisplayName(item);
                      }}
                      data-testid={`option-${item}`}
                      key={item}
                    >
                      {item}
                    </DropdownItem>
                  )),
                ]}
              />
            </ToolbarItem>
          </ToolbarGroup>
        </Toolbar>
        <DataList
          aria-label={t("common:groups")}
          onDragFinish={onDragFinish}
          onDragStart={onDragStart}
          onDragMove={onDragMove}
          onDragCancel={onDragCancel}
          itemOrder={itemOrder}
          isCompact
        >
          <DataListItem aria-labelledby={"aria"} id="data" key="data">
            <DataListItemRow className="test" data-testid="data-list-row">
              <DataListDragButton
                className="header-drag-button"
                aria-label="Reorder"
                aria-describedby={t("common-help:dragHelp")}
                aria-pressed="false"
                isDisabled
              />
              <DataListItemCells
                className="data-list-cells"
                dataListCells={[
                  <DataListCell className="name" key="name">
                    <>{t("realm-settings:name")}</>
                  </DataListCell>,
                  <DataListCell className="provider" key="provider">
                    <>{t("realm-settings:provider")}</>
                  </DataListCell>,
                  <DataListCell
                    className="provider-description"
                    key="provider-description"
                  >
                    <>{t("realm-settings:providerDescription")}</>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          {(filteredComponents.length === 0
            ? components
            : filteredComponents
          ).map((component, idx) => (
            <DataListItem
              draggable
              aria-labelledby={"aria"}
              key={component.id}
              id={component.id}
            >
              <DataListItemRow data-testid="data-list-row">
                <DataListControl>
                  <Tooltip content={t("dragInstruction")} position="top">
                    <DataListDragButton
                      className="kc-row-drag-button"
                      aria-label="Reorder"
                      aria-describedby={t("common-help:dragHelp")}
                      aria-pressed="false"
                    />
                  </Tooltip>
                </DataListControl>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell
                      data-testid="provider-name"
                      key={`name-${idx}`}
                    >
                      <Link
                        key={component.name}
                        data-testid="provider-name-link"
                        to={`${url}/${component.id}/${component.providerId}/settings`}
                      >
                        {component.name}
                      </Link>
                    </DataListCell>,
                    <DataListCell key={`providerId-${idx}`}>
                      {component.providerId}
                    </DataListCell>,
                    <DataListCell key={`providerDescription-${idx}`}>
                      {component.providerDescription}
                    </DataListCell>,
                    <DataListAction
                      aria-labelledby="data-list-action"
                      aria-label="Actions"
                      isPlainButtonAction
                      key={`data-action-list-${idx}`}
                      id={`data-action-list-${idx}`}
                    >
                      <Dropdown
                        isPlain
                        position={DropdownPosition.right}
                        isOpen={actionListOpen[idx]}
                        toggle={
                          <KebabToggle
                            data-testid="provider-action"
                            onToggle={() => {
                              toggleActionList(idx);
                            }}
                          />
                        }
                        dropdownItems={[
                          <DropdownItem
                            key="action"
                            component="button"
                            data-testid="delete-action"
                            onClick={() => {
                              setSelectedComponent(component);
                              toggleDeleteDialog();
                              toggleActionList(idx);
                            }}
                          >
                            {t("common:delete")}
                          </DropdownItem>,
                        ]}
                      />
                    </DataListAction>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
          ))}
        </DataList>
        <div className="pf-screen-reader" aria-live="assertive">
          {liveText}
        </div>
      </PageSection>
    </>
  );
};

type KeysProps = {
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
  refresh: () => void;
};

export const KeysProvidersTab = ({
  keyProviderComponentTypes,
  realmComponents,
  refresh,
  ...props
}: KeysProps) => {
  return (
    <KeysTabInner
      components={realmComponents?.map((component) => {
        const provider = keyProviderComponentTypes.find(
          (componentType: ComponentTypeRepresentation) =>
            component.providerId === componentType.id
        );

        return {
          ...component,
          providerDescription: provider?.helpText,
        };
      })}
      keyProviderComponentTypes={keyProviderComponentTypes}
      refresh={refresh}
      realmComponents={realmComponents}
      {...props}
    />
  );
};
