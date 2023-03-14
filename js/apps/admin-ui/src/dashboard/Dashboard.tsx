import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { union, filter } from "lodash-es";
import {
  Brand,
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  EmptyState,
  EmptyStateBody,
  Grid,
  GridItem,
  Label,
  List,
  ListItem,
  ListVariant,
  PageSection,
  Tab,
  TabTitleText,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";

import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { toUpperCase } from "../util";
import { HelpItem } from "ui-shared";
import environment from "../environment";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import useLocaleSort from "../utils/useLocaleSort";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { DashboardTab, toDashboard } from "./routes/Dashboard";
import { ProviderInfo } from "./ProviderInfo";

import "./dashboard.css";

const EmptyDashboard = () => {
  const { t } = useTranslation("dashboard");
  const { realm } = useRealm();
  return (
    <PageSection variant="light">
      <EmptyState variant="large">
        <Brand
          src={environment.resourceUrl + "/icon.svg"}
          alt="Keycloak icon"
          className="keycloak__dashboard_icon"
        />
        <Title headingLevel="h2" size="3xl">
          {t("welcome")}
        </Title>
        <Title headingLevel="h1" size="4xl">
          {realm}
        </Title>
        <EmptyStateBody>{t("introduction")}</EmptyStateBody>
      </EmptyState>
    </PageSection>
  );
};

const Dashboard = () => {
  const { t } = useTranslation("dashboard");
  const { realm } = useRealm();
  const serverInfo = useServerInfo();
  const localeSort = useLocaleSort();

  const isDeprecatedFeature = (feature: string) =>
    disabledFeatures.includes(feature);

  const isExperimentalFeature = (feature: string) =>
    serverInfo.profileInfo?.experimentalFeatures?.includes(feature);

  const isPreviewFeature = (feature: string) =>
    serverInfo.profileInfo?.previewFeatures?.includes(feature);

  const isSupportedFeature = (feature: string) =>
    !isExperimentalFeature(feature) && !isPreviewFeature(feature);

  const disabledFeatures = useMemo(
    () =>
      localeSort(
        serverInfo.profileInfo?.disabledFeatures ?? [],
        (item) => item
      ),
    [serverInfo.profileInfo]
  );

  const enabledFeatures = useMemo(
    () =>
      localeSort(
        filter(
          union(
            serverInfo.profileInfo?.experimentalFeatures,
            serverInfo.profileInfo?.previewFeatures
          ),
          (feature) => {
            return !isDeprecatedFeature(feature);
          }
        ),
        (item) => item
      ),
    [serverInfo.profileInfo]
  );

  const useTab = (tab: DashboardTab) =>
    useRoutableTab(
      toDashboard({
        realm,
        tab,
      })
    );

  const infoTab = useTab("info");
  const providersTab = useTab("providers");

  if (Object.keys(serverInfo).length === 0) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <PageSection variant="light">
        <TextContent className="pf-u-mr-sm">
          <Text component="h1">{t("realmName", { name: realm })}</Text>
        </TextContent>
      </PageSection>
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs
          data-testid="dashboard-tabs"
          defaultLocation={toDashboard({
            realm,
            tab: "info",
          })}
          isBox
          mountOnEnter
        >
          <Tab
            id="info"
            data-testid="infoTab"
            title={<TabTitleText>{t("serverInfo")}</TabTitleText>}
            {...infoTab}
          >
            <PageSection variant="light">
              <Grid hasGutter>
                <GridItem lg={2} sm={12}>
                  <Card className="keycloak__dashboard_card">
                    <CardTitle>{t("serverInfo")}</CardTitle>
                    <CardBody>
                      <DescriptionList>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("version")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {serverInfo.systemInfo?.version}
                          </DescriptionListDescription>
                          <DescriptionListTerm>
                            {t("product")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {toUpperCase(serverInfo.profileInfo?.name!)}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                      </DescriptionList>
                    </CardBody>
                    <CardTitle>{t("memory")}</CardTitle>
                    <CardBody>
                      <DescriptionList>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("totalMemory")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {serverInfo.memoryInfo?.totalFormated}
                          </DescriptionListDescription>
                          <DescriptionListTerm>
                            {t("freeMemory")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {serverInfo.memoryInfo?.freeFormated}
                          </DescriptionListDescription>
                          <DescriptionListTerm>
                            {t("usedMemory")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {serverInfo.memoryInfo?.usedFormated}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                      </DescriptionList>
                    </CardBody>
                  </Card>
                </GridItem>
                <GridItem lg={10} sm={12}>
                  <Card className="keycloak__dashboard_card">
                    <CardTitle>{t("profile")}</CardTitle>
                    <CardBody>
                      <DescriptionList>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("enabledFeatures")}{" "}
                            <HelpItem
                              fieldLabelId="dashboard:enabledFeatures"
                              helpText={t("dashboard:infoEnabledFeatures")}
                            />
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            <List variant={ListVariant.inline}>
                              {enabledFeatures.map((feature) => (
                                <ListItem key={feature} className="pf-u-mb-sm">
                                  {feature}{" "}
                                  {isExperimentalFeature(feature) ? (
                                    <Label color="orange">
                                      {t("experimental")}
                                    </Label>
                                  ) : null}
                                  {isPreviewFeature(feature) ? (
                                    <Label color="blue">{t("preview")}</Label>
                                  ) : null}
                                </ListItem>
                              ))}
                            </List>
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("disabledFeatures")}{" "}
                            <HelpItem
                              fieldLabelId="dashboard:disabledFeatures"
                              helpText={t("dashboard:infoDisabledFeatures")}
                            />
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            <List variant={ListVariant.inline}>
                              {disabledFeatures.map((feature) => (
                                <ListItem key={feature} className="pf-u-mb-sm">
                                  {feature}{" "}
                                  {isExperimentalFeature(feature) ? (
                                    <Label color="orange">
                                      {t("experimental")}
                                    </Label>
                                  ) : null}
                                  {isPreviewFeature(feature) ? (
                                    <Label color="blue">{t("preview")}</Label>
                                  ) : null}
                                  {isSupportedFeature(feature) ? (
                                    <Label color="green">
                                      {t("supported")}
                                    </Label>
                                  ) : null}
                                </ListItem>
                              ))}
                            </List>
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                      </DescriptionList>
                    </CardBody>
                  </Card>
                </GridItem>
              </Grid>
            </PageSection>
          </Tab>
          <Tab
            id="providers"
            data-testid="providersTab"
            title={<TabTitleText>{t("providerInfo")}</TabTitleText>}
            {...providersTab}
          >
            <ProviderInfo />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
};

export default function DashboardSection() {
  const { realm } = useRealm();
  const isMasterRealm = realm === "master";
  return (
    <>
      {!isMasterRealm && <EmptyDashboard />}
      {isMasterRealm && <Dashboard />}
    </>
  );
}
