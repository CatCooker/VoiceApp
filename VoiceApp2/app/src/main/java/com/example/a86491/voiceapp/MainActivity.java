package com.example.a86491.voiceapp;

/**
 * Created by 86491 on 2018/5/15.
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.example.a86491.asrfinishjson.AsrFinishJsonData;
import com.example.a86491.asrpartialjson.AsrPartialJsonData;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.markushi.ui.CircleButton;

/**
 *  集成文档： http://ai.baidu.com/docs#/ASR-Android-SDK/top 集成指南一节
 *  demo目录下doc_integration_DOCUMENT
 *      ASR-INTEGRATION-helloworld  ASR集成指南-集成到helloworld中 对应 ActivityMiniRecog
 *      ASR-INTEGRATION-TTS-DEMO ASR集成指南-集成到合成DEMO中 对应 ActivityRecog
 */

public class MainActivity extends AppCompatActivity implements EventListener {
    /****************************************   parameter    **************************************************/
    private CircleButton myCircleButton;
    private static RecyclerView myRecyclerView;
    private ProgressBar myProgressBar;
    private static MsgAdapter  myMsgAdapter;
    private TextView myTextView;
    private  static List<Msg> myMsgList = new ArrayList<>();
    private String answer_r;
    private EventManager asr ;
    private boolean enableOffline = false; // 测试离线命令词，需要改成true
    private String final_result;
    public String partial_result;
    private Https https ;
    private static final String TAG = "RecogEventAdapter";
    /***************************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sorce_layout);
        initMsg();   // empty
        initView();
        initPermission();
       // listener  = new MessageStatusRecogListener(handler);
       // myRecognizer = new MyRecognizer(this,listener);
       // EventManagerFactory eventManagerFactory= new EventManagerFactory();
        asr = EventManagerFactory.create(this,"asr");
        asr.registerListener(this);
        https = new Https(this);
        myCircleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myTextView.getVisibility() == View.VISIBLE)
                {
                    myProgressBar.setVisibility(View.VISIBLE);
                    myTextView.setVisibility(View.GONE);
                    start();
                }
                else
                {
                    myTextView.setVisibility(View.VISIBLE);
                    myProgressBar.setVisibility(View.GONE);
                    stop();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        asr.send(SpeechConstant.ASR_CANCEL,null,null,0,0);
        if (enableOffline) {
            unloadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
    }
    /**********************************************************json to String***************************************/
    /**************************************Init******************************************************/
    private void initMsg()
    {

           Msg msg1 = new Msg("你好，我是Android,我能为你做什么?",Msg.TYPE_RECEIVED);
           myMsgList.add((msg1));

    }
    private void initView() {
        myCircleButton = (CircleButton) findViewById(R.id.mycirclebutton);
        myProgressBar = (ProgressBar) findViewById((R.id.myprogressbar));
        myTextView = (TextView) findViewById(R.id.mytext);
        myRecyclerView = (RecyclerView) findViewById(R.id.myrecyclerview);
        LinearLayoutManager myLinearLayoutManager = new LinearLayoutManager(this);
        myRecyclerView.setLayoutManager(myLinearLayoutManager);
        myMsgAdapter = new MsgAdapter(myMsgList);
        myRecyclerView.setAdapter(myMsgAdapter);
        myProgressBar.setVisibility(View.GONE);
    }

    /*******************begin to recognise*****************************/
    private void start() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String event = null;
        event = SpeechConstant.ASR_START; // 替换成测试的event

