import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ValidatedOptions,
} from "@patternfly/react-core";

import type CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";

import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../components/keycloak-text-area/KeycloakTextArea";

type AddCrossDomainTrustProps = {
  isOpen: boolean;
  onAdded: (config: CrossDomainTrustConfig) => void;
  onClose: () => void;
};

export const AddCrossDomainTrustDialig = ({
  isOpen,
  onAdded,
  onClose,
}: AddCrossDomainTrustProps) => {
  const { t } = useTranslation();
  const form = useForm<CrossDomainTrustConfig>({
    mode: "onChange",
    reValidateMode: "onChange",
  });
  const {
    register,
    getValues,
    reset,
    formState: { errors, isValid },
  } = form;

  const onSubmit = async () => {
    if (!(await form.trigger())) {
      return;
    }

    const config: CrossDomainTrustConfig = {
      issuer: getValues("issuer"),
      audience: getValues("audience"),
      certificate: getValues("certificate"),
    };

    // initiate callback and reset the dialog form
    onAdded(config);
    reset();
    onClose();
  };

  return (
    <Modal
      title={t("crossDomainTrustConfigAddTitle")}
      isOpen={isOpen}
      onClose={onClose}
      variant="small"
      actions={[
        <Button
          id="add-config-confirm"
          key="confirm"
          form="config-form"
          isDisabled={!isValid}
          onClick={() => onSubmit()}
        >
          {t("add")}
        </Button>,
        <Button
          id="add-config-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => onClose()}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form id="config-form" isHorizontal>
        <FormGroup
          label={t("crossDomainTrustConfigIssuer")}
          fieldId="issuer"
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("crossDomainTrustConfigIssuerHelp")}
              fieldLabelId="issuer"
            />
          }
          validated={
            errors.issuer ? ValidatedOptions.error : ValidatedOptions.default
          }
          helperTextInvalid={t("required")}
        >
          <KeycloakTextInput
            id="issuer"
            {...register("issuer", { required: true })}
            validated={
              errors.issuer ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          label={t("crossDomainTrustConfigAudience")}
          fieldId="audience"
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("crossDomainTrustConfigAudienceHelp")}
              fieldLabelId="audience"
            />
          }
          validated={
            errors.audience ? ValidatedOptions.error : ValidatedOptions.default
          }
          helperTextInvalid={t("required")}
        >
          <KeycloakTextInput
            id="audience"
            {...register("audience", { required: true })}
            validated={
              errors.audience
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          label={t("crossDomainTrustConfigCert")}
          fieldId="certificate"
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("crossDomainTrustConfigCertHelp")}
              fieldLabelId="certificate"
            />
          }
          validated={
            errors.certificate
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          helperTextInvalid={t("required")}
        >
          <KeycloakTextArea
            id="certificate"
            data-testid="certificate"
            {...register("certificate", { required: true })}
            validated={
              errors.certificate
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
