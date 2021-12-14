import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import React, { useState } from "react";
import { useParams } from "react-router-dom";
import type { UserFederationLdapMapperParams } from "../../routes/UserFederationLdapMapper";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

export type LdapMapperRoleGroupProps = {
  form: UseFormMethods;
  type: string;
};

export const LdapMapperRoleGroup = ({
  form,
  type,
}: LdapMapperRoleGroupProps) => {
  const { t } = useTranslation("user-federation");
  const adminClient = useAdminClient();
  const [isMbAttTypeDropdownOpen, setIsMbAttTypeDropdownOpen] = useState(false);
  const [isModeDropdownOpen, setIsModeDropdownOpen] = useState(false);
  const [isRetrieveStratDropdownOpen, setIsRetrieveStratDropdownOpen] =
    useState(false);
  const [isClientIdDropdownOpen, setIsClientIdDropdownOpen] = useState(false);
  const [vendorType, setVendorType] = useState<string | undefined>();
  const [clients, setClients] = useState<ClientRepresentation[]>([]);
  const { id, mapperId } = useParams<UserFederationLdapMapperParams>();

  let isRole = true;
  const groupMapper = "group-ldap-mapper";

  if (type === groupMapper) {
    isRole = false;
  }

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();
      if (clients) {
        setClients(clients);
      }
      return clients;
    },
    (clients) => setClients(clients),
    []
  );

  useFetch(
    async () => {
      return await adminClient.components.findOne({ id });
    },
    (fetchedComponent) => {
      if (fetchedComponent) {
        const vendor = fetchedComponent.config?.vendor[0];
        setVendorType(vendor);
        if (mapperId === "new" && vendor === "ad") {
          form.setValue(
            isRole
              ? "config.role.object.classes[0]"
              : "config.group.object.classes[0]",
            "group"
          );
          form.setValue("config.membership.user.ldap.attribute[0]", "cn");
        }
      } else if (id) {
        throw new Error(t("common:notFound"));
      }
    },
    []
  );

  return (
    <>
      <FormGroup
        label={isRole ? t("ldapRolesDn") : t("ldapGroupsDn")}
        labelIcon={
          <HelpItem
            helpText={`user-federation-help:${
              isRole ? "ldapRolesDnHelp" : "ldapGroupsDnHelp"
            }`}
            fieldLabelId={`user-federation:${
              isRole ? "ldapRolesDn" : "ldapGroupsDn"
            }`}
          />
        }
        fieldId="kc-ldap-dn"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-ldap-dn"
          data-testid="ldap-dn"
          name={isRole ? "config.roles.dn[0]" : "config.groups.dn[0]"}
          ref={form.register({ required: true })}
          validated={
            isRole
              ? form.errors.config?.["roles-dn"]
                ? ValidatedOptions.error
                : ValidatedOptions.default
              : form.errors.config?.["groups-dn"]
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup
        label={
          isRole ? t("roleNameLdapAttribute") : t("groupNameLdapAttribute")
        }
        labelIcon={
          <HelpItem
            helpText={`user-federation-help:
              ${
                isRole
                  ? "roleNameLdapAttributeHelp"
                  : "groupNameLdapAttributeHelp"
              }`}
            fieldLabelId={`user-federation:${
              isRole ? "roleNameLdapAttribute" : "groupNameLdapAttribute"
            }`}
          />
        }
        fieldId="kc-name-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-name-attribute"
          data-testid="name-attribute"
          defaultValue="cn"
          name={
            isRole
              ? "config.role.name.ldap.attribute[0]"
              : "config.group.name.ldap.attribute[0]"
          }
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={isRole ? t("roleObjectClasses") : t("groupObjectClasses")}
        labelIcon={
          <HelpItem
            helpText={`user-federation-help:
              ${isRole ? "roleObjectClassesHelp" : "groupObjectClassesHelp"}
            `}
            fieldLabelId={`user-federation:${
              isRole ? "roleObjectClasses" : "groupObjectClasses"
            }`}
          />
        }
        fieldId="kc-object-classes"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-object-classes"
          data-testid="object-classes"
          defaultValue="groupOfNames"
          name={
            isRole
              ? "config.role.object.classes[0]"
              : "config.group.object.classes[0]"
          }
          ref={form.register}
        />
      </FormGroup>
      {!isRole && (
        <>
          <FormGroup
            label={t("preserveGroupInheritance")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:preserveGroupInheritanceHelp"
                fieldLabelId="user-federation:preserveGroupInheritance"
              />
            }
            fieldId="kc-preserve-inheritance"
            hasNoPaddingTop
          >
            <Controller
              name="config.preserve.group.inheritance"
              defaultValue={["true"]}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id={"kc-preserve-inheritance"}
                  isDisabled={false}
                  onChange={(value) => onChange([`${value}`])}
                  isChecked={value[0] === "true"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                />
              )}
            ></Controller>
          </FormGroup>
          <FormGroup
            label={t("ignoreMissingGroups")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:ignoreMissingGroupsHelp"
                fieldLabelId="user-federation:ignoreMissingGroups"
              />
            }
            fieldId="kc-ignore-missing"
            hasNoPaddingTop
          >
            <Controller
              name="config.ignore.missing.groups"
              defaultValue={["false"]}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id={"kc-ignore-missing"}
                  isDisabled={false}
                  onChange={(value) => onChange([`${value}`])}
                  isChecked={value[0] === "true"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                />
              )}
            ></Controller>
          </FormGroup>
        </>
      )}
      <FormGroup
        label={t("membershipLdapAttribute")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:membershipLdapAttributeHelp"
            fieldLabelId="user-federation:membershipLdapAttribute"
          />
        }
        fieldId="kc-membership-ldap-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          defaultValue="member"
          id="kc-membership-ldap-attribute"
          data-testid="membership-ldap-attribute"
          name="config.membership.ldap.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("membershipAttributeType")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:membershipAttributeTypeHelp"
            fieldLabelId="user-federation:membershipAttributeType"
          />
        }
        fieldId="kc-membership-attribute-type"
      >
        <Controller
          name="config.membership.attribute.type[0]"
          defaultValue="DN"
          control={form.control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-membership-attribute-type"
              onToggle={() =>
                setIsMbAttTypeDropdownOpen(!isMbAttTypeDropdownOpen)
              }
              isOpen={isMbAttTypeDropdownOpen}
              onSelect={(_, value) => {
                onChange(value as string);
                setIsMbAttTypeDropdownOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
            >
              <SelectOption key={0} value="DN">
                DN
              </SelectOption>
              <SelectOption key={1} value="UID">
                UID
              </SelectOption>
            </Select>
          )}
        ></Controller>
      </FormGroup>
      <FormGroup
        label={t("membershipUserLdapAttribute")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:membershipUserLdapAttributeHelp"
            fieldLabelId="user-federation:membershipUserLdapAttribute"
          />
        }
        fieldId="kc-membership-user-ldap-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-membership-user-ldap-attribute"
          data-testid="membership-user-ldap-attribute"
          defaultValue="uid"
          name="config.membership.user.ldap.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("ldapFilter")}
        labelIcon={
          <HelpItem
            helpText="user-federation:ldapFilterHelp"
            fieldLabelId="user-federation:ldapFilter"
          />
        }
        fieldId="kc-ldap-filter"
      >
        <TextInput
          type="text"
          id="kc-ldap-filter"
          data-testid="ldap-filter"
          name={
            isRole
              ? "config.roles.ldap.filter[0]"
              : "config.groups.ldap.filter[0]"
          }
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("mode")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:modeHelp"
            fieldLabelId="user-federation:mode"
          />
        }
        fieldId="kc-mode"
      >
        <Controller
          name="config.mode[0]"
          defaultValue="READ_ONLY"
          control={form.control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-mode"
              onToggle={() => setIsModeDropdownOpen(!isModeDropdownOpen)}
              isOpen={isModeDropdownOpen}
              onSelect={(_, value) => {
                onChange(value as string);
                setIsModeDropdownOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
            >
              <SelectOption key={0} value="READ_ONLY">
                READ_ONLY
              </SelectOption>
              <SelectOption key={1} value="LDAP_ONLY">
                LDAP_ONLY
              </SelectOption>
              <SelectOption key={2} value="IMPORT">
                IMPORT
              </SelectOption>
            </Select>
          )}
        ></Controller>
      </FormGroup>
      {isRole ? (
        <FormGroup
          label={t("userRolesRetrieveStrategy")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:userRolesRetrieveStrategyHelp"
              fieldLabelId="user-federation:userRolesRetrieveStrategy"
            />
          }
          fieldId="kc-user-retrieve-strategy"
        >
          <Controller
            name="config.user.roles.retrieve.strategy[0]"
            defaultValue="LOAD_ROLES_BY_MEMBER_ATTRIBUTE"
            control={form.control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-user-retrieve-strategy"
                onToggle={() =>
                  setIsRetrieveStratDropdownOpen(!isRetrieveStratDropdownOpen)
                }
                isOpen={isRetrieveStratDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsRetrieveStratDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption key={0} value="LOAD_ROLES_BY_MEMBER_ATTRIBUTE">
                  LOAD_ROLES_BY_MEMBER_ATTRIBUTE
                </SelectOption>
                <SelectOption
                  key={1}
                  value="GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE"
                >
                  GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE
                </SelectOption>
                <SelectOption
                  hidden={vendorType !== "ad"}
                  key={2}
                  value="LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY"
                >
                  LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY
                </SelectOption>
              </Select>
            )}
          ></Controller>
        </FormGroup>
      ) : (
        <FormGroup
          label={t("userGroupsRetrieveStrategy")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:userGroupsRetrieveStrategyHelp"
              fieldLabelId="user-federation:userGroupsRetrieveStrategy"
            />
          }
          fieldId="kc-user-retrieve-strategy"
        >
          <Controller
            name="config.user.roles.retrieve.strategy[0]"
            defaultValue="LOAD_GROUPS_BY_MEMBER_ATTRIBUTE"
            control={form.control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-user-retrieve-strategy"
                onToggle={() =>
                  setIsRetrieveStratDropdownOpen(!isRetrieveStratDropdownOpen)
                }
                isOpen={isRetrieveStratDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsRetrieveStratDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption key={0} value="LOAD_GROUPS_BY_MEMBER_ATTRIBUTE">
                  LOAD_GROUPS_BY_MEMBER_ATTRIBUTE
                </SelectOption>
                <SelectOption
                  key={1}
                  value="GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE"
                >
                  GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE
                </SelectOption>
                <SelectOption
                  hidden={vendorType !== "ad"}
                  key={2}
                  value="LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY"
                >
                  LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY
                </SelectOption>
              </Select>
            )}
          ></Controller>
        </FormGroup>
      )}
      <FormGroup
        label={t("memberofLdapAttribute")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:memberofLdapAttributeHelp"
            fieldLabelId="user-federation:memberofLdapAttribute"
          />
        }
        fieldId="kc-member-of-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-member-of-attribute"
          defaultValue="memberOf"
          data-testid="member-of-attribute"
          name="config.memberof.ldap.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      {isRole && (
        <>
          <FormGroup
            label={t("useRealmRolesMapping")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:useRealmRolesMappingHelp"
                fieldLabelId="user-federation:useRealmRolesMapping"
              />
            }
            fieldId="kc-use-realm-roles"
            hasNoPaddingTop
          >
            <Controller
              name="config.use.realm.roles.mapping"
              defaultValue={["true"]}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id={"kc-use-realm-roles"}
                  isDisabled={false}
                  onChange={(value) => onChange([`${value}`])}
                  isChecked={value[0] === "true"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                />
              )}
            ></Controller>
          </FormGroup>
          <FormGroup
            label={t("common:clientId")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:clientIdHelp"
                fieldLabelId="clientId"
              />
            }
            fieldId="kc-client-id"
          >
            <Controller
              name="config.client.id[0]"
              defaultValue=""
              control={form.control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-client-id"
                  onToggle={() =>
                    setIsClientIdDropdownOpen(!isClientIdDropdownOpen)
                  }
                  isOpen={isClientIdDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setIsClientIdDropdownOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                >
                  {clients.map((client) => (
                    <SelectOption key={client.id} value={client.id}>
                      {client.clientId}
                    </SelectOption>
                  ))}
                </Select>
              )}
            ></Controller>
          </FormGroup>
        </>
      )}
      {!isRole && (
        <>
          <FormGroup
            label={t("mappedGroupAttributes")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:mappedGroupAttributesHelp"
                fieldLabelId="user-federation:mappedGroupAttributes"
              />
            }
            fieldId="kc-mapped-attributes"
          >
            <TextInput
              type="text"
              id="kc-mapped-attributes"
              data-testid="mapped-attributes"
              name="config.mapped.group.attributes[0]"
              ref={form.register}
            />
          </FormGroup>
          <FormGroup
            label={t("dropNonexistingGroupsDuringSync")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:dropNonexistingGroupsDuringSyncHelp"
                fieldLabelId="user-federation:dropNonexistingGroupsDuringSync"
              />
            }
            fieldId="kc-drop-nonexisting"
            hasNoPaddingTop
          >
            <Controller
              name="config.drop.non.existing.groups.during.sync"
              defaultValue={["false"]}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id={"kc-drop-nonexisting"}
                  isDisabled={false}
                  onChange={(value) => onChange([`${value}`])}
                  isChecked={value[0] === "true"}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                />
              )}
            ></Controller>
          </FormGroup>
          <FormGroup
            label={t("groupsPath")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:groupsPathHelp"
                fieldLabelId="user-federation:groupsPath"
              />
            }
            fieldId="kc-path"
            isRequired
          >
            <TextInput
              isRequired
              type="text"
              id="kc-path"
              data-testid="path"
              defaultValue="/"
              name="config.groups.path[0]"
              ref={form.register({ required: true })}
              validated={
                form.errors.config?.["groups-path"]
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
            />
          </FormGroup>
        </>
      )}
    </>
  );
};
