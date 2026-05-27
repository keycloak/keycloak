import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SelectControl, TextControl, useFetch } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import CibaPolicyRepresentation from "libs/keycloak-admin-client/lib/defs/cibaPolicyRepresentation"

const CIBA_BACKHANNEL_TOKEN_DELIVERY_MODES = ["poll", "ping"] as const;
const CIBA_EXPIRES_IN_MIN = 10;
const CIBA_EXPIRES_IN_MAX = 600;
const CIBA_INTERVAL_MIN = 0;
const CIBA_INTERVAL_MAX = 600;

type CibaPolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
};

export const CibaPolicy = ({ realm, realmUpdated }: CibaPolicyProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<CibaPolicyRepresentation>({ mode: "onChange" });
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { reset } = form;
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const setupForm = (policy: CibaPolicyRepresentation) => reset(policy);

  useFetch(
    () => adminClient.authenticationManagement.getCibaPolicy({ realm: realmName }),
    (policy) => {
      setupForm(policy);
    },
    [key],
  );

  const onSubmit = async (formValues: CibaPolicyRepresentation) => {
    try {
      await adminClient.authenticationManagement.updateCibaPolicy({ realm: realmName }, formValues);
      const updatedRealm = await adminClient.realms.findOne({realm: realmName});
      realmUpdated(updatedRealm!);
      refresh();
      addAlert(t("updateCibaSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateCibaError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={form.handleSubmit(onSubmit)}
      >
        <FormProvider {...form}>
          <SelectControl
            name="backchannelTokenDeliveryMode"
            label={t("cibaBackchannelTokenDeliveryMode")}
            labelIcon={t("cibaBackchannelTokenDeliveryModeHelp")}
            options={CIBA_BACKHANNEL_TOKEN_DELIVERY_MODES.map((mode) => ({
              key: mode,
              value: t(`cibaBackhannelTokenDeliveryModes.${mode}`),
            }))}
            controller={{ defaultValue: "" }}
          />
          <TextControl
            name="expiresIn"
            type="number"
            min={CIBA_EXPIRES_IN_MIN}
            max={CIBA_EXPIRES_IN_MAX}
            label={t("cibaExpiresIn")}
            labelIcon={t("cibaExpiresInHelp")}
            rules={{
              min: {
                value: CIBA_EXPIRES_IN_MIN,
                message: t("greaterThan", {
                  value: CIBA_EXPIRES_IN_MIN,
                }),
              },
              max: {
                value: CIBA_EXPIRES_IN_MAX,
                message: t("lessThan", { value: CIBA_EXPIRES_IN_MAX }),
              },
              required: t("required"),
            }}
          />
          <TextControl
            name="interval"
            type="number"
            min={CIBA_INTERVAL_MIN}
            max={CIBA_INTERVAL_MAX}
            label={t("cibaInterval")}
            labelIcon={t("cibaIntervalHelp")}
            rules={{
              min: {
                value: CIBA_INTERVAL_MIN,
                message: t("greaterThan", {
                  value: CIBA_INTERVAL_MIN,
                }),
              },
              max: {
                value: CIBA_INTERVAL_MAX,
                message: t("lessThan", { value: CIBA_INTERVAL_MAX }),
              },
              required: t("required"),
            }}
          />
          <SelectControl
            name="authRequestedUserHint"
            label={t("cibaAuthRequestedUserHint")}
            labelIcon={t("cibaAuthRequestedUserHintHelp")}
            options={["login_hint", "id_token_hint", "login_hint_token"]}
            controller={{ defaultValue: "" }}
            isDisabled
          />
        </FormProvider>
        <ActionGroup>
          <Button
            data-testid="save"
            variant="primary"
            type="submit"
            isDisabled={!form.formState.isValid || !form.formState.isDirty}
          >
            {t("save")}
          </Button>
          <Button
            data-testid="reload"
            variant={ButtonVariant.link}
            onClick={() => refresh()}
          >
            {t("reload")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
