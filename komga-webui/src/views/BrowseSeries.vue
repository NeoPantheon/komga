<template>
  <div>
    <toolbar-sticky>
      <!--   Go back to parent library   -->
      <v-btn icon
             title="Go to library"
             :to="{name:'browse-libraries', params: {libraryId: series.libraryId ? series.libraryId : 0 }}"
      >
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>

      <!--   Action menu   -->
      <v-menu offset-y v-if="isAdmin">
        <template v-slot:activator="{ on }">
          <v-btn icon v-on="on">
            <v-icon>mdi-dots-vertical</v-icon>
          </v-btn>
        </template>
        <v-list>
          <v-list-item @click="analyze()">
            <v-list-item-title>Analyze</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>

      <v-toolbar-title>
        <span v-if="series.name">{{ series.name }}</span>
        <badge class="ml-4" v-if="totalElements" v-model="totalElements"/>
      </v-toolbar-title>

      <v-spacer/>

      <!--   Sort menu   -->
      <sort-menu-button :sort-default="sortDefault"
                        :sort-options="sortOptions"
                        :sort-active.sync="sortActive"
      />
    </toolbar-sticky>

    <v-container fluid class="px-6">
      <v-row>
        <v-col cols="4" sm="4" md="auto" lg="auto" xl="auto">
          <v-img :src="thumbnailUrl"
                 lazy-src="../assets/cover.svg"
                 max-height="300"
                 max-width="212"
          />

        </v-col>
        <v-col cols="8">
          <v-row>
            <v-col>
              <div class="headline" v-if="series.name">{{ series.name }}</div>
            </v-col>
          </v-row>

          <v-row>
            <v-col cols="auto" class="body-2">STATUS</v-col>
            <v-col cols="auto" class="body-2 text-capitalize" v-if="series.metadata">{{
              series.metadata.status.toLowerCase() }}
            </v-col>
          </v-row>
        </v-col>
      </v-row>

      <v-divider class="my-4"/>

      <v-row justify="start" ref="content" v-resize="updateCardWidth">

        <v-skeleton-loader v-for="(b, i) in books"
                           :key="i"
                           :width="cardWidth"
                           height="328.13"
                           justify-self="start"
                           :loading="b === null"
                           type="card, text"
                           class="ma-3 mx-2"
                           v-intersect="onCardIntersect"
                           :data-index="i"
        >
          <card-book :book="b" :width="cardWidth"/>
        </v-skeleton-loader>

      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import Badge from '@/components/Badge.vue'
import CardBook from '@/components/CardBook.vue'
import SortMenuButton from '@/components/SortMenuButton.vue'
import ToolbarSticky from '@/components/ToolbarSticky.vue'
import { parseQuerySort } from '@/functions/query-params'
import { seriesThumbnailUrl } from '@/functions/urls'
import { LoadState } from '@/types/common'
import Vue from 'vue'

