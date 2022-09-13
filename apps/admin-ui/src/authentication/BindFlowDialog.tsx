import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  Modal,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Select,
  SelectVariant,
  SelectOption,
  AlertVariant,
} from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import useToggle from "../utils/useToggle";
import { REALM_FLOWS } from "./AuthenticationSection";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

type BindingForm = {
  bindingType: keyof RealmRepresentation;
};

type BindFlowDialogProps = {
  flowAlias: string;
  onClose: () => void;
};

export const BindFlowDialog = ({ flowAlias, onClose }: BindFlowDialogProps) => {
  const { t } = useTranslation("authentication");
  const { control, handleSubmit } = useForm<BindingForm>();

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [open, toggle] = useToggle();

  const save = async ({ bindingType }: BindingForm) => {
    const realmRep = await adminClient.realms.findOne({ realm });

    try {
      await adminClient.realms.update(
        { realm },
        { ...realmRep, [bindingType]: flowAlias }
      );
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updateFlowError", error);
    }

    onClose();
  };

  return (
    <Modal
      title={t("bindFlow")}
      isOpen
      variant="small"
      onClose={onClose}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          data-testid="save"
          type="submit"
          form="bind-form"
        >
          {t("common:save")}
        </Button>,
        <Button
          data-testid="cancel"
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form id="bind-form" isHorizontal onSubmit={handleSubmit(save)}>
        <FormGroup label={t("chooseBindingType")} fieldId="chooseBindingType">
          <Controller
            name="bindingType"
            defaultValue={"browserFlow"}
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="chooseBindingType"
                onToggle={toggle}
                onSelect={(_, value) => {
                  onChange(value.toString());
                  toggle();
                }}
                selections={value}
                variant={SelectVariant.single}
                aria-label={t("bindingFlow")}
                isOpen={open}
                menuAppendTo="parent"
              >
                {[...REALM_FLOWS.keys()]
                  .filter((f) => f !== "dockerAuthenticationFlow")
                  .map((key) => {
                    const value = REALM_FLOWS.get(key);
                    return (
                      <SelectOption
                        selected={key === REALM_FLOWS.get(key)}
                        key={key}
                        value={key}
                      >
                        {t(`flow.${value}`)}
                      </SelectOption>
                    );
                  })}
              </Select>
            )}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
