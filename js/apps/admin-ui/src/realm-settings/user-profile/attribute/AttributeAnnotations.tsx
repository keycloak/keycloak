import { useTranslation } from "react-i18next";
import { FormGroup, Grid, GridItem } from "@patternfly/react-core";

import { FormAccess } from "../../../components/form/FormAccess";
import { KeyValueInput } from "../../../components/key-value-form/KeyValueInput";

import "../../realm-settings-section.css";

export const AttributeAnnotations = () => {
  const { t } = useTranslation();

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
              defaultKeyValue={[
                {
                  key: "inputType",
                  label: t("inputType"),
                  values: [
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
                  ],
                },
                {
                  key: "inputHelperTextBefore",
                  label: t("inputHelperTextBefore"),
                },
                {
                  key: "inputHelperTextAfter",
                  label: t("inputHelperTextAfter"),
                },
                {
                  key: "inputOptionLabelsI18nPrefix",
                  label: t("inputOptionLabelsI18nPrefix"),
                },
                {
                  key: "inputTypePlaceholder",
                  label: t("inputTypePlaceholder"),
                },
                {
                  key: "inputTypeSize",
                  label: t("inputTypeSize"),
                },
                {
                  key: "inputTypeCols",
                  label: t("inputTypeCols"),
                },
                {
                  key: "inputTypeRows",
                  label: t("inputTypeRows"),
                },
                {
                  key: "inputTypeStep",
                  label: t("inputTypeStep"),
                },
                {
                  key: "kcNumberFormat",
                  label: t("kcNumberFormat"),
                },
                {
                  key: "kcNumberUnFormat",
                  label: t("kcNumberUnFormat"),
                },
              ]}
            />
          </GridItem>
        </Grid>
      </FormGroup>
    </FormAccess>
  );
};
