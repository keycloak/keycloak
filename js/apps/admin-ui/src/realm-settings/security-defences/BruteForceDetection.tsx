import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  NumberInput,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
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

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    setIsBruteForceModeUpdated(false);
  };
  useEffect(setupForm, []);

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
          <Select
            toggleId="kc-brute-force-mode"
            onToggle={() => setIsBruteForceModeOpen(!isBruteForceModeOpen)}
            onSelect={(_, value) => {
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
          </Select>
        </FormGroup>
        {bruteForceMode !== BruteForceMode.Disabled && (
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

            {bruteForceMode ===
              BruteForceMode.PermanentAfterTemporaryLockout && (
              <FormGroup
                label={t("maxTemporaryLockouts")}
                labelIcon={
                  <HelpItem
                    helpText={t("maxTemporaryLockoutsHelp")}
                    fieldLabelId="maxTemporaryLockouts"
                  />
                }
                fieldId="maxTemporaryLockouts"
                hasNoPaddingTop
              >
                <Controller
                  name="maxTemporaryLockouts"
                  defaultValue={0}
                  control={control}
                  render={({ field }) => (
                    <NumberInput
                      type="text"
                      id="maxTemporaryLockouts"
                      value={field.value}
                      onPlus={() => field.onChange(field.value + 1)}
                      onMinus={() => field.onChange(field.value - 1)}
                      onChange={(event) =>
                        field.onChange(
                          Number((event.target as HTMLInputElement).value),
                        )
                      }
                      aria-label={t("maxTemporaryLockouts")}
                    />
                  )}
                />
              </FormGroup>
            )}

            {(bruteForceMode === BruteForceMode.TemporaryLockout ||
              bruteForceMode ===
                BruteForceMode.PermanentAfterTemporaryLockout) && (
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
