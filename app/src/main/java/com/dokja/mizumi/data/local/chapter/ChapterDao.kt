package com.dokja.mizumi.data.local.chapter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dokja.mizumi.data.ChapterWithContext
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM Chapter")
    suspend fun getAll(): List<Chapter>

    @Query(
        """
        SELECT * FROM Chapter
        WHERE Chapter.bookUrl == :bookUrl
        ORDER BY Chapter.position ASC
    """
    )
    suspend fun chapters(bookUrl: String): List<Chapter>

    @Update
    suspend fun update(chapter: Chapter)

    @Query("SELECT EXISTS(SELECT * FROM Chapter WHERE Chapter.bookUrl = :bookUrl LIMIT 1)")
    suspend fun hasChapters(bookUrl: String): Boolean

    @Query(
        """
        SELECT * FROM Chapter
        WHERE Chapter.bookUrl = :bookUrl
        ORDER BY Chapter.position ASC
        LIMIT 1
    """
    )
    suspend fun getFirstChapter(bookUrl: String): Chapter?

    @Query("UPDATE Chapter SET read = 1 WHERE url in (:chaptersUrl)")
    suspend fun setAsRead(chaptersUrl: List<String>)

    @Query("UPDATE Chapter SET read = :read WHERE url = :chapterUrl")
    suspend fun setAsRead(chapterUrl: String, read: Boolean)

    @Query(
        """
        UPDATE Chapter 
        SET lastReadPosition = :lastReadPosition, lastReadOffset = :lastReadOffset
        WHERE url = :chapterUrl
    """
    )
    suspend fun updatePosition(chapterUrl: String, lastReadPosition: Int, lastReadOffset: Int)

    @Query("UPDATE Chapter SET title = :title WHERE url == :url")
    suspend fun updateTitle(url: String, title: String)

    @Query("UPDATE Chapter SET read = 0 WHERE url in (:chaptersUrl)")
    suspend fun setAsUnread(chaptersUrl: List<String>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(chapters: List<Chapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(chapters: List<Chapter>)

    @Query("SELECT * FROM Chapter WHERE url = :url")
    suspend fun get(url: String): Chapter?

    @Query("DELETE FROM Chapter WHERE Chapter.bookUrl = :bookUrl")
    suspend fun removeAllFromBook(bookUrl: String)

    @Query("DELETE FROM Chapter WHERE Chapter.bookUrl NOT IN (SELECT book_library.url FROM book_library)")
    suspend fun removeAllNonLibraryRows()

    @Query(
        """
            SELECT Chapter.*, ChapterBody.url IS NOT NULL AS downloaded, book_library.lastReadChapter IS NOT NULL AS lastReadChapter
            FROM Chapter
            LEFT JOIN ChapterBody ON ChapterBody.url = Chapter.url
            LEFT JOIN book_library on book_library.url = :bookUrl AND book_library.lastReadChapter == Chapter.url
            WHERE Chapter.bookUrl == :bookUrl
            ORDER BY position ASC
        """
    )
    fun getChaptersWithContextFlow(bookUrl: String): Flow<List<ChapterWithContext>>
}