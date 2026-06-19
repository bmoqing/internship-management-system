/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const BASE_URL = 'http://localhost:8080'

// 1. 创建 axios 实例
const request = axios.create({
    // 后端接口地址，注意端口要和后端一致
    baseURL: BASE_URL,
    timeout: 5000 // 超时时间
})

const authClient = axios.create({
    baseURL: BASE_URL,
    timeout: 5000
})

const getStoredUser = () => {
    try {
        return JSON.parse(localStorage.getItem('user') || '{}')
    } catch (e) {
        return {}
    }
}

const saveStoredUser = (user) => {
    localStorage.setItem('user', JSON.stringify(user || {}))
}

const clearAuthAndRedirect = () => {
    localStorage.removeItem('user')
    if (window.location.pathname !== '/login') {
        window.location.href = '/login'
    }
}

const isAuthEndpoint = (url = '') => {
    return url.includes('/api/auth/login')
        || url.includes('/api/auth/register')
        || url.includes('/api/auth/refresh')
        || url.includes('/api/auth/logout')
}

let refreshPromise = null

const refreshToken = async () => {
    const user = getStoredUser()
    if (!user.refreshToken) {
        throw new Error('刷新令牌不存在')
    }

    const res = await authClient.post('/api/auth/refresh', {
        refreshToken: user.refreshToken
    })

    const body = res.data || {}
    const nextToken = body.data?.token
    const nextRefreshToken = body.data?.refreshToken
    const nextUser = body.data?.user || user

    if (body.code !== 200 || !nextToken || !nextRefreshToken) {
        throw new Error(body.message || '登录状态刷新失败')
    }

    const mergedUser = {
        ...user,
        ...nextUser,
        token: nextToken,
        refreshToken: nextRefreshToken
    }
    saveStoredUser(mergedUser)
    return nextToken
}

const tryLogoutQuietly = async () => {
    const user = getStoredUser()
    const headers = {}
    if (user.token) {
        headers.Authorization = `Bearer ${user.token}`
    }

    try {
        await authClient.post('/api/auth/logout', {
            refreshToken: user.refreshToken || null
        }, {
            headers
        })
    } catch (e) {
        // ignore
    }
}

// 2. 请求拦截器 (发送请求前做的事)
request.interceptors.request.use(config => {
    const user = getStoredUser()
    if (user.token) {
        config.headers = config.headers || {}
		// 自动注入 JWT Bearer Token 到 Authorization 头
        config.headers.Authorization = `Bearer ${user.token}`
    }
    return config
}, error => {
    return Promise.reject(error)
})

// 3. 响应拦截器 (收到结果后做的事)
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果返回的是文件流，直接返回
        if (response.config.responseType === 'blob') {
            return res
        }
        // 兼容处理：如果你后端返回的是 String，尝试解析成 JSON
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        return res;
    },
    async error => {
        const status = error.response?.status
        const originalRequest = error.config || {}

        if (status === 401 && !originalRequest._retry && !isAuthEndpoint(originalRequest.url)) {
            originalRequest._retry = true

            try {
                if (!refreshPromise) {
                    refreshPromise = refreshToken().finally(() => {
                        refreshPromise = null
                    })
                }
                const nextToken = await refreshPromise
                originalRequest.headers = originalRequest.headers || {}
                originalRequest.headers.Authorization = `Bearer ${nextToken}`
                return request(originalRequest)
            } catch (refreshError) {
                await tryLogoutQuietly()
                clearAuthAndRedirect()
                ElMessage.error('登录已过期，请重新登录')
                return Promise.reject(refreshError)
            }
        }

        const msg = error.response?.data?.message || error.message || '系统异常'
        ElMessage.error(msg)

        if (status === 401) {
            clearAuthAndRedirect()
        }

        return Promise.reject(error)
    }
)

export default request
