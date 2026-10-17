package Banking_System.Ui;

import Banking_System.BankService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;


class AdminFrame extends JFrame {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Account", "Customer", "Email", "Balance", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel totals = new JLabel(" ");
    private List<BankService.AdminRow> rows = List.of();

    AdminFrame() {
        super("finCore Bank - Admin console");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        totals.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        totals.setFont(totals.getFont().deriveFont(Font.BOLD, 14f));

        JButton toggle = new JButton("Freeze / Unfreeze selected");
        toggle.addActionListener(e -> toggle());
        JButton reload = new JButton("Refresh");
        reload.addActionListener(e -> refresh());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        bar.add(toggle); bar.add(reload);

        add(totals, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);
        setSize(820, 480);
        setLocationRelativeTo(null);
        refresh();
    }

    private void refresh() {
        App.async(() -> App.bank.allAccounts(), r -> {
            rows = r;
            model.setRowCount(0);
            BigDecimal sum = BigDecimal.ZERO;
            for (var a : r) {
                model.addRow(new Object[]{a.accountNo(), a.name(), a.email(), App.money(a.balance()), a.status()});
                sum = sum.add(a.balance());
            }
            totals.setText(r.size() + " accounts   |   Total deposits held: " + App.money(sum));
        }, this);
    }

    private void toggle() {
        int i = table.getSelectedRow();
        if (i < 0) { JOptionPane.showMessageDialog(this, "Select an account first."); return; }
        var a = rows.get(table.convertRowIndexToModel(i));
        boolean freeze = a.status().equals("ACTIVE");
        if (JOptionPane.showConfirmDialog(this, (freeze ? "Freeze " : "Unfreeze ") + a.accountNo() + " (" + a.name() + ")?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        App.async(() -> { App.bank.setFrozen(a.accountId(), freeze); return true; }, x -> refresh(), this);
    }
}
