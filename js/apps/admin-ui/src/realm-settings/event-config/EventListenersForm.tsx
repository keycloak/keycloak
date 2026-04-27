import { ActionGroup, Button } from "@patternfly/react-core";
import { FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  KeycloakSpinner,
  useFetch,
  SelectControl,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useAdminClient } from "../../admin-client";

type EventListenerRepresentation = {
  id: string;
};

type EventListenersFormProps = {
  form: UseFormReturn;
  reset: () => void;
};

export const EventListenersForm = ({
  form,
  reset,
}: EventListenersFormProps) => {
  const { t } = useTranslation();

  const [eventListeners, setEventListeners] =
    useState<EventListenerRepresentation[]>();

  const { adminClient } = useAdminClient();

  useFetch(
    () =>
      fetchAdminUI<EventListenerRepresentation[]>(
        adminClient,
        "ui-ext/available-event-listeners",
      ),
    setEventListeners,
    [],
  );

  if (!eventListeners) {
    return <KeycloakSpinner />;
  }

  return (
    <FormProvider {...form}>
      <SelectControl
        name="eventsListeners"
        label={t("eventListeners")}
        labelIcon={t("eventListenersHelpTextHelp")}
        controller={{
          defaultValue: "",
        }}
        className="kc_eventListeners_select"
        chipGroupProps={{
          numChips: 3,
          expandedText: t("hide"),
          collapsedText: t("showRemaining"),
        }}
        variant={SelectVariant.typeaheadMulti}
        options={eventListeners.map((value) => value.id)}
      />
      <ActionGroup>
        <Button
          variant="primary"
          type="submit"
          data-testid={"saveEventListenerBtn"}
        >
          {t("save")}
        </Button>
        <Button
          variant="link"
          data-testid={"revertEventListenerBtn"}
          onClick={reset}
        >
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormProvider>
  );
};
