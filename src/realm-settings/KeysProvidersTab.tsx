import React, { Component, useState, useEffect } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
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
  PageSection,
} from "@patternfly/react-core";
import { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

import "./RealmSettingsSection.css";
import {
  cellWidth,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import ComponentTypeRepresentation from "keycloak-admin/lib/defs/componentTypeRepresentation";
import { asyncStateFetch } from "../context/auth/AdminClient";
import { useErrorHandler } from "react-error-boundary";

type ComponentData = KeyMetadataRepresentation & {
  providerDescription?: string;
  name?: string;
};

type KeysTabInnerProps = {
  components: ComponentData[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];
};

export const KeysTabInner = ({
  components,
  keyProviderComponentTypes,
}: KeysTabInnerProps) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const errorHandler = useErrorHandler();

  const [itemOrder, setItemOrder] = useState(["data1", "data2", "data3"]);
  const [id, setId] = useState("");

  const loader = async () => {
    return components;
  };

  //   useEffect(
  //     () =>
  //       asyncStateFetch(
  //         async () => {
  //             return components;
  //         },
  //         async () => {

  //             setFetchedComponents(components)
  //         },
  //         errorHandler
  //       ),
  //     []
  //   );

  React.useEffect(() => {
    refresh();
  }, [components]);

  const goToCreate = () => history.push(`${url}/add-role`);

  console.log("componentsss", components);

  const onDragStart = (id: string) => {
    console.log(itemOrder);
    setId(id);
  };

  const onDragMove = (oldIndex, newIndex) => {
    console.log(`Dragging item ${id}.`);
  };

  const columns = [
    t("realm-settings:name"),
    t("realm-settings:provider"),
    t("realm-settings:providerDescription"),
  ];

  //   const onDragCancel = () => {
  //     this.setState({
  //       liveText: `Dragging cancelled. List is unchanged.`
  //     });
  //   };

  const onDragFinish = (itemOrder) => {
    setItemOrder(itemOrder);
  };

  return (
    <>
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        {/* <PublicKeyDialog />
        <CertificateDialog /> */}
        {/* <KeycloakDataTable
          key={key}
          loader={loader}
          ariaLabelKey="realm-settings:keysList"
          searchPlaceholderKey="realm-settings:searchKey"
          canSelectAll
          columns={[
            {
              name: "providerId",
              displayKey: "realm-settings:name",
              cellFormatters: [emptyFormatter()],
            //   cellRenderer: DataCellRendererTest,
              transforms: [cellWidth(15)],
            },
            {
              name: "name",
              displayKey: "realm-settings:provider",
              cellFormatters: [emptyFormatter()],
              transforms: [cellWidth(10)],
            },
            {
              name: "providerDescription",
              displayKey: "realm-settings:providerDescription",
            //   cellRenderer: ProviderDescriptionRenderer,
              cellFormatters: [emptyFormatter()],
            },
          ]}
          emptyState={
            <ListEmptyState
              hasIcon={true}
              message={t("noRoles")}
              instructions={t("noRolesInstructions")}
              primaryActionText={t("createRole")}
              onPrimaryAction={goToCreate}
            />
          }
        /> */}
        <Table
          aria-label="Simple Table"
          //   variant={TableVariant}
          //   borders={choice !== 'compactBorderless'}
          cells={columns}
          //   rows={rows}
        >
          <TableHeader />
          <TableBody />
        </Table>
        <DataList
          // onSelectDataListItem={(value) => {
          //   setGroupId(value);
          // }}
          aria-label={t("groups")}
          onDragFinish={onDragFinish}
          onDragStart={onDragStart}
          onDragMove={onDragMove}
          //   onDragCancel={this.onDragCancel}
          itemOrder={itemOrder}
          isCompact
        >
          {/* <DataListItem aria-labelledby={"aria"} id="data1" key="1">
            <DataListItemRow className="test" data-testid={"group.name"}>
              <DataListItemCells className="test2"
                dataListCells={[
                  <DataListCell key={"1"}>
                    <>{t("realm-settings:name")}</>
                  </DataListCell>,
                  <DataListCell key={"2"}>
                    <>{t("realm-settings:provider")}</>
                  </DataListCell>,
                  <DataListCell key={"3"}>
                    <>{t("realm-settings:providerDescription")}</>
                  </DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem> */}
          {components.map((component, idx) => (
            <DataListItem
              draggable
              aria-labelledby={"aria"}
              key={`key-${idx + 1}`}
              id={`data${idx + 1}`}
            >
              <DataListItemRow data-testid={"data-list-row"}>
                <DataListControl>
                  <DataListDragButton
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
  // console.log("components", components)
  // console.log("keyz", keys)
  // console.log("keyz", keyProviderComponentTypes)

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
      //   keyProviderComponentTypes={keyProviderComponentTypes?.map((key) => {
      //     const provider = keyProviderComponentTypes.find(
      //       (key: key) =>
      //         component.id === key.providerId
      //     );
      //     return { ...key, provider: provider?.providerId };
      //   })}
      {...props}
    />
  );
};
