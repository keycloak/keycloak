import FeatureRepresentation, {
  FeatureType,
} from "@keycloak/keycloak-admin-client/lib/defs/featureRepresentation";
import { HelpItem, label, useEnvironment } from "@keycloak/keycloak-ui-shared";
import {
  ActionList,
  ActionListItem,
  Brand,
  Button,
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Divider,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  Flex,
  FlexItem,
  Grid,
  GridItem,
  Label,
  PageSection,
  Tab,
  TabTitleText,
  Text,
  TextContent,
  TextVariants,
  Title,
  Tooltip,
} from "@patternfly/react-core";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import helpUrls from "../help-urls";
import useLocaleSort, { mapByKey } from "../utils/useLocaleSort";
import { ProviderInfo } from "./ProviderInfo";
import { DashboardTab, toDashboard } from "./routes/Dashboard";

import "./dashboard.css";

const EmptyDashboard = () => {
  const { environment } = useEnvironment();

  const { t } = useTranslation();
  const { realm, realmRepresentation: realmInfo } = useRealm();
  const brandImage = environment.logo ? environment.logo : "/icon.svg";
  const realmDisplayInfo = label(t, realmInfo?.displayName, realm);

  return (
    <PageSection variant="light">
      <EmptyState variant="lg">
        <Brand
          src={environment.resourceUrl + brandImage}
          alt="Keycloak icon"
          className="keycloak__dashboard_icon"
        />
        <EmptyStateHeader titleText={<>{t("welcome")}</>} headingLevel="h2" />
        <EmptyStateHeader titleText={realmDisplayInfo} headingLevel="h1" />
        <EmptyStateBody>{t("introduction")}</EmptyStateBody>
      </EmptyState>
    </PageSection>
  );
};

type FeatureItemProps = {
  feature: FeatureRepresentation;
};

const FeatureItem = ({ feature }: FeatureItemProps) => {
  const content = (
    <Label color="grey" className="pf-v5-u-font-size-sm">
      {feature.name}
    </Label>
  );

  return feature.label ? (
    <Tooltip content={<div>{feature.label}</div>}>{content}</Tooltip>
  ) : (
    content
  );
};

type FeatureTypeColumnProps = {
  typeName: string;
  typeColor: "orange" | "blue" | "green" | "grey";
  features: FeatureRepresentation[];
  showLabel?: boolean;
  layoutDirection?: "horizontal" | "vertical";
  showWhenEmpty?: boolean;
  typeDescription?: string;
  flex?: "flex_1" | "flex_2" | undefined;
};

