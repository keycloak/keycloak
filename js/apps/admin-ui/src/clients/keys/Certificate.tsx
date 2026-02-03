import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import { FormGroup, TextArea } from "@patternfly/react-core";
import { useId } from "react";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";

type CertificateProps = Omit<CertificateDisplayProps, "id"> & {
  plain?: boolean;
};

type CertificateDisplayProps = {
  id: string;
  helpTextKey?: string;
  type?: "jwks" | "certificate" | "publicKey";
  keyInfo?: CertificateRepresentation;
};

const CertificateDisplay = ({
  id,
  type = "certificate",
  keyInfo,
}: CertificateDisplayProps) => {
  const { t } = useTranslation();
  return (
    <TextArea
      readOnly
      rows={5}
      id={id}
      data-testid={type}
      value={keyInfo?.[type]}
      aria-label={t(type)}
    />
  );
};

export const Certificate = ({
  helpTextKey,
  type = "certificate",
  keyInfo,
  plain = false,
}: CertificateProps) => {
  const { t } = useTranslation();
  const id = useId();

  return plain ? (
    <CertificateDisplay id={id} type={type} keyInfo={keyInfo} />
  ) : (
    <FormGroup
      label={t(type)}
      fieldId={id}
      labelIcon={
        helpTextKey ? (
          <HelpItem helpText={t(helpTextKey)} fieldLabelId={id} />
        ) : undefined
      }
    >
      <CertificateDisplay id={id} type={type} keyInfo={keyInfo} />
    </FormGroup>
  );
};

export const KeyInfoArea = ({ type, keyInfo, ...rest }: CertificateProps) => {
  const myType = type
    ? type
    : keyInfo?.jwks
      ? "jwks"
      : keyInfo?.certificate
        ? "certificate"
        : "publicKey";
  return <Certificate type={myType} keyInfo={keyInfo} {...rest} />;
};
