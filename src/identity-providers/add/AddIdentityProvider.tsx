import React from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ClipboardCopy,
  FormGroup,
  NumberInput,
  PageSection,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import IdentityProviderRepresentation from "keycloak-admin/lib/defs/identityProviderRepresentation";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { getBaseUrl, toUpperCase } from "../../util";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";

export const AddIdentityProvider = () => {
  const { t } = useTranslation("identity-providers");
  const { t: th } = useTranslation("identity-providers-help");
  const { id } = useParams<{ id: string }>();
  const {
    handleSubmit,
    register,
    errors,
    control,
    formState: { isDirty },
  } = useForm<IdentityProviderRepresentation>();

  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

  const callbackUrl = `${getBaseUrl(adminClient)}/realms/${realm}/broker`;

  const save = async (provider: IdentityProviderRepresentation) => {
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId: id,
        alias: id,
      });
      addAlert(t("createSuccess"), AlertVariant.success);
      history.push(`/${realm}/identity-providers`);
    } catch (error) {
      addAlert(t("createError", { error }), AlertVariant.danger);
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={t("addIdentityProvider", { provider: toUpperCase(id) })}
      />
      <PageSection variant="light">
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("redirectURI")}
            labelIcon={
              <HelpItem
                helpText={th("redirectURI")}
                forLabel={t("redirectURI")}
                forID="kc-redirect-uri"
              />
            }
            fieldId="kc-redirect-uri"
          >
            <ClipboardCopy
              isReadOnly
            >{`${callbackUrl}/${id}/endpoint`}</ClipboardCopy>
          </FormGroup>
          <FormGroup
            label={t("clientId")}
            labelIcon={
              <HelpItem
                helpText={th("clientId")}
                forLabel={t("clientId")}
                forID="kc-client-id"
              />
            }
            fieldId="kc-client-id"
            isRequired
            validated={
              errors.config && errors.config.clientId
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              isRequired
              type="text"
              id="kc-client-id"
              data-testid="clientId"
              name="config.clientId"
              ref={register({ required: true })}
            />
          </FormGroup>
          <FormGroup
            label={t("clientSecret")}
            labelIcon={
              <HelpItem
                helpText={th("clientSecret")}
                forLabel={t("clientSecret")}
                forID="kc-client-secret"
              />
            }
            fieldId="kc-client-secret"
            isRequired
            validated={
              errors.config && errors.config.clientSecret
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              isRequired
              type="password"
              id="kc-client-secret"
              data-testid="clientSecret"
              name="config.clientSecret"
              ref={register({ required: true })}
            />
          </FormGroup>
          <FormGroup
            label={t("displayOrder")}
            labelIcon={
              <HelpItem
                helpText={th("displayOrder")}
                forLabel={t("displayOrder")}
                forID="kc-display-order"
              />
            }
            fieldId="kc-display-order"
          >
            <Controller
              name="config.guiOrder"
              control={control}
              defaultValue={0}
              render={({ onChange, value }) => (
                <NumberInput
                  value={value}
                  data-testid="displayOrder"
                  onMinus={() => onChange(value - 1)}
                  onChange={onChange}
                  onPlus={() => onChange(value + 1)}
                  inputName="input"
                  inputAriaLabel={t("displayOrder")}
                  minusBtnAriaLabel={t("common:minus")}
                  plusBtnAriaLabel={t("common:plus")}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button
              isDisabled={!isDirty}
              variant="primary"
              type="submit"
              data-testid="createProvider"
            >
              {t("common:add")}
            </Button>
            <Button
              variant="link"
              data-testid="cancel"
              onClick={() => history.push(`/${realm}/identity-providers`)}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
