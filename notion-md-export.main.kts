#!/usr/bin/env kotlin
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.7.2")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-httpclient:1.7.2")

import notion.api.v1.NotionClient
import notion.api.v1.http.JavaNetHttpClient
import notion.api.v1.model.blocks.*
import notion.api.v1.model.common.Emoji
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.common.RichTextType
import notion.api.v1.model.databases.query.filter.PropertyFilter
import notion.api.v1.model.databases.query.filter.condition.CheckboxFilter
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageProperty
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.script.experimental.dependencies.DependsOn

val token = args[0]
val databaseId = args[1]
val publishPropertyName = "Publish"
val client = NotionClient(token = token, httpClient = JavaNetHttpClient())
val queryResult = client.queryDatabase(
    databaseId = databaseId,
    filter = PropertyFilter(property = publishPropertyName, checkbox = CheckboxFilter(true))
)

for (page in queryResult.results) {
    var str = buildHeader(page)
    val outPutMDPath = createPath(getSlug(page), getPostDate(page))
    str += buildBody(loadBlocks(page.id), outPutMDPath)
    writeMD(outPutMDPath, str)
    val propertyMap = mutableMapOf<String, PageProperty>()
    propertyMap[publishPropertyName] = PageProperty(checkbox = false)
    client.updatePage(page.id, propertyMap)
}
client.close()

fun loadBlocks(blockId: String): MutableList<Block> {
    val children = client.retrieveBlockChildren(blockId, pageSize = 1000)
    val blocks = mutableListOf<Block>()
    blocks.addAll(children.results)
    var cursor = children.nextCursor
    while (cursor != null) {
        val nextChildren = client.retrieveBlockChildren(blockId, startCursor = cursor, pageSize = 1000)
        blocks.addAll(nextChildren.results)
        cursor = nextChildren.nextCursor
    }
    return blocks
}

fun buildHeader(page: Page): String {
    var header = "---\n"
    getTitle(page)?.let { header += "title: \"$it\"\n"}
    getPostDate(page)?.let { header += "date: \"$it\"\n"}
    getTags(page)?.let { header += "tags: [$it]\n"}
    header += "---\n"
    return header
}

fun buildBody(blocks: MutableList<Block>, outPutMDPath: String, indentSize: Int = 0): String {
    var str = ""
    for ((index, block) in blocks.withIndex()) {
        str += when (block.type) {
            BlockType.Paragraph -> block2MD(block.asParagraph(), indentSize = indentSize)
            BlockType.HeadingOne -> block2MD(block.asHeadingOne())
            BlockType.HeadingTwo -> block2MD(block.asHeadingTwo())
            BlockType.HeadingThree -> block2MD(block.asHeadingThree())
            BlockType.BulletedListItem -> block2MD(block.asBulletedListItem(), indentSize = indentSize)
            BlockType.NumberedListItem -> block2MD(block.asNumberedListItem(), indentSize = indentSize)
            BlockType.LinkPreview -> block2MD(block.asLinkPreview())
            BlockType.Bookmark -> block2MD(block.asBookmark())
            BlockType.Callout -> block2MD(block.asCallout())
            BlockType.Column -> ""  // NOP
            BlockType.ColumnList -> ""  // NOP
            BlockType.Divider -> "---\n"
            BlockType.Quote -> block2MD(block.asQuote())
            BlockType.ToDo -> block2MD(block.asToDo(), indentSize = indentSize)
            BlockType.Toggle -> block2MD(block.asToggle(), indentSize = indentSize)
            BlockType.Code -> block2MD(block.asCode())
            BlockType.Embed -> block2MD(block.asEmbed())
            BlockType.Image -> block2MD(block.asImage(), Path.of(outPutMDPath).parent)
            BlockType.ChildPage -> block2MD(block.asChildPage())
            BlockType.Table -> block2MD(block.asTable())
            BlockType.TableRow -> ""    // NOP
            else -> {
                println("Unsupported:${block.type}")
            }
        }

        if (block.hasChildren == true) {
            block.id?.let { id ->
                val childBlocks = loadBlocks(id)

                if (childBlocks.size >= 1 && !isContinuousBlock(block.type, childBlocks[0].type)) {
                    str += "\n"
                }
                str += if (block.type == BlockType.Column) {
                    buildBody(childBlocks, outPutMDPath, indentSize = 0)    // reset Indent
                } else {
                    buildBody(childBlocks, outPutMDPath, indentSize = indentSize + 1)
                }
            }
        } else {
            if (!isContinuousBlock(index, blocks)) {
                str += "\n"
            }
        }
    }
    return str
}

