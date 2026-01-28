// variable is the PF5 variable with the --pf-v5-global-- prefix removed
// Dependencies use {{color}} token which gets replaced with parent's current value
type Value = { light?: string; dark?: string };
export type DefaultValueType = string | Value;

type DependencyVariable = {
  name: string;
  defaultValue: DefaultValueType;
  variable: string | Value;
};

type FlattenedDependencyVariable = {
  name: string;
  defaultValue: DefaultValueType;
  variable: string;
};

type VariableDefinition = {
  name: string;
  defaultValue: string | Value;
  variable: string | Value;
  dependencies?: DependencyVariable[];
};

const variables: VariableDefinition[] = [
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
    name: "primaryColor",
    defaultValue: "#0066cc",
    variable: { light: "primary-color--100", dark: "primary-color--300" },
    dependencies: [
      {
        name: "primaryColorHover",
        defaultValue: "color-mix(in srgb, {{color}} 63%, black)",
        variable: "primary-color--200",
      },
      {
        name: "activeColor",
        defaultValue: {
          light: "{{color}}",
          dark: "color-mix(in srgb, {{color}} 78%, white)",
        },
        variable: "active-color--100",
      },
      {
        name: "secondaryColor",
        defaultValue: {
          light: "{{color}}",
          dark: "color-mix(in srgb, {{color}} 78%, white)",
        },
        variable: "primary-color--100",
      },
    ],
  },
  {
    name: "linkColor",
    defaultValue: { light: "#0066cc", dark: "#1fa7f8" },
    variable: "link--Color",
    dependencies: [
      {
        name: "linkColorHover",
        defaultValue: {
          light: "color-mix(in srgb, {{color}} 63%, black)",
          dark: "color-mix(in srgb, {{color}} 63%, white)",
        },
        variable: "link--Color--hover",
      },
    ],
  },
  {
    name: "backgroundColor",
    defaultValue: { light: "#ffffff", dark: "#1b1d21" },
    variable: "BackgroundColor--light-100",
    dependencies: [
      {
        name: "backgroundColorAccent",
        defaultValue: "color-mix(in srgb, {{color}} 95%, white)",
        variable: { dark: "BackgroundColor--300" },
      },
      {
        name: "backgroundColorNav",
        defaultValue: {
          light: "color-mix(in srgb, {{color}} 14%, black)",
          dark: "color-mix(in srgb, {{color}} 99%, black)",
        },
        variable: {
          light: "BackgroundColor--dark-300",
          dark: "BackgroundColor--100",
        },
      },
      {
        name: "backgroundColorHeader",
        defaultValue: {
          light: "color-mix(in srgb, {{color}} 8%, black)",
          dark: "color-mix(in srgb, {{color}} 10%, black)",
        },
        variable: {
          light: "BackgroundColor--dark-100",
          dark: "palette--black-1000",
        },
      },
    ],
  },
  { name: "iconColor", defaultValue: "#f0f0f0", variable: "Color--light-200" },
  {
    name: "textColor",
    defaultValue: { light: "#151515", dark: "#e0e0e0" },
    variable: "Color--100",
    dependencies: [
      {
        name: "lightTextColor",
        defaultValue: { light: "#ffffff", dark: "{{color}}" },
        variable: "Color--light-100",
      },
      {
        name: "inputTextColor",
        defaultValue: { light: "{{color}}", dark: "{{color}}" },
        variable: "Color--dark-100",
      },
    ],
  },
  {
    name: "inputBackgroundColor",
    defaultValue: "#36373a",
    variable: { dark: "BackgroundColor--400" },
  },
];

type ThemeType = keyof Value;

export type FlattenedVariable = Omit<VariableDefinition, "dependencies"> & {
  parentName?: string;
  dependencies?: FlattenedDependencyVariable[];
};

const convert = (v: string | Value | undefined, theme: ThemeType) =>
  typeof v === "string" ? v : v?.[theme];

const flattenVariables = (theme: ThemeType): FlattenedVariable[] => {
  const result: FlattenedVariable[] = [];

  variables.forEach((v) => {
    const defaultValue = convert(v.defaultValue, theme);
    const variable = convert(v.variable, theme);

    // Skip variables that don't have a value for this theme
    if (defaultValue === undefined && variable === undefined) return;

    const flattenedVar: FlattenedVariable = {
      name: v.name,
      defaultValue: defaultValue!,
      variable: variable!,
    };

    if (v.dependencies && v.dependencies.length > 0) {
      flattenedVar.dependencies = v.dependencies
        .map((dep) => {
          const depVariable = convert(dep.variable, theme);
          if (!depVariable) return null;
          return {
            name: dep.name,
            variable: depVariable,
            defaultValue: dep.defaultValue,
          };
        })
        .filter((dep): dep is FlattenedDependencyVariable => dep !== null);
    }

    result.push(flattenedVar);

    if (v.dependencies) {
      v.dependencies.forEach((dep) => {
        const depVariable = convert(dep.variable, theme);
        if (!depVariable) return;
        result.push({
          name: dep.name,
          defaultValue: dep.defaultValue,
          variable: depVariable,
          parentName: v.name,
        });
      });
    }
  });

  return result;
};

export const lightTheme = (): FlattenedVariable[] =>
  flattenVariables("light").filter(
    (v) => v.defaultValue !== undefined || v.parentName !== undefined,
  );

export const darkTheme = (): FlattenedVariable[] => flattenVariables("dark");

export function resolveColorToHex(colorValue: string) {
  // If already a valid hex color, return it directly
  if (/^#[0-9a-fA-F]{6}$/i.test(colorValue)) {
    return colorValue.toLowerCase();
  }

  const el = document.createElement("div");
  el.style.cssText = `position:absolute;left:-9999px;color:${colorValue}`;
  document.body.appendChild(el);
  const computed = getComputedStyle(el).color;
  el.remove();

  let r = 0,
    g = 0,
    b = 0;

  // Parse color(srgb 0 0.252 0.504) format (0-1 range)
  let matches = /color\(srgb\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)/.exec(
    computed || "",
  );
  if (matches) {
    [, r, g, b] = matches.map(Number);
    r = Math.round(r * 255);
    g = Math.round(g * 255);
    b = Math.round(b * 255);
  } else {
    // Parse rgb(r, g, b) or rgba(r, g, b, a) format
    matches = /rgba?\(\s*(\d+),\s*(\d+),\s*(\d+)/.exec(computed || "");
    if (matches) {
      [, r, g, b] = matches.map(Number);
    }
  }
  return "#" + [r, g, b].map((x) => x.toString(16).padStart(2, "0")).join("");
}

export function resolveColorReferences(
  colorValue: DefaultValueType,
  parentValue: string,
  theme: ThemeType,
): string {
  return convert(colorValue, theme)!.replace(/\{\{color\}\}/g, parentValue);
}
