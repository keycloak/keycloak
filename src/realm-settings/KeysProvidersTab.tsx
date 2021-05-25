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
  DropdownToggle,
  InputGroup,
  PageSection,
  TextInput,
  Toolbar,
  ToolbarGroup,
  ToolbarItem,
} from "@patternfly/react-core";
import type { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

import "./RealmSettingsSection.css";
import type ComponentTypeRepresentation from "keycloak-admin/lib/defs/componentTypeRepresentation";
import { SearchIcon } from "@patternfly/react-icons";

type ComponentData = KeyMetadataRepresentation & {
  providerDescription?: string;
  name?: string;
};

type KeysTabInnerProps = {
  components: ComponentData[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
};

export const KeysTabInner = ({ components }: KeysTabInnerProps) => {
  const { t } = useTranslation("roles");

  const [id, setId] = useState("");
  const [searchVal, setSearchVal] = useState("");
  const [filteredComponents, setFilteredComponents] = useState<ComponentData[]>(
    []
  );

  const itemIds = components.map((item, idx) => "data" + idx);

  const [itemOrder, setItemOrder] = useState<string[]>([]);

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

  return (
    <>
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
                  onSelect={() => {}}
                  toggle={
                    <DropdownToggle isPrimary>
                      {t("realm-settings:addProvider")}
                    </DropdownToggle>
                  }
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
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
};

export const KeysProviderTab = ({
  components,
  keyProviderComponentTypes,
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
      {...props}
    />
  );
};
