package com.example.shiftwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {
    Button b1,b2;
    EditText ed1,ed2;

    TextView tx1;
    int counter = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credential_input);

        b1 = (Button)findViewById(R.id.submit_button);
        ed1 = (EditText)findViewById(R.id.email_entry);
        ed2 = (EditText)findViewById(R.id.password_entry);

        final SharedPreferences prefs = this.getSharedPreferences("com.example.shiftwidget", Context.MODE_PRIVATE);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = ed1.getText().toString();
                String password = ed2.getText().toString();

                prefs.edit().putString("user_email", email).commit();
                prefs.edit().putString("user_password", password).commit();

                finish();
            }
        });



    }

    @Override
    public void finish() {
        super.finish();
    }
}