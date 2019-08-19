package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.Serie
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.net.URL

@Repository
interface SerieRepository : JpaRepository<Serie, Long>, JpaSpecificationExecutor<Serie> {
  fun deleteAllByUrlNotIn(urls: Iterable<URL>)
  fun countByUrlNotIn(urls: Iterable<URL>): Long
  fun findByUrl(url: URL): Serie?
}