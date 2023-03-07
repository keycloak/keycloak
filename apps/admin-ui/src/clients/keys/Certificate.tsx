import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import { FormGroup } from "@patternfly/react-core";
import { useId } from "react";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";

type CertificateProps = Omit<CertificateDisplayProps, "id"> & {
  plain?: boolean;
};

type CertificateDisplayProps = {
  id: string;
  keyInfo?: CertificateRepresentation;
};

const CertificateDisplay = ({ id, keyInfo }: CertificateDisplayProps) => {
  const { t } = useTranslation("clients");
  return (
    <KeycloakTextArea
      readOnly
      rows={5}
      id={id}
      data-testid="certificate"
      value={keyInfo?.certificate}
      aria-label={t("certificate")}
    />
  );
};

export const Certificate = ({ keyInfo, plain = false }: CertificateProps) => {
  const { t } = useTranslation("clients");
  const id = useId();

  return plain ? (
    <CertificateDisplay id={id} keyInfo={keyInfo} />
  ) : (
    <FormGroup
      label={t("certificate")}
      fieldId={id}
      labelIcon={
        <HelpItem
          helpText={t("clients-help:certificate")}
          fieldLabelId={`clients:${id}`}
        />
      }
    >
      <CertificateDisplay id={id} keyInfo={keyInfo} />
    </FormGroup>
  );
};
