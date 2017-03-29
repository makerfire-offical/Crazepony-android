/* Project: App Controller for copter 4(including Crazepony)
 * Author: 	Huang Yong xiang 
 * Brief:	This is an open source project under the terms of the GNU General Public License v3.0
 * TOBE FIXED:  1. disconnect and connect fail with Bluetooth due to running thread 
 * 				2. Stick controller should be drawn in dpi instead of pixel.  
 * 
 * */

package com.makerfire.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.makerfire.R;
import com.makerfire.listener.JoyControlTouchListener;
import com.makerfire.threadpool.ThreadPool;
import com.makerfire.utils.LogUtil;
import com.makerfire.views.JoystickControlView;
import com.test.Crazeponyy.BluetoothLeService;
import com.test.Crazeponyy.Protocol;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@SuppressLint("NewApi")
public class BTClientActivity extends Activity implements View.OnClickListener
{

    private final static String TAG = BTClientActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService; //BLE收发服务
    private boolean mConnected = false;

    private final static int REQUEST_CONNECT_DEVICE = 1; // 宏定义查询设备句柄

    //向BLE发送数据的周期，现在是两类数据，一是摇杆的4通道值，二是请求IMU数据跟新命令
    //BLE模块本身传输速率有限，尽量减少数据发送量
    private final static int WRITE_DATA_PERIOD = 40;
    private static int IMU_CNT = 0; //update IMU period，跟新IMU数据周期，40*10ms
    Handler timeHandler = new Handler();    //定时器周期，用于跟新IMU数据等


    private TextView tv_pitch_ang, tv_roll_ang, tv_yaw_ang, tv_voltage;
    //    private TextView tv_throttle, tv_yaw, tv_pitch, tv_roll;
//    private TextView tv_pitch_ang, tv_roll_ang, tv_yaw_ang, tv_alt, tv_distance, tv_voltage;
    private Button btn_connect, btn_lock, btn_take_off, btn_header_free, btn_fixed_alt, btn_calibration;

    private TextView tv_connect;
    private TextView tv_unlock;
    private TextView tv_take_off;
    private TextView tv_no_header;
    private TextView tv_fixed_height;
    private TextView tv_calculation;

    //摇杆界面实现类，joystick UI
//    private MySurfaceView stickView;

    private JoystickControlView joystickControlViewLeft;
    private JoystickControlView joystickControlViewRight;

    private JoyControlTouchListener joyControlTouchListenerLeft;
    private JoyControlTouchListener joyControlTouchListenerRight;

    private static final int UPDATE_UI_DATA = 0x0;

    private Future<Integer> future;

