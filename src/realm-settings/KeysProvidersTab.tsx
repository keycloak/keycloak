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
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";

import type { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "keycloak-admin/lib/defs/componentTypeRepresentation";

import "./RealmSettingsSection.css";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { Link, useRouteMatch } from "react-router-dom";
import { AESGeneratedModal } from "./key-providers/aes-generated/AESGeneratedModal";
import { JavaKeystoreModal } from "./JavaKeystoreModal";
import { HMACGeneratedModal } from "./key-providers/hmac-generated/HMACGeneratedModal";
import { ECDSAGeneratedModal } from "./key-providers/ecdsa-generated/ECDSAGeneratedModal";
import { RSAModal } from "./RSAModal";
import { RSAGeneratedModal } from "./RSAGeneratedModal";

type ComponentData = KeyMetadataRepresentation & {
  id?: string;
  providerDescription?: string;
  name?: string;
  toggleHidden?: boolean;
  config?: any;
};

type KeysTabInnerProps = {
  components: ComponentData[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
  refresh: () => void;
};

export const KeysTabInner = ({ components, refresh }: KeysTabInnerProps) => {
  const { t } = useTranslation("realm-settings");
  const { addAlert } = useAlerts();
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
  const providerTypes = serverInfo.componentTypes![
    "org.keycloak.keys.KeyProvider"
  ].map((item) => item.id);

  const itemIds = components.map((_, idx) => "data" + idx);

  const [itemOrder, setItemOrder] = useState<string[]>([]);
  const [providerDropdownOpen, setProviderDropdownOpen] = useState(false);

  const [defaultConsoleDisplayName, setDefaultConsoleDisplayName] =
    useState("");

  const [selectedComponent, setSelectedComponent] =
    useState<ComponentRepresentation>();

  const [liveText, setLiveText] = useState("");

  useEffect(() => {
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
        addAlert(t("deleteProviderError", { error }), AlertVariant.danger);
      }
    },
  });

  const onDragStart = (id: string) => {
    setLiveText(t("onDragStart", { id }));
    setId(id);
  };

  const onDragMove = () => {
    setLiveText(t("onDragMove", { id }));
  };

  const onDragCancel = () => {
    setLiveText(t("onDragCancel"));
  };

  const onDragFinish = (itemOrder: string[]) => {
    setItemOrder(["data", ...itemOrder.filter((i) => i !== "data")]);
    setLiveText(t("onDragCancel"));
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
          <>
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
          </>
        </Toolbar>

        <DataList
          aria-label={t("groups")}
          onDragFinish={onDragFinish}
          onDragStart={onDragStart}
          onDragMove={onDragMove}
          onDragCancel={onDragCancel}
          itemOrder={itemOrder}
          isCompact
        >
          <DataListItem aria-labelledby={"aria"} id="data" key="data">
            <DataListItemRow className="test" data-testid={"data-list-row"}>
              <DataListDragButton
                className="header-drag-button"
                aria-label="Reorder"
                aria-labelledby="simple-item"
                aria-describedby="Press space or enter to begin dragging, and use the arrow keys to navigate up or down. Press enter to confirm the drag, or any other key to cancel the drag operation."
                aria-pressed="false"
                isDisabled
              />
              <DataListItemCells
                className="data-list-cells"
                dataListCells={[
                  <DataListCell className="name" key={"1"}>
                    <>{t("realm-settings:name")}</>
                  </DataListCell>,
                  <DataListCell className="provider" key={"2"}>
                    <>{t("realm-settings:provider")}</>
                  </DataListCell>,
                  <DataListCell className="provider-description" key={"3"}>
                    <>{t("realm-settings:providerDescription")}</>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          {(filteredComponents.length === 0
            ? components
            : filteredComponents
          ).map((component: ComponentData, idx) => (
            <DataListItem
              draggable
              aria-labelledby={"aria"}
              key={`data${idx}`}
              id={`data${idx}`}
            >
              <DataListItemRow key={idx} data-testid={"data-list-row"}>
                <DataListControl>
                  <DataListDragButton
                    className="row-drag-button"
                    aria-label="Reorder"
                    aria-labelledby="simple-item2"
                    aria-describedby="Press space or enter to begin dragging, and use the arrow keys to navigate up or down. Press enter to confirm the drag, or any other key to cancel the drag operation."
                    aria-pressed="false"
                  />
                </DataListControl>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell
                      data-testid="provider-name"
                      key={`name-${idx}`}
                    >
                      <>
                        <Link
                          key={component.name}
                          data-testid="provider-name-link"
                          to={`${url}/${component.id}/${component.providerId}/settings`}
                        >
                          {component.name}
                        </Link>
                      </>
                    </DataListCell>,
                    <DataListCell key={`providerId-${idx}`}>
                      <>{component.providerId}</>
                    </DataListCell>,
                    <DataListCell key={`providerDescription-${idx}`}>
                      <>{component.providerDescription}</>
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
