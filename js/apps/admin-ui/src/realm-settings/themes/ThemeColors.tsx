import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { TextControlProps } from "@keycloak/keycloak-ui-shared/dist/controls/TextControl";
import { Button, PageSection } from "@patternfly/react-core";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { convertAttributeNameToForm, convertToFormValues } from "../../util";
import mapping from "./PatternflyVars";

type MappingKey = keyof typeof mapping;

type ColorControlProps = TextControlProps<any> & {
  attribute: MappingKey;
};

const ColorControl = ({ attribute, ...props }: ColorControlProps) => (
  <TextControl
    {...props}
    customIcon={
      <span
        style={{
          height: "1em",
          backgroundColor: mapping[attribute],
        }}
      >
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      </span>
    }
  />
);

type ThemeColorsProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const ThemeColors = ({ realm, save }: ThemeColorsProps) => {
  const { t } = useTranslation();
  const form = useForm<RealmRepresentation>();
  const { handleSubmit, setValue } = form;

  const reset = () =>
    convertToFormValues({ attributes: { style: mapping } }, setValue);
  const setupForm = () => {
    // if it has one of the default colors, we use that as the base
    if (realm?.attributes?.["style.palette--black-100"]) {
      convertToFormValues(realm, setValue);
    } else {
      reset();
    }
  };
  useEffect(setupForm, [realm]);

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        role="manage-realm"
        onSubmit={handleSubmit(save)}
      >
        <FormProvider {...form}>
          <TextControl
            name={convertAttributeNameToForm("attributes.style.logo")}
            label={t("logo")}
          />
          {Object.keys(mapping).map((key) => (
            <ColorControl
              key={key}
              attribute={key as MappingKey}
              name={convertAttributeNameToForm(`attributes.style.${key}`)}
              label={`--pf-v5-global--${key}`}
            />
          ))}
        </FormProvider>
        <FixedButtonsGroup name="colors" isSubmit reset={setupForm}>
          <Button type="button" variant="link" onClick={reset}>
            {t("defaults")}
          </Button>
        </FixedButtonsGroup>
      </FormAccess>
    </PageSection>
  );
};
