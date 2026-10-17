package Banking_System.Ui;

import Banking_System.BankService;
import Banking_System.StatementPdf;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

class Dashboard extends JFrame {
    private record Snap(BigDecimal bal, List<BankService.Txn> tx) {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private final BankService.Session s;
    private final JLabel balance = new JLabel("…");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Date", "Type", "Amount", "Balance after"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    Dashboard(BankService.Session s) {
        super("FinCore Bank- " + s.name());
        this.s = s;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(new Color(20, 50, 110));
        head.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JLabel who = new JLabel("Welcome, " + s.name() + "   |   Account " + s.accountNo());
        who.setForeground(Color.WHITE);
        JLabel cap = new JLabel("Available balance", SwingConstants.RIGHT);
        cap.setForeground(new Color(190, 205, 235));
        balance.setForeground(Color.WHITE);
        balance.setFont(balance.getFont().deriveFont(Font.BOLD, 30f));
        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        balance.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(cap); right.add(balance);
        head.add(who, BorderLayout.WEST);
        head.add(right, BorderLayout.EAST);

        // History table
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(RIGHT); }
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String type = (String) t.getValueAt(r, 1);
                boolean in = type.equals("DEPOSIT") || type.equals("TRANSFER_IN");
                setForeground(sel ? t.getSelectionForeground() : in ? new Color(0, 128, 0) : new Color(190, 30, 30));
                setForeground(sel ? t.getSelectionForeground() : in ? new Color(0, 128, 0) : new Color(190, 30, 30));
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        // Actions
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        bar.add(button("Deposit", e -> deposit()));
        bar.add(button("Withdraw", e -> withdraw()));
        bar.add(button("Transfer", e -> transfer()));
        bar.add(button("Refresh", e -> refresh()));
        bar.add(button("Statement (PDF)", e -> statement()));
        bar.add(button("Spending chart", e -> new ChartDialog(this, s.accountId()).setVisible(true)));
        if (s.admin()) bar.add(button("Admin", e -> new AdminFrame().setVisible(true)));
        bar.add(button("Sign out", e -> { new LoginFrame().setVisible(true); dispose(); }));

        add(head, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);
        setSize(920, 540);
        setLocationRelativeTo(null);
        refresh();
    }

    private JButton button(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.addActionListener(l);
        return b;
    }

    private void refresh() {
        App.async(() -> new Snap(App.bank.balance(s.accountId()), App.bank.history(s.accountId(), 100)),
                snap -> {
                    balance.setText(App.money(snap.bal()));
                    model.setRowCount(0);
                    for (var t : snap.tx()) {
                        boolean in = t.type().equals("DEPOSIT") || t.type().equals("TRANSFER_IN");
                        model.addRow(new Object[]{
                                t.at().toLocalDateTime().format(FMT), t.type(),
                                (in ? "+" : "-") + App.money(t.amount()), App.money(t.balanceAfter())});
                    }
                }, this);
    }

    private void deposit() {
        String v = JOptionPane.showInputDialog(this, "Deposit amount:", "Deposit", JOptionPane.PLAIN_MESSAGE);
        if (v == null) return;
        App.async(() -> App.bank.deposit(s.accountId(), App.parse(v)), x -> refresh(), this);
    }

    private void withdraw() {
        String v = JOptionPane.showInputDialog(this, "Withdraw amount:", "Withdraw", JOptionPane.PLAIN_MESSAGE);
        if (v == null) return;
        App.async(() -> App.bank.withdraw(s.accountId(), App.parse(v)), x -> refresh(), this);
    }

    private void transfer() {
        JTextField to = new JTextField(), amt = new JTextField();
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"To account number:", to, "Amount:", amt},
                "Transfer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        App.async(() -> App.bank.transfer(s.accountId(), to.getText().trim(), App.parse(amt.getText())),
                x -> { refresh(); JOptionPane.showMessageDialog(this, "Transfer successful."); }, this);
    }

    private void statement() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("statement-" + s.accountNo() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        App.async(() -> {
            StatementPdf.write(f, s, App.bank.balance(s.accountId()), App.bank.history(s.accountId(), 1000));
            return f;
        }, file -> JOptionPane.showMessageDialog(this, "Statement saved:\n" + file.getAbsolutePath()), this);
    }
}
