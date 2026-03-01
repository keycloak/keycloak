import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  NumberControl,
  SelectVariant,
  SelectControl,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  FormGroup,
  SelectOption,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
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
    formState: { isDirty },
  } = form;

  const [isBruteForceModeOpen, setIsBruteForceModeOpen] = useState(false);
  const [isBruteForceModeUpdated, setIsBruteForceModeUpdated] = useState(false);

  enum BruteForceMode {
    Disabled = "Disabled",
    PermanentLockout = "PermanentLockout",
    TemporaryLockout = "TemporaryLockout",
    PermanentAfterTemporaryLockout = "PermanentAfterTemporaryLockout",
  }

  const bruteForceModes = [
    BruteForceMode.Disabled,
    BruteForceMode.PermanentLockout,
    BruteForceMode.TemporaryLockout,
    BruteForceMode.PermanentAfterTemporaryLockout,
  ];

  const bruteForceStrategyTypes = ["MULTIPLE", "LINEAR"];

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    setIsBruteForceModeUpdated(false);
  };
  useEffect(setupForm, [realm]);

  const bruteForceMode = (() => {
    if (!form.getValues("bruteForceProtected")) {
      return BruteForceMode.Disabled;
    }
    if (!form.getValues("permanentLockout")) {
      return BruteForceMode.TemporaryLockout;
    }
    return form.getValues("maxTemporaryLockouts") == 0
      ? BruteForceMode.PermanentLockout
      : BruteForceMode.PermanentAfterTemporaryLockout;
  })();

  return (
    <FormProvider {...form}>
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(save)}
      >
        <FormGroup
          label={t("bruteForceMode")}
          fieldId="kc-brute-force-mode"
          labelIcon={
            <HelpItem
              helpText={t("bruteForceModeHelpText")}
              fieldLabelId="bruteForceMode"
            />
          }
        >
          <KeycloakSelect
            toggleId="kc-brute-force-mode"
            onToggle={() => setIsBruteForceModeOpen(!isBruteForceModeOpen)}
            onSelect={(value) => {
              switch (value as BruteForceMode) {
                case BruteForceMode.Disabled:
                  form.setValue("bruteForceProtected", false);
                  form.setValue("permanentLockout", false);
                  form.setValue("maxTemporaryLockouts", 0);
                  break;
                case BruteForceMode.TemporaryLockout:
                  form.setValue("bruteForceProtected", true);
                  form.setValue("permanentLockout", false);
                  form.setValue("maxTemporaryLockouts", 0);
                  break;
                case BruteForceMode.PermanentLockout:
                  form.setValue("bruteForceProtected", true);
                  form.setValue("permanentLockout", true);
                  form.setValue("maxTemporaryLockouts", 0);
                  break;
                case BruteForceMode.PermanentAfterTemporaryLockout:
                  form.setValue("bruteForceProtected", true);
                  form.setValue("permanentLockout", true);
                  form.setValue("maxTemporaryLockouts", 1);
                  break;
              }
              setIsBruteForceModeUpdated(true);
              setIsBruteForceModeOpen(false);
            }}
            selections={bruteForceMode}
            variant={SelectVariant.single}
            isOpen={isBruteForceModeOpen}
            data-testid="select-brute-force-mode"
            aria-label={t("selectUnmanagedAttributePolicy")}
          >
            {bruteForceModes.map((mode) => (
              <SelectOption key={mode} value={mode}>
                {t(`bruteForceMode.${mode}`)}
              </SelectOption>
            ))}
          </KeycloakSelect>
        </FormGroup>
        {bruteForceMode !== BruteForceMode.Disabled && (
          <>
            <NumberControl
              name="failureFactor"
              label={t("failureFactor")}
              labelIcon={t("failureFactorHelp")}
              controller={{
                defaultValue: 0,
                rules: { required: t("required"), min: 0 },
              }}
            />
            {bruteForceMode ===
              BruteForceMode.PermanentAfterTemporaryLockout && (
              <NumberControl
                name="maxTemporaryLockouts"
                label={t("maxTemporaryLockouts")}
                labelIcon={t("maxTemporaryLockoutsHelp")}
                controller={{
                  defaultValue: 0,
                  rules: { min: 0 },
                }}
              />
            )}
            {(bruteForceMode === BruteForceMode.TemporaryLockout ||
              bruteForceMode ===
                BruteForceMode.PermanentAfterTemporaryLockout) && (
              <>
                <SelectControl
                  name="bruteForceStrategy"
                  label={t("bruteForceStrategy")}
                  labelIcon={t("bruteForceStrategyHelp", {
                    failureFactor: form.getValues("failureFactor"),
                  })}
                  controller={{ defaultValue: "" }}
                  options={bruteForceStrategyTypes.map((key) => ({
                    key,
                    value: t(`bruteForceStrategy.${key}`),
                  }))}
                />
                <Time name="waitIncrementSeconds" min={0} />
                <Time name="maxFailureWaitSeconds" min={0} />
                <Time name="maxDeltaTimeSeconds" min={0} />
              </>
            )}
            <NumberControl
              name="quickLoginCheckMilliSeconds"
              label={t("quickLoginCheckMilliSeconds")}
              labelIcon={t("quickLoginCheckMilliSecondsHelp")}
              controller={{
                defaultValue: 0,
                rules: { min: 0 },
              }}
            />
            <Time name="minimumQuickLoginWaitSeconds" min={0} />
          </>
        )}

        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="brute-force-tab-save"
            isDisabled={!isDirty && !isBruteForceModeUpdated}
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
