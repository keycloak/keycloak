import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  Flex,
  FlexItem,
  FormGroup,
  InputGroup,
  InputGroupItem,
  PageSection,
  Tab,
  Tabs,
  Text,
  TextContent,
  TextInputProps,
  ToggleGroup,
  ToggleGroupItem,
} from "@patternfly/react-core";
import { useEffect, useMemo, useState } from "react";
import {
  FormProvider,
  useForm,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import useToggle from "../../utils/useToggle";
import { fileToDataUri } from "./fileUtils";
import { FileNameDialog } from "./FileNameDialog";
import { ImageUpload } from "./ImageUpload";
import { LoginPreviewWindow } from "./LoginPreviewWindow";
import { usePreviewLogo } from "./LogoContext";
import { usePreviewBackground } from "./BackgroundContext";
import { darkTheme, lightTheme } from "./PatternflyVars";
import { PreviewWindow } from "./PreviewWindow";
import { ThemeRealmRepresentation } from "./ThemesTab";
import { UploadJar } from "./UploadJar";

type ThemeType = "light" | "dark";

type ColorControlProps = TextInputProps & {
  name: string;
  label: string;
  color: string;
};

const ColorControl = ({ name, color, label, ...props }: ColorControlProps) => {
  const { t } = useTranslation();
  const { control, setValue } = useFormContext();
  const currentValue = useWatch({
    control,
    name,
  });
  return (
    <InputGroup>
      <InputGroupItem isFill>
        <TextControl {...props} name={name} label={t(label)} />
      </InputGroupItem>
      <input
        type="color"
        value={currentValue || color}
        onChange={(e) => setValue(name, e.target.value)}
      />
    </InputGroup>
  );
};

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
  theme?: "light" | "dark";
};

export const ThemeColors = ({
  realm,
  save,
  theme: initialTheme,
}: ThemeColorsProps) => {
  const { t } = useTranslation();
  const form = useForm();
  const { handleSubmit, watch } = form;
  const style = watch();
  const contextLogo = usePreviewLogo();
  const contextBackground = usePreviewBackground();
  const [open, toggle, setOpen] = useToggle();
  const [previewTab, setPreviewTab] = useState(0);

  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  const getDarkModeFromRealm = () => {
    const darkMode = realm.attributes?.darkMode;
    if (darkMode === "false") return "light";
    if (darkMode === "true") return "dark";
    return mediaQuery.matches ? "dark" : "light";
  };
  const originalTheme = useMemo(() => getDarkModeFromRealm(), []);
  const [theme, setTheme] = useState<ThemeType>(initialTheme || originalTheme);
  const mapping = useMemo(
    () => (theme === "light" ? lightTheme() : darkTheme()),
    [theme],
  );

  const reset = () => {
    form.reset({
      [theme]: mapping.reduce(
        (acc, m) => ({
          ...acc,
          [m.variable!]: m.defaultValue,
        }),
        {},
      ),
    });
  };

  const setupForm = () => {
    reset();
  };

  const upload = async (values: ThemeRealmRepresentation) => {
    form.setValue("bgimage", values.bgimage);
    form.setValue("favicon", values.favicon);
    form.setValue("logo", values.logo);
    form.setValue("logoWidth", values.logoWidth);
    form.setValue("logoHeight", values.logoHeight);
    form.reset(values);

    // Update contexts with data URIs so preview reflects uploaded images
    if (values.logo) {
      const logoDataUri = await fileToDataUri(values.logo);
      contextLogo?.setLogo(logoDataUri);
    }
    if (values.bgimage) {
      const bgDataUri = await fileToDataUri(values.bgimage);
      contextBackground?.setBackground(bgDataUri);
    }
  };

  const convert = (values: Record<string, File | string>) => {
    const styles = JSON.parse(realm.attributes?.style || "{}");
    save({
      ...realm,
      favicon: values.favicon as File,
      logo: values.logo as File,
      logoWidth: values.logoWidth as string,
      logoHeight: values.logoHeight as string,
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
      switchTheme(originalTheme);
    };
  }, [realm, theme]);

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
                {!initialTheme && (
                  <FormGroup label={t("themeMode")}>
                    <ToggleGroup aria-label={t("themeMode")}>
                      <ToggleGroupItem
                        text={t("lightMode")}
                        isSelected={theme === "light"}
                        onChange={() => setTheme("light")}
                      />
                      <ToggleGroupItem
                        text={t("darkMode")}
                        isSelected={theme === "dark"}
                        onChange={() => setTheme("dark")}
                      />
                    </ToggleGroup>
                  </FormGroup>
                )}
                <FormGroup label={t("favicon")}>
                  <ImageUpload name="favicon" />
                </FormGroup>
                <FormGroup label={t("logo")}>
                  <ImageUpload
                    name="logo"
                    onChange={(logo) => contextLogo?.setLogo(logo)}
                  />
                </FormGroup>
                <TextControl
                  name={"logoWidth"}
                  label={t("logoWidth")}
                  placeholder="300px"
                  defaultValue="300px"
                />
                <TextControl
                  name={"logoHeight"}
                  label={t("logoHeight")}
                  placeholder="63px"
                  defaultValue="63px"
                />
                <FormGroup label={t("backgroundImage")}>
                  <ImageUpload
                    name="bgimage"
                    onChange={(bg) => contextBackground?.setBackground(bg)}
                  />
                </FormGroup>
                {mapping.map((m) => (
                  <ColorControl
                    key={m.name}
                    color={m.defaultValue!}
                    name={`${theme}.${m.variable!}`}
                    label={m.name}
                  />
                ))}
              </FormProvider>
            </FormAccess>
          </FlexItem>
          <FlexItem grow={{ default: "grow" }} style={{ zIndex: 0 }}>
            <Tabs
              activeKey={previewTab}
              isBox
              onSelect={(_, index) => setPreviewTab(index as number)}
            >
              <Tab title={t("loginPagePreview")} eventKey={0}>
                <LoginPreviewWindow
                  cssVars={{
                    ...(style?.[theme] || {}),
                    logoWidth: style?.["logoWidth"],
                    logoHeight: style?.["logoHeight"],
                  }}
                />
              </Tab>
              <Tab title={t("adminConsolePreview")} eventKey={1}>
                <PreviewWindow cssVars={style?.[theme] || {}} />
              </Tab>
            </Tabs>
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
