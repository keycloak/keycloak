import type {
  UserProfileAttribute,
  UserProfileAttributeRequired,
} from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import {
  Form,
  FormGroup,
  Select,
  SelectOption,
  Text,
} from "@patternfly/react-core";
import { Fragment } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { useUserProfile } from "../realm-settings/user-profile/UserProfileContext";
import useToggle from "../utils/useToggle";

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];
const DEFAULT_ROLES = ["admin", "user"];

type UserProfileFieldsProps = {
  roles?: string[];
};

export type UserProfileError = {
  responseData: { errors?: { errorMessage: string }[] };
};

export function isUserProfileError(error: unknown): error is UserProfileError {
  return !!(error as UserProfileError).responseData.errors;
}

export function userProfileErrorToString(error: UserProfileError) {
  return (
    error.responseData["errors"]?.map((e) => e["errorMessage"]).join("\n") || ""
  );
}

export const UserProfileFields = ({
  roles = ["admin"],
}: UserProfileFieldsProps) => {
  const { t } = useTranslation("realm-settings");
  const { config } = useUserProfile();

  return (
    <ScrollForm
      sections={[{ name: "" }, ...(config?.groups || [])].map((g) => ({
        title: g.name || t("general"),
        panel: (
          <Form>
            {g.displayDescription && (
              <Text className="pf-u-pb-lg">{g.displayDescription}</Text>
            )}
            {config?.attributes?.map((attribute) => (
              <Fragment key={attribute.name}>
                {(attribute.group || "") === g.name &&
                  (attribute.permissions?.view || DEFAULT_ROLES).some((r) =>
                    roles.includes(r)
                  ) && <FormField attribute={attribute} roles={roles} />}
              </Fragment>
            ))}
          </Form>
        ),
      }))}
    />
  );
};

type FormFieldProps = {
  attribute: UserProfileAttribute;
  roles: string[];
};

const FormField = ({ attribute, roles }: FormFieldProps) => {
  const { t } = useTranslation("users");
  const {
    formState: { errors },
    register,
    control,
  } = useFormContext();
  const [open, toggle] = useToggle();

  const isBundleKey = (displayName?: string) => displayName?.includes("${");
  const unWrap = (key: string) => key.substring(2, key.length - 1);

  const isSelect = (attribute: UserProfileAttribute) =>
    Object.hasOwn(attribute.validations || {}, "options");

  const isRootAttribute = (attr?: string) =>
    attr && ROOT_ATTRIBUTES.includes(attr);

  const isRequired = (required: UserProfileAttributeRequired | undefined) =>
    Object.keys(required || {}).length !== 0;

  const fieldName = (attribute: UserProfileAttribute) =>
    `${isRootAttribute(attribute.name) ? "" : "attributes."}${attribute.name}`;

  return (
    <FormGroup
      key={attribute.name}
      label={
        (isBundleKey(attribute.displayName)
          ? t(unWrap(attribute.displayName!))
          : attribute.displayName) || attribute.name
      }
      fieldId={attribute.name}
      isRequired={isRequired(attribute.required)}
      validated={errors.username ? "error" : "default"}
      helperTextInvalid={t("common:required")}
    >
      {isSelect(attribute) ? (
        <Controller
          name={fieldName(attribute)}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId={attribute.name}
              onToggle={toggle}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                toggle();
              }}
              selections={field.value}
              variant="single"
              aria-label={t("common:selectOne")}
              isOpen={open}
              isDisabled={
                !(attribute.permissions?.edit || DEFAULT_ROLES).some((r) =>
                  roles.includes(r)
                )
              }
            >
              {[
                <SelectOption key="empty" value="">
                  {t("common:choose")}
                </SelectOption>,
                ...(
                  attribute.validations?.options as { options: string[] }
                ).options.map((option) => (
                  <SelectOption
                    selected={field.value === option}
                    key={option}
                    value={option}
                  >
                    {option}
                  </SelectOption>
                )),
              ]}
            </Select>
          )}
        />
      ) : (
        <KeycloakTextInput
          id={attribute.name}
          isDisabled={
            !(attribute.permissions?.edit || DEFAULT_ROLES).some((r) =>
              roles.includes(r)
            )
          }
          {...register(fieldName(attribute))}
        />
      )}
    </FormGroup>
  );
};
