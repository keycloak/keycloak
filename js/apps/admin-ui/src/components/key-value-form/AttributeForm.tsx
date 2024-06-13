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
  name?: string;
  isDisabled?: boolean;
};

export const AttributesForm = ({
  form,
  reset,
  save,
  fineGrainedAccess,
  name = "attributes",
  isDisabled = false,
}: AttributesFormProps) => {
  const { t } = useTranslation();
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
        <KeyValueInput name={name} isDisabled={isDisabled} />
      </FormProvider>
      {!noSaveCancelButtons && (
        <ActionGroup className="kc-attributes__action-group">
          <Button
            data-testid="save-attributes"
            variant="primary"
            type="submit"
            isDisabled={!isDirty}
          >
            {t("save")}
          </Button>
          <Button onClick={reset} variant="link" isDisabled={!isDirty}>
            {t("revert")}
          </Button>
        </ActionGroup>
      )}
    </FormAccess>
  );
};
