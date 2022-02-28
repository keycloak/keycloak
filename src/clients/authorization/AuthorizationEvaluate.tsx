import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import React, { useState, KeyboardEvent } from "react";
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
import { AttributeInput } from "../../components/attribute-input/AttributeInput";
import { defaultContextAttributes } from "../utils";
import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import { useParams } from "react-router-dom";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import type { KeyValueType } from "../../components/attribute-form/attribute-convert";
import { TableComposable, Th, Thead, Tr } from "@patternfly/react-table";
import "./auth-evaluate.css";
import { AuthorizationEvaluateResource } from "./AuthorizationEvaluateResource";
import { SearchIcon } from "@patternfly/react-icons";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";

interface EvaluateFormInputs
  extends Omit<ResourceEvaluation, "context" | "resources"> {
  applyToResource: boolean;
  alias: string;
  authScopes: string[];
  context: {
    attributes: Record<string, string>[];
  };
  resources: Record<string, string>[];
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
  clients: ClientRepresentation[];
  clientName?: string;
  save: () => void;
  users: UserRepresentation[];
  clientRoles: RoleRepresentation[];
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

export const AuthorizationEvaluate = ({
  clients,
  clientRoles,
  clientName,
  users,
}: Props) => {
  const form = useFormContext<EvaluateFormInputs>();
  const { control, reset, trigger } = form;
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const realm = useRealm();
  const { clientId } = useParams<{ clientId: string }>();

  const [clientsDropdownOpen, setClientsDropdownOpen] = useState(false);
  const [scopesDropdownOpen, setScopesDropdownOpen] = useState(false);

  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [roleDropdownOpen, setRoleDropdownOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [applyToResourceType, setApplyToResourceType] = useState(false);
  const [resources, setResources] = useState<ResourceRepresentation[]>([]);
  const [scopes, setScopes] = useState<ScopeRepresentation[]>([]);
  const [selectedClient, setSelectedClient] = useState<ClientRepresentation>();
  const [selectedUser, setSelectedUser] = useState<UserRepresentation>();
  const [evaluateResults, setEvaluateResults] = useState<
    EvaluationResultRepresentation[]
  >([]);
  const [showEvaluateResults, setShowEvaluateResults] = useState(false);
  const [searchVal, setSearchVal] = useState("");
  const [filteredResources, setFilteredResources] = useState<
    EvaluationResultRepresentation[]
  >([]);
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(new Date().getTime());
  };

  const FilterType = {
    allResults: t("allResults"),
    resultPermit: t("resultPermit"),
    resultDeny: t("resultDeny"),
  };

  const [filterType, setFilterType] = useState(FilterType.allResults);

  useFetch(
    async () =>
      Promise.all([
        adminClient.clients.listResources({
          id: clientId,
        }),
        adminClient.clients.listAllScopes({
          id: clientId,
        }),
      ]),
    ([resources, scopes]) => {
      setResources(resources);
      setScopes(scopes);
    },
    [key, filterType]
  );

  const evaluate = async () => {
    if (!(await trigger())) {
      return;
    }
    const formValues = form.getValues();
    const keys = formValues.resources.map(({ key }) => key);
    const resEval: ResourceEvaluation = {
      roleIds: formValues.roleIds ?? [],
      clientId: selectedClient ? selectedClient.id! : clientId,
      userId: selectedUser?.id!,
      resources: resources.filter((resource) => keys.includes(resource.name!)),
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
      { id: clientId!, realm: realm.realm },
      resEval
    );

    setEvaluateResults(evaluation.results);
    setShowEvaluateResults(true);
    return evaluateResults;
  };

  const onSearch = () => {
    if (searchVal !== "") {
      setSearchVal(searchVal);
      const filtered = evaluateResults.filter((resource) =>
        resource.resource?.name?.includes(searchVal)
      );
      setFilteredResources(filtered);
    } else {
      setSearchVal("");
      setFilteredResources(evaluateResults);
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

  const noEvaluatedData = evaluateResults.length === 0;
  const noFilteredData = filteredResources.length === 0;

  const options = [
    <SelectOption
      key={1}
      data-testid="all-results-option"
      value={FilterType.allResults}
      isPlaceholder
    />,
    <SelectOption
      data-testid="result-permit-option"
      key={2}
      value={FilterType.resultPermit}
    />,
    <SelectOption
      data-testid="result-deny-option"
      key={3}
      value={FilterType.resultDeny}
    />,
  ];

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
                onClick={() => onSearch()}
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
                if (value === FilterType.allResults) {
                  setFilterType(FilterType.allResults);
                } else if (value === FilterType.resultPermit) {
                  const filterPermit = evaluateResults.filter(
                    (resource) => resource.status === "PERMIT"
                  );
                  setFilteredResources(filterPermit);
                  setFilterType(FilterType.resultPermit);

                  refresh();
                } else if (value === FilterType.resultDeny) {
                  const filterDeny = evaluateResults.filter(
                    (resource) => resource.status === "DENY"
                  );
                  setFilterType(FilterType.resultDeny);
                  setFilteredResources(filterDeny);
                  refresh();
                }
                setFilterDropdownOpen(false);
              }}
              selections={filterType}
            >
              {options}
            </Select>
          </ToolbarItem>
        </ToolbarGroup>
      </Toolbar>
      {!noEvaluatedData && !noFilteredData && (
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
          {(filterType == FilterType.allResults
            ? evaluateResults
            : filteredResources
          ).map((resource, rowIndex) => (
            <AuthorizationEvaluateResource
              key={rowIndex}
              rowIndex={rowIndex}
              resource={resource}
              evaluateResults={evaluateResults}
            />
          ))}
        </TableComposable>
      )}
      {noEvaluatedData ||
        (noFilteredData && (
          <ListEmptyState
            isSearchVariant
            message={t("common:noSearchResults")}
            instructions={t("common:noSearchResultsInstructions")}
          />
        ))}
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
              name="clientId"
              rules={{
                validate: (value) => value.length > 0,
              }}
              defaultValue={clientName}
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="client"
                  onToggle={setClientsDropdownOpen}
                  onSelect={(_, value) => {
                    setSelectedClient(value as ClientRepresentation);
                    onChange((value as ClientRepresentation).clientId);
                    setClientsDropdownOpen(false);
                  }}
                  selections={selectedClient === value ? value : clientName}
                  variant={SelectVariant.typeahead}
                  aria-label={t("client")}
                  isOpen={clientsDropdownOpen}
                >
                  {clients.map((client) => (
                    <SelectOption
                      selected={client === value}
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
            fieldId="loginTheme"
          >
            <Controller
              name="userId"
              rules={{
                validate: (value) => value.length > 0,
              }}
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="user"
                  placeholderText={t("selectAUser")}
                  onToggle={setUserDropdownOpen}
                  onSelect={(_, value) => {
                    setSelectedUser(value as UserRepresentation);
                    onChange((value as UserRepresentation).username);
                    setUserDropdownOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.typeahead}
                  aria-label={t("user")}
                  isOpen={userDropdownOpen}
                >
                  {users.map((user) => (
                    <SelectOption
                      selected={user.username === value}
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
            <Controller
              name="applyToResource"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  id="applyToResource-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => {
                    onChange(value.toString());
                    setApplyToResourceType(value);
                  }}
                />
              )}
            />
          </FormGroup>

          {!applyToResourceType && (
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
              <AttributeInput
                selectableValues={resources.map<AttributeType>((item) => ({
                  name: item.name!,
                  key: item._id!,
                }))}
                resources={resources}
                isKeySelectable
                name="resources"
              />
            </FormGroup>
          )}
          {applyToResourceType && (
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
                <TextInput
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
              <AttributeInput
                selectableValues={defaultContextAttributes}
                isKeySelectable
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
