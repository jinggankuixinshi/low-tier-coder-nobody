import { computed, ref } from 'vue'

export function usePagination(initialPageSize = 14) {
  const currentPage = ref(1)
  const pageSize = ref(initialPageSize)
  const total = ref(0)

  const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 0)

  const visiblePages = computed(() => {
    const pages = totalPages.value
    if (pages <= 10) {
      return Array.from({ length: pages }, (_, i) => i + 1)
    }
    const cp = currentPage.value
    if (cp <= 5) {
      return [1, 2, 3, 4, 5, 6, -1, pages]
    }
    if (cp >= pages - 4) {
      return [1, -1, pages - 5, pages - 4, pages - 3, pages - 2, pages - 1, pages]
    }
    return [1, -1, cp - 2, cp - 1, cp, cp + 1, cp + 2, -1, pages]
  })

  function goPage(page: number) {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }

  return { currentPage, pageSize, total, totalPages, visiblePages, goPage }
}
