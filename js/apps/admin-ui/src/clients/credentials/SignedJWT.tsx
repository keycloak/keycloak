import { useTranslation } from "react-i18next";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

type SignedJWTProps = {
  clientAuthenticatorType: string;
};

export const SignedJWT = ({ clientAuthenticatorType }: SignedJWTProps) => {
  const { cryptoInfo } = useServerInfo();
  const providers =
    clientAuthenticatorType === "client-jwt"
      ? cryptoInfo?.clientSignatureAsymmetricAlgorithms ?? []
      : cryptoInfo?.clientSignatureSymmetricAlgorithms ?? [];

  const { t } = useTranslation();

  return (
    <SelectControl
      name={convertAttributeNameToForm<FormFields>(
        "attributes.token.endpoint.auth.signing.alg",
      )}
      label={t("signatureAlgorithm")}
      labelIcon={t("signatureAlgorithmHelp")}
      controller={{
        defaultValue: "",
      }}
      isScrollable
      maxMenuHeight="200px"
      options={[
        { key: "", value: t("anyAlgorithm") },
        ...providers.map((option) => ({ key: option, value: option })),
      ]}
    />
  );
};
