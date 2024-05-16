import { Checkbox, FormGroup, Grid, GridItem } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../../components/form/FormAccess";
import { HelpItem } from "@keycloak/keycloak-ui-shared";

import "../../realm-settings-section.css";

const Permissions = ({ name }: { name: string }) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <Grid>
      <Controller
        name={`permissions.${name}`}
        control={control}
        defaultValue={[]}
        render={({ field }) => (
          <>
            <GridItem lg={4} sm={6}>
              <Checkbox
                id={`user-${name}`}
                label={t("user")}
                value="user"
                data-testid={`user-${name}`}
                isChecked={field.value.includes("user")}
                onChange={() => {
                  const option = "user";
                  const changedValue = field.value.includes(option)
                    ? field.value.filter((item: string) => item !== option)
                    : [...field.value, option];

                  field.onChange(changedValue);
                }}
              />
            </GridItem>
            <GridItem lg={8} sm={6}>
              <Checkbox
                id={`admin-${name}`}
                label={t("admin")}
                value="admin"
                data-testid={`admin-${name}`}
                isChecked={field.value.includes("admin")}
                onChange={() => {
                  const option = "admin";
                  const changedValue = field.value.includes(option)
                    ? field.value.filter((item: string) => item !== option)
                    : [...field.value, option];

                  field.onChange(changedValue);
                }}
              />
            </GridItem>
          </>
        )}
      />
    </Grid>
  );
};

export const AttributePermission = () => {
  const { t } = useTranslation();

  return (
    <FormAccess role="manage-realm" isHorizontal>
      <FormGroup
        hasNoPaddingTop
        label={t("whoCanEdit")}
        labelIcon={
          <HelpItem helpText={t("whoCanEditHelp")} fieldLabelId="whoCanEdit" />
        }
        fieldId="kc-who-can-edit"
      >
        <Permissions name="edit" />
      </FormGroup>
      <FormGroup
        hasNoPaddingTop
        label={t("whoCanView")}
        labelIcon={
          <HelpItem helpText={t("whoCanViewHelp")} fieldLabelId="whoCanView" />
        }
        fieldId="kc-who-can-view"
      >
        <Permissions name="view" />
      </FormGroup>
    </FormAccess>
  );
};
