import {
  ActionGroup,
  Button,
  FormGroup,
  ValidatedOptions,
  TextInput,
} from "@patternfly/react-core";
import { Controller, useFormContext, useWatch } from "react-hook-form";

import { useTranslation } from "react-i18next";

import {
  SwitchControl,
  TextControl,
  TextAreaControl,
  HelpItem,
} from "@keycloak/keycloak-ui-shared";

import { FormFields } from "../ClientDetails";
import { beerify, convertAttributeNameToForm } from "../../util";
import { FormAccess } from "../../components/form/FormAccess";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";

type SamlAttributeQueryConfigProps = {
  save: () => void;
  reset: () => void;
};

export const SamlAttributeQueryConfig = ({
  save,
  reset,
}: SamlAttributeQueryConfigProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<FormFields>();
  const enabled = useWatch({
    control: control,
    name: convertAttributeNameToForm(
      "attributes.saml.attributeQuery.supported",
    ),
  });

  const validate = (name: string) =>
    errors.attributes?.[beerify(name)]
      ? ValidatedOptions.error
      : ValidatedOptions.default;

  return (
    <FormAccess role="manage-realm" isHorizontal>
      <SwitchControl
        name={convertAttributeNameToForm(
          "attributes.saml.attributeQuery.supported",
        )}
        label={t("samlAttributeQueryConfig.supported.label")}
        labelIcon={t("samlAttributeQueryConfig.supported.help")}
        labelOn=""
        labelOff=""
        stringify
      />
      {enabled == "true" && (
        <>
          <FormGroup
            label={t("samlAttributeQueryConfig.issuer.label")}
            labelIcon={
              <HelpItem
                helpText={t("samlAttributeQueryConfig.issuer.help")}
                fieldLabelId="issuer"
              />
            }
            fieldId="issuer"
            isRequired
          >
            <TextInput
              id="issuer"
              data-testid="issuer"
              validated={validate("saml.attributeQuery.issuer")}
              {...register(
                convertAttributeNameToForm(
                  "attributes.saml.attributeQuery.issuer",
                ),
                { required: true },
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("samlAttributeQueryConfig.targetAudience.label")}
            labelIcon={
              <HelpItem
                helpText={t("samlAttributeQueryConfig.targetAudience.help")}
                fieldLabelId="audience"
              />
            }
            fieldId="audience"
            isRequired
          >
            <TextInput
              id="audience"
              data-testid="audience"
              validated={validate("saml.attributeQuery.targetAudience")}
              {...register(
                convertAttributeNameToForm(
                  "attributes.saml.attributeQuery.targetAudience",
                ),
                { required: true },
              )}
            />
          </FormGroup>
          <TextAreaControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.signingCert",
            )}
            label={t("samlAttributeQueryConfig.signingCert.label")}
            labelIcon={t("samlAttributeQueryConfig.signingCert.help")}
          />
          <TextAreaControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.encryptionCert",
            )}
            label={t("samlAttributeQueryConfig.encryptionCert.label")}
            labelIcon={t("samlAttributeQueryConfig.encryptionCert.help")}
          />
          <TextControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.userLookupAttribute",
            )}
            label={t("samlAttributeQueryConfig.userLookupAttribute.label")}
            labelIcon={t("samlAttributeQueryConfig.userLookupAttribute.help")}
          />
          <FormGroup
            label={t("samlAttributeQueryConfig.filters.label")}
            labelIcon={
              <HelpItem
                helpText={t("samlAttributeQueryConfig.filters.help")}
                fieldLabelId="samlAttributeQueryConfig.filters.label"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.saml.attributeQuery.filters",
              )}
              defaultValue={[]}
              control={control}
              render={() => (
                <MultiLineInput
                  name={convertAttributeNameToForm(
                    "attributes.saml.attributeQuery.filters",
                  )}
                  aria-label={t("samlAttributeQueryConfig.filters.label")}
                  addButtonLabel="samlAttributeQueryConfig.filters.addLabel"
                />
              )}
            />
          </FormGroup>
          <SwitchControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.requireSignedRequest",
            )}
            label={t("samlAttributeQueryConfig.requireSignedRequest.label")}
            labelIcon={t("samlAttributeQueryConfig.requireSignedRequest.help")}
            labelOn=""
            labelOff=""
            stringify
          />
          <SwitchControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.requireEncryptedRequest",
            )}
            label={t("samlAttributeQueryConfig.requireEncryptedRequest.label")}
            labelIcon={t(
              "samlAttributeQueryConfig.requireEncryptedRequest.help",
            )}
            labelOn=""
            labelOff=""
            stringify
          />
          <SwitchControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.signResponseDocument",
            )}
            label={t("samlAttributeQueryConfig.signResponseDocument.label")}
            labelIcon={t("samlAttributeQueryConfig.signResponseDocument.help")}
            labelOn=""
            labelOff=""
            stringify
          />
          <SwitchControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.signResponseAssertion",
            )}
            label={t("samlAttributeQueryConfig.signResponseAssertion.label")}
            labelIcon={t("samlAttributeQueryConfig.signResponseAssertion.help")}
            labelOn=""
            labelOff=""
            stringify
          />
          <SwitchControl
            name={convertAttributeNameToForm(
              "attributes.saml.attributeQuery.encryptResponse",
            )}
            label={t("samlAttributeQueryConfig.encryptResponse.label")}
            labelIcon={t("samlAttributeQueryConfig.encryptResponse.help")}
            labelOn=""
            labelOff=""
            stringify
          />
        </>
      )}

      <ActionGroup>
        <Button
          variant="tertiary"
          onClick={save}
          data-testid="samlAttributeQuerySave"
        >
          {t("save")}
        </Button>
        <Button
          variant="link"
          onClick={reset}
          data-testid="samlAttributeQueryRevert"
        >
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
