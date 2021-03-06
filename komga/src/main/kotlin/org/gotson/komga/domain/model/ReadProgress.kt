package org.gotson.komga.domain.model

import java.time.LocalDateTime

data class ReadProgress(
  val bookId: Long,
  val userId: Long,
  val page: Int,
  val completed: Boolean,

  override val createdDate: LocalDateTime = LocalDateTime.now(),
  override val lastModifiedDate: LocalDateTime = LocalDateTime.now()
) : Auditable()
