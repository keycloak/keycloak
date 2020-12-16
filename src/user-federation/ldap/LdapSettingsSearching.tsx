import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useEffect, useState } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useParams } from "react-router-dom";
import { convertToFormValues } from "../../util";

export const LdapSettingsSearching = () => {
  const { t } = useTranslation("user-federation");
  const adminClient = useAdminClient();
  const helpText = useTranslation("user-federation-help").t;
  const [isEditModeDropdownOpen, setIsEditModeDropdownOpen] = useState(false);
  const { id } = useParams<{ id: string }>();
  const [isSearchScopeDropdownOpen, setIsSearchScopeDropdownOpen] = useState(
    false
  );
  const { register, control, setValue } = useForm<ComponentRepresentation>();

  const convertSearchScopes = (scope: string) => {
    switch (scope) {
      case "1":
      default:
        return `${t("oneLevel")}`;
      case "2":
        return `${t("subtree")}`;
    }
  };

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", setValue);
        if (entry[1].searchScope) {
          setValue(
            "config.searchScope",
            convertSearchScopes(entry[1].searchScope[0])
          );
        }
      } else {
        setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("editMode")}
          labelIcon={
            <HelpItem
              helpText={helpText("editModeLdapHelp")}
              forLabel={t("editMode")}
              forID="kc-edit-mode"
            />
          }
          fieldId="kc-edit-mode"
        >
          <Controller
            name="config.editMode"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-edit-mode"
                required
                onToggle={() =>
                  setIsEditModeDropdownOpen(!isEditModeDropdownOpen)
                }
                isOpen={isEditModeDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsEditModeDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption
                  key={0}
                  value={t("common:choose")}
                  isPlaceholder
                />
                <SelectOption key={1} value="RACT_ONLY" />
                <SelectOption key={2} value="WRITABLE" />
                <SelectOption key={3} value="UNSYNCED" />
              </Select>
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("usersDN")}
          labelIcon={
            <HelpItem
              helpText={helpText("usersDNHelp")}
              forLabel={t("usersDN")}
              forID="kc-console-users-dn"
            />
          }
          fieldId="kc-console-users-dn"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-console-users-dn"
            name="config.usersDn"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("usernameLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText={helpText("usernameLdapAttributeHelp")}
              forLabel={t("usernameLdapAttribute")}
              forID="kc-username-ldap-attribute"
            />
          }
          fieldId="kc-username-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-username-ldap-attribute"
            name="config.usernameLDAPAttribute"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("rdnLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText={helpText("rdnLdapAttributeHelp")}
              forLabel={t("rdnLdapAttribute")}
              forID="kc-rdn-ldap-attribute"
            />
          }
          fieldId="kc-rdn-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-rdn-ldap-attribute"
            name="config.rdnLDAPAttribute"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("uuidLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText={helpText("uuidLdapAttributeHelp")}
              forLabel={t("uuidLdapAttribute")}
              forID="kc-uuid-ldap-attribute"
            />
          }
          fieldId="kc-uuid-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-uuid-ldap-attribute"
            name="config.uuidLDAPAttribute"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("userObjectClasses")}
          labelIcon={
            <HelpItem
              helpText={helpText("userObjectClassesHelp")}
              forLabel={t("userObjectClasses")}
              forID="kc-user-object-classes"
            />
          }
          fieldId="kc-user-object-classes"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-user-object-classes"
            name="config.userObjectClasses"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("userLdapFilter")}
          labelIcon={
            <HelpItem
              helpText={helpText("userLdapFilterHelp")}
              forLabel={t("userLdapFilter")}
              forID="kc-user-ldap-filter"
            />
          }
          fieldId="kc-user-ldap-filter"
        >
          <TextInput
            type="text"
            id="kc-user-ldap-filter"
            name="config.customUserSearchFilter"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("searchScope")}
          labelIcon={
            <HelpItem
              helpText={helpText("searchScopeHelp")}
              forLabel={t("searchScope")}
              forID="kc-search-scope"
            />
          }
          fieldId="kc-search-scope"
        >
          <Controller
            name="config.searchScope"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-search-scope"
                required
                onToggle={() =>
                  setIsSearchScopeDropdownOpen(!isSearchScopeDropdownOpen)
                }
                isOpen={isSearchScopeDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsSearchScopeDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption
                  key={0}
                  value={t("common:choose")}
                  isPlaceholder
                />
                <SelectOption key={1} value={t("oneLevel")} />
                <SelectOption key={2} value={t("subtree")} />
              </Select>
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("readTimeout")}
          labelIcon={
            <HelpItem
              helpText={helpText("readTimeoutHelp")}
              forLabel={t("readTimeout")}
              forID="kc-read-timeout"
            />
          }
          fieldId="kc-read-timeout"
        >
          <TextInput
            type="text"
            id="kc-read-timeout"
            name="config.readTimeout"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("pagination")}
          labelIcon={
            <HelpItem
              helpText={helpText("paginationHelp")}
              forLabel={t("pagination")}
              forID="kc-console-pagination"
            />
          }
          fieldId="kc-console-pagination"
          hasNoPaddingTop
        >
          <Controller
            name="config.pagination"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-console-pagination"}
                isChecked={value[0] === "true"}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
