import { TextControl } from "@keycloak/keycloak-ui-shared";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionToggle,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useRef, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

type FormFields = {
  borderRadiusMain: string;
  borderRadiusInput: string;
  borderRadiusButton: string;
};

type DependentBorderRadiusField = "borderRadiusInput" | "borderRadiusButton";

const BORDER_RADIUS_PATTERN = /^(?:0|(?:\d+|\d*\.\d+)(?:px|rem|em|%))$/;
const BORDER_RADIUS_WITH_UNIT_PATTERN = /^(\d*\.?\d+)(px|rem|em|%)$/;

export function sanitizeBorderRadiusValue(value?: string) {
  const trimmed = value?.trim();
  if (!trimmed || !BORDER_RADIUS_PATTERN.test(trimmed)) {
    return undefined;
  }

  return trimmed;
}

function toBorderRadiusDeclaration(value?: string) {
  const borderRadius = sanitizeBorderRadiusValue(value);
  return borderRadius ? `border-radius: ${borderRadius};` : "";
}

function toCssVariableDeclaration(variable: string, value?: string) {
  const borderRadius = sanitizeBorderRadiusValue(value);
  return borderRadius ? `${variable}: ${borderRadius};` : "";
}

function toSubcomponentRadius(value?: string) {
  const borderRadius = sanitizeBorderRadiusValue(value);
  if (!borderRadius) {
    return "";
  }

  if (borderRadius === "0") {
    return "0";
  }

  const match = BORDER_RADIUS_WITH_UNIT_PATTERN.exec(borderRadius);
  if (!match) {
    return "";
  }

  return `${Number.parseFloat(match[1]) / 2}${match[2]}`;
}

export function borderRadiusToCss({
  borderRadiusMain,
  borderRadiusInput,
  borderRadiusButton,
}: FormFields | Record<string, string>) {
  return `
.pf-v5-c-login__main {
  ${toBorderRadiusDeclaration(borderRadiusMain)}
}
.pf-v5-c-login__main-header {
  ${toBorderRadiusDeclaration(borderRadiusMain)}
}
.pf-v5-c-button {
  ${toBorderRadiusDeclaration(borderRadiusButton)}
  ${toCssVariableDeclaration(
    "--pf-v5-c-button--after--BorderRadius",
    borderRadiusButton,
  )}
}
.pf-v5-c-form-control,
.pf-v5-c-form-control::after {
  ${toBorderRadiusDeclaration(borderRadiusInput)}
}
.pf-v5-c-button.pf-m-control::after {
  ${toBorderRadiusDeclaration(borderRadiusButton)}
}
  `;
}

export const BorderRadiusControl = () => {
  const { t } = useTranslation();
  const { control, setValue, register } = useFormContext();
  const [expanded, setExpanded] = useState(false);
  const dependentFieldsRef = useRef<DependentBorderRadiusField[]>([
    "borderRadiusInput",
    "borderRadiusButton",
  ]);

  const mainBorderValue = useWatch({
    name: "borderRadiusMain",
    control,
    defaultValue: "",
  });

  useEffect(() => {
    const subcomponentRadius = toSubcomponentRadius(mainBorderValue);
    dependentFieldsRef.current.forEach((field) => {
      setValue(field, subcomponentRadius);
    });
  }, [mainBorderValue, setValue]);

  const stopAutoUpdate = (field: DependentBorderRadiusField) => {
    dependentFieldsRef.current = dependentFieldsRef.current.filter(
      (dependentField) => dependentField !== field,
    );
  };

  return (
    <>
      <TextControl
        name={"borderRadiusMain"}
        label={t("defaultBorderRadius")}
        placeholder="in px"
        defaultValue=""
      />
      <Accordion asDefinitionList={false} isBordered togglePosition="start">
        <AccordionItem>
          <AccordionToggle
            onClick={() => setExpanded(!expanded)}
            isExpanded={expanded}
            id="border-radius-toggle"
          >
            {t("subcomponentBorderRadius")}
          </AccordionToggle>
          <AccordionContent id="border-radius-content" isHidden={!expanded}>
            <div className="pf-v5-c-form">
              <FormGroup
                label={t("borderRadiusInput")}
                fieldId={"borderRadiusInput"}
              >
                <TextInput
                  {...register("borderRadiusInput", {
                    onChange: () => stopAutoUpdate("borderRadiusInput"),
                  })}
                  id="borderRadiusInput"
                  placeholder="0px"
                />
              </FormGroup>
              <FormGroup
                label={t("borderRadiusButton")}
                fieldId={"borderRadiusButton"}
              >
                <TextInput
                  {...register("borderRadiusButton", {
                    onChange: () => stopAutoUpdate("borderRadiusButton"),
                  })}
                  id="borderRadiusButton"
                  placeholder="4px"
                />
              </FormGroup>
            </div>
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    </>
  );
};
