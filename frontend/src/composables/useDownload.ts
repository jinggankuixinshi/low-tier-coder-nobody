import { useFormatTime } from './useFormatTime'

export function useDownload() {
  const { fmt: fmtTime } = useFormatTime()

  function sanitizeFilename(name: string): string {
    return (name || 'report')
      .replace(/[\\/:*?"<>|]/g, '_')
      .replace(/\s+/g, '_')
      .trim()
  }

  function downloadBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  return { downloadBlob, sanitizeFilename, fmtTime }
}
