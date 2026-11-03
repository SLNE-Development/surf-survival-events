package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.survival.events.freebuild.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.bukkit.Bukkit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

/** Result of [SkinLoader.loadFace]. */
sealed interface SkinFace {
    /** The 8x8 face of the player's skin. */
    data class Loaded(val image: BufferedImage) : SkinFace

    /** The player uses one of the default skins. */
    data object DefaultSkin : SkinFace

    /** The skin could not be loaded, for example because the session server refused further requests. */
    data object Unavailable : SkinFace
}

/**
 * Loads the face of a player's current skin without blocking any server thread.
 *
 * Paper keeps the profiles of known players in its profile cache, so most lookups do not reach the session server. A
 * failed lookup, for example because of its rate limit, is tried once more after a short pause.
 */
object SkinLoader {
    private const val TEXTURE_HOST = "textures.minecraft.net"
    private const val ATTEMPTS = 2
    private val timeout = 10.seconds
    private val retryDelay = 5.seconds
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun loadFace(uuid: UUID): SkinFace {
        var lastFailure: Exception? = null

        repeat(ATTEMPTS) { attempt ->
            if (attempt > 0) {
                delay(retryDelay)
            }

            try {
                return withTimeout(timeout) { loadOnce(uuid) }
            } catch (exception: TimeoutCancellationException) {
                lastFailure = exception
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                lastFailure = exception
            }
        }

        plugin.logger.warning("Could not load the skin of $uuid, using the fallback face: $lastFailure")
        return SkinFace.Unavailable
    }

    private suspend fun loadOnce(uuid: UUID): SkinFace {
        val profile = Bukkit.createProfile(uuid).update().await()
        check(profile.hasTextures()) { "The session server returned no textures" }

        val skinUrl = profile.textures.skin ?: return SkinFace.DefaultSkin
        return SkinFace.Loaded(SkinHead.extractFace(download(skinUrl.toURI())))
    }

    private suspend fun download(url: URI): BufferedImage {
        val request = HttpRequest.newBuilder(secure(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
        val response = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).await()
        check(response.statusCode() == 200) { "Skin download returned HTTP ${response.statusCode()}" }

        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(response.body())) ?: error("Skin is not a readable image")
        }
    }

    /** Mojang hands out plain http texture URLs; the texture server also serves them via https. */
    private fun secure(url: URI): URI =
        if (url.scheme == "http" && url.host == TEXTURE_HOST) URI("https", url.host, url.path, null) else url
}
