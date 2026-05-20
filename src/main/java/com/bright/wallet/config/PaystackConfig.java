package com.bright.wallet.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * PaystackConfig
 *
 * Binds Paystack credentials from application.properties / environment variables
 * and provides a shared OkHttpClient bean for all Paystack API calls.
 *
 * Properties required in application.properties (or environment):
 *   paystack.secret-key     = sk_test_xxxxxxxxxxxxxxxxxxxx
 *   paystack.base-url       = https://api.paystack.co
 *   paystack.callback-url   = https://yourdomain.com/payment/paystack/callback
 */
@Configuration
public class PaystackConfig {

    /** Paystack secret key — NEVER expose this to the frontend. */
    @Value("${paystack.secret-key}")
    private String secretKey;

    /** Paystack REST API base URL. */
    @Value("${paystack.base-url:https://api.paystack.co}")
    private String baseUrl;

    /**
     * Redirect URL after Paystack checkout completes.
     * Must be whitelisted in your Paystack dashboard → Settings → API Keys & Webhooks.
     */
    @Value("${paystack.callback-url}")
    private String callbackUrl;

    public String getSecretKey()   { return secretKey; }
    public String getBaseUrl()     { return baseUrl; }
    public String getCallbackUrl() { return callbackUrl; }

    /**
     * Shared OkHttpClient bean.
     * Timeouts are generous (30 s) to handle Paystack API latency gracefully.
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}