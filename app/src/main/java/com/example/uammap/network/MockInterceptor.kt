package com.example.uammap.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        return when {
            path.endsWith("campus.geojson") -> {
                val jsonString = context.assets.open("campus.geojson").bufferedReader().use { it.readText() }
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body(jsonString.toResponseBody("application/json".toMediaType()))
                    .addHeader("content-type", "application/json")
                    .build()
            }
            path.endsWith("location") -> {
                Response.Builder()
                    .code(202)
                    .message("Accepted")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            else -> chain.proceed(request)
        }
    }
}
