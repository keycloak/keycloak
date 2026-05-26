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
  useJwksUrlName?: string;
  jwksUrlName?: string;
  publicKeySignatureVerifierName?: string;
  publicKeySignatureVerifierKeyIdName?: string;
  publicKeyLabel?: string;
  publicKeyHelp?: string;
  showPublicKeyId?: boolean;
  allowImport?: boolean;
};

export const JwksSettings = ({
  readOnly = false,
  useJwksUrlName = "config.useJwksUrl",
  jwksUrlName = "config.jwksUrl",
  publicKeySignatureVerifierName = "config.publicKeySignatureVerifier",
  publicKeySignatureVerifierKeyIdName = "config.publicKeySignatureVerifierKeyId",
  publicKeyLabel = "validatingPublicKey",
  publicKeyHelp = "validatingPublicKeyHelp",
  showPublicKeyId = true,
  allowImport = true,
}: JwksSettingsProps) => {
  const { t } = useTranslation();
  const { control, setValue } = useFormContext();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [openImportKeys, toggleOpenImportKeys, setOpenImportKeys] = useToggle();
  const useJwks = useWatch({
    control,
    name: useJwksUrlName,
    defaultValue: "true",
  });
  const publicKeySignatureVerifier = useWatch({
    control,
    name: publicKeySignatureVerifierName,
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
        setValue(publicKeySignatureVerifierName, info.jwks, {
          shouldDirty: true,
        });
        setValue(publicKeySignatureVerifierKeyIdName, "", {
          shouldDirty: true,
        });
        addAlert(t("importSuccess"), AlertVariant.success);
      } else if (info.publicKey) {
        setValue(publicKeySignatureVerifierName, info.publicKey, {
          shouldDirty: true,
        });
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
        name={useJwksUrlName}
        label={t("useJwksUrl")}
        labelIcon={t("useJwksUrlHelp")}
        isDisabled={readOnly}
        defaultValue={"true"}
        stringify
      />
      {useJwks === "true" ? (
        <TextControl
          name={jwksUrlName}
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
          {allowImport && openImportKeys && (
            <ImportKeyDialog
              toggleDialog={toggleOpenImportKeys}
              save={importKey}
              title="importKey"
              description="importKeysDescription"
            />
          )}
          {showPublicKeyId &&
            !publicKeySignatureVerifier?.trim().startsWith("{") && (
              <TextControl
                name={publicKeySignatureVerifierKeyIdName}
                label={t("validatingPublicKeyId")}
                labelIcon={t("validatingPublicKeyIdHelp")}
                readOnly={readOnly}
              />
            )}
          <TextAreaControl
            name={publicKeySignatureVerifierName}
            label={t(publicKeyLabel)}
            labelIcon={publicKeyHelp ? t(publicKeyHelp) : undefined}
            rules={{ required: t("required") }}
            readOnly={readOnly}
          />
          {allowImport && !readOnly && (
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
