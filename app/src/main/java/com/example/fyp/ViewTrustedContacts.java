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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ViewTrustedContacts extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    ListView lv1;
    //    Button back;
    // ArrayList<String> contacts = new ArrayList<String>();
    ArrayList<String> items = new ArrayList<String>();
    ArrayList<String> deleteContacts = new ArrayList<String>();
    //  String name;
    // String phoneNumber;

    TextView contactName, contactPhoneno;

    ImageView backbutton;

    Button deleteContactsBtn;

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_trusted_contacts);

        String name = "name";
        String number = "number";
        try {
            db = openOrCreateDatabase("DriverAssistant", MODE_PRIVATE, null);
            Cursor cursor = db.rawQuery("select * from TrustedContacts", null);
            items = new ArrayList<String>();
            if (cursor.moveToFirst()) {
                do {
                    items.add(cursor.getString(cursor.getColumnIndex(name)) + ":"
                            + cursor.getString(cursor.getColumnIndex(number)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "No trusted contacts added yet.", Toast.LENGTH_SHORT).show();
        }

        lv1 = (ListView) findViewById(R.id.lv);


        lv1.setAdapter(new CustomList(this, R.layout.row, items));


        lv1.setOnItemClickListener(this);

        backbutton = (ImageView) findViewById(R.id.backbtn1);

        backbutton.setOnClickListener(this);

        deleteContactsBtn = (Button) findViewById(R.id.btn1);

        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.trustedcontacts);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            TextView tv1 = (TextView) findViewById(R.id.textView2);
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            backbutton.setImageResource(R.drawable.ic_back_button_black);
            deleteContactsBtn.setTextColor(getResources().getColor(R.color.light_grey));
        }


        deleteContactsBtn.setVisibility(View.GONE);

        deleteContactsBtn.setOnClickListener(this);

    }

    public boolean deleteRecord(String number) {
        return db.delete("TrustedContacts", "number" + "=" + "'" + number + "'", null) > 0;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


//Get related checkbox and change flag status..
        CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox_contact);
        cb.setChecked(!cb.isChecked());
        deleteContactsBtn.setVisibility(View.VISIBLE);
        //       Toast.makeText(this, "Click item" + items.get(position), Toast.LENGTH_SHORT).show();
        if (cb.isChecked() == true) {
            deleteContacts.add(items.get(position));
        } else {
            deleteContacts.remove(items.get(position));
        }


    }

    @Override
    public void onClick(View v) {
        if (v.getId() == backbutton.getId()) {


            finish();
        } else if (v.getId() == deleteContactsBtn.getId()) {

            String name = "name";
            String number = "number";

            for (int i = 0; i < deleteContacts.size(); i++) {

                String[] contactInfo = deleteContacts.get(i).split(":");
                deleteRecord(contactInfo[1]);
            }

            Toast.makeText(getApplicationContext(), "Contacts deleted Successfully", Toast.LENGTH_SHORT).show();


            Cursor cursor = db.rawQuery("select * from TrustedContacts", null);
            items.clear();
            items = new ArrayList<String>();
            if (cursor.moveToFirst()) {
                do {
                    items.add(cursor.getString(cursor.getColumnIndex(name)) + ":"
                            + cursor.getString(cursor.getColumnIndex(number)));
                } while (cursor.moveToNext());
            }
            lv1.setAdapter(new CustomList(this, R.layout.row, items));
//            Toast.makeText(getApplicationContext(), "Trusted Contacts: " + items, Toast.LENGTH_SHORT).show();
        }
    }

    class CustomList extends ArrayAdapter {

        public CustomList(Context context, int resource, ArrayList<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            View v = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.row, null);


            contactName = (TextView) v.findViewById(R.id.name);
            contactPhoneno = (TextView) v.findViewById(R.id.phoneno);

            String[] contactsInfo = items.get(position).split(":");


            contactName.setText(contactsInfo[0]);
            contactPhoneno.setText(contactsInfo[1]);

            SharedPreferences settings = getSharedPreferences("home_settings", 0);
            boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
            if (!darkModeUi_value) {
                contactName.setTextColor(getResources().getColor(R.color.dark_grey));
                contactPhoneno.setTextColor(getResources().getColor(R.color.dark_grey));
                ImageView img1 = (ImageView) v.findViewById(R.id.imageView2);
                img1.setImageResource(R.drawable.ic_contact_black);
                CheckBox ch = (CheckBox) v.findViewById(R.id.checkbox_contact);
                //ch.setButtonTint(getResources().getColor(R.color.dark_grey));
                CompoundButtonCompat.setButtonTintList(ch, ColorStateList
                        .valueOf(getResources().getColor(R.color.dark_grey)));
            }

            return v;
        }
    }
}


