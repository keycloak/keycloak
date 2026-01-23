import type { TFunction } from "i18next";

export type ExportConfig = {
  filename: string;
  columns: ExportColumn[];
  csvDelimiter?: string;
};

export type ExportColumn = {
  key: string;
  label: string;
};

/**
 * Extracts text content from React elements or returns the value as string
 */
function extractTextContent(value: any): string {
  if (value === null || value === undefined) {
    return "";
  }

  if (typeof value === "string" || typeof value === "number") {
    return String(value);
  }

  if (typeof value === "boolean") {
    return value ? "true" : "false";
  }

  // Handle React elements
  if (value && typeof value === "object") {
    // Check if it's a React element
    if (value.props) {
      // Try to extract children
      if (value.props.children) {
        return extractTextContent(value.props.children);
      }
      // Try to extract href for links
      if (value.props.href) {
        return value.props.href;
      }
      // Try to extract common text properties
      if (value.props.title) {
        return extractTextContent(value.props.title);
      }
    }

    // Handle arrays (multiple children)
    if (Array.isArray(value)) {
      return value.map(extractTextContent).join(" ");
    }

    // Try to convert to string
    if (value.toString && value.toString !== Object.prototype.toString) {
      const str = value.toString();
      if (str !== "[object Object]") {
        return str;
      }
    }
  }

  return "";
}

/**
 * Gets the value from an object using a key path (e.g., "user.name" or "attributes['gui.order']")
 */
function getNestedValue(obj: any, path: string): any {
  // Handle special attribute syntax like "attributes['gui.order']"
  const attributeMatch = path.match(/^(.+)\['(.+)'\]$/);
  if (attributeMatch) {
    const [, basePath, attrKey] = attributeMatch;
    const baseValue = basePath.split(".").reduce((acc, part) => acc?.[part], obj);
    return baseValue?.[attrKey];
  }

  // Handle regular dot notation
  return path.split(".").reduce((acc, part) => acc?.[part], obj);
}

/**
 * Exports data to CSV format
 */
export function exportToCSV<T>(
  data: T[],
  config: ExportConfig,
  t: TFunction,
): void {
  const delimiter = config.csvDelimiter || ";";
  const headers = config.columns.map((col) => col.label).join(delimiter);

  const rows = data.map((item) => {
    return config.columns
      .map((col) => {
        const value = getNestedValue(item, col.key);
        const text = extractTextContent(value);
        // Escape quotes and wrap in quotes if contains delimiter, newline, or quote
        if (
          text.includes(delimiter) ||
          text.includes("\n") ||
          text.includes('"')
        ) {
          return `"${text.replace(/"/g, '""')}"`;
        }
        return text;
      })
      .join(delimiter);
  });

  const csv = [headers, ...rows].join("\n");
  const blob = new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8;" }); // UTF-8 BOM
  downloadBlob(blob, `${config.filename}.csv`);
}

/**
 * Exports data to PDF format
 * Uses dynamic imports to avoid TypeScript errors when dependencies aren't installed
 */
export async function exportToPDF<T>(
  data: T[],
  config: ExportConfig,
  t: TFunction,
): Promise<void> {
  // Dynamically import PDF libraries to avoid TypeScript errors when not installed
  const [{ jsPDF }, autoTable] = await Promise.all([
    import("jspdf"),
    import("jspdf-autotable"),
  ]);

  const doc = new jsPDF.jsPDF({
    orientation: config.columns.length > 5 ? "landscape" : "portrait",
    unit: "mm",
    format: "a4",
  });

  // Add title
  doc.setFontSize(16);
  doc.text(config.filename, 14, 15);

  // Prepare table data
  const headers = config.columns.map((col) => col.label);
  const rows = data.map((item) =>
    config.columns.map((col) => {
      const value = getNestedValue(item, col.key);
      return extractTextContent(value);
    }),
  );

  // Generate table
  autoTable.default(doc, {
    head: [headers],
    body: rows,
    startY: 20,
    styles: {
      fontSize: 8,
      cellPadding: 2,
    },
    headStyles: {
      fillColor: [41, 128, 185],
      textColor: 255,
      fontStyle: "bold",
    },
    alternateRowStyles: {
      fillColor: [245, 245, 245],
    },
    margin: { top: 20, left: 14, right: 14 },
  });

  doc.save(`${config.filename}.pdf`);
}

/**
 * Helper function to download a blob
 */
function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
