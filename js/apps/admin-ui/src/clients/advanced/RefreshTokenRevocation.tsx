import { SelectControl, TextControl } from "@keycloak/keycloak-ui-shared";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

const REVOKE_REFRESH_TOKEN = convertAttributeNameToForm<FormFields>(
  "attributes.revoke.refresh.token",
);
const REFRESH_TOKEN_MAX_REUSE = convertAttributeNameToForm<FormFields>(
  "attributes.refresh.token.max.reuse",
);

/**
 * Client-level override of the realm settings "Revoke Refresh Token" and "Refresh Token Max Reuse".
 * An empty value means the realm setting is inherited.
 */
export const RefreshTokenRevocation = () => {
  const { t } = useTranslation();
  const { realmRepresentation: realm } = useRealm();
  const { watch } = useFormContext();

  const revokeRefreshToken = String(watch(REVOKE_REFRESH_TOKEN, "") ?? "")
    .trim()
    .toLowerCase();
  const realmRevokeRefreshToken = realm.revokeRefreshToken ?? false;
  // Only explicit "true"/"false" are overrides, anything else inherits the realm setting (same as the server side)
  const effectiveRevokeRefreshToken =
    revokeRefreshToken === "true" ||
    (revokeRefreshToken !== "false" && realmRevokeRefreshToken);

  return (
    <>
      <SelectControl
        id="revokeRefreshToken"
        name={REVOKE_REFRESH_TOKEN}
        label={t("revokeRefreshToken")}
        labelIcon={t("revokeRefreshTokenClientHelp")}
        controller={{
          defaultValue: "",
        }}
        options={[
          {
            key: "",
            value: t("inheritsFromRealmSettings", {
              value: t(realmRevokeRefreshToken ? "enabled" : "disabled"),
            }),
          },
          { key: "true", value: t("enabled") },
          { key: "false", value: t("disabled") },
        ]}
      />
      {effectiveRevokeRefreshToken && (
        <TextControl
          type="number"
          min={0}
          name={REFRESH_TOKEN_MAX_REUSE}
          label={t("refreshTokenMaxReuse")}
          labelIcon={t("refreshTokenMaxReuseClientHelp")}
          placeholder={t("inheritsFromRealmSettings", {
            value: realm.refreshTokenMaxReuse ?? 0,
          })}
          rules={{
            validate: (value?: string | number) =>
              value === undefined ||
              value === "" ||
              /^\d+$/.test(value.toString()) ||
              t("refreshTokenMaxReuseClientInvalid"),
          }}
        />
      )}
    </>
  );
};
