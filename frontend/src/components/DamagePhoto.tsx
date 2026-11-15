import { useEffect, useState } from 'react'
import { Box, CircularProgress, Typography } from '@mui/material'
import { getDamagePhoto } from '../api/protocols'

interface Props {
  damageId: number
  // Zmienia się po wgraniu nowego zdjęcia, co wymusza ponowne pobranie
  version: string
}

export function DamagePhoto({ damageId, version }: Props) {
  const [url, setUrl] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let objectUrl: string | null = null
    let active = true
    getDamagePhoto(damageId)
      .then((blob) => {
        if (!active) return
        objectUrl = URL.createObjectURL(blob)
        setUrl(objectUrl)
        setFailed(false)
      })
      .catch(() => active && setFailed(true))
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [damageId, version])

  if (failed) {
    return <Typography variant="body2" color="error">Nie udało się wczytać zdjęcia</Typography>
  }
  if (!url) {
    return <CircularProgress size={24} />
  }
  return (
    <Box
      component="a"
      href={url}
      target="_blank"
      rel="noreferrer"
      title="Otwórz w pełnym rozmiarze"
      sx={{ display: 'inline-block', lineHeight: 0 }}
    >
      <Box component="img" src={url} alt="Zdjęcie uszkodzenia" sx={{ width: 96, height: 72, objectFit: 'cover', borderRadius: 1 }} />
    </Box>
  )
}
