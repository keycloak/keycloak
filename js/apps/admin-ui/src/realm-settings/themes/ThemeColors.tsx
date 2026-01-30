import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  Alert,
  Flex,
  FlexItem,
  FormGroup,
  PageSection,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import useToggle from "../../utils/useToggle";
import { FileNameDialog } from "./FileNameDialog";
import { ImageUpload } from "./ImageUpload";
import { usePreviewLogo } from "./LogoContext";
import {
  darkTheme,
  lightTheme,
  resolveColorToHex,
  resolveColorReferences,
  DefaultValueType,
} from "./PatternflyVars";
import { PreviewWindow } from "./PreviewWindow";
import { ThemeRealmRepresentation } from "./ThemesTab";
import { UploadJar } from "./UploadJar";
import { DefaultColorAccordion } from "./DefaultColorAccordion";
import { ColorControl } from "./ColorControl";

type ThemeType = "light" | "dark";

const switchTheme = (theme: ThemeType) => {
  if (theme === "light") {
    document
      .querySelector('meta[name="color-scheme"]')!
      .setAttribute("content", "light");
    document.documentElement.classList.remove("pf-v5-theme-dark");
  } else {
    document.documentElement.classList.add("pf-v5-theme-dark");
  }
};

type ThemeColorsProps = {
  realm: RealmRepresentation;
  save: (realm: ThemeRealmRepresentation) => void;
  theme: "light" | "dark";
};

export const ThemeColors = ({ realm, save, theme }: ThemeColorsProps) => {
  const { t } = useTranslation();
  const form = useForm();
  const { handleSubmit, watch, control, setValue } = form;
  const style = watch();
  const contextLogo = usePreviewLogo();
  const [open, toggle, setOpen] = useToggle();
  const [overriddenColors, setOverriddenColors] = useState<Set<string>>(
    new Set(),
  );

  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  const mapping = useMemo(
    () => (theme === "light" ? lightTheme() : darkTheme()),
    [theme],
  );

  const parentColors = useMemo(
    () => mapping.filter((m) => m.dependencies && m.dependencies.length > 0),
    [mapping],
  );

  const parentWatchNames = useMemo(
    () =>
      parentColors
        .map((p) => (p.variable ? `${theme}.${p.variable}` : null))
        .filter((name): name is string => name !== null),
    [parentColors, theme],
  );

  const parentColorValues = useWatch({
    control,
    name: parentWatchNames,
  });

  const prevParentValues = useRef<(string | undefined)[]>([]);

  // Mark a color as overridden by user
  const markAsOverridden = useCallback((colorName: string) => {
    setOverriddenColors((prev) => new Set(prev).add(colorName));
  }, []);

  // Recalculate derived colors when any parent color changes
  useEffect(() => {
    if (!parentColorValues || parentColorValues.length === 0) return;

    // Find which parent colors changed and update their dependents
    parentColors.forEach((parent, index) => {
      const currentValue = parentColorValues[index];
      const prevValue = prevParentValues.current[index];

      if (currentValue && currentValue !== prevValue && parent.dependencies) {
        // Update each dependent that hasn't been manually overridden
        parent.dependencies.forEach((dep) => {
          if (!overriddenColors.has(dep.name)) {
            const resolved = resolveColorReferences(
              dep.defaultValue,
              currentValue,
              theme,
            );
            const hexValue = resolveColorToHex(resolved);
            setValue(`${theme}.${dep.variable}`, hexValue);
          }
        });
      }
    });

    prevParentValues.current = [...parentColorValues];
  }, [parentColorValues, parentColors, overriddenColors, theme, setValue]);

  const defaultValue = (v: DefaultValueType | undefined, theme: ThemeType) =>
    typeof v === "string" ? v : v?.[theme];

  const reset = () => {
    setOverriddenColors(new Set());

    const parentDefaults: Record<string, string> = {};
    mapping.forEach((m) => {
      if (m.defaultValue && !m.parentName) {
        parentDefaults[m.name] = defaultValue(m.defaultValue, theme)!;
      }
    });

    form.reset({
      [theme]: mapping.reduce<Record<string, string>>((acc, m) => {
        if (!m.variable) return acc;

        let value = defaultValue(m.defaultValue, theme);
        if (m.parentName && value?.includes("{{")) {
          const parentValue = parentDefaults[m.parentName];
          if (parentValue) {
            const resolved = resolveColorReferences(value, parentValue, theme);
            value = resolveColorToHex(resolved);
          }
        }

        if (value !== undefined) {
          acc[defaultValue(m.variable, theme)!] = value;
        }
        return acc;
      }, {}),
    });
  };

  const setupForm = () => {
    reset();
  };

  const upload = (values: ThemeRealmRepresentation) => {
    form.setValue("bgimage", values.bgimage);
    form.setValue("favicon", values.favicon);
    form.setValue("logo", values.logo);
    form.reset(values);
  };

  const convert = (values: Record<string, File | string>) => {
    const styles = JSON.parse(realm.attributes?.style || "{}");
    save({
      ...realm,
      favicon: values.favicon as File,
      logo: values.logo as File,
      bgimage: values.bgimage as File,
      fileName: values.name as string,
      attributes: {
        ...realm.attributes,
        style: JSON.stringify({
          ...styles,
          ...values,
        }),
      },
    });
  };

  useEffect(() => {
    setupForm();
    switchTheme(theme);
    return () => {
      switchTheme(mediaQuery.matches ? "dark" : "light");
    };
  }, [realm]);

  return (
    <>
      {open && (
        <FileNameDialog
          onSave={async (name) => {
            await handleSubmit((data) => convert({ ...data, name }))();
            setOpen(false);
          }}
          onClose={toggle}
        />
      )}
      <PageSection variant="light">
        <TextContent className="pf-v5-u-mb-lg">
          <Text>{t("themeColorInfo")}</Text>
        </TextContent>
        {mediaQuery.matches && theme === "light" && (
          <Alert variant="info" isInline title={t("themePreviewInfo")} />
        )}
        <Flex className="pf-v5-u-pt-lg">
          <FlexItem>
            <FormAccess isHorizontal role="manage-realm">
              <FormProvider {...form}>
                <FormGroup label={t("favicon")}>
                  <ImageUpload name="favicon" />
                </FormGroup>
                <FormGroup label={t("logo")}>
                  <ImageUpload
                    name="logo"
                    onChange={(logo) => contextLogo?.setLogo(logo)}
                  />
                </FormGroup>
                <FormGroup label={t("backgroundImage")}>
                  <ImageUpload name="bgimage" />
                </FormGroup>
                {mapping.map((m) =>
                  m.parentName ? (
                    <DefaultColorAccordion
                      label={m.name}
                      color={defaultValue(m.defaultValue, theme)!}
                      key={m.name}
                      name={`${theme}.${m.variable}`}
                      colorName={m.name}
                      onOverride={markAsOverridden}
                    />
                  ) : (
                    <ColorControl
                      key={m.name}
                      color={defaultValue(m.defaultValue, theme)!}
                      name={`${theme}.${m.variable!}`}
                      label={m.name}
                    />
                  ),
                )}
              </FormProvider>
            </FormAccess>
          </FlexItem>
          <FlexItem grow={{ default: "grow" }} style={{ zIndex: 0 }}>
            <PreviewWindow cssVars={style?.[theme] || {}} />
          </FlexItem>
        </Flex>
        <FixedButtonsGroup
          name="colors"
          saveText={t("downloadThemeJar")}
          save={toggle}
          reset={setupForm}
        >
          <UploadJar onUpload={upload} />
        </FixedButtonsGroup>
      </PageSection>
    </>
  );
};
