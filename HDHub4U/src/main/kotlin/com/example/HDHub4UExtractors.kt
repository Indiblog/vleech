// Ported faithfully from:
// https://github.com/phisher98/cloudstream-extensions-phisher/blob/master/HDhub4u/src/main/kotlin/com/hdhub4u/Extractors.kt
// Changes:
//   - package: com.hdhub4u → com.example
//   - HubCloud class renamed to HDHubCloud to avoid collision with Cinevood's HubCloud in com.example
//   - All extraction logic is identical.

package com.example

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.PixelDrain
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.fixUrl
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class HdStream4u : VidHidePro() {
    override var mainUrl = "https://hdstream4u.com"
}

open class VidStack : ExtractorApi() {
    override var name            = "Vidstack"
    override var mainUrl         = "https://vidstack.io"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val headers  = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0")
        val hash     = url.substringAfterLast("#").substringAfter("/")
        val baseurl  = getBaseUrl(url)
        val encoded  = app.get("$baseurl/api/v1/video?id=$hash", headers = headers).text.trim()
        val key      = "kiemtienmua911ca"
        val ivList   = listOf("1234567890oiuytr", "0123456789abcdef")
        val decrypted = ivList.firstNotNullOfOrNull { iv ->
            try { AesHelper.decryptAES(encoded, key, iv) } catch (_: Exception) { null }
        } ?: throw Exception("Failed to decrypt with all IVs")
        val m3u8 = Regex(""""source":"(.*?)"""").find(decrypted)?.groupValues?.get(1)?.replace("\\/", "/") ?: ""
        val subSection = Regex(""""subtitle":\{(.*?)\}""").find(decrypted)?.groupValues?.get(1)
        subSection?.let { section ->
            Regex(""""([^"]+)":\s*"([^"]+)"""").findAll(section).forEach { m ->
                val lang    = m.groupValues[1]
                val rawPath = m.groupValues[2].split("#")[0]
                if (rawPath.isNotEmpty()) subtitleCallback(newSubtitleFile(lang, fixUrl(mainUrl + rawPath.replace("\\/", "/"))))
            }
        }
        callback(newExtractorLink(name, name, m3u8.replace("https", "http"), ExtractorLinkType.M3U8) {
            this.referer = url; this.quality = Qualities.Unknown.value
        })
    }

    private fun getBaseUrl(url: String): String =
        try { URI(url).let { "${it.scheme}://${it.host}" } } catch (e: Exception) { Log.e("Vidstack", e.message ?: ""); mainUrl }
}

object AesHelper {
    private const val TRANSFORMATION = "AES/CBC/PKCS5PADDING"
    fun decryptAES(inputHex: String, key: String, iv: String): String {
        val cipher  = Cipher.getInstance(TRANSFORMATION)
        val sKey    = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec  = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        cipher.init(Cipher.DECRYPT_MODE, sKey, ivSpec)
        return String(cipher.doFinal(inputHex.hexToByteArray()), Charsets.UTF_8)
    }
    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}

class Hubstream : VidStack() {
    override var mainUrl = "https://hubstream.*"
}

class Hubstreamdad : Hblinks() {
    override var mainUrl = "https://hblinks.*"
}

open class Hblinks : ExtractorApi() {
    override val name            = "Hblinks"
    override val mainUrl         = "https://hblinks.*"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        app.get(url).document.select("h3 a,h5 a,div.entry-content p a").amap {
            val href  = it.absUrl("href").ifBlank { it.attr("href") }
            val lower = href.lowercase()
            when {
                "hubdrive"  in lower -> Hubdrive().getUrl(href, name, subtitleCallback, callback)
                "hubcloud"  in lower -> HDHubCloud().getUrl(href, name, subtitleCallback, callback)
                "hubcdn"    in lower -> HUBCDN().getUrl(href, name, subtitleCallback, callback)
                else                 -> loadSourceNameExtractor(name, href, "", Qualities.Unknown.value, subtitleCallback, callback)
            }
        }
    }
}

class Hubcdnn : ExtractorApi() {
    override val name            = "Hubcdn"
    override val mainUrl         = "https://hubcdn.*"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        app.get(url).document.toString().let {
            val encoded = Regex("r=([A-Za-z0-9+/=]+)").find(it)?.groups?.get(1)?.value
            if (!encoded.isNullOrEmpty()) {
                val m3u8 = base64Decode(encoded).substringAfterLast("link=")
                callback(newExtractorLink(name, name, m3u8, ExtractorLinkType.M3U8) { this.referer = url; this.quality = Qualities.Unknown.value })
            } else Log.e("Error", "Encoded URL not found")
        }
    }
}

