import { IdentityProviderType } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  HelpItem,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  FormFieldGroup,
  FormFieldGroupHeader,
  FormGroup,
} from "@patternfly/react-core";
import { PlusCircleIcon, TrashIcon } from "@patternfly/react-icons";
import { useFieldArray, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { IdentityProviderSelect } from "../../components/identity-provider/IdentityProviderSelect";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { CredentialSetsSettings } from "./CredentialSetsSettings";
import {
  CredentialForm,
  emptyCredential,
  REQUESTED_CREDENTIALS,
  SD_JWT_VC_FORMAT,
} from "./requestedCredentials";

const CREDENTIAL_ID = /^[A-Za-z0-9_-]+$/;

type CredentialFieldsProps = {
  index: number;
  onRemove?: () => void;
};

const CredentialFields = ({ index, onRemove }: CredentialFieldsProps) => {
  const { t } = useTranslation();
  const name = `${REQUESTED_CREDENTIALS}.credentials.${index}`;
  const id: string | undefined = useWatch({ name: `${name}.id` });

  return (
    <FormFieldGroup
      header={
        <FormFieldGroupHeader
          titleText={{
            text: id?.trim() || t("oid4vpNewCredential"),
            id: `oid4vp-credential-${index}`,
          }}
          actions={
            <Button
              variant="plain"
              aria-label={t("oid4vpRemoveCredential")}
              isDisabled={!onRemove}
              onClick={onRemove}
              data-testid={`removeCredential${index}`}
            >
              <TrashIcon />
            </Button>
          }
        />
      }
    >
      <TextControl
        name={`${name}.id`}
        label={t("oid4vpCredentialId")}
        labelIcon={t("oid4vpCredentialIdHelp")}
        rules={{
          required: t("required"),
          pattern: {
            value: CREDENTIAL_ID,
            message: t("oid4vpCredentialIdInvalid"),
          },
        }}
      />
      <SelectControl
        name={`${name}.format`}
        label={t("oid4vpCredentialFormat")}
        labelIcon={t("oid4vpCredentialFormatHelp")}
        controller={{
          defaultValue: SD_JWT_VC_FORMAT,
          rules: { required: { value: true, message: t("required") } },
        }}
        options={[{ key: SD_JWT_VC_FORMAT, value: t("oid4vpSdJwtVcFormat") }]}
      />
      <FormGroup
        label={t("oid4vpCredentialTypes")}
        fieldId={`${name}.types`}
        isRequired
        labelIcon={
          <HelpItem
            helpText={t("oid4vpCredentialTypesHelp")}
            fieldLabelId="oid4vpCredentialTypes"
          />
        }
      >
        <MultiLineInput
          id={`${name}.types`}
          name={`${name}.types`}
          aria-label={t("oid4vpCredentialTypes")}
          addButtonLabel="oid4vpAddCredentialType"
          isRequired
        />
      </FormGroup>
      <IdentityProviderSelect
        name={`${name}.trustMaterialIdps`}
        label={t("oid4vpTrustMaterialIdps")}
        helpText={t("oid4vpTrustMaterialIdpsHelp")}
        identityProviderType={IdentityProviderType.TRUST_MATERIAL}
        realmOnly
        required
        convertToName={(name) => name}
      />
    </FormFieldGroup>
  );
};

export const RequestedCredentialsSettings = () => {
  const { t } = useTranslation();
  const { fields, append, remove } = useFieldArray({
    name: `${REQUESTED_CREDENTIALS}.credentials`,
    keyName: "key",
  });
  const credentials: CredentialForm[] | undefined = useWatch({
    name: `${REQUESTED_CREDENTIALS}.credentials`,
  });
  const credentialIds = [
    ...new Set(
      (credentials ?? []).map(({ id }) => id.trim()).filter((id) => id),
    ),
  ];

  return (
    <>
      <FormFieldGroup
        header={
          <FormFieldGroupHeader
            titleText={{
              text: t("oid4vpRequestedCredentials"),
              id: "oid4vp-requested-credentials",
            }}
            titleDescription={t("oid4vpRequestedCredentialsHelp")}
          />
        }
      >
        {fields.map((field, index) => (
          <CredentialFields
            key={field.key}
            index={index}
            onRemove={fields.length > 1 ? () => remove(index) : undefined}
          />
        ))}
        <Button
          variant="link"
          icon={<PlusCircleIcon />}
          onClick={() => append(emptyCredential())}
          data-testid="addCredential"
        >
          {t("oid4vpAddCredential")}
        </Button>
      </FormFieldGroup>
      <CredentialSetsSettings credentialIds={credentialIds} />
      <FormFieldGroup
        header={
          <FormFieldGroupHeader
            titleText={{
              text: t("oid4vpPrincipal"),
              id: "oid4vp-principal",
            }}
            titleDescription={t("oid4vpPrincipalHelp")}
          />
        }
      >
        <SelectControl
          name={`${REQUESTED_CREDENTIALS}.principal.credential`}
          label={t("oid4vpPrincipalCredential")}
          labelIcon={t("oid4vpPrincipalCredentialHelp")}
          controller={{
            defaultValue: "",
            rules: { required: { value: true, message: t("required") } },
          }}
          options={credentialIds}
        />
        <TextControl
          name={`${REQUESTED_CREDENTIALS}.principal.claim`}
          label={t("oid4vpPrincipalClaim")}
          labelIcon={t("oid4vpPrincipalClaimHelp")}
          rules={{ required: t("required") }}
        />
      </FormFieldGroup>
    </>
  );
};
