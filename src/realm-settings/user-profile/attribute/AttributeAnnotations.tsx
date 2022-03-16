import React from "react";
import { FormGroup, Grid, GridItem } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../../components/form-access/FormAccess";
import "../../realm-settings-section.css";
import { FormProvider, useFormContext } from "react-hook-form";
import {
  AttributeInput,
  AttributeType,
} from "../../../components/attribute-input/AttributeInput";

export type AttributeAnnotationsProps = {
  isKeySelectable?: boolean;
  selectableValues?: AttributeType[];
};

export const AttributeAnnotations = ({
  isKeySelectable,
  selectableValues,
}: AttributeAnnotationsProps) => {
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
              <AttributeInput
                isKeySelectable={isKeySelectable}
                selectableValues={selectableValues}
                name="annotations"
              />
            </FormProvider>
          </GridItem>
        </Grid>
      </FormGroup>
    </FormAccess>
  );
};
