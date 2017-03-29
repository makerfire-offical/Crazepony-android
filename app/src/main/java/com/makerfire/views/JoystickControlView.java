package com.makerfire.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.makerfire.R;
import com.makerfire.utils.LogUtil;


/**
 * 作者: DongZhi 2016/3/11. 保佑以下代码无bug...
 */
public class JoystickControlView extends View
{
    private Paint mainCircle;
    private float centerX;
    private float centerY;
    private float joystickRadius;
    private Paint centerCircle;
    private Paint mainBgCircle;
    private float xPosition;
    private float yPosition;

    private float buttonRadius;
    private Bitmap centerCircleBitmap;
    private Bitmap controlDirectionsBitmap;
    private float offset;
    private boolean isCanTouch = true;
    private ValueAnimator valueAnimatorMoveX;
    private ValueAnimator valueAnimatorMoveY;
    private boolean isCanReset = true;
    private boolean isMoveing;
    private DisplayMetrics dm = getResources().getDisplayMetrics();
    private float directionPaintSize = 2 * dm.density;
    private float centerPaintSize = 4 * dm.density;

    private float OFFSET_M_SIZE = directionPaintSize / 2;
    private float OFFSET_C_SIZE = OFFSET_M_SIZE + centerPaintSize / 2;


    public static boolean touchReadyToSend = false;


    public boolean isMoveing()
    {
        return isMoveing;
    }

    public boolean isCanReset()
    {
        return isCanReset;
    }

    public void setCanReset(boolean canReset)
    {
        isCanReset = canReset;
    }

    public boolean isCanTouch()
    {
        return isCanTouch;
    }

    public void setCanTouch(boolean canTouch)
    {
        isCanTouch = canTouch;
        reset();
    }

    public float getButtonRadius()
    {
        return buttonRadius;
    }

    private float mainRadius;
    private float minY;
    private float maxY;
    private Context context;

    public JoystickControlView(Context context)
    {
        super(context);
        this.context = context;
    }

