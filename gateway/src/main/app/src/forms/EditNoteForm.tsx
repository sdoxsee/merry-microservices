import React, { useState, useEffect } from 'react'
import { Note } from '../Note'
import { Form, Label, Input, Button, FormGroup } from 'reactstrap'

interface Props {
  currentNote: Note,
  updateNote: (id: number, note: Note) => void,
  setEditing: (isEditing: boolean) => void,
  editing: boolean
}
interface Event {
  target: any
}

const EditNoteForm = (props: Props) => {
  const [ note, setNote ] = useState(props.currentNote)

  useEffect(
    () => {
      setNote(props.currentNote)
    },
    [ props ]
  )
  // You can tell React to skip applying an effect if certain values haven’t changed between re-renders. [ props ]

  const handleInputChange = (event: Event) => {
    const { name, value } = event.target

    let newval = value
    if (name === 'confidential') {
      newval = !note.confidential
    }

    setNote({ ...note, [name]: newval })
  }

  return (
    <Form
      onSubmit={event => {
        event.preventDefault()

        props.updateNote(note.id, note)
      }}
    >
      <FormGroup>
        <Label>Text</Label>
        <Input type="text" name="text" value={note.text} onChange={handleInputChange} />
      </FormGroup>
      <FormGroup check>      
        <Label check>
        <Input type="checkbox" name="confidential" checked={note.confidential} onChange={handleInputChange} /> Confidential
        </Label>
      </FormGroup>
      <Button color="primary">Update note</Button>{' '}
      <Button color="secondary" onClick={() => props.setEditing(false)} className="button muted-button">
        Cancel
      </Button>
    </Form>
  )
}

export default EditNoteForm
