declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'jsencrypt' {
  export default class JSEncrypt {
    setPublicKey(key: string): void
    encrypt(text: string): string | false
    setPrivateKey(key: string): void
    decrypt(text: string): string | false
  }
}