    public JoystickControlView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        init();
    }

    public JoystickControlView(Context context, AttributeSet attrs, int defaultStyle)
    {
        super(context, attrs, defaultStyle);
        this.context = context;
        init();
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld)
    {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        int d = Math.min(xNew, yNew);
        int width = getWidth();
        int height = getHeight();
        offset = d / 6.0f;
        buttonRadius = (int) (d / 2 * 0.25);
        //joystickRadius = (int) (d / 2 * 0.5);
        xPosition = width / 2.0f;
        yPosition = width / 2.0f;
        centerX = width / 2.0f;
        centerY = height / 2.0f;
        joystickRadius = centerX - offset;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // setting the measured values to resize the view to a certain
        // width and
        // height
//        LogUtil.LOGI("onMeasure");
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
        d = (int) (190 * dm.density);
        setMeasuredDimension(d, d);
    }

    private int measure(int measureSpec)
    {
        int result = 0;
        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED)
        {
            // Return a default size of 200 if no bounds are
            // specified.
            result = 200;
        } else
        {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    private void init()
    {
        // 主圆背景
//        mainBgCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
//        mainBgCircle.setColor(getResources().getColor(R.color.bg_hold_layout));
//        mainBgCircle.setAlpha(60);//设置透明度
//        mainBgCircle.setStyle(Paint.Style.FILL);
//        mainBgCircle.setAntiAlias(true);
//
//        // 主圆外框
//        mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
//        mainCircle.setColor(Color.WHITE);
//        mainCircle.setStyle(Paint.Style.STROKE);
//        mainCircle.setStrokeWidth(directionPaintSize);
//        mainCircle.setAntiAlias(true);
//        //	mainCircle.setAlpha( 75 );//设置透明度
//
//        // 中间圆
//        centerCircle = new Paint();
//        centerCircle.setColor(Color.WHITE);
//        centerCircle.setAntiAlias(true);
//        centerCircle.setStrokeWidth(centerPaintSize);
//        centerCircle.setStyle(Paint.Style.STROKE);//STROKE 空心圆 FILL实心圆
        //	centerCircle.setAlpha( 75 );//设置透明度
//        centerCircleBitmap = BitmapFactory.decodeResource(getResources(),
//                R.drawable.ic_control_center_circle);
//        controlDirectionsBitmap = BitmapFactory.decodeResource(getResources(),
//                R.drawable.ic_control_center_four_directions);


        //主圆背景
        mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainCircle.setColor(Color.WHITE);
        mainCircle.setStyle(Paint.Style.FILL);

        //中间圆
        centerCircle = new Paint();
        centerCircle.setColor(0xFF2191DA);
        centerCircle.setStyle(Paint.Style.FILL);

        centerCircleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.center_circle);
        controlDirectionsBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.big_circle);


        post(new Runnable()
        {
            @Override
            public void run()
            {
                float bakX = xPosition;
                float bakY = yPosition;
                move(getWidth() / 2, 0);
                minY = yPosition;
                move(getWidth() / 2, getHeight());
                maxY = yPosition;
                System.err.println("maxY=" + maxY + ",minY=" + minY);
                move(bakX, bakY);// 恢复
            }
        });
    }

    public void setMainBackground(int resId)
    {
        controlDirectionsBitmap = BitmapFactory.decodeResource(getResources(), resId);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        LogUtil.LOGI("onDraw " + centerPaintSize + " " + OFFSET_C_SIZE + " " + directionPaintSize + " " + OFFSET_M_SIZE);
        // canvas.drawCircle(centerX, centerY, mainRadius,
        // mainCircle);
        // canvas.drawCircle(xPosition, yPosition, buttonRadius,
        // centerCircle);

        //画背景
                /*canvas.drawBitmap(controlDirectionsBitmap,
                        new Rect(0, 0, controlDirectionsBitmap.getWidth(),
								controlDirectionsBitmap.getHeight()),
						new Rect(0, 0, getWidth(), getHeight()), centerCircle);
			 //画中心控制圆圈
				canvas.drawBitmap(centerCircleBitmap,
						new Rect(0, 0, centerCircleBitmap.getWidth(),
								centerCircleBitmap.getHeight()),
						new RectF(xPosition - offset, yPosition - offset, xPosition + offset,
								yPosition + offset),
						centerCircle);*/
//        canvas.drawCircle(centerX, centerX,  centerX, mainBgCircle);
//        canvas.drawCircle(centerX, centerX, centerX - OFFSET_M_SIZE, mainCircle);
//        canvas.drawCircle(xPosition, yPosition, offset - OFFSET_C_SIZE, centerCircle);


        canvas.drawBitmap(controlDirectionsBitmap,
                new Rect(0, 0, controlDirectionsBitmap.getWidth(),
                        controlDirectionsBitmap.getHeight()),
                new Rect(0, 0, getWidth(), getHeight()),
                centerCircle);

        canvas.drawBitmap(centerCircleBitmap, new Rect(0, 0, centerCircleBitmap.getWidth(),
                centerCircleBitmap.getHeight()), new RectF(xPosition - offset, yPosition - offset, xPosition + offset, yPosition + offset), centerCircle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!isCanTouch)
        {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (isCanReset)
            {
                reset();
            } else
            {
                resetLR();
            }

        } else
        {
            if (valueAnimatorResetX != null && valueAnimatorResetY != null)
            {
                valueAnimatorResetX.removeAllUpdateListeners();
                valueAnimatorResetY.removeAllUpdateListeners();
            }
            move(event.getX(), event.getY());
            touchReadyToSend = true;
        }
        if (ocl != null)
        {
            ocl.onMoveChange(getXValue(minDX, maxDX), getYValue(minDY, maxDY));
        }
        return true;
    }

    private Runnable action = new Runnable()
    {
        @Override
        public void run()
        {
            isMoveing = false;
        }
    };

    /**
     * 重置左右
     */
    private void resetLR()
    {
        valueAnimatorResetX = new ValueAnimator();
        valueAnimatorResetX.setFloatValues(xPosition, centerX);
        valueAnimatorResetX.setDuration(200);
        valueAnimatorResetX.start();
        valueAnimatorResetX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                xPosition = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });

    }

    /**
     * 重置上下
     */
    private void resetTB()
    {
        valueAnimatorResetY = new ValueAnimator();
        valueAnimatorResetY.setFloatValues(yPosition, centerY);
        valueAnimatorResetY.setDuration(200);
        valueAnimatorResetY.start();
        valueAnimatorResetY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                yPosition = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });

    }

    /**
     * 移动
     *
     * @param x
     * @param y
     */
    public void move(float x, float y)
    {
        isMoveing = true;
        xPosition = x;
        yPosition = y;
        double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        if (abs > joystickRadius)
        {
            xPosition = (float) ((xPosition - centerX) * joystickRadius / abs
                    + centerX);
            yPosition = (float) ((yPosition - centerY) * joystickRadius / abs
                    + centerY);
        }
        invalidate();
        removeCallbacks(action);
        postDelayed(action, 200);
    }

    /**
     * 移动
     */
    public void moveY(float y)
    {

        yPosition = y;
        double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        if (abs > joystickRadius)
        {
            // xPosition = (float) ((xPosition - centerX) *
            // joystickRadius / abs + centerX);
            yPosition = (float) ((yPosition - centerY) * joystickRadius / abs
                    + centerY);
        }
        invalidate();
    }

    /**
     * 移动
     *
     * @param x
     */
    public void moveX(float x)
    {
        xPosition = x;
        double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        if (abs > joystickRadius)
        {
            xPosition = (float) ((xPosition - centerX) * joystickRadius / abs
                    + centerX);
            // yPosition = (float) ((yPosition - centerY) *
            // joystickRadius / abs + centerY);
        }
        invalidate();
    }

    /**
     * 移动并带动画
     */
    public void moveAnima(final float x, final float y)
    {
        if (valueAnimatorMoveX != null)
        {
            valueAnimatorMoveX.removeAllUpdateListeners();
        }
        if (valueAnimatorMoveY != null)
        {
            valueAnimatorMoveY.removeAllUpdateListeners();
        }
        valueAnimatorMoveX = new ValueAnimator();

        valueAnimatorMoveX.setFloatValues(xPosition, x);
        valueAnimatorMoveX.setDuration(100);
        valueAnimatorMoveX.start();
        valueAnimatorMoveX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                moveX((Float) animation.getAnimatedValue());

            }
        });
        valueAnimatorMoveY = new ValueAnimator();

        valueAnimatorMoveY.setFloatValues(yPosition, y);
        valueAnimatorMoveY.setDuration(100);
        valueAnimatorMoveY.start();
        valueAnimatorMoveY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                moveY((Float) animation.getAnimatedValue());
            }
        });
    }

    private ValueAnimator valueAnimatorResetX;
    private ValueAnimator valueAnimatorResetY;

    /**
     * 重置
     */
    public void reset()
    {
        resetLR();
        resetTB();
    }

    public static final int POSITION_CENTER = 1;
    public static final int POSITION_BOTTOM = 2;

    public void setDefaultPosition(int positionType)
    {
        switch (positionType)
        {
            case POSITION_BOTTOM:
                setCanReset(false);
                post(new Runnable()
                {
                    @Override
                    public void run()
                    {// 默认为最下端
                        move(getPositionX(), 2 * getCenterY());
                    }
                });
                break;
            case POSITION_CENTER:
            default:
                setCanReset(true);
                break;
        }

    }

    public int getXValue(int minRangeX, int maxRangeX)
    {
//        LogUtil.LOGI("rangPoxXXX:" + minRangeX + "  " + maxRangeX + "  " + xPosition + "  " + minY + "  " + maxY);

        return Math.round((((xPosition - minY) * (maxRangeX - minRangeX) / (maxY - minY)))
                + minRangeX);
    }

    public int getYValue(int minRangeY, int maxRangeY)
    {
//        LogUtil.LOGI("rangPoxYYY:" + minRangeY + "  " + maxRangeY + "  " + yPosition + "  " + minY + "  " + maxY);

        return Math.round(((maxRangeY - minRangeY)
                - ((yPosition - minY) * (maxRangeY - minRangeY) / (maxY - minY)))
                + minRangeY);
    }

    int minDX, maxDX;
    int minDY, maxDY;

    public void setXData(int minX, int maxX)
    {
        minDX = minX;
        maxDX = maxX;
    }

    public void setYData(int minY, int maxY)
    {
        minDY = minY;
        maxDY = maxY;
    }

    public float getCenterY()
    {
        return centerY;
    }

    public float getCenterX()
    {
        return centerX;
    }

    public float getPositionX()
    {
        return xPosition;
    }

    public float getPositionY()
    {
        return yPosition;
    }

    private OnMoveChangeListener ocl;

    public void setOnMoveChangeListener(OnMoveChangeListener ocl)
    {
        this.ocl = ocl;
    }

    public interface OnMoveChangeListener
    {
        void onMoveChange(float x, float y);

    }

    public float getMin()
    {
        return minY;
    }

    public float getMax()
    {
        return maxY;
    }
}