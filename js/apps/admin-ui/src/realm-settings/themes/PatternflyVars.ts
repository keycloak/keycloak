// variable is the PF5 variable with the --pf-v5-global-- prefix removed
const variables = [
  {
    name: "font",
    defaultValue: '"RedHatText", helvetica, arial, sans-serif',
    variable: "FontFamily--text",
  },
  {
    name: "errorColor",
    defaultValue: { light: "#c9190b", dark: "#fe5142" },
    variable: "danger-color--100",
  },
  {
    name: "successColor",
    defaultValue: { light: "#3e8635", dark: "#5ba352" },
    variable: "success-color--100",
  },
  {
    name: "activeColor",
    defaultValue: { light: "#0066cc", dark: "#1fa7f8" },
    variable: "active-color--100",
  },
  {
    name: "primaryColor",
    defaultValue: "#0066cc",
    variable: { light: "primary-color--100", dark: "primary-color--300" },
  },
  {
    name: "primaryColorHover",
    defaultValue: "#004080",
    variable: "primary-color--200",
  },
  {
    name: "secondaryColor",
    defaultValue: { light: "#0066cc", dark: "#1fa7f8" },
    variable: "primary-color--100",
  },
  {
    name: "linkColor",
    defaultValue: { light: "#0066cc", dark: "#1fa7f8" },
    variable: "link--Color",
  },
  {
    name: "linkColorHover",
    defaultValue: { light: "#004080", dark: "#73bcf7" },
    variable: "link--Color--hover",
  },
  {
    name: "backgroundColor",
    defaultValue: { light: "#ffffff", dark: "#1b1d21" },
    variable: "BackgroundColor--light-100",
  },
  {
    name: "backgroundColorAccent",
    defaultValue: "#26292d",
    variable: { dark: "BackgroundColor--300" },
  },
  {
    name: "backgroundColorNav",
    defaultValue: { light: "#212427", dark: "#1b1d21" },
    variable: {
      light: "BackgroundColor--dark-300",
      dark: "BackgroundColor--100",
    },
  },
  {
    name: "backgroundColorHeader",
    defaultValue: { light: "#151515", dark: "#030303" },
    variable: {
      light: "BackgroundColor--dark-100",
      dark: "palette--black-1000",
    },
  },
  { name: "iconColor", defaultValue: "#f0f0f0", variable: "Color--light-200" },
  {
    name: "textColor",
    defaultValue: { light: "#151515", dark: "#e0e0e0" },
    variable: "Color--100",
  },
  {
    name: "lightTextColor",
    defaultValue: { light: "#ffffff", dark: "#e0e0e0" },
    variable: "Color--light-100",
  },
  {
    name: "inputBackgroundColor",
    defaultValue: "#36373a",
    variable: { dark: "BackgroundColor--400" },
  },
  {
    name: "inputTextColor",
    defaultValue: { light: "#151515", dark: "#e0e0e0" },
    variable: "Color--dark-100",
  },
];

type Value = { light?: string; dark: string };
type ThemeType = keyof Value;

const convert = (v: string | Value, theme: ThemeType) =>
  typeof v === "string" ? v : v[theme];

export const lightTheme = () =>
  variables
    .filter((v) => typeof v.defaultValue !== "string")
    .map((v) => ({
      name: v.name,
      defaultValue: convert(v.defaultValue, "light"),
      variable: convert(v.variable, "light"),
    }));

export const darkTheme = () =>
  variables.map((v) => ({
    name: v.name,
    defaultValue: convert(v.defaultValue, "dark"),
    variable: convert(v.variable, "dark"),
  }));
