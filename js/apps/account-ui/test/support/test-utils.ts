export async function retryOperation<T>(
  operation: () => Promise<T>,
  maxRetries = 15,
  initialDelay = 300,
): Promise<T> {
  let lastError: Error | undefined;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error as Error;

      // Only retry on server errors or network errors, not on validation errors
      const isRetryableError =
        error instanceof Error &&
        (error.message.includes("unknown_error") ||
          error.message.includes("500") ||
          error.message.includes("ECONNREFUSED"));

      if (isRetryableError) {
        const delay = initialDelay * Math.pow(1.5, attempt);
        await new Promise((resolve) => setTimeout(resolve, delay));
        continue;
      }

      // For other errors (validation, 4xx, etc.), throw immediately
      throw error;
    }
  }

  throw lastError;
}

export async function waitForRealmReady(delayMs = 500): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, delayMs));
}
