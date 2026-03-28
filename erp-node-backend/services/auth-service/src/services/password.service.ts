import bcrypt from "bcryptjs";

export interface PasswordService {
  hash(password: string): Promise<string>;
  compare(rawPassword: string, hashedPassword: string): Promise<boolean>;
}

export class BcryptPasswordService implements PasswordService {
  async hash(password: string): Promise<string> {
    return bcrypt.hash(password, 10);
  }

  async compare(rawPassword: string, hashedPassword: string): Promise<boolean> {
    return bcrypt.compare(rawPassword, hashedPassword);
  }
}