fun isContinuousBlock(currentIndex: Int, blocks: MutableList<Block>): Boolean {
    if (blocks.size - currentIndex > 1) {
        return isContinuousBlock(blocks[currentIndex].type, blocks[currentIndex + 1].type)
    }
    return true
}

fun isContinuousBlock(currentBlockType: BlockType, nextBlockType: BlockType): Boolean {
    val types = listOf(BlockType.BulletedListItem, BlockType.NumberedListItem, BlockType.ToDo, BlockType.Toggle)
    return types.contains(currentBlockType) && types.contains(nextBlockType)
}

fun createPath(orgSlug: String?, date: String?): String {
    var path = "./content/"
    var isIndividual = false
    var slug = orgSlug?.run {
        isIndividual = this.startsWith('/')
        this.replace(".", "").trim('/').lowercase()
    }

    path += if (isIndividual) {
        "$slug/index.md"
    } else {
        if (date.isNullOrEmpty()) {
            if (slug.isNullOrEmpty()) {
                slug = "default"
            }
            "$slug/index.md"
        } else {
            val datePath = if (date.length > 10) {
                val dateTime = OffsetDateTime.parse(date)
                "post/${dateTime.year}/${dateTime.monthValue}/${dateTime.dayOfMonth}"
            } else {
                val dateTime = LocalDate.parse(date)
                "post/${dateTime.year}/${dateTime.monthValue}/${dateTime.dayOfMonth}"
            }
            if (slug.isNullOrEmpty()) {
                "$datePath/index.md"
            } else {
                "$datePath/$slug/index.md"
            }
        }
    }
    return path
}

fun getTitle(page: Page): String? {
    for(property in page.properties) {
        if (property.value.type == PropertyType.Title) {
            property.value.title?.get(0)?.let { title ->
                return title.plainText
            }
        }
    }
    return null
}

fun getPostDate(page: Page): String? {
    page.properties["Date"]?.date?.let {
        return it.start
    }
    return null
}

fun getSlug(page: Page): String? {
    page.properties["Slug"]?.richText?.let {
        if (it.isNotEmpty()) {
            return it[0].plainText
        }
    }
    return null
}

fun getTags(page: Page): String? {
    page.properties["Tags"]?.multiSelect?.let { tags ->
        val tagList = mutableListOf<String>()
        for (tag in tags) {
            tag.name?.let { tagList.add("\"$it\"") }
        }
        return if (tagList.size > 0) tagList.joinToString(",") else null
    }
    return null
}

fun block2MD(block: ParagraphBlock, indentSize: Int = 0): String =
    "${indent(indentSize)}${getRichText(block.paragraph.richText)}\n"

fun block2MD(block: HeadingOneBlock): String = "# ${getRichText(block.heading1.richText)}\n"

fun block2MD(block: HeadingTwoBlock): String = "## ${getRichText(block.heading2.richText)}\n"

fun block2MD(block: HeadingThreeBlock): String = "### ${getRichText(block.heading3.richText)}\n"

fun block2MD(block: BulletedListItemBlock, indentSize: Int = 0): String =
    "${indent(indentSize)}- ${getRichText(block.bulletedListItem.richText)}\n"

fun block2MD(block: NumberedListItemBlock, indentSize: Int = 0): String =
    "${indent(indentSize)}1. ${getRichText(block.numberedListItem.richText)}\n"

fun block2MD(block: BookmarkBlock): String = block.bookmark?.run { "[${this.url}](${this.url})" } + "\n"

fun block2MD(block: LinkPreviewBlock): String = block.linkPreview?.run { "[${this.url}](${this.url})"} + "\n"

fun block2MD(block: CalloutBlock): String {
    return block.callout?.run {
        val icon = this.icon?.run { (this as Emoji).emoji } ?: ""
        this.richText?.run { "<aside>\n$icon${getRichText(this)}\n</aside>\n" }
    } + "\n"
}

fun block2MD(block: QuoteBlock): String = block.quote?.richText?.run { "> ${getRichText(this)}" } + "\n"

