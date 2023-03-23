import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Alert,
  AlertVariant,
  Button,
  DescriptionList,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ExpandableRowContent,
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toPolicyDetails } from "../routes/PolicyDetails";
import { MoreLabel } from "./MoreLabel";
import { toUpperCase } from "../../util";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import useToggle from "../../utils/useToggle";
import { NewPolicyDialog } from "./NewPolicyDialog";
import { toCreatePolicy } from "../routes/NewPolicy";
import { toPermissionDetails } from "../routes/PermissionDetails";
import { SearchDropdown, SearchForm } from "./SearchDropdown";
import { DetailDescriptionLink } from "./DetailDescription";

type PoliciesProps = {
  clientId: string;
};

type ExpandablePolicyRepresentation = PolicyRepresentation & {
  dependentPolicies?: PolicyRepresentation[];
  isExpanded: boolean;
};

const DependentPoliciesRenderer = ({
  row,
}: {
  row: ExpandablePolicyRepresentation;
}) => {
  return (
    <>
      {row.dependentPolicies?.[0]?.name}{" "}
      <MoreLabel array={row.dependentPolicies} />
    </>
  );
};

export const AuthorizationPolicies = ({ clientId }: PoliciesProps) => {
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const navigate = useNavigate();

  const [policies, setPolicies] = useState<ExpandablePolicyRepresentation[]>();
  const [selectedPolicy, setSelectedPolicy] =
    useState<ExpandablePolicyRepresentation>();
  const [policyProviders, setPolicyProviders] =
    useState<PolicyProviderRepresentation[]>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState<SearchForm>({});
  const [newDialog, toggleDialog] = useToggle();

  useFetch(
    async () => {
      const policies = await adminClient.clients.listPolicies({
        first,
        max: max + 1,
        id: clientId,
        permission: "false",
        ...search,
      });

      return await Promise.all([
        adminClient.clients.listPolicyProviders({ id: clientId }),
        ...(policies || []).map(async (policy) => {
          const dependentPolicies =
            await adminClient.clients.listDependentPolicies({
              id: clientId,
              policyId: policy.id!,
            });

          return {
            ...policy,
            dependentPolicies,
            isExpanded: false,
          };
        }),
      ]);
    },
    ([providers, ...policies]) => {
      setPolicyProviders(
        providers.filter((p) => p.type !== "resource" && p.type !== "scope")
      );
      setPolicies(policies);
    },
    [key, search, first, max]
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:deletePolicy",
    children: (
      <>
        {t("deletePolicyConfirm")}
        {selectedPolicy?.dependentPolicies &&
          selectedPolicy.dependentPolicies.length > 0 && (
            <Alert
              variant="warning"
              isInline
              isPlain
              title={t("deletePolicyWarning")}
              className="pf-u-pt-lg"
            >
              <p className="pf-u-pt-xs">
                {selectedPolicy.dependentPolicies.map((policy) => (
                  <strong key={policy.id} className="pf-u-pr-md">
                    {policy.name}
                  </strong>
                ))}
              </p>
            </Alert>
          )}
      </>
    ),
    continueButtonLabel: "clients:confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPolicy({
          id: clientId,
          policyId: selectedPolicy?.id!,
        });
        addAlert(t("policyDeletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("clients:policyDeletedError", error);
      }
    },
  });

  if (!policies) {
    return <KeycloakSpinner />;
  }

  const noData = policies.length === 0;
  const searching = Object.keys(search).length !== 0;
  return (
    <PageSection variant="light" className="pf-u-p-0">
      <DeleteConfirm />
      {(!noData || searching) && (
        <>
          {newDialog && (
            <NewPolicyDialog
              policyProviders={policyProviders}
              onSelect={(p) =>
                navigate(
                  toCreatePolicy({ id: clientId, realm, policyType: p.type! })
                )
              }
              toggleDialog={toggleDialog}
            />
          )}

          <PaginatingTableToolbar
            count={policies.length}
            first={first}
            max={max}
            onNextClick={setFirst}
            onPreviousClick={setFirst}
            onPerPageSelect={(first, max) => {
              setFirst(first);
              setMax(max);
            }}
            toolbarItem={
              <>
                <ToolbarItem>
                  <SearchDropdown
                    types={policyProviders}
                    search={search}
                    onSearch={setSearch}
                  />
                </ToolbarItem>
                <ToolbarItem>
                  <Button data-testid="createPolicy" onClick={toggleDialog}>
                    {t("createPolicy")}
                  </Button>
                </ToolbarItem>
              </>
            }
          >
            {!noData && (
              <TableComposable aria-label={t("resources")} variant="compact">
                <Thead>
                  <Tr>
                    <Th />
                    <Th>{t("common:name")}</Th>
                    <Th>{t("common:type")}</Th>
                    <Th>{t("dependentPermission")}</Th>
                    <Th>{t("common:description")}</Th>
                    <Th />
                  </Tr>
                </Thead>
                {policies.map((policy, rowIndex) => (
                  <Tbody key={policy.id} isExpanded={policy.isExpanded}>
                    <Tr>
                      <Td
                        expand={{
                          rowIndex,
                          isExpanded: policy.isExpanded,
                          onToggle: (_, rowIndex) => {
                            const rows = policies.map((policy, index) =>
                              index === rowIndex
                                ? { ...policy, isExpanded: !policy.isExpanded }
                                : policy
                            );
                            setPolicies(rows);
                          },
                        }}
                      />
                      <Td data-testid={`name-column-${policy.name}`}>
                        <Link
                          to={toPolicyDetails({
                            realm,
                            id: clientId,
                            policyType: policy.type!,
                            policyId: policy.id!,
                          })}
                        >
                          {policy.name}
                        </Link>
                      </Td>
                      <Td>{toUpperCase(policy.type!)}</Td>
                      <Td>
                        <DependentPoliciesRenderer row={policy} />
                      </Td>
                      <Td>{policy.description}</Td>
                      <Td
                        actions={{
                          items: [
                            {
                              title: t("common:delete"),
                              onClick: async () => {
                                setSelectedPolicy(policy);
                                toggleDeleteDialog();
                              },
                            },
                          ],
                        }}
                      />
                    </Tr>
                    <Tr
                      key={`child-${policy.id}`}
                      isExpanded={policy.isExpanded}
                    >
                      <Td />
                      <Td colSpan={4}>
                        <ExpandableRowContent>
                          {policy.isExpanded && (
                            <DescriptionList
                              isHorizontal
                              className="keycloak_resource_details"
                            >
                              <DetailDescriptionLink
                                name="dependentPermission"
                                array={policy.dependentPolicies}
                                convert={(p) => p.name!}
                                link={(permission) =>
                                  toPermissionDetails({
                                    realm,
                                    id: clientId,
                                    permissionId: permission.id!,
                                    permissionType: permission.type!,
                                  })
                                }
                              />
                            </DescriptionList>
                          )}
                        </ExpandableRowContent>
                      </Td>
                    </Tr>
                  </Tbody>
                ))}
              </TableComposable>
            )}
          </PaginatingTableToolbar>
        </>
      )}
      {noData && searching && (
        <ListEmptyState
          isSearchVariant
          message={t("common:noSearchResults")}
          instructions={t("common:noSearchResultsInstructions")}
        />
      )}
      {noData && !searching && (
        <>
          {newDialog && (
            <NewPolicyDialog
              policyProviders={policyProviders?.filter(
                (p) => p.type !== "aggregate"
              )}
              onSelect={(p) =>
                navigate(
                  toCreatePolicy({ id: clientId, realm, policyType: p.type! })
                )
              }
              toggleDialog={toggleDialog}
            />
          )}
          <ListEmptyState
            message={t("emptyPolicies")}
            instructions={t("emptyPoliciesInstructions")}
            primaryActionText={t("createPolicy")}
            onPrimaryAction={toggleDialog}
          />
        </>
      )}
    </PageSection>
  );
};
