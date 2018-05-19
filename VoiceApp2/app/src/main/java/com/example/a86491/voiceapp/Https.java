package com.example.a86491.voiceapp;


import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import com.example.a86491.voiceapp.MainActivity;
/**
 * Created by 86491 on 2018/5/19.
 */
public class Https {
    private Context context;
    private static RequestQueue queue;
    public  String answer;

    public Https(Context context) {
        super();
        this.context = context;
        queue = Volley.newRequestQueue(context);

    }

    private static String API_KEY = "beb892fec3ea42b8a7877276b32dfd8b";
    private static String URL = "http://openapi.tuling123.com/openapi/api";

    private static String setParams(String msg) {
        try {
            msg = URLEncoder.encode(msg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return URL + "?key=" + API_KEY + "&info=" + msg;

    }
    public void doGet(String msg) {
        String url = setParams(msg);
        final StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    answer = obj.getString("text");
                    MainActivity.addResultLeft(answer);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d("jige","CommonRequest中出错了");
            }
        }
        );
        queue.add(request);
    }
}