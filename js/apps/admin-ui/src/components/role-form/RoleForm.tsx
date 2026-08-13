import { ActionGroup, Button, PageSection } from "@patternfly/react-core";
import {
  SubmitHandler,
  UseFormReturn,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, To } from "react-router-dom";
import {
  FormSubmitButton,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";

import { FormAccess } from "../form/FormAccess";
import { AttributeForm } from "../key-value-form/AttributeForm";
import { ViewHeader } from "../view-header/ViewHeader";

export type RoleFormProps = {
  form: UseFormReturn<AttributeForm>;
  onSubmit: SubmitHandler<AttributeForm>;
  cancelLink: To;
  role: "manage-realm" | "manage-clients";
  editMode: boolean;
};

export const RoleForm = ({
  form: { formState },
  onSubmit,
  cancelLink,
  role,
  editMode,
}: RoleFormProps) => {
  const { t } = useTranslation();
  const { control, handleSubmit } = useFormContext<AttributeForm>();

  const roleName = useWatch({
    control,
    defaultValue: undefined,
    name: "name",
  });

  return (
    <>
      {!editMode && <ViewHeader titleKey={t("createRole")} />}
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
          role={role}
          className="pf-v5-u-mt-lg"
          fineGrainedAccess={true} // We would never want to show this form in read-only mode
        >
          <TextControl
            name="name"
            label={t("roleName")}
            rules={{
              required: !editMode ? t("required") : undefined,
              validate(value) {
                if (!value?.trim()) {
                  return t("required");
                }
              },
            }}
            isDisabled={editMode}
          />
          <TextAreaControl
            name="description"
            label={t("description")}
            rules={{
              maxLength: {
                value: 255,
                message: t("maxLength", { length: 255 }),
              },
            }}
            isDisabled={roleName?.includes("default-roles") ?? false}
          />
          <ActionGroup>
            <FormSubmitButton
              formState={formState}
              data-testid="save"
              allowInvalid
              allowNonDirty
            >
              {t("save")}
            </FormSubmitButton>
            <Button
              data-testid="cancel"
              variant="link"
              component={(props) => <Link {...props} to={cancelLink} />}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
