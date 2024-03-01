import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type LdapSettingsSearchingProps = {
  form: UseFormReturn;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const LdapSettingsSearching = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsSearchingProps) => {
  const { t } = useTranslation();

  const [isSearchScopeDropdownOpen, setIsSearchScopeDropdownOpen] =
    useState(false);
  const [isEditModeDropdownOpen, setIsEditModeDropdownOpen] = useState(false);
  const [isReferralDropdownOpen, setIsReferralDropdownOpen] = useState(false);

  return (
    <FormProvider {...form}>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("ldapSearchingAndUpdatingSettings")}
          description={t("ldapSearchingAndUpdatingSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("editMode")}
          labelIcon={
            <HelpItem
              helpText={t("editModeLdapHelp")}
              fieldLabelId="editMode"
            />
          }
          fieldId="kc-edit-mode"
          isRequired
          validated={
            (form.formState.errors.config as any)?.editMode?.[0]
              ? "error"
              : "default"
          }
          helperTextInvalid={
            (form.formState.errors.config as any)?.editMode?.[0].message
          }
        >
          <Controller
            name="config.editMode[0]"
            defaultValue=""
            control={form.control}
            rules={{
              required: { value: true, message: t("validateEditMode") },
            }}
            render={({ field }) => (
              <Select
                toggleId="kc-edit-mode"
                required
                onToggle={() =>
                  setIsEditModeDropdownOpen(!isEditModeDropdownOpen)
                }
                isOpen={isEditModeDropdownOpen}
                onSelect={(_, value) => {
                  field.onChange(value.toString());
                  setIsEditModeDropdownOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("selectEditMode")}
                validated={
                  (form.formState.errors.config as any)?.editMode?.[0]
                    ? "error"
                    : "default"
                }
              >
                <SelectOption
                  aria-label={t("emptySelection")}
                  value=""
                  isPlaceholder
                />
                <SelectOption
                  aria-label={t("readOnlySelection")}
                  value="READ_ONLY"
                />
                <SelectOption
                  aria-label={t("writableSelection")}
                  value="WRITABLE"
                />
                <SelectOption
                  aria-label={t("unsyncedSelection")}
                  value="UNSYNCED"
                />
              </Select>
            )}
          />
        </FormGroup>
        <TextControl
          name="config.usersDn.0"
          label={t("usersDN")}
          labelIcon={t("usersDNHelp")}
          isRequired
          rules={{
            required: t("validateUsersDn").toString(),
          }}
        />
        <TextControl
          name="config.usernameLDAPAttribute.0"
          label={t("usernameLdapAttribute")}
          labelIcon={t("usernameLdapAttributeHelp")}
          isRequired
          defaultValue="cn"
          rules={{
            required: t("validateUsernameLDAPAttribute").toString(),
          }}
        />
        <TextControl
          name="config.rdnLDAPAttribute.0"
          label={t("rdnLdapAttribute")}
          labelIcon={t("rdnLdapAttributeHelp")}
          isRequired
          defaultValue="cn"
          rules={{
            required: t("validateRdnLdapAttribute").toString(),
          }}
        />
        <TextControl
          name="config.uuidLDAPAttribute.0"
          label={t("uuidLdapAttribute")}
          labelIcon={t("uuidLdapAttributeHelp")}
          isRequired
          defaultValue="objectGUID"
          rules={{
            required: t("validateUuidLDAPAttribute").toString(),
          }}
        />
        <TextControl
          name="config.userObjectClasses.0"
          label={t("userObjectClasses")}
          labelIcon={t("userObjectClassesHelp")}
          isRequired
          defaultValue="person, organizationalPerson, user"
          rules={{
            required: t("validateUserObjectClasses").toString(),
          }}
        />
        <TextControl
          name="config.customUserSearchFilter.0"
          label={t("userLdapFilter")}
          labelIcon={t("userLdapFilterHelp")}
          rules={{
            pattern: {
              value: /(\(.*\))$/,
              message: t("validateCustomUserSearchFilter").toString(),
            },
          }}
        />
        <FormGroup
          label={t("searchScope")}
          labelIcon={
            <HelpItem
              helpText={t("searchScopeHelp")}
              fieldLabelId="searchScope"
            />
          }
          fieldId="kc-search-scope"
        >
          <Controller
            name="config.searchScope[0]"
            defaultValue=""
            control={form.control}
            render={({ field }) => (
              <Select
                toggleId="kc-search-scope"
                required
                onToggle={() =>
                  setIsSearchScopeDropdownOpen(!isSearchScopeDropdownOpen)
                }
                isOpen={isSearchScopeDropdownOpen}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setIsSearchScopeDropdownOpen(false);
                }}
                selections={field.value}
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
          />
        </FormGroup>
        <TextControl
          name="config.readTimeout.0"
          label={t("readTimeout")}
          labelIcon={t("readTimeoutHelp")}
          type="number"
          min={0}
        />
        <FormGroup
          label={t("pagination")}
          labelIcon={
            <HelpItem
              helpText={t("paginationHelp")}
              fieldLabelId="pagination"
            />
          }
          fieldId="kc-ui-pagination"
          hasNoPaddingTop
        >
          <Controller
            name="config.pagination"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-ui-pagination"
                data-testid="ui-pagination"
                isDisabled={false}
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("pagination")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("referral")}
          labelIcon={
            <HelpItem helpText={t("referralHelp")} fieldLabelId="referral" />
          }
          fieldId="kc-referral"
        >
          <Controller
            name="config.referral.0"
            defaultValue=""
            control={form.control}
            render={({ field }) => (
              <Select
                toggleId="kc-referral"
                onToggle={() =>
                  setIsReferralDropdownOpen(!isReferralDropdownOpen)
                }
                isOpen={isReferralDropdownOpen}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setIsReferralDropdownOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
              >
                <SelectOption value="ignore" isPlaceholder />
                <SelectOption value="follow" />
              </Select>
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </FormProvider>
  );
};
