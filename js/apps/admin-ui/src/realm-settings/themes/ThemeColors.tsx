import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { TextControlProps } from "@keycloak/keycloak-ui-shared/dist/controls/TextControl";
import {
  Alert,
  Button,
  Flex,
  FlexItem,
  InputGroup,
  InputGroupItem,
  PageSection,
} from "@patternfly/react-core";
import { useEffect, useMemo } from "react";
import {
  FormProvider,
  useForm,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { convertAttributeNameToForm, convertToFormValues } from "../../util";
import { darkTheme, lightTheme } from "./PatternflyVars";
import { PreviewWindow } from "./PreviewWindow";

type ThemeType = "light" | "dark";

type ColorControlProps = TextControlProps<any> & {
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

const convertToCssVars = (
  attributes: Record<string, string>,
  theme: ThemeType,
) =>
  Object.entries(attributes)
    .filter(([key]) => key.startsWith("style"))
    .reduce(
      (acc, [key, value]) => ({
        ...acc,
        [key.substring(`style.${theme}.`.length + 2)]: value,
      }),
      {},
    );

const switchTheme = (theme: ThemeType) => {
  if (theme === "light") {
    document.documentElement.classList.remove("pf-v5-theme-dark");
  } else {
    document.documentElement.classList.add("pf-v5-theme-dark");
  }
};

type ThemeColorsProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
  theme: "light" | "dark";
};

export const ThemeColors = ({ realm, save, theme }: ThemeColorsProps) => {
  const { t } = useTranslation();
  const form = useForm<RealmRepresentation>();
  const { handleSubmit, setValue } = form;
  const attributes = useWatch({
    control: form.control,
    name: "attributes",
    defaultValue: {},
  });

  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  const mapping = useMemo(
    () => (theme === "light" ? lightTheme() : darkTheme()),
    [],
  );

  const reset = () =>
    convertToFormValues(
      {
        attributes: {
          style: {
            [theme]: mapping.reduce(
              (acc, m) => ({
                ...acc,
                [m.variable!]: m.defaultValue,
              }),
              {},
            ),
          },
        },
      },
      setValue,
    );

  const setupForm = () => {
    reset();
    convertToFormValues(realm, setValue);
  };

  useEffect(() => {
    setupForm();
    switchTheme(theme);
    return () => {
      switchTheme(mediaQuery.matches ? "dark" : "light");
    };
  }, [realm]);

  return (
    <PageSection variant="light">
      {mediaQuery.matches && theme === "light" && (
        <Alert variant="info" isInline title={t("themePreviewInfo")} />
      )}
      <Flex className="pf-v5-u-pt-lg">
        <FlexItem>
          <FormAccess isHorizontal role="manage-realm">
            <FormProvider {...form}>
              <TextControl
                name={convertAttributeNameToForm("attributes.style.logo")}
                label={t("logo")}
              />
              <TextControl
                name={convertAttributeNameToForm("attributes.style.bglogo")}
                label={t("backgroundLogo")}
              />
              {mapping.map((m) => (
                <ColorControl
                  key={m.name}
                  color={m.defaultValue!}
                  name={convertAttributeNameToForm(
                    `attributes.style.${theme}.${m.variable}`,
                  )}
                  label={m.name}
                />
              ))}
            </FormProvider>
          </FormAccess>
        </FlexItem>
        <FlexItem grow={{ default: "grow" }} style={{ zIndex: 0 }}>
          <PreviewWindow cssVars={convertToCssVars(attributes!, theme)} />
        </FlexItem>
      </Flex>
      <FixedButtonsGroup
        name="colors"
        save={handleSubmit(save)}
        reset={setupForm}
      >
        <Button type="button" variant="link" onClick={reset}>
          {t("defaults")}
        </Button>
      </FixedButtonsGroup>
    </PageSection>
  );
};
