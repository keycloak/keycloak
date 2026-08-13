import {
  HelpItem,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
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
        <SelectControl
          id="editMode"
          name="config.editMode[0]"
          label={t("editMode")}
          labelIcon={t("editModeLdapHelp")}
          controller={{
            defaultValue: "",
            rules: {
              required: {
                value: true,
                message: t("validateEditMode"),
              },
            },
          }}
          options={["", "READ_ONLY", "WRITABLE", "UNSYNCED"]}
        />
        <TextControl
          name="config.usersDn.0"
          label={t("usersDN")}
          labelIcon={t("usersDNHelp")}
          rules={{
            required: t("validateUsersDn"),
          }}
        />
        <TextControl
          name="config.relativeCreateDn.0"
          label={t("relativeUserCreateDn")}
          labelIcon={t("relativeUserCreateDnHelp")}
        />
        <TextControl
          name="config.usernameLDAPAttribute.0"
          label={t("usernameLdapAttribute")}
          labelIcon={t("usernameLdapAttributeHelp")}
          defaultValue="cn"
          rules={{
            required: t("validateUsernameLDAPAttribute"),
          }}
        />
        <TextControl
          name="config.rdnLDAPAttribute.0"
          label={t("rdnLdapAttribute")}
          labelIcon={t("rdnLdapAttributeHelp")}
          defaultValue="cn"
          rules={{
            required: t("validateRdnLdapAttribute"),
          }}
        />
        <TextControl
          name="config.uuidLDAPAttribute.0"
          label={t("uuidLdapAttribute")}
          labelIcon={t("uuidLdapAttributeHelp")}
          defaultValue="objectGUID"
          rules={{
            required: t("validateUuidLDAPAttribute"),
          }}
        />
        <TextControl
          name="config.userObjectClasses.0"
          label={t("userObjectClasses")}
          labelIcon={t("userObjectClassesHelp")}
          defaultValue="person, organizationalPerson, user"
          rules={{
            required: t("validateUserObjectClasses"),
          }}
        />
        <TextControl
          name="config.customUserSearchFilter.0"
          label={t("userLdapFilter")}
          labelIcon={t("userLdapFilterHelp")}
          rules={{
            pattern: {
              value: /(\(.*\))$/,
              message: t("validateCustomUserSearchFilter"),
            },
          }}
        />
        <SelectControl
          id="kc-search-scope"
          name="config.searchScope[0]"
          label={t("searchScope")}
          labelIcon={t("searchScopeHelp")}
          controller={{
            defaultValue: "1",
          }}
          options={[
            { key: "1", value: t("oneLevel") },
            { key: "2", value: t("subtree") },
          ]}
        />
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
            defaultValue={["true"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-ui-pagination"
                data-testid="ui-pagination"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("pagination")}
              />
            )}
          ></Controller>
        </FormGroup>
        <SelectControl
          name="config.referral.0"
          label={t("referral")}
          labelIcon={t("referralHelp")}
          controller={{
            defaultValue: "",
          }}
          options={["ignore", "follow"]}
        />
      </FormAccess>
    </FormProvider>
  );
};
