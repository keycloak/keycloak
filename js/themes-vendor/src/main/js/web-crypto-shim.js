import { sha256, sha384 } from "@noble/hashes/sha2.js";

// Shim for Web Crypto API specifically for Keycloak JS, as this API can sometimes be missing, for example in an insecure context:
// https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts
// Since we have decided to support insecure contexts, we (sadly) need to provide a fallback for the Web Crypto API.
if (typeof crypto === "undefined") {
  globalThis.crypto = {};
}

if (typeof crypto.subtle === "undefined") {
  Object.defineProperty(crypto, "subtle", {
    value: {
      digest: async (algorithm, data) => {
        if (algorithm === "SHA-256") {
          return sha256(data);
        }

        if (algorithm === "SHA-384") {
          return sha384(data);
        }

        throw new Error("Unsupported algorithm");
      },
    },
  });
}

if (typeof crypto.getRandomValues === "undefined") {
  Object.defineProperty(crypto, "getRandomValues", {
    value: (array) => {
      for (let i = 0; i < array.length; i++) {
        array[i] = Math.floor(Math.random() * 256);
      }

      return array;
    },
  });
}

if (typeof crypto.randomUUID === "undefined") {
  Object.defineProperty(crypto, "randomUUID", {
    value: () => {
      const arr = new Uint8Array(16);
      crypto.getRandomValues(arr);

      // UUID version (4) and variant (1: 10xx)
      arr[6] = (arr[6] & 0x0f) | 0x40; // bits 12-15 are 0100
      arr[8] = (arr[8] & 0x3f) | 0x80; // bits 14-15 are 10

      // Convert to hex string
      return Array.from(arr, (v) => v.toString(16).padStart(2, "0"))
        .join("")
        .replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/, "$1-$2-$3-$4-$5");
    },
  });
}
