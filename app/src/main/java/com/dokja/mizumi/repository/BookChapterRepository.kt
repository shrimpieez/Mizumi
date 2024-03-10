package com.dokja.mizumi.repository

import com.dokja.mizumi.data.local.AppDatabaseOperations
import com.dokja.mizumi.data.local.chapter.Chapter
import com.dokja.mizumi.data.local.chapter.ChapterDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookChaptersRepository(
    private val chapterDao: ChapterDao,
    private val operations: AppDatabaseOperations
) {
    suspend fun update(chapter: Chapter) = chapterDao.update(chapter)
    suspend fun updatePosition(chapterUrl: String, lastReadPosition: Int, lastReadOffset: Int) =
        chapterDao.updatePosition(
            chapterUrl = chapterUrl,
            lastReadPosition = lastReadPosition,
            lastReadOffset = lastReadOffset
        )

    suspend fun setAsRead(chapterUrl: String, read: Boolean) =
        chapterDao.setAsRead(chapterUrl, read)

    suspend fun updateBookmark(chapterUrl: String, bookmark: Boolean) =
        chapterDao.updateBookmark(chapterUrl, bookmark)

    suspend fun get(url: String) = chapterDao.get(url)
    suspend fun hasChapters(bookUrl: String) = chapterDao.hasChapters(bookUrl)
    suspend fun getAll() = chapterDao.getAll()
    suspend fun updateTitle(url: String, title: String) =
        chapterDao.updateTitle(url, title)

    suspend fun setAsRead(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterDao.setAsRead(it) }


    suspend fun setAsUnread(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterDao.setAsUnread(it) }

    suspend fun insert(chapters: List<Chapter>) =
        chapterDao.insert(chapters.filter(::isValid))

    suspend fun insertReplace(chapters: List<Chapter>) =
        chapterDao.insertReplace(chapters.filter(::isValid))

    suspend fun removeAllFromBook(bookUrl: String) = chapterDao.removeAllFromBook(bookUrl)
    suspend fun chapters(bookUrl: String) = chapterDao.chapters(bookUrl)
    suspend fun getFirstChapter(bookUrl: String) = chapterDao.getFirstChapter(bookUrl)
    fun getChaptersWithContextFlow(bookUrl: String) =
        chapterDao.getChaptersWithContextFlow(bookUrl)

    suspend fun merge(newChapters: List<Chapter>, bookUrl: String) = withContext(Dispatchers.IO){
        val current = chapters(bookUrl).associateBy { it.url }.toMutableMap()
        for (chapter in newChapters)
            current.merge(
                chapter.url,
                chapter
            ) { old, new -> old.copy(position = new.position) }
        insertReplace(current.values.toList())
    }
}