import {
  ActionGroup,
  Button,
  Divider,
  FormGroup,
  Switch,
} from "@patternfly/react-core";
import { Controller, FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";

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
    <FormProvider {...form}>
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
              onChange={(_event, value) => {
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
      {type === "admin" && (
        <DefaultSwitchControl
          name="adminEventsDetailsEnabled"
          label={t("includeRepresentation")}
          labelIcon={t("includeRepresentationHelp")}
        />
      )}
      {eventsEnabled && (
        <TimeSelectorControl
          name={type === "user" ? "eventsExpiration" : "adminEventsExpiration"}
          label={t("expiration")}
          labelIcon={t("expirationHelp")}
          defaultValue=""
          units={["minute", "hour", "day"]}
          controller={{
            defaultValue: "",
          }}
        />
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
    </FormProvider>
  );
};
