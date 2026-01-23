// Type declarations for jspdf modules to avoid TypeScript errors during build
// These modules are loaded dynamically at runtime

declare module "jspdf" {
  export class jsPDF {
    constructor(options?: {
      orientation?: "portrait" | "landscape";
      unit?: "pt" | "px" | "in" | "mm" | "cm" | "ex" | "em" | "pc";
      format?: string | number[];
    });
    setFontSize(size: number): void;
    text(text: string, x: number, y: number): void;
    save(filename: string): void;
  }
}

declare module "jspdf-autotable" {
  import type { jsPDF } from "jspdf";
  
  interface AutoTableOptions {
    head?: string[][];
    body?: string[][];
    startY?: number;
    styles?: {
      fontSize?: number;
      cellPadding?: number;
    };
    headStyles?: {
      fillColor?: number[];
      textColor?: number;
      fontStyle?: string;
    };
    alternateRowStyles?: {
      fillColor?: number[];
    };
    margin?: {
      top?: number;
      left?: number;
      right?: number;
    };
  }
  
  function autoTable(doc: jsPDF, options: AutoTableOptions): void;
  export default autoTable;
}
