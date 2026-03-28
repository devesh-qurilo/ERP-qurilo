export interface PasswordService {
  hash(password: string): Promise<string>;
  compare(rawPassword: string, hashedPassword: string): Promise<boolean>;
}

export class PlaceholderPasswordService implements PasswordService {
  async hash(password: string): Promise<string> {
    return `plain:${password}`;
  }

  async compare(rawPassword: string, hashedPassword: string): Promise<boolean> {
    return hashedPassword === `plain:${rawPassword}` || hashedPassword === rawPassword;
  }
}