        if (enableOffline) {
            loadOfflineEngine();
        }
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 2000);
        params.put(SpeechConstant.DECODER,0);
        params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        params.put(SpeechConstant.PID, 1536);
       // params.put(SpeechConstant.NLU, "enable");
        // params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 0); // 长语音
        // params.put(SpeechConstant.IN_FILE, "res:///com/baidu/android/voicedemo/16k_test.pcm");
        // params.put(SpeechConstant.PROP ,20000);
        // 请先使用如‘在线识别’界面测试和生成识别参数。 params同ActivityRecog类中myRecognizer.start(params);
      asr.send(event,params.toString(),null,0,0);
    }
    /**************************************over**********************************************/
    private void stop() {
        final_result = "";
        answer_r = "";
        asr.send(SpeechConstant.ASR_STOP,null,null,0,0);
    }
    /***********************************load and unload offlineEngine()*********************************/
    private void loadOfflineEngine() {
        //addResultLeft("loadOfflineEngine");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.DECODER, 0);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH,"assets//baidu_speech_grammar.bsg");
       asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE,params.toString(),null,0,0);
    }

    private void unloadOfflineEngine() {
        asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE,null,null,0,0); //
    }
    /******************************************EventListener  回调方法**************************************/
    private void parseAsrPartialJsonData(String data) {
        Gson gson = new Gson();
        AsrPartialJsonData jsonData = gson.fromJson(data, AsrPartialJsonData.class);
        String resultType = jsonData.getResult_type();
        if(resultType != null && resultType.equals("final_result")){
            {
               partial_result = jsonData.getBest_result();

                //addResultLeft("at parsePartial");
                // addResult(final_result);
            }
        }
    }
    private void parseAsrFinishJsonData(String data) {
        Gson gson = new Gson();
        AsrFinishJsonData jsonData = gson.fromJson(data, AsrFinishJsonData.class);
        String desc = jsonData.getDesc();
        if(desc !=null && desc.equals("Speech Recognize success.")){
            //addResultLeft("at parseFinish");
            final_result = partial_result;
        }else{
            String errorCode = "\n错误码:" + jsonData.getError();
            String errorSubCode = "\n错误子码:"+ jsonData.getSub_error();
            String errorResult = errorCode + errorSubCode;
            //addResultLeft("");
        }
    }
    /*******************************************************************************************************************/

    public static void addResultRight(String result)
    {
        Msg myMsg = new Msg(result,Msg.TYPE_SENT);
        myMsgList.add(myMsg);
        myMsgAdapter.notifyItemInserted(myMsgList.size() - 1);
        myRecyclerView.scrollToPosition(myMsgList.size() - 1);
    }
    public static void addResultLeft(String answer)
    {
        Msg myMsg = new Msg(answer,Msg.TYPE_RECEIVED);
        myMsgList.add(myMsg);
        myMsgAdapter.notifyItemInserted(myMsgList.size() - 1);
        myRecyclerView.scrollToPosition(myMsgList.size() - 1);
    }
    @Override
    public void onEvent(String name, String param, byte[] data, int offset, int length) {
        String eventName = "";
        String parameters ="";
        //addResultLeft(param);
        if(length > 0 && data.length > 0)
        {
            eventName += ",语义解析结果："+ new String(data,offset,length);
            //  parseAsrResultJsonData(param);
           // addResultLeft(final_answer);
        }
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY))
        {
            eventName += "CALLBACK_EVENT_ASR_READY";
            //addResultLeft(eventName);
        }
        else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN))
        {
            eventName += "CALLBACK_EVENT_ASR_BEGIN";
           // addResultLeft(eventName);
        }
        else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END))
        {
            eventName += "CALLBACK_EVENT_ASR_END";
           // addResultLeft(eventName);
            if (param != null && !param.isEmpty())
            {
                parameters += "params:" + param + "\n";
                //addResultLeft(parameters);
            }
        }
        else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL))
        {
            eventName +="CALLBACK_EVENT_ASR_PARTIAL";
            //addResultLeft(eventName);
            if (param != null && !param.isEmpty())
            {
                parameters += "params:" + param + "\n";
               // addResultLeft(parameters);
            }
            parseAsrPartialJsonData(param);
        }
        else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH))
        {
            eventName += "CALLBACK_EVENT_ASR_FINISH";
            //addResultLeft(eventName);
            myCircleButton.setEnabled(true);
            asr.send(SpeechConstant.ASR_STOP,null,null,0,0);
            if (param != null && !param.isEmpty())
            {
                parameters += "param:" + param + "\n";
            }
            parseAsrFinishJsonData(param);
            https.doGet(final_result);
            addResultRight(final_result);
        }
    }
    /********************************************Interface***************************************/
    /**
     * ASR_START 输入事件调用后，引擎准备完毕
     */


    /******************************************Add Result**************************************/

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。

    }

}
