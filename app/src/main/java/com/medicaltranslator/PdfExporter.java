package com.medicaltranslator;

import android.content.Context;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PdfExporter {

    public static File exportWithTranslation(Context context, File originalPdfFile, List<String> translationPages, List<String> glossaryLines) throws IOException {

        PDDocument original = PDDocument.load(originalPdfFile);
        PDDocument output = new PDDocument();

        PDFont fallbackFont = null;
        try (InputStream is = context.getAssets().open("fonts/Roboto-Regular.ttf")) {
            fallbackFont = PDType0Font.load(output, is, true);
        } catch (IOException e) {
            // Handle font loading error
        }

        for (int i = 0; i < translationPages.size() && i < original.getNumberOfPages(); i++) {
            String translationText = translationPages.get(i);
            PDPage originalPage = original.getPage(i);
            PDPage transPage = new PDPage(PDRectangle.A4);
            output.addPage(transPage);

            String stringTranslationText = i < translationPages.size() ? translationPages.get(i) : "";
            PDPageContentStream pcs = new PDPageContentStream(output, transPage);
            pcs.beginText();
            pcs.setFont(fallbackFont, 12);
            pcs.newLineAtOffset(60, 750);
            String[] lines = translationText.split("\n");
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
            pcs.setFont(fallbackFont, 12);
            pcs.newLineAtOffset(60, 750);
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