fun block2MD(block: ToDoBlock, indentSize: Int): String {
    val check = if (block.toDo.checked) "x" else " "
    val item = block.toDo.richText?.run { getRichText(this) } ?: ""
    return "${indent(indentSize)}- [$check] $item\n"
}

fun block2MD(block: ToggleBlock, indentSize: Int): String =
    "${indent(indentSize)}- ${getRichText(block.toggle.richText)}\n"

fun block2MD(block: CodeBlock): String {
    var str = ""
    block.code?.let { code ->
        val lang = code.language ?: ""
        str += "```$lang\n"
        code.richText?.let { str += "${getRichText(it)}\n" }
        str += "```\n"
        val caption = code.caption?.run { getRichText(this) }
        if (!caption.isNullOrEmpty()) { str += "$caption\n" }
    }
    return str
}

fun block2MD(block: EmbedBlock): String {
    var str = ""
    block.embed?.let { embed ->
        embed.url?.let {
            str += "[${it}](${it})\n"
            val caption = embed.caption?.run { getRichText(this) }
            if (!caption.isNullOrEmpty()) { str += "$caption\n" }
        }
    }
    return str
}

fun block2MD(block: ImageBlock, outPutPath: Path): String {
    var str = ""
    block.image?.let { image ->
        val caption = image.caption?.run { getRichText(this) }
        image.external?.url?.let { externalUrl ->
            str += "![$externalUrl]($externalUrl)\n"
            if (!caption.isNullOrEmpty()) { str += "$caption\n" }
        }
        image.file?.url?.let { imageUrl ->
            val url = URL(imageUrl)
            val path = Path.of(url.path)
            val extension = path.fileName.toString().substringAfterLast('.', "")
            val fileName = "${path.parent.fileName}.$extension"
            url.openStream().use {
                writeBinary("$outPutPath/$fileName", it.readAllBytes())
            }
            str += "![${caption ?: ""}](./$fileName)\n"
            if (!caption.isNullOrEmpty()) { str += "$caption\n" }
        }
    }
    return str
}

fun block2MD(block: ChildPageBlock): String {
    var str = ""
    if (block.hasChildren == true) {
        block.id?.let { id ->
            str += "[${block.childPage.title}](https://www.notion.so/${id.replace("-", "")})\n"
        }
    }
    return str
}

fun block2MD(block: TableBlock): String {
    var str = ""
    val colNum = block.table.tableWidth
    if (block.hasChildren == true) {
        block.id?.let { id ->
            for ((index, rowBlocks) in loadBlocks(id).withIndex()) {
                if (rowBlocks.type == BlockType.TableRow) {
                    if (index == 1) {
                        str += "|"+" --- |".repeat(colNum)+"\n"
                    }
                    val celList = mutableListOf<String>()
                    for (richText in rowBlocks.asTableRow().tableRow.cells) {
                        celList.add(getRichText(richText))
                    }
                    str += "|${celList.joinToString("|")}|\n"
                }
            }
        }
    }
    return str
}

fun indent(size: Int): String = "\t".repeat(size)

fun getRichText(richTextList: List<PageProperty.RichText>): String {
    var str = ""
    for (richText in richTextList) {
        when (richText.type) {
            RichTextType.Text -> {
                richText.plainText?.let { plainText ->
                    var text = plainText
                    richText.href?.let { text = "[$text]($it)" }
                    richText.annotations?.let { annotation ->
                        annotation.code?.let { if (it) text = "`$text`"}
                        annotation.bold?.let { if (it) text = "**$text**"}
                        annotation.italic?.let { if (it) text = "*$text*"}
                        annotation.strikethrough?.let { if (it) text = "~~$text~~"}
                    }
                    str += text
                }
            }
            RichTextType.Equation -> {
                str += richText.equation?.run { "\$${this.expression}\$" } ?: ""
            }
            RichTextType.Mention -> {
                str += richText.plainText?.run { this }
            }
        }
    }
    return str.replace("\n", "  \n")
}

fun writeBinary(filePath: String, array: ByteArray) {
    Path.of(filePath).parent.toFile().mkdirs()
    File(filePath).writeBytes(array)
}

fun writeMD(filePath: String, buf: String) {
    Path.of(filePath).parent.toFile().mkdirs()
    File(filePath).writeText(buf, Charset.forName("UTF-8"))
}
