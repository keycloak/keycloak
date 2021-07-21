import {
  ActionGroup,
  Button,
  Divider,
  FormGroup,
  PageSection,
  Select,
  SelectGroup,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import type ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import type ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";
import type RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import React, { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useHistory, useParams } from "react-router-dom";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";

export const RoleMappingForm = () => {
  const { realm } = useRealm();
  const adminClient = useAdminClient();
  const history = useHistory();
  const { addAlert } = useAlerts();

  const { t } = useTranslation("client-scopes");
  const { register, handleSubmit, control, errors } = useForm();
  const { id } = useParams<{ id: string }>();

  const [roleOpen, setRoleOpen] = useState(false);

  const [clientsOpen, setClientsOpen] = useState(false);
  const [clients, setClients] = useState<ClientRepresentation[]>([]);
  const [selectedClient, setSelectedClient] = useState<ClientRepresentation>();
  const [clientRoles, setClientRoles] = useState<RoleRepresentation[]>([]);

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();

      const asyncFilter = async (
        predicate: (client: ClientRepresentation) => Promise<boolean>
      ) => {
        const results = await Promise.all(clients.map(predicate));
        return clients.filter((_, index) => results[index]);
      };

      const filteredClients = await asyncFilter(
        async (client) =>
          (await adminClient.clients.listRoles({ id: client.id! })).length > 0
      );

      filteredClients.map(
        (client) =>
          (client.toString = function () {
            return this.clientId!;
          })
      );
      return filteredClients;
    },
    (filteredClients) => setClients(filteredClients),
    []
  );

  useFetch(
    async () => {
      const client = selectedClient as ClientRepresentation;
      if (client && client.name !== "realmRoles") {
        const clientRoles = await adminClient.clients.listRoles({
          id: client.id!,
        });
        return clientRoles;
      } else {
        return await adminClient.roles.find();
      }
    },
    (clientRoles) => setClientRoles(clientRoles),
    [selectedClient]
  );

  const save = async (mapping: ProtocolMapperRepresentation) => {
    try {
      await adminClient.clientScopes.addProtocolMapper({ id }, mapping);
      addAlert(t("mapperCreateSuccess"));
    } catch (error) {
      addAlert(t("mapperCreateError", error));
    }
  };

  const createSelectGroup = (clients: ClientRepresentation[]) => {
    return [
      <SelectGroup key="role" label={t("roleGroup")}>
        <SelectOption
          key="realmRoles"
          value={
            {
              name: "realmRoles",
              toString: () => t("realmRoles"),
            } as ClientRepresentation
          }
        >
          {realm}
        </SelectOption>
      </SelectGroup>,
      <Divider key="divider" />,
      <SelectGroup key="group" label={t("clientGroup")}>
        {clients.map((client) => (
          <SelectOption key={client.id} value={client}>
            {client.clientId}
          </SelectOption>
        ))}
      </SelectGroup>,
    ];
  };

  const roleSelectOptions = () => {
    const createItem = (role: RoleRepresentation) => (
      <SelectOption key={role.id} value={role}>
        {role.name}
      </SelectOption>
    );
    return clientRoles.map((role) => createItem(role));
  };

  return (
    <>
      <ViewHeader
        titleKey="common:addMapper"
        subKey="client-scopes:addMapperExplain"
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="manage-clients"
        >
          <FormGroup
            label={t("common:mapperType")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:protocolMapper"
                forLabel={t("common:mapperType")}
                forID="protocolMapper"
              />
            }
            fieldId="protocolMapper"
          >
            <TextInput
              ref={register()}
              type="text"
              id="protocolMapper"
              name="protocolMapper"
              isReadOnly
            />
          </FormGroup>
          <FormGroup
            label={t("common:name")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:mapperName"
                forLabel={t("common:name")}
                forID="name"
              />
            }
            fieldId="name"
            isRequired
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              ref={register({ required: true })}
              type="text"
              id="name"
              name="name"
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
            />
          </FormGroup>
          <FormGroup
            label={t("common:role")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:role"
                forLabel={t("common:role")}
                forID="role"
              />
            }
            validated={errors["config.role"] ? "error" : "default"}
            helperTextInvalid={t("common:required")}
            fieldId="role"
          >
            <Split hasGutter>
              <SplitItem>
                <Select
                  toggleId="role"
                  onToggle={() => setClientsOpen(!clientsOpen)}
                  isOpen={clientsOpen}
                  variant={SelectVariant.typeahead}
                  typeAheadAriaLabel={t("selectASourceOfRoles")}
                  placeholderText={t("selectASourceOfRoles")}
                  isGrouped
                  onFilter={(evt) => {
                    const textInput = evt?.target.value || "";
                    if (textInput === "") {
                      return createSelectGroup(clients);
                    } else {
                      return createSelectGroup(
                        clients.filter((client) =>
                          client
                            .name!.toLowerCase()
                            .includes(textInput.toLowerCase())
                        )
                      );
                    }
                  }}
                  selections={selectedClient}
                  onClear={() => setSelectedClient(undefined)}
                  onSelect={(_, value) => {
                    if (value) {
                      setSelectedClient(value as ClientRepresentation);
                      setClientsOpen(false);
                    }
                  }}
                >
                  {createSelectGroup(clients)}
                </Select>
              </SplitItem>
              <SplitItem>
                <Controller
                  name="config.role"
                  defaultValue=""
                  control={control}
                  rules={{ required: true }}
                  render={({ onChange, value }) => (
                    <Select
                      onToggle={() => setRoleOpen(!roleOpen)}
                      isOpen={roleOpen}
                      variant={SelectVariant.typeahead}
                      placeholderText={
                        selectedClient && selectedClient.name !== "realmRoles"
                          ? t("clientRoles")
                          : t("selectARole")
                      }
                      isDisabled={!selectedClient}
                      typeAheadAriaLabel={t("selectARole")}
                      selections={value.name}
                      onSelect={(_, value) => {
                        onChange(value);
                        setRoleOpen(false);
                      }}
                      onClear={() => onChange("")}
                    >
                      {roleSelectOptions()}
                    </Select>
                  )}
                />
              </SplitItem>
            </Split>
          </FormGroup>
          <FormGroup
            label={t("newRoleName")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:newRoleName"
                forLabel={t("newRoleName")}
                forID="newRoleName"
              />
            }
            fieldId="newRoleName"
          >
            <TextInput
              ref={register()}
              type="text"
              id="newRoleName"
              name="config.new-role-name"
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:save")}
            </Button>
            <Button variant="link" onClick={() => history.push("..")}>
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
