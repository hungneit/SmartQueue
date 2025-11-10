import React from 'react'
import { Routes, Route } from 'react-router-dom'
import { Layout } from 'antd'
import Header from './components/Header'
import Home from './pages/Home'
import Queue from './pages/Queue'
import Admin from './pages/Admin'
import './App.css'

const { Content, Footer } = Layout

function App() {
  return (
    <Layout className="min-h-screen">
      <Header />
      <Content className="p-6">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/queue/:queueId" element={<Queue />} />
          <Route path="/admin" element={<Admin />} />
        </Routes>
      </Content>
      <Footer className="text-center">
        SmartQueue ©2024 - Multi-cloud Queue Management System
      </Footer>
    </Layout>
  )
}

export default App