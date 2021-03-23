import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ToolbarItem,
  // Dropdown,
  // DropdownItem,
  // DropdownToggle,
} from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";

// import {
//   Table,
//   TableBody,
//   TableHeader,
//   TableVariant,
// } from "@patternfly/react-table";
import { useErrorHandler } from "react-error-boundary";
import { KeycloakDataTable } from "../../../components/table-toolbar/KeycloakDataTable";

// import { TableToolbar } from "../../../components/table-toolbar/TableToolbar";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../../components/alert/Alerts";
import {
  useAdminClient,
  asyncStateFetch,
} from "../../../context/auth/AdminClient";

import { useParams, Link } from "react-router-dom";

interface ComponentMapperRepresentation {
  config?: Record<string, any>;
  id?: string;
  name?: string;
  providerId?: string;
  providerType?: string;
  parentID?: string;
}

// type Row = ComponentMapperRepresentation {
//   name: JSX.Element;
//   type: string;
// };

export const LdapMapperList = () => {
  const [mappers, setMappers] = useState<ComponentMapperRepresentation[]>();

  const { t } = useTranslation("client-scopes");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

//  const [mapperAction, setMapperAction] = useState(false);

  const handleError = useErrorHandler();
  const [key, setKey] = useState(0);

  const { id } = useParams<{ id: string }>();

  useEffect(() => {
    return asyncStateFetch(
      () => {
        const testParams: { [name: string]: string | number } = {
          parent: id,
          type: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
        };
        return adminClient.components.find(testParams);
      },
      (mappers) => {
        setMappers(mappers);
        console.log(mappers);
      },
      handleError
    );
  }, [key]);

  if (!mappers) {
    return (
      <>
        <ListEmptyState
          message={t("emptyMappers")}
          instructions={t("emptyMappersInstructions")}
          primaryActionText={t("emptyPrimaryAction")}
        />
      </>
    );
  }

  const loader = async () =>
    Promise.resolve(
      (mappers || []).map((mapper) => {
        // const mapperType = mappers.filter(
        //   (type) => type.id === mapper.protocolMapper
        // )[0];
        return {
          ...mapper,
          name: mapper.name,
          type: mapper.providerId,
        } as ComponentMapperRepresentation;
      })
      // .sort((a, b) => a.priority - b.priority)
    );

  const url = "mappers";
  const MapperLink = (mapper: ComponentMapperRepresentation) => (
    <>
      <Link to={`${url}/${mapper.id}`}>{mapper.name}</Link>
    </>
  );

  return (
    <>
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="client-scopes:clientScopeList"
        searchPlaceholderKey="client-scopes:mappersSearchFor"
          toolbarItem={
              <ToolbarItem>
                <Button
                  data-testid="createMapperBtn"
                  variant="primary"
                  // onClick={handleModalToggle}
                  onClick={ () => addAlert(t("Add functionality not implemented yet!"), AlertVariant.success)}
                >
                  {/* TODO: Add proper strings */}
                  {t("Add mapper")} 
                </Button>
              </ToolbarItem>        
            }
        //   <Dropdown
        //     onSelect={() => setMapperAction(false)}
        //     toggle={
        //       <DropdownToggle
        //         isPrimary
        //         id="mapperAction"
        //         onToggle={() => setMapperAction(!mapperAction)}
        //         toggleIndicator={CaretDownIcon}
        //       >
        //         {t("addMapper")}
        //       </DropdownToggle>
        //     }
        //     isOpen={mapperAction}
        //     dropdownItems={[
        //       <DropdownItem
        //         key="predefined"
        //         onClick={() =>
        //           addAlert(t("mappingCreatedSuccess"), AlertVariant.success)
        //         }
        //       >
        //         {t("fromPredefinedMapper")}
        //       </DropdownItem>,
        //     ]}
        //   />

        // toolbarItem={
        //   <Dropdown
        //     onSelect={() => setMapperAction(false)}
        //     toggle={
        //       <DropdownToggle
        //         isPrimary
        //         id="mapperAction"
        //         onToggle={() => setMapperAction(!mapperAction)}
        //         toggleIndicator={CaretDownIcon}
        //       >
        //         {t("addMapper")}
        //       </DropdownToggle>
        //     }
        //     isOpen={mapperAction}
        //     dropdownItems={[
        //       <DropdownItem
        //         key="predefined"
        //         onClick={() => toggleAddMapperDialog(true)}
        //       >
        //         {t("fromPredefinedMapper")}
        //       </DropdownItem>,
        //       <DropdownItem
        //         key="byConfiguration"
        //         onClick={() => toggleAddMapperDialog(false)}
        //       >
        //         {t("byConfiguration")}
        //       </DropdownItem>,
        //     ]}
        //  />
        // }
        // actions={[
        //   {
        //     title: t("common:delete"),
        //     onRowClick: async (mapper) => {
        //       try {
        //         await adminClient.clientScopes.delProtocolMapper({
        //           id: clientScope.id!,
        //           mapperId: mapper.id!,
        //         });
        //         addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
        //         refresh();
        //       } catch (error) {
        //         addAlert(
        //           t("mappingDeletedError", { error }),
        //           AlertVariant.danger
        //         );
        //       }
        //       return true;
        //     },
        //   },
        // ]}
        actions={[
          {
            title: t("common:delete"),
            onRowClick: async (mapper) => {
              try {
                // await adminClient.clientScopes.delProtocolMapper({
                //   id: mapper.id!,
                //   mapperId: mapper.id!,
                // });
                // addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
                addAlert(
                  "Delete functionality not implemented yet!",
                  AlertVariant.success
                );
                // refresh();
              } catch (error) {
                addAlert(
                  t("mappingDeletedError", { error }),
                  AlertVariant.danger
                );
              }
              return true;
            },
          },
        ]}
        columns={[
          {
            name: "name",
            cellRenderer: MapperLink,
          },
          {
            name: "type",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyMappers")}
            instructions={t("emptyMappersInstructions")}
            primaryActionText={t("emptyPrimaryAction")}
            // onPrimaryAction={() => toggleAddMapperDialog(true)}
            // secondaryActions={[
            //   {
            //     text: t("emptySecondaryAction"),
            //     onClick: () => toggleAddMapperDialog(false),
            //     type: ButtonVariant.secondary,
            //   },
            // ]}
          />
        }
      />
    </>

    // <TableToolbar
    //   inputGroupName="clientsScopeToolbarTextInput"
    //   inputGroupPlaceholder={t("mappersSearchFor")}
    // >
    //   <Table
    //     variant={TableVariant.compact}
    //     cells={[
    //       t("common:name"),
    //       t("common:type"),
    //     ]}
    //     rows={mappers.map((cell) => {
    //       return {
    //         cells: Object.values([cell.name, cell.providerId]),
    //       };
    //     })}
    //     aria-label={t("clientScopeList")}
    //     actions={[
    //       {
    //         title: t("common:delete"),
    //         onClick: () => {
    //           addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
    //         },
    //       },
    //     ]}
    //   >
    //     <TableHeader />
    //     <TableBody />
    //   </Table>
    // </TableToolbar>
  );
};

/*
Sample responses:

const mapperList = [
  {
    id: "699b72e5-b936-41b9-98fc-5d5b3ec5ea6f",
    name: "username",
    providerId: "user-attribute-ldap-mapper",
    providerType: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
    parentId: "f1da61f9-08f7-4dc5-83dd-d774fba518d4",
    config: {
      "ldap.attribute": ["cn"],
      "is.mandatory.in.ldap": ["true"],
      "always.read.value.from.ldap": ["false"],
      "read.only": ["true"],
      "user.model.attribute": ["username"],
    },
  },
  {
    id: "c11788d2-62be-442f-813f-4b00382a1e10",
    name: "last name",
    providerId: "user-attribute-ldap-mapper",
    providerType: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
    parentId: "f1da61f9-08f7-4dc5-83dd-d774fba518d4",
    config: {
      "ldap.attribute": ["sn"],
      "is.mandatory.in.ldap": ["true"],
      "always.read.value.from.ldap": ["true"],
      "read.only": ["true"],
      "user.model.attribute": ["lastName"],
    },
  },
];
*/
