package joshuajungen.shoppinghelper;

import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.IPickerResult;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;
import com.microsoft.onedrivesdk.saver.SaverException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import excel.ExcelExpenseHandler;
import expense.Expense;
import expense.ExpenseType;
import expense.PaidBy;



public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener{

    private TextView tvSelectedFile, tvDate, tvAmount;
    private AutoCompleteTextView tvPurpose;
    private Spinner spinnerExpenseType, spinnerPaidBy;
    private Button btnExpense;
    private ImageButton imgbtnCalendar, imgbtnFilePicker;

    private Calendar calendar;
    private IPicker oneDrivePicker;
    private ISaver oneDriveSaver;
    private File appDir;
    private File selectedExcelFile;

    /* *********************************************************************************************
        Lifecycle Methods
    ********************************************************************************************* */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        appDir = Environment.getExternalStoragePublicDirectory(getResources().getString(R.string.dir_name));
        clearAppDir();
        registerReceiver(onCompleteDownload, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        calendar = Calendar.getInstance();

        tvSelectedFile = (TextView) findViewById(R.id.tvSelectedFile);

        tvDate = (TextView) findViewById(R.id.tvDate);
        if (tvDate != null) {
            tvDate.setText(getDateAsString(calendar));
        }

        tvAmount = (TextView) findViewById(R.id.tvAmount);
        tvPurpose = (AutoCompleteTextView) findViewById(R.id.tvPurpose);
        tvPurpose.setAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, getResources().getStringArray(R.array.autocomplete_shop_item)));

        spinnerExpenseType = (Spinner) findViewById(R.id.spinnerExpenseType);
        if (spinnerExpenseType != null) {
            spinnerExpenseType.setAdapter(new ArrayAdapter<ExpenseType>(this, R.layout.support_simple_spinner_dropdown_item, ExpenseType.values()));
        }

        spinnerPaidBy = (Spinner) findViewById(R.id.spinnerPaidBy);
        if (spinnerPaidBy != null) {
            spinnerPaidBy.setAdapter(new ArrayAdapter<PaidBy>(this, R.layout.support_simple_spinner_dropdown_item, PaidBy.values()));
            SharedPreferences pref = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
            int index = pref.getInt("lastPaidBy", 0);
            spinnerPaidBy.setSelection(index);
        }
        spinnerPaidBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences pref = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                pref.edit().putInt("lastPaidBy", spinnerPaidBy.getSelectedItemPosition()).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnExpense = (Button) findViewById(R.id.btnExpense);
        btnExpense.setEnabled(false);

        imgbtnCalendar = (ImageButton) findViewById(R.id.imgbtnCalendar);
    }

    @Override
    protected void onResume() {
        SharedPreferences pref = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        int index = pref.getInt("lastPaidBy", 0);
        spinnerPaidBy.setSelection(index);
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        tidyUp();
        unregisterReceiver(onCompleteDownload);
        super.onDestroy();
    }

    private Long downloadID = null;

    private BroadcastReceiver onCompleteDownload = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
                if (downloadID != null) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadID);
                    Cursor c = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).query(q);
                    if (c.moveToFirst()){
                        int colSucces = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(colSucces)){
                            // succes
                            lockWriting = false;
                            ProgressBar pbDownload = (ProgressBar) findViewById(R.id.pbDownloading);
                            if (pbDownload != null)
                                pbDownload.setVisibility(View.INVISIBLE);
                            btnExpense.setEnabled(true);
                            return;
                        }
                    }
                }
            } else {
                Log.e("#DOWNLOAD MANAGER", "Download failed");
                Toast.makeText(getApplicationContext(), "Download failed", Toast.LENGTH_SHORT).show();
            }

        }
    };

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null){
            String strDate = savedInstanceState.getString("" + tvDate.getId(), null);
            if (strDate != null)
                tvDate.setText(strDate);

            int iExpPos = savedInstanceState.getInt("" + spinnerExpenseType.getId(), -1);
            if (iExpPos != -1)
                spinnerExpenseType.setSelection(iExpPos);

            String strPurpose = savedInstanceState.getString("" + tvPurpose.getId(), null);
            if (strPurpose != null)
                tvPurpose.setText(strPurpose);

            String strAmount = savedInstanceState.getString("" + tvAmount.getId(), null);
            if (strAmount != null)
                tvAmount.setText(strAmount);

            int iPaidPos = savedInstanceState.getInt("" + spinnerPaidBy.getId(), -1);
            if (iPaidPos != -1)
                spinnerPaidBy.setSelection(iPaidPos);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("" + tvDate.getId(), tvDate.getText().toString());
        outState.putInt("" + spinnerExpenseType.getId(), spinnerExpenseType.getSelectedItemPosition());
        outState.putString("" + tvPurpose.getId(), tvPurpose.getText().toString());
        outState.putString("" + tvAmount.getId(), tvAmount.getText().toString());
        outState.putInt("" + spinnerPaidBy.getId(), spinnerPaidBy.getSelectedItemPosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* *********************************************************************************************
        Action Listener Methods
    ********************************************************************************************* */

    public void imgbtnCalendar_OnClick(View v) {
        Toast.makeText(getApplicationContext(), "Calendar", Toast.LENGTH_SHORT).show();

        DatePickerDialog datePicker = new DatePickerDialog(this, this,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    public void btnExpense_OnClick(View v) {
        if (validateUserInput()) {
            ExpenseType type = (ExpenseType) spinnerExpenseType.getSelectedItem();
            PaidBy paidBy = (PaidBy) spinnerPaidBy.getSelectedItem();
            double amount = Double.parseDouble((String) tvAmount.getText().toString());
            final Expense exp = new Expense(calendar, type, tvPurpose.getText().toString(), amount, paidBy);

            if (tvSelectedFile.getText().length() > 0) {
                writeExpense(exp);
            } else {
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void imgbtnFilePicker_OnClick(View v) {
        startPicking();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(year, monthOfYear, dayOfMonth);
        if (tvDate != null) {
            tvDate.setText(getDateAsString(calendar));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IPickerResult res = null;
        if (oneDrivePicker != null) { // retrieve selection from oneDrivePicker
            res = oneDrivePicker.getPickerResult(requestCode, resultCode, data);
            oneDrivePicker = null;
        }

        if (oneDriveSaver != null) {
            try {
                if (oneDriveSaver.handleSave(requestCode, resultCode, data)) {
                    tidyUp();
                } else {
                    Toast.makeText(this, "Could not save file", Toast.LENGTH_SHORT).show();
                }
            } catch (SaverException e) {
                Log.e("#ONEDRIVE", "Could not save file: " + e.getMessage());
                Toast.makeText(this, "Could not save file", Toast.LENGTH_LONG).show();
            } finally {
                oneDriveSaver = null;
            }
        }

        if (res != null) {
            downloadFromOneDrive(res.getName(), res.getLink());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /* *********************************************************************************************
        Cloud Methods
    ********************************************************************************************* */

    private void downloadFromOneDrive(String fileName, Uri fileUri) {
        clearAppDir();
        tvSelectedFile.setText(fileName);

        DownloadManager.Request request = new DownloadManager.Request(fileUri);
        request.setDescription("Download selected file from OneDrive");
        request.setTitle("OneDrive File Download");
        request.setDestinationInExternalPublicDir(getResources().getString(R.string.dir_name), fileName);
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadID = manager.enqueue(request);

        ProgressBar pbDownload = (ProgressBar) findViewById(R.id.pbDownloading);
        if (pbDownload != null)
            pbDownload.setVisibility(View.VISIBLE);
    }

    private void startPicking(){
        if (oneDrivePicker == null) {
            oneDrivePicker = Picker.createPicker("000000004018AA0D");
        }
        oneDrivePicker.startPicking(this, LinkType.DownloadLink);
    }

    private void startSaving(){
        if (oneDriveSaver == null){
            oneDriveSaver = Saver.createSaver("000000004018AA0D");
        }
        oneDriveSaver.startSaving(MainActivity.this, selectedExcelFile.getName(), Uri.parse("file://" + selectedExcelFile.getAbsolutePath()));
    }

    /* *********************************************************************************************
        Excel Methods
    ********************************************************************************************* */
    private boolean lockWriting = false;

    public void writeExpense(Expense expense) {
        if (lockWriting) {
            startSaving();
            return;
        }

        File[] files = appDir.listFiles();
        selectedExcelFile = null;
        if (files.length == 1)
            selectedExcelFile = files[0];

        if (selectedExcelFile == null) {
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
            Log.e("#EXCEL", "File not found");
            lockWriting = false;
        }

        ExcelExpenseHandler eeHandler = new ExcelExpenseHandler(selectedExcelFile, expense);
        ExcelBackgroundWorker worker = new ExcelBackgroundWorker();
        worker.execute(eeHandler);
    }

    private class ExcelBackgroundWorker extends AsyncTask<ExcelExpenseHandler, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar pb = (ProgressBar) findViewById(R.id.pbWriting);
            if (pb != null)
                pb.setVisibility(View.VISIBLE);
        }

        private Boolean res = null;
        @Override
        protected Boolean doInBackground(ExcelExpenseHandler... params) {
            lockWriting = true;
            res = true;
            if (params.length == 1)
                try {
                    params[0].write();
                    startSaving();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Could not write to file", Toast.LENGTH_LONG);
                    Log.e("#EXCEL", "Could not write to file");
                    res = false;
                }
            lockWriting = res;
            return res;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            ProgressBar pb = (ProgressBar) findViewById(R.id.pbWriting);
            if (pb != null)
                pb.setVisibility(View.INVISIBLE);
        }
    }

    /* *********************************************************************************************
        Aux. Methods
    ********************************************************************************************* */

    private void clearAppDir(){
        if (appDir.isDirectory()){ // clear directory
            for(File f: appDir.listFiles())
                f.delete();
        }
    }

    private String getDateAsString(Calendar c) {
        SimpleDateFormat dFormat = new SimpleDateFormat("dd.MM.yyyy");
        String strDate = dFormat.format(c.getTime());
        return strDate;
    }

    private boolean validateUserInput() {
        Calendar c = Calendar.getInstance();
        boolean valid = true;
        String info = "Check your input";
        if (c.get(Calendar.MONTH) != calendar.get(Calendar.MONTH)) {
            info = getResources().getString(R.string.checkDate);
            valid = false;
        } else if (tvPurpose.getText().length() == 0) {
            info = getResources().getString(R.string.checkPurpose);
            valid = false;
        } else if (tvAmount.getText().length() == 0) {
            info = getResources().getString(R.string.checkAmount_Empty);
            valid = false;
        } else {
            try {
                double amount = Double.parseDouble((String) tvAmount.getText().toString());
            } catch (NumberFormatException e) {
                info = getResources().getString(R.string.checkAmount_Invalid);
                valid = false;
            }
        }

        if (!valid) {
            Toast.makeText(this, info, Toast.LENGTH_LONG).show();
        }

        return valid;
    }

    private void tidyUp(){
        lockWriting = false;
        selectedExcelFile = null;
        oneDriveSaver = null;
        oneDrivePicker = null;

        clearAppDir();

        btnExpense.setEnabled(false);
        tvSelectedFile.setText("");
        tvDate.setText(getDateAsString(Calendar.getInstance()));
        spinnerExpenseType.setSelection(0);
        tvPurpose.setText("");
        tvAmount.setText("");
    }


}
