import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import React, { useState } from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods } from "react-hook-form";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { useTranslation } from "react-i18next";
import { LdapMapperGeneral } from "./shared/LdapMapperGeneral";

export type LdapMapperRoleGroupProps = {
  form: UseFormMethods;
};

export const LdapMapperRoleGroup = ({ form }: LdapMapperRoleGroupProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const [isMbAttTypeDropdownOpen, setIsMbAttTypeDropdownOpen] = useState(false);
  const [isModeDropdownOpen, setIsModeDropdownOpen] = useState(false);
  const [
    isRetrieveStratDropdownOpen,
    setIsRetrieveStratDropdownOpen,
  ] = useState(false);
  const [isClientIdDropdownOpen, setIsClientIdDropdownOpen] = useState(false);

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <LdapMapperGeneral form={form} />

        <FormGroup
          label={t("ldapRolesDN")}
          labelIcon={
            <HelpItem
              helpText={helpText("ldapRolesDnHelp")}
              forLabel={t("ldapRolesDN")}
              forID="kc-roles-dn"
            />
          }
          fieldId="kc-roles-dn"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-roles-dn"
            data-testid="roles-dn"
            name="config.roles-dn"
            ref={form.register}
          />
        </FormGroup>

        <FormGroup
          label={t("roleNameLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText={helpText("roleNameLdapAttributeHelp")}
              forLabel={t("roleNameLdapAttribute")}
              forID="kc-role-name-attribute"
            />
          }
          fieldId="kc-role-name-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-role-name-attribute"
            data-testid="role-name-attribute"
            name="config.role-name-ldap-attribute"
            ref={form.register}
          />
        </FormGroup>

        <FormGroup
          label={t("roleObjectClasses")}
          labelIcon={
            <HelpItem
              helpText={helpText("roleObjectClassesHelp")}
              forLabel={t("roleObjectClasses")}
              forID="kc-full-name-attribute"
            />
          }
          fieldId="kc-role-object-classes"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-role-object-classes"
            data-testid="role-object-classes"
            name="config.role-object-classes"
            ref={form.register}
          />
        </FormGroup>

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
            id="kc-membership-ldap-attribute"
            data-testid="membership-ldap-attribute"
            name="config.membership-ldap-attribute"
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
            defaultValue=""
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
            name="config.membership-user-ldap-attribute"
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
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-filter"
            data-testid="ldap-filter"
            name="config.ldap-filter"
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
            defaultValue=""
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
          label={t("userRolesRetrieveStrategy")}
          labelIcon={
            <HelpItem
              helpText={helpText("userRolesRetrieveStrategyHelp")}
              forLabel={t("userRolesRetrieveStrategy")}
              forID="kc-user-roles-retrieve-strategy"
            />
          }
          fieldId="kc-user-roles-retrieve-strategy"
        >
          <Controller
            name="config.user-roles-retrieve-strategy[0]"
            defaultValue=""
            control={form.control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-user-roles-retrieve-strategy"
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
            data-testid="member-of-attribute"
            name="config.memberof-ldap-attribute"
            ref={form.register}
          />
        </FormGroup>

        <FormGroup
          label={t("useRealmRolesMapping")}
          labelIcon={
            <HelpItem
              helpText={helpText("useRealmRolesMappingHelp")}
              forLabel={t("useRealmRolesMapping")}
              forID="kc-user-realm-roles"
            />
          }
          fieldId="kc-user-realm-roles"
          hasNoPaddingTop
        >
          <Controller
            name="config.use-realm-roles-mapping"
            defaultValue={["false"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-user-realm-roles"}
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
                <SelectOption key={0} value="account">
                  Need to fetch clients here
                </SelectOption>
                <SelectOption key={1} value="admin-cli">
                  These are placeholders
                </SelectOption>
              </Select>
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
