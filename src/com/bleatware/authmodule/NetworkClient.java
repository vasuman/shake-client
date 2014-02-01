package com.bleatware.authmodule;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * AuthModule
 * User: vasuman
 * Date: 1/31/14
 * Time: 12:08 AM
 */
public class NetworkClient {
    public String auth(String username, String password) {
        try {
            HashMap<String, String> map = new HashMap<String, String>(2);
            map.put("username", username);
            map.put("password", password);
            JSONObject object = makeRequest("/auth", map);
            if(object.getInt("code") != 0) {
                return null;
            }
            return object.getString("token");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class PollResult {
        public float amount;
        public int id;

        public PollResult(float amount, int id) {
            this.amount = amount;
            this.id = id;
        }
    }
    private HttpClient client;
    public static final String DOMAIN = "http://payment.paypal.com:8000";
    public NetworkClient() {
        client = new DefaultHttpClient();
    }

    public PollResult poll(String uname) {
        try {
            HashMap<String, String> map = new HashMap<String, String>(1);
            map.put("token", uname);
            JSONObject object = makeRequest("/poll", map);
            int id = object.getInt("code");
            float amount = 0;
            if(id == 103) {
                    amount = (float) object.getDouble("amount");
            }
            return new PollResult(amount, id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject makeRequest(String method, HashMap<String, String> params) throws Exception {
        HttpPost post = new HttpPost(DOMAIN + method);
        List<NameValuePair> postArgs = new ArrayList<NameValuePair>(params.size());
        for(HashMap.Entry<String, String> entry: params.entrySet()) {
            postArgs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        post.setEntity(new UrlEncodedFormEntity(postArgs));
        HttpResponse response = client.execute(post);
        return new JSONObject(EntityUtils.toString(response.getEntity()));
    }

    public void confirmPayment(String token, boolean confirm) {
        try {
            HashMap<String, String> map = new HashMap<String, String>(1);
            map.put("confirm", Boolean.toString(confirm));
            map.put("token", token);
            makeRequest("/confirm", map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
