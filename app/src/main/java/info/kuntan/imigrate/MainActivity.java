package info.kuntan.imigrate;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1001;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_CONTACTS = 1002;

    private Handler backThread;
    private ArrayList<String> contacts;
    private String exFileName = "contacts.txt";
    private Cursor cursor;
    private int    counter;
    private String out;
    private boolean bRDContacts = false;
    private boolean bWRContacts = false;

    public void askPermissions (){
        // This is a general routing for asking permissions

        // READ_CONTACTS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        } else {
            bRDContacts = true;
        }

        // WRITE_CONTACTS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        } else {
            bWRContacts = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bRDContacts = true;

                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    bWRContacts = true;
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // start a background thread
        backThread = new Handler();

        askPermissions();

    }

    public void getContacts () {
        contacts = new ArrayList<String>();
        final Activity activity = this;
        String phoneNumber = null;
        String email = null;

        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA;

        // query phone contacts
        StringBuffer output;
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null,null, null, null);
        // Iterate every contact in the phone


        // format string: n:xxx; p:xxx; p:xxx; m:xxx; m:xxx
        if (cursor.getCount() > 0) {
            counter = 0;
            while (cursor.moveToNext()) {
                output = new StringBuffer();
                // Update the progress message

                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));

                if ( name.trim().equals("")) {
                    continue;
                }

                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {
                    output.append("n:" + name + "; ");
                    //This is to read multiple phone numbers associated with the same contact
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
                    while (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        output.append("p:" + phoneNumber + "; ");
                    }
                    phoneCursor.close();

                    // Read every email id associated with the contact
                    Cursor emailCursor = contentResolver.query(EmailCONTENT_URI, null, EmailCONTACT_ID + " = ?", new String[]{contact_id}, null);
                    while (emailCursor.moveToNext()) {
                        email = emailCursor.getString(emailCursor.getColumnIndex(DATA));
                        output.append("m:" + email + "; ");
                    }
                    emailCursor.close();
                }
                // Add the contact to the ArrayList
                out = output.toString().trim();
                if (!out.equals("")) {
                    contacts.add(out);
                    counter ++;
                }
            }

            // display the list on the view
            backThread.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.refreshView ();
                }
            });
        }
    }

    public void refreshView() {
        EditText v = (EditText) findViewById ( R.id.view );
        String txt = "";
        for (int i=0; i<contacts.size(); i++) {
            txt = txt + contacts.get(i) + "\n";
        }
        v.setText( txt );

        saveContacts ();
    }

    public void saveContacts () {
        File file = new File(this.getExternalFilesDir(null), exFileName);
        try {
            FileWriter writer = new FileWriter(file);
            for (int i=0; i<contacts.size(); i++) {
                String line = contacts.get (i);
                line.trim();
                if ( !line.equals( "" )) {
                    writer.append( line + "\n" );
                }

            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setContacts () {
        //
        loadContacts ();

 //       String testline = "n:test one; p:+1 990 876 4032; p:+8613671133; m:fky@hotmail.com";
 //       if ( contacts == null ) {
 //           contacts = new ArrayList<String>();
 //       } else {
 //           contacts.clear();
 //       }
     //   contacts.add(testline);

        ArrayList<ContentProviderOperation> cntProOper = new ArrayList<ContentProviderOperation>();
        int referenceID = cntProOper.size();

        for ( int i=0; i<contacts.size();i++){
            String line = contacts.get(i).trim();
            if (line.equals("")) {
                continue;
            }
            //
            // add a contact

            //Newly Inserted contact
            // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)//Step1
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

            String[] cmds = line.split(";");
            for (int j=0; j<cmds.length; j++) {
                String cmd = cmds[j].trim();
                String[] kv = cmd.split(":");

                if (kv[0].equals("n")) {
                    //Display name will be inserted in ContactsContract.Data table
                    cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step2
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, referenceID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, kv[1]) // Name of the contact
                            .build());
                } else if (kv[0].equals("p")) {
                    //Mobile number will be inserted in ContactsContract.Data table
                    cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 3
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, referenceID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, kv[1]) // Number to be added
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build()); //Type like HOME, MOBILE etc
                } else if (kv[0].equals("m")) {
                    // email
                    cntProOper.add(ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, referenceID)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, kv[1])
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK).build());

                }
            }

            // add through intent
            try
            {
                // We will do batch operation to insert all above data
                //Contains the output of the app of a ContentProviderOperation.
                //It is sure to have exactly one of uri or count set
                ContentProviderResult[] contentProresult = null;
                contentProresult = getContentResolver()
                        .applyBatch(ContactsContract.AUTHORITY, cntProOper);
                //apply above data insertion into contacts list
                if ( contentProresult != null ) {
                    Log.d ("dbg", contentProresult[0].uri.toString());
                }

            }
            catch (RemoteException exp)
            {
                //logs;
            }
            catch (OperationApplicationException exp)
            {
                //logs
            }
            cntProOper.clear(); // ready for next?
        }



        backThread.post(new Runnable() {
            @Override
            public void run() {
                refreshView();
            }
        });
    }

    public void loadContacts () {
        File file = new File(this.getExternalFilesDir(null), exFileName);
        try{
            BufferedReader rd = new BufferedReader(new FileReader( file ));
            if ( contacts == null ) {
                contacts = new ArrayList<String>();
            } else {
                contacts.clear();
            }
            String line;
            while ( (line = rd.readLine()) != null ) {
                contacts.add (line);
            }
            rd.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_export) {
            if ( bRDContacts ) {
                backThread.post(new Runnable() {
                    @Override
                    public void run() {
                        getContacts();
                    }
                });
            }
            return true;
        }

        if (id == R.id.action_import) {
            if ( bWRContacts ) {
                backThread.post(new Runnable() {
                    @Override
                    public void run() {
                        setContacts();
                    }
                });
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
