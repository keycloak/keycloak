import { useMemo, useState, KeyboardEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
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

import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

import type { ProviderType } from "../routes/KeyProvider";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useRouteMatch } from "react-router-dom";
import { Link } from "react-router-dom-v5-compat";
import { KEY_PROVIDER_TYPE } from "../../util";
import { DraggableTable } from "../../authentication/components/DraggableTable";
import { KeyProviderModal } from "./key-providers/KeyProviderModal";
import useToggle from "../../utils/useToggle";

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
  const { t } = useTranslation("realm-settings");
  const { addAlert, addError } = useAlerts();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { url } = useRouteMatch();

  const [searchVal, setSearchVal] = useState("");
  const [filteredComponents, setFilteredComponents] = useState<ComponentData[]>(
    []
  );

  const [isCreateModalOpen, handleModalToggle] = useToggle();
  const serverInfo = useServerInfo();
  const keyProviderComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];
  const providerTypes = keyProviderComponentTypes.map((item) => item.id);

  const [providerDropdownOpen, setProviderDropdownOpen] = useState(false);
  const [defaultUIDisplayName, setDefaultUIDisplayName] =
    useState<ProviderType>();

  const [selectedComponent, setSelectedComponent] =
    useState<ComponentRepresentation>();

  const components = useMemo(
    () =>
      realmComponents.map((component) => {
        const provider = keyProviderComponentTypes.find(
          (componentType: ComponentTypeRepresentation) =>
            component.providerId === componentType.id
        );

        return {
          ...component,
          providerDescription: provider?.helpText,
        };
      }),
    [realmComponents]
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "realm-settings:deleteProviderTitle",
    messageKey: t("deleteProviderConfirm", {
      provider: selectedComponent?.name,
    }),
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

  const onSearch = () => {
    if (searchVal !== "") {
      setSearchVal(searchVal);
      const filteredComponents = components.filter(
        (component) =>
          component.name?.includes(searchVal) ||
          component.providerId?.includes(searchVal)
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
                <TextInput
                  name={"inputGroupName"}
                  id={"inputGroupName"}
                  data-testid="provider-search-input"
                  type="search"
                  aria-label={t("common:search")}
                  placeholder={t("common:search")}
                  onChange={handleInputChange}
                  onKeyDown={handleKeyDown}
                />
                <Button
                  variant={ButtonVariant.control}
                  aria-label={t("common:search")}
                  onClick={onSearch}
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
                    {t("addProvider")}
                  </DropdownToggle>
                }
                dropdownItems={[
                  providerTypes.map((item) => (
                    <DropdownItem
                      onClick={() => {
                        handleModalToggle();

                        setProviderDropdownOpen(false);
                        setDefaultUIDisplayName(item as ProviderType);
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
                }
              );
            });

            try {
              await Promise.all(updateAll);
              refresh();
              addAlert(t("saveProviderListSuccess"), AlertVariant.success);
            } catch (error) {
              addError("realm-settings:saveProviderError", error);
            }
          }}
          columns={[
            {
              name: "name",
              displayKey: "realm-settings:name",
              cellRenderer: (component) => (
                <Link
                  key={component.name}
                  data-testid="provider-name-link"
                  to={`${url}/${component.id}/${component.providerId}/settings`}
                >
                  {component.name}
                </Link>
              ),
            },
            {
              name: "providerId",
              displayKey: "realm-settings:provider",
            },
            {
              name: "providerDescription",
              displayKey: "realm-settings:providerDescription",
            },
          ]}
          actions={[
            {
              title: t("common:delete"),
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
