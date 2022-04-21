import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import React, { useState, KeyboardEvent, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  Select,
  SelectVariant,
  SelectOption,
  PageSection,
  ActionGroup,
  Button,
  Switch,
  ExpandableSection,
  TextInput,
  ButtonVariant,
  InputGroup,
  Toolbar,
  ToolbarGroup,
  ToolbarItem,
  Divider,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { FormPanel } from "../../components/scroll-form/FormPanel";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type ResourceEvaluation from "@keycloak/keycloak-admin-client/lib/defs/resourceEvaluation";
import { useRealm } from "../../context/realm-context/RealmContext";
import { KeyBasedAttributeInput } from "./KeyBasedAttributeInput";
import { defaultContextAttributes } from "../utils";
import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { TableComposable, Th, Thead, Tr } from "@patternfly/react-table";
import "./auth-evaluate.css";
import { AuthorizationEvaluateResource } from "./AuthorizationEvaluateResource";
import { SearchIcon } from "@patternfly/react-icons";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

interface EvaluateFormInputs
  extends Omit<ResourceEvaluation, "context" | "resources"> {
  alias: string;
  authScopes: string[];
  context: {
    attributes: Record<string, string>[];
  };
  resources: Record<string, string>[];
  client: ClientRepresentation;
  user: UserRepresentation;
}

export type AttributeType = {
  key: string;
  name: string;
  custom?: boolean;
  values?: {
    [key: string]: string;
  }[];
};

type ClientSettingsProps = {
  client: ClientRepresentation;
  save: () => void;
};

export type AttributeForm = Omit<
  EvaluateFormInputs,
  "context" | "resources"
> & {
  context: {
    attributes?: KeyValueType[];
  };
  resources?: KeyValueType[];
};

type Props = ClientSettingsProps & EvaluationResultRepresentation;

enum ResultsFilter {
  All = "ALL",
  StatusDenied = "STATUS_DENIED",
  StatusPermitted = "STATUS_PERMITTED",
}

function filterResults(
  results: EvaluationResultRepresentation[],
  filter: ResultsFilter
) {
  switch (filter) {
    case ResultsFilter.StatusPermitted:
      return results.filter(({ status }) => status === "PERMIT");
    case ResultsFilter.StatusDenied:
      return results.filter(({ status }) => status === "DENY");
    default:
      return results;
  }
}

export const AuthorizationEvaluate = ({ client }: Props) => {
  const form = useFormContext<EvaluateFormInputs>();
  const { control, reset, trigger } = form;
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const realm = useRealm();

  const [clientsDropdownOpen, setClientsDropdownOpen] = useState(false);
  const [scopesDropdownOpen, setScopesDropdownOpen] = useState(false);

  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [roleDropdownOpen, setRoleDropdownOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [applyToResourceType, setApplyToResourceType] = useState(false);
  const [resources, setResources] = useState<ResourceRepresentation[]>([]);
  const [scopes, setScopes] = useState<ScopeRepresentation[]>([]);
  const [evaluateResults, setEvaluateResults] = useState<
    EvaluationResultRepresentation[]
  >([]);
  const [showEvaluateResults, setShowEvaluateResults] = useState(false);
  const searchQueryRef = useRef("");
  const [searchQuery, setSearchQuery] = useState("");
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(key + 1);
  };

  const [filter, setFilter] = useState(ResultsFilter.All);

  const [clients, setClients] = useState<ClientRepresentation[]>([]);
  const [clientRoles, setClientRoles] = useState<RoleRepresentation[]>([]);
  const [users, setUsers] = useState<UserRepresentation[]>([]);

  const filteredResources = useMemo(
    () =>
      filterResults(evaluateResults, filter).filter(
        ({ resource }) => resource?.name?.includes(searchQuery) ?? false
      ),
    [evaluateResults, filter, searchQuery]
  );

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.find(),
        adminClient.roles.find(),
        adminClient.users.find(),
      ]),
    ([clients, roles, users]) => {
      setClients(clients);
      setClientRoles(roles);
      setUsers(users);
    },
    []
  );

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.listResources({
          id: client.id!,
        }),
        adminClient.clients.listAllScopes({
          id: client.id!,
        }),
      ]),
    ([resources, scopes]) => {
      setResources(resources);
      setScopes(scopes);
    },
    [key, filter]
  );

  const evaluate = async () => {
    if (!(await trigger())) {
      return;
    }
    const formValues = form.getValues();
    const keys = formValues.resources.map(({ key }) => key);
    const resEval: ResourceEvaluation = {
      roleIds: formValues.roleIds ?? [],
      clientId: formValues.client.id!,
      userId: formValues.user.id!,
      resources: formValues.resources.filter((resource) =>
        keys.includes(resource.name!)
      ),
      entitlements: false,
      context: {
        attributes: Object.fromEntries(
          formValues.context.attributes
            .filter((item) => item.key || item.value !== "")
            .map(({ key, value }) => [key, value])
        ),
      },
    };

    const evaluation = await adminClient.clients.evaluateResource(
      { id: client.id!, realm: realm.realm },
      resEval
    );

    setEvaluateResults(evaluation.results);
    setShowEvaluateResults(true);
    return evaluateResults;
  };

  const confirmSearchQuery = () => {
    setSearchQuery(searchQueryRef.current);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      confirmSearchQuery();
    }
  };

  const handleInputChange = (value: string) => {
    searchQueryRef.current = value;
  };

  const noEvaluatedData = evaluateResults.length === 0;
  const noFilteredData = filteredResources.length === 0;

  return showEvaluateResults ? (
    <PageSection>
      <Toolbar>
        <ToolbarGroup className="providers-toolbar">
          <ToolbarItem>
            <InputGroup>
              <TextInput
                name={"inputGroupName"}
                id={"inputGroupName"}
                type="search"
                aria-label={t("common:search")}
                placeholder={t("common:search")}
                onChange={handleInputChange}
                onKeyDown={handleKeyDown}
              />
              <Button
                variant={ButtonVariant.control}
                aria-label={t("common:search")}
                onClick={() => confirmSearchQuery()}
              >
                <SearchIcon />
              </Button>
            </InputGroup>
          </ToolbarItem>
          <ToolbarItem>
            <Select
              width={300}
              data-testid="filter-type-select"
              isOpen={filterDropdownOpen}
              className="kc-filter-type-select"
              variant={SelectVariant.single}
              onToggle={() => setFilterDropdownOpen(!filterDropdownOpen)}
              onSelect={(_, value) => {
                setFilter(value as ResultsFilter);
                setFilterDropdownOpen(false);
                refresh();
              }}
              selections={filter}
            >
              <SelectOption
                data-testid="all-results-option"
                value={ResultsFilter.All}
                isPlaceholder
              >
                {t("allResults")}
              </SelectOption>
              <SelectOption
                data-testid="result-permit-option"
                value={ResultsFilter.StatusPermitted}
              >
                {t("resultPermit")}
              </SelectOption>
              <SelectOption
                data-testid="result-deny-option"
                value={ResultsFilter.StatusDenied}
              >
                {t("resultDeny")}
              </SelectOption>
            </Select>
          </ToolbarItem>
        </ToolbarGroup>
      </Toolbar>
      {!noFilteredData && (
        <TableComposable aria-label={t("evaluationResults")}>
          <Thead>
            <Tr>
              <Th />
              <Th>{t("resource")}</Th>
              <Th>{t("overallResults")}</Th>
              <Th>{t("scopes")}</Th>
              <Th />
            </Tr>
          </Thead>
          {filteredResources.map((resource, rowIndex) => (
            <AuthorizationEvaluateResource
              key={rowIndex}
              rowIndex={rowIndex}
              resource={resource}
              evaluateResults={evaluateResults}
            />
          ))}
        </TableComposable>
      )}
      {(noFilteredData || noEvaluatedData) && (
        <>
          <Divider />
          <ListEmptyState
            isSearchVariant
            message={t("common:noSearchResults")}
            instructions={t("common:noSearchResultsInstructions")}
          />
        </>
      )}
      <ActionGroup className="kc-evaluated-options">
        <Button
          data-testid="authorization-eval"
          id="back-btn"
          onClick={() => setShowEvaluateResults(false)}
        >
          {t("common:back")}
        </Button>
        <Button
          data-testid="authorization-reevaluate"
          id="reevaluate-btn"
          variant="secondary"
          onClick={() => evaluate()}
        >
          {t("clients:reevaluate")}
        </Button>
        <Button data-testid="authorization-revert" variant="secondary">
          {t("showAuthData")}
        </Button>
      </ActionGroup>
    </PageSection>
  ) : (
    <PageSection>
      <FormPanel
        className="kc-identity-information"
        title={t("clients:identityInformation")}
      >
        <FormAccess
          isHorizontal
          role="manage-clients"
          onSubmit={form.handleSubmit(evaluate)}
        >
          <FormGroup
            label={t("client")}
            isRequired
            labelIcon={
              <HelpItem
                helpText="clients-help:client"
                fieldLabelId="clients:client"
              />
            }
            fieldId="client"
          >
            <Controller
              name="client"
              defaultValue={client}
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="client"
                  onToggle={setClientsDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value);
                    setClientsDropdownOpen(false);
                  }}
                  selections={value.clientId}
                  variant={SelectVariant.typeahead}
                  aria-label={t("client")}
                  isOpen={clientsDropdownOpen}
                >
                  {clients.map((client) => (
                    <SelectOption
                      selected={client.id === value.id}
                      key={client.clientId}
                      value={client}
                    >
                      {client.clientId}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("user")}
            isRequired
            labelIcon={
              <HelpItem
                helpText="clients-help:userSelect"
                fieldLabelId="clients:userSelect"
              />
            }
            fieldId="user"
          >
            <Controller
              name="user"
              rules={{
                required: true,
              }}
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="user"
                  placeholderText={t("selectAUser")}
                  onToggle={setUserDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value);
                    setUserDropdownOpen(false);
                  }}
                  selections={value.username}
                  variant={SelectVariant.typeahead}
                  aria-label={t("user")}
                  isOpen={userDropdownOpen}
                >
                  {users.map((user) => (
                    <SelectOption
                      selected={user.username === value.username}
                      key={user.username}
                      value={user}
                    >
                      {user.username}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("roles")}
            labelIcon={
              <HelpItem
                helpText="clients-help:roles"
                fieldLabelId="clients:roles"
              />
            }
            fieldId="realmRole"
          >
            <Controller
              name="roleIds"
              placeholderText={t("selectARole")}
              control={control}
              defaultValue={[]}
              render={({ onChange, value }) => (
                <Select
                  variant={SelectVariant.typeaheadMulti}
                  toggleId="role"
                  onToggle={setRoleDropdownOpen}
                  selections={value}
                  onSelect={(_, v) => {
                    const option = v.toString();
                    if (value.includes(option)) {
                      onChange(value.filter((item: string) => item !== option));
                    } else {
                      onChange([...value, option]);
                    }
                    setRoleDropdownOpen(false);
                  }}
                  onClear={(event) => {
                    event.stopPropagation();
                    onChange([]);
                  }}
                  aria-label={t("realmRole")}
                  isOpen={roleDropdownOpen}
                >
                  {clientRoles.map((role) => (
                    <SelectOption
                      selected={role.name === value}
                      key={role.name}
                      value={role.name}
                    />
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel className="kc-permissions" title={t("permissions")}>
        <FormAccess isHorizontal role="manage-clients">
          <FormGroup
            label={t("applyToResourceType")}
            fieldId="applyToResourceType"
            labelIcon={
              <HelpItem
                helpText="clients-help:applyToResourceType"
                fieldLabelId="clients:applyToResourceType"
              />
            }
          >
            <Switch
              id="applyToResource-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={applyToResourceType}
              onChange={setApplyToResourceType}
            />
          </FormGroup>

          {!applyToResourceType ? (
            <FormGroup
              label={t("resourcesAndAuthScopes")}
              id="resourcesAndAuthScopes"
              isRequired
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:contextualAttributes")}
                  fieldLabelId={`resourcesAndAuthScopes`}
                />
              }
              helperTextInvalid={t("common:required")}
              fieldId="resourcesAndAuthScopes"
            >
              <KeyBasedAttributeInput
                selectableValues={resources.map<AttributeType>((item) => ({
                  name: item.name!,
                  key: item._id!,
                }))}
                resources={resources}
                name="resources"
              />
            </FormGroup>
          ) : (
            <>
              <FormGroup
                label={t("resourceType")}
                isRequired
                labelIcon={
                  <HelpItem
                    helpText="clients-help:resourceType"
                    fieldLabelId="clients:resourceType"
                  />
                }
                fieldId="client"
              >
                <KeycloakTextInput
                  type="text"
                  id="alias"
                  name="alias"
                  data-testid="alias"
                  ref={form.register({ required: true })}
                />
              </FormGroup>
              <FormGroup
                label={t("authScopes")}
                labelIcon={
                  <HelpItem
                    helpText="clients-help:scopesSelect"
                    fieldLabelId="clients:client"
                  />
                }
                fieldId="authScopes"
              >
                <Controller
                  name="authScopes"
                  defaultValue={[]}
                  control={control}
                  render={({ onChange, value }) => (
                    <Select
                      toggleId="authScopes"
                      onToggle={setScopesDropdownOpen}
                      onSelect={(_, v) => {
                        const option = v.toString();
                        if (value.includes(option)) {
                          onChange(
                            value.filter((item: string) => item !== option)
                          );
                        } else {
                          onChange([...value, option]);
                        }
                        setScopesDropdownOpen(false);
                      }}
                      selections={value}
                      variant={SelectVariant.typeaheadMulti}
                      aria-label={t("authScopes")}
                      isOpen={scopesDropdownOpen}
                    >
                      {scopes.map((scope) => (
                        <SelectOption
                          selected={scope.name === value}
                          key={scope.id}
                          value={scope.name}
                        />
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
            </>
          )}
          <ExpandableSection
            toggleText={t("contextualInfo")}
            onToggle={() => setIsExpanded(!isExpanded)}
            isExpanded={isExpanded}
          >
            <FormGroup
              label={t("contextualAttributes")}
              id="contextualAttributes"
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:contextualAttributes")}
                  fieldLabelId={`contextualAttributes`}
                />
              }
              helperTextInvalid={t("common:required")}
              fieldId="contextualAttributes"
            >
              <KeyBasedAttributeInput
                selectableValues={defaultContextAttributes}
                name="context.attributes"
              />
            </FormGroup>
          </ExpandableSection>
        </FormAccess>
        <ActionGroup>
          <Button
            data-testid="authorization-eval"
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
            isDisabled={form.getValues().resources?.every((e) => e.key === "")}
            onClick={() => evaluate()}
          >
            {t("evaluate")}
          </Button>
          <Button
            data-testid="authorization-revert"
            variant="link"
            onClick={() => reset()}
          >
            {t("common:revert")}
          </Button>
          <Button
            data-testid="authorization-revert"
            variant="primary"
            onClick={() => reset()}
            isDisabled
          >
            {t("lastEvaluation")}
          </Button>
        </ActionGroup>
      </FormPanel>
    </PageSection>
  );
};
