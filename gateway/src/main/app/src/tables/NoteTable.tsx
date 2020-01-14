import React from 'react'
import { Note } from '../Note'
import { Table, Button } from 'reactstrap'

interface Props {
  notes: Note[]
  editRow: (note: Note) => void,
  deleteNote: (id: number) => void
}

const NoteTable = (props: Props) => (
  <Table hover responsive>
    <thead>
      <tr>
        <th>Text</th>
        <th>Confidential</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      {props.notes.length > 0 ? (
        props.notes.map(note => (
          <tr key={note.id}>
            <td>{note.text}</td>
            <td>{note.confidential ? 'true' : 'false'}</td>
            <td>
              <Button color="secondary"
                onClick={() => {
                  props.editRow(note)
                }}
              >
                Edit
              </Button>{' '}
              <Button color="danger"
                onClick={() => props.deleteNote(note.id)}
              >
                Delete
              </Button>
            </td>
          </tr>
        ))
      ) : (
        <tr>
          <td colSpan={3}>No notes</td>
        </tr>
      )}
    </tbody>
  </Table>
)

export default NoteTable
