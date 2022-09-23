import React from 'react'
import AppUpdate from 'components/appUpdate'
import classnames from 'classnames'
import {matchPath} from 'react-router'
import {Link, useLocation, Outlet} from 'react-router-dom'

import './app.css'

const AppLayout = () => {
  const location = useLocation()
  const isActive = (path) => (
    matchPath(path, location.pathname)
  )

  return (
    <div className="app">
      <main className="app__main" role="main">

        <AppUpdate />

        <Outlet />
      </main>
    </div>
  )
}

export default AppLayout
