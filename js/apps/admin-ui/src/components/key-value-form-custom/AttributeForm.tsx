import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { ActionGroup, Button } from "@patternfly/react-core";
import { FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../form/FormAccess";
import type { KeyValueType } from "./key-value-convert";
import { KeyValueInput } from "./KeyValueInput";

export type AttributeForm = Omit<RoleRepresentation, "attributes"> & {
  attributes?: KeyValueType[];
};

export type AttributesFormProps = {
  form: UseFormReturn<AttributeForm>;
  save?: (model: AttributeForm) => void;
  reset?: () => void;
  fineGrainedAccess?: boolean;
  allowFullClear?: boolean;
};

export const AttributesForm = ({
  form,
  reset,
  save,
  fineGrainedAccess,
  allowFullClear,
}: AttributesFormProps) => {
  const { t } = useTranslation("roles");
  const noSaveCancelButtons = !save && !reset;
  const {
    formState: { isDirty },
    handleSubmit,
  } = form;

  return (
    <FormAccess
      role="manage-realm"
      onSubmit={save ? handleSubmit(save) : undefined}
      fineGrainedAccess={fineGrainedAccess}
    >
      <FormProvider {...form}>
        <KeyValueInput name="attributes" allowFullClear={allowFullClear} />
      </FormProvider>
      {!noSaveCancelButtons && (
        <ActionGroup className="kc-attributes__action-group">
          <Button
            data-testid="save-attributes"
            variant="primary"
            type="submit"
            isDisabled={!isDirty}
          >
            {t("common:save")}
          </Button>
          <Button onClick={reset} variant="link" isDisabled={!isDirty}>
            {t("common:revert")}
          </Button>
        </ActionGroup>
      )}
    </FormAccess>
  );
};
