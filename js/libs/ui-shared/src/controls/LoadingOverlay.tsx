import { Skeleton } from "@patternfly/react-core";
import { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { KeycloakSpinner } from "./KeycloakSpinner";

import style from "./loading-overlay.module.css";

type LoadingOverlayProps = {
  isLoading: boolean;
  children: ReactNode;
  className?: string;
  skeleton?: ReactNode;
  "data-testid"?: string;
};

export const TableLoadingSkeleton = ({ rows = 5 }: { rows?: number }) => {
  const { t } = useTranslation();

  return (
    <>
      <span className="pf-v5-u-screen-reader">{t("spinnerLoading")}</span>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div className={style.skeletonRow} key={rowIndex}>
          <Skeleton className={style.skeletonCellWide} height="2rem" />
          <Skeleton className={style.skeletonCell} height="2rem" />
          <Skeleton className={style.skeletonCell} height="2rem" />
        </div>
      ))}
    </>
  );
};

export const LoadingOverlay = ({
  isLoading,
  children,
  className,
  skeleton,
  "data-testid": dataTestId,
}: LoadingOverlayProps) => {
  return (
    <div
      className={`${style.overlay} ${isLoading ? style.overlayBusy : ""} ${className ?? ""}`}
      aria-busy={isLoading}
      data-testid={dataTestId}
    >
      {children}
      {isLoading && (
        <div className={style.overlaySkeleton} data-testid="loading-spinner">
          {skeleton ? <TableLoadingSkeleton /> : <KeycloakSpinner />}
        </div>
      )}
    </div>
  );
};
