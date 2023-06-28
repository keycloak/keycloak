type Message = string | number[] | ArrayBuffer | Uint8Array;

interface Hasher {
  /**
   * Update hash
   *
   * @param message The message you want to hash.
   */
  update(message: Message): Hasher;

  /**
   * Return hash in hex string.
   */
  hex(): string;

  /**
   * Return hash in hex string.
   */
  toString(): string;

  /**
   * Return hash in ArrayBuffer.
   */
  arrayBuffer(): ArrayBuffer;

  /**
   * Return hash in integer array.
   */
  digest(): number[];

  /**
   * Return hash in integer array.
   */
  array(): number[];
}

interface Hmac {
  /**
   * Computes a Hash-based message authentication code (HMAC) using a secret key
   *
   * @param secretKey The Secret Key
   * @param message The message you want to hash.
   */
  (secretKey: string, message: Message): string;

  /**
   * Create a hash object using a secret key.
   *
   * @param secretKey The Secret Key
   */
  create(secretKey: string): Hasher;

  /**
   * Create a hash object and hash message using a secret key
   *
   * @param secretKey The Secret Key
   * @param message The message you want to hash.
   */
  update(secretKey: string, message: Message): Hasher;

  /**
   * Return hash in hex string.
   *
   * @param secretKey The Secret Key
   * @param message The message you want to hash.
   */
  hex(secretKey: string, message: Message): string;

  /**
   * Return hash in ArrayBuffer.
   *
   * @param secretKey The Secret Key
   * @param message The message you want to hash.
   */
  arrayBuffer(secretKey: string, message: Message): ArrayBuffer;

  /**
   * Return hash in integer array.
   *
   * @param secretKey The Secret Key
   * @param message The message you want to hash.
   */
  digest(secretKey: string, message: Message): number[];

  /**
   * Return hash in integer array.
   *
   * @param secretKey The Secret Key
   * @param message The message you want to hash.
   */
  array(secretKey: string, message: Message): number[];
}

interface Hash {
  /**
   * Hash and return hex string.
   *
   * @param message The message you want to hash.
   */
  (message: Message): string;

  /**
   * Create a hash object.
   */
  create(): Hasher;

  /**
   * Create a hash object and hash message.
   *
   * @param message The message you want to hash.
   */
  update(message: Message): Hasher;

  /**
   * Return hash in hex string.
   *
   * @param message The message you want to hash.
   */
  hex(message: Message): string;

  /**
   * Return hash in ArrayBuffer.
   *
   * @param message The message you want to hash.
   */
  arrayBuffer(message: Message): ArrayBuffer;

  /**
   * Return hash in integer array.
   *
   * @param message The message you want to hash.
   */
  digest(message: Message): number[];

  /**
   * Return hash in integer array.
   *
   * @param message The message you want to hash.
   */
  array(message: Message): number[];

  /**
   * HMAC interface
   */
  hmac: Hmac;
}

export var sha256: Hash;
export var sha224: Hash;
