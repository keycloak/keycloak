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
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import type ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";

export type LdapMapperRoleGroupProps = {
  form: UseFormMethods;
  type: string;
};

export const LdapMapperRoleGroup = ({
  form,
  type,
}: LdapMapperRoleGroupProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;
  const adminClient = useAdminClient();
  const [isMbAttTypeDropdownOpen, setIsMbAttTypeDropdownOpen] = useState(false);
  const [isModeDropdownOpen, setIsModeDropdownOpen] = useState(false);
  const [isRetrieveStratDropdownOpen, setIsRetrieveStratDropdownOpen] =
    useState(false);
  const [isClientIdDropdownOpen, setIsClientIdDropdownOpen] = useState(false);
  const [clients, setClients] = useState<ClientRepresentation[]>([]);

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

  return (
    <>
      <FormGroup
        label={isRole ? t("ldapRolesDn") : t("ldapGroupsDn")}
        labelIcon={
          <HelpItem
            helpText={
              isRole
                ? helpText("ldapRolesDnHelp")
                : helpText("ldapGroupsDnHelp")
            }
            forLabel={isRole ? t("ldapRolesDN") : t("ldapGroupsDN")}
            forID="kc-ldap-dn"
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
          name={isRole ? "config.roles-dn[0]" : "config.groups-dn[0]"}
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
            helpText={
              isRole
                ? helpText("roleNameLdapAttributeHelp")
                : helpText("groupNameLdapAttributeHelp")
            }
            forLabel={
              isRole ? t("roleNameLdapAttribute") : t("groupNameLdapAttribute")
            }
            forID="kc-name-attribute"
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
              ? "config.role-name-ldap-attribute[0]"
              : "config.group-name-ldap-attribute[0]"
          }
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={isRole ? t("roleObjectClasses") : t("groupObjectClasses")}
        labelIcon={
          <HelpItem
            helpText={
              isRole
                ? helpText("roleObjectClassesHelp")
                : helpText("groupObjectClassesHelp")
            }
            forLabel={isRole ? t("roleObjectClasses") : t("groupObjectClasses")}
            forID="kc-object-classes"
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
          defaultValue="group"
          name={
            isRole
              ? "config.role-object-classes[0]"
              : "config.group-object-classes[0]"
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
                helpText={helpText("preserveGroupInheritanceHelp")}
                forLabel={t("preserveGroupInheritance")}
                forID="kc-preserve-inheritance"
              />
            }
            fieldId="kc-preserve-inheritance"
            hasNoPaddingTop
          >
            <Controller
              name="config.preserve-group-inheritance"
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
                helpText={helpText("ignoreMissingGroupsHelp")}
                forLabel={t("ignoreMissingGroups")}
                forID="kc-ignore-missing"
              />
            }
            fieldId="kc-ignore-missing"
            hasNoPaddingTop
          >
            <Controller
              name="config.ignore-missing-groups"
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
            helpText={helpText("membershipLdapAttributeHelp")}
            forLabel={t("membershipLdapAttribute")}
            forID="kc-membership-ldap-attribute"
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
          name="config.membership-ldap-attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("membershipAttributeType")}
        labelIcon={
          <HelpItem
            helpText={helpText("membershipAttributeTypeHelp")}
            forLabel={t("membershipAttributeType")}
            forID="kc-membership-attribute-type"
          />
        }
        fieldId="kc-membership-attribute-type"
      >
        <Controller
          name="config.membership-attribute-type[0]"
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
            helpText={helpText("membershipUserLdapAttributeHelp")}
            forLabel={t("membershipUserLdapAttribute")}
            forID="kc-membership-user-ldap-attribute"
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
          defaultValue="cn"
          name="config.membership-user-ldap-attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("ldapFilter")}
        labelIcon={
          <HelpItem
            helpText={helpText("ldapFilterHelp")}
            forLabel={t("ldapFilter")}
            forID="kc-ldap-filter"
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
              ? "config.roles-ldap-filter[0]"
              : "config.groups-ldap-filter[0]"
          }
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("mode")}
        labelIcon={
          <HelpItem
            helpText={helpText("modeHelp")}
            forLabel={t("mode")}
            forID="kc-mode"
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
      <FormGroup
        label={
          isRole
            ? t("userRolesRetrieveStrategy")
            : t("userGroupsRetrieveStrategy")
        }
        labelIcon={
          <HelpItem
            helpText={
              isRole
                ? helpText("userRolesRetrieveStrategyHelp")
                : helpText("userGroupsRetrieveStrategyHelp")
            }
            forLabel={
              isRole
                ? t("userRolesRetrieveStrategy")
                : t("userGroupsRetrieveStrategy")
            }
            forID="kc-user-retrieve-strategy"
          />
        }
        fieldId="kc-user-retrieve-strategy"
      >
        <Controller
          name={
            isRole
              ? "config.user-roles-retrieve-strategy[0]"
              : "config.user-groups-retrieve-strategy[0]"
          }
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
                key={2}
                value="LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY"
              >
                LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY
              </SelectOption>
            </Select>
          )}
        ></Controller>
      </FormGroup>
      <FormGroup
        label={t("memberofLdapAttribute")}
        labelIcon={
          <HelpItem
            helpText={helpText("memberofLdapAttributeHelp")}
            forLabel={t("memberofLdapAttribute")}
            forID="kc-member-of-attribute"
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
          name="config.memberof-ldap-attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      {isRole && (
        <>
          <FormGroup
            label={t("useRealmRolesMapping")}
            labelIcon={
              <HelpItem
                helpText={helpText("useRealmRolesMappingHelp")}
                forLabel={t("useRealmRolesMapping")}
                forID="kc-use-realm-roles"
              />
            }
            fieldId="kc-use-realm-roles"
            hasNoPaddingTop
          >
            <Controller
              name="config.use-realm-roles-mapping"
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
                helpText={helpText("clientIdHelp")}
                forLabel={t("common:clientId")}
                forID="kc-client-id"
              />
            }
            fieldId="kc-client-id"
          >
            <Controller
              name="config.client-id[0]"
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
                helpText={helpText("mappedGroupAttributesHelp")}
                forLabel={t("mappedGroupAttributes")}
                forID="kc-mapped-attributes"
              />
            }
            fieldId="kc-mapped-attributes"
          >
            <TextInput
              type="text"
              id="kc-mapped-attributes"
              data-testid="mapped-attributes"
              name="config.mapped-group-attributes[0]"
              ref={form.register}
            />
          </FormGroup>
          <FormGroup
            label={t("dropNonexistingGroupsDuringSync")}
            labelIcon={
              <HelpItem
                helpText={helpText("dropNonexistingGroupsDuringSyncHelp")}
                forLabel={t("dropNonexistingGroupsDuringSync")}
                forID="kc-drop-nonexisting"
              />
            }
            fieldId="kc-drop-nonexisting"
            hasNoPaddingTop
          >
            <Controller
              name="config.drop-non-existing-groups-during-sync"
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
                helpText={helpText("groupsPathHelp")}
                forLabel={t("groupsPath")}
                forID="kc-path"
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
              name="config.groups-path[0]"
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
