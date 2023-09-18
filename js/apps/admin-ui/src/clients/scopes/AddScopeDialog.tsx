import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import {
  Button,
  ButtonVariant,
  Dropdown,
  DropdownDirection,
  DropdownItem,
  DropdownToggle,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import {
  CaretDownIcon,
  CaretUpIcon,
  FilterIcon,
} from "@patternfly/react-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  ClientScopeType,
  clientScopeTypesDropdown,
} from "../../components/client-scope/ClientScopeTypes";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import useToggle from "../../utils/useToggle";
import { getProtocolName } from "../utils";

import "./client-scopes.css";

export type AddScopeDialogProps = {
  clientScopes: ClientScopeRepresentation[];
  clientName?: string;
  open: boolean;
  toggleDialog: () => void;
  onAdd: (
    scopes: { scope: ClientScopeRepresentation; type?: ClientScopeType }[],
  ) => void;
  isClientScopesConditionType?: boolean;
};

enum FilterType {
  Name = "Name",
  Protocol = "Protocol",
}

enum ProtocolType {
  All = "All",
  SAML = "SAML",
  OpenIDConnect = "OpenID Connect",
}

export const AddScopeDialog = ({
  clientScopes: scopes,
  clientName,
  open,
  toggleDialog,
  onAdd,
  isClientScopesConditionType,
}: AddScopeDialogProps) => {
  const { t } = useTranslation();
  const [addToggle, setAddToggle] = useState(false);
  const [rows, setRows] = useState<ClientScopeRepresentation[]>([]);
  const [filterType, setFilterType] = useState(FilterType.Name);
  const [protocolType, setProtocolType] = useState(ProtocolType.All);

  const [isFilterTypeDropdownOpen, toggleIsFilterTypeDropdownOpen] =
    useToggle();

  const [isProtocolTypeDropdownOpen, toggleIsProtocolTypeDropdownOpen] =
    useToggle(false);

  const clientScopes = useMemo(() => {
    if (protocolType === ProtocolType.OpenIDConnect) {
      return scopes.filter((item) => item.protocol === "openid-connect");
    } else if (protocolType === ProtocolType.SAML) {
      return scopes.filter((item) => item.protocol === "saml");
    }
    return scopes;
  }, [scopes, filterType, protocolType]);

  const action = (scope: ClientScopeType) => {
    const scopes = rows.map((row) => {
      return { scope: row, type: scope };
    });
    onAdd(scopes);
    setAddToggle(false);
    toggleDialog();
  };

  const onFilterTypeDropdownSelect = (filterType: string) => {
    if (filterType === FilterType.Name) {
      setFilterType(FilterType.Protocol);
    } else if (filterType === FilterType.Protocol) {
      setFilterType(FilterType.Name);
      setProtocolType(ProtocolType.All);
    }

    toggleIsFilterTypeDropdownOpen();
  };

  const onProtocolTypeDropdownSelect = (protocolType: string) => {
    if (protocolType === ProtocolType.SAML) {
      setProtocolType(ProtocolType.SAML);
    } else if (protocolType === ProtocolType.OpenIDConnect) {
      setProtocolType(ProtocolType.OpenIDConnect);
    } else if (protocolType === ProtocolType.All) {
      setProtocolType(ProtocolType.All);
    }

    toggleIsProtocolTypeDropdownOpen();
  };

  const protocolTypeOptions = [
    <SelectOption key={1} value={ProtocolType.SAML}>
      {t("protocolTypes.saml")}
    </SelectOption>,
    <SelectOption key={2} value={ProtocolType.OpenIDConnect}>
      {t("protocolTypes.openid-connect")}
    </SelectOption>,
    <SelectOption key={3} value={ProtocolType.All} isPlaceholder>
      {t("protocolTypes.all")}
    </SelectOption>,
  ];

  return (
    <Modal
      variant={ModalVariant.medium}
      title={
        isClientScopesConditionType
          ? t("addClientScope")
          : t("addClientScopesTo", { clientName })
      }
      isOpen={open}
      onClose={toggleDialog}
      actions={
        isClientScopesConditionType
          ? [
              <Button
                id="modal-add"
                data-testid="confirm"
                key="add"
                variant={ButtonVariant.primary}
                onClick={() => {
                  const scopes = rows.map((scope) => ({ scope }));
                  onAdd(scopes);
                  toggleDialog();
                }}
                isDisabled={rows.length === 0}
              >
                {t("add")}
              </Button>,
              <Button
                id="modal-cancel"
                data-testid="cancel"
                key="cancel"
                variant={ButtonVariant.link}
                onClick={() => {
                  setRows([]);
                  toggleDialog();
                }}
              >
                {t("cancel")}
              </Button>,
            ]
          : [
              <Dropdown
                className="keycloak__client-scopes-add__add-dropdown"
                id="add-dropdown"
                key="add-dropdown"
                direction={DropdownDirection.up}
                isOpen={addToggle}
                toggle={
                  <DropdownToggle
                    isDisabled={rows.length === 0}
                    onToggle={() => setAddToggle(!addToggle)}
                    isPrimary
                    toggleIndicator={CaretUpIcon}
                    id="add-scope-toggle"
                  >
                    {t("add")}
                  </DropdownToggle>
                }
                dropdownItems={clientScopeTypesDropdown(t, action)}
              />,
              <Button
                id="modal-cancel"
                key="cancel"
                variant={ButtonVariant.link}
                onClick={() => {
                  setRows([]);
                  toggleDialog();
                }}
              >
                {t("cancel")}
              </Button>,
            ]
      }
    >
      <KeycloakDataTable
        loader={clientScopes}
        ariaLabelKey="chooseAMapperType"
        searchPlaceholderKey={
          filterType === FilterType.Name ? "searchForClientScope" : undefined
        }
        isSearching={filterType !== FilterType.Name}
        searchTypeComponent={
          <Dropdown
            onSelect={() => {
              onFilterTypeDropdownSelect(filterType);
            }}
            data-testid="filter-type-dropdown"
            toggle={
              <DropdownToggle
                id="toggle-id-9"
                onToggle={toggleIsFilterTypeDropdownOpen}
                toggleIndicator={CaretDownIcon}
                icon={<FilterIcon />}
              >
                {filterType}
              </DropdownToggle>
            }
            isOpen={isFilterTypeDropdownOpen}
            dropdownItems={[
              <DropdownItem
                data-testid="filter-type-dropdown-item"
                key="filter-type"
              >
                {filterType === FilterType.Name ? t("protocol") : t("name")}
              </DropdownItem>,
            ]}
          />
        }
        toolbarItem={
          filterType === FilterType.Protocol && (
            <>
              <Dropdown
                onSelect={() => {
                  onFilterTypeDropdownSelect(filterType);
                }}
                data-testid="filter-type-dropdown"
                toggle={
                  <DropdownToggle
                    id="toggle-id-9"
                    onToggle={toggleIsFilterTypeDropdownOpen}
                    toggleIndicator={CaretDownIcon}
                    icon={<FilterIcon />}
                  >
                    {filterType}
                  </DropdownToggle>
                }
                isOpen={isFilterTypeDropdownOpen}
                dropdownItems={[
                  <DropdownItem
                    data-testid="filter-type-dropdown-item"
                    key="filter-type"
                  >
                    {t("name")}
                  </DropdownItem>,
                ]}
              />
              <Select
                variant={SelectVariant.single}
                className="kc-protocolType-select"
                aria-label={t("selectOne")}
                onToggle={toggleIsProtocolTypeDropdownOpen}
                onSelect={(_, value) =>
                  onProtocolTypeDropdownSelect(value.toString())
                }
                selections={protocolType}
                isOpen={isProtocolTypeDropdownOpen}
              >
                {protocolTypeOptions}
              </Select>
            </>
          )
        }
        canSelectAll
        onSelect={(rows) => setRows(rows)}
        columns={[
          {
            name: "name",
          },
          {
            name: "protocol",
            displayKey: "protocol",
            cellRenderer: (client) =>
              getProtocolName(t, client.protocol ?? "openid-connect"),
          },
          {
            name: "description",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyAddClientScopes")}
            instructions={t("emptyAddClientScopesInstructions")}
          />
        }
      />
    </Modal>
  );
};
