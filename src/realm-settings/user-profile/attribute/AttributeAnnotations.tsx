import React from "react";
import { FormGroup, Grid, GridItem } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { FormProvider, useFormContext } from "react-hook-form";
import { AttributeInput } from "../../../components/attribute-input/AttributeInput";

import "../../realm-settings-section.css";

export const AttributeAnnotations = () => {
  const { t } = useTranslation("realm-settings");
  const form = useFormContext();

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
            <FormProvider {...form}>
              <AttributeInput name="annotations" />
            </FormProvider>
          </GridItem>
        </Grid>
      </FormGroup>
    </FormAccess>
  );
};
