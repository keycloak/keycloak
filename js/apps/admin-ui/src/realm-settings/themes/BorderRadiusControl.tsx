import { TextControl } from "@keycloak/keycloak-ui-shared";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionToggle,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

type FormFields = {
  borderRadiusMain: string;
  borderRadiusInput: string;
  borderRadiusButton: string;
};

export function borderRadiusToCss({
  borderRadiusMain,
  borderRadiusInput,
  borderRadiusButton,
}: FormFields | Record<string, string>) {
  return `
.pf-v5-c-login__main {
  ${borderRadiusMain ? "border-radius:" + borderRadiusMain : ""}
}
.pf-v5-c-login__main-header {
  ${borderRadiusMain ? "border-radius:" + borderRadiusMain : ""}
}
.pf-v5-c-button {
  ${borderRadiusMain ? "border-radius:" + borderRadiusMain : ""}
}
.pf-v5-c-form-control {
  ${borderRadiusMain ? "border-radius:" + borderRadiusMain : ""}
}
.pf-v5-c-form-control input {
  ${borderRadiusInput ? "border-radius:" + borderRadiusInput : ""}
}
.pf-v5-c-form-control::after {
  ${borderRadiusButton ? "border-radius:" + borderRadiusButton : ""}
}
.pf-v5-c-button.pf-m-control::after {
  ${borderRadiusButton ? "border-radius:" + borderRadiusButton : ""}
}
  `;
}
function pxToNumber(value: string) {
  return parseInt(value.replaceAll("px", ""), 10);
}

export const BorderRadiusControl = () => {
  const { t } = useTranslation();
  const { control, setValue, register } = useFormContext();
  const [expanded, setExpanded] = useState(false);
  const [dependend, setDependend] = useState([
    "borderRadiusInput",
    "borderRadiusButton",
  ]);

  const mainBorderValue = useWatch({
    name: "borderRadiusMain",
    control,
    defaultValue: "",
  });

  useEffect(() => {
    dependend.map((d) =>
      setValue(d, `${Math.floor(pxToNumber(mainBorderValue) / 2)}px`),
    );
  }, [mainBorderValue, dependend]);

  return (
    <>
      <TextControl
        name={"borderRadiusMain"}
        label={t("borderRadius")}
        placeholder="in px"
        defaultValue=""
      />
      <Accordion asDefinitionList={false} isBordered togglePosition="start">
        <AccordionItem>
          <AccordionToggle
            onClick={() => setExpanded(!expanded)}
            isExpanded={expanded}
            id="default-color-toggle"
          >
            {t("borderRadius")}
          </AccordionToggle>
          <AccordionContent id="default-color-content" isHidden={!expanded}>
            <div className="pf-v5-c-form">
              <FormGroup
                label={t("borderRadiusInput")}
                fieldId={"borderRadiusInput"}
              >
                <TextInput
                  {...register("borderRadiusInput", {
                    onChange: () =>
                      setDependend(
                        dependend.filter((d) => d !== "borderRadiusInput"),
                      ),
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
                    onChange: () =>
                      setDependend(
                        dependend.filter((d) => d !== "borderRadiusButton"),
                      ),
                  })}
                  id="borderRadiusButton"
                  label={t("borderRadiusButton")}
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
