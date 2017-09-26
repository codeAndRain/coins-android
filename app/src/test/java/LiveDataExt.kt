import android.arch.lifecycle.LiveData
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Igor Bubelov
 */

fun <T> LiveData<T>.blockingObserve(observationsCount: Int = 1): T {
    var value: T? = null
    val latch = CountDownLatch(observationsCount)

    observeForever({
        value = it
        latch.countDown()
    })

    latch.await(10, TimeUnit.SECONDS)
    @Suppress("UNCHECKED_CAST")
    return value as T
}