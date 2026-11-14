package com.example.data.engine

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChessApiRequest(
    val fen: String,
    val depth: Int = 14,
    val maxThinkingTime: Int = 80,
    val variants: Int = 1,
    val searchmoves: String? = null
)

@JsonClass(generateAdapter = true)
data class ChessApiResponse(
    val type: String? = null,
    val text: String? = null,
    @Json(name = "eval") val evalScore: Double? = null,
    val centipawns: String? = null,
    val mate: Int? = null,
    val winChance: Double? = null,
    val depth: Int? = null,
    val move: String? = null,
    val san: String? = null,
    val from: String? = null,
    val to: String? = null,
    val continuationArr: List<String>? = null,
    val error: String? = null
)

interface StockfishApiService {
    @POST("v1")
    suspend fun evaluatePosition(@Body request: ChessApiRequest): ChessApiResponse

    companion object {
        private const val BASE_URL = "https://chess-api.com/"

        fun create(): StockfishApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(6, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(StockfishApiService::class.java)
        }
    }
}
