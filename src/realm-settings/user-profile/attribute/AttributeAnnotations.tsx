import { useTranslation } from "react-i18next";
import { FormGroup, Grid, GridItem } from "@patternfly/react-core";

import { FormAccess } from "../../../components/form-access/FormAccess";
import { KeyValueInput } from "../../../components/key-value-form/KeyValueInput";

import "../../realm-settings-section.css";

export const AttributeAnnotations = () => {
  const { t } = useTranslation("realm-settings");

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
            <KeyValueInput name="annotations" />
          </GridItem>
        </Grid>
      </FormGroup>
    </FormAccess>
  );
};
