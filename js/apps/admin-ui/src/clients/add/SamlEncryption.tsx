import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const SamlEncryption = () => {
  const { t } = useTranslation();
  const { watch, setValue } = useFormContext();
  const ALGORITHM_RSA_OAEP = "http://www.w3.org/2009/xmlenc11#rsa-oaep";
  const ALGORITHM_RSA_OAEP_MGF1P =
    "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
  const keyEncryptionAlgorithm = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.saml.encryption.keyAlgorithm",
    ),
    "",
  );

  // remove optional fields if not displayed
  useEffect(() => {
    if (keyEncryptionAlgorithm !== ALGORITHM_RSA_OAEP) {
      setValue(
        convertAttributeNameToForm<FormFields>(
          "attributes.saml.encryption.maskGenerationFunction",
        ),
        "",
      );
      if (keyEncryptionAlgorithm !== ALGORITHM_RSA_OAEP_MGF1P) {
        setValue(
          convertAttributeNameToForm<FormFields>(
            "attributes.saml.encryption.digestMethod",
          ),
          "",
        );
      }
    }
  }, [keyEncryptionAlgorithm]);

  return (
    <>
      <SelectControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.saml.encryption.algorithm",
        )}
        label={t("samlClientEncryptionAlgorithm")}
        labelIcon={t("samlClientEncryptionAlgorithmHelp")}
        controller={{
          defaultValue: "",
        }}
        options={[
          { key: "", value: t("choose") },
          {
            key: "http://www.w3.org/2009/xmlenc11#aes256-gcm",
            value: "AES_256_GCM",
          },
          {
            key: "http://www.w3.org/2009/xmlenc11#aes192-gcm",
            value: "AES_192_GCM",
          },
          {
            key: "http://www.w3.org/2009/xmlenc11#aes128-gcm",
            value: "AES_128_GCM",
          },
          {
            key: "http://www.w3.org/2001/04/xmlenc#aes256-cbc",
            value: "AES_256_CBC",
          },
          {
            key: "http://www.w3.org/2001/04/xmlenc#aes192-cbc",
            value: "AES_192_CBC",
          },
          {
            key: "http://www.w3.org/2001/04/xmlenc#aes128-cbc",
            value: "AES_128_CBC",
          },
        ]}
      />

      <SelectControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.saml.encryption.keyAlgorithm",
        )}
        label={t("samlClientKeyEncryptionAlgorithm")}
        labelIcon={t("samlClientKeyEncryptionAlgorithmHelp")}
        controller={{
          defaultValue: "",
        }}
        options={[
          { key: "", value: t("choose") },
          {
            key: ALGORITHM_RSA_OAEP,
            value: "RSA-OAEP-11",
          },
          {
            key: ALGORITHM_RSA_OAEP_MGF1P,
            value: "RSA-OAEP-MGF1P",
          },
          { key: "http://www.w3.org/2001/04/xmlenc#rsa-1_5", value: "RSA1_5" },
        ]}
      />

      {(keyEncryptionAlgorithm === ALGORITHM_RSA_OAEP ||
        keyEncryptionAlgorithm === ALGORITHM_RSA_OAEP_MGF1P) && (
        <SelectControl
          name={convertAttributeNameToForm<FormFields>(
            "attributes.saml.encryption.digestMethod",
          )}
          label={t("samlClientEncryptionDigestMethod")}
          labelIcon={t("samlClientEncryptionDigestMethodHelp")}
          controller={{
            defaultValue: "",
          }}
          options={[
            { key: "", value: t("choose") },
            {
              key: "http://www.w3.org/2001/04/xmlenc#sha512",
              value: "SHA-512",
            },
            {
              key: "http://www.w3.org/2001/04/xmlenc#sha256",
              value: "SHA-256",
            },
            { key: "http://www.w3.org/2000/09/xmldsig#sha1", value: "SHA-1" },
          ]}
        />
      )}

      {keyEncryptionAlgorithm === ALGORITHM_RSA_OAEP && (
        <SelectControl
          name={convertAttributeNameToForm<FormFields>(
            "attributes.saml.encryption.maskGenerationFunction",
          )}
          label={t("samlClientEncryptionMaskGenerationFunction")}
          labelIcon={t("samlClientEncryptionMaskGenerationFunctionHelp")}
          controller={{
            defaultValue: "",
          }}
          options={[
            { key: "", value: t("choose") },
            {
              key: "http://www.w3.org/2009/xmlenc11#mgf1sha512",
              value: "mgf1sha512",
            },
            {
              key: "http://www.w3.org/2009/xmlenc11#mgf1sha384",
              value: "mgf1sha384",
            },
            {
              key: "http://www.w3.org/2009/xmlenc11#mgf1sha256",
              value: "mgf1sha256",
            },
            {
              key: "http://www.w3.org/2009/xmlenc11#mgf1sha224",
              value: "mgf1sha224",
            },
            {
              key: "http://www.w3.org/2009/xmlenc11#mgf1sha1",
              value: "mgf1sha1",
            },
          ]}
        />
      )}
    </>
  );
};
