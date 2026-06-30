export function useFormatSize() {
  function formatSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    const k = 1024
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i]
  }

  function formatTotalSize(sizesJson: string): string {
    if (!sizesJson) return '0 B'
    try {
      const sizes = JSON.parse(sizesJson)
      if (Array.isArray(sizes)) {
        const total = sizes.reduce((sum: number, s: number) => sum + (Number(s) || 0), 0)
        return formatSize(total)
      }
      return formatSize(Number(sizesJson) || 0)
    } catch {
      return '0 B'
    }
  }

  return { formatSize, formatTotalSize }
}
