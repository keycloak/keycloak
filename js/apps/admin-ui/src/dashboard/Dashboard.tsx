import FeatureRepresentation, {
  FeatureType,
} from "@keycloak/keycloak-admin-client/lib/defs/featureRepresentation";
import {
  HelpItem,
  KeycloakSpinner,
  label,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionList,
  ActionListItem,
  Brand,
  Button,
  Card,
  CardBody,
  CardTitle,
  Content,
  ContentVariants,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  EmptyState,
  EmptyStateBody,
  Flex,
  Grid,
  GridItem,
  Label,
  List,
  ListItem,
  ListVariant,
  PageSection,
  Tab,
  TabTitleText,
  Title,
} from "@patternfly/react-core";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import helpUrls from "../help-urls";
import useLocaleSort, { mapByKey } from "../utils/useLocaleSort";
import { ProviderInfo } from "./ProviderInfo";
import { toDashboard } from "./routes/Dashboard";

import "./dashboard.css";

const EmptyDashboard = () => {
  const { environment } = useEnvironment();

  const { t } = useTranslation();
  const { realm, realmRepresentation: realmInfo } = useRealm();
  const brandImage = environment.logo ? environment.logo : "/icon.svg";
  const realmDisplayInfo = label(t, realmInfo?.displayName, realm);

  return (
    <PageSection hasBodyWrapper={false}>
      <EmptyState headingLevel="h1" titleText={realmDisplayInfo} variant="lg">
        <Brand
          src={environment.resourceUrl + brandImage}
          alt="Keycloak icon"
          className="keycloak__dashboard_icon"
        />
        <EmptyStateBody>{t("introduction")}</EmptyStateBody>
      </EmptyState>
    </PageSection>
  );
};

type FeatureItemProps = {
  feature: FeatureRepresentation;
};

const FeatureItem = ({ feature }: FeatureItemProps) => {
  const { t } = useTranslation();
  return (
    <ListItem className="pf-v6-u-mb-sm">
      {feature.name}&nbsp;
      {feature.type === FeatureType.Experimental && (
        <Label color="orange">{t("experimental")}</Label>
      )}
      {feature.type === FeatureType.Preview && (
        <Label color="blue">{t("preview")}</Label>
      )}
      {feature.type === FeatureType.Default && (
        <Label color="green">{t("supported")}</Label>
      )}
    </ListItem>
  );
};

const Dashboard = () => {
  const { t } = useTranslation();
  const { realm, realmRepresentation: realmInfo } = useRealm();
  const serverInfo = useServerInfo();
  const localeSort = useLocaleSort();

  const sortedFeatures = useMemo(
    () => localeSort(serverInfo.features ?? [], mapByKey("name")),
    [serverInfo.features],
  );

  const disabledFeatures = useMemo(
    () => sortedFeatures.filter((f) => !f.enabled) || [],
    [serverInfo.features],
  );

  const enabledFeatures = useMemo(
    () => sortedFeatures.filter((f) => f.enabled) || [],
    [serverInfo.features],
  );

  const realmDisplayInfo = label(t, realmInfo?.displayName, realm);

  const welcomeTab = useRoutableTab(toDashboard({ realm, tab: "welcome" }));
  const infoTab = useRoutableTab(toDashboard({ realm, tab: "info" }));
  const providersTab = useRoutableTab(toDashboard({ realm, tab: "providers" }));

  if (Object.keys(serverInfo).length === 0) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <PageSection hasBodyWrapper={false}>
        <Content className="pf-v6-u-mr-sm">
          <Content component="h1">
            {t("realmNameTitle", { name: realm })}
          </Content>
        </Content>
      </PageSection>
      <PageSection hasBodyWrapper={false} className="pf-v6-u-p-0">
        <RoutableTabs
          data-testid="dashboard-tabs"
          defaultLocation={toDashboard({
            realm,
            tab: "welcome",
          })}
          isBox
          mountOnEnter
        >
          <Tab
            id="welcome"
            data-testid="welcomeTab"
            title={<TabTitleText>{t("welcomeTabTitle")}</TabTitleText>}
            {...welcomeTab}
          >
            <PageSection hasBodyWrapper={false}>
              <Title data-testid="welcomeTitle" headingLevel="h2" size="3xl">
                {t("welcomeTo", { realmDisplayInfo })}
              </Title>
              <Content
                component={ContentVariants.p}
                className="keycloak__dashboard_welcome_tab"
              >
                {t("welcomeText")}
              </Content>
              <Flex columnGap={{ default: "columnGapNone" }}>
                <Button
                  component="a"
                  href={helpUrls.documentation}
                  target="_blank"
                  variant="primary"
                >
                  {t("viewDocumentation")}
                </Button>
              </Flex>
              <ActionList className="pf-v6-u-mt-sm">
                <ActionListItem>
                  <Button
                    component="a"
                    href={helpUrls.guides}
                    target="_blank"
                    variant="tertiary"
                  >
                    {t("viewGuides")}
                  </Button>
                </ActionListItem>
                <ActionListItem>
                  <Button
                    component="a"
                    href={helpUrls.community}
                    target="_blank"
                    variant="tertiary"
                  >
                    {t("joinCommunity")}
                  </Button>
                </ActionListItem>
                <ActionListItem>
                  <Button
                    component="a"
                    href={helpUrls.blog}
                    target="_blank"
                    variant="tertiary"
                  >
                    {t("readBlog")}
                  </Button>
                </ActionListItem>
              </ActionList>
            </PageSection>
          </Tab>
          <Tab
            id="info"
            data-testid="infoTab"
            title={<TabTitleText>{t("serverInfo")}</TabTitleText>}
            {...infoTab}
          >
            <PageSection hasBodyWrapper={false}>
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
                              fieldLabelId="enabledFeatures"
                              helpText={t("infoEnabledFeatures")}
                            />
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            <List variant={ListVariant.inline}>
                              {enabledFeatures.map((feature) => (
                                <FeatureItem
                                  key={feature.name}
                                  feature={feature}
                                />
                              ))}
                            </List>
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("disabledFeatures")}{" "}
                            <HelpItem
                              fieldLabelId="disabledFeatures"
                              helpText={t("infoDisabledFeatures")}
                            />
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            <List variant={ListVariant.inline}>
                              {disabledFeatures.map((feature) => (
                                <FeatureItem
                                  key={feature.name}
                                  feature={feature}
                                />
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
