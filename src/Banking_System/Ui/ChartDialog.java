package Banking_System.Ui;

import Banking_System.BankService;

import javax.swing.*;
import java.awt.*;
import java.util.List;


class ChartDialog extends JDialog {
    private List<BankService.Monthly> data = List.of();

    ChartDialog(Frame owner, long accountId) {
        super(owner, "Income vs spending - last 6 months", true);
        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), left = 70, right = 20, top = 40, bottom = 40;
                if (data.isEmpty()) { g.drawString("No transactions in this period.", left, h / 2); g.dispose(); return; }
                double max = 1;
                for (var m : data) max = Math.max(max, Math.max(m.income().doubleValue(), m.spent().doubleValue()));
                int ph = h - top - bottom, pw = w - left - right;
                for (int i = 0; i <= 4; i++) {
                    int y = top + ph - ph * i / 4;
                    g.setColor(new Color(225, 225, 225)); g.drawLine(left, y, w - right, y);
                    g.setColor(Color.DARK_GRAY); g.drawString(String.format("$%,.0f", max * i / 4), 6, y + 4);
                }
                int slot = pw / data.size(), bw = Math.max(10, slot / 4);
                FontMetrics fm = g.getFontMetrics();
                for (int i = 0; i < data.size(); i++) {
                    var m = data.get(i);
                    int x0 = left + i * slot + slot / 2 - bw;
                    int hi = (int) (ph * m.income().doubleValue() / max), hs = (int) (ph * m.spent().doubleValue() / max);
                    g.setColor(new Color(40, 160, 80)); g.fillRect(x0, top + ph - hi, bw, hi);
                    g.setColor(new Color(200, 60, 60)); g.fillRect(x0 + bw, top + ph - hs, bw, hs);
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(m.month(), left + i * slot + slot / 2 - fm.stringWidth(m.month()) / 2, h - bottom + 18);
                }
                g.setColor(new Color(40, 160, 80)); g.fillRect(left, 12, 12, 12); g.setColor(Color.DARK_GRAY); g.drawString("Income", left + 18, 23);
                g.setColor(new Color(200, 60, 60)); g.fillRect(left + 90, 12, 12, 12); g.setColor(Color.DARK_GRAY); g.drawString("Spending", left + 108, 23);
                g.dispose();
            }
        };
        add(chart);
        setSize(640, 400);
        setLocationRelativeTo(owner);
        App.async(() -> App.bank.monthlySummary(accountId, 6), d -> { data = d; chart.repaint(); }, this);
    }
}
