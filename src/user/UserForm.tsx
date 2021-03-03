import React, { useState } from "react";
import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";
import { useHistory } from "react-router-dom";
import { FormAccess } from "../components/form-access/FormAccess";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useRealm } from "../context/realm-context/RealmContext";

export type UserFormProps = {
  form: UseFormMethods<UserRepresentation>;
  save: (user: UserRepresentation) => void;
};

export const UserForm = ({
  form: { handleSubmit, register, errors, watch, control },
  save,
}: UserFormProps) => {
  const { t } = useTranslation("users");
  const { realm } = useRealm();

  const [
    isRequiredUserActionsDropdownOpen,
    setRequiredUserActionsDropdownOpen,
  ] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const history = useHistory();

  const watchUsernameInput = watch("username");

  const requiredUserActionsOptions = [
    <SelectOption key={0} value="Configure OTP">
      {t("configureOTP")}
    </SelectOption>,
    <SelectOption key={1} value="Update Password">
      {t("updatePassword")}
    </SelectOption>,
    <SelectOption key={2} value="Update Profile">
      {t("updateProfile")}
    </SelectOption>,
    <SelectOption key={3} value="Verify Email">
      {t("verifyEmail")}
    </SelectOption>,
    <SelectOption key={4} value="Update User Locale">
      {t("updateUserLocale")}
    </SelectOption>,
  ];

  const clearSelection = () => {
    setSelected([]);
    setRequiredUserActionsDropdownOpen(false);
  };

  return (
    <FormAccess
      isHorizontal
      onSubmit={handleSubmit(save)}
      role="manage-users"
      className="pf-u-mt-lg"
    >
      <FormGroup
        label={t("username")}
        fieldId="kc-username"
        isRequired
        validated={errors.username ? "error" : "default"}
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          ref={register({ required: true })}
          type="text"
          id="kc-username"
          name="username"
        />
      </FormGroup>

      <FormGroup
        label={t("email")}
        fieldId="kc-description"
        validated={errors.email ? "error" : "default"}
        helperTextInvalid={errors.email?.message}
      >
        <TextInput
          ref={register()}
          type="text"
          id="kc-email"
          name="email"
          aria-label={t("emailInput")}
        />
      </FormGroup>
      <FormGroup
        label={t("emailVerified")}
        fieldId="kc-email-verified"
        helperTextInvalid={t("common:required")}
        labelIcon={
          <HelpItem
            helpText={t("emailVerifiedHelpText")}
            forLabel={t("emailVerified")}
            forID="email-verified"
          />
        }
      >
        <Controller
          name="user-email-verified"
          defaultValue={false}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-user-email-verified"}
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
        label={t("firstName")}
        fieldId="kc-firstname"
        validated={errors.firstName ? "error" : "default"}
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          ref={register()}
          type="text"
          id="kc-firstname"
          name="firstname"
        />
      </FormGroup>
      <FormGroup
        label={t("lastName")}
        fieldId="kc-name"
        validated={errors.lastName ? "error" : "default"}
      >
        <TextInput
          ref={register()}
          type="text"
          id="kc-lastname"
          name="lastname"
          aria-label={t("lastName")}
        />
      </FormGroup>
      <FormGroup
        label={t("common:enabled")}
        fieldId="kc-enabled"
        labelIcon={
          <HelpItem
            helpText={t("disabledHelpText")}
            forLabel={t("enabled")}
            forID="enabled-label"
          />
        }
      >
        <Controller
          name="user-enabled"
          defaultValue={false}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-user-enabled"}
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
        label={t("requiredUserActions")}
        fieldId="kc-required-user-actions"
        validated={errors.requiredActions ? "error" : "default"}
        helperTextInvalid={t("common:required")}
        labelIcon={
          <HelpItem
            helpText={t("requiredUserActionsHelpText")}
            forLabel={t("requiredUserActions")}
            forID="required-user-actions-label"
          />
        }
      >
        <Controller
          name="required-user-actions"
          defaultValue={["0"]}
          typeAheadAriaLabel="Select an action"
          control={control}
          render={() => (
            <Select
              placeholderText="Select action"
              toggleId="kc-required-user-actions"
              onToggle={() =>
                setRequiredUserActionsDropdownOpen(
                  !isRequiredUserActionsDropdownOpen
                )
              }
              isOpen={isRequiredUserActionsDropdownOpen}
              selections={selected}
              onSelect={(_, value) => {
                const option = value as string;
                if (selected.includes(option)) {
                  setSelected(selected.filter((item) => item !== option));
                } else {
                  setSelected([...selected, option]);
                }
              }}
              onClear={clearSelection}
              variant="typeaheadmulti"
            >
              {requiredUserActionsOptions}
            </Select>
          )}
        ></Controller>
      </FormGroup>
      <ActionGroup>
        <Button
          isDisabled={!watchUsernameInput}
          variant="primary"
          type="submit"
        >
          {t("common:Create")}
        </Button>
        <Button
          data-testid="cancel-create-user"
          onClick={() => history.push(`/${realm}/users`)}
          variant="link"
        >
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
