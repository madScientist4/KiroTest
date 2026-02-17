import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-fnb-light">
        <header className="bg-fnb-primary text-white shadow-lg">
          <div className="container mx-auto px-4 py-4">
            <h1 className="text-2xl font-bold">API Error Logger</h1>
          </div>
        </header>
        
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<div>Dashboard - Coming Soon</div>} />
            <Route path="/errors" element={<div>Error List - Coming Soon</div>} />
            <Route path="/errors/:id" element={<div>Error Detail - Coming Soon</div>} />
            <Route path="/submit" element={<div>Submit Error - Coming Soon</div>} />
            <Route path="/admin/openapi" element={<div>OpenAPI Management - Coming Soon</div>} />
          </Routes>
        </main>
      </div>
    </Router>
  )
}

export default App
