import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";
import useToggle from "../utils/useToggle";
import { REALM_FLOWS } from "./AuthenticationSection";

type BindingForm = {
  bindingType: keyof RealmRepresentation;
};

type BindFlowDialogProps = {
  flowAlias: string;
  onClose: () => void;
};

export const BindFlowDialog = ({ flowAlias, onClose }: BindFlowDialogProps) => {
  const { t } = useTranslation();
  const { control, handleSubmit } = useForm<BindingForm>();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [open, toggleOpen] = useToggle();

  const onSubmit = async ({ bindingType }: BindingForm) => {
    const realmRep = await adminClient.realms.findOne({ realm });

    try {
      await adminClient.realms.update(
        { realm },
        { ...realmRep, [bindingType]: flowAlias },
      );
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateFlowError", error);
    }

    onClose();
  };

  return (
    <Modal
      title={t("bindFlow")}
      variant="small"
      onClose={onClose}
      actions={[
        <Button key="confirm" data-testid="save" type="submit" form="bind-form">
          {t("save")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
      isOpen
    >
      <Form id="bind-form" isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <FormGroup label={t("chooseBindingType")} fieldId="chooseBindingType">
          <Controller
            name="bindingType"
            defaultValue="browserFlow"
            control={control}
            render={({ field }) => (
              <Select
                toggleId="chooseBindingType"
                onToggle={toggleOpen}
                onSelect={(_, value) => {
                  field.onChange(value.toString());
                  toggleOpen();
                }}
                selections={field.value}
                variant={SelectVariant.single}
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