export default Vue.extend({
  name: 'BrowseSeries',
  components: { CardBook, ToolbarSticky, SortMenuButton, Badge },
  data: () => {
    return {
      series: {} as SeriesDto,
      books: [] as BookDto[],
      pagesState: [] as LoadState[],
      pageSize: 20,
      visibleCards: [] as number[],
      totalElements: null as number | null,
      sortOptions: [{ name: 'Number', key: 'number' }, { name: 'Date added', key: 'createdDate' }, {
        name: 'File size',
        key: 'fileSize'
      }] as SortOption[],
      sortActive: {} as SortActive,
      sortDefault: { key: 'number', order: 'asc' } as SortActive,
      cardWidth: 150
    }
  },
  computed: {
    isAdmin (): boolean {
      return this.$store.getters.meAdmin
    },
    sortCustom (): boolean {
      return this.sortActive.key !== this.sortDefault.key || this.sortActive.order !== this.sortDefault.order
    },
    thumbnailUrl (): string {
      return seriesThumbnailUrl(this.seriesId)
    }
  },
  props: {
    seriesId: {
      type: Number,
      required: true
    }
  },
  watch: {
    sortActive () {
      this.updateRoute()
      this.reloadData(this.seriesId)
    }
  },
  async created () {
    this.series = await this.$komgaSeries.getOneSeries(this.seriesId)
  },
  mounted () {
    // fill books skeletons if an index is provided, so scroll position can be restored
    if (this.$route.params.index) {
      this.books = Array(Number(this.$route.params.index)).fill(null)
    } else { // else fill one page of skeletons
      this.books = Array(this.pageSize).fill(null)
    }

    // restore sort from query param
    this.sortActive = this.parseQuerySortOrDefault(this.$route.query.sort)
  },
  async beforeRouteUpdate (to, from, next) {
    if (to.params.seriesId !== from.params.seriesId) {
      this.series = await this.$komgaSeries.getOneSeries(Number(to.params.seriesId))
      this.sortActive = this.parseQuerySortOrDefault(to.query.sort)
      this.reloadData(Number(to.params.seriesId))
    }

    next()
  },
  methods: {
    updateCardWidth () {
      const content = this.$refs.content as HTMLElement
      switch (this.$vuetify.breakpoint.name) {
        case 'xs':
          this.cardWidth = (content.clientWidth - (16 * 2)) / 2
          break
        case 'sm':
          this.cardWidth = (content.clientWidth - (16 * 3)) / 3
          break
        default:
          this.cardWidth = 150
      }
    },
    parseQuerySortOrDefault (querySort: any): SortActive {
      return parseQuerySort(querySort, this.sortOptions) || this.$_.clone(this.sortDefault)
    },
    async onCardIntersect (entries: any, observer: any, isIntersecting: boolean) {
      const elementIndex = Number(entries[0].target.dataset['index'])
      if (isIntersecting) {
        this.visibleCards.push(elementIndex)
        const pageNumber = Math.floor(elementIndex / this.pageSize)
        if (this.pagesState[pageNumber] === undefined || this.pagesState[pageNumber] === LoadState.NotLoaded) {
          this.processPage(await this.loadPage(pageNumber, this.seriesId))
        }
      } else {
        this.$_.pull(this.visibleCards, elementIndex)
      }

      const max = this.$_.max(this.visibleCards)
      const index = (max === undefined ? 0 : max).toString()

      if (this.$route.params.index !== index) {
        this.updateRoute(index)
      }
    },
    updateRoute (index?: string) {
      this.$router.replace({
        name: this.$route.name,
        params: { seriesId: this.$route.params.seriesId, index: index || this.$route.params.index },
        query: {
          sort: `${this.sortActive.key},${this.sortActive.order}`
        }
      })
    },
    reloadData (seriesId: number) {
      this.totalElements = null
      this.pagesState = []
      this.visibleCards = []
      this.books = Array(this.pageSize).fill(null)
      this.loadInitialData(seriesId)
    },
    async loadInitialData (seriesId: number, pageToLoad: number = 0) {
      this.processPage(await this.loadPage(pageToLoad, seriesId))
    },
    async loadPage (page: number, seriesId: number): Promise<Page<BookDto>> {
      this.pagesState[page] = LoadState.Loading
      const pageRequest = {
        page: page,
        size: this.pageSize
      } as PageRequest

      if (this.sortActive != null) {
        pageRequest.sort = [`${this.sortActive.key},${this.sortActive.order}`]
      }
      return this.$komgaSeries.getBooks(seriesId, pageRequest)
    },
    processPage (page: Page<BookDto>) {
      if (this.totalElements === null) {
        // initialize page data
        this.totalElements = page.totalElements
        this.books = Array(this.totalElements).fill(null)
        this.pagesState = Array(page.totalPages).fill(LoadState.NotLoaded)
      }
      this.books.splice(page.number * page.size, page.size, ...page.content)
      this.pagesState[page.number] = LoadState.Loaded
    },
    analyze () {
      this.$komgaSeries.analyzeSeries(this.series)
    }
  }
})
</script>

<style scoped>
</style>