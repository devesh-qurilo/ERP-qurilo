import { randomUUID } from "node:crypto";

let tempCounter = 1;

export function generateTempEmployeeId(): string {
  return `TEMP-${String(tempCounter++).padStart(4, "0")}`;
}

export function generateFinalEmployeeId(): string {
  return `EMP-${randomUUID().slice(0, 8).toUpperCase()}`;
}
