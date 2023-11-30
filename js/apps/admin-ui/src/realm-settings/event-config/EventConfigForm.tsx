import {
  ActionGroup,
  Button,
  Divider,
  FormGroup,
  Switch,
} from "@patternfly/react-core";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { HelpItem } from "ui-shared";
import { TimeSelector } from "../../components/time-selector/TimeSelector";

export type EventsType = "admin" | "user";

type EventConfigFormProps = {
  type: EventsType;
  form: UseFormReturn;
  reset: () => void;
  clear: () => void;
};

export const EventConfigForm = ({
  type,
  form,
  reset,
  clear,
}: EventConfigFormProps) => {
  const { t } = useTranslation();
  const {
    control,
    watch,
    setValue,
    formState: { isDirty },
  } = form;
  const eventKey = type === "admin" ? "adminEventsEnabled" : "eventsEnabled";
  const eventsEnabled: boolean = watch(eventKey);

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "events-disable-title",
    messageKey: "events-disable-confirm",
    continueButtonLabel: "confirm",
    onConfirm: () => setValue(eventKey, false, { shouldDirty: true }),
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
            helpText={t(`save-${type}-eventsHelp`)}
            fieldLabelId="saveEvents"
          />
        }
      >
        <Controller
          name={eventKey}
          defaultValue={false}
          control={control}
          render={({ field }) => (
            <Switch
              data-testid={eventKey}
              id={`${eventKey}-switch`}
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value}
              onChange={(value) => {
                if (!value) {
                  toggleDisableDialog();
                } else {
                  field.onChange(value);
                }
              }}
              aria-label={t("saveEvents")}
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
                  helpText={t("includeRepresentationHelp")}
                  fieldLabelId="includeRepresentation"
                />
              }
            >
              <Controller
                name="adminEventsDetailsEnabled"
                defaultValue={false}
                control={control}
                render={({ field }) => (
                  <Switch
                    data-testid="includeRepresentation"
                    id="includeRepresentation"
                    label={t("on")}
                    labelOff={t("off")}
                    isChecked={field.value}
                    onChange={field.onChange}
                    aria-label={t("includeRepresentation")}
                  />
                )}
              />
            </FormGroup>
          )}
          <FormGroup
            label={t("expiration")}
            fieldId="expiration"
            labelIcon={
              <HelpItem
                helpText={t("expirationHelp")}
                fieldLabelId="expiration"
              />
            }
          >
            <Controller
              name={
                type === "user" ? "eventsExpiration" : "adminEventsExpiration"
              }
              defaultValue=""
              control={control}
              render={({ field }) => (
                <TimeSelector
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
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
          {t("save")}
        </Button>
        <Button variant="link" onClick={reset}>
          {t("revert")}
        </Button>
      </ActionGroup>
      <Divider />
      <FormGroup
        label={type === "user" ? t("clearUserEvents") : t("clearAdminEvents")}
        fieldId={`clear-${type}-events`}
        labelIcon={
          <HelpItem
            helpText={t(`${type}-clearEventsHelp`)}
            fieldLabelId={`clear-${type}-events`}
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
