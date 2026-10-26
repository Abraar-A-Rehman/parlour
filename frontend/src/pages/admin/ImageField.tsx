import { useState, type ChangeEvent } from 'react'
import { api } from '../../api/api'
import { errorMessage } from '../../api/client'

interface Props {
  label: string
  value: string
  onChange: (url: string) => void
}

/** Image URL box + an "Upload" button that stores the file on the server and fills in the URL. */
export default function ImageField({ label, value, onChange }: Props) {
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const onFile = async (e: ChangeEvent<HTMLInputElement>) => {
    const input = e.target
    const file = input.files?.[0]
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      const { url } = await api.upload(file)
      onChange(url)
    } catch (err: unknown) {
      setError(errorMessage(err))
    } finally {
      setUploading(false)
      input.value = ''
    }
  }

  return (
    <div className="image-field">
      <label>
        {label}
        <input value={value} onChange={(e) => onChange(e.target.value)} placeholder="https://… or upload a file" />
      </label>
      <label className="btn btn-outline btn-small upload-btn">
        {uploading ? 'Uploading…' : 'Upload image'}
        <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={(e) => void onFile(e)} hidden />
      </label>
      {value && <img src={value} alt="Preview" className="image-preview" />}
      {error && <p className="error">{error}</p>}
    </div>
  )
}
