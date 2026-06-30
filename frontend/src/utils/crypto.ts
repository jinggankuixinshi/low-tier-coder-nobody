import JSEncrypt from 'jsencrypt'
import request from './request'

let publicKey: string | null = null
let publicKeyPromise: Promise<string> | null = null

export async function getPublicKey(): Promise<string> {
  if (publicKey) return publicKey
  if (publicKeyPromise) return publicKeyPromise

  publicKeyPromise = request.get('/auth/public-key').then((res: any) => {
    const key = res && (res.publicKey || (typeof res === 'string' ? res : null))
    if (!key) throw new Error('获取公钥失败：服务器返回数据异常')
    if (!key.includes('BEGIN PUBLIC KEY')) throw new Error('公钥格式异常，应为PEM格式')
    publicKey = key
    publicKeyPromise = null
    return publicKey
  }).catch((e: Error) => {
    publicKeyPromise = null
    publicKey = null
    console.error('获取RSA公钥失败:', e)
    throw e
  })

  return publicKeyPromise
}

export async function encryptPassword(password: string): Promise<string> {
  const key = await getPublicKey()
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(key)
  const encrypted = encrypt.encrypt(password)
  if (!encrypted) {
    console.error('JSEncrypt加密失败，公钥:', key.substring(0, 50) + '...')
    throw new Error('密码加密失败，请刷新页面重试')
  }
  return encrypted
}
