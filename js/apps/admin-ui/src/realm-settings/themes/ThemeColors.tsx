import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { TextControlProps } from "@keycloak/keycloak-ui-shared/dist/controls/TextControl";
import {
  Button,
  Flex,
  FlexItem,
  InputGroup,
  InputGroupItem,
  PageSection,
} from "@patternfly/react-core";
import { useEffect } from "react";
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
import mapping from "./PatternflyVars";
import { PreviewWindow } from "./PreviewWindow";

type ColorControlProps = TextControlProps<any> & {
  color: string;
};

const ColorControl = ({ name, color, ...props }: ColorControlProps) => {
  const { control, setValue } = useFormContext();
  const currentValue = useWatch({
    control,
    name,
  });
  return (
    <InputGroup>
      <InputGroupItem isFill>
        <TextControl {...props} name={name} />
      </InputGroupItem>
      <input
        type="color"
        value={currentValue || color}
        onChange={(e) => setValue(name, e.target.value)}
      />
    </InputGroup>
  );
};

const convertToCssVars = (attributes: Record<string, string>) =>
  Object.entries(attributes)
    .filter(([key]) => key.startsWith("style"))
    .reduce(
      (acc, [key, value]) => ({
        ...acc,
        [key.substring("style".length + 2)]: value,
      }),
      {},
    );

type ThemeColorsProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const ThemeColors = ({ realm, save }: ThemeColorsProps) => {
  const { t } = useTranslation();
  const form = useForm<RealmRepresentation>();
  const { handleSubmit, setValue } = form;
  const attributes = useWatch({
    control: form.control,
    name: "attributes",
    defaultValue: {},
  });

  const reset = () =>
    convertToFormValues(
      {
        attributes: {
          style: mapping.reduce((acc, m) => ({
            ...acc,
            [m.variable]: m.defaultValue,
          })),
        },
      },
      setValue,
    );

  const setupForm = () => {
    reset();
    convertToFormValues(realm, setValue);
  };

  useEffect(setupForm, [realm]);

  return (
    <PageSection variant="light">
      <Flex>
        <FlexItem>
          <FormAccess isHorizontal role="manage-realm">
            <FormProvider {...form}>
              <TextControl
                name={convertAttributeNameToForm("attributes.style.logo")}
                label={t("logo")}
              />
              {mapping.map((m) => (
                <ColorControl
                  key={m.name}
                  color={m.defaultValue}
                  name={convertAttributeNameToForm(
                    `attributes.style.${m.variable}`,
                  )}
                  label={m.name}
                />
              ))}
            </FormProvider>
          </FormAccess>
        </FlexItem>
        <FlexItem grow={{ default: "grow" }} style={{ zIndex: 0 }}>
          <PreviewWindow cssVars={convertToCssVars(attributes!)} />
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
