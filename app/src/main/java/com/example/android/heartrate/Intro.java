package com.example.android.heartrate;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Intro extends AppCompatActivity {
    private Button signBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        signBtn = (Button)findViewById(R.id.button);

        signBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn() {
        startActivity(new Intent(getApplicationContext(), CameraActivity.class));

    }
}
