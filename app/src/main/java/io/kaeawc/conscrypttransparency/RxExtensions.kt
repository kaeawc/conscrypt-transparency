package io.kaeawc.conscrypttransparency

import arrow.core.*
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit
import kotlin.math.pow

fun <A> Single<A>.mapTry(): Single<Try<A>> {
    return this.map { result -> Try { result } }
        .onErrorReturn { Failure(it) }
}

fun <A, B> Single<Try<A>>.remap(mapper: (A) -> B): Single<Try<B>> {
    return this.map { result -> result.map { mapper(it) } }
}

fun <A, B> Single<Try<A>>.reflatMap(error: (Throwable) -> Single<Try<B>>, success: (A) -> Single<Try<B>>): Single<Try<B>> {
    return this.flatMap {
            result ->
        when (result) {
            is Success -> success(result.value)
            is Failure -> error(result.exception)
        }
    }
}

fun <A, B> Single<Try<A>>.reflatMap(success: (A) -> Single<Try<B>>): Single<Try<B>> {
    return this.flatMap {
            result ->
        when (result) {
            is Success -> success(result.value)
            is Failure -> Single.just(Failure(result.exception))
        }
    }
}

fun <A> Single<Try<A>>.foldRight(mapper: (A) -> Unit): Single<Try<A>> {
    return this.doOnSuccess {
            result ->
        result.fold({}, mapper)
    }
}

fun <A> Single<Try<A>>.foldLeft(mapper: (Throwable) -> Unit): Single<Try<A>> {
    return this.doOnSuccess {
            result ->
        result.fold(mapper, {})
    }
}

fun <A> Single<Try<A>>.fold(error: (Throwable) -> Unit, success: (A) -> Unit): Single<Try<A>> {
    return this.doOnSuccess {
            result ->
        result.fold(error, success)
    }
}

fun <T> SingleEmitter<Try<T>>.emit(result: Try<T>) {
    if (!isDisposed) onSuccess(result)
}

fun <T> trySingle(result: () -> T): Single<Try<T>> {
    return Single.create { e -> e.emit(Try { result() }) }
}

/**
 * Any [Failure] or errors in the stream will trigger [Single.retryWhen] to attempt
 * to recover any recoverable errors at least once and up to the maximum number
 * of retries allowed. It will then ensure that any errors in the stream are
 * re-wrapped as [Failure].
 */
fun <T> Single<Try<T>>.retryWithBackoff(maxRetries: Int): Single<Try<T>> {
    return this.throwFailures()
        .retryWhen { errors -> errors.attemptDelayedRecovery(maxRetries) }
        .catchFailures()
}

/**
 * In some cases we need to allow for throwing failures in a stream
 */
private fun <T> Single<Try<T>>.throwFailures(): Single<Try<T>> {

    return this.map { result ->
        when (result) {
            is Success -> Try { result.value }
            is Failure -> throw result.exception
        }
    }
}

/**
 * In some cases we need to catch for failures thrown in a stream.
 */
fun <T> Single<Try<T>>.catchFailures(): Single<Try<T>> {
    return this.onErrorReturn(::Failure)
}

fun <A> Flowable<A>.mapTry(): Flowable<Try<A>> {
    return this.map { result -> Try { result } }
        .onErrorReturn { Failure(it) }
}

fun <A, B> Flowable<Try<A>>.remap(mapper: (A) -> B): Flowable<Try<B>> {
    return this.map { result -> result.map { mapper(it) } }
}

fun <A, B> Flowable<Try<A>>.reflatMap(error: (Throwable) -> Flowable<Try<B>>, success: (A) -> Flowable<Try<B>>): Flowable<Try<B>> {
    return this.flatMap {
            result ->
        when (result) {
            is Success -> success(result.value)
            is Failure -> error(result.exception)
        }
    }
}

fun <A, B> Flowable<Try<A>>.reflatMap(success: (A) -> Flowable<Try<B>>): Flowable<Try<B>> {
    return this.flatMap {
            result ->
        when (result) {
            is Success -> success(result.value)
            is Failure -> Flowable.just(Failure(result.exception))
        }
    }
}

fun <A> Flowable<Try<A>>.foldRight(mapper: (A) -> Unit): Flowable<Try<A>> {
    return this.doOnNext { result ->
        result.fold({}, mapper)
    }
}

fun <A> Flowable<Try<A>>.foldLeft(mapper: (Throwable) -> Unit): Flowable<Try<A>> {
    return this.doOnNext {
            result ->
        result.fold(mapper, {})
    }
}

fun <A> Flowable<Try<A>>.fold(error: (Throwable) -> Unit, success: (A) -> Unit): Flowable<Try<A>> {
    return this.doOnNext {
            result ->
        result.fold(error, success)
    }
}

/**
 * Given a stream of [Throwable] we will:
 *  - Map it to a range of possible retries
 *  - Filter out unrecoverable errors
 *  - Delay the next retry attempt with an exponential backoff
 */
fun Flowable<Throwable>.attemptDelayedRecovery(maxRetries: Int): Flowable<Long> {

    // Only allow a certain number of retries with a minimum of at least one.
    return Flowable.zip(
        this,
        Flowable.range(1, maxRetries.coerceAtLeast(1)),
        BiFunction { error: Throwable, retryCount: Int -> error to retryCount })

        // Filter out any errors that we know are not recoverable
        // or throw if we cant recover
        .filter { (error: Throwable, _: Int)  ->
            if(error.isRecoverable() ) {
                true
            } else {
                throw error
            }
        }

        // Determine how long to delay before the next retry
        .map { (_: Throwable, retryCount: Int) -> retryCount.toDouble().pow(2.0).toLong() }

        // FlatMap over a timer with the given delay before allowing a retry
        .flatMap { delay: Long -> Flowable.timer(delay, TimeUnit.SECONDS) }
}
