package com.example.bid2buy.util

import android.content.Context
import com.example.bid2buy.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class ExchangeRateResponse(
    val result: String,
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("conversion_rates") val conversionRates: Map<String, Double>
)

interface ExchangeRateApiService {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    suspend fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): ExchangeRateResponse
}

class CurrencyManager private constructor(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("bid2buy_prefs", Context.MODE_PRIVATE)
    private val apiKey = BuildConfig.EXCHANGE_RATE_API_KEY
    
    private val api: ExchangeRateApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ExchangeRateApiService::class.java)
    }

    private var cachedRates: Map<String, Double>? = null
    private var lastFetchTime: Long = 0
    private var baseCurrency: String = "ILS"

    init {
        val ratesJson = sharedPrefs.getString("cached_rates", null)
        if (ratesJson != null) {
            val type = object : TypeToken<Map<String, Double>>() {}.type
            cachedRates = Gson().fromJson(ratesJson, type)
            lastFetchTime = sharedPrefs.getLong("last_fetch_time", 0)
        }
    }

    suspend fun fetchRatesIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (cachedRates != null && (currentTime - lastFetchTime) < TimeUnit.HOURS.toMillis(12)) {
            return
        }

        try {
            val response = api.getExchangeRates(apiKey, baseCurrency)
            if (response.result == "success") {
                cachedRates = response.conversionRates
                lastFetchTime = currentTime
                
                sharedPrefs.edit()
                    .putString("cached_rates", Gson().toJson(cachedRates))
                    .putLong("last_fetch_time", lastFetchTime)
                    .apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRates(): Map<String, Double>? = cachedRates

    /**
     * Converts an amount from one currency to another using the cached rates.
     * @param amount The value to convert.
     * @param fromCurrency The source currency code (e.g., "USD").
     * @param toCurrency The target currency code (e.g., "EUR").
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        val rates = cachedRates ?: return amount
        
        // 1. Convert from source currency to base currency (ILS)
        val amountInBase = if (fromCurrency == baseCurrency) {
            amount
        } else {
            val fromRate = rates[fromCurrency] ?: return amount
            amount / fromRate
        }
        
        // 2. Convert from base currency to target currency
        return if (toCurrency == baseCurrency) {
            amountInBase
        } else {
            val toRate = rates[toCurrency] ?: return amountInBase
            amountInBase * toRate
        }
    }

    fun getSelectedCurrency(): String {
        return sharedPrefs.getString("selected_currency", "ILS") ?: "ILS"
    }

    fun formatPrice(amount: Double, currencyCode: String): String {
        val symbol = when (currencyCode) {
            "ILS" -> "₪"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "AUD" -> "A$"
            "CAD" -> "C$"
            "CHF" -> "CHF"
            else -> currencyCode
        }
        return String.format("%.2f %s", amount, symbol)
    }

    companion object {
        @Volatile
        private var INSTANCE: CurrencyManager? = null

        fun getInstance(context: Context): CurrencyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrencyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
