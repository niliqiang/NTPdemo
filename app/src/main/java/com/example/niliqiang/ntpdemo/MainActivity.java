package com.example.niliqiang.ntpdemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.instacart.library.truetime.TrueTimeRx;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.widget.TextView.OnEditorActionListener;
import io.reactivex.schedulers.Schedulers;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private String serverAddr;
    private TextView tv_ntpTime;
    private TextView tv_systemTime;
    private EditText et_serverAddr;
    private Context mContext;
    public static final int MSG_ONE = 1;
    private Handler handlerNTPTime = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_ONE:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mGetNTPTime();
                        }
                    }).start();
                    break;
                default:
                    break;
            }
        }
    };

    private Handler handlerSystemTime = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_ONE:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mGetSystemTime();
                        }
                    }).start();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        // Get the application context
        mContext = getApplicationContext();
        et_serverAddr.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Get the input method manager
                    InputMethodManager inputMethodManager = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    // Hide the soft keyboard
                    inputMethodManager.hideSoftInputFromWindow(et_serverAddr.getWindowToken(),0);
                    Toast.makeText(MainActivity.this, "授时开始...", Toast.LENGTH_SHORT).show();
                    // 获取编辑框的内容
                    serverAddr = et_serverAddr.getText().toString();
                    initTrueTimeRx(serverAddr);
                    new TrueTimeThread().start();
                    new SystemTimeThread().start();
                    //sleep(500);//初始化完 TrueTimeRx 后，调用 TrueTimeRx.isInitialized() 之前需要有一段时间的延时
                    //mGetTime();

                    handled = true;
                }
                return handled;
            }
        });
    }

    public class TrueTimeThread extends Thread{
        @Override
        public void run(){
            super.run();
            do {
                try{
                    Thread.sleep(1000);
                    Message msgTrueTime = new Message();
                    msgTrueTime.what = MSG_ONE;
                    handlerNTPTime.sendMessage(msgTrueTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(true);
        }
    }

    public class SystemTimeThread extends Thread{
        @Override
        public void run(){
            super.run();
            do {
                try{
                    Thread.sleep(1000);
                    Message msgSystemTime = new Message();
                    msgSystemTime.what = MSG_ONE;
                    handlerSystemTime.sendMessage(msgSystemTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(true);
        }
    }

    /**
     * TrueTimeRx 初始化
     * */
    public void initTrueTimeRx(String serverAddr){
        TrueTimeRx.build()                   //校园网NTP服务器地址：10.103.199.3
                .initializeRx(serverAddr)    //ntp1.aliyun.com  120.25.115.20
                .subscribeOn(Schedulers.io())
                .subscribe(date -> {
                    Log.v(TAG, "TrueTime was initialized and we have a time: " + date);
                }, throwable -> {
                    throwable.printStackTrace();
                });

    }
    /**
     * 从 NTP 服务器获取网络时间
     * */
    public void mGetNTPTime() {
        if(!TrueTimeRx.isInitialized()){
            Toast.makeText(this, "Sorry, TrueTime not yet initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        Date dateTrueTime = TrueTimeRx.now();
        //ntpTime.setText("1970年01月01日 00:00:00");
        //ntpTime.setText(dateToString(dateTrueTime, "yyyy-MM-dd HH:mm:ss"));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_ntpTime.setText(dateToString(dateTrueTime, "yyyy-MM-dd HH:mm:ss.SSS"));
            }
        });
    }

    public void mGetSystemTime() {
        Date dateSystemTime = new Date(System.currentTimeMillis());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_systemTime.setText(dateToString(dateSystemTime, "yyyy-MM-dd HH:mm:ss.SSS"));
            }
        });
    }

    private void initView() {
        tv_ntpTime = findViewById(R.id.tv_ntp_time);
        tv_systemTime = findViewById(R.id.tv_system_time);
        et_serverAddr = findViewById(R.id.et_server_addr);
    }

    // formatType格式为yyyy-MM-dd HH:mm:ss（yyyy年MM月dd日 HH时mm分ss秒）
    // data Date类型的时间
    public String dateToString(Date data, String formatType) {
        return new SimpleDateFormat(formatType).format(data);
    }
}