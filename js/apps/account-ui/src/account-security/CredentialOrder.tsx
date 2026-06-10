import {
  Button,
  Card,
  CardBody,
  CardFooter,
  Flex,
  FlexItem,
  Gallery,
  PageSection,
  Switch,
  Title,
  Tooltip,
} from "@patternfly/react-core";
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  FingerprintIcon,
  LockIcon,
  ListIcon,
  MobileAltIcon,
  PlusCircleIcon,
} from "@patternfly/react-icons";
import { type ComponentType, type ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { moveDown, moveUp } from "../api/methods";
import { useAccountAlerts } from "../utils/useAccountAlerts";
import style from "./CredentialOrder.module.css";
import { CredentialContainer } from "../api/representations";
import type { TFuncKey } from "../i18n-type";
import { AccountEnvironment } from "..";

const credentialIcons: Partial<Record<string, ComponentType>> = {
  password: LockIcon,
  otp: MobileAltIcon,
  webauthn: FingerprintIcon,
  "webauthn-passwordless": FingerprintIcon,
  "recovery-authn-codes": ListIcon,
};

type ArrowButtonProps = {
  visible: boolean;
  disabled: boolean;
  tooltip: string;
  testId?: string;
  icon: ReactNode;
  onClick: () => void;
};

const ArrowButton = ({
  visible,
  disabled,
  tooltip,
  testId,
  icon,
  onClick,
}: ArrowButtonProps) => {
  if (!visible) {
    return (
      <Button
        variant="plain"
        size="sm"
        isDisabled
        data-testid={testId}
        icon={icon}
      />
    );
  }

  return (
    <Tooltip content={tooltip}>
      <Button
        variant="plain"
        size="sm"
        isDisabled={disabled}
        data-testid={testId}
        onClick={onClick}
        icon={icon}
      />
    </Tooltip>
  );
};

type CredentialOrderProps = {
  credentials: CredentialContainer[];
  onOrderChanged: () => void;
};

export const CredentialOrder = ({
  credentials,
  onOrderChanged,
}: CredentialOrderProps) => {
  const { t } = useTranslation();
  const context = useEnvironment<AccountEnvironment>();
  const { addError } = useAccountAlerts();

  const [includePasswordless, setIncludePasswordless] = useState(
    () => localStorage.getItem("includePasswordlessOrder") === "true",
  );
  const [isMoving, setIsMoving] = useState(false);

  if (credentials.length < 2) {
    return null;
  }

  const hasPasswordless = credentials.some(
    (c) => c.category === "passwordless",
  );

  const visibleCredentials = includePasswordless
    ? credentials
    : credentials.filter((c) => c.category !== "passwordless");

  if (visibleCredentials.length < 2) {
    return null;
  }

  const configuredCount = visibleCredentials.filter(
    (c) => c.userCredentialMetadatas.length > 0,
  ).length;

  const handleMove = async (moveFn: typeof moveUp, type: string) => {
    setIsMoving(true);
    try {
      await moveFn(context, type);
      onOrderChanged();
    } catch (error) {
      addError("credentialMoveError", error);
    } finally {
      setIsMoving(false);
    }
  };

  return (
    <PageSection
      variant="light"
      className="pf-v5-u-px-0 pf-v5-u-pt-0"
      data-testid="credential-order-section"
    >
      <Title headingLevel="h2" size="xl" className="pf-v5-u-mb-sm">
        {t("credentialOrder")}
      </Title>
      <p className="pf-v5-u-mb-md pf-v5-u-color-200">
        {t("credentialOrderDescription")}
      </p>
      <Gallery hasGutter minWidths={{ default: "150px" }}>
        {visibleCredentials.map((container, index) => {
          const IconComponent = credentialIcons[container.type] || LockIcon;
          const displayName = t(`${container.type}-display-name` as TFuncKey);
          const isConfigured = container.userCredentialMetadatas.length > 0;
          const canMove = isConfigured && configuredCount > 1 && !isMoving;

          return (
            <Card
              key={container.type}
              isCompact
              isFullHeight
              isDisabled={!isConfigured}
              data-testid={`${container.type}/order-card`}
            >
              <CardBody
                className="pf-v5-u-display-flex pf-v5-u-flex-direction-column pf-v5-u-align-items-center pf-v5-u-flex-grow-1 pf-v5-u-pb-xs"
                style={{ position: "relative" }}
              >
                {!isConfigured && (
                  <Tooltip content={t("setUpNew", { name: displayName })}>
                    <Button
                      variant="plain"
                      className={style.setupButton}
                      onClick={() =>
                        context.keycloak.login({
                          action: container.createAction,
                        })
                      }
                      icon={<PlusCircleIcon />}
                    />
                  </Tooltip>
                )}
                <div className="pf-v5-u-font-size-3xl pf-v5-u-mb-sm">
                  <IconComponent />
                </div>
                <div className="pf-v5-u-text-align-center pf-v5-u-display-flex pf-v5-u-flex-direction-column pf-v5-u-align-items-center pf-v5-u-justify-content-center pf-v5-u-flex-grow-1">
                  <span
                    className="cred-title pf-v5-u-font-weight-bold"
                    data-testid={`${container.type}/order-title`}
                  >
                    {displayName}
                  </span>
                  <span className="pf-v5-u-color-200 pf-v5-u-font-size-sm">
                    {t([
                      `${container.category}-short`,
                      container.category,
                    ] as TFuncKey[])}
                  </span>
                </div>
              </CardBody>
              <CardFooter className="pf-v5-u-pt-xs pf-v5-u-pb-xs">
                <Flex
                  justifyContent={{ default: "justifyContentCenter" }}
                  spaceItems={{ default: "spaceItemsNone" }}
                >
                  <FlexItem>
                    <ArrowButton
                      visible={index > 0}
                      disabled={!canMove}
                      tooltip={t("moveCredentialUp", { type: displayName })}
                      testId={`${container.type}/up`}
                      icon={<ArrowLeftIcon />}
                      onClick={() => handleMove(moveUp, container.type)}
                    />
                  </FlexItem>
                  <FlexItem>
                    <ArrowButton
                      visible={index < visibleCredentials.length - 1}
                      disabled={!canMove}
                      tooltip={t("moveCredentialDown", { type: displayName })}
                      testId={`${container.type}/down`}
                      icon={<ArrowRightIcon />}
                      onClick={() => handleMove(moveDown, container.type)}
                    />
                  </FlexItem>
                </Flex>
              </CardFooter>
            </Card>
          );
        })}
      </Gallery>
      {hasPasswordless && (
        <Switch
          id="include-passwordless"
          className="pf-v5-u-mt-md pf-v5-u-font-size-sm"
          label={
            <span style={{ position: "relative", top: 2 }}>
              {t("includePasswordlessOrder")}
            </span>
          }
          isChecked={includePasswordless}
          onChange={(_e, checked) => {
            setIncludePasswordless(checked);
            localStorage.setItem("includePasswordlessOrder", String(checked));
          }}
          data-testid="include-passwordless-switch"
        />
      )}
    </PageSection>
  );
};
