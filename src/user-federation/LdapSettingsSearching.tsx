import {
  Form,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useState } from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

export const LdapSettingsSearching = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const [isEditModeDropdownOpen, setIsEditModeDropdownOpen] = useState(false);
  const [
    isUserLdapFilterModeDropdownOpen,
    setIsUserLdapFilterModeDropdownOpen,
  ] = useState(false);
  const [isSearchScopeDropdownOpen, setIsSearchScopeDropdownOpen] = useState(
    false
  );

  const { register, handleSubmit, control } = useForm<
    ComponentRepresentation
  >();
  const onSubmit = (data: ComponentRepresentation) => {
    console.log(data);
  };

  return (
    <>
      {/* Cache settings */}
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
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
            name="editMode"
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
                // aria-label="Other"
                // isDisabled
              >
                <SelectOption key={0} value="Choose..." isPlaceholder />
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
            name="usersDn"
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
            name="usernameLdapAttribute"
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
            name="rdnLdapAttribute"
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
            name="uuidLdapAttribute"
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
            name="userObjectClasses"
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
          <Controller
            name="userLdapFilter"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-user-ldap-filter"
                required
                onToggle={() =>
                  setIsUserLdapFilterModeDropdownOpen(
                    !isUserLdapFilterModeDropdownOpen
                  )
                }
                isOpen={isUserLdapFilterModeDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsUserLdapFilterModeDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption key={0} value="Choose..." isPlaceholder />
                <SelectOption key={1} value="to do " />
              </Select>
            )}
          ></Controller>
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
            name="searchScope"
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
                <SelectOption key={0} value="Choose..." isPlaceholder />
                <SelectOption key={1} value="simple" />
                <SelectOption key={2} value="none" />
                <SelectOption key={5} value="Other" />
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
            name="readTimeout"
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
            name="pagination"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-console-pagination"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>

        <button type="submit">Test Submit</button>
      </Form>
    </>
  );
};
