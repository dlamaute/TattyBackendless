package com.wearabletattoos.diana.tatty;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class CoreActivity extends ActionBarActivity {

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_core);
        userEmail = getIntent().getStringExtra("email");

    }

    public void goToScan(View v) {
        Log.d("WARN", "registered button scan");
        Intent scanNFCIntent = new Intent(this, ScanTattooActivity.class);
        scanNFCIntent.putExtra("email", userEmail);
        startActivity(scanNFCIntent);
        Log.d("WARN", "supposedly i started the intent");
    }

    public void goToTattoos(View v) {
        Log.d("WARN", "registered button tats");
        Intent myTattoosIntent = new Intent(this, MyTattoosActivity.class);
        myTattoosIntent.putExtra("email", userEmail);
        startActivity(myTattoosIntent);
        Log.d("WARN", "supposedly i started the intent");
    }
}
