import { useRef, type ChangeEvent } from 'react'
import { Button } from '@mui/material'
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera'
import { useMutation } from '@tanstack/react-query'
import { uploadDamagePhoto, type Damage } from '../api/protocols'
import { useNotify } from '../notifications/useNotify'

const MAX_SIZE = 5 * 1024 * 1024
const TYPES = ['image/jpeg', 'image/png']

interface Props {
  damage: Damage
  onUploaded: () => void
}

export function PhotoUploadButton({ damage, onUploaded }: Props) {
  const input = useRef<HTMLInputElement>(null)
  const notify = useNotify()

  const upload = useMutation({
    mutationFn: (file: File) => uploadDamagePhoto(damage.id, file),
    onSuccess: () => {
      notify.success('Zdjęcie zapisane.')
      onUploaded()
    },
    onError: (error) => notify.apiError(error),
  })

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!TYPES.includes(file.type)) {
      notify.error('Wybierz zdjęcie JPEG lub PNG.')
      return
    }
    if (file.size > MAX_SIZE) {
      notify.error('Plik jest za duży (maks. 5 MB).')
      return
    }
    upload.mutate(file)
  }

  return (
    <>
      <input ref={input} type="file" accept={TYPES.join(',')} hidden onChange={handleChange} />
      <Button size="small" startIcon={<PhotoCameraIcon />} loading={upload.isPending} onClick={() => input.current?.click()}>
        {damage.photoUrl ? 'Zmień zdjęcie' : 'Dodaj zdjęcie'}
      </Button>
    </>
  )
}
