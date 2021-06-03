import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  DataList,
  DataListCell,
  DataListControl,
  DataListDragButton,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  InputGroup,
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
import { AESGeneratedModal } from "./AESGeneratedModal";
import { ECDSAGeneratedModal } from "./ECDSAGeneratedModal";
import { HMACGeneratedModal } from "./HMACGeneratedModal";
import { JavaKeystoreModal } from "./JavaKeystoreModal";
import { RSAModal } from "./RSAModal";
import { RSAGeneratedModal } from "./RSAGeneratedModal";

type ComponentData = KeyMetadataRepresentation & {
  providerDescription?: string;
  name?: string;
};

type KeysTabInnerProps = {
  components: ComponentData[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
  refresh: () => void;
};

export const KeysTabInner = ({ components, refresh }: KeysTabInnerProps) => {
  const { t } = useTranslation("roles");

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

  const [defaultConsoleDisplayName, setDefaultConsoleDisplayName] = useState(
    ""
  );

  const [liveText, setLiveText] = useState("");

  useEffect(() => {
    setItemOrder(["data", ...itemIds]);
  }, [components, searchVal]);

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
      {/* {defaultConsoleDisplayName === "ecdsa-generated" && (
        <ECDSAGeneratedModal
          handleModalToggle={handleModalToggle}
          providerType={defaultConsoleDisplayName}
          refresh={refresh}
          open={isCreateModalOpen}
        />
      )} */}
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
          ).map((component, idx) => (
            <DataListItem
              draggable
              aria-labelledby={"aria"}
              key={`data${idx}`}
              id={`data${idx}`}
            >
              <DataListItemRow data-testid={"data-list-row"}>
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
                    <DataListCell key={"4"}>
                      <>
                        <Button variant="link">{component.providerId}</Button>
                      </>
                    </DataListCell>,
                    <DataListCell key={"5"}>
                      <>{component.name}</>
                    </DataListCell>,
                    <DataListCell key={"6"}>
                      <>{component.providerDescription}</>
                    </DataListCell>,
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
  components: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
  refresh: () => void;
};

export const KeysProviderTab = ({
  components,
  keyProviderComponentTypes,
  refresh,
  ...props
}: KeysProps) => {
  return (
    <KeysTabInner
      components={components?.map((component) => {
        const provider = keyProviderComponentTypes.find(
          (componentType: ComponentTypeRepresentation) =>
            component.providerId === componentType.id
        );
        return { ...component, providerDescription: provider?.helpText };
      })}
      keyProviderComponentTypes={keyProviderComponentTypes}
      refresh={refresh}
      realmComponents={components}
      {...props}
    />
  );
};
