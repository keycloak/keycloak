import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import {
  AlertVariant,
  ButtonVariant,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
  Title,
} from "@patternfly/react-core";

import type { RealmEventsConfigRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/realmEventsConfigRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { useFetch, useAdminClient } from "../../context/auth/AdminClient";
import { EventConfigForm, EventsType } from "./EventConfigForm";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { EventsTypeTable, EventType } from "./EventsTypeTable";
import { AddEventTypesDialog } from "./AddEventTypesDialog";
import { EventListenersForm } from "./EventListenersForm";

export const EventsTab = () => {
  const { t } = useTranslation("realm-settings");
  const form = useForm<RealmEventsConfigRepresentation>();
  const { setValue, handleSubmit, watch, reset } = form;

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const [tableKey, setTableKey] = useState(0);
  const reload = () => setTableKey(new Date().getTime());

  const [activeTab, setActiveTab] = useState("event");
  const [events, setEvents] = useState<RealmEventsConfigRepresentation>();
  const [type, setType] = useState<EventsType>();
  const [addEventType, setAddEventType] = useState(false);

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const setupForm = (eventConfig?: RealmEventsConfigRepresentation) => {
    reset(eventConfig);
    setEvents(eventConfig);
    Object.entries(eventConfig || {}).forEach((entry) =>
      setValue(entry[0], entry[1])
    );
  };

  const clear = async (type: EventsType) => {
    setType(type);
    toggleDeleteDialog();
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "realm-settings:deleteEvents",
    messageKey: "realm-settings:deleteEventsConfirm",
    continueButtonLabel: "common:clear",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        switch (type) {
          case "admin":
            await adminClient.realms.clearAdminEvents({ realm });
            break;
          case "user":
            await adminClient.realms.clearEvents({ realm });
            break;
        }
        addAlert(t(`${type}-events-cleared`), AlertVariant.success);
      } catch (error) {
        addError(`realm-settings:${type}-events-cleared-error`, error);
      }
    },
  });

  useFetch(
    () => adminClient.realms.getConfigEvents({ realm }),
    (eventConfig) => {
      setupForm(eventConfig);
      reload();
    },
    [key]
  );

  const save = async (eventConfig: RealmEventsConfigRepresentation) => {
    const updatedEventListener =
      events?.eventsListeners !== eventConfig.eventsListeners;

    try {
      await adminClient.realms.updateConfigEvents({ realm }, eventConfig);
      setupForm({ ...events, ...eventConfig });
      addAlert(
        updatedEventListener
          ? t("realm-settings:saveEventListenersSuccess")
          : t("realm-settings:eventConfigSuccessfully"),
        AlertVariant.success
      );
    } catch (error) {
      addError(
        updatedEventListener
          ? t("realm-settings:saveEventListenersError")
          : t("realm-settings:eventConfigError"),
        error
      );
    }
  };

  const addEventTypes = async (eventTypes: EventType[]) => {
    const eventsTypes = eventTypes.map((type) => type.id);
    const enabledEvents = events!.enabledEventTypes?.concat(eventsTypes);
    await addEvents(enabledEvents);
  };

  const addEvents = async (events: string[] = []) => {
    const eventConfig = { ...form.getValues(), enabledEventTypes: events };
    await save(eventConfig);
    setAddEventType(false);
    refresh();
  };

  const eventsEnabled: boolean = watch("eventsEnabled") || false;
  return (
    <>
      <DeleteConfirm />
      {addEventType && (
        <AddEventTypesDialog
          onConfirm={(eventTypes) => addEventTypes(eventTypes)}
          configured={events?.enabledEventTypes || []}
          onClose={() => setAddEventType(false)}
        />
      )}
      <Tabs
        activeKey={activeTab}
        onSelect={(_, key) => setActiveTab(key as string)}
      >
        <Tab
          eventKey="event"
          title={<TabTitleText>{t("eventListeners")}</TabTitleText>}
          data-testid="rs-event-listeners-tab"
        >
          <PageSection>
            <FormAccess
              role="manage-events"
              isHorizontal
              onSubmit={handleSubmit(save)}
            >
              <EventListenersForm form={form} reset={() => setupForm(events)} />
            </FormAccess>
          </PageSection>
        </Tab>
        <Tab
          eventKey="user"
          title={<TabTitleText>{t("userEventsSettings")}</TabTitleText>}
          data-testid="rs-events-tab"
        >
          <PageSection>
            <Title headingLevel="h1" size="xl">
              {t("userEventsConfig")}
            </Title>
          </PageSection>
          <PageSection>
            <FormAccess
              role="manage-events"
              isHorizontal
              onSubmit={handleSubmit(save)}
            >
              <EventConfigForm
                type="user"
                form={form}
                reset={() => setupForm(events)}
                clear={() => clear("user")}
              />
            </FormAccess>
          </PageSection>
          {eventsEnabled && (
            <PageSection>
              <EventsTypeTable
                key={tableKey}
                addTypes={() => setAddEventType(true)}
                loader={() =>
                  Promise.resolve(
                    events?.enabledEventTypes?.map((id) => {
                      return { id };
                    }) || []
                  )
                }
                onDelete={(value) => {
                  const enabledEventTypes = events?.enabledEventTypes?.filter(
                    (e) => e !== value.id
                  );
                  addEvents(enabledEventTypes);
                  setEvents({ ...events, enabledEventTypes });
                }}
              />
            </PageSection>
          )}
        </Tab>
        <Tab
          eventKey="admin"
          title={<TabTitleText>{t("adminEventsSettings")}</TabTitleText>}
          data-testid="rs-admin-events-tab"
        >
          <PageSection>
            <Title headingLevel="h4" size="xl">
              {t("adminEventsConfig")}
            </Title>
          </PageSection>
          <PageSection>
            <FormAccess
              role="manage-events"
              isHorizontal
              onSubmit={handleSubmit(save)}
            >
              <EventConfigForm
                type="admin"
                form={form}
                reset={() => setupForm(events)}
                clear={() => clear("admin")}
              />
            </FormAccess>
          </PageSection>
        </Tab>
      </Tabs>
    </>
  );
};
