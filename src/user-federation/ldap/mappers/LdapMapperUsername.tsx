import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { UseFormMethods } from "react-hook-form";
import { FormAccess } from "../../../components/form-access/FormAccess";

export type LdapMapperUsernameProps = {
  form: UseFormMethods;
};

export const LdapMapperUsername = ({ form }: LdapMapperUsernameProps) => {
  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label="ID"
          labelIcon={
            <HelpItem
              helpText="stuff about mapper id"
              forLabel="ID"
              forID="kc-ldap-mapper-id"
            />
          }
          fieldId="kc-ldap-mapper-id"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-mapper-id"
            data-testid="ldap-mapper-id"
            name="id"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label="Name"
          labelIcon={
            <HelpItem
              helpText="stuff about mapper name"
              forLabel="ID"
              forID="kc-ldap-mapper-name"
            />
          }
          fieldId="kc-ldap-mapper-name"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-mapper-name"
            data-testid="ldap-mapper-name"
            name="name"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label="Mapper Type"
          labelIcon={
            <HelpItem
              helpText="stuff about mapper type"
              forLabel="Mapper Type"
              forID="kc-ldap-mapper-id"
            />
          }
          fieldId="kc-ldap-mapper-type"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-mapper-type"
            data-testid="ldap-mapper-type"
            name="providerId"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label="User Model Attribute"
          labelIcon={
            <HelpItem
              helpText="stuff about user model attribute"
              forLabel="User Model Attribute"
              forID="kc-user-model-attribute"
            />
          }
          fieldId="kc-user-model-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-user-model-attribute"
            data-testid="user-model-attribute"
            name="config.user-model-attribute"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label="LDAP Attribute"
          labelIcon={
            <HelpItem
              helpText="stuff about ldap attribute"
              forLabel="LDAP Attribute"
              forID="kc-ldap-attribute"
            />
          }
          fieldId="kc-ldap-attribute"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-attribute"
            data-testid="ldap-attribute"
            name="config.ldap-attribute"
            ref={form.register}
          />
        </FormGroup>
      </FormAccess>
    </>
  );
};
