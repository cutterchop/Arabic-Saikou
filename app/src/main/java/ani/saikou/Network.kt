package ani.saikou

import android.content.Context
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.addGenericDns
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.io.Serializable
import kotlin.reflect.KFunction

val defaultHeaders = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"
)
lateinit var cache: Cache

lateinit var okHttpClient: OkHttpClient
lateinit var client: Requests

fun initializeNetwork(context: Context) {
    val dns = loadData<Int>("settings_dns",context)
    cache = Cache(
        File(context.cacheDir, "http_cache"),
        50L * 1024L * 1024L // 50 MiB
    )
    okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(cache)
        .apply {
            when (dns) {
                1 -> addGoogleDns()
                2 -> addCloudFlareDns()
                3 -> addAdGuardDns()
            }
        }
        .build()
    client = Requests(
        okHttpClient,
        defaultHeaders =  defaultHeaders)
}

val mapper = Requests.mapper

//fun <K, V, R> Map<out K, V>.asyncMap(f: suspend (Map.Entry<K, V>) -> R): List<R> = runBlocking {
//    map { withContext(Dispatchers.IO) { async { f(it) } } }.map { it.await() }
//}

fun <A, B> Collection<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

fun logError(e: Exception) {
    toastString(e.localizedMessage)
    e.printStackTrace()
}

fun <T> tryWith(call: () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Exception) {
        logError(e)
        null
    }
}

suspend fun <T> tryWithSuspend(call: suspend () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Exception) {
        logError(e)
        null
    }
}

/**
 * A url, which can also have headers
 * **/
data class FileUrl(
    val url: String,
    val headers: Map<String, String> = mapOf()
) : Serializable

//Credits to leg
data class Lazier<T>(
    val lClass: KFunction<T>
) {
    val get = lazy { lClass.call() }
}

fun <T> lazyList(vararg objects: KFunction<T>): List<Lazier<T>> {
    return objects.map {
        Lazier(it)
    }
}

//Kangings from CS333333333333

fun OkHttpClient.Builder.addGoogleDns() = (
        addGenericDns(
            "https://dns.google/dns-query",
            listOf(
                "8.8.4.4",
                "8.8.8.8"
            )
        ))

fun OkHttpClient.Builder.addCloudFlareDns() = (
        addGenericDns(
            "https://cloudflare-dns.com/dns-query",
            listOf(
                "1.1.1.1",
                "1.0.0.1",
                "2606:4700:4700::1111",
                "2606:4700:4700::1001"
            )
        ))

fun OkHttpClient.Builder.addAdGuardDns() = (
        addGenericDns(
            "https://dns.adguard.com/dns-query",
            listOf(
                // "Non-filtering"
                "94.140.14.140",
                "94.140.14.141",
            )
        ))