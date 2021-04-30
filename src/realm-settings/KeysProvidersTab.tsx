import React, { Component, useState, useEffect } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, ButtonVariant, DataList, DataListAction, DataListCell, DataListItem, DataListItemCells, DataListItemRow, PageSection } from "@patternfly/react-core";
import { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

import "./RealmSettingsSection.css";
import { cellWidth } from "@patternfly/react-table";
import ComponentTypeRepresentation from "keycloak-admin/lib/defs/componentTypeRepresentation";
import { asyncStateFetch } from "../context/auth/AdminClient";
import { useErrorHandler } from "react-error-boundary";

type ComponentData = KeyMetadataRepresentation & {
  providerDescription?: string;
};

type KeysTabInnerProps = {
  components: ComponentData[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];

};

export const KeysTabInner = ({ components, keyProviderComponentTypes }: KeysTabInnerProps) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const errorHandler = useErrorHandler();


  const [publicKey, setPublicKey] = useState("");
  const [certificate, setCertificate] = useState("");
  const [fetchedComponents, setFetchedComponents] = useState<ComponentRepresentation[]>([]);

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

  const ProviderDescriptionRenderer = ({ providerDescription }: ComponentData) => {


    return <>{providerDescription}</>;
  };

  return (
    <>
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        {/* <PublicKeyDialog />
        <CertificateDialog /> */}
        <KeycloakDataTable
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
              cellRenderer: ProviderDescriptionRenderer,
              cellFormatters: [emptyFormatter()],
            }
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
        />
        {/* <DataList
        // onSelectDataListItem={(value) => {
        //   setGroupId(value);
        // }}
        aria-label={t("groups")}
        isCompact
      >
        {(keyProviderComponentTypes).map((component) => (
          <DataListItem draggable
            aria-labelledby={"aria"}
            key={"key"}
            id={"id"}
            // onClick={(e) => {
            //   if ((e.target as HTMLInputElement).type !== "checkbox") {
            //     setGroupId(group.id);
            //   }
            // }}
          >
            <DataListItemRow data-testid={"group.name"}>

              <DataListItemCells
                dataListCells={[
                  <DataListCell key={`name}`}>
                    <>{"group.name"}</>
                  </DataListCell>,
                ]}
              />
              <DataListAction
                aria-labelledby={`select`}
                id={`select`}
                aria-label={t("groupName")}
                isPlainButtonAction
              >
              </DataListAction>
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList> */}
      </PageSection>
    </>
  );
};

type KeysProps = {
  components: ComponentRepresentation[];
  realmComponents: ComponentRepresentation[];
  keyProviderComponentTypes: ComponentTypeRepresentation[];

};


export const KeysProviderTab = ({ components, keyProviderComponentTypes, ...props }: KeysProps) => {
    console.log("components", components)
    // console.log("keyz", keys)
    // console.log("keyz", keyProviderComponentTypes)

  return (
    <KeysTabInner
      components={components?.map((key) => {
        const provider = keyProviderComponentTypes.find(
          (component: ComponentTypeRepresentation) =>
            component.id === key.providerId
        );
        return { ...key, providerDescription: provider?.helpText };
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
