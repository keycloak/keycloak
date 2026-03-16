import { useTranslation } from "react-i18next";
import { PasswordControl, TextControl } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";


export const ClientIdSecret = ({
  secretRequired = true,
  create = true,
  isTideIdp = false, // TIDECLOAK IMPLEMENTATION
}: {
  secretRequired?: boolean;
  create?: boolean;
  isTideIdp?: boolean; // TIDECLOAK IMPLEMENTATION

}) => {
  const { t } = useTranslation();

  return (
    <>
      <FormGroup
        style={{ display: isTideIdp ? 'none' : undefined }}>
        <TextControl
          isDisabled={isTideIdp} //TIDECLOAK IMPLEMENTATION
          name="config.clientId"
          label={t("clientId")}
          labelIcon={t("clientIdHelp")}
          rules={
            isTideIdp ? {} : { required: t("required") }
          }
      />
        <PasswordControl
          isTideIdp={isTideIdp} //TIDECLOAK IMPLEMENTATION
          isDisabled={isTideIdp} //TIDECLOAK IMPLEMENTATION
          name="config.clientSecret"
          label={t("clientSecret")}
          labelIcon={t("clientSecretHelp")}
          hasReveal={create}
          rules={{ required: { value: secretRequired, message: t("required") } }}
        />

      </FormGroup>
    </>
  );
};
