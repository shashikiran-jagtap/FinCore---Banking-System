package Banking_System.Ui;

import javax.swing.*;
import java.awt.*;

class LoginFrame extends JFrame {

    LoginFrame() {
        super("FinCore Bank");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JLabel title = new JLabel("JAVA BANK", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(new Color(20, 50, 110));
        title.setBorder(BorderFactory.createEmptyBorder(18, 0, 6, 0));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sign in", signIn());
        tabs.addTab("Open account", register());
        add(title, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setSize(440, 420);
        setLocationRelativeTo(null);
    }

    private JPanel signIn() {
        JTextField email = new JTextField();
        JPasswordField pw = new JPasswordField();
        JButton go = new JButton("Sign in");
        go.addActionListener(e -> App.async(
                () -> App.bank.login(email.getText(), new String(pw.getPassword())),
                opt -> {
                    if (opt.isPresent()) { new Dashboard(opt.get()).setVisible(true); dispose(); }
                    else JOptionPane.showMessageDialog(this, "Invalid email or password.",
                            "Sign in", JOptionPane.WARNING_MESSAGE);
                }, this));
        pw.addActionListener(e -> go.doClick());
        return form(new String[]{"Email", "Password"}, new JComponent[]{email, pw}, go);
    }

    private JPanel register() {
        JTextField name = new JTextField(), email = new JTextField();
        JPasswordField pw = new JPasswordField(), pw2 = new JPasswordField();
        JButton go = new JButton("Open account");
        go.addActionListener(e -> {
            String p = new String(pw.getPassword());
            if (!p.equals(new String(pw2.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.", "Register", JOptionPane.WARNING_MESSAGE);
                return;
            }
            App.async(() -> App.bank.register(name.getText().trim(), email.getText().trim(), p),
                    acc -> {
                        JOptionPane.showMessageDialog(this, "Account opened!\nYour account number: " + acc
                                + "\nYou can now sign in.");
                        name.setText(""); email.setText(""); pw.setText(""); pw2.setText("");
                    }, this);
        });
        return form(new String[]{"Full name", "Email", "Password", "Confirm"},
                new JComponent[]{name, email, pw, pw2}, go);
    }

    private static JPanel form(String[] labels, JComponent[] fields, JButton action) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            g.gridy = i; g.gridx = 0; g.weightx = 0; p.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.weightx = 1; p.add(fields[i], g);
        }
        g.gridy = labels.length; g.gridx = 0; g.gridwidth = 2; g.insets = new Insets(18, 4, 4, 4);
        p.add(action, g);
        return p;
    }
}
