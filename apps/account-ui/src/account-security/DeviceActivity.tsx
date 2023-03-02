import {
  Button,
  DataList,
  DataListContent,
  DataListItem,
  DataListItemRow,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Label,
  Spinner,
  Split,
  SplitItem,
  Title,
  Tooltip,
} from "@patternfly/react-core";
import {
  SyncAltIcon,
  MobileAltIcon,
  DesktopIcon,
} from "@patternfly/react-icons";
import { TFuncKey } from "i18next";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { deleteSession, getDevices } from "../api/methods";
import {
  DeviceRepresentation,
  SessionRepresentation,
  ClientRepresentation,
} from "../api/representations";
import { useAlerts, ContinueCancelModal } from "ui-shared";
import useFormatter from "../components/formatter/format-date";
import { Page } from "../components/page/Page";
import { keycloak } from "../keycloak";
import { usePromise } from "../utils/usePromise";

const DeviceActivity = () => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { formatTime } = useFormatter();

  const [devices, setDevices] = useState<DeviceRepresentation[]>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const moveCurrentToTop = (devices: DeviceRepresentation[]) => {
    let currentDevice = devices[0];

    const index = devices.findIndex((d) => d.current);
    currentDevice = devices.splice(index, 1)[0];
    devices.unshift(currentDevice);

    const sessionIndex = currentDevice.sessions.findIndex((s) => s.current);
    const currentSession = currentDevice.sessions.splice(sessionIndex, 1)[0];
    currentDevice.sessions.unshift(currentSession);

    setDevices(devices);
  };

  usePromise((signal) => getDevices({ signal }), moveCurrentToTop, [key]);

  const signOutAll = async () => {
    await deleteSession();
    keycloak.logout();
  };

  const signOutSession = async (
    session: SessionRepresentation,
    device: DeviceRepresentation
  ) => {
    try {
      await deleteSession(session.id);
      addAlert(t("signedOutSession", [session.browser, device.os]));
      refresh();
    } catch (error) {
      addError(t("errorSignOutMessage", { error }).toString());
    }
  };

  const makeClientsString = (clients: ClientRepresentation[]): string => {
    let clientsString = "";
    clients.forEach((client, index) => {
      let clientName: string;
      if (client.clientName !== "") {
        clientName = t(client.clientName as TFuncKey);
      } else {
        clientName = client.clientId;
      }

      clientsString += clientName;

      if (clients.length > index + 1) clientsString += ", ";
    });

    return clientsString;
  };

  if (!devices) {
    return <Spinner />;
  }

  return (
    <Page
      title={t("device-activity")}
      description={t("signedInDevicesExplanation")}
    >
      <Split hasGutter className="pf-u-mb-lg">
        <SplitItem isFilled>
          <Title headingLevel="h2" size="xl">
            {t("signedInDevices")}
          </Title>
        </SplitItem>
        <SplitItem>
          <Tooltip content={t("refreshPage")}>
            <Button
              aria-describedby="refresh page"
              id="refresh-page"
              variant="link"
              onClick={() => refresh()}
              icon={<SyncAltIcon />}
            >
              Refresh
            </Button>
          </Tooltip>

          {(devices.length > 1 || devices[0].sessions.length > 1) && (
            <ContinueCancelModal
              buttonTitle={t("signOutAllDevices")}
              modalTitle={t("signOutAllDevices")}
              modalMessage={t("signOutAllDevicesWarning")}
              onContinue={() => signOutAll()}
            />
          )}
        </SplitItem>
      </Split>
      <DataList
        className="signed-in-device-list"
        aria-label={t("signedInDevices")}
      >
        <DataListItem aria-labelledby="sessions">
          {devices.map((device) =>
            device.sessions.map((session) => (
              <DataListItemRow key={device.id}>
                <DataListContent
                  aria-label="device-sessions-content"
                  className="pf-u-flex-grow-1"
                >
                  <Grid hasGutter>
                    <GridItem span={1} rowSpan={2}>
                      {device.mobile ? <MobileAltIcon /> : <DesktopIcon />}
                    </GridItem>
                    <GridItem sm={8} md={9} span={10}>
                      <span className="pf-u-mr-md session-title">
                        {device.os.toLowerCase().includes("unknown")
                          ? t("unknownOperatingSystem")
                          : device.os}{" "}
                        {!device.osVersion.toLowerCase().includes("unknown") &&
                          device.osVersion}{" "}
                        / {session.browser}
                      </span>
                      {session.current && (
                        <Label color="green">{t("currentSession")}</Label>
                      )}
                    </GridItem>
                    <GridItem
                      className="pf-u-text-align-right"
                      sm={3}
                      md={2}
                      span={1}
                    >
                      {!session.current && (
                        <ContinueCancelModal
                          buttonTitle={t("doSignOut")}
                          modalTitle={t("doSignOut")}
                          buttonVariant="secondary"
                          modalMessage={t("signOutWarning")}
                          onContinue={() => signOutSession(session, device)}
                        />
                      )}
                    </GridItem>
                    <GridItem span={11}>
                      <DescriptionList
                        className="signed-in-device-grid"
                        columnModifier={{ sm: "2Col", lg: "3Col" }}
                        cols={5}
                        rows={1}
                      >
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("ipAddress")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {session.ipAddress}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("lastAccessedOn")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {formatTime(session.lastAccess)}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("clients")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {makeClientsString(session.clients)}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("started")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {formatTime(session.started)}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("expires")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {formatTime(session.expires)}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                      </DescriptionList>
                    </GridItem>
                  </Grid>
                </DataListContent>
              </DataListItemRow>
            ))
          )}
        </DataListItem>
      </DataList>
    </Page>
  );
};

export default DeviceActivity;
