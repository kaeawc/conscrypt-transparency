package io.kaeawc.conscrypttransparency.api

import io.kaeawc.conscrypttransparency.okhttp.Empty
import io.reactivex.Single
import retrofit2.http.GET

interface GenericApi {

    @GET("/")
    fun get(): Single<Empty>
}
