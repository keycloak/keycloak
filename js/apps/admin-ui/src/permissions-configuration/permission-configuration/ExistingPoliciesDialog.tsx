import PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import {
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { CaretDownIcon, FilterIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { ListEmptyState, useFetch } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { capitalize, sortBy } from "lodash-es";
import useToggle from "../../utils/useToggle";

export type ExistingPoliciesDialogProps = {
  toggleDialog: () => void;
  onAssign: (policies: { policy: PolicyRepresentation }[]) => void;
  open: boolean;
  permissionClientId: string;
};

export const ExistingPoliciesDialog = ({
  toggleDialog,
  onAssign,
  open,
  permissionClientId,
}: ExistingPoliciesDialogProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const [rows, setRows] = useState<PolicyRepresentation[]>([]);
  const [filterType, setFilterType] = useState<string | undefined>(undefined);
  const [isFilterTypeDropdownOpen, toggleIsFilterTypeDropdownOpen] =
    useToggle();
  const [policies, setPolicies] = useState<PolicyRepresentation[]>([]);
  const [providers, setProviders] = useState<string[]>([]);

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.listPolicyProviders({
          id: permissionClientId!,
        }),
        adminClient.clients.listPolicies({
          id: permissionClientId!,
          permission: "false",
        }),
      ]),
    ([providers, policies]) => {
      const formattedProviders = providers
        .filter((p) => p.type !== "resource" && p.type !== "scope")
        .map((provider) => provider.name)
        .filter((name) => name !== undefined);
      setProviders(sortBy(formattedProviders));
      setPolicies(policies || []);
    },
    [permissionClientId],
  );

  const filteredPolicies = filterType
    ? policies.filter((policy) => capitalize(policy.type) === filterType)
    : policies;

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("assignExistingPolicies")}
      isOpen={open}
      onClose={toggleDialog}
      actions={[
        <>
          <Button
            id="modal-assignExistingPolicies"
            data-testid="confirm"
            key="assign"
            variant={ButtonVariant.primary}
            onClick={() => {
              const selectedPolicies = rows.map((policy) => ({ policy }));
              onAssign(selectedPolicies);
              toggleDialog();
            }}
            isDisabled={rows.length === 0}
          >
            {t("assign")}
          </Button>
          <Button
            id="modal-cancelExistingPolicies"
            data-testid="cancel"
            key="cancel"
            variant={ButtonVariant.link}
            onClick={() => {
              setRows([]);
              toggleDialog();
            }}
          >
            {t("cancel")}
          </Button>
        </>,
      ]}
    >
      <KeycloakDataTable
        loader={filteredPolicies}
        ariaLabelKey={t("chooseAPolicyType")}
        searchPlaceholderKey={t("searchClientAuthorizationPolicy")}
        isSearching={true}
        searchTypeComponent={
          <Dropdown
            onSelect={(event, value) => {
              setFilterType(value as string | undefined);
              toggleIsFilterTypeDropdownOpen();
            }}
            onOpenChange={toggleIsFilterTypeDropdownOpen}
            toggle={(ref) => (
              <MenuToggle
                ref={ref}
                data-testid="filter-type-dropdown-existingPolicies"
                id="toggle-id-10"
                onClick={toggleIsFilterTypeDropdownOpen}
                icon={<FilterIcon />}
                statusIcon={<CaretDownIcon />}
              >
                {filterType ? filterType : t("allTypes")}
              </MenuToggle>
            )}
            isOpen={isFilterTypeDropdownOpen}
          >
            <DropdownList>
              <DropdownItem
                data-testid="filter-type-dropdown-existingPolicies-all"
                key="all"
                onClick={() => setFilterType(undefined)}
              >
                {t("allTypes")}
              </DropdownItem>
              {providers.map((name) => (
                <DropdownItem
                  data-testid={`filter-type-dropdown-existingPolicies-${name}`}
                  key={name}
                  onClick={() => setFilterType(name)}
                >
                  {name}
                </DropdownItem>
              ))}
            </DropdownList>
          </Dropdown>
        }
        canSelectAll
        onSelect={(selectedRows) => setRows(selectedRows)}
        columns={[
          { name: "name", displayKey: t("name") },
          {
            name: "type",
            displayKey: t("type"),
            cellFormatters: [(value) => capitalize(String(value || ""))],
          },
          { name: "description", displayKey: t("description") },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyAssignExistingPolicies")}
            instructions={t("emptyAssignExistingPoliciesInstructions")}
          />
        }
      />
    </Modal>
  );
};
