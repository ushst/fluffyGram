package org.ushastoe.fluffy.helpers;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IpApiHelper {
    private static final Gson gson = new Gson();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static OkHttpClient okHttpClient;

    public static void getIpInfo(String ipAddress, BiConsumer<IpInfoResponse, Exception> callback) {
        String cleanIp = ipAddress.replaceFirst("^(https?://)", "");
        Log.d("fluffy", "querying " + cleanIp);
        executorService.submit(() -> {
            var client = getOkHttpClient();
            var request = new Request.Builder()
                    .url("http://ip-api.com/json/" + cleanIp)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("fluffy", "response: " + response.body());
                    var responseBody = response.body().string();
                    IpInfoResponse ipInfo = gson.fromJson(responseBody, IpInfoResponse.class);
                    callback.accept(ipInfo, null);
                } else {
                    callback.accept(null, new Exception("Request failed with code: " + response.code()));
                }
            } catch (Exception e) {
                callback.accept(null, e);
            }
        });
    }

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            var builder = new OkHttpClient.Builder();
            builder.connectTimeout(120, TimeUnit.SECONDS);
            builder.readTimeout(120, TimeUnit.SECONDS);
            builder.writeTimeout(120, TimeUnit.SECONDS);
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    public static class IpInfoResponse {
        @SerializedName("query")
        @Expose
        public String query;

        @SerializedName("status")
        @Expose
        public String status;

        @SerializedName("country")
        @Expose
        public String country;

        @SerializedName("countryCode")
        @Expose
        public String countryCode;

        @SerializedName("region")
        @Expose
        public String region;

        @SerializedName("regionName")
        @Expose
        public String regionName;

        @SerializedName("city")
        @Expose
        public String city;

        @SerializedName("zip")
        @Expose
        public String zip;

        @SerializedName("lat")
        @Expose
        public double lat;

        @SerializedName("lon")
        @Expose
        public String lon;

        @SerializedName("timezone")
        @Expose
        public String timezone;

        @SerializedName("isp")
        @Expose
        public String isp;

        @SerializedName("org")
        @Expose
        public String org;

        @SerializedName("as")
        @Expose
        public String as;

        @Override
        public String toString() {
            return "query: " + query + "\n" +
                    "country: " + country + "\n" +
                    "city: " + city + "\n" +
                    "lat: " + lat + "\n" +
                    "lon: " + lon;
        }
    }
}