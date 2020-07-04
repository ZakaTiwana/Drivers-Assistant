

package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.CompoundButtonCompat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AddContacts extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    ListView lv1;
    //    Button back;
    ArrayList<String> contacts = new ArrayList<String>();
    ArrayList<String> trustedContacts = new ArrayList<String>();
    ArrayList<String> Ocontacts = new ArrayList<String>();
    String name;
    String phoneNumber;

    TextView contactName, contactPhoneno;

    ImageView backbutton;

    Button addContacts;

    SQLiteDatabase db;

    EditText editText;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);

//        contacts.add("Zaka  :03145310281");
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()) {
            name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            contacts.add(name + ":" + phoneNumber);
            Ocontacts.add(name + ":" + phoneNumber);
        }
        phones.close();
//        ArrayAdapter<String> myAdapter= new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1,
//                contacts);
//
        lv1 = (ListView) findViewById(R.id.lv);
//        lv1.setAdapter(myAdapter);

        adapter = new CustomList(this, R.layout.row, contacts);
        lv1.setAdapter(adapter);
        //  lv1.setTextFilterEnabled(true);
        lv1.setOnItemClickListener(this);

        backbutton = (ImageView) findViewById(R.id.backbtn1);

        backbutton.setOnClickListener(this);

        addContacts = (Button) findViewById(R.id.btn1);
        addContacts.setVisibility(View.GONE);


        addContacts.setOnClickListener(this);

        editText = (EditText) findViewById(R.id.editText2);

        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.addcontacts);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            TextView tv1=(TextView)findViewById(R.id.textView2);
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            backbutton.setImageResource(R.drawable.ic_back_button_black);
            addContacts.setTextColor(getResources().getColor(R.color.light_grey));
            editText.setBackgroundResource(R.drawable.white_border);
            editText.setTextColor(getResources().getColor(R.color.light_grey));
        }


        editText.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {

            }

            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {

            }

            public void afterTextChanged(Editable arg0) {
                // AddContacts.this.adapter.getFilter().filter(arg0);
                if (arg0.toString().equals("")) {

                    // reset listview

                    initList();

                } else {

                    // perform search

                    searchItem(arg0.toString());
                }
            }
        });

    }

    public void initList() {
        contacts = Ocontacts;
        adapter = new CustomList(this, R.layout.row, contacts);
        lv1.setAdapter(adapter);
        lv1.setOnItemClickListener(this);
    }

    public void searchItem(String textToSearch) {
        ArrayList<String> Newcontacts = new ArrayList<String>();
        String[] ContactsInfo;
        textToSearch = textToSearch.toLowerCase();
        String name;
        for (int i = 0; i < contacts.size(); i++) {
            ContactsInfo = contacts.get(i).split(":");
            name = ContactsInfo[0].toLowerCase();
            if (name.contains(textToSearch)) {
                Newcontacts.add(contacts.get(i));
            }
        }
        contacts = Newcontacts;
        adapter = new CustomList(this, R.layout.row, contacts);
        lv1.setAdapter(adapter);
        lv1.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


//Get related checkbox and change flag status..
        CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox_contact);
        cb.setChecked(!cb.isChecked());
        addContacts.setVisibility(View.VISIBLE);
        //     Toast.makeText(this, "Click item" + contacts.get(position), Toast.LENGTH_SHORT).show();
        if (cb.isChecked() == true) {
            trustedContacts.add(contacts.get(position));
        } else {
            trustedContacts.remove(contacts.get(position));
        }


    }

    @Override
    public void onClick(View v) {
        if (v.getId() == backbutton.getId()) {
//            db.execSQL("DELETE FROM TrustedContacts;");
//            Toast.makeText(getApplicationContext(), "Data Deleted", Toast.LENGTH_SHORT).show();

            finish();
        } else if (v.getId() == addContacts.getId()) {

            db = openOrCreateDatabase("DriverAssistant", MODE_PRIVATE, null);
            db.execSQL("create table if not exists  TrustedContacts(name varchar(20), number varchar(20))");

            String name = "name";
            String number = "number";

            Cursor cursor = db.rawQuery("select * from TrustedContacts", null);
            ArrayList<String> check = new ArrayList<String>();
            if (cursor.moveToFirst()) {
                do {
                    check.add(cursor.getString(cursor.getColumnIndex(name)) + ":"
                            + cursor.getString(cursor.getColumnIndex(number)));
                } while (cursor.moveToNext());
            }


            for (int i = 0; i < trustedContacts.size(); i++) {
                String[] trustedContactsInfo = trustedContacts.get(i).split(":");
                Log.d("Check", "TrustedContactValue " + i + " " + trustedContactsInfo[1]);
                for (int j = 0; j < check.size(); j++) {
                    String[] checkInfo = check.get(j).split(":");
                    Log.d("Check", "CheckValue " + j + " " + checkInfo[1]);
                    if (trustedContactsInfo[1].equals((checkInfo[1]))) {
                        trustedContacts.remove(trustedContacts.get(i));
                        i = i - 1;
//                        size=size+1;
                        break;
                    }

                }
            }


            for (int i = 0; i < trustedContacts.size(); i++) {

                String[] contactInfo = trustedContacts.get(i).split(":");
                db.execSQL("insert into TrustedContacts(" + name + "," + number + ")values (" + "'" + contactInfo[0] + "'" + "," + "'" + contactInfo[1] + "'" + ");");

            }
            if (trustedContacts.size() > 0) {
                Toast.makeText(getApplicationContext(), "Contacts Added Successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Selected Contact Already exists!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class CustomList extends ArrayAdapter {

        ArrayList<String> Customcontacts = new ArrayList<String>();

        public CustomList(Context context, int resource, ArrayList<String> objects) {
            super(context, resource, objects);
            Customcontacts = objects;
            Set<String> set = new HashSet<>(Customcontacts);
            Customcontacts.clear();
            Customcontacts.addAll(set);
            Collections.sort(Customcontacts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            View v = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.row, null);


            contactName = (TextView) v.findViewById(R.id.name);
            contactPhoneno = (TextView) v.findViewById(R.id.phoneno);

            String[] contactsInfo = Customcontacts.get(position).split(":");


            contactName.setText(contactsInfo[0]);
            contactPhoneno.setText(contactsInfo[1]);

            SharedPreferences settings = getSharedPreferences("home_settings", 0);
            boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
            if (!darkModeUi_value) {
                contactName.setTextColor(getResources().getColor(R.color.dark_grey));
                contactPhoneno.setTextColor(getResources().getColor(R.color.dark_grey));
                ImageView img1=(ImageView)v.findViewById(R.id.imageView2);
                img1.setImageResource(R.drawable.ic_contact_black);
                CheckBox ch=(CheckBox)v.findViewById(R.id.checkbox_contact);
                //ch.setButtonTint(getResources().getColor(R.color.dark_grey));
                CompoundButtonCompat.setButtonTintList(ch, ColorStateList
                        .valueOf(getResources().getColor(R.color.dark_grey)));
            }
            return v;
        }
    }
}
