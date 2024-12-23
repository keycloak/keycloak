import {
  AlertVariant,
  Button,
  Flex,
  FlexItem,
  List,
  ListItem,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useRealm } from "./context/realm-context/RealmContext";
import { useAdminClient } from "./admin-client";
import { useTranslation } from "react-i18next";
import { HelpItem, useAlerts } from "@keycloak/keycloak-ui-shared";

export type ClearCachesModalProps = {
  onClose: () => void;
};
export const PageHeaderClearCachesModal = ({
  onClose,
}: ClearCachesModalProps) => {
  const { realm: realmName } = useRealm();
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addError, addAlert } = useAlerts();

  const clearCache =
    (clearCacheFn: typeof adminClient.cache.clearRealmCache) =>
    async (realm: string) => {
      try {
        await clearCacheFn({ realm });
        addAlert(t("clearCacheSuccess"), AlertVariant.success);
      } catch (error) {
        addError("clearCacheError", error);
      }
    };
  const clearRealmCache = clearCache(adminClient.cache.clearRealmCache);
  const clearUserCache = clearCache(adminClient.cache.clearUserCache);
  const clearKeysCache = clearCache(adminClient.cache.clearKeysCache);
  const clearCrlCache = clearCache(adminClient.cache.clearCrlCache);

  return (
    <Modal
      title={t("clearCachesTitle")}
      variant={ModalVariant.small}
      isOpen
      onClose={onClose}
      onClick={(e) => e.stopPropagation()}
    >
      <List isPlain isBordered>
        <ListItem>
          <Flex justifyContent={{ default: "justifyContentSpaceBetween" }}>
            <FlexItem>
              {t("realmCache")}{" "}
              <HelpItem
                helpText={t("clearRealmCacheHelp")}
                fieldLabelId="clearRealmCacheHelp"
              />
            </FlexItem>
            <FlexItem>
              <Button onClick={() => clearRealmCache(realmName)}>
                {t("clearButtonTitle")}
              </Button>
            </FlexItem>
          </Flex>
        </ListItem>
        <ListItem>
          <Flex justifyContent={{ default: "justifyContentSpaceBetween" }}>
            <FlexItem>
              {t("userCache")}{" "}
              <HelpItem
                helpText={t("clearUserCacheHelp")}
                fieldLabelId="clearUserCacheHelp"
              />
            </FlexItem>
            <FlexItem>
              <Button onClick={() => clearUserCache(realmName)}>
                {t("clearButtonTitle")}
              </Button>
            </FlexItem>
          </Flex>
        </ListItem>
        <ListItem>
          <Flex justifyContent={{ default: "justifyContentSpaceBetween" }}>
            <FlexItem>
              {t("keysCache")}{" "}
              <HelpItem
                helpText={t("clearKeysCacheHelp")}
                fieldLabelId="clearKeysCacheHelp"
              />
            </FlexItem>
            <FlexItem>
              <Button onClick={() => clearKeysCache(realmName)}>
                {t("clearButtonTitle")}
              </Button>
            </FlexItem>
          </Flex>
        </ListItem>
        <ListItem>
          <Flex justifyContent={{ default: "justifyContentSpaceBetween" }}>
            <FlexItem>
              {t("crlCache")}{" "}
              <HelpItem
                helpText={t("clearCrlCacheHelp")}
                fieldLabelId="clearCrlCacheHelp"
              />
            </FlexItem>
            <FlexItem>
              <Button onClick={() => clearCrlCache(realmName)}>
                {t("clearButtonTitle")}
              </Button>
            </FlexItem>
          </Flex>
        </ListItem>
      </List>
    </Modal>
  );
};
