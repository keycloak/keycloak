import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ValidatedOptions,
  TextInput,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";

import type CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";
import { HelpItem, KeycloakTextArea } from "@keycloak/keycloak-ui-shared";

type AddCrossDomainTrustProps = {
  isOpen: boolean;
  onAdded: (config: CrossDomainTrustConfig, edit: boolean) => void;
  onClose: () => void;
  edit?: CrossDomainTrustConfig;
};

export const AddCrossDomainTrustDialig = ({
  isOpen,
  onAdded,
  onClose,
  edit,
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
    setValue,
  } = form;

  useEffect(
    () => setupForm(edit ? edit : ({} as CrossDomainTrustConfig)),
    [edit],
  );

  const setupForm = (config: CrossDomainTrustConfig) => {
    setValue("issuer", config.issuer);
    setValue("audience", config.audience);
    setValue("certificate", config.certificate);
  };

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
    onAdded(config, !!edit);
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
          {t("save")}
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
        >
          <TextInput
            id="issuer"
            {...register("issuer", { required: true })}
            validated={
              errors.issuer ? ValidatedOptions.error : ValidatedOptions.default
            }
            isDisabled={!!edit}
          />
          {errors.issuer && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem variant={ValidatedOptions.error}>
                  {t("required")}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
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
        >
          <TextInput
            id="audience"
            {...register("audience", { required: true })}
            validated={
              errors.audience
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
          {errors.audience && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem variant={ValidatedOptions.error}>
                  {t("required")}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
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
          {errors.certificate && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem variant={ValidatedOptions.error}>
                  {t("required")}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
        </FormGroup>
      </Form>
    </Modal>
  );
};
