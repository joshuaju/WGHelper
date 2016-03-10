package excel;

import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import expense.Expense;
import joshuajungen.shoppinghelper.R;

import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;

/**
 * Created by Joshua Jungen on 06.03.2016.
 */
public class ExcelExpenseHandler {

    private final int ROW_FIRST = 3;

    private final int COL_SHARED_DATE = 1;
    private final int COL_SHARED_PURPOSE = 2;
    private final int COL_SHARED_AMOUNT = 3;
    private final int COL_SHARED_PAID_BY = 4;

    private final int COL_EXTRA_PURPOSE = 5;
    private final int COL_EXTRA_AMOUNT = 6;
    private final int COL_EXTRA_PAID_BY = 7;

    private final File excelFile;
    private final Expense expense;

    private XSSFWorkbook wb;
    private Sheet sheet;


    public ExcelExpenseHandler(File excelFile, Expense expense) {
        this.excelFile = excelFile;
        this.expense = expense;
    }

    public void write() throws IOException {
        initWorkbook();

        int startColIndex = getFirstColumn();
        int endColIndex = startColIndex + getColSpan();
        int rowIndex = getFirstEmptyRow();

        Log.w("#EXCEL", "Write-Parameter [row, firstCol, lastCol] = " + rowIndex + ", " + startColIndex + ", " + endColIndex);

        Row row = sheet.getRow(rowIndex);
        for (int colIndex = startColIndex; colIndex < endColIndex; colIndex++) {
            setValueForColumn(row, colIndex);
        }

        wb.setForceFormulaRecalculation(true);

        OutputStream os = new FileOutputStream(excelFile);
        wb.write(os);
    }

    private void initWorkbook() throws IOException {
        if (wb == null) {
            InputStream is = new FileInputStream(excelFile);
            wb = new XSSFWorkbook(is);
            sheet = wb.getSheetAt(0);
        }
    }

    private void setValueForColumn(Row row, int colIndex) {
        switch (expense.getType()) {
            case SharedExpense:
                setValForCol_SHARED(row, colIndex);
                break;
            case ExtraExpense:
                setValForCol_EXTRA(row, colIndex);
                break;
            default:
                throw new IllegalArgumentException("Did you forgot to implement a new ExpenseType?");
        }
    }

    private void setValForCol_SHARED(Row row, int colIndex) {
        String strVal = null;
        Double dVal = null;
        switch (colIndex) {
            case COL_SHARED_DATE:
                strVal = expense.getDate();
                break;
            case COL_SHARED_PURPOSE:
                strVal = expense.getPurpose();
                break;
            case COL_SHARED_AMOUNT:
                dVal = expense.getAmount();
                break;
            case COL_SHARED_PAID_BY:
                dVal = (double) expense.getPaidBy().getId();
                break;
            default:
                throw new IllegalArgumentException("Index of column is not specified for a shared expense: " + colIndex);
        }

        Cell c = row.getCell(colIndex);
        if (strVal != null){
            if (c == null){
                c = row.createCell(colIndex);
                c.setCellType(Cell.CELL_TYPE_STRING);
            }
            c.setCellValue(strVal);
        } else if (dVal != null) {
            if (c == null){
                c = row.createCell(colIndex);
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
            }
            c.setCellValue(dVal);
        }
    }

    private void setValForCol_EXTRA(Row row, int colIndex) {
        String strVal = null;
        Double dVal = null;
        switch (colIndex) {
            case COL_EXTRA_PURPOSE:
                strVal = expense.getPurpose();
                break;
            case COL_EXTRA_AMOUNT:
                dVal = expense.getAmount();
                break;
            case COL_EXTRA_PAID_BY:
                dVal = (double) expense.getPaidBy().getId();
                break;
            default:
                throw new IllegalArgumentException("Index of column is not specified for an extra expense: " + colIndex);
        }

        Cell c = row.getCell(colIndex);
        if (strVal != null){
            if (c == null){
                c = row.createCell(colIndex);
                c.setCellType(Cell.CELL_TYPE_STRING);
            }
            c.setCellValue(strVal);
        } else if (dVal != null) {
            if (c == null){
                c = row.createCell(colIndex);
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
            }
            c.setCellValue(dVal);
        }
    }

    private int getFirstEmptyRow() {
        int colSpan = getColSpan();
        int startRow = ROW_FIRST;

        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        for (; startRow < sheet.getPhysicalNumberOfRows(); startRow++) {
            Row row = sheet.getRow(startRow);

            boolean allColsEmpty = true;
            for (int colIndex = getFirstColumn(); colIndex < getFirstColumn() + colSpan; colIndex++) {
                String val = getCellAsString(row, colIndex, evaluator);
                if (val != null) {
                    allColsEmpty = false;
                    break;
                }
            }

            if (allColsEmpty) {
                break;
            }
        }

        return startRow;
    }

    private int getFirstColumn() {
        switch (expense.getType()) {
            case SharedExpense:
                return 1;
            case ExtraExpense:
                return 5;
            default:
                throw new IllegalArgumentException("Did you forgot to implement a new ExpenseType?");
        }
    }

    private int getColSpan() {
        switch (expense.getType()) {
            case SharedExpense:
                return 4;
            case ExtraExpense:
                return 3;
            default:
                throw new IllegalArgumentException("Did you forgot to implement a new ExpenseType?");
        }
    }

    protected String getCellAsString(Row row, int c, FormulaEvaluator formulaEvaluator) {
        String value = "";
        try {
            Cell cell = row.getCell(c);
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            switch (cellValue.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    value = "" + cellValue.getBooleanValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    double numericValue = cellValue.getNumberValue();
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        double date = cellValue.getNumberValue();
                        SimpleDateFormat formatter =
                                new SimpleDateFormat("dd/MM/yy");
                        value = formatter.format(HSSFDateUtil.getJavaDate(date));
                    } else {
                        value = "" + numericValue;
                    }
                    break;
                case Cell.CELL_TYPE_STRING:
                    value = "" + cellValue.getStringValue();
                    break;
                default:
            }
        } catch (NullPointerException e) {
            return null;
        }
        return value;
    }

}
