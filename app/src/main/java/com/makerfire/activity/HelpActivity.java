package com.makerfire.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.makerfire.R;

/**
 * Created by lindengfu on 17-3-23.
 */

public class HelpActivity extends Activity
{

    private ImageView iv_full;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_activity);
        iv_full = (ImageView) findViewById(R.id.iv_full);

        iv_full.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int eventAction = event.getActionMasked();
//        LogUtil.LogI("action:" + eventAction);
                switch (eventAction)
                {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        HelpActivity.this.finish();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
