package com.ndobaak.backend.rentalcycle.service;

import com.lowagie.text.pdf.BaseFont;
import com.ndobaak.backend.rentalcycle.dto.LeaseContractPdfData;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Component
public class ContractPdfGenerator {

    private final TemplateEngine templateEngine;

    public ContractPdfGenerator() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public byte[] generate(LeaseContractPdfData data) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("contract", data);
        String html = templateEngine.process("lease-contract", ctx);

        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.getFontResolver().addFont(
                "classpath:/fonts/DejaVuSans.ttf",
                BaseFont.IDENTITY_H,
                true
            );
            renderer.setDocumentFromString(html);
            renderer.layout();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du contrat PDF.", e);
        }
    }
}
