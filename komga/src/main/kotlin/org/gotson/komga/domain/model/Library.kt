package org.gotson.komga.domain.model

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime

data class Library(
  val name: String,
  val root: URL,
  val importComicInfoBook: Boolean = true,
  val importComicInfoSeries: Boolean = true,
  val importComicInfoCollection: Boolean = true,
  val importEpubBook: Boolean = true,
  val importEpubSeries: Boolean = true,

  val id: Long = 0,

  override val createdDate: LocalDateTime = LocalDateTime.now(),
  override val lastModifiedDate: LocalDateTime = LocalDateTime.now()
) : Auditable() {

  fun path(): Path = Paths.get(this.root.toURI())
}
