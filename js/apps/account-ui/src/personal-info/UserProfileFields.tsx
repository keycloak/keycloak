import { useFormContext } from "react-hook-form";
import {
  UserProfileAttributeMetadata,
  UserProfileMetadata,
} from "../api/representations";
import { LocaleSelector } from "./LocaleSelector";
import { OptionComponent } from "./components/OptionsComponent";
import { SelectComponent } from "./components/SelectComponent";
import { TextAreaComponent } from "./components/TextAreaComponent";
import { TextComponent } from "./components/TextComponent";
import { fieldName } from "./utils";

type UserProfileFieldsProps = {
  metaData: UserProfileMetadata;
};

export type Options = {
  options: string[] | undefined;
};

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

export const UserProfileFields = ({ metaData }: UserProfileFieldsProps) =>
  metaData.attributes.map((attribute) => (
    <FormField key={attribute.name} attribute={attribute} />
  ));

type FormFieldProps = {
  attribute: UserProfileAttributeMetadata;
};

const FormField = ({ attribute }: FormFieldProps) => {
  const { watch } = useFormContext();
  const value = watch(fieldName(attribute));

  const componentType = (attribute.annotations?.["inputType"] ||
    (Array.isArray(value) ? "multiselect" : "text")) as Field;
  const Component = FIELDS[componentType];

  if (attribute.name === "locale") return <LocaleSelector />;
  return <Component {...{ ...attribute }} />;
};
