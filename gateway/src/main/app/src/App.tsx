// adapted from https://github.com/taniarascia/react-hooks

import './App.css'
import React, { useState, Fragment, useEffect } from 'react'
import AddNoteForm from './forms/AddNoteForm'
import EditNoteForm from './forms/EditNoteForm'
import NoteTable from './tables/NoteTable'
import { Note } from './Note'
import { Container, Row, Col, Jumbotron, Button, Form, Input } from 'reactstrap'
import { useCookies } from 'react-cookie'

const App = () => {
  // Data
  const notesData : Note[] = []
  const initialFormState = {id: 0, text: '', confidential: false }

  // Setting state
  const [ notes, setNotes ] = useState(notesData)
  const [ currentNote, setCurrentNote ] = useState(initialFormState)
  const [ editing, setEditing ] = useState(false)
  const [ isAuthenticated, setAuthenticated ] = useState(false)
  const [ authenticatedUser, setAuthenticatedUser ] = useState('')
  const [ cookies ] = useCookies(['XSRF-TOKEN'])

  const getNotes = async () => {
    try {
      const response = await fetch('/api/notes')
      const notes = await response.json()
      setNotes(notes)
    } catch {
      // add better error handling here (e.g. 401?)
    }
  }

  useEffect(() => {
    // Create a scoped async function in the hook
    async function runAsync() {
      try {
        const response = await fetch('/api/user')
        const body = await response.text()

        if (body === '') {
          setAuthenticated(false)
          setAuthenticatedUser('')
        } else {
          setAuthenticated(true)
          setAuthenticatedUser(body)
          getNotes()
        }
      } catch {
        // add better error handling here
      }
    }
    // Execute the created function directly
    runAsync()
  // https://stackoverflow.com/a/55854902/1098564  
  // eslint-disable-next-line
  }, [])

  // CRUD operations
  const addNote = (note: {text: string, confidential: boolean}) => {
    fetch('/api/notes', {
      // credentials: 'include',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': cookies['XSRF-TOKEN']
      },
      body: JSON.stringify(note),
    }).then((result) => {
      getNotes()
    })
  }
    
  const deleteNote = (id: number) => {
    setEditing(false)

    fetch(`/api/notes/${id}`, {
      // credentials: 'include',
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': cookies['XSRF-TOKEN']
      }
    }).then(() => {
        getNotes()
    })
  }

  const updateNote = (id: number, updatedNote: Note) => {
    setEditing(false)

    fetch(`/api/notes/${id}`, {
      // credentials: 'include',
      method: 'PUT',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': cookies['XSRF-TOKEN']
      },
      body: JSON.stringify(updatedNote),
    }).then((result) => {
      getNotes()
    })
  }

  const editRow = (note: Note) => {
    setEditing(true)

    setCurrentNote({ id: note.id, text: note.text, confidential: note.confidential })
  }
  
  const login = () => {
    let port = (window.location.port ? ':' + window.location.port : '')
    window.location.href = '//' + window.location.hostname + port + '/private'
  }

  return (
    <Container>
      <Jumbotron id="jumbotron">
        <h1>CRUD App with Hooks</h1>
        {isAuthenticated ?
        <>
          <Form action="/logout" method="POST">
            <Input type="hidden" name="_csrf" value={cookies['XSRF-TOKEN']}/>
            <h3>Welcome {authenticatedUser}!</h3><Button color="secondary">Logout</Button>
          </Form>
        </> :
        <Button onClick={login}>Login</Button>
        }
      </Jumbotron>
      {isAuthenticated &&
      <Row>
        <Col lg="6">
          {editing ? (
            <Fragment>
              <h2>Edit note</h2>
              <EditNoteForm
                editing={editing}
                setEditing={setEditing}
                currentNote={currentNote}
                updateNote={updateNote}
              />
            </Fragment>
          ) : (
            <Fragment>
              <h2>Add note</h2>
              <AddNoteForm addNote={addNote} />
            </Fragment>
          )}
        </Col>
        <Col lg="6">
          <h2>View notes</h2>
          <NoteTable notes={notes} editRow={editRow} deleteNote={deleteNote} />
        </Col>
      </Row>
      }
    </Container>    
  )
}

export default App
