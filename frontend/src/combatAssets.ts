// Imported artwork stays in local IndexedDB and is never sent to the combat API.
let database: Promise<IDBDatabase> | undefined
function openDatabase() {
  if (!database) database = new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open('dmhelper-battle-artwork', 1)
    request.onupgradeneeded = () => request.result.createObjectStore('images')
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(new Error('无法打开本地图片库。'))
  })
  return database
}
export async function loadArtwork(): Promise<Record<string, string>> {
  const db = await openDatabase()
  return new Promise((resolve, reject) => {
    const transaction = db.transaction('images', 'readonly')
    const request = transaction.objectStore('images').openCursor()
    const images: Record<string, string> = {}
    request.onsuccess = () => {
      const cursor = request.result
      if (cursor) { images[String(cursor.key)] = String(cursor.value); cursor.continue() }
    }
    transaction.oncomplete = () => resolve(images)
    transaction.onerror = () => reject(new Error('无法读取本地图片。'))
  })
}
export async function saveArtwork(key: string, image: string | null) {
  const db = await openDatabase()
  return new Promise<void>((resolve, reject) => {
    const transaction = db.transaction('images', 'readwrite')
    const store = transaction.objectStore('images')
    if (image === null) store.delete(key)
    else store.put(image, key)
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(new Error('图片保存失败，本地存储空间可能不足。'))
    transaction.onabort = () => reject(new Error('图片保存失败。'))
  })
}
export async function readArtwork(file: File, icon: boolean): Promise<string> {
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) throw new Error('请选择 PNG、JPG 或 WebP 图片。')
  if (file.size > 10 * 1024 * 1024) throw new Error('图片不能超过 10 MB。')
  const bitmap = await createImageBitmap(file).catch(() => { throw new Error('图片损坏或无法读取。') })
  try {
    const limit = icon ? 256 : 2048
    const scale = Math.min(1, limit / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(bitmap.width * scale))
    canvas.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = canvas.getContext('2d')
    if (!context) throw new Error('当前环境无法处理图片。')
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    return canvas.toDataURL('image/webp', .9)
  } finally { bitmap.close() }
}
