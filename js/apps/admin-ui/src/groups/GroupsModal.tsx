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
import { FormSubmitButton, TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type GroupsModalProps = {
  id?: string;
  rename?: GroupRepresentation;
  duplicate?: GroupRepresentation;
  handleModalToggle: () => void;
  refresh: (group?: GroupRepresentation) => void;
};

export const GroupsModal = ({
  id,
  rename,
  duplicate,
  handleModalToggle,
  refresh,
}: GroupsModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const defaultName = duplicate ? `Copy of ${duplicate.name}` : rename?.name;

  const form = useForm({
    defaultValues: { name: defaultName },
  });
  const { handleSubmit, formState } = form;

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
      } else if (duplicate) {
        const newGroup = { ...duplicate, name: group.name };
        delete newGroup.id;
        await adminClient.groups.create(newGroup);
      } else {
        await (group.id
          ? adminClient.groups.updateChildGroup({ id }, group)
          : adminClient.groups.createChildGroup({ id }, group));
      }

      refresh(rename ? { ...rename, name: group.name } : undefined);
      handleModalToggle();
      addAlert(
        t(
          rename
            ? "groupUpdated"
            : duplicate
              ? "groupDuplicated"
              : "groupCreated",
        ),
        AlertVariant.success,
      );
    } catch (error) {
      addError("couldNotCreateGroup", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(
        rename
          ? "renameAGroup"
          : duplicate
            ? "duplicateAGroup"
            : "createAGroup",
      )}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid={`${rename ? "rename" : duplicate ? "duplicate" : "create"}Group`}
          key="confirm"
          form="group-form"
          allowInvalid
          allowNonDirty
        >
          {t(rename ? "rename" : duplicate ? "duplicate" : "create")}
        </FormSubmitButton>,
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
