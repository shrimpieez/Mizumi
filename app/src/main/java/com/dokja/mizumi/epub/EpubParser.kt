package com.dokja.mizumi.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.invariantSeparatorsPathString

//Models
data class EpubChapter(val absPath: String, val title: String, val body: String)
data class EpubBook(
    val fileName: String,
    val title: String,
    val coverImage: EpubImage?,
    val chapters: List<EpubChapter>,
    val images: List<EpubImage>
)
data class EpubImage(val absPath: String, val image: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpubImage

        if (absPath != other.absPath) return false
        return image.contentEquals(other.image)
    }

    override fun hashCode(): Int {
        var result = absPath.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}

data class EpubManifestItem(
    val id: String, val absPath: String, val mediaType: String, val properties: String
)

data class TempEpubChapter(
    val url: String, val title: String?, val body: String, val chapterIndex: Int
)

data class EpubFile(val absPath: String, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpubFile

        if (absPath != other.absPath) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = absPath.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}


private suspend fun getZipFiles(
    inputStream: InputStream
): Map<String, EpubFile> = withContext(Dispatchers.IO) {
    ZipInputStream(inputStream).use { zipInputStream ->
        zipInputStream
            .entries()
            .filterNot { it.isDirectory }
            .map { EpubFile(absPath = it.name, data = zipInputStream.readBytes()) }
            .associateBy { it.absPath }
    }
}

@Throws(Exception::class)
suspend fun epubCoverParser(
    inputStream: InputStream
): EpubImage? = withContext(Dispatchers.Default) {
    val files = getZipFiles(inputStream)

    val container = files["META-INF/container.xml"]
        ?: throw Exception("META-INF/container.xml file missing")

    val opfFilePath = parseXMLFile(container.data)
        ?.selectFirstTag("rootfile")
        ?.getAttributeValue("full-path")
        ?.decodedURL ?: throw Exception("Invalid container.xml file")

    val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

    val document = parseXMLFile(opfFile.data)
        ?: throw Exception(".opf file failed to parse data")
    val metadata = document.selectFirstTag("metadata")
        ?: throw Exception(".opf file metadata section missing")
    val manifest = document.selectFirstTag("manifest")
        ?: throw Exception(".opf file manifest section missing")

    val metadataCoverId = metadata
        .selectChildTag("meta")
        .find { it.getAttributeValue("name") == "cover" }
        ?.getAttributeValue("content")

    val hrefRootPath = File(opfFilePath).parentFile ?: File("")
    fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
        .toPath()
        .invariantSeparatorsPathString
        .removePrefix("/")

    data class EpubManifestItem(
        val id: String,
        val absoluteFilePath: String,
        val mediaType: String,
        val properties: String
    )

    val manifestItems = manifest
        .selectChildTag("item").map {
            EpubManifestItem(
                id = it.getAttribute("id"),
                absoluteFilePath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                mediaType = it.getAttribute("media-type"),
                properties = it.getAttribute("properties")
            )
        }.associateBy { it.id }

    manifestItems[metadataCoverId]
        ?.let { files[it.absoluteFilePath] }
        ?.let { EpubImage(absPath = it.absPath, image = it.data) }
}
@Throws(Exception::class)
suspend fun epubParser(
    inputStream: InputStream
): EpubBook = withContext(Dispatchers.Default) {
        val files = getZipFiles(inputStream)

        val container = files["META-INF/container.xml"]
            ?: throw Exception("META-INF/container.xml file missing")

        val opfFilePath = parseXMLFile(container.data)
            ?.selectFirstTag("rootfile")
            ?.getAttributeValue("full-path")
            ?.decodedURL ?: throw Exception("Invalid container.xml file")

        val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

        val document = parseXMLFile(opfFile.data)
            ?: throw Exception(".opf file failed to parse data")
        val metadata = document.selectFirstTag("metadata")
            ?: throw Exception(".opf file metadata section missing")
        val manifest = document.selectFirstTag("manifest")
            ?: throw Exception(".opf file manifest section missing")
        val spine = document.selectFirstTag("spine")
            ?: throw Exception(".opf file spine section missing")

        val metadataTitle = metadata.selectFirstChildTag("dc:title")?.textContent
            ?: "Unknown Title"

        val metadataCoverId = metadata
            .selectChildTag("meta")
            .find { it.getAttributeValue("name") == "cover" }
            ?.getAttributeValue("content")

        val hrefRootPath = File(opfFilePath).parentFile ?: File("")
        fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
            .toPath()
            .invariantSeparatorsPathString
            .removePrefix("/")

        val manifestItems = manifest.selectChildTag("item").map {
            EpubManifestItem(
                id = it.getAttribute("id"),
                absPath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                mediaType = it.getAttribute("media-type"),
                properties = it.getAttribute("properties")
            )
        }.associateBy { it.id }


        var chapterIndex = 0
        val chapterExtensions = listOf("xhtml", "xml", "html").map { ".$it" }
        val chapters = spine
            .selectChildTag("itemref")
            .mapNotNull { manifestItems[it.getAttribute("idref")] }
            .filter { item ->
                chapterExtensions.any {
                    item.absPath.endsWith(it, ignoreCase = true)
                } || item.mediaType.startsWith("image/")
            }
            .mapNotNull { files[it.absPath]?.let { file -> it to file } }
            .mapIndexed { index, (item, file) ->
                val parser = EpubXMLFileParser(file.absPath, file.data, files)
                if (item.mediaType.startsWith("image/")) {
                    TempEpubChapter(
                        url = "image_${file.absPath}",
                        title = null,
                        body = parser.parseAsImage(item.absPath),
                        chapterIndex = chapterIndex,
                    )
                } else {
                    val res = parser.parseAsDocument()
                    // A full chapter usually is split in multiple sequential entries,
                    // try to merge them and extract the main title of each one.
                    // Is is not perfect but better than dealing with a table of contents
                    val chapterTitle = res.title ?: if (index == 0) metadataTitle else null
                    if (chapterTitle != null)
                        chapterIndex += 1

                    TempEpubChapter(
                        url = file.absPath,
                        title = chapterTitle,
                        body = res.body,
                        chapterIndex = chapterIndex,
                    )
                }
            }.groupBy {
                it.chapterIndex
            }.map { (index, list) ->
                EpubChapter(
                    absPath = list.first().url,
                    title = list.first().title ?: "Chapter $index",
                    body = list.joinToString("\n\n") { it.body }
                )
            }.filter {
                it.body.isNotBlank()
            }

        val imageExtensions =
            listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp", "svg").map { ".$it" }
        val unlistedImages = files
            .asSequence()
            .filter { (_, file) ->
                imageExtensions.any { file.absPath.endsWith(it, ignoreCase = true) }
            }
            .map { (_, file) ->
                EpubImage(absPath = file.absPath, image = file.data)
            }

        val listedImages = manifestItems.asSequence()
            .map { it.value }
            .filter { it.mediaType.startsWith("image") }
            .mapNotNull { files[it.absPath] }
            .map { EpubImage(absPath = it.absPath, image = it.data) }

        val images = (listedImages + unlistedImages).distinctBy { it.absPath }

        val coverImage = manifestItems[metadataCoverId]
            ?.let { files[it.absPath] }
            ?.let { EpubImage(absPath = it.absPath, image = it.data) }

        val coverImageBm: Bitmap? = if (coverImage?.image != null) {
            BitmapFactory.decodeByteArray(coverImage.image, 0, coverImage.image.size)
        } else {
            null
        }
        return@withContext EpubBook(
            fileName = metadataTitle.asFileName(),
            title = metadataTitle,
            coverImage = coverImage,
            chapters = chapters.toList(),
            images = images.toList()
        )
    }

