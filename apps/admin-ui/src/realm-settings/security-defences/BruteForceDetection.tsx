import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  NumberInput,
  Switch,
} from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { Time } from "./Time";
import { convertToFormValues } from "../../util";

type BruteForceDetectionProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const BruteForceDetection = ({
  realm,
  save,
}: BruteForceDetectionProps) => {
  const { t } = useTranslation("realm-settings");
  const form = useForm({ shouldUnregister: false });
  const {
    setValue,
    handleSubmit,
    control,
    formState: { isDirty },
  } = form;

  const enable = useWatch({
    control,
    name: "bruteForceProtected",
  });

  const permanentLockout = useWatch({
    control,
    name: "permanentLockout",
  });

  const setupForm = () => convertToFormValues(realm, setValue);
  useEffect(setupForm, []);

  return (
    <FormProvider {...form}>
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(save)}
      >
        <FormGroup
          label={t("common:enabled")}
          fieldId="bruteForceProtected"
          hasNoPaddingTop
        >
          <Controller
            name="bruteForceProtected"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id="bruteForceProtected"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value}
                onChange={onChange}
                aria-label={t("common:enabled")}
              />
            )}
          />
        </FormGroup>
        {enable && (
          <>
            <FormGroup
              label={t("failureFactor")}
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:failureFactor"
                  fieldLabelId="realm-settings:failureFactor"
                />
              }
              fieldId="failureFactor"
            >
              <Controller
                name="failureFactor"
                defaultValue={0}
                control={control}
                rules={{ required: true }}
                render={({ onChange, value }) => (
                  <NumberInput
                    type="text"
                    id="failureFactor"
                    value={value}
                    onPlus={() => onChange(value + 1)}
                    onMinus={() => onChange(value - 1)}
                    onChange={(event) =>
                      onChange(Number((event.target as HTMLInputElement).value))
                    }
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("permanentLockout")}
              fieldId="permanentLockout"
              hasNoPaddingTop
            >
              <Controller
                name="permanentLockout"
                defaultValue={false}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    id="permanentLockout"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
                    aria-label={t("permanentLockout")}
                  />
                )}
              />
            </FormGroup>

            {!permanentLockout && (
              <>
                <Time name="waitIncrementSeconds" />
                <Time name="maxFailureWaitSeconds" />
                <Time name="maxDeltaTimeSeconds" />
              </>
            )}

            <FormGroup
              label={t("quickLoginCheckMilliSeconds")}
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:quickLoginCheckMilliSeconds"
                  fieldLabelId="realm-settings:quickLoginCheckMilliSeconds"
                />
              }
              fieldId="quickLoginCheckMilliSeconds"
            >
              <Controller
                name="quickLoginCheckMilliSeconds"
                defaultValue={0}
                control={control}
                render={({ onChange, value }) => (
                  <NumberInput
                    type="text"
                    id="quickLoginCheckMilliSeconds"
                    value={value}
                    onPlus={() => onChange(value + 1)}
                    onMinus={() => onChange(value - 1)}
                    onChange={(event) =>
                      onChange(Number((event.target as HTMLInputElement).value))
                    }
                  />
                )}
              />
            </FormGroup>

            <Time name="minimumQuickLoginWaitSeconds" />
          </>
        )}

        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="brute-force-tab-save"
            isDisabled={!isDirty}
          >
            {t("common:save")}
          </Button>
          <Button variant="link" onClick={setupForm}>
            {t("common:revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </FormProvider>
  );
};
