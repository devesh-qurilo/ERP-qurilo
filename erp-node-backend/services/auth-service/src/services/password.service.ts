import bcrypt from "bcryptjs";

export interface PasswordService {
  hash(password: string): Promise<string>;
  compare(rawPassword: string, hashedPassword: string): Promise<boolean>;
  needsUpgrade(storedPassword: string): boolean;
}

export class BcryptPasswordService implements PasswordService {
  async hash(password: string): Promise<string> {
    return bcrypt.hash(password, 10);
  }

  async compare(rawPassword: string, hashedPassword: string): Promise<boolean> {
    if (!this.isBcryptHash(hashedPassword)) {
      return hashedPassword === `plain:${rawPassword}` || hashedPassword === rawPassword;
    }

    return bcrypt.compare(rawPassword, hashedPassword);
  }

  needsUpgrade(storedPassword: string): boolean {
    return !this.isBcryptHash(storedPassword);
  }

  private isBcryptHash(value: string): boolean {
    return /^\$2[aby]\$\d{2}\$/.test(value);
  }
}
