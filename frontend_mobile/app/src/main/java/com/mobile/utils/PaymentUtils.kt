package com.mobile.utils

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Utility class for handling payments with Paymongo
 * This is a simplified implementation for demonstration purposes.
 * In a production app, you would use the official Paymongo SDK.
 */
object PaymentUtils {
    // Paymongo API credentials and URLs
    private const val PAYMONGO_API_URL = "https://api.paymongo.com/v1"
    private const val API_KEY = "PUT_YOUR_API_KEY_HERE" // Replace with your Paymongo API key
    
    /**
     * Data class for payment source
     */
    data class PaymentSource(
        val id: String,
        val type: String,
        val amount: Double,
        val currency: String,
        val status: String,
        val reference: String?
    )
    
    /**
     * Data class for payment method
     */
    data class PaymentMethod(
        val id: String,
        val type: String,
        val details: Map<String, String>
    )
    
    /**
     * Data class for payment intent
     */
    data class PaymentIntent(
        val id: String,
        val amount: Double,
        val currency: String,
        val status: String,
        val paymentMethodId: String?,
        val clientKey: String?
    )
    
    /**
     * Creates a payment source using credit card information
     * @param amount Amount to charge in PHP (minimum 100)
     * @param description Description of the transaction
     * @param callbackUrl URL to redirect after payment
     * @return PaymentSource object if successful, null otherwise
     */
    @WorkerThread
    suspend fun createPaymentSource(
        amount: Double,
        description: String,
        callbackUrl: String
    ): Result<PaymentSource> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert amount to smallest currency unit (centavos)
                val amountInCentavos = (amount * 100).toInt()
                
                // Prepare request body
                val requestBodyJson = JSONObject().apply {
                    put("data", JSONObject().apply {
                        put("attributes", JSONObject().apply {
                            put("type", "gcash")
                            put("amount", amountInCentavos)
                            put("currency", "PHP")
                            put("redirect", JSONObject().apply {
                                put("success", callbackUrl)
                                put("failed", callbackUrl)
                            })
                            put("billing", JSONObject().apply {
                                put("name", "Tutoring Session")
                                put("email", "customer@email.com")
                                put("phone", "09123456789")
                            })
                        })
                    })
                }
                
                // Create connection
                val url = URL("$PAYMONGO_API_URL/sources")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Basic ${getBase64AuthString()}")
                connection.doOutput = true
                
                // Write request body
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBodyJson.toString())
                    writer.flush()
                }
                
                // Read response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseJson)
                    val data = jsonObject.getJSONObject("data")
                    val id = data.getString("id")
                    val attributes = data.getJSONObject("attributes")
                    
                    val source = PaymentSource(
                        id = id,
                        type = attributes.getString("type"),
                        amount = attributes.getDouble("amount") / 100, // Convert back to PHP
                        currency = attributes.getString("currency"),
                        status = attributes.getString("status"),
                        reference = if (attributes.has("reference")) attributes.getString("reference") else null
                    )
                    
                    Result.success(source)
                } else {
                    Log.e("PaymentUtils", "Error creating payment source: ${connection.responseMessage}")
                    Result.failure(Exception("Failed to create payment source. Code: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("PaymentUtils", "Error in createPaymentSource", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Retrieves a payment source by ID
     * @param sourceId ID of the payment source
     * @return PaymentSource object if successful, null otherwise
     */
    @WorkerThread
    suspend fun getPaymentSource(sourceId: String): Result<PaymentSource> {
        return withContext(Dispatchers.IO) {
            try {
                // Create connection
                val url = URL("$PAYMONGO_API_URL/sources/$sourceId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Basic ${getBase64AuthString()}")
                
                // Read response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseJson)
                    val data = jsonObject.getJSONObject("data")
                    val id = data.getString("id")
                    val attributes = data.getJSONObject("attributes")
                    
                    val source = PaymentSource(
                        id = id,
                        type = attributes.getString("type"),
                        amount = attributes.getDouble("amount") / 100, // Convert back to PHP
                        currency = attributes.getString("currency"),
                        status = attributes.getString("status"),
                        reference = if (attributes.has("reference")) attributes.getString("reference") else null
                    )
                    
                    Result.success(source)
                } else {
                    Log.e("PaymentUtils", "Error getting payment source: ${connection.responseMessage}")
                    Result.failure(Exception("Failed to get payment source. Code: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("PaymentUtils", "Error in getPaymentSource", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Helper method to encode API key in Base64 for Authorization header
     */
    private fun getBase64AuthString(): String {
        val auth = "$API_KEY:"
        return android.util.Base64.encodeToString(
            auth.toByteArray(),
            android.util.Base64.NO_WRAP
        )
    }
} 