package com.jqjm.myliblist;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.jqjm.pinlibrary.PinEdit;

public class MainActivity extends AppCompatActivity {
    PinEdit pinEntry;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    Toast.makeText(this,"密度："+this.getResources().getDisplayMetrics().density,Toast.LENGTH_LONG).show();
        pinEntry = (PinEdit) findViewById(R.id.txt_pin_entry);
        if (pinEntry != null) {
            pinEntry.setPinBackgroundDrawable(getDrawable(com.jqjm.pinlibrary.R.drawable.gray_white));
            pinEntry.setOnPinEnteredListener(new PinEdit.OnPinEnteredListener() {
                @Override
                public void onPinEntered(CharSequence str) {
                    if (str.toString().equals("123456")) {
                        Toast.makeText(MainActivity.this, "SUCCESS", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "FAIL", Toast.LENGTH_SHORT).show();
                        pinEntry.setText(null);
                    }
                }
            });
        }



    }
}
