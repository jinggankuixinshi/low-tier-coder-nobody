export function useFormatTime() {
  function fmt(t: string | null | undefined): string {
    if (!t) return '-'
    return t.toString().replace('T', ' ').substring(0, 16)
  }
  return { fmt }
}