class PixelDrainDev : PixelDrain() {
    override var mainUrl = "https://pixeldrain.dev"
}

class Hubdrive : ExtractorApi() {
    override val name            = "Hubdrive"
    override val mainUrl         = "https://hubdrive.space"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val href = app.get(url, timeout = 5000L).document.select(".btn.btn-primary.btn-user.btn-success1.m-1").attr("href")
        if (href.contains("hubcloud", ignoreCase = true)) HDHubCloud().getUrl(href, "HubDrive", subtitleCallback, callback)
        else loadExtractor(href, "HubDrive", subtitleCallback, callback)
    }
}

// Renamed from HubCloud to HDHubCloud to avoid symbol clash with Cinevood's HubCloud in the same package.
class HDHubCloud : ExtractorApi() {
    override val name            = "Hub-Cloud"
    override var mainUrl         = "https://hubcloud.foo"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val tag    = "HDHubCloud"
        val uri    = runCatching { URI(url) }.getOrElse { Log.e(tag, "Invalid URL: ${it.message}"); return }
        val realUrl = uri.toString()
        val baseUrl = "${uri.scheme}://${uri.host}"

        val href = runCatching {
            if ("hubcloud.php" in realUrl) realUrl
            else {
                val raw = app.get(realUrl).document.selectFirst("#download")?.attr("href").orEmpty()
                if (raw.startsWith("http", true)) raw else baseUrl.trimEnd('/') + "/" + raw.trimStart('/')
            }
        }.getOrElse { Log.e(tag, "Failed to extract href: ${it.message}"); "" }
        if (href.isBlank()) return

        val document     = app.get(href).document
        val size         = document.selectFirst("i#size")?.text().orEmpty()
        val header       = document.selectFirst("div.card-header")?.text().orEmpty()
        val headerDetail = cleanTitle(header)
        val quality      = getIndexQuality(header)
        val labelExtras  = buildString {
            if (headerDetail.isNotEmpty()) append("[$headerDetail]")
            if (size.isNotEmpty()) append("[$size]")
        }

        document.select("a.btn").amap { el ->
            val link  = el.attr("href")
            val label = el.ownText().lowercase()
            when {
                "fsl server"   in label -> callback(newExtractorLink("$referer [FSL Server]", "$referer [FSL Server] $labelExtras", link) { this.quality = quality })
                "download file" in label -> callback(newExtractorLink(referer ?: name, "${referer ?: name} $labelExtras", link) { this.quality = quality })
                "buzzserver"   in label -> {
                    val resp  = app.get("$link/download", referer = link, allowRedirects = false)
                    val dlink = resp.headers["hx-redirect"] ?: resp.headers["HX-Redirect"].orEmpty()
                    if (dlink.isNotBlank()) callback(newExtractorLink("$referer [BuzzServer]", "$referer [BuzzServer] $labelExtras", dlink) { this.quality = quality })
                }
                "pixeldra" in label || "pixel" in label -> {
                    val lBase   = getBaseUrl(link)
                    val finalUrl = if ("download" in link) link else "$lBase/api/file/${link.substringAfterLast("/")}?download"
                    callback(newExtractorLink("$referer Pixeldrain", "$referer Pixeldrain $labelExtras", finalUrl) { this.quality = quality })
                }
                "s3 server" in label   -> callback(newExtractorLink("$referer [S3 Server]", "$referer [S3 Server] $labelExtras", link) { this.quality = quality })
                else                   -> loadExtractor(link, "", subtitleCallback, callback)
            }
        }
    }

    private fun getIndexQuality(str: String?): Int =
        Regex("(\\d{3,4})[pP]").find(str.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Qualities.P2160.value

    private fun getBaseUrl(url: String): String =
        runCatching { URI(url).let { "${it.scheme}://${it.host}" } }.getOrDefault("")

    // Reuse the standalone cleanTitle() from HDHub4UUtils.kt — no duplication needed.
}

class HUBCDN : ExtractorApi() {
    override val name            = "HUBCDN"
    override val mainUrl         = "https://hubcdn.*"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val doc         = app.get(url).document
        val scriptText  = doc.selectFirst("script:containsData(var reurl)")?.data()
        val encodedUrl  = Regex("""reurl\s*=\s*"([^"]+)"""").find(scriptText ?: "")?.groupValues?.get(1)?.substringAfter("?r=")
        val decodedUrl  = encodedUrl?.let { base64Decode(it) }?.substringAfterLast("link=")
        if (decodedUrl != null) {
            callback(newExtractorLink(name, name, decodedUrl, INFER_TYPE) { this.quality = Qualities.Unknown.value })
        }
    }
}
