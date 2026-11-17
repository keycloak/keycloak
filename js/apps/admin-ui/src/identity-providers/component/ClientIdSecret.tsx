import { useEffect, useRef } from "react";
import { get, isEqual } from "lodash-es";
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

const SECRET_MASK = "**********";

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
  const previousDestinationValues = useRef(destinationValues);

  const { dirtyFields } = useFormState({
    control: form.control,
    name: [...CLIENT_SECRET_DESTINATION_FIELDS],
  });

  // When destination/auth fields change in edit mode, clear the client secret so the
  // masked placeholder is not submitted and the admin must re-enter credentials.
  // Only clear when the destination values themselves change; otherwise a re-render
  // (or typing the new secret) would wipe the value the admin just entered.
  useEffect(() => {
    if (create) return;

    const destinationDirty = CLIENT_SECRET_DESTINATION_FIELDS.some((field) =>
      Boolean(get(dirtyFields, field)),
    );
    const destinationChanged = !isEqual(
      previousDestinationValues.current,
      destinationValues,
    );
    previousDestinationValues.current = destinationValues;

    if (destinationDirty && destinationChanged) {
      const currentSecret = form.getValues("config.clientSecret");
      if (!currentSecret || currentSecret === SECRET_MASK) {
        form.setValue("config.clientSecret", "");
      }
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
