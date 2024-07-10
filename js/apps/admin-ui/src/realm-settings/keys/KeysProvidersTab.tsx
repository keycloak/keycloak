import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  InputGroup,
  InputGroupItem,
  PageSection,
  TextInput,
  Toolbar,
  ToolbarGroup,
  ToolbarItem,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { KeyboardEvent, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { DraggableTable } from "../../authentication/components/DraggableTable";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { KEY_PROVIDER_TYPE } from "../../util";
import useToggle from "../../utils/useToggle";
import { ProviderType, toKeyProvider } from "../routes/KeyProvider";
import { KeyProviderModal } from "./key-providers/KeyProviderModal";
import { KeyProvidersPicker } from "./key-providers/KeyProvidersPicker";

import "../realm-settings-section.css";

type ComponentData = KeyMetadataRepresentation & {
  id?: string;
  providerDescription?: string;
  name?: string;
  toggleHidden?: boolean;
  config?: any;
  parentId?: string;
};

type KeysProvidersTabProps = {
  realmComponents: ComponentRepresentation[];
  refresh: () => void;
};

export const KeysProvidersTab = ({
  realmComponents,
  refresh,
}: KeysProvidersTabProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const [searchVal, setSearchVal] = useState("");
  const [filteredComponents, setFilteredComponents] = useState<ComponentData[]>(
    [],
  );

  const [isCreateModalOpen, handleModalToggle] = useToggle();
  const serverInfo = useServerInfo();
  const keyProviderComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const [providerOpen, toggleProviderOpen] = useToggle();
  const [defaultUIDisplayName, setDefaultUIDisplayName] =
    useState<ProviderType>();

  const [selectedComponent, setSelectedComponent] =
    useState<ComponentRepresentation>();

  const components = useMemo(
    () =>
      realmComponents.map((component) => {
        const provider = keyProviderComponentTypes.find(
          (componentType: ComponentTypeRepresentation) =>
            component.providerId === componentType.id,
        );

        return {
          ...component,
          providerDescription: provider?.helpText,
        };
      }),
    [realmComponents],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteProviderTitle",
    messageKey: t("deleteProviderConfirm", {
      provider: selectedComponent?.name,
    }),
    continueButtonLabel: "delete",
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
        addError("deleteProviderError", error);
      }
    },
  });

  const onSearch = () => {
    if (searchVal !== "") {
      setSearchVal(searchVal);
      const filteredComponents = components.filter(
        (component) =>
          component.name?.includes(searchVal) ||
          component.providerId?.includes(searchVal),
      );
      setFilteredComponents(filteredComponents);
    } else {
      setSearchVal("");
      setFilteredComponents(components);
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      onSearch();
    }
  };

  const handleInputChange = (value: string) => {
    setSearchVal(value);
  };

  return (
    <>
      {providerOpen && (
        <KeyProvidersPicker
          onClose={() => toggleProviderOpen()}
          onConfirm={(provider) => {
            handleModalToggle();
            setDefaultUIDisplayName(provider as ProviderType);
            toggleProviderOpen();
          }}
        />
      )}
      {isCreateModalOpen && defaultUIDisplayName && (
        <KeyProviderModal
          providerType={defaultUIDisplayName}
          onClose={() => {
            handleModalToggle();
            refresh();
          }}
        />
      )}
      <DeleteConfirm />
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <Toolbar>
          <ToolbarGroup className="providers-toolbar">
            <ToolbarItem>
              <InputGroup>
                <InputGroupItem isFill>
                  <TextInput
                    name={"inputGroupName"}
                    id={"inputGroupName"}
                    data-testid="provider-search-input"
                    type="search"
                    aria-label={t("search")}
                    placeholder={t("search")}
                    onChange={(_event, value: string) =>
                      handleInputChange(value)
                    }
                    onKeyDown={handleKeyDown}
                  />
                </InputGroupItem>
                <InputGroupItem>
                  <Button
                    variant={ButtonVariant.control}
                    aria-label={t("search")}
                    onClick={onSearch}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroupItem>
              </InputGroup>
            </ToolbarItem>
            <ToolbarItem>
              <Button
                data-testid="addProviderDropdown"
                className="add-provider-dropdown"
                onClick={() => toggleProviderOpen()}
              >
                {t("addProvider")}
              </Button>
            </ToolbarItem>
          </ToolbarGroup>
        </Toolbar>
        <DraggableTable
          variant="compact"
          className="kc-draggable-table"
          keyField="id"
          data={
            filteredComponents.length === 0 ? components : filteredComponents
          }
          onDragFinish={async (_, itemOrder) => {
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
                },
              );
            });

            try {
              await Promise.all(updateAll);
              refresh();
              addAlert(t("saveProviderListSuccess"), AlertVariant.success);
            } catch (error) {
              addError("saveProviderError", error);
            }
          }}
          columns={[
            {
              name: "name",
              displayKey: "name",
              cellRenderer: (component) => (
                <Link
                  key={component.name}
                  data-testid="provider-name-link"
                  to={toKeyProvider({
                    realm,
                    id: component.id!,
                    providerType: component.providerId as ProviderType,
                  })}
                >
                  {component.name}
                </Link>
              ),
            },
            {
              name: "providerId",
              displayKey: "provider",
            },
            {
              name: "providerDescription",
              displayKey: "providerDescription",
            },
          ]}
          actions={[
            {
              title: t("delete"),
              onClick: (_key, _idx, component) => {
                setSelectedComponent(component as ComponentRepresentation);
                toggleDeleteDialog();
              },
            },
          ]}
        />
      </PageSection>
    </>
  );
};
