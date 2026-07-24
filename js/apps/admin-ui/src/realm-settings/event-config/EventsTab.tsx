import type { RealmEventsConfigRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/realmEventsConfigRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  ButtonVariant,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";
import { isEqual } from "lodash-es";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertToFormValues } from "../../util";
import { AddEventTypesDialog } from "./AddEventTypesDialog";
import { EventConfigForm, EventsType } from "./EventConfigForm";
import { EventListenersForm } from "./EventListenersForm";
import { EventsTypeTable, EventType } from "./EventsTypeTable";

type EventsTabProps = {
  realm: RealmRepresentation;
};

type EventsConfigForm = RealmEventsConfigRepresentation & {
  adminEventsExpiration?: number;
};

export const EventsTab = ({ realm }: EventsTabProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<EventsConfigForm>();
  const { setValue, handleSubmit } = form;

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const [tableKey, setTableKey] = useState(0);
  const reload = () => setTableKey(new Date().getTime());

  const [activeTab, setActiveTab] = useState("event");
  const [events, setEvents] = useState<RealmEventsConfigRepresentation>();
  const [type, setType] = useState<EventsType>();
  const [addEventType, setAddEventType] = useState(false);

  const { addAlert, addError } = useAlerts();
  const { realm: realmName, refresh: refreshRealm } = useRealm();

  const setupForm = (eventConfig?: EventsConfigForm) => {
    setEvents(eventConfig);
    convertToFormValues(eventConfig || {}, setValue);
  };

  const clear = async (type: EventsType) => {
    setType(type);
    toggleDeleteDialog();
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteEvents",
    messageKey: "deleteEventsConfirm",
    continueButtonLabel: "clear",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        switch (type) {
          case "admin":
            await adminClient.realms.clearAdminEvents({ realm: realmName });
            break;
          case "user":
            await adminClient.realms.clearEvents({ realm: realmName });
            break;
        }
        addAlert(t(`${type}-events-cleared`), AlertVariant.success);
      } catch (error) {
        addError(`${type}-events-cleared-error`, error);
      }
    },
  });

  useFetch(
    () => adminClient.realms.getConfigEvents({ realm: realmName }),
    (eventConfig) => {
      setupForm({
        ...eventConfig,
        adminEventsExpiration: realm.attributes?.adminEventsExpiration,
      });
      reload();
    },
    [key],
  );

  const save = async (config: EventsConfigForm) => {
    const updatedEventListener = !isEqual(
      events?.eventsListeners,
      config.eventsListeners,
    );

    const { adminEventsExpiration, ...eventConfig } = config;
    if (realm.attributes?.adminEventsExpiration !== adminEventsExpiration) {
      await adminClient.realms.update(
        { realm: realmName },
        {
          ...realm,
          attributes: { ...(realm.attributes || {}), adminEventsExpiration },
        },
      );
    }

    try {
      await adminClient.realms.updateConfigEvents(
        { realm: realmName },
        eventConfig,
      );
      setupForm({ ...events, ...eventConfig, adminEventsExpiration });
      addAlert(
        updatedEventListener
          ? t("saveEventListenersSuccess")
          : t("eventConfigSuccessfully"),
        AlertVariant.success,
      );
    } catch (error) {
      addError(
        updatedEventListener
          ? t("saveEventListenersError")
          : t("eventConfigError"),
        error,
      );
    }

    refreshRealm();
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

  const removeEvents = async (eventTypes: EventType[] = []) => {
    const values = eventTypes.map((type) => type.id);
    const enabledEventTypes = events?.enabledEventTypes?.filter(
      (e) => !values.includes(e),
    );
    await addEvents(enabledEventTypes);
    setEvents({ ...events, enabledEventTypes });
  };

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
          <PageSection>
            <EventsTypeTable
              key={tableKey}
              addTypes={() => setAddEventType(true)}
              eventTypes={events?.enabledEventTypes || []}
              onDelete={(value) => removeEvents([value])}
              onDeleteAll={removeEvents}
            />
          </PageSection>
        </Tab>
        <Tab
          eventKey="admin"
          title={<TabTitleText>{t("adminEventsSettings")}</TabTitleText>}
          data-testid="rs-admin-events-tab"
        >
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
