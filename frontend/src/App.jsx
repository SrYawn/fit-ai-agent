import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useState } from 'react'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ChatPage from './pages/ChatPage'
import KnowledgePage from './pages/KnowledgePage'
import UserRecordsPage from './pages/UserRecordsPage'

function App() {
  const [user, setUser] = useState(null)

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginPage onSelectUser={setUser} />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/chat"
          element={user ? <ChatPage user={user} onLogout={() => setUser(null)} /> : <Navigate to="/" />}
        />
        <Route
          path="/knowledge"
          element={user && user.role === 'ADMIN' ? <KnowledgePage user={user} /> : <Navigate to="/" />}
        />
        <Route
          path="/records"
          element={user ? <UserRecordsPage user={user} /> : <Navigate to="/" />}
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
