package com.example.testapp;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;



import com.openerp.CreateAsyncTask;
import com.openerp.OpenErpHolder;
import com.openerp.ReadAsyncTask;
import com.openerp.ReadExtraAsyncTask;
import com.openerp.WriteAsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;


public class FormActivity extends FragmentActivity implements
        ReadActivityInterface, OnClickListener, DialogInterface.OnClickListener {
    private final static int SPACING_VERTICAL = 25;
    private final static int SPACING_HORIZONTAL = 10;
    private final static int M2VAL = 1; //For M2M fields
    private final static int INTVAL = 2; //For integer Fields
    private final static int STRVAL = 3; //For Char and Text Fields
    private final static int DOUBLEVAL = 4; //For Float fields
    private final static int BOOLVAL = 5; // For Boolean Fields
    private final static int DOWNLOAD_BUTTON_ID = 6;
    private final static int UPLOAD_BUTTON_ID = 7;
    private final static int CLEAR_BUTTON_ID = 8;
    private final static int BINARY_FIELD_ID = 9;
    private final static int MULT_CONSTANT = 1000;


    private ArrayList<View> mFormViews;
    private ScrollView mSvRecords;
    private LinearLayout mLlRecordframe;
    private LinearLayout mLlMainframe;
    private LinearLayout mLlTopframe;
    private Button mBSave;
    private HashMap<String, Object> mValues;
    private boolean mEditMode;
    private HashMap<String, Object> mValuesToEdit;
    private CreateAsyncTask mCreateTask;
    private String[] mFieldNames;
    private ReadExtraAsyncTask mReadExtraAsyncTask;
    private WriteAsyncTask mWriteTask;
    private HashMap<String, Object> mFieldsAttributes; //Fields OpenERP Attributes


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.initializeVars();
        this.initializeLayout();

    }


    private void initializeVars() {
        this.mValuesToEdit = null;
        this.mFieldNames = OpenErpHolder.getInstance().getmFieldNames();
        Bundle extras = getIntent().getExtras();
        this.mValues = new HashMap<String, Object>();
        if (extras != null) {
            if (extras.containsKey("editRecordId")) {
                this.mValuesToEdit =  OpenErpHolder.getInstance().getmData().get(((Integer) extras.get("editRecordId")));
                this.mEditMode = true;
            } else {
                this.mEditMode = false;
            }

        }
        this.mReadExtraAsyncTask = new ReadExtraAsyncTask(this, this.mValuesToEdit);
        this.mReadExtraAsyncTask.execute("");
    }

    private void initializeLayout() {

        // Layout params definition
        LinearLayout.LayoutParams llpMatch = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams llpWrap = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);

        // Create layouts and apply params
        this.mLlMainframe = new LinearLayout(this);
        this.mLlMainframe.setOrientation(LinearLayout.VERTICAL);

        this.mBSave = new Button(this);
        this.mBSave.setText(R.string.sSave);
        this.mBSave.setOnClickListener(this);

        this.mLlTopframe = new LinearLayout(this);
        this.mLlTopframe.setLayoutParams(llpWrap);
        this.mLlTopframe.setPadding(SPACING_HORIZONTAL, SPACING_VERTICAL, 0, 0);

        this.mSvRecords = new ScrollView(this);

        this.mLlRecordframe = new LinearLayout(this);
        this.mLlRecordframe.setLayoutParams(llpWrap);
        this.mLlRecordframe.setOrientation(LinearLayout.VERTICAL);
        this.mLlRecordframe.setPadding(SPACING_HORIZONTAL, SPACING_VERTICAL, 0, 0);

        // Define view structure
        this.mLlTopframe.addView(mBSave);
        this.mSvRecords.addView(mLlRecordframe);
        this.mLlMainframe.addView(mLlTopframe);
        this.mLlMainframe.addView(mSvRecords);

        // Set content view
        setContentView(this.mLlMainframe, llpMatch);
    }


    /**
     * Get OpenERP Field Type in string
     *
     * @param fieldname Field name string
     * @return field type
     */
    private String getFieldType(String fieldname) {
        HashMap<String, Object> fAttr = (HashMap<String, Object>) this.mFieldsAttributes.get(fieldname);
        return ((String) fAttr.get("type")).toUpperCase(Locale.US);
    }

    /*
     * Read field types to decide the view layout (Called when
     * FieldsGetAsyncTask ends)
     *
     * @see
     * com.example.testapp.FieldsGetActivityInterface#fieldsFeyched(java.util
     * .HashMap)
     */
    @Override
    public void dataFetched() {
        // Draws layout
        // The views arraylist holds the views to retrieve data on Save
        this.mFieldsAttributes = OpenErpHolder.getInstance().getmFieldsDescrip();
        mFormViews = new ArrayList<View>();
        int fieldcount = 0;
        int binfieldscount = 0;
        for (String fieldname : this.mFieldNames) {
            fieldcount++;
            TextView tvLabel = new TextView(this);
            String headerText = (String) ((HashMap<String, Object>) this.mFieldsAttributes.get(fieldname)).get("string");
            tvLabel.setText(headerText);
            tvLabel.setPadding(0, SPACING_VERTICAL, 0, 0);
            mLlRecordframe.addView(tvLabel);
            String ftype = getFieldType(fieldname);
            //Not a function field
            if( !((HashMap<String,Object>)this.mFieldsAttributes.get(fieldname)).containsKey("function"))
                switch (OpenErpHolder.OoType.valueOf(ftype)) {
                    case BOOLEAN:
                        CheckBox cb = new CheckBox(this);
                        cb.setTag(fieldname);
                        mFormViews.add(cb);
                        mLlRecordframe.addView(cb);
                        cb.setId(BOOLVAL * MULT_CONSTANT + fieldcount);
                        if (this.mEditMode) {
                            Boolean checked = (Boolean) this.mValuesToEdit.get(fieldname);
                            cb.setChecked(checked);
                        }
                        cb.setClickable(true);
                        break;
                    case INTEGER:
                        EditText etInt = new EditText(this);
                        etInt.setInputType(InputType.TYPE_CLASS_NUMBER);
                        etInt.setTag(fieldname);
                        etInt.setId(INTVAL * MULT_CONSTANT + fieldcount);
                        mFormViews.add(etInt);
                        mLlRecordframe.addView(etInt);
                        if (this.mEditMode) {
                            if (!(this.mValuesToEdit.get(fieldname) instanceof Boolean)) {
                                etInt.setText(this.mValuesToEdit.get(fieldname).toString());
                            }
                        }
                        break;
                    case FLOAT:
                        EditText etFloat = new EditText(this);
                        etFloat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        etFloat.setTag(fieldname);
                        etFloat.setId(DOUBLEVAL * MULT_CONSTANT + fieldcount);
                        mFormViews.add(etFloat);
                        mLlRecordframe.addView(etFloat);
                        if (this.mEditMode) {
                            if (!(this.mValuesToEdit.get(fieldname) instanceof Boolean)) {
                                etFloat.setText(this.mValuesToEdit.get(fieldname).toString());
                            }
                        }
                        break;
                    case CHAR:
                    case TEXT:
                        EditText etStrfield = new EditText(this);
                        etStrfield.setTag(fieldname);
                        etStrfield.setId(STRVAL * MULT_CONSTANT + fieldcount);
                        mFormViews.add(etStrfield);
                        mLlRecordframe.addView(etStrfield);
                        if (this.mEditMode) {
                            if (!(this.mValuesToEdit.get(fieldname) instanceof Boolean)) {
                                etStrfield.setText((String) this.mValuesToEdit.get(fieldname));
                            }
                        }
                        break;
                    case DATE:
                        TextView tvDate = new TextView(this, null, android.R.attr.spinnerStyle);
                        tvDate.setTag(fieldname);
                        tvDate.setId(STRVAL * MULT_CONSTANT + fieldcount);
                        tvDate.setOnClickListener(this);
                        mFormViews.add(tvDate);
                        mLlRecordframe.addView(tvDate);
                        if (this.mEditMode) {
                            if (!(this.mValuesToEdit.get(fieldname) instanceof Boolean)) {
                                tvDate.setText((String) this.mValuesToEdit.get(fieldname));
                            }
                        }
                        break;
                    case DATETIME:
                        TextView tvDateTime = new TextView(this, null, android.R.attr.spinnerStyle);
                        tvDateTime.setTag(fieldname);
                        tvDateTime.setId(STRVAL * MULT_CONSTANT + fieldcount);
                        tvDateTime.setOnClickListener(this);
                        mFormViews.add(tvDateTime);
                        mLlRecordframe.addView(tvDateTime);
                        if (this.mEditMode) {
                            if (!(this.mValuesToEdit.get(fieldname) instanceof Boolean)) {
                                tvDateTime.setText((String) this.mValuesToEdit.get(fieldname));
                            }
                        }
                        break;
                    case BINARY:
                        Button btDownload = new Button(this);
                        btDownload.setText(R.string.sDownload);
                        btDownload.setId(DOWNLOAD_BUTTON_ID * MULT_CONSTANT + fieldcount);
                        btDownload.setOnClickListener(this);
                        Button btClear = new Button(this);
                        btClear.setText(R.string.sClear);
                        btClear.setId(CLEAR_BUTTON_ID * MULT_CONSTANT + fieldcount);
                        btClear.setOnClickListener(this);
                        Button btUpload = new Button(this);
                        btUpload.setText(R.string.sUpload);
                        btUpload.setId(UPLOAD_BUTTON_ID * MULT_CONSTANT + fieldcount);
                        btUpload.setOnClickListener(this);
                        TextView tvBinFieldShow = new TextView(this);
                        tvBinFieldShow.setId(BINARY_FIELD_ID*MULT_CONSTANT+fieldcount);
                        tvBinFieldShow.setMinimumWidth(100);
                        tvBinFieldShow.setMaxLines(2);
                        btDownload.setEnabled(false);
                        btClear.setEnabled(false);
                        if (this.mEditMode) {
                            Object binfield = this.mReadExtraAsyncTask.getListBinary().get(binfieldscount).get(fieldname);
                            Object binname = this.mReadExtraAsyncTask.getListBinaryNames().get(binfieldscount++).get(fieldname + "_name");
                            if (!(binfield instanceof Boolean)) {
                                byte[] bytes = (((String) binfield).getBytes());
                                this.mValuesToEdit.put(fieldname, bytes);
                                String showText = readableFileSize(bytes.length);
                                if (!(binname instanceof Boolean) && binname != null) {
                                    binname = ((String) binname).replaceAll("[^a-zA-Z0-9-_\\.]", "_");
                                    showText = binname + "\n" + showText;
                                    this.mValuesToEdit.put(fieldname + "_name", binname);
                                }
                                tvBinFieldShow.setText(showText);
                                btDownload.setTag(fieldname);
                                btDownload.setEnabled(true);
                                btClear.setEnabled(true);
                            }
                        }
                        LinearLayout llBinary = new LinearLayout(this);
                        llBinary.setOrientation(LinearLayout.HORIZONTAL);
                        llBinary.addView(tvBinFieldShow);
                        llBinary.addView(btDownload);
                        llBinary.addView(btUpload);
                        llBinary.addView(btClear);

                        mLlRecordframe.addView(llBinary);
                        break;
                    case SELECTION:
                        LinkedList<IdString> slist = new LinkedList<IdString>();
                        Spinner selspinner = new Spinner(this);
                        selspinner.setTag(fieldname);
                        selspinner.setId(STRVAL*MULT_CONSTANT + fieldcount);
                        //Blank list option
                        IdString sdummyIdStr = new IdString("", "");
                        slist.add(sdummyIdStr);
                        int spos = slist.indexOf(sdummyIdStr);
                        selspinner.setSelection(spos); //Set as default
                        //--
                        Object[] selections = (Object[]) ((HashMap<String, Object>)this.mFieldsAttributes.get(fieldname)).get("selection");
                        for(Object obj : selections){
                            slist.add(new IdString((String)((Object[])obj)[0],(String)((Object[])obj)[1]));
                        }
                        ArrayAdapter<IdString> selspinnerArraydAdapter = new ArrayAdapter<IdString>(this, android.R.layout.simple_spinner_dropdown_item,slist);
                        selspinner.setAdapter(selspinnerArraydAdapter);
                        mFormViews.add(selspinner);
                        mLlRecordframe.addView(selspinner);
                        if(this.mEditMode){
                            if(!(this.mValuesToEdit.get(fieldname) instanceof Boolean)){
                                String edSid = (String) this.mValuesToEdit.get(fieldname);
                                String edStr = "";
                                for (Object obj : selections){
                                    if(edSid.equals(((Object[]) obj)[0])){
                                        edStr = (String)((Object[])obj)[1];
                                    }
                                }
                                IdString  edIdStr = new IdString(edSid,edStr);
                                spos = slist.indexOf(edIdStr);
                                selspinner.setSelection(spos);
                            }
                        }
                        break;

                    case MANY2ONE:

                        LinkedList<IdString> manylist = new LinkedList<IdString>();
                        Spinner mspinner = new Spinner(this);
                        mspinner.setTag(fieldname);
                        mspinner.setId(INTVAL*MULT_CONSTANT + fieldcount);
                        //Blank many2list option
                        IdString dummyIdStr = new IdString(-1, "");
                        manylist.add(dummyIdStr);
                        int pos = manylist.indexOf(dummyIdStr);
                        mspinner.setSelection(pos); //Set as default
                        //--
                        for (HashMap<String, Object> record : this.mReadExtraAsyncTask.getMany2DataLists().get(fieldname)) {
                            manylist.add(new IdString((Integer) record.get("id"),
                                    (String) record.get("name")));
                        }
                        ArrayAdapter<IdString> spinnerArrayAdapter = new ArrayAdapter<IdString>(
                                this, android.R.layout.simple_spinner_dropdown_item,
                                manylist);
                        mspinner.setAdapter(spinnerArrayAdapter);
                        mFormViews.add(mspinner);
                        mLlRecordframe.addView(mspinner);
                        if (this.mEditMode) {
                            if (!(this.mValuesToEdit.get(fieldname) instanceof Boolean)) {
                                int edId = (Integer) ((Object[]) this.mValuesToEdit
                                        .get(fieldname))[0];
                                String edStr = ((Object[]) this.mValuesToEdit
                                        .get(fieldname))[1].toString();
                                IdString edIdStr = new IdString(edId, edStr);
                                pos = manylist.indexOf(edIdStr);
                                mspinner.setSelection(pos);
                            }
                        }
                        break;
                    case ONE2ONE:
                        break;
                    case ONE2MANY:
                        break;
                    case MANY2MANY:
                        TextView tvM2m = new TextView(this, null, android.R.attr.spinnerStyle);
                        tvM2m.setId(M2VAL * MULT_CONSTANT + fieldcount);
                        tvM2m.setOnClickListener(this);
                        mFormViews.add(tvM2m);
                        mLlRecordframe.addView(tvM2m);

                        break;
                    case RELATED:
                        break;
                    // TODO Complete field types views
                    default:
                        break;
                }
        }
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /*
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View v) {
        Boolean goodInput = true;

        // Get input data into HashMap<String,Object>
        // TODO check input data (required mFieldNames, bad input...)
        if (v.getId() == this.mBSave.getId()) {
            for (View view : mFormViews) {
                String field = (String) view.getTag();
                switch (view.getId() / MULT_CONSTANT) {
                    case BOOLVAL:
                        Boolean checked = ((CheckBox) view).isChecked();
                        mValues.put(field, checked);
                        break;
                    case INTVAL:
                        if (view instanceof Spinner) { //Type many2one
                            Integer m2id = ((IdString) ((Spinner) view).getSelectedItem()).getId();
                            if (m2id == -1) {
                                mValues.put(field, false);
                            } else {
                                mValues.put(field, m2id);
                            }
                        } else {
                            String intText = ((EditText) view).getText().toString();
                            if (intText.length() == 0) {
                                mValues.put(field, false);
                            } else {
                                mValues.put(field, Integer.valueOf(intText));
                            }
                        }
                        break;
                    case STRVAL:
                        if(view instanceof Spinner){ //Type selection
                            //TODO Save data into mValues
                            String sid = ((IdString) ((Spinner) view).getSelectedItem()).getSid();
                            if (sid.equals("")){
                                mValues.put(field,false);
                            }
                            else{
                                mValues.put(field,sid);
                            }
                        }
                        else{
                            String strText="";
                            if (view instanceof EditText) {
                                strText = ((EditText) view).getText().toString();
                            } else {
                                strText = ((TextView) view).getText().toString();
                            }
                            if (strText.length() == 0) {
                                mValues.put(field, false);
                            } else {
                                mValues.put(field, strText);
                            }
                        }

                        break;
                    case DOUBLEVAL:
                        String doubleText = ((EditText) view).getText().toString();
                        if (doubleText.length() == 0) {
                            mValues.put(field, false);
                        } else {
                            mValues.put(field, Double.valueOf(doubleText));
                        }
                        break;
                }
            }

            if (goodInput) {
                if (mEditMode) {
                    this.mWriteTask = new WriteAsyncTask(this);
                    this.mWriteTask.execute(this.mValuesToEdit, mValues);
                } else {
                    // Call AsyncTask to actually insert record
                    this.mCreateTask = new CreateAsyncTask(this);
                    this.mCreateTask.execute(mValues);
                }

            }
        }

        //Binary Field buttons ->

        if(v.getId() / MULT_CONSTANT == CLEAR_BUTTON_ID){
            TextView binTv = (TextView) findViewById(v.getId()+MULT_CONSTANT);
            binTv.setText("");
            Button binBt = (Button) findViewById(v.getId()-(2*MULT_CONSTANT));
            binBt.setEnabled(false);
            Button clrBt = (Button) findViewById(v.getId());
            clrBt.setEnabled(false);
            mValues.put((String)binBt.getTag(),false);
        }

        if(v.getId() / MULT_CONSTANT == UPLOAD_BUTTON_ID){

            Button binBt = (Button) findViewById(v.getId()-(2*MULT_CONSTANT));
            binBt.setEnabled(true);
        }
        if (v.getId() / MULT_CONSTANT == DOWNLOAD_BUTTON_ID) {
            byte[] buffer = ((byte[]) this.mValuesToEdit.get(v.getTag()));
            if (this.mValues.containsKey(v.getTag())) {
                buffer = ((byte[]) this.mValues.get(v.getTag()));
            }
            byte[] dec_buffer = Base64.decode(buffer);
            String path = "/"+getString(getBaseContext().getApplicationInfo().labelRes)+"/";
            String fileName = "openerp_file";
            if (this.mValuesToEdit.containsKey(v.getTag() + "_name")) {
                fileName = (String) this.mValuesToEdit.get(v.getTag() + "_name");
            }
            if (this.mValues.containsKey(v.getTag() + "_name")) {
                fileName = (String) this.mValues.get(v.getTag() + "_name");
            }

            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + path);
                dir.mkdirs();
                File f = new File(dir,fileName);
                FileOutputStream fout = new FileOutputStream(f);
                fout.write(dec_buffer, 0, dec_buffer.length);
                fout.close();

                if(f.exists()){
                    showNotification(f);
                }
            } catch (Exception e) {
                Log.d("Download", e.getMessage());
            }

        }

        // If TextView for DATE or DATETIME is clicked
        if (v.getId() / MULT_CONSTANT == STRVAL) {
            DateTimePickerDialog newFragment;
            if (getFieldType((String) v.getTag()).equals("DATETIME")) {
                newFragment = new DateTimePickerDialog(v, true);
                newFragment.show(getSupportFragmentManager(), "DateTimePick");
            } else {
                if (getFieldType((String) v.getTag()).equals("DATE")) {
                    newFragment = new DateTimePickerDialog(v, false);
                    newFragment.show(getSupportFragmentManager(), "DatePick");
                }
            }
        }

        //If Many2Many field is clicked
        if (v.getId()/MULT_CONSTANT == M2VAL){
            M2Dialog m2diag = new M2Dialog(this.mReadExtraAsyncTask);
            m2diag.show(getSupportFragmentManager(),"M2MChoose");
        }

    }

    private void showNotification(File file) {

        //Intent to open file
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);

        //Get file type
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext=file.getName().substring(file.getName().indexOf(".")+1);
        String type = mime.getMimeTypeFromExtension(ext);
        intent.setDataAndType(Uri.fromFile(file),type);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder noti =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle(getString(R.string.sCompletedDownload))
                        .setContentText(file.getName())
                        .setContentIntent(pIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Hide the notification after its selected
        notificationManager.notify(0,noti.build());
        noti.build().flags |= Notification.FLAG_AUTO_CANCEL;
    }


    /**
     * Return to TreeView on Back and dialog confirm
     */
    @Override
    public void onBackPressed() {
        goBackDiscard();
    }

    private void goBackDiscard() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setPositiveButton(R.string.sYes, this);
        alertDialog.setNegativeButton(getString(R.string.sNo), null);
        alertDialog.setMessage(getString(R.string.sDiscard));
        alertDialog.setTitle("TestApp");
        alertDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
