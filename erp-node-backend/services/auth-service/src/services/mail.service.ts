export interface MailService {
  sendOtpMail(to: string, otp: string): Promise<void>;
  sendAdminNotification(subject: string, bodyText: string): Promise<void>;
}

export class ConsoleMailService implements MailService {
  constructor(private readonly adminEmails: string[]) {}

  async sendOtpMail(to: string, otp: string): Promise<void> {
    console.log(`[mail:otp] to=${to} otp=${otp}`);
  }

  async sendAdminNotification(subject: string, bodyText: string): Promise<void> {
    console.log(`[mail:admin] recipients=${this.adminEmails.join(",")} subject=${subject} body=${bodyText}`);
  }
}

