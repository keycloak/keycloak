import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { FormGroup } from "@patternfly/react-core";
import { HelpItem } from "@keycloak/keycloak-ui-shared";

type SignedJWTProps = {
  clientAuthenticatorType: string;
};

export const SignedJWT = ({ clientAuthenticatorType }: SignedJWTProps) => {
  const { cryptoInfo } = useServerInfo();
  const providers =
    clientAuthenticatorType === "client-jwt"
      ? (cryptoInfo?.clientSignatureAsymmetricAlgorithms ?? [])
      : (cryptoInfo?.clientSignatureSymmetricAlgorithms ?? []);

  const { t } = useTranslation();
  const { control } = useFormContext<FormFields>();

  return (
    <>
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
      <FormGroup
        label={t("signatureMaxExp")}
        fieldId="signatureMaxExp"
        className="pf-v5-u-my-md"
        labelIcon={
          <HelpItem
            helpText={t("signatureMaxExpHelp")}
            fieldLabelId="signatureMaxExp"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.token.endpoint.auth.signing.max.exp",
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <TimeSelector
              value={field.value!}
              onChange={field.onChange}
              units={["second", "minute"]}
              min="1"
            />
          )}
        />
      </FormGroup>
    </>
  );
};
