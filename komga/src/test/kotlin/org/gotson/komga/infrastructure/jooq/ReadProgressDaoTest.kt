package org.gotson.komga.infrastructure.jooq

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.ReadProgress
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureTestDatabase
class ReadProgressDaoTest(
  @Autowired private val readProgressDao: ReadProgressDao,
  @Autowired private val userRepository: KomgaUserRepository,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val libraryRepository: LibraryRepository
) {
  private var library = makeLibrary()
  private var series = makeSeries("Series")

  private var user1 = KomgaUser("user1@example.org", "", false)
  private var user2 = KomgaUser("user2@example.org", "", false)

  private var book1 = makeBook("Book1")
  private var book2 = makeBook("Book2")

  @BeforeAll
  fun setup() {
    library = libraryRepository.insert(library)
    series = seriesRepository.insert(series.copy(libraryId = library.id))
    user1 = userRepository.save(user1)
    user2 = userRepository.save(user2)
    book1 = bookRepository.insert(book1.copy(libraryId = library.id, seriesId = series.id))
    book2 = bookRepository.insert(book2.copy(libraryId = library.id, seriesId = series.id))
  }

  @AfterEach
  fun deleteReadProgress() {
    readProgressDao.deleteAll()
  }

  @AfterAll
  fun tearDown() {
    userRepository.deleteAll()
    bookRepository.deleteAll()
    seriesRepository.deleteAll()
    libraryRepository.deleteAll()
  }

  @Test
  fun `given book without user progress when saving progress then progress is saved`() {
    val now = LocalDateTime.now()

    readProgressDao.save(ReadProgress(
      book1.id,
      user1.id,
      5,
      false
    ))

    val readProgressList = readProgressDao.findByUserId(user1.id)

    assertThat(readProgressList).hasSize(1)
    with(readProgressList.first()) {
      assertThat(page).isEqualTo(5)
      assertThat(completed).isEqualTo(false)
      assertThat(bookId).isEqualTo(book1.id)
      assertThat(createdDate)
        .isAfterOrEqualTo(now)
        .isEqualTo(lastModifiedDate)
    }
  }

  @Test
  fun `given book with user progress when saving progress then progress is updated`() {
    readProgressDao.save(ReadProgress(
      book1.id,
      user1.id,
      5,
      false
    ))

    Thread.sleep(5)
    val modificationDate = LocalDateTime.now()

    readProgressDao.save(ReadProgress(
      book1.id,
      user1.id,
      10,
      true
    ))

    val readProgressList = readProgressDao.findByUserId(user1.id)

    assertThat(readProgressList).hasSize(1)
    with(readProgressList.first()) {
      assertThat(page).isEqualTo(10)
      assertThat(completed).isEqualTo(true)
      assertThat(bookId).isEqualTo(book1.id)
      assertThat(createdDate)
        .isBefore(modificationDate)
        .isNotEqualTo(lastModifiedDate)
      assertThat(lastModifiedDate).isAfterOrEqualTo(modificationDate)
    }
  }
}
