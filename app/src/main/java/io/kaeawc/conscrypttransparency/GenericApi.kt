package io.kaeawc.conscrypttransparency

import io.reactivex.Single
import retrofit2.http.GET

interface GenericApi {

    @GET("/")
    fun get(): Single<Empty>
}
