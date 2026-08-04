package com.rch.escposbitmapprinttest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printTest();
            }
        });
    }

    void printTest() {
        String printerIp = "127.0.0.1";
        int printerPort = 4617;

// Trigger print command
        NetworkPrinterHelper.printAssetBitmapOverSocket(
                this,           // Activity Context
                printerIp,      // Target Printer IP
                printerPort,    // Port 4617
                "image.bmp",     // Image inside assets/
                384             // 384px for 58mm printer roll
        );
    }
}