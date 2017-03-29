package com.makerfire.listener;

import android.view.MotionEvent;
import android.view.View;

import com.makerfire.utils.LogUtil;

/**
 * Created by lindengfu on 9/9/16.
 */
public class JoyControlTouchListener implements View.OnTouchListener
{
    public boolean up = true;

    public interface UpProcesser
    {
        public void process();
    }


    private UpProcesser upProcesser;

    public JoyControlTouchListener(UpProcesser upProcesser)
    {
        this.upProcesser = upProcesser;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        // 判断是否需要初始化view的宽高值
        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                up = false;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                up = true;
                if (upProcesser != null)
                {
                    LogUtil.LOGI("执行upProcesser");
                    upProcesser.process();
                }
                break;
        }
        return false;
    }
}