    public Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case UPDATE_UI_DATA:
                    processUpdaaLogData(msg.arg1);
                    break;
            }
        }
    };


    // Code to manage Service lifecycle.
    // 管理BLE数据收发服务整个生命周期
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            } else
            {
                mBluetoothLeService.setActivity(BTClientActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mBluetoothLeService = null;
        }
    };


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    // 定义处理BLE收发服务的各类事件接收机mGattUpdateReceiver，主要包括下面几种：
    // ACTION_GATT_CONNECTED: 连接到GATT
    // ACTION_GATT_DISCONNECTED: 断开GATT
    // ACTION_GATT_SERVICES_DISCOVERED: 发现GATT下的服务
    // ACTION_DATA_AVAILABLE: BLE收到数据


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            int reCmd = -2;
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                mConnected = true;
                resetButtonValue(R.string.Disconnect);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                mConnected = false;
                resetButtonValue(R.string.Connect);
                stopSendDataThread();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {

                // Show all the supported services and characteristics on the user interface.
                // 获得所有的GATT服务，对于Crazepony的BLE透传模块，包括GAP（General Access Profile），
                // GATT（General Attribute Profile），还有Unknown（用于数据读取）
                LogUtil.LOGI("getSupportedGattServices");
                LogUtil.LOGI("写操作 getSupport");
                mBluetoothLeService.getSupportedGattServices();
                startSendDataThread();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                //64 61 74 61 3A 0D 0A
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                if (data != null && data.length > 0)
                {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    Log.i(TAG, "RX Data:" + stringBuilder);
                }

                //解析得到的数据，获得MSP命令编号
                reCmd = Protocol.processDataIn(data, data.length);
                updateLogData(1);   //跟新IMU数据，update the IMU data
            }
        }
    };


    public void startSendDataThread()
    {
        SendDataThread sendDataThread = new SendDataThread();
        future = ThreadPool.threadPool.submit(sendDataThread);
    }

    public void stopSendDataThread()
    {
        future.cancel(true);
    }


    private class SendControlThread extends Thread
    {
        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    Thread.sleep(45);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                if (JoystickControlView.touchReadyToSend == true)
                {
                    btSendBytes(Protocol.getSendData(Protocol.SET_4CON,
                            Protocol.getCommandData(Protocol.SET_4CON)));

//                    Log.i(TAG, "Thro: " + Protocol.throttle + ",yaw: " + Protocol.yaw + ",roll: "
//                            + Protocol.roll + ",pitch: " + Protocol.pitch);

//                    stickView.touchReadyToSend = false;
                }

            }
        }
    }

    private class SendDataThread implements Callable<Integer>
    {
        @Override
        public Integer call() throws Exception
        {
            while (true)
            {
                //request for IMU data update，请求IMU跟新
                byte[] data = Protocol.getSendData(Protocol.FLY_STATE, Protocol.getCommandData(Protocol.FLY_STATE));
                btSendBytes(data);
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
//                if (JoystickControlView.touchReadyToSend == true)
//                {
//                    btSendBytes(Protocol.getSendData(Protocol.SET_4CON,
//                            Protocol.getCommandData(Protocol.SET_4CON)));
//
//                    Log.i(TAG, "Thro: " + Protocol.throttle + ",yaw: " + Protocol.yaw + ",roll: "
//                            + Protocol.roll + ",pitch: " + Protocol.pitch);
//
////                    stickView.touchReadyToSend = false;
//                }

                //跟新显示摇杆数据，update the joystick data
                updateLogData(0);
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_calibration:
                calibrationBtnClicked();
                break;
            case R.id.btn_connect:
                connectBtnClicked();
                break;
            case R.id.btn_fixed_alt:
                altBtnClicked();
                break;
            case R.id.btn_header_free:
                headerFreeBtnClicked();
                break;
            case R.id.btn_lock:
                lockBtnClicked();
                break;
            case R.id.btn_take_off:
                takeOffBtnClicked();
                break;
        }
    }

    public void processConnect()
    {
        btSendBytes(Protocol.getSendData(Protocol.FLY_STATE,
                Protocol.getCommandData(Protocol.FLY_STATE)));
        btSendBytes(Protocol.getSendData(Protocol.SET_4CON,
                Protocol.getCommandData(Protocol.SET_4CON)));
    }


    //设置按键显示为默认初始化值
    //在连接成功或者断开的时候，都需要把button的值复位
    private void resetButtonValue(final int connectBtnId)
    {
        tv_connect.setText(connectBtnId);
        if (connectBtnId == R.string.Connect)
        {
            btn_connect.setBackgroundResource(R.drawable.connect);
        } else if (connectBtnId == R.string.Disconnect)
        {
            btn_connect.setBackgroundResource(R.drawable.disconnect);
        }
        tv_unlock.setText(R.string.Unarm);
        tv_take_off.setText(R.string.Launch);
        tv_no_header.setTextColor(Color.WHITE);
        tv_fixed_height.setTextColor(Color.WHITE);
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); // 设置画面为主画面 main.xml
        findViewById();
        setListener();
        startBLEService();
        init();
    }

    public void startBLEService()
    {
        //绑定BLE收发服务mServiceConnection
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    public void setListener()
    {
        btn_fixed_alt.setOnClickListener(this);
        btn_lock.setOnClickListener(this);
        btn_connect.setOnClickListener(this);
        btn_header_free.setOnClickListener(this);
        btn_take_off.setOnClickListener(this);
        btn_calibration.setOnClickListener(this);
    }


    public void init()
    {
        joystickControlViewLeft.setXData(1000, 2000);
        joystickControlViewLeft.setYData(1000, 2000);
        joystickControlViewRight.setXData(1000, 2000);
        joystickControlViewRight.setYData(1000, 2000);
        joystickControlViewLeft.setOnMoveChangeListener(new JoystickControlView.OnMoveChangeListener()
        {
            @Override
            public void onMoveChange(float x, float y)
            {
                if (joyControlTouchListenerLeft.up == false)
                {
                    LogUtil.LOGI("left:" + x + " , " + y);
                    Protocol.yaw = (int) x;
                    Protocol.throttle = (int) y;
                }
            }
        });
        joystickControlViewRight.setOnMoveChangeListener(new JoystickControlView.OnMoveChangeListener()
        {
            @Override
            public void onMoveChange(float x, float y)
            {
                if (joyControlTouchListenerRight.up == false)
                {
                    LogUtil.LOGI("right:" + x + " , " + y);
                    Protocol.pitch = (int) y;
                    Protocol.roll = (int) x;
                }
            }
        });


        joyControlTouchListenerLeft = new JoyControlTouchListener(new JoyControlTouchListener.UpProcesser()
        {
            @Override
            public void process()
            {
                Protocol.yaw = 1500;
                Protocol.throttle = 1500;
            }
        });

        joyControlTouchListenerRight = new JoyControlTouchListener(new JoyControlTouchListener.UpProcesser()
        {
            @Override
            public void process()
            {
                Protocol.pitch = 1500;
                Protocol.roll = 1500;
            }
        });

        joystickControlViewLeft.setOnTouchListener(joyControlTouchListenerLeft);

        joystickControlViewRight.setOnTouchListener(joyControlTouchListenerRight);


        Protocol.yaw = 1500;
        Protocol.throttle = 1500;
        Protocol.pitch = 1500;
        Protocol.roll = 1500;

        SendControlThread sendControlThread = new SendControlThread();
        ThreadPool.threadPool.submit(sendControlThread);
    }

    public void findViewById()
    {
        //显示 text
//        tv_throttle = (TextView) findViewById(R.id.tv_throttle); //
//        tv_yaw = (TextView) findViewById(R.id.tv_yaw);
//        tv_pitch = (TextView) findViewById(R.id.tv_pitch);
//        tv_roll = (TextView) findViewById(R.id.tv_roll);
//        //pitchAngText,rollAngText,yawAngText,altText,voltageText
//        tv_pitch_ang = (TextView) findViewById(R.id.tv_pitch_ang);
//        tv_roll_ang = (TextView) findViewById(R.id.tv_roll_ang);
//        tv_yaw_ang = (TextView) findViewById(R.id.tv_yaw_ang);
//        tv_alt = (TextView) findViewById(R.id.tv_alt);
//        tv_voltage = (TextView) findViewById(R.id.tv_voltage);
//        tv_distance = (TextView) findViewById(R.id.tv_distance);

        //摇杆
//        stickView = (MySurfaceView) findViewById(R.id.stickView);
        joystickControlViewLeft = (JoystickControlView) findViewById(R.id.jv_left);
        joystickControlViewRight = (JoystickControlView) findViewById(R.id.jv_right);

        //按钮
        btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_lock = (Button) findViewById(R.id.btn_lock);
        btn_take_off = (Button) findViewById(R.id.btn_take_off);
        btn_header_free = (Button) findViewById(R.id.btn_header_free);
        btn_fixed_alt = (Button) findViewById(R.id.btn_fixed_alt);
        btn_calibration = (Button) findViewById(R.id.btn_calibration);

        tv_connect = (TextView) findViewById(R.id.tv_connect);
        tv_unlock = (TextView) findViewById(R.id.tv_unlock);
        tv_take_off = (TextView) findViewById(R.id.tv_take_off);
        tv_no_header = (TextView) findViewById(R.id.tv_no_header);
        tv_fixed_height = (TextView) findViewById(R.id.tv_fixed_height);
        tv_calculation = (TextView) findViewById(R.id.tv_calculation);


        tv_pitch_ang = (TextView) findViewById(R.id.tv_pitch_ang);
        tv_roll_ang = (TextView) findViewById(R.id.tv_roll_ang);
        tv_yaw_ang = (TextView) findViewById(R.id.tv_yaw_ang);

        tv_voltage = (TextView) findViewById(R.id.tv_voltage);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //注册BLE收发服务接收机mGattUpdateReceiver
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            Log.d(TAG, "mBluetoothLeService NOT null");
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();
        //注销BLE收发服务接收机mGattUpdateReceiver
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //解绑BLE收发服务mServiceConnection
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        BluetoothLeService.closeLED();

    }


    // 连接按键响应函数
    public void connectBtnClicked()
    {
        if (!mConnected)
        {
            //进入扫描页面
            Intent serverIntent = new Intent(this, DeviceScanActivity.class); // 跳转程序设置
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义

        } else
        {
            //断开连接
            mBluetoothLeService.disconnect();
        }
    }

    public void lockBtnClicked()
    {
        String arm = getResources().getString(R.string.Arm);
        String unarm = getResources().getString(R.string.Unarm);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

        if (mConnected)
        {
            if (tv_unlock.getText() != arm)
            {
                btSendBytes(Protocol.getSendData(Protocol.ARM_IT, Protocol.getCommandData(Protocol.ARM_IT)));
                tv_unlock.setText(arm);
                btn_lock.setBackgroundResource(R.drawable.lock);
            } else
            {
                btSendBytes(Protocol.getSendData(Protocol.DISARM_IT, Protocol.getCommandData(Protocol.DISARM_IT)));
                tv_unlock.setText(unarm);
                btn_lock.setBackgroundResource(R.drawable.unlock);
            }
        } else
        {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
    }

    //Take off , land down
    public void takeOffBtnClicked()
    {
        String launch = getResources().getString(R.string.Launch);
        String land = getResources().getString(R.string.Land);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

        if (mConnected)
        {
            if (tv_take_off.getText() != land)
            {
                btSendBytes(Protocol.getSendData(Protocol.LAUCH, Protocol.getCommandData(Protocol.LAUCH)));
                tv_take_off.setText(land);
                Protocol.throttle = Protocol.LAUCH_THROTTLE;
                btn_take_off.setBackgroundResource(R.drawable.landing);
//                stickView.SmallRockerCircleY = stickView.rc2StickPosY(Protocol.throttle);
//                stickView.touchReadyToSend = true;
            } else
            {
                btSendBytes(Protocol.getSendData(Protocol.LAND_DOWN, Protocol.getCommandData(Protocol.LAND_DOWN)));
                tv_take_off.setText(launch);
                Protocol.throttle = Protocol.LAND_THROTTLE;
                btn_take_off.setBackgroundResource(R.drawable.take_off);
//                stickView.SmallRockerCircleY = stickView.rc2StickPosY(Protocol.throttle);
//                stickView.touchReadyToSend = true;
            }
        } else
        {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean noHeaderGreen = false;
    private boolean fixHeightGreen = false;


    //无头模式键
    public void headerFreeBtnClicked()
    {
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

        if (mConnected)
        {
            if (noHeaderGreen == false)
            {
                btSendBytes(Protocol.getSendData(Protocol.HEAD_FREE, Protocol.getCommandData(Protocol.HEAD_FREE)));
//                tv_no_header.setTextColor(Color.GREEN);
                btn_header_free.setBackgroundResource(R.drawable.no_header_green);
                noHeaderGreen = true;
            } else
            {
                btSendBytes(Protocol.getSendData(Protocol.STOP_HEAD_FREE, Protocol.getCommandData(Protocol.STOP_HEAD_FREE)));
//                tv_no_header.setTextColor(Color.WHITE);
                btn_header_free.setBackgroundResource(R.drawable.no_header);
                noHeaderGreen = false;
            }
        } else
        {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }

    }

    //定高键
    public void altBtnClicked()
    {
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

        if (mConnected)
        {
            if (fixHeightGreen == false)
            {    //定高定点都开
                btSendBytes(Protocol.getSendData(Protocol.HOLD_ALT, Protocol.getCommandData(Protocol.HOLD_ALT)));
//                tv_fixed_height.setTextColor(Color.GREEN);
                btn_fixed_alt.setBackgroundResource(R.drawable.fix_height_green);
                fixHeightGreen = true;
//                stickView.altCtrlMode = 1;
            } else
            {
                btSendBytes(Protocol.getSendData(Protocol.STOP_HOLD_ALT, Protocol.getCommandData(Protocol.STOP_HOLD_ALT)));
//                tv_fixed_height.setTextColor(Color.WHITE);
                btn_fixed_alt.setBackgroundResource(R.drawable.fix_height);
                fixHeightGreen = false;
//                stickView.altCtrlMode = 0;
            }
        } else
        {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
    }

    //校准
    public void calibrationBtnClicked()
    {
        String disconnectToast = getResources().getString(R.string.DisconnectToast);
        if (mConnected)
        {
            btSendBytes(Protocol.getSendData(Protocol.MSP_ACC_CALIBRATION, Protocol.getCommandData(Protocol.MSP_ACC_CALIBRATION)));
        } else
        {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
    }

    public void btSendBytes(byte[] data)
    {
        //当已经连接上时才发送
        if (mConnected)
        {
            mBluetoothLeService.writeCharacteristic(data);
        }
    }

    // 接收扫描结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK)
                {
                    mDeviceName = data.getExtras().getString(EXTRAS_DEVICE_NAME);
                    mDeviceAddress = data.getExtras().getString(EXTRAS_DEVICE_ADDRESS);

                    Log.i(TAG, "mDeviceName:" + mDeviceName + ",mDeviceAddress:" + mDeviceAddress);

                    //连接该BLE Crazepony模块
                    if (mBluetoothLeService != null)
                    {
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        Log.d(TAG, "Connect request result=" + result);
                    }
                }
                break;
            default:
                break;
        }
    }

    //跟新Log相关的数据，主要是飞控传过来的IMU数据和摇杆值数据
    //update log,included the IMU data from FC and joysticks data
    //msg 0 -> joystick data
    //msg 1 -> IMU data
    private void updateLogData(int msg)
    {
        Message message = Message.obtain();
        message.what = UPDATE_UI_DATA;
        message.arg1 = msg;
        handler.sendMessage(message);
    }

    private void processUpdaaLogData(int msg)
    {
        if (0 == msg)
        {
//            tv_throttle.setText("Throttle:" + Integer.toString(Protocol.throttle));
//            tv_yaw.setText("Yaw:" + Integer.toString(Protocol.yaw));
//            tv_pitch.setText("Pitch:" + Integer.toString(Protocol.pitch));
//            tv_roll.setText("Roll:" + Integer.toString(Protocol.roll));
        } else if (1 == msg)
        {
            LogUtil.LOGI("Protocol.pitchAng:"+Protocol.pitchAng);
//            LogUtil.LOGI("RX Datas跟新UI");
            tv_pitch_ang.setText("Pitch Ang: " + Protocol.pitchAng);
            tv_roll_ang.setText("Roll Ang: " + Protocol.rollAng);
            tv_yaw_ang.setText("Yaw Ang: " + Protocol.yawAng);
            tv_voltage.setText("Voltage: " + Protocol.voltage);
//            tv_alt.setText("Alt:" + Protocol.alt + "m");
//            tv_distance.setText("speedZ:" + Protocol.speedZ + "m/s");
//            LogUtil.LOGI("RX Datas跟新UIspeedZ:" + Protocol.speedZ + "m/s");
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}