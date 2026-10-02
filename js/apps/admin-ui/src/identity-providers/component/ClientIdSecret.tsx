import { useEffect } from "react";
import { get } from "lodash-es";
import { useFormContext, useFormState, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { PasswordControl, TextControl } from "@keycloak/keycloak-ui-shared";

/** Config fields that determine where/how the client secret is sent. */
const CLIENT_SECRET_DESTINATION_FIELDS = [
  "config.tokenUrl",
  "config.tokenIntrospectionUrl",
  "config.clientId",
  "config.baseUrl",
  "config.tenantId",
  "config.sandbox",
  "config.clientAuthMethod",
] as const;

export const ClientIdSecret = ({
  secretRequired = true,
  create = true,
}: {
  secretRequired?: boolean;
  create?: boolean;
}) => {
  const { t } = useTranslation();
  const form = useFormContext();

  const destinationValues = useWatch({
    control: form.control,
    name: [...CLIENT_SECRET_DESTINATION_FIELDS],
  });

  const { dirtyFields } = useFormState({
    control: form.control,
    name: [...CLIENT_SECRET_DESTINATION_FIELDS],
  });

  // When destination/auth fields change in edit mode, clear the client secret so the
  // masked placeholder is not submitted and the admin must re-enter credentials.
  useEffect(() => {
    if (create) return;

    const destinationDirty = CLIENT_SECRET_DESTINATION_FIELDS.some((field) =>
      Boolean(get(dirtyFields, field)),
    );

    if (destinationDirty) {
      form.setValue("config.clientSecret", "");
    }
  }, [destinationValues, dirtyFields, create, form]);

  return (
    <>
      <TextControl
        name="config.clientId"
        label={t("clientId")}
        labelIcon={t("clientIdHelp")}
        rules={{
          required: t("required"),
        }}
      />
      <PasswordControl
        name="config.clientSecret"
        label={t("clientSecret")}
        labelIcon={t("clientSecretHelp")}
        hasReveal={create}
        rules={{ required: { value: secretRequired, message: t("required") } }}
      />
    </>
  );
};
