import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  SwitchControl,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";
import { useRealm } from "../../context/realm-context/RealmContext";

type TrustedDevicePolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
};

type FormFields = Omit<
    RealmRepresentation,
  "clients" | "components" | "groups" | "users" | "federatedUsers"
>;

export const TrustedDevicePolicy = ({ realm, realmUpdated }: TrustedDevicePolicyProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<FormFields>({
    mode: "onChange",
    defaultValues: {
      trustedDeviceExpiration: realm.trustedDeviceExpiration ?? 604800,
      trustedDeviceEnabled: realm.trustedDeviceEnabled ?? false
    }
    });
  const {
    reset,
    handleSubmit,
    formState: { isValid, isDirty },
  } = form;
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();

  const setupForm = (formValues: FormFields) => reset(formValues);

  const onSubmit = async (formValues: FormFields) => {
    try {
      await adminClient.realms.update({ realm: realmName }, formValues);
      const updatedRealm = await adminClient.realms.findOne({
        realm: realmName,
      });
      realmUpdated(updatedRealm!);
      setupForm(updatedRealm!);
      addAlert(t("updateTrustedDeviceSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateTrustedDeviceError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(onSubmit)}
        className="keycloak__trusted_device_policies_authentication__form"
      >
        <FormProvider {...form}>

            <TimeSelectorControl
              name="trustedDeviceExpiration"
              label={t("trustedDeviceExpiration")}
              labelIcon={t("trustedDeviceExpirationHelp")}
              units={["hour", "day"]}
              controller={{
                defaultValue: 604800, // 1 week
                rules: {
                  min: 86400, // 1 day
                  max: {
                    value: 7776000, // 90 days
                    message: t("maxLength", { length: "90 " + t("days") }),
                  },
                },
              }}
            />
            <SwitchControl
              name="trustedDeviceEnabled"
              label={t("trustedDeviceEnabled")}
              labelIcon={t("trustedDeviceEnabledHelp")}
              labelOn={t("on")}
              labelOff={t("off")}
            />

          <ActionGroup>
            <Button
              data-testid="save"
              variant="primary"
              type="submit"
              isDisabled={!isValid || !isDirty}
            >
              {t("save")}
            </Button>
            <Button
              data-testid="reload"
              variant={ButtonVariant.link}
              onClick={() => reset({ ...realm })}
            >
              {t("reload")}
            </Button>
          </ActionGroup>
        </FormProvider>
      </FormAccess>
    </PageSection>
  );
};
