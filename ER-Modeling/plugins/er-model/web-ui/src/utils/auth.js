// src/utils/auth.js
const TOKEN_KEY = 'opengauss-token'

export const isLogin = () => !!localStorage.getItem(TOKEN_KEY)
export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)
