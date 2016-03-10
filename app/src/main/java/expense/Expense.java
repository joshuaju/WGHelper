package expense;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Joshua Jungen on 05.03.2016.
 */
public class Expense {

    private final Calendar calendar;
    private final ExpenseType type;
    private final String purpose;
    private final double amount;
    private final PaidBy paidBy;

    public Expense(Calendar calendar, ExpenseType type, String purpose, double amount, PaidBy paidBy) {
        this.calendar = calendar;
        this.type = type;
        this.purpose = purpose;
        this.amount = amount;
        this.paidBy = paidBy;
    }

    public String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String strDate = dateFormat.format(calendar.getTime());
        return strDate;
    }

    public ExpenseType getType() {
        return type;
    }

    public String getPurpose() {
        return purpose;
    }

    public double getAmount() {
        double val = amount*100;
        val = (double) ((int) val);
        val = val / 100.;
        return val;
    }

    public PaidBy getPaidBy() {
        return paidBy;
    }

    @Override
    public String toString() {

        return "{ " + getDate() + ", " +
                getType().name() + ", " +
                getPurpose() + ", " +
                getAmount() + ", " +
                getPaidBy().name() + " }";
    }
}