class EpubXMLFileParser(
    fileAbsolutePath: String,
    val data: ByteArray,
    private val zipFile: Map<String, EpubFile>
) {
    data class Output(val title: String?, val body: String)

    val fileParentFolder: File = File(fileAbsolutePath).parentFile ?: File("")


    fun parseAsDocument(): Output {
        val body = Jsoup.parse(data.inputStream(), "UTF-8", "").body()
        val title = body.selectFirst("h1, h2, h3, h4, h5, h6")?.text()
        body.selectFirst("h1, h2, h3, h4, h5, h6")?.remove()
        return Output(
            title = title,
            body = getNodeStructuredText(body)
        )
    }

    fun parseAsImage(absolutePathImage: String): String {
        // Use run catching so it can be run locally without crash
        val bitmap = zipFile[absolutePathImage]?.data?.runCatching {
            BitmapFactory.decodeByteArray(this, 0, this.size)
        }?.getOrNull()

        val text = BookTextMapper.ImgEntry(
            path = absolutePathImage,
            yrel = bitmap?.let { it.height.toFloat() / it.width.toFloat() } ?: 1.45f
        ).toXMLString()

        return "\n\n$text\n\n"
    }

    // Rewrites the image node to xml for the next stage.
    private fun declareImgEntry(node: org.jsoup.nodes.Node): String {
        val attrs = node.attributes().associate { it.key to it.value }
        val relPathEncoded = attrs["src"] ?: attrs["xlink:href"] ?: ""

        val absolutePathImage = File(fileParentFolder, relPathEncoded.decodedURL)
            .canonicalFile
            .toPath()
            .invariantSeparatorsPathString
            .removePrefix("/")

        return parseAsImage(absolutePathImage)
    }

    private fun getPTraverse(node: org.jsoup.nodes.Node): String {
        fun innerTraverse(node: org.jsoup.nodes.Node): String =
            node.childNodes().joinToString("") { child ->
                when {
                    child.nodeName() == "br" -> "\n"
                    child.nodeName() == "img" -> declareImgEntry(child)
                    child.nodeName() == "image" -> declareImgEntry(child)
                    child is TextNode -> child.text()
                    else -> innerTraverse(child)
                }
            }

        val paragraph = innerTraverse(node).trim()
        return if (paragraph.isEmpty()) "" else innerTraverse(node).trim() + "\n\n"
    }

    private fun getNodeTextTraverse(node: org.jsoup.nodes.Node): String {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child.nodeName() == "image" -> declareImgEntry(child)
                child is TextNode -> {
                    val text = child.text().trim()
                    if (text.isEmpty()) "" else text + "\n\n"
                }

                else -> getNodeTextTraverse(child)
            }
        }
    }

    private fun getNodeStructuredText(node: org.jsoup.nodes.Node): String {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child.nodeName() == "image" -> declareImgEntry(child)
                child is TextNode -> child.text().trim()
                else -> getNodeTextTraverse(child)
            }
        }
    }
}
