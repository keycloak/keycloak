import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import React, { useState } from "react";
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
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import { useParams } from "react-router-dom";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";

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
  reset: () => void;
  users: UserRepresentation[];
  clientRoles: RoleRepresentation[];
};

export const AuthorizationEvaluate = ({
  clients,
  clientRoles,
  clientName,
  users,
  reset,
}: ClientSettingsProps) => {
  const form = useFormContext<ResourceEvaluation>();
  const { control } = form;
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
    []
  );

  const evaluate = (formValues: ResourceEvaluation) => {
    const resEval: ResourceEvaluation = {
      roleIds: formValues.roleIds ?? [],
      userId: selectedUser?.id!,
      entitlements: false,
      context: formValues.context,
      resources: formValues.resources,
      clientId: selectedClient?.id!,
    };
    return adminClient.clients.evaluateResource(
      { id: clientId!, realm: realm.realm },
      resEval
    );
  };

  return (
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
                  selections={value}
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
              name="rolesIds"
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
              fieldId={name!}
            >
              <AttributeInput
                selectableValues={resources.map((item) => item.name!)}
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
              fieldId={name!}
            >
              <AttributeInput
                selectableValues={defaultContextAttributes.map(
                  (item) => item.name
                )}
                isKeySelectable
                name="context"
              />
            </FormGroup>
          </ExpandableSection>
          <ActionGroup>
            <Button data-testid="authorization-eval" type="submit">
              {t("evaluate")}
            </Button>
            <Button
              data-testid="authorization-revert"
              variant="link"
              onClick={reset}
            >
              {t("common:revert")}
            </Button>
            <Button
              data-testid="authorization-revert"
              variant="primary"
              onClick={reset}
              isDisabled
            >
              {t("lastEvaluation")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
