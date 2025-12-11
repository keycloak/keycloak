import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { AlertVariant, FormGroup, Button } from "@patternfly/react-core";
import { useFormContext, useWatch } from "react-hook-form";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useTranslation } from "react-i18next";
import {
  TextAreaControl,
  TextControl,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  ImportFile,
  ImportKeyDialog,
} from "../../clients/keys/ImportKeyDialog";
import useToggle from "../../utils/useToggle";
import { useAdminClient } from "../../admin-client";

type JwksSettingsProps = {
  readOnly?: boolean;
};

export const JwksSettings = ({ readOnly = false }: JwksSettingsProps) => {
  const { t } = useTranslation();
  const { control, setValue } =
    useFormContext<IdentityProviderRepresentation>();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [openImportKeys, toggleOpenImportKeys, setOpenImportKeys] = useToggle();
  const useJwks = useWatch({
    control,
    name: "config.useJwksUrl",
    defaultValue: "true",
  });
  const publicKeySignatureVerifier = useWatch({
    control,
    name: "config.publicKeySignatureVerifier",
  });

  const importKey = async (importFile: ImportFile) => {
    try {
      const formData = new FormData();
      const { file, ...rest } = importFile;

      for (const [key, value] of Object.entries(rest)) {
        formData.append(key, value);
      }

      formData.append("file", file);
      const info = await adminClient.identityProviders.uploadCertificate(
        {},
        formData,
      );
      if (info.jwks) {
        setValue("config.publicKeySignatureVerifier", info.jwks);
        setValue("config.publicKeySignatureVerifierKeyId", "");
        addAlert(t("importSuccess"), AlertVariant.success);
      } else if (info.publicKey) {
        setValue("config.publicKeySignatureVerifier", info.publicKey);
        addAlert(t("importSuccess"), AlertVariant.success);
      } else {
        addError("importError", t("emptyResources"));
      }
    } catch (error) {
      addError("importError", error);
    }
  };

  return (
    <>
      <DefaultSwitchControl
        name="config.useJwksUrl"
        label={t("useJwksUrl")}
        labelIcon={t("useJwksUrlHelp")}
        isDisabled={readOnly}
        defaultValue={"true"}
        stringify
      />
      {useJwks === "true" ? (
        <TextControl
          name="config.jwksUrl"
          label={t("jwksUrl")}
          labelIcon={t("jwksUrlHelp")}
          type="url"
          readOnly={readOnly}
          rules={{
            required: t("required"),
          }}
        />
      ) : (
        <>
          {openImportKeys && (
            <ImportKeyDialog
              toggleDialog={toggleOpenImportKeys}
              save={importKey}
              title="importKey"
              description="importKeysDescription"
            />
          )}
          {!publicKeySignatureVerifier?.trim().startsWith("{") && (
            <TextControl
              name="config.publicKeySignatureVerifierKeyId"
              label={t("validatingPublicKeyId")}
              labelIcon={t("validatingPublicKeyIdHelp")}
              readOnly={readOnly}
            />
          )}
          <TextAreaControl
            name="config.publicKeySignatureVerifier"
            label={t("validatingPublicKey")}
            labelIcon={t("validatingPublicKeyHelp")}
            rules={{ required: t("required") }}
            readOnly={readOnly}
          />
          {!readOnly && (
            <FormGroup fieldId="kc-import-certificate-button">
              <Button
                variant="secondary"
                data-testid="import-certificate-button"
                onClick={() => setOpenImportKeys(true)}
              >
                {t("import")}
              </Button>
            </FormGroup>
          )}
        </>
      )}
    </>
  );
};
