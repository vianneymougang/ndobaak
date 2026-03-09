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
  <meta name="color-scheme" content="light">
  <meta name="supported-color-schemes" content="light">
  <title>R\u00e9initialisation de mot de passe \u2014 Ndoba\u00e2k</title>
  <link href="https://fonts.googleapis.com/css2?family=Urbanist:wght@400;500;600;700&display=swap" rel="stylesheet" type="text/css">
</head>
<body style="margin:0;padding:0;background-color:#F0EDEA;font-family:'Helvetica Neue',Arial,sans-serif;-webkit-font-smoothing:antialiased;">

  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F0EDEA;padding:48px 16px;">
    <tr>
      <td align="center">

        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:580px;">

          <!-- Gold top accent -->
          <tr>
            <td style="height:3px;background:#D4A017;border-radius:3px 3px 0 0;"></td>
          </tr>

          <!-- Card -->
          <tr>
            <td style="background:#ffffff;border-radius:0 0 12px 12px;box-shadow:0 4px 24px rgba(27,67,50,0.08);">

              <!-- Wordmark -->
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:36px 48px 32px;">
                    <table role="presentation" cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="vertical-align:middle;padding-right:10px;">
                          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                            <path d="M3 10.5L12 3L21 10.5V21H15.5V15H8.5V21H3V10.5Z" fill="#1B4332"/>
                            <path d="M8.5 21V15H15.5V21" stroke="#D4A017" stroke-width="1.5" stroke-linejoin="round"/>
                          </svg>
                        </td>
                        <td style="vertical-align:middle;">
                          <span style="font-family:'Urbanist','Helvetica Neue',Arial,sans-serif;font-size:19px;font-weight:700;color:#1B4332;letter-spacing:-0.2px;">Ndoba\u00e2k</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>

              <!-- Divider -->
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:0 48px;">
                    <div style="height:1px;background-color:#E5E0D8;"></div>
                  </td>
                </tr>
              </table>

              <!-- Main content -->
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:44px 48px 0;">

                    <h1 style="margin:0 0 20px;font-family:'Urbanist','Helvetica Neue',Arial,sans-serif;font-size:26px;font-weight:700;color:#1B4332;line-height:1.25;letter-spacing:-0.4px;">
                      R\u00e9initialisez<br>votre mot de passe
                    </h1>

                    <p style="margin:0 0 12px;font-size:15px;color:#374151;line-height:1.75;">
                      Bonjour&#160;<strong style="color:#1B4332;font-weight:600;">%s</strong>,
                    </p>

                    <p style="margin:0 0 8px;font-size:15px;color:#374151;line-height:1.75;">
                      Vous avez demand\u00e9 la r\u00e9initialisation du mot de passe associ\u00e9 \u00e0 votre compte Ndoba\u00e2k.
                    </p>

                    <p style="margin:0 0 36px;font-size:15px;color:#374151;line-height:1.75;">
                      Cliquez sur le bouton ci-dessous pour en d\u00e9finir un nouveau. Ce lien expire dans&#160;<strong style="color:#1B4332;">30&#160;minutes</strong>.
                    </p>

                    <!-- CTA -->
                    <table role="presentation" cellpadding="0" cellspacing="0" style="margin-bottom:32px;">
                      <tr>
                        <td style="border-radius:6px;background-color:#D4A017;">
                          <a href="%s"
                             style="display:inline-block;padding:14px 30px;font-family:'Urbanist','Helvetica Neue',Arial,sans-serif;font-size:15px;font-weight:700;color:#1B4332;text-decoration:none;letter-spacing:0.01em;white-space:nowrap;">
                            R\u00e9initialiser mon mot de passe
                          </a>
                        </td>
                      </tr>
                    </table>

                    <!-- Fallback link -->
                    <p style="margin:0 0 44px;font-size:12px;color:#9CA3AF;line-height:1.75;">
                      Le bouton ne s\u2019affiche pas\u00a0? Copiez ce lien dans votre navigateur&nbsp;:<br>
                      <a href="%s" style="color:#B8860B;text-decoration:underline;word-break:break-all;">%s</a>
                    </p>

                  </td>
                </tr>
              </table>

              <!-- Security notice -->
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:0 48px;">
                    <div style="height:1px;background-color:#E5E0D8;"></div>
                  </td>
                </tr>
                <tr>
                  <td style="padding:24px 48px 40px;">
                    <p style="margin:0;font-size:13px;color:#6B7280;line-height:1.75;">
                      <strong style="color:#374151;font-weight:600;">Vous n\u2019avez pas fait cette demande\u00a0?</strong><br>
                      Ignorez simplement cet email. Votre mot de passe restera inchang\u00e9 et votre compte reste s\u00e9curis\u00e9.
                    </p>
                  </td>
                </tr>
              </table>

            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="padding:24px 48px 8px;">
              <p style="margin:0;font-size:11px;color:#9CA3AF;text-align:center;line-height:1.8;letter-spacing:0.01em;">
                &copy; 2026 Ndoba\u00e2k &nbsp;&mdash;&nbsp; Logement \u00e9tudiant \u00e0 Yaound\u00e9, Cameroun<br>
                Email envoy\u00e9 \u00e0&#160;%s
              </p>
            </td>
          </tr>

        </table>

      </td>
    </tr>
  </table>

</body>
</html>
""".formatted(name, link, link, link, toEmail);
    }
}
