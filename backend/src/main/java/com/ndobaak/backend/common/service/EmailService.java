package com.ndobaak.backend.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom("noreply@ndobaak.com", "Ndobaâk");
            helper.setSubject("Ndobaâk \u2014 R\u00e9initialisation de votre mot de passe");
            helper.setText(buildPasswordResetHtml(recipientName, resetLink, toEmail), true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildPasswordResetHtml(String name, String link, String toEmail) {
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
  <title>R\u00e9initialisation de mot de passe</title>
</head>
<body style="margin:0;padding:0;background-color:#F5F7FA;font-family:'Helvetica Neue',Arial,sans-serif;-webkit-font-smoothing:antialiased;">

  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F7FA;padding:48px 16px;">
    <tr>
      <td align="center">

        <!-- Card -->
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(27,58,92,0.10);">

          <!-- ── Header ── -->
          <tr>
            <td style="background:linear-gradient(135deg,#1B3A5C 0%%,#2D5F8A 58%%,#1A7A5C 100%%);padding:36px 40px 32px;">
              <table role="presentation" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="width:44px;height:44px;background:#E8762B;border-radius:12px;text-align:center;vertical-align:middle;font-size:22px;line-height:44px;">
                    🏠
                  </td>
                  <td style="padding-left:14px;vertical-align:middle;">
                    <span style="font-family:'Poppins','Helvetica Neue',Arial,sans-serif;font-size:24px;font-weight:700;color:#ffffff;letter-spacing:-0.3px;">Ndoba\u00e2k</span>
                  </td>
                </tr>
              </table>
              <p style="margin:20px 0 0;font-size:13px;color:rgba(255,255,255,0.60);line-height:1.5;">
                La r\u00e9f\u00e9rence du logement \u00e9tudiant \u00e0 Yaound\u00e9
              </p>
            </td>
          </tr>

          <!-- ── Divider accent ── -->
          <tr>
            <td style="height:4px;background:linear-gradient(90deg,#E8762B 0%%,#C9611B 100%%);"></td>
          </tr>

          <!-- ── Body ── -->
          <tr>
            <td style="padding:40px 40px 32px;">

              <h1 style="margin:0 0 6px;font-family:'Poppins','Helvetica Neue',Arial,sans-serif;font-size:22px;font-weight:700;color:#1B3A5C;line-height:1.3;">
                R\u00e9initialisation de votre mot de passe
              </h1>
              <p style="margin:0 0 28px;font-size:14px;color:#6C757D;">
                Bonjour <strong style="color:#1B3A5C;">%s</strong>,
              </p>

              <p style="margin:0 0 28px;font-size:15px;color:#2C3E50;line-height:1.7;">
                Vous avez demand\u00e9 la r\u00e9initialisation de votre mot de passe pour votre compte Ndoba\u00e2k.
                Cliquez sur le bouton ci-dessous pour d\u00e9finir un nouveau mot de passe.
              </p>

              <!-- CTA -->
              <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 32px;">
                <tr>
                  <td style="border-radius:10px;background:#E8762B;box-shadow:0 4px 16px rgba(232,118,43,0.35);">
                    <a href="%s"
                       style="display:inline-block;padding:15px 36px;font-family:'Poppins','Helvetica Neue',Arial,sans-serif;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;letter-spacing:0.1px;">
                      R\u00e9initialiser mon mot de passe
                    </a>
                  </td>
                </tr>
              </table>

              <!-- Fallback link -->
              <p style="margin:0 0 28px;font-size:12px;color:#6C757D;line-height:1.6;">
                Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur&nbsp;:<br>
                <a href="%s" style="color:#E8762B;word-break:break-all;font-size:11px;">%s</a>
              </p>

              <!-- Info box -->
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#FFF3E0;border-radius:10px;border-left:4px solid #E8762B;">
                <tr>
                  <td style="padding:16px 20px;">
                    <p style="margin:0 0 6px;font-size:13px;font-weight:600;color:#E8762B;">
                      &#9201;&nbsp; Ce lien expire dans <strong>30 minutes</strong>
                    </p>
                    <p style="margin:0;font-size:13px;color:#2C3E50;line-height:1.5;">
                      Apr\u00e8s expiration, vous devrez soumettre une nouvelle demande de r\u00e9initialisation.
                    </p>
                  </td>
                </tr>
              </table>

            </td>
          </tr>

          <!-- ── Security notice ── -->
          <tr>
            <td style="padding:0 40px 40px;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-top:1px solid #DEE2E6;">
                <tr>
                  <td style="padding-top:24px;">
                    <p style="margin:0;font-size:13px;color:#6C757D;line-height:1.6;">
                      &#128274;&nbsp; <strong style="color:#1B3A5C;">Vous n'avez pas fait cette demande\u00a0?</strong><br>
                      Ignorez simplement cet email. Votre mot de passe restera inchang\u00e9 et votre compte reste s\u00e9curis\u00e9.
                    </p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- ── Footer ── -->
          <tr>
            <td style="background:#F5F7FA;padding:20px 40px;border-top:1px solid #DEE2E6;">
              <p style="margin:0;font-size:12px;color:#6C757D;text-align:center;line-height:1.6;">
                &copy; 2026 <strong>Ndoba\u00e2k</strong> &mdash; Logement \u00e9tudiant \u00e0 Yaound\u00e9, Cameroun<br>
                <span style="color:#DEE2E6;">|</span>
                Cet email a \u00e9t\u00e9 envoy\u00e9 \u00e0 %s
              </p>
            </td>
          </tr>

        </table>
        <!-- /Card -->

      </td>
    </tr>
  </table>

</body>
</html>
""".formatted(name, link, link, link, toEmail);
    }
}
