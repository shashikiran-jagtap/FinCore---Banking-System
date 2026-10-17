package Banking_System;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public final class StatementPdf {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private StatementPdf() {}

    public static void write(File file, BankService.Session s, BigDecimal balance, List<BankService.Txn> tx)
            throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        try (OutputStream os = new FileOutputStream(file)) {
            PdfWriter.getInstance(doc, os);
            doc.open();
            doc.add(new Paragraph("FinCore - Account Statement",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(20, 50, 110))));
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            doc.add(new Paragraph("\nCustomer: " + s.name() + "\nAccount: " + s.accountNo()
                    + "\nGenerated: " + LocalDateTime.now().format(FMT)
                    + "\nClosing balance: " + String.format("$%,.2f", balance), body));

            PdfPTable t = new PdfPTable(new float[]{3, 2.5f, 2, 2});
            t.setWidthPercentage(100);
            t.setSpacingBefore(16);
            for (String h : new String[]{"Date", "Type", "Amount", "Balance"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
                c.setBackgroundColor(new Color(20, 50, 110));
                c.setPadding(5);
                t.addCell(c);
            }
            for (var x : tx) {
                boolean in = x.type().equals("DEPOSIT") || x.type().equals("TRANSFER_IN");
                t.addCell(new Phrase(x.at().toLocalDateTime().format(FMT), body));
                t.addCell(new Phrase(x.type(), body));
                t.addCell(new Phrase((in ? "+" : "-") + String.format("$%,.2f", x.amount()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10, in ? new Color(0, 128, 0) : new Color(190, 30, 30))));
                t.addCell(new Phrase(String.format("$%,.2f", x.balanceAfter()), body));
            }
            doc.add(t);
            doc.close();
        }
    }
}
