import React from 'react'
import AppLayout from './pages/app'
import Dashboard from './pages/dashboard'

// routes
export const routes = [{
  element: <AppLayout />,
  children: [
    {
      path: '/',
      element: <Dashboard />
    }
  ]
}]
