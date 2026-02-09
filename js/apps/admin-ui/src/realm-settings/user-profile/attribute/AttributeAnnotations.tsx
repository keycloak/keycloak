import { FormGroup, Grid, GridItem, TextInput } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../../components/form/FormAccess";
import { KeyValueInput } from "../../../components/key-value-form/KeyValueInput";
import { KeySelect } from "./KeySelect";
import { ValueSelect } from "./ValueSelect";

import "../../realm-settings-section.css";

export const AttributeAnnotations = () => {
  const { t } = useTranslation();
  const { register } = useFormContext();

  return (
    <FormAccess role="manage-realm" isHorizontal>
      <FormGroup
        hasNoPaddingTop
        label={t("annotations")}
        fieldId="kc-annotations"
        className="kc-annotations-label"
      >
        <Grid className="kc-annotations">
          <GridItem>
            <KeyValueInput
              name="annotations"
              label={t("annotations")}
              KeyComponent={(props) => (
                <KeySelect
                  {...props}
                  selectItems={[
                    {
                      key: "inputType",
                      value: t("inputType"),
                    },
                    {
                      key: "inputHelperTextBefore",
                      value: t("inputHelperTextBefore"),
                    },
                    {
                      key: "inputHelperTextAfter",
                      value: t("inputHelperTextAfter"),
                    },
                    {
                      key: "inputOptionLabelsI18nPrefix",
                      value: t("inputOptionLabelsI18nPrefix"),
                    },
                    {
                      key: "inputTypePlaceholder",
                      value: t("inputTypePlaceholder"),
                    },
                    {
                      key: "inputTypeSize",
                      value: t("inputTypeSize"),
                    },
                    {
                      key: "inputTypeCols",
                      value: t("inputTypeCols"),
                    },
                    {
                      key: "inputTypeRows",
                      value: t("inputTypeRows"),
                    },
                    {
                      key: "inputTypeStep",
                      value: t("inputTypeStep"),
                    },
                    {
                      key: "kcNumberFormat",
                      value: t("kcNumberFormat"),
                    },
                    {
                      key: "kcNumberUnFormat",
                      value: t("kcNumberUnFormat"),
                    },
                  ]}
                />
              )}
              ValueComponent={(props) =>
                props.keyValue === "inputType" ? (
                  <ValueSelect
                    selectItems={[
                      "text",
                      "textarea",
                      "select",
                      "select-radiobuttons",
                      "multiselect",
                      "multiselect-checkboxes",
                      "html5-email",
                      "html5-tel",
                      "html5-url",
                      "html5-number",
                      "html5-range",
                      "html5-datetime-local",
                      "html5-date",
                      "html5-month",
                      "html5-week",
                      "html5-time",
                    ]}
                    {...props}
                  />
                ) : (
                  <TextInput
                    aria-label={t("customValue")}
                    data-testid={props.name}
                    {...props}
                    {...register(props.name)}
                  />
                )
              }
            />
          </GridItem>
        </Grid>
      </FormGroup>
    </FormAccess>
  );
};
