import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import {
  AlertVariant,
  ButtonVariant,
  EmptyState,
  EmptyStateActions,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  Dropdown,
  DropdownItem,
  DropdownToggle,
} from "@patternfly/react-core/deprecated";

import { PlusCircleIcon } from "@patternfly/react-icons";

import helpUrls from "../../help-urls";
import { useFetch } from "../../utils/useFetch";
import { adminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { useAlerts } from "../alert/Alerts";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../view-header/ViewHeader";
import { Action, KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { toCustomAttributeStoreInstance } from "./routes/CustomInstance";

export default function AttributeStoreTab() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const [addProviderOpen, setAddProviderOpen] = useState(false);
  const [attributeStoreInstances, setattributeStoreInstances] =
    useState<ComponentRepresentation[]>();

  const [currentCard, setCurrentCard] = useState({} as ComponentRepresentation);

  const refresh = () => setKey(new Date().getTime());

  const providers =
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.attributes.AttributeStoreProvider"
    ] || [];

  const createProviderDropdownItems = useMemo(
    () =>
      providers.map((p: ComponentTypeRepresentation) => (
        <DropdownItem
          key={p.id}
          onClick={() => {
            navigate(
              toCustomAttributeStoreInstance({ realm, providerId: p.id! }),
            );
          }}
        >
          {p.metadata.displayName || p.id}
        </DropdownItem>
      )),
    [],
  );

  useFetch(
    async () => {
      // fetch realm
      const realmModel = await adminClient.realms.findOne({ realm });

      // search for instances of attribute store providers
      const search: { [name: string]: string | number } = {
        parentId: realmModel!.id!,
        type: "org.keycloak.storage.attributes.AttributeStoreProvider",
      };
      return adminClient.components.find(search);
    },
    (instances) => {
      setattributeStoreInstances(instances);
    },
    [key],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("attributeStore.tab.delete.title", {
      name: currentCard.config?.displayName || currentCard.providerId,
    }),
    messageKey: t("attributeStore.tab.delete.description", {
      name: currentCard.config?.displayName || currentCard.providerId,
      id: currentCard.id,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id: currentCard.id! });
        refresh();
        addAlert(
          t("attributeStore.providers.deleteSuccess"),
          AlertVariant.success,
        );
      } catch (error) {
        addError("attributeStore.providers.deleteError", error);
      }
    },
  });

  const toggleDeleteForCard = (instance: ComponentRepresentation) => {
    setCurrentCard(instance);
    toggleDeleteDialog();
  };

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey="attributeStore.tab.title"
        subKey="attributeStore.tab.description"
        helpUrl={helpUrls.userFederationUrl}
      />
      <PageSection>
        <KeycloakDataTable
          key={key}
          loader={attributeStoreInstances || []}
          isPaginated
          ariaLabelKey="identityProviders"
          toolbarItem={
            <ToolbarItem>
              <Dropdown
                toggle={
                  <DropdownToggle
                    onToggle={() => setAddProviderOpen(!addProviderOpen)}
                    toggleVariant="primary"
                  >
                    {t("addProvider")}
                  </DropdownToggle>
                }
                isOpen={addProviderOpen}
                dropdownItems={createProviderDropdownItems}
              />
            </ToolbarItem>
          }
          actions={[
            {
              title: t("delete"),
              onRowClick: (provider) => {
                toggleDeleteForCard(provider);
              },
            } as Action<ComponentRepresentation>,
          ]}
          columns={[
            {
              name: "displayName",
              displayKey: "name",
              cellRenderer: (c) => (
                <Link
                  to={toCustomAttributeStoreInstance({
                    realm,
                    providerId: c.providerId!,
                    id: c.id!,
                  })}
                >
                  {c.config?.displayName?.[0] || "unknown"}
                </Link>
              ),
            },
            {
              name: "id",
              displayKey: "id",
            },
            {
              name: "providerId",
              displayKey: "providerType",
            },
          ]}
          emptyState={
            <EmptyState data-testid="empty-state" variant="lg">
              <EmptyStateIcon icon={PlusCircleIcon} />
              <EmptyStateHeader titleText={t("getStarted")} headingLevel="h1" />
              <EmptyStateFooter>
                <EmptyStateActions>
                  <Dropdown
                    data-testid="addProviderDropdown"
                    toggle={
                      <DropdownToggle
                        onToggle={() => setAddProviderOpen(!addProviderOpen)}
                        toggleVariant="primary"
                      >
                        {t("addProvider")}
                      </DropdownToggle>
                    }
                    isOpen={addProviderOpen}
                    dropdownItems={createProviderDropdownItems}
                  />
                </EmptyStateActions>
              </EmptyStateFooter>
            </EmptyState>
          }
        />
      </PageSection>
    </>
  );
}
