package com.example.android.heartrate;

import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;


public class LoginActivity extends AppCompatActivity {
    private ImageButton signBtn;
    private ImageButton introBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        signBtn = (ImageButton) findViewById(R.id.sign_in_button);

        signBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        introBtn = (ImageButton) findViewById(R.id.introduction);

        introBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intro();
            }
        });

    }


    private void signIn() {
        startActivity(new Intent(getApplicationContext(), CameraActivity.class));

    }

    private void Intro() {
        startActivity(new Intent(getApplicationContext(), Intro.class));

    }
}