
package com.medicaltranslator;

import android.content.Context;
import android.os.Environment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class PdfExporter {
    public static File exportWithTranslations(Context context, File originalPdfFile,
                                              List<String> translationsPerPage,
                                              List<String> glossaryLines) throws Exception {
        PDDocument original = PDDocument.load(originalPdfFile);
        PDDocument output = new PDDocument();

        PDType0Font fallbackFont = null;
        try {
            InputStream is = context.getAssets().open("fonts/Roboto-Regular.ttf");
            fallbackFont = PDType0Font.load(output, is, true);
        } catch (Exception e) {}

        int numPages = original.getNumberOfPages();

        for (int i = 0; i < numPages; i++) {
            PDPage origPage = original.getPage(i);
            output.addPage(origPage);
            PDPage transPage = new PDPage(PDRectangle.A4);
            output.addPage(transPage);
            String translationText = (i < translationsPerPage.size()) ? translationsPerPage.get(i) : "";
            PDPageContentStream pcs = new PDPageContentStream(output, transPage);
            pcs.beginText();
            if (fallbackFont != null) pcs.setFont(fallbackFont, 12);
            pcs.newLineAtOffset(40, 750);
            String[] lines = translationText.split("\\n");
            for (String line : lines) {
                pcs.showText(line);
                pcs.newLineAtOffset(0, -14);
            }
            pcs.endText();
            pcs.close();
        }

        if (glossaryLines != null && glossaryLines.size() > 0) {
            PDPage glossaryPage = new PDPage(PDRectangle.A4);
            output.addPage(glossaryPage);
            PDPageContentStream pcs = new PDPageContentStream(output, glossaryPage);
            pcs.beginText();
            if (fallbackFont != null) pcs.setFont(fallbackFont, 12);
            pcs.newLineAtOffset(40, 750);
            for (String line : glossaryLines) {
                pcs.showText(line);
                pcs.newLineAtOffset(0, -14);
            }
            pcs.endText();
            pcs.close();
        }

        File outDir = new File(Environment.getExternalStorageDirectory(), "MedicalTranslator/Export");
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "MedicalTranslator_" + System.currentTimeMillis() + ".pdf");
        output.save(outFile);
        output.close();
        original.close();
        return outFile;
    }
}
