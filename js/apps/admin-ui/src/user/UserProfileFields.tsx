import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { Text } from "@patternfly/react-core";
import { Fragment } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { OptionComponent } from "./components/OptionsComponent";
import { SelectComponent } from "./components/SelectComponent";
import { TextAreaComponent } from "./components/TextAreaComponent";
import { TextComponent } from "./components/TextComponent";
import { fieldName } from "./utils";

type UserProfileFieldsProps = {
  config: UserProfileConfig;
  roles?: string[];
};

export type UserProfileError = {
  responseData: { errors?: { errorMessage: string }[] };
};

export type Options = {
  options: string[] | undefined;
};

export function isUserProfileError(error: unknown): error is UserProfileError {
  return !!(error as UserProfileError).responseData.errors;
}

export function userProfileErrorToString(error: UserProfileError) {
  return (
    error.responseData["errors"]?.map((e) => e["errorMessage"]).join("\n") || ""
  );
}

const FieldTypes = [
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

export type Field = (typeof FieldTypes)[number];

export const FIELDS: {
  [index in Field]: (props: any) => JSX.Element;
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

export const isValidComponentType = (value: string): value is Field =>
  value in FIELDS;

export const UserProfileFields = ({
  config,
  roles = ["admin"],
}: UserProfileFieldsProps) => {
  const { t } = useTranslation();

  return (
    <ScrollForm
      sections={[{ name: "" }, ...(config.groups || [])].map((g) => ({
        title: g.displayHeader || g.name || t("general"),
        panel: (
          <div className="pf-c-form">
            {g.displayDescription && (
              <Text className="pf-u-pb-lg">{g.displayDescription}</Text>
            )}
            {config.attributes?.map((attribute) => (
              <Fragment key={attribute.name}>
                {(attribute.group || "") === g.name && (
                  <FormField attribute={attribute} roles={roles} />
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
  attribute: UserProfileAttribute;
  roles: string[];
};

const FormField = ({ attribute, roles }: FormFieldProps) => {
  const { watch } = useFormContext();
  const value = watch(fieldName(attribute));

  const componentType = (attribute.annotations?.["inputType"] ||
    (Array.isArray(value) ? "multiselect" : "text")) as Field;

  const Component = FIELDS[componentType];

  return <Component {...{ ...attribute, roles }} />;
};
