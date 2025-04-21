/// <reference types="c3" />

interface Window {
  patternfly: Patternfly;
}

interface PFChartDataItem {
  id: string;
  index: number;
  value: string;
  name: string;
  ratio: number;
}

type PFChartData = PFChartDataItem[];

interface Patternfly {
  pfSetDonutChartTitle(
    selector: Node,
    primary: string,
    secondary: string
  ): void;

  pfDonutTooltipContents(
    data: PFChartData,
    defaultTitleFormat: string,
    defaultValueFormat: string,
    color: (id: number | string) => string
  ): string;

  pfGetUtilizationDonutTooltipContentsFn(
    units: string | number
  ): (data: PFChartData) => string;

  pfGetBarChartTooltipContentsFn(
    categories?: string[]
  ): (data: PFChartData) => string;

  pfSingleLineChartTooltipContentsFn(
    categories?: string[]
  ): (data: PFChartData) => string;

  pfPieTooltipContents: Patternfly['pfDonutTooltipContents'];

  c3ChartDefaults(): {
    getDefaultAreaAxis(): c3.Axis
    getDefaultAreaConfig(): c3.ChartConfiguration;
    getDefaultAreaLegend(): c3.LegendOptions;
    getDefaultAreaPoint(): c3.PointOptions;
    getDefaultBarConfig(categories: string[]): c3.ChartConfiguration;
    getDefaultBarGrid(): c3.Grid;
    getDefaultBarLegend(): c3.LegendOptions;
    getDefaultBarTooltip(categories: string[]): c3.TooltipOptions;
    getDefaultColors(): c3.ChartConfiguration['color'];
    getDefaultDonut(title: string): c3.ChartConfiguration['donut'];
    getDefaultDonutColors(): c3.ChartConfiguration['color'];
    getDefaultDonutConfig(title: string): c3.ChartConfiguration;
    getDefaultDonutLegend(): c3.LegendOptions;
    getDefaultDonutSize(): c3.ChartConfiguration['size'];
    getDefaultDonutTooltip(): c3.TooltipOptions;
    getDefaultGroupedBarConfig(): c3.ChartConfiguration;
    getDefaultGroupedBarGrid(): c3.Grid;
    getDefaultGroupedBarLegend(): c3.LegendOptions;
    getDefaultLineAxis(): c3.Axis;
    getDefaultLineConfig(): c3.ChartConfiguration;
    getDefaultLineGrid(): c3.Grid;
    getDefaultLineLegend(): c3.LegendOptions;
    getDefaultLinePoint(): c3.PointOptions;
    getDefaultPie(): c3.ChartConfiguration['pie'];
    getDefaultPieColors(): c3.ChartConfiguration['color'];
    getDefaultPieConfig(): c3.ChartConfiguration;
    getDefaultPieLegend(): c3.LegendOptions;
    getDefaultPieSize(): c3.ChartConfiguration['size'];
    getDefaultPieTooltip(): c3.TooltipOptions;
    getDefaultRelationshipDonutColors(): c3.ChartConfiguration['color'];
    getDefaultRelationshipDonutConfig(): c3.ChartConfiguration;
    getDefaultSingleAreaConfig(): c3.ChartConfiguration;
    getDefaultSingleAreaTooltip(): c3.TooltipOptions;
    getDefaultSingleLineConfig(): c3.ChartConfiguration;
    getDefaultSingleLineTooltip(): c3.TooltipOptions;
    getDefaultSparklineArea(): c3.ChartConfiguration['area'];
    getDefaultSparklineAxis(): c3.Axis;
    getDefaultSparklineConfig(): c3.ChartConfiguration;
    getDefaultSparklineLegend(): c3.LegendOptions;
    getDefaultSparklinePoint(): c3.PointOptions;
    getDefaultSparklineSize(): c3.ChartConfiguration['size'];
    getDefaultSparklineTooltip(): c3.TooltipOptions;
    getDefaultStackedBarConfig(): c3.ChartConfiguration;
    getDefaultStackedBarGrid(): c3.Grid;
    getDefaultStackedBarLegend(): c3.LegendOptions;
  };
}
