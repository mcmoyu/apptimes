package com.moyu.apptimes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.List;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    UseTimeDataManager mUseTimeDataManager;
    String APPID = "27cf70ce161fce68b45127b9bc9ad08d";
    String id = "";
    TextView tv;
    ProgressBar pb;
    MyAudioRecord myAudioRecord;
    Double[] arr = new Double[200];
    int index = 0;

    Button btn;
    Button get;
    Button openMic;
    Button getDb;
    Button getData;

    private static String[] PERMISSIONS_STORAGE = {android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO};
    private static int REQUEST_PERMISSION_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(this);
        get = findViewById(R.id.get);
        get.setOnClickListener(this);
        openMic = findViewById(R.id.openMic);
        openMic.setOnClickListener(this);
        getDb = findViewById(R.id.getDb);
        getDb.setOnClickListener(this);
        getData = findViewById(R.id.getData);
        getData.setOnClickListener(this);
        tv = findViewById(R.id.tv);
        pb = findViewById(R.id.pb);
        setRxJavaErrorHandler();
        Bmob.initialize(this, APPID);
        myAudioRecord = new MyAudioRecord(this);
    }

    public String[] getJsonObjectStr() {
        String jsonAppdeTails = "";
        String time = "";
        String count = "";
        String data[] = new String[3];
        try {
        List<PackageInfo> packageInfos = mUseTimeDataManager.getmPackageInfoListOrderByTime();
            JSONObject jsonObject2 = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < packageInfos.size(); i++) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonArray.put(i, jsonObject.accumulate("appname", packageInfos.get(i).getmAppName()));
                    jsonArray.put(i, jsonObject.accumulate("name", packageInfos.get(i).getmPackageName()));
                    jsonArray.put(i, jsonObject.accumulate("time", ("" + packageInfos.get(i).getmUsedTime()/60000.0).substring(0,("" + packageInfos.get(i).getmUsedTime()/60000.0).indexOf('.')+2)));
                    jsonArray.put(i, jsonObject.accumulate("count", packageInfos.get(i).getmUsedCount()));
                    if(packageInfos.get(i).getmPackageName().equals("com.tencent.tmgp.sgame")){
                        time = ("" + packageInfos.get(i).getmUsedTime()/60000.0).substring(0,("" + packageInfos.get(i).getmUsedTime()/60000.0).indexOf('.')+2);
                        count = "" + packageInfos.get(i).getmUsedCount();
                        data[0] = time + " min";
                        data[1] = count + " times";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            jsonObject2.put("details", jsonArray);
            jsonAppdeTails = jsonObject2.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        data[2] = jsonAppdeTails;
        return data;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                mUseTimeDataManager = UseTimeDataManager.getInstance(MainActivity.this);
                mUseTimeDataManager.refreshData(0);
                String[] data = getJsonObjectStr();
                AppTimes appTimes = new AppTimes();
                appTimes.setTime(data[0]);
                appTimes.setCount(data[1]);
                appTimes.setOthers(data[2]);
                if("".equals(id)) {
                    appTimes.save(new SaveListener<String>() {
                        @Override
                        public void done(String s, BmobException e) {
                            if (e == null) {
                                id = s;
                                Toast.makeText(MainActivity.this, "签到成功！", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Code:" + e.getErrorCode() + "," + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    appTimes.update(id, new UpdateListener() {
                        @Override
                        public void done(BmobException e) {
                            if(e==null){
                                Toast.makeText(MainActivity.this, "签到成功！", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Code:" + e.getErrorCode() + "," + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                break;
            case R.id.get:
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                Toast.makeText(this,"找到【签到】然后点击[允许]后，返回再点[签到]按钮" , Toast.LENGTH_SHORT).show();
                break;
            case R.id.getDb:
                getWzry();
                myAudioRecord.getNoiseLevel();
                break;
            case R.id.openMic:
                open();
                getWzry();
                Toast.makeText(this, "请点击始终允许", Toast.LENGTH_LONG).show();
                break;
            case R.id.getData:
                Double min=100.0,avg=0.0,max=0.0;
                for(int i = 0; i < arr.length; i++){
                    if(i == 0 && arr[0]==null) {
                        min = avg = max = arr[0];
                    } else {
                        min = min<=arr[i]?min:arr[i];
                        max = max>=arr[i]?max:arr[i];
                        avg += arr[i];
                    }
                }
                TextView show = findViewById(R.id.show);
                show.setText("Min:" + turnPointTwo(min) + "\nAvg:" + turnPointTwo(avg/arr.length) + "\nMax:" + turnPointTwo(max));
//                show.setText(String.valueOf(index) + String.valueOf(arr[index]));
                break;
        }
    }

    private String turnPointTwo(Double d){
        String s = String.valueOf(d);
        if(s.length() - s.indexOf(".") > 2){
            s = s.substring(0,s.indexOf(".") + 3);
        }
        return s;
    }

    private void getWzry() {
        mUseTimeDataManager = UseTimeDataManager.getInstance(MainActivity.this);
        mUseTimeDataManager.refreshData(0);
        String[] data = getJsonObjectStr();
        AppTimes appTimes = new AppTimes();
        appTimes.setTime(data[0]);
        appTimes.setCount(data[1]);
        appTimes.setOthers(data[2]);
        if("".equals(id)) {
            appTimes.save(new SaveListener<String>() {
                @Override
                public void done(String s, BmobException e) {
                    if (e == null) {
                        id = s;
//                        Toast.makeText(MainActivity.this, "签到成功！", Toast.LENGTH_SHORT).show();
                    } else {
//                        Toast.makeText(MainActivity.this, "Code:" + e.getErrorCode() + "," + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            appTimes.update(id, new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if(e==null){
//                        Toast.makeText(MainActivity.this, "签到成功！", Toast.LENGTH_SHORT).show();
                    } else {
//                        Toast.makeText(MainActivity.this, "Code:" + e.getErrorCode() + "," + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void setRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.d("Main", "Throw test");
            }
        });
    }

    public void open(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            for (int i = 0 ; i < PERMISSIONS_STORAGE.length ; i++){
                if (ActivityCompat.checkSelfPermission(this,
                        PERMISSIONS_STORAGE[i]) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
                }
            }
        }
    }
}
