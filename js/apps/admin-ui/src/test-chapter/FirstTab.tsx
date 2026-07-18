import { useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Flex,
  FlexItem,
  PageSection,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { CopyIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useRealm } from "../context/realm-context/RealmContext";

export default function FirstTab() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const copyRealmName = async () => {
    try {
      await navigator.clipboard.writeText(realm);
      addAlert(t("copySuccess"), AlertVariant.success);
    } catch (error) {
      addError("clipboardCopyError", error);
    }
  };

  return (
    <PageSection data-testid="test-section-first-tab" variant="light">
      <TextContent>
        <Text>{t("firstTabContent")}</Text>
      </TextContent>
      <Flex
        alignItems={{ default: "alignItemsCenter" }}
        className="pf-v5-u-mt-md"
      >
        <FlexItem>
          <Text data-testid="test-section-realm-name">
            <strong>{t("currentRealmName")}:</strong> {realm}
          </Text>
        </FlexItem>
        <FlexItem>
          <Button
            data-testid="test-section-copy-realm-name"
            variant={ButtonVariant.secondary}
            icon={<CopyIcon />}
            onClick={copyRealmName}
          >
            {t("copy")}
          </Button>
        </FlexItem>
      </Flex>
    </PageSection>
  );
}
