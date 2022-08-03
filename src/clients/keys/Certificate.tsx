import { useTranslation } from "react-i18next";
import { FormGroup, GenerateId } from "@patternfly/react-core";

import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";

type CertificateProps = Omit<CertificateDisplayProps, "id"> & {
  plain?: boolean;
};

type CertificateDisplayProps = {
  id: string;
  keyInfo?: CertificateRepresentation;
};

const CertificateDisplay = ({ id, keyInfo }: CertificateDisplayProps) => (
  <KeycloakTextArea
    readOnly
    rows={5}
    id={id}
    data-testid="certificate"
    value={keyInfo?.certificate}
  />
);

export const Certificate = ({ keyInfo, plain = false }: CertificateProps) => {
  const { t } = useTranslation("clients");
  return (
    <GenerateId prefix="certificate">
      {(id) =>
        plain ? (
          <CertificateDisplay id={id} keyInfo={keyInfo} />
        ) : (
          <FormGroup
            label={t("certificate")}
            fieldId={id}
            labelIcon={
              <HelpItem
                helpText="clients-help:certificate"
                fieldLabelId={`clients:${id}`}
              />
            }
          >
            <CertificateDisplay id={id} keyInfo={keyInfo} />
          </FormGroup>
        )
      }
    </GenerateId>
  );
};
