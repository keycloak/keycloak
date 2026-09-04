import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormSubmitButton } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  convertAttributeNameToForm,
  convertFormValuesToObject,
  convertToFormValues,
} from "../util";
import { IdentityProviderSelect } from "./IdentityProviderSelect";

type LinkIdentityProviderModalProps = {
  orgId: string;
  identityProvider?: IdentityProviderRepresentation;
  onClose: () => void;
};

type LinkRepresentation = {
  alias: string[] | string;
  hideOnLogin: boolean;
  config: Record<string, string>;
};

export const LinkIdentityProviderModal = ({
  orgId,
  identityProvider,
  onClose,
}: LinkIdentityProviderModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const form = useForm<LinkRepresentation>({ mode: "onChange" });
  const { handleSubmit, formState, setValue } = form;

  useEffect(
    () =>
      convertToFormValues(
        {
          ...identityProvider,
          alias: [identityProvider?.alias],
          hideOnLogin: identityProvider?.hideOnLogin,
        },
        setValue,
      ),
    [],
  );

  const submitForm = async (data: LinkRepresentation) => {
    try {
      const foundIdentityProvider = await adminClient.identityProviders.findOne(
        {
          alias: data.alias[0],
        },
      );
      if (!foundIdentityProvider) {
        throw new Error(t("notFound"));
      }
      const { config } = convertFormValuesToObject(data);
      foundIdentityProvider.config = {
        ...foundIdentityProvider.config,
        ...config,
      };
      foundIdentityProvider.hideOnLogin = data.hideOnLogin;
      await adminClient.identityProviders.update(
        { alias: data.alias[0] },
        foundIdentityProvider,
      );

      if (!identityProvider) {
        await adminClient.organizations.linkIdp({
          orgId,
          alias: data.alias[0],
        });
      }
      addAlert(
        t(!identityProvider ? "linkSuccessful" : "linkUpdatedSuccessful"),
      );
      onClose();
    } catch (error) {
      addError(!identityProvider ? "linkError" : "linkUpdatedError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("linkIdentityProvider")}
      isOpen
      onClose={onClose}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid="confirm"
          key="confirm"
          form="form"
          allowInvalid
          allowNonDirty
        >
          {t("save")}
        </FormSubmitButton>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="form" onSubmit={handleSubmit(submitForm)}>
          <IdentityProviderSelect
            name="alias"
            label={t("identityProvider")}
            defaultValue={[]}
            isRequired
            isDisabled={!!identityProvider}
          />
          <DefaultSwitchControl
            name="hideOnLogin"
            label={t("hideOnLoginPage")}
            labelIcon={t("hideOnLoginPageHelp")}
            defaultValue={true}
          />
          <DefaultSwitchControl
            name={convertAttributeNameToForm(
              "config.kc.org.broker.login.hide-when-org-unknown",
            )}
            label={t("hideOnLoginWhenOrgNotResolved")}
            labelIcon={t("hideOnLoginWhenOrgNotResolvedHelp")}
            stringify
          />
          <DefaultSwitchControl
            name={convertAttributeNameToForm(
              "config.kc.org.broker.login.show-when-linked-elsewhere",
            )}
            label={t("showOnLoginForUnlinkedMembers")}
            labelIcon={t("showOnLoginForUnlinkedMembersHelp")}
            stringify
          />
        </Form>
      </FormProvider>
    </Modal>
  );
};
