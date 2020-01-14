import React, { useState } from 'react'
import { Form, Label, Input, Button, FormGroup } from 'reactstrap'

interface Props {
  addNote: (note: {text: string, confidential: boolean}) => void
}
interface Event {
  target: any
}

const AddUserForm = (props: Props) => {
  const initialFormState: {text: string, confidential: boolean} = { text: '', confidential: false }
  const [ note, setNote ] = useState(initialFormState)

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
        
        if (!note.text) return // prevent adding empty notes!

        props.addNote(note)
        setNote(initialFormState)
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
      <Button color="primary">Add new note</Button>
    </Form>
  )
}

export default AddUserForm
