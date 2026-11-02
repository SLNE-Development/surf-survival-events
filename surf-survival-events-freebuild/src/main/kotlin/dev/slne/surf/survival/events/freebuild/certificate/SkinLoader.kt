package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.survival.events.freebuild.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
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
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

/** Loads the face of a player's current skin without blocking any server thread. */
object SkinLoader {
    private const val TEXTURE_HOST = "textures.minecraft.net"
    private val timeout = 15.seconds
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** Returns the 8x8 face of the player's skin, or `null` if the player has no custom skin or loading failed. */
    suspend fun loadFace(uuid: UUID): BufferedImage? = try {
        withTimeout(timeout) {
            val profile = Bukkit.createProfile(uuid).update().await()
            val skinUrl = profile.textures.skin ?: return@withTimeout null
            val skin = download(skinUrl.toURI())
            SkinHead.extractFace(skin)
        }
    } catch (_: TimeoutCancellationException) {
        plugin.logger.warning("Loading the skin of $uuid timed out, using the fallback face")
        null
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        plugin.logger.log(Level.WARNING, "Could not load the skin of $uuid, using the fallback face", exception)
        null
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
