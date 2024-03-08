import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { TextControl } from "ui-shared";
import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";

type GroupsModalProps = {
  id?: string;
  rename?: GroupRepresentation;
  handleModalToggle: () => void;
  refresh: (group?: GroupRepresentation) => void;
};

export const GroupsModal = ({
  id,
  rename,
  handleModalToggle,
  refresh,
}: GroupsModalProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const form = useForm({
    defaultValues: { name: rename?.name },
  });
  const { handleSubmit } = form;

  const submitForm = async (group: GroupRepresentation) => {
    group.name = group.name?.trim();

    try {
      if (!id) {
        await adminClient.groups.create(group);
      } else if (rename) {
        await adminClient.groups.update(
          { id },
          { ...rename, name: group.name },
        );
      } else {
        await (group.id
          ? adminClient.groups.updateChildGroup({ id }, group)
          : adminClient.groups.createChildGroup({ id }, group));
      }

      refresh(rename ? { ...rename, name: group.name } : undefined);
      handleModalToggle();
      addAlert(
        t(rename ? "groupUpdated" : "groupCreated"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("couldNotCreateGroup", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(rename ? "renameAGroup" : "createAGroup")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid={`${rename ? "rename" : "create"}Group`}
          key="confirm"
          variant="primary"
          type="submit"
          form="group-form"
        >
          {t(rename ? "rename" : "create")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="group-form" isHorizontal onSubmit={handleSubmit(submitForm)}>
          <TextControl
            name="name"
            label={t("name")}
            rules={{ required: t("required") }}
            autoFocus
          />
        </Form>
      </FormProvider>
    </Modal>
  );
};