const FeatureTypeColumn = ({
  typeName,
  typeColor,
  features,
  showLabel = true,
  layoutDirection = "horizontal",
  showWhenEmpty = false,
  typeDescription,
  flex,
}: FeatureTypeColumnProps) => {
  if (features.length === 0 && !showWhenEmpty) return null;

  const typeLabel = (
    <Label color={typeColor} className="pf-v5-u-font-weight-bold">
      {typeName}
    </Label>
  );

  const content = (
    <Flex
      direction={{ default: "column" }}
      spaceItems={{ default: "spaceItemsSm" }}
    >
      {showLabel && (
        <FlexItem className="pf-v5-u-text-align-center">
          {typeDescription ? (
            <Tooltip content={<div>{typeDescription}</div>}>
              {typeLabel}
            </Tooltip>
          ) : (
            typeLabel
          )}
        </FlexItem>
      )}
      <FlexItem>
        <Flex
          direction={{
            default: layoutDirection === "vertical" ? "column" : "row",
          }}
          spaceItems={{ default: "spaceItemsSm" }}
          flexWrap={{
            default: layoutDirection === "horizontal" ? "wrap" : "nowrap",
          }}
          justifyContent={{ default: "justifyContentCenter" }}
        >
          {features.map((feature) => (
            <FlexItem key={feature.name}>
              <FeatureItem feature={feature} />
            </FlexItem>
          ))}
        </Flex>
      </FlexItem>
    </Flex>
  );

  return flex ? (
    <FlexItem flex={{ default: flex }}>{content}</FlexItem>
  ) : (
    <FlexItem>{content}</FlexItem>
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

  // Group enabled features by type
  const enabledFeaturesByType = useMemo(() => {
    return {
      experimental: enabledFeatures.filter(
        (f) => f.type === FeatureType.Experimental,
      ),
      preview: enabledFeatures.filter(
        (f) =>
          f.type === FeatureType.Preview ||
          f.type === FeatureType.PreviewDisabledByDefault,
      ),
      default: enabledFeatures.filter(
        (f) =>
          f.type === FeatureType.Default ||
          f.type === FeatureType.DisabledByDefault,
      ),
      deprecated: enabledFeatures.filter(
        (f) => f.type === FeatureType.Deprecated,
      ),
    };
  }, [enabledFeatures]);

  // Determine if small categories should be combined
  const shouldCombineSmallCategories = useMemo(() => {
    const smallCategories = [
      enabledFeaturesByType.preview,
      enabledFeaturesByType.deprecated,
      enabledFeaturesByType.experimental,
    ];

    const allSmall = smallCategories.every((cat) => cat.length <= 3);
    const hasMultipleCategories =
      smallCategories.filter((cat) => cat.length > 0).length >= 2;

    return allSmall && hasMultipleCategories;
  }, [enabledFeaturesByType]);

  // Group disabled features by type
  const disabledFeaturesByType = useMemo(() => {
    return {
      experimental: disabledFeatures.filter(
        (f) => f.type === FeatureType.Experimental,
      ),
      preview: disabledFeatures.filter(
        (f) =>
          f.type === FeatureType.Preview ||
          f.type === FeatureType.PreviewDisabledByDefault,
      ),
      supported: disabledFeatures.filter(
        (f) =>
          f.type === FeatureType.Default ||
          f.type === FeatureType.DisabledByDefault,
      ),
      deprecated: disabledFeatures.filter(
        (f) => f.type === FeatureType.Deprecated,
      ),
    };
  }, [disabledFeatures]);

  const useTab = (tab: DashboardTab) =>
    useRoutableTab(
      toDashboard({
        realm,
        tab,
      }),
    );

  const realmDisplayInfo = label(t, realmInfo?.displayName, realm);

  const welcomeTab = useTab("welcome");
  const infoTab = useTab("info");
  const providersTab = useTab("providers");

  if (Object.keys(serverInfo).length === 0) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <PageSection variant="light">
        <TextContent className="pf-v5-u-mr-sm">
          <Text component="h1">{t("realmNameTitle", { name: realm })}</Text>
        </TextContent>
      </PageSection>
      <PageSection variant="light" className="pf-v5-u-p-0">
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
            <PageSection variant="light">
              <div className="pf-v5-l-grid pf-v5-u-ml-lg">
                <div className="pf-v5-l-grid__item pf-m-12-col">
                  <Title
                    data-testid="welcomeTitle"
                    className="pf-v5-u-font-weight-bold"
                    headingLevel="h2"
                    size="3xl"
                  >
                    {t("welcomeTo", { realmDisplayInfo })}
                  </Title>
                </div>
                <div className="pf-v5-l-grid__item keycloak__dashboard_welcome_tab">
                  <Text component={TextVariants.h3}>{t("welcomeText")}</Text>
                </div>
                <div className="pf-v5-l-grid__item pf-m-10-col pf-v5-u-mt-md">
                  <Button
                    className="pf-v5-u-px-lg pf-v5-u-py-sm"
                    component="a"
                    href={helpUrls.documentation}
                    target="_blank"
                    variant="primary"
                  >
                    {t("viewDocumentation")}
                  </Button>
                </div>
                <ActionList className="pf-v5-u-mt-sm">
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
              </div>
            </PageSection>
          </Tab>
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
                        </DescriptionListGroup>
                      </DescriptionList>
                    </CardBody>
                    <CardTitle>{t("cpu")}</CardTitle>
                    <CardBody>
                      <DescriptionList>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("processorCount")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {serverInfo.cpuInfo?.processorCount}
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
                            {shouldCombineSmallCategories ? (
                              <Flex spaceItems={{ default: "spaceItemsMd" }}>
                                <FeatureTypeColumn
                                  typeName={t("supported")}
                                  typeColor="green"
                                  features={enabledFeaturesByType.default}
                                  layoutDirection="horizontal"
                                  typeDescription={t(
                                    "featureTypeSupportedHelp",
                                  )}
                                  flex="flex_2"
                                />
                                <FlexItem flex={{ default: "flex_1" }}>
                                  <Flex
                                    direction={{ default: "column" }}
                                    spaceItems={{ default: "spaceItemsSm" }}
                                    className="pf-v5-u-p-sm"
                                  >
                                    <FeatureTypeColumn
                                      typeName={t("preview")}
                                      typeColor="blue"
                                      features={enabledFeaturesByType.preview}
                                      layoutDirection="horizontal"
                                      typeDescription={t(
                                        "featureTypePreviewHelp",
                                      )}
                                    />
                                    {enabledFeaturesByType.preview.length >
                                      0 && <Divider />}
                                    <FeatureTypeColumn
                                      typeName={t("deprecated")}
                                      typeColor="grey"
                                      features={
                                        enabledFeaturesByType.deprecated
                                      }
                                      layoutDirection="horizontal"
                                      typeDescription={t(
                                        "featureTypeDeprecatedHelp",
                                      )}
                                    />
                                    {enabledFeaturesByType.deprecated.length >
                                      0 && <Divider />}
                                    <FeatureTypeColumn
                                      typeName={t("experimental")}
                                      typeColor="orange"
                                      features={
                                        enabledFeaturesByType.experimental
                                      }
                                      layoutDirection="horizontal"
                                      typeDescription={t(
                                        "featureTypeExperimentalHelp",
                                      )}
                                    />
                                  </Flex>
                                </FlexItem>
                              </Flex>
                            ) : (
                              <Flex spaceItems={{ default: "spaceItemsMd" }}>
                                <FeatureTypeColumn
                                  typeName={t("supported")}
                                  typeColor="green"
                                  features={enabledFeaturesByType.default}
                                  layoutDirection="horizontal"
                                  typeDescription={t(
                                    "featureTypeSupportedHelp",
                                  )}
                                  flex="flex_2"
                                />
                                <FeatureTypeColumn
                                  typeName={t("preview")}
                                  typeColor="blue"
                                  features={enabledFeaturesByType.preview}
                                  layoutDirection="horizontal"
                                  typeDescription={t("featureTypePreviewHelp")}
                                  flex="flex_1"
                                />
                                <FeatureTypeColumn
                                  typeName={t("deprecated")}
                                  typeColor="grey"
                                  features={enabledFeaturesByType.deprecated}
                                  layoutDirection="horizontal"
                                  typeDescription={t(
                                    "featureTypeDeprecatedHelp",
                                  )}
                                  flex="flex_1"
                                />
                                <FeatureTypeColumn
                                  typeName={t("experimental")}
                                  typeColor="orange"
                                  features={enabledFeaturesByType.experimental}
                                  layoutDirection="horizontal"
                                  typeDescription={t(
                                    "featureTypeExperimentalHelp",
                                  )}
                                  flex="flex_1"
                                />
                              </Flex>
                            )}
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
                            <Flex spaceItems={{ default: "spaceItemsMd" }}>
                              <FeatureTypeColumn
                                typeName={t("supported")}
                                typeColor="green"
                                features={disabledFeaturesByType.supported}
                                layoutDirection="horizontal"
                                typeDescription={t("featureTypeSupportedHelp")}
                                flex="flex_1"
                              />
                              <FeatureTypeColumn
                                typeName={t("preview")}
                                typeColor="blue"
                                features={disabledFeaturesByType.preview}
                                layoutDirection="horizontal"
                                typeDescription={t("featureTypePreviewHelp")}
                                flex="flex_1"
                              />
                              <FeatureTypeColumn
                                typeName={t("deprecated")}
                                typeColor="grey"
                                features={disabledFeaturesByType.deprecated}
                                layoutDirection="horizontal"
                                typeDescription={t("featureTypeDeprecatedHelp")}
                                flex="flex_1"
                              />
                              <FeatureTypeColumn
                                typeName={t("experimental")}
                                typeColor="orange"
                                features={disabledFeaturesByType.experimental}
                                layoutDirection="horizontal"
                                typeDescription={t(
                                  "featureTypeExperimentalHelp",
                                )}
                                flex="flex_1"
                              />
                            </Flex>
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
