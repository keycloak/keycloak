import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import {
  Alert,
  AlertVariant,
  Button,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { useFetch } from "@keycloak/keycloak-ui-shared";

type CreateVerifiableCredentialModalProps = {
  userId: string;
  onClose: () => void;
  onCreated: () => void;
};

type FormValues = {
  credentialScopeName: string;
};

export const CreateVerifiableCredentialModal = ({
  userId,
  onClose,
  onCreated,
}: CreateVerifiableCredentialModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const [oid4vcScopes, setOid4vcScopes] = useState<ClientScopeRepresentation[]>(
    [],
  );

  const form = useForm<FormValues>({
    mode: "onChange",
  });

  const {
    handleSubmit,
    formState: { isValid, isSubmitting },
  } = form;

  useFetch(
    async () => {
      const scopes = await adminClient.clientScopes.find();
      return scopes.filter((scope) => scope.protocol === "oid4vc");
    },
    (scopes) => setOid4vcScopes(scopes),
    [],
  );

  const onSubmit = async (data: FormValues) => {
    try {
      await adminClient.users.createVerifiableCredential(
        { id: userId },
        { credentialScopeName: data.credentialScopeName },
      );
      addAlert(t("createVerifiableCredentialSuccess"), AlertVariant.success);
      onCreated();
    } catch (error) {
      addError("createVerifiableCredentialError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("createVerifiableCredential")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          key="create"
          variant="primary"
          onClick={handleSubmit(onSubmit)}
          isDisabled={!isValid || isSubmitting || oid4vcScopes.length === 0}
          isLoading={isSubmitting}
        >
          {t("create")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      {oid4vcScopes.length === 0 ? (
        <Alert
          variant="warning"
          isInline
          title={t("noOid4vcScopesAvailable")}
        />
      ) : (
        <FormProvider {...form}>
          <SelectControl
            name="credentialScopeName"
            label={t("selectCredentialScope")}
            labelIcon={t("credentialScopeName")}
            controller={{
              defaultValue: "",
              rules: { required: t("required") },
            }}
            options={oid4vcScopes.map((scope) => ({
              key: scope.name!,
              value: scope.name!,
            }))}
          />
        </FormProvider>
      )}
    </Modal>
  );
};
