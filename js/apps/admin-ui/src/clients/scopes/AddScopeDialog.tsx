import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import { KeycloakSelect } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Modal,
  ModalVariant,
  SelectOption,
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
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import useToggle from "../../utils/useToggle";
import { getProtocolName } from "../utils";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";

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
  OID4VC = "OpenID4VC",
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
  const isFeatureEnabled = useIsFeatureEnabled();
  const [addToggle, setAddToggle] = useState(false);
  const [rows, setRows] = useState<ClientScopeRepresentation[]>([]);
  const [filterType, setFilterType] = useState(FilterType.Name);
  const [protocolType, setProtocolType] = useState(ProtocolType.All);

  const isOid4vcEnabled = isFeatureEnabled(Feature.OpenId4VCI);

  const [isFilterTypeDropdownOpen, toggleIsFilterTypeDropdownOpen] =
    useToggle();

  const [isProtocolTypeDropdownOpen, toggleIsProtocolTypeDropdownOpen] =
    useToggle(false);

  const clientScopes = useMemo(() => {
    if (protocolType === ProtocolType.OpenIDConnect) {
      return scopes.filter((item) => item.protocol === "openid-connect");
    } else if (protocolType === ProtocolType.SAML) {
      return scopes.filter((item) => item.protocol === "saml");
    } else if (protocolType === ProtocolType.OID4VC) {
      return scopes.filter((item) => item.protocol === "oid4vc");
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
    } else if (protocolType === ProtocolType.OID4VC) {
      setProtocolType(ProtocolType.OID4VC);
    } else if (protocolType === ProtocolType.All) {
      setProtocolType(ProtocolType.All);
    }

    toggleIsProtocolTypeDropdownOpen();
  };

  const protocolTypeOptions = useMemo(() => {
    const options = [
      <SelectOption key={1} value={ProtocolType.SAML}>
        {t("protocolTypes.saml")}
      </SelectOption>,
      <SelectOption key={2} value={ProtocolType.OpenIDConnect}>
        {t("protocolTypes.openid-connect")}
      </SelectOption>,
    ];

    if (isOid4vcEnabled) {
      options.push(
        <SelectOption key={3} value={ProtocolType.OID4VC}>
          {t("protocolTypes.oid4vc")}
        </SelectOption>,
      );
    }

    options.push(
      <SelectOption key={4} value={ProtocolType.All}>
        {t("protocolTypes.all")}
      </SelectOption>,
    );

    return options;
  }, [t, isOid4vcEnabled]);

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
                popperProps={{
                  direction: "up",
                }}
                onOpenChange={(isOpen) => setAddToggle(isOpen)}
                className="keycloak__client-scopes-add__add-dropdown"
                key="add-dropdown"
                isOpen={addToggle}
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    isDisabled={rows.length === 0}
                    onClick={() => setAddToggle(!addToggle)}
                    variant="primary"
                    id="add-dropdown"
                    data-testid="add-dropdown"
                    statusIcon={<CaretUpIcon />}
                  >
                    {t("add")}
                  </MenuToggle>
                )}
              >
                <DropdownList>
                  {clientScopeTypesDropdown(t, action)}
                </DropdownList>
              </Dropdown>,
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
            onOpenChange={toggleIsFilterTypeDropdownOpen}
            toggle={(ref) => (
              <MenuToggle
                ref={ref}
                data-testid="filter-type-dropdown"
                id="toggle-id-9"
                onClick={toggleIsFilterTypeDropdownOpen}
                icon={<FilterIcon />}
                statusIcon={<CaretDownIcon />}
              >
                {filterType}
              </MenuToggle>
            )}
            isOpen={isFilterTypeDropdownOpen}
          >
            <DropdownList>
              <DropdownItem
                data-testid="filter-type-dropdown-item"
                key="filter-type"
              >
                {filterType === FilterType.Name ? t("protocol") : t("name")}
              </DropdownItem>
            </DropdownList>
          </Dropdown>
        }
        toolbarItem={
          filterType === FilterType.Protocol && (
            <>
              <Dropdown
                onSelect={() => {
                  onFilterTypeDropdownSelect(filterType);
                }}
                onOpenChange={toggleIsFilterTypeDropdownOpen}
                data-testid="filter-type-dropdown"
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    id="toggle-id-9"
                    onClick={toggleIsFilterTypeDropdownOpen}
                    statusIcon={<CaretDownIcon />}
                    icon={<FilterIcon />}
                  >
                    {filterType}
                  </MenuToggle>
                )}
                isOpen={isFilterTypeDropdownOpen}
              >
                <DropdownList>
                  <DropdownItem
                    data-testid="filter-type-dropdown-item"
                    key="filter-type"
                  >
                    {t("name")}
                  </DropdownItem>
                </DropdownList>
              </Dropdown>
              <KeycloakSelect
                className="kc-protocolType-select"
                aria-label={t("selectOne")}
                onToggle={toggleIsProtocolTypeDropdownOpen}
                onSelect={(value) =>
                  onProtocolTypeDropdownSelect(value.toString())
                }
                selections={protocolType}
                isOpen={isProtocolTypeDropdownOpen}
              >
                {protocolTypeOptions}
              </KeycloakSelect>
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
