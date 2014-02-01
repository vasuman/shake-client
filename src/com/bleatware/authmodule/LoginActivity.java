package com.bleatware.authmodule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * AuthModule
 * User: vasuman
 * Date: 1/30/14
 * Time: 6:52 PM
 */
public class LoginActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

    }

    public void login(View view) {
        EditText passField = (EditText) findViewById(R.id.pass_field);
        EditText unameField = (EditText) findViewById(R.id.uname_field);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("password", passField.getText().toString());
        intent.putExtra("username", unameField.getText().toString());
        startActivity(intent);
    }
}