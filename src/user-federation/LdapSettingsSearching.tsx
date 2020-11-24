import {
  Form,
  FormGroup,
  Select,
  SelectOption,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";

export const LdapSettingsSearching = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      {/* Cache settings */}
      <Form isHorizontal>
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
          <Select
            toggleId="kc-edit-mode"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="Select a mode"
          >
            {/* {configFormats.map((configFormat) => ( */}
            <SelectOption
              key={"key"}
              value={"value"}
              // isSelected={selected === configFormat.id}
            >
              {"display name"}
            </SelectOption>
            {/* ))} */}
          </Select>
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
            name="kc-console-users-dn"
            // value={value1}
            // onChange={this.handleTextInputChange1}
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
            name="kc-username-ldap-attribute"
            // value={value1}
            // onChange={this.handleTextInputChange1}
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
            name="kc-rdn-ldap-attribute"
            // value={value1}
            // onChange={this.handleTextInputChange1}
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
            name="kc-uuid-ldap-attribute"
            // value={value1}
            // onChange={this.handleTextInputChange1}
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
            name="kc-user-object-classes"
            // value={value1}
            // onChange={this.handleTextInputChange1}
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
          <Select
            toggleId="kc-user-ldap-filter"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="Only for LDAPS" // TODO
          >
            {/* {configFormats.map((configFormat) => ( */}
            <SelectOption
              key={"key"}
              value={"value"}
              // isSelected={selected === configFormat.id}
            >
              {"display name"}
            </SelectOption>
            {/* ))} */}
          </Select>
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
          <Select
            toggleId="kc-search-scope"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="Only for LDAPS" // TODO
          >
            {/* {configFormats.map((configFormat) => ( */}
            <SelectOption
              key={"key"}
              value={"value"}
              // isSelected={selected === configFormat.id}
            >
              {"display name"}
            </SelectOption>
            {/* ))} */}
          </Select>
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
            name="kc-read-timeout"
            // value={value1}
            // onChange={this.handleTextInputChange1}
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
          <Switch
            id={"kc-console-pagination"}
            isChecked={true}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>
      </Form>
    </>
  );
};
