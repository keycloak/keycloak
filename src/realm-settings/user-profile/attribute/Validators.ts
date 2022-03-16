export type Validator = {
  name: string;
  description?: string;
  config?: ValidatorConfig[];
};

export type ValidatorConfig = {
  name?: string;
  label?: string;
  helpText?: string;
  type?: string;
  defaultValue?: any;
  options?: string[];
  secret?: boolean;
};

export const validators: Validator[] = [
  {
    name: "double",
    description:
      "Check if the value is a double and within a lower and/or upper range. If no range is defined, the validator only checks whether the value is a valid number.",
    config: [
      {
        type: "String",
        defaultValue: "",
        helpText: "The minimal allowed value - this config is optional.",
        label: "Minimum",
        name: "min",
      },
      {
        type: "String",
        defaultValue: "",
        helpText: "The maximal allowed value - this config is optional.",
        label: "Maximum",
        name: "max",
      },
    ],
  },
  {
    name: "email",
    description: "Check if the value has a valid e-mail format.",
    config: [],
  },
  {
    name: "integer",
    description:
      "Check if the value is an integer and within a lower and/or upper range. If no range is defined, the validator only checks whether the value is a valid number.",
    config: [
      {
        type: "String",
        defaultValue: "",
        helpText: "The minimal allowed value - this config is optional.",
        label: "Minimum",
        name: "min",
      },
      {
        type: "String",
        defaultValue: "",
        helpText: "The maximal allowed value - this config is optional.",
        label: "Maximum",
        name: "max",
      },
    ],
  },
  {
    name: "length",
    description:
      "Check the length of a string value based on a minimum and maximum length.",
    config: [
      {
        type: "String",
        defaultValue: "",
        helpText: "The minimum length",
        label: "Minimum length",
        name: "min",
      },
      {
        type: "String",
        defaultValue: "",
        helpText: "The maximum length",
        label: "Maximum length",
        name: "max",
      },
      {
        type: "boolean",
        defaultValue: false,
        helpText:
          "Disable trimming of the String value before the length check",
        label: "Trimming disabled",
        name: "trim-disabled",
      },
    ],
  },
  {
    name: "local-date",
    description:
      "Check if the value has a valid format based on the realm and/or user locale.",
    config: [],
  },
  {
    name: "options",
    description:
      "Check if the value is from the defined set of allowed values. Useful to validate values entered through select and multiselect fields.",
    config: [
      {
        type: "MultivaluedString",
        defaultValue: "",
        helpText: "List of allowed options",
        label: "Options",
        name: "options",
      },
    ],
  },
  {
    name: "pattern",
    description: "Check if the value matches a specific RegEx pattern.",
    config: [
      {
        type: "String",
        defaultValue: "",
        helpText:
          "RegExp pattern the value must match. Java Pattern syntax is used.",
        label: "RegExp pattern",
        name: "pattern",
      },
      {
        type: "String",
        defaultValue: "",
        helpText:
          "Key of the error message in i18n bundle. Dafault message key is error-pattern-no-match",
        label: "Error message key",
        name: "error-message",
      },
    ],
  },
  {
    name: "person-name-prohibited-characters",
    description:
      "Check if the value is a valid person name as an additional barrier for attacks such as script injection. The validation is based on a default RegEx pattern that blocks characters not common in person names.",
    config: [
      {
        type: "String",
        defaultValue: "",
        helpText:
          "Key of the error message in i18n bundle. Dafault message key is error-person-name-invalid-character",
        label: "Error message key",
        name: "error-message",
      },
    ],
  },
  {
    name: "uri",
    description: "Check if the value is a valid URI.",
    config: [],
  },
  {
    name: "username-prohibited-characters",
    description:
      "Check if the value is a valid username as an additional barrier for attacks such as script injection. The validation is based on a default RegEx pattern that blocks characters not common in usernames.",
    config: [
      {
        type: "String",
        defaultValue: "",
        helpText:
          "Key of the error message in i18n bundle. Dafault message key is error-username-invalid-character",
        label: "Error message key",
        name: "error-message",
      },
    ],
  },
];
