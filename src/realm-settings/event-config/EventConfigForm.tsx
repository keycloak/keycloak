import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";
import {
  ActionGroup,
  Button,
  Divider,
  FormGroup,
  Switch,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";

export type EventsType = "admin" | "user";

type EventConfigFormProps = {
  type: EventsType;
  form: UseFormMethods;
  reset: () => void;
  clear: () => void;
};

export const EventConfigForm = ({
  type,
  form,
  reset,
  clear,
}: EventConfigFormProps) => {
  const { t } = useTranslation("realm-settings");
  const {
    control,
    watch,
    setValue,
    formState: { isDirty },
  } = form;

  const eventKey = type === "admin" ? "adminEventsEnabled" : "eventsEnabled";
  const eventsEnabled: boolean = watch(eventKey);

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "realm-settings:events-disable-title",
    messageKey: "realm-settings:events-disable-confirm",
    continueButtonLabel: "realm-settings:confirm",
    onConfirm: () => setValue(eventKey, false),
  });

  return (
    <>
      <DisableConfirm />
      <FormGroup
        hasNoPaddingTop
        label={t("saveEvents")}
        fieldId={eventKey}
        labelIcon={
          <HelpItem
            helpText={`realm-settings-help:save-${type}-events`}
            fieldLabelId="realm-settings:saveEvents"
          />
        }
      >
        <Controller
          name={eventKey}
          defaultValue={false}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              data-testid={eventKey}
              id={`${eventKey}-switch`}
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value}
              onChange={(value) => {
                if (!value) {
                  toggleDisableDialog();
                } else {
                  onChange(value);
                }
              }}
            />
          )}
        />
      </FormGroup>
      {eventsEnabled && (
        <>
          {type === "admin" && (
            <FormGroup
              hasNoPaddingTop
              label={t("includeRepresentation")}
              fieldId="includeRepresentation"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:includeRepresentation"
                  fieldLabelId="realm-settings:includeRepresentation"
                />
              }
            >
              <Controller
                name="adminEventsDetailsEnabled"
                defaultValue={false}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    data-testid="includeRepresentation"
                    id="includeRepresentation"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormGroup>
          )}
          {type === "user" && (
            <FormGroup
              label={t("expiration")}
              fieldId="expiration"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:expiration"
                  fieldLabelId="realm-settings:expiration"
                />
              }
            >
              <Controller
                name="eventsExpiration"
                defaultValue=""
                control={control}
                render={({ onChange, value }) => (
                  <TimeSelector
                    value={value}
                    onChange={onChange}
                    units={["minutes", "hours", "days"]}
                  />
                )}
              />
            </FormGroup>
          )}
        </>
      )}
      <ActionGroup>
        <Button
          variant="primary"
          type="submit"
          id={`save-${type}`}
          data-testid={`save-${type}`}
          isDisabled={!isDirty}
        >
          {t("common:save")}
        </Button>
        <Button variant="link" onClick={reset}>
          {t("common:revert")}
        </Button>
      </ActionGroup>
      <Divider />
      <FormGroup
        label={type === "user" ? t("clearUserEvents") : t("clearAdminEvents")}
        fieldId={`clear-${type}-events`}
        labelIcon={
          <HelpItem
            helpText={`realm-settings-help:${type}-clearEvents`}
            fieldLabelId={`realm-settings:clear-${type}-events`}
          />
        }
      >
        <Button
          variant="danger"
          id={`clear-${type}-events`}
          data-testid={`clear-${type}-events`}
          onClick={() => clear()}
        >
          {type === "user" ? t("clearUserEvents") : t("clearAdminEvents")}
        </Button>
      </FormGroup>
    </>
  );
};
