import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  NumberInput,
  Switch,
} from "@patternfly/react-core";
import { useEffect } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { convertToFormValues } from "../../util";
import { Time } from "./Time";

type BruteForceDetectionProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const BruteForceDetection = ({
  realm,
  save,
}: BruteForceDetectionProps) => {
  const { t } = useTranslation();
  const form = useForm();
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
          label={t("enabled")}
          fieldId="bruteForceProtected"
          hasNoPaddingTop
        >
          <Controller
            name="bruteForceProtected"
            defaultValue={false}
            control={control}
            render={({ field }) => (
              <Switch
                id="bruteForceProtected"
                label={t("on")}
                labelOff={t("off")}
                isChecked={field.value}
                onChange={field.onChange}
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
                  helpText={t("failureFactorHelp")}
                  fieldLabelId="failureFactor"
                />
              }
              fieldId="failureFactor"
            >
              <Controller
                name="failureFactor"
                defaultValue={0}
                control={control}
                rules={{ required: true }}
                render={({ field }) => (
                  <NumberInput
                    type="text"
                    id="failureFactor"
                    value={field.value}
                    onPlus={() => field.onChange(field.value + 1)}
                    onMinus={() => field.onChange(field.value - 1)}
                    onChange={(event) =>
                      field.onChange(
                        Number((event.target as HTMLInputElement).value),
                      )
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
                render={({ field }) => (
                  <Switch
                    id="permanentLockout"
                    label={t("on")}
                    labelOff={t("off")}
                    isChecked={field.value}
                    onChange={field.onChange}
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
                  helpText={t("quickLoginCheckMilliSecondsHelp")}
                  fieldLabelId="quickLoginCheckMilliSeconds"
                />
              }
              fieldId="quickLoginCheckMilliSeconds"
            >
              <Controller
                name="quickLoginCheckMilliSeconds"
                defaultValue={0}
                control={control}
                render={({ field }) => (
                  <NumberInput
                    type="text"
                    id="quickLoginCheckMilliSeconds"
                    value={field.value}
                    onPlus={() => field.onChange(field.value + 1)}
                    onMinus={() => field.onChange(field.value - 1)}
                    onChange={(event) =>
                      field.onChange(
                        Number((event.target as HTMLInputElement).value),
                      )
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
            {t("save")}
          </Button>
          <Button variant="link" onClick={setupForm}>
            {t("revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </FormProvider>
  );
};
