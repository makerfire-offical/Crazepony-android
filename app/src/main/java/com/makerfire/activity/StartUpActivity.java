package com.makerfire.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.makerfire.R;

/**
 * Created by lindengfu on 17-3-21.
 */

public class StartUpActivity extends Activity implements View.OnClickListener
{

    private Button btn_start;
    private Button btn_help;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_up_activity);
        init();
        setListener();
    }

    private void init()
    {
        btn_help = (Button) findViewById(R.id.btn_help);
        btn_start = (Button) findViewById(R.id.btn_start);
    }

    private void setListener()
    {
        btn_help.setOnClickListener(this);
        btn_start.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_help:
                Intent intent = new Intent(StartUpActivity.this, HelpActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.center_big_enter, 0);
                break;
            case R.id.btn_start:
                intent = new Intent(StartUpActivity.this, BTClientActivity.class);
                startActivity(intent);
                break;
        }
    }
}
