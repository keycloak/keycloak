import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

type ExtendedFieldsFormProps = {
  providerId: string;
};

export const ExtendedFieldsForm = ({ providerId }: ExtendedFieldsFormProps) => {
  switch (providerId) {
    case "facebook":
      return <FacebookFields />;
    case "github":
      return <GithubFields />;
    case "google":
      return <GoogleFields />;
    case "openshift-v3":
    case "openshift-v4":
      return <OpenshiftFields />;
    case "paypal":
      return <PaypalFields />;
    case "stackoverflow":
      return <StackoverflowFields />;
    default:
      return null;
  }
};

const FacebookFields = () => {
  const { t } = useTranslation("identity-providers");
  const { register } = useFormContext();

  return (
    <FormGroup
      label={t("facebook.fetchedFields")}
      labelIcon={
        <HelpItem
          helpText="identity-providers-help:facebook:fetchedFields"
          fieldLabelId="identity-providers:facebook:fetchedFields"
        />
      }
      fieldId="facebookFetchedFields"
    >
      <KeycloakTextInput
        id="facebookFetchedFields"
        name="config.fetchedFields"
        ref={register()}
      />
    </FormGroup>
  );
};

const GithubFields = () => {
  const { t } = useTranslation("identity-providers");
  const { register } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("baseUrl")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:baseUrl"
            fieldLabelId="identity-providers:baseUrl"
          />
        }
        fieldId="baseUrl"
      >
        <KeycloakTextInput
          id="baseUrl"
          name="config.baseUrl"
          ref={register()}
        />
      </FormGroup>
      <FormGroup
        label={t("apiUrl")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:apiUrl"
            fieldLabelId="identity-providers:apiUrl"
          />
        }
        fieldId="apiUrl"
      >
        <KeycloakTextInput id="apiUrl" name="config.apiUrl" ref={register()} />
      </FormGroup>
    </>
  );
};

const GoogleFields = () => {
  const { t } = useTranslation("identity-providers");
  const { register, control } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("google.hostedDomain")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:google:hostedDomain"
            fieldLabelId="identity-providers:google:hostedDomain"
          />
        }
        fieldId="googleHostedDomain"
      >
        <KeycloakTextInput
          id="googleHostedDomain"
          name="config.hostedDomain"
          ref={register()}
        />
      </FormGroup>
      <FormGroup
        label={t("google.userIp")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:google:userIp"
            fieldLabelId="identity-providers:google:userIp"
          />
        }
        fieldId="googleUserIp"
      >
        <Controller
          name="config.userIp"
          defaultValue="false"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="googleUserIp"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange(value.toString())}
              aria-label={t("google.userIp")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("google.offlineAccess")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:google:offlineAccess"
            fieldLabelId="identity-providers:google:offlineAccess"
          />
        }
        fieldId="googleOfflineAccess"
      >
        <Controller
          name="config.offlineAccess"
          defaultValue="false"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="googleOfflineAccess"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange(value.toString())}
              aria-label={t("google.offlineAccess")}
            />
          )}
        />
      </FormGroup>
    </>
  );
};

const OpenshiftFields = () => {
  const { t } = useTranslation("identity-providers");
  const {
    register,
    formState: { errors },
  } = useFormContext();

  return (
    <FormGroup
      label={t("baseUrl")}
      labelIcon={
        <HelpItem
          helpText="identity-providers-help:openshift:baseUrl"
          fieldLabelId="identity-providers:baseUrl"
        />
      }
      fieldId="baseUrl"
      isRequired
      validated={
        errors.config?.baseUrl
          ? ValidatedOptions.error
          : ValidatedOptions.default
      }
      helperTextInvalid={t("common:required")}
    >
      <KeycloakTextInput
        id="baseUrl"
        name="config.baseUrl"
        ref={register({ required: true })}
        isRequired
      />
    </FormGroup>
  );
};

const PaypalFields = () => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("paypal.sandbox")}
      labelIcon={
        <HelpItem
          helpText="identity-providers-help:paypal:sandbox"
          fieldLabelId="identity-providers:paypal:sandbox"
        />
      }
      fieldId="paypalSandbox"
    >
      <Controller
        name="config.sandbox"
        defaultValue="false"
        control={control}
        render={({ onChange, value }) => (
          <Switch
            id="paypalSandbox"
            label={t("common:on")}
            labelOff={t("common:off")}
            isChecked={value === "true"}
            onChange={(value) => onChange(value.toString())}
            aria-label={t("paypal.sandbox")}
          />
        )}
      />
    </FormGroup>
  );
};

const StackoverflowFields = () => {
  const { t } = useTranslation("identity-providers");
  const {
    register,
    formState: { errors },
  } = useFormContext();

  return (
    <FormGroup
      label={t("stackoverflow.key")}
      labelIcon={
        <HelpItem
          helpText="identity-providers-help:stackoverflow:key"
          fieldLabelId="identity-providers:stackoverflow:key"
        />
      }
      fieldId="stackoverflowKey"
      isRequired
      validated={
        errors.config?.key ? ValidatedOptions.error : ValidatedOptions.default
      }
      helperTextInvalid={t("common:required")}
    >
      <KeycloakTextInput
        id="stackoverflowKey"
        name="config.key"
        ref={register({ required: true })}
        isRequired
      />
    </FormGroup>
  );
};
