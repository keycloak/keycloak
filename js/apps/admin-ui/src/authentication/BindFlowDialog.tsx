import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { SelectControl, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import { REALM_FLOWS } from "./constants";

type BindingForm = {
  bindingType: keyof RealmRepresentation;
};

type BindFlowDialogProps = {
  flowAlias: string;
  onClose: (used?: boolean) => void;
};

export const BindFlowDialog = ({ flowAlias, onClose }: BindFlowDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<BindingForm>();
  const { addAlert, addError } = useAlerts();
  const { realm, realmRepresentation: realmRep, refresh } = useRealm();

  const onSubmit = async ({ bindingType }: BindingForm) => {
    try {
      await adminClient.realms.update(
        { realm },
        { ...realmRep, [bindingType]: flowAlias },
      );
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateFlowError", error);
    }

    onClose(true);
  };

  const flowKeys = Array.from(REALM_FLOWS.keys());

  return (
    <Modal
      title={t("bindFlow")}
      variant="small"
      onClose={() => onClose()}
      actions={[
        <Button key="confirm" data-testid="save" type="submit" form="bind-form">
          {t("save")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => onClose()}
        >
          {t("cancel")}
        </Button>,
      ]}
      isOpen
    >
      <Form id="bind-form" isHorizontal onSubmit={form.handleSubmit(onSubmit)}>
        <FormProvider {...form}>
          <SelectControl
            id="chooseBindingType"
            name="bindingType"
            label={t("chooseBindingType")}
            options={flowKeys
              .filter((f) => f !== "dockerAuthenticationFlow")
              .map((key) => ({
                key,
                value: t(`flow.${REALM_FLOWS.get(key)}`),
              }))}
            controller={{ defaultValue: flowKeys[0] }}
            menuAppendTo="parent"
            aria-label={t("chooseBindingType")}
          />
        </FormProvider>
      </Form>
    </Modal>
  );
};
