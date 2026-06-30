import type { Component } from 'vue'

export function useFileTag() {
  function getExt(name: string): string {
    const idx = name.lastIndexOf('.')
    return idx >= 0 ? name.substring(idx + 1).toLowerCase() : 'file'
  }

  function fileTagType(name: string): string {
    const ext = getExt(name)
    const codeTypes = ['java', 'py', 'js', 'ts', 'cpp', 'c', 'h', 'vue', 'go', 'rs', 'sh', 'bat', 'sql', 'html', 'css', 'xml', 'json', 'yml', 'yaml', 'md']
    const docTypes = ['doc', 'docx', 'pdf', 'ppt', 'pptx', 'xls', 'xlsx', 'txt']
    const archiveTypes = ['zip', 'rar', '7z', 'tar', 'gz', 'tgz']
    const imgTypes = ['png', 'jpg', 'jpeg', 'gif', 'bmp']
    const videoTypes = ['mp4', 'avi', 'mov', 'wmv', 'webm']

    if (codeTypes.includes(ext)) return 'info'
    if (docTypes.includes(ext)) return 'primary'
    if (archiveTypes.includes(ext)) return 'warning'
    if (imgTypes.includes(ext)) return 'success'
    if (videoTypes.includes(ext)) return 'danger'
    return ''
  }

  function parseArray(v: unknown): any[] {
    if (Array.isArray(v)) return v
    if (typeof v === 'string') {
      try { const parsed = JSON.parse(v); return Array.isArray(parsed) ? parsed : [v] } catch { return [v] }
    }
    return v ? [v] : []
  }

  return { getExt, fileTagType, parseArray }
}
