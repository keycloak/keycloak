import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useState } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { UseFormMethods, Controller } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type LdapSettingsSearchingProps = {
  form: UseFormMethods;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const LdapSettingsSearching = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsSearchingProps) => {
  const { t } = useTranslation("user-federation");
  const { t: helpText } = useTranslation("user-federation-help");

  const [isSearchScopeDropdownOpen, setIsSearchScopeDropdownOpen] =
    useState(false);
  const [isEditModeDropdownOpen, setIsEditModeDropdownOpen] = useState(false);

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("ldapSearchingAndUpdatingSettings")}
          description={helpText("ldapSearchingAndUpdatingSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("editMode")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:editModeLdapHelp"
              fieldLabelId="user-federation:editMode"
            />
          }
          fieldId="kc-edit-mode"
          isRequired
        >
          <Controller
            name="config.editMode[0]"
            defaultValue=""
            control={form.control}
            rules={{
              required: { value: true, message: t("validateEditMode") },
            }}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-edit-mode"
                required
                onToggle={() =>
                  setIsEditModeDropdownOpen(!isEditModeDropdownOpen)
                }
                isOpen={isEditModeDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value.toString());
                  setIsEditModeDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption value="" isPlaceholder />
                <SelectOption value="READ_ONLY" />
                <SelectOption value="WRITABLE" />
                <SelectOption value="UNSYNCED" />
              </Select>
            )}
          />
          {form.errors.config?.editMode?.[0] && (
            <div className="error">
              {form.errors.config.editMode[0].message}
            </div>
          )}
        </FormGroup>
        <FormGroup
          label={t("usersDN")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:usersDNHelp"
              fieldLabelId="user-federation:usersDn"
            />
          }
          fieldId="kc-console-users-dn"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            defaultValue=""
            id="kc-console-users-dn"
            data-testid="ldap-users-dn"
            name="config.usersDn[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateUsersDn")}`,
              },
            })}
          />
          {form.errors.config?.usersDn?.[0] && (
            <div className="error">{form.errors.config.usersDn[0].message}</div>
          )}
        </FormGroup>
        <FormGroup
          label={t("usernameLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:usernameLdapAttributeHelp"
              fieldLabelId="user-federation:usernameLdapAttribute"
            />
          }
          fieldId="kc-username-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            defaultValue="cn"
            id="kc-username-ldap-attribute"
            data-testid="ldap-username-attribute"
            name="config.usernameLDAPAttribute[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateUsernameLDAPAttribute")}`,
              },
            })}
          />
          {form.errors.config?.usernameLDAPAttribute?.[0] && (
            <div className="error">
              {form.errors.config.usernameLDAPAttribute[0].message}
            </div>
          )}
        </FormGroup>
        <FormGroup
          label={t("rdnLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:rdnLdapAttributeHelp"
              fieldLabelId="user-federation:rdnLdapAttribute"
            />
          }
          fieldId="kc-rdn-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            defaultValue="cn"
            id="kc-rdn-ldap-attribute"
            data-testid="ldap-rdn-attribute"
            name="config.rdnLDAPAttribute[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateRdnLdapAttribute")}`,
              },
            })}
          />
          {form.errors.config?.rdnLDAPAttribute?.[0] && (
            <div className="error">
              {form.errors.config.rdnLDAPAttribute[0].message}
            </div>
          )}
        </FormGroup>
        <FormGroup
          label={t("uuidLdapAttribute")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:uuidLdapAttributeHelp"
              fieldLabelId="user-federation:uuidLdapAttribute"
            />
          }
          fieldId="kc-uuid-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            defaultValue="objectGUID"
            id="kc-uuid-ldap-attribute"
            data-testid="ldap-uuid-attribute"
            name="config.uuidLDAPAttribute[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateUuidLDAPAttribute")}`,
              },
            })}
          />
          {form.errors.config?.uuidLDAPAttribute?.[0] && (
            <div className="error">
              {form.errors.config.uuidLDAPAttribute[0].message}
            </div>
          )}
        </FormGroup>
        <FormGroup
          label={t("userObjectClasses")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:userObjectClassesHelp"
              fieldLabelId="user-federation:userObjectClasses"
            />
          }
          fieldId="kc-user-object-classes"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            defaultValue="person, organizationalPerson, user"
            id="kc-user-object-classes"
            data-testid="ldap-user-object-classes"
            name="config.userObjectClasses[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateUserObjectClasses")}`,
              },
            })}
          />
          {form.errors.config?.userObjectClasses?.[0] && (
            <div className="error">
              {form.errors.config.userObjectClasses[0].message}
            </div>
          )}
        </FormGroup>
        <FormGroup
          label={t("userLdapFilter")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:userLdapFilterHelp"
              fieldLabelId="user-federation:userLdapFilter"
            />
          }
          fieldId="kc-user-ldap-filter"
        >
          <TextInput
            type="text"
            id="kc-user-ldap-filter"
            name="config.customUserSearchFilter[0]"
            ref={form.register({
              pattern: {
                value: /(\(.*\))$/,
                message: `${t("validateCustomUserSearchFilter")}`,
              },
            })}
          />
          {form.errors.config?.customUserSearchFilter?.[0] && (
            <div className="error">
              {form.errors.config.customUserSearchFilter[0].message}
            </div>
          )}
        </FormGroup>

        <FormGroup
          label={t("searchScope")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:searchScopeHelp"
              fieldLabelId="user-federation:searchScope"
            />
          }
          fieldId="kc-search-scope"
        >
          <Controller
            name="config.searchScope[0]"
            defaultValue=""
            control={form.control}
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
                <SelectOption key={0} value="1" isPlaceholder>
                  {t("oneLevel")}
                </SelectOption>
                <SelectOption key={1} value="2">
                  {t("subtree")}
                </SelectOption>
              </Select>
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("readTimeout")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:readTimeoutHelp"
              fieldLabelId="user-federation:readTimeout"
            />
          }
          fieldId="kc-read-timeout"
        >
          <TextInput
            type="number"
            min={0}
            id="kc-read-timeout"
            name="config.readTimeout[0]"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label={t("pagination")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:paginationHelp"
              fieldLabelId="user-federation:pagination"
            />
          }
          fieldId="kc-console-pagination"
          hasNoPaddingTop
        >
          <Controller
            name="config.pagination"
            defaultValue={["false"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-console-pagination"}
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
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
