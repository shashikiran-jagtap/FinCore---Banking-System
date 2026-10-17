package Banking_System.Ui;

import Banking_System.BankService;

import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.function.Consumer;


public class App {
    static final BankService bank = new BankService();

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    static <T> void async(Callable<T> work, Consumer<T> onOk, Component parent) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try { onOk.accept(get()); }
                catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(parent, c.getMessage(),
                            c instanceof ArithmeticException ? "Cannot complete" : "System error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    static BigDecimal parse(String s) throws ArithmeticException {
        try { return new BigDecimal(s.trim()); }
        catch (RuntimeException e) { throw new ArithmeticException("Not a valid amount."); }
    }

    static String money(BigDecimal b) { return String.format("$%,.2f", b); }
}
