import { Text } from "@patternfly/react-core";
import { useMemo } from "react";
import { FieldPath, UseFormReturn } from "react-hook-form";
import { ScrollForm } from "../main";
import { LocaleSelector } from "./LocaleSelector";
import { MultiInputComponent } from "./MultiInputComponent";
import { OptionComponent } from "./OptionsComponent";
import { SelectComponent } from "./SelectComponent";
import { TextAreaComponent } from "./TextAreaComponent";
import { TextComponent } from "./TextComponent";
import {
  UserFormFields,
  UserProfileAttributeGroupMetadata,
  UserProfileAttributeMetadata,
  UserProfileMetadata,
} from "./userProfileConfig";
import { TranslationFunction, fieldName, isRootAttribute } from "./utils";

export type UserProfileError = {
  responseData: { errors?: { errorMessage: string }[] };
};

export type Options = {
  options?: string[];
};

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
  "multi-input",
] as const;

export type InputType = (typeof INPUT_TYPES)[number];

export type UserProfileFieldProps = {
  t: TranslationFunction;
  form: UseFormReturn<UserFormFields>;
  inputType: InputType;
  attribute: UserProfileAttributeMetadata;
  renderer?: (
    attribute: UserProfileAttributeMetadata,
  ) => JSX.Element | undefined;
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
  "multi-input": MultiInputComponent,
} as const;

export type UserProfileFieldsProps = {
  t: TranslationFunction;
  form: UseFormReturn<UserFormFields>;
  userProfileMetadata: UserProfileMetadata;
  supportedLocales: string[];
  hideReadOnly?: boolean;
  renderer?: (
    attribute: UserProfileAttributeMetadata,
  ) => JSX.Element | undefined;
};

type GroupWithAttributes = {
  group: UserProfileAttributeGroupMetadata;
  attributes: UserProfileAttributeMetadata[];
};

export const UserProfileFields = ({
  t,
  form,
  userProfileMetadata,
  supportedLocales,
  hideReadOnly = false,
  renderer,
}: UserProfileFieldsProps) => {
  // Group attributes by group, for easier rendering.
  const groupsWithAttributes = useMemo(() => {
    // If there are no attributes, there is no need to group them.
    if (!userProfileMetadata.attributes) {
      return [];
    }

    // Hide read-only attributes if 'hideReadOnly' is enabled.
    const attributes = hideReadOnly
      ? userProfileMetadata.attributes.filter(({ readOnly }) => !readOnly)
      : userProfileMetadata.attributes;

    return [
      // Insert an empty group for attributes without a group.
      { name: undefined },
      ...(userProfileMetadata.groups ?? []),
    ].map<GroupWithAttributes>((group) => ({
      group,
      attributes: attributes.filter(
        (attribute) => attribute.group === group.name,
      ),
    }));
  }, [
    hideReadOnly,
    userProfileMetadata.groups,
    userProfileMetadata.attributes,
  ]);

  if (groupsWithAttributes.length === 0) {
    return null;
  }

  return (
    <ScrollForm
      label={t("jumpToSection")}
      sections={groupsWithAttributes
        .filter((group) => group.attributes.length > 0)
        .map(({ group, attributes }) => ({
          title: group.displayHeader || group.name || t("general"),
          panel: (
            <div className="pf-c-form">
              {group.displayDescription && (
                <Text className="pf-u-pb-lg">{group.displayDescription}</Text>
              )}
              {attributes.map((attribute) => (
                <FormField
                  key={attribute.name}
                  t={t}
                  form={form}
                  supportedLocales={supportedLocales}
                  renderer={renderer}
                  attribute={attribute}
                />
              ))}
            </div>
          ),
        }))}
    />
  );
};

type FormFieldProps = {
  t: TranslationFunction;
  form: UseFormReturn<UserFormFields>;
  supportedLocales: string[];
  attribute: UserProfileAttributeMetadata;
  renderer?: (
    attribute: UserProfileAttributeMetadata,
  ) => JSX.Element | undefined;
};

const FormField = ({
  t,
  form,
  renderer,
  supportedLocales,
  attribute,
}: FormFieldProps) => {
  const value = form.watch(
    fieldName(attribute.name) as FieldPath<UserFormFields>,
  );
  const inputType = determineInputType(attribute, value);
  const Component = FIELDS[inputType];

  if (attribute.name === "locale")
    return (
      <LocaleSelector
        form={form}
        supportedLocales={supportedLocales}
        t={t}
        attribute={attribute}
      />
    );
  return (
    <Component
      t={t}
      form={form}
      inputType={inputType}
      attribute={attribute}
      renderer={renderer}
    />
  );
};

const DEFAULT_INPUT_TYPE = "text" satisfies InputType;

function determineInputType(
  attribute: UserProfileAttributeMetadata,
  value: string | string[],
): InputType {
  // Always treat the root attributes as a text field.
  if (isRootAttribute(attribute.name)) {
    return "text";
  }

  const inputType = attribute.annotations?.inputType;

  // If the attribute has no valid input type, it is always multi-valued.
  if (!isValidInputType(inputType)) {
    return DEFAULT_INPUT_TYPE;
  }

  // An attribute with multiple values is always multi-valued, even if an input type is provided.
  if (Array.isArray(value) && value.length > 1) {
    return "multi-input";
  }

  return inputType;
}

const isValidInputType = (value: unknown): value is InputType =>
  typeof value === "string" && value in FIELDS;
