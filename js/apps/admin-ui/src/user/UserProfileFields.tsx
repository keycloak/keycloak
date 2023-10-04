import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { Text } from "@patternfly/react-core";
import { Fragment } from "react";
import { FieldPath, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { OptionComponent } from "./components/OptionsComponent";
import { SelectComponent } from "./components/SelectComponent";
import { TextAreaComponent } from "./components/TextAreaComponent";
import { TextComponent } from "./components/TextComponent";
import { UserFormFields } from "./form-state";
import { fieldName } from "./utils";

export type UserProfileError = {
  responseData: { errors?: { errorMessage: string }[] };
};

export type Options = {
  options?: string[];
};

export function isUserProfileError(error: unknown): error is UserProfileError {
  return !!(error as UserProfileError).responseData.errors;
}

export function userProfileErrorToString(error: UserProfileError) {
  return (
    error.responseData["errors"]?.map((e) => e["errorMessage"]).join("\n") || ""
  );
}

const INPUT_TYPES = [
  "text",
  "textarea",
  "select",
  "select-radiobuttons",
  "multiselect",
  "multiselect-checkboxes",
  "html5-email",
  "html5-tel",
  "html5-url",
  "html5-number",
  "html5-range",
  "html5-datetime-local",
  "html5-date",
  "html5-month",
  "html5-time",
] as const;

export type InputType = (typeof INPUT_TYPES)[number];

export type UserProfileFieldProps = {
  form: UseFormReturn<UserFormFields>;
  inputType: InputType;
  attribute: UserProfileAttribute;
  roles: string[];
};

export const FIELDS: {
  [type in InputType]: (props: UserProfileFieldProps) => JSX.Element;
} = {
  text: TextComponent,
  textarea: TextAreaComponent,
  select: SelectComponent,
  "select-radiobuttons": OptionComponent,
  multiselect: SelectComponent,
  "multiselect-checkboxes": OptionComponent,
  "html5-email": TextComponent,
  "html5-tel": TextComponent,
  "html5-url": TextComponent,
  "html5-number": TextComponent,
  "html5-range": TextComponent,
  "html5-datetime-local": TextComponent,
  "html5-date": TextComponent,
  "html5-month": TextComponent,
  "html5-time": TextComponent,
} as const;

export type UserProfileFieldsProps = {
  form: UseFormReturn<UserFormFields>;
  config: UserProfileConfig;
  roles?: string[];
  hideReadOnly?: boolean;
};

export const UserProfileFields = ({
  form,
  config,
  roles = ["admin"],
  hideReadOnly = false,
}: UserProfileFieldsProps) => {
  const { t } = useTranslation();
  // Hide read-only attributes if 'hideReadOnly' is enabled.
  const attributes = hideReadOnly
    ? config.attributes?.filter(({ readOnly }) => !readOnly)
    : config.attributes;

  return (
    <ScrollForm
      sections={[{ name: "" }, ...(config.groups || [])].map((g) => ({
        title: g.displayHeader || g.name || t("general"),
        panel: (
          <div className="pf-c-form">
            {g.displayDescription && (
              <Text className="pf-u-pb-lg">{g.displayDescription}</Text>
            )}
            {attributes?.map((attribute) => (
              <Fragment key={attribute.name}>
                {(attribute.group || "") === g.name && (
                  <FormField form={form} attribute={attribute} roles={roles} />
                )}
              </Fragment>
            ))}
          </div>
        ),
      }))}
    />
  );
};

type FormFieldProps = {
  form: UseFormReturn<UserFormFields>;
  attribute: UserProfileAttribute;
  roles: string[];
};

const FormField = ({ form, attribute, roles }: FormFieldProps) => {
  const value = form.watch(fieldName(attribute) as FieldPath<UserFormFields>);
  const inputType = determineInputType(attribute, value);
  const Component = FIELDS[inputType];

  return (
    <Component
      form={form}
      inputType={inputType}
      attribute={attribute}
      roles={roles}
    />
  );
};

const DEFAULT_INPUT_TYPE = "multiselect" satisfies InputType;

function determineInputType(
  attribute: UserProfileAttribute,
  value: string | string[],
): InputType {
  const inputType = attribute.annotations?.inputType;

  // If the attribute has no valid input type, it is always multi-valued.
  if (!isValidInputType(inputType)) {
    return DEFAULT_INPUT_TYPE;
  }

  // An attribute with multiple values is always multi-valued, even if an input type is provided.
  if (Array.isArray(value) && value.length > 1) {
    return DEFAULT_INPUT_TYPE;
  }

  return inputType;
}

const isValidInputType = (value: unknown): value is InputType =>
  typeof value === "string" && value in FIELDS;
