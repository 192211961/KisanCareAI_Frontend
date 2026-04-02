import CONFIG from './config.js';

export const apiRequest = async (endpoint, options = {}) => {
    const url = endpoint.startsWith('http') ? endpoint : `${CONFIG.API_BASE_URL}${endpoint}`;
    
    const defaultHeaders = {
        'Content-Type': 'application/json',
    };

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers,
        },
    };

    try {
        const response = await fetch(url, config);
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.error || 'Something went wrong');
        }
        
        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
};

export const auth = {
    login: (email, password) => apiRequest('/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
    }),
    register: (full_name, email, password) => apiRequest('/register', {
        method: 'POST',
        body: JSON.stringify({ full_name, email, password })
    }),
    verifyOtp: (email, otp) => apiRequest('/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ email, otp })
    }),
    resendOtp: (email) => apiRequest('/resend-otp', {
        method: 'POST',
        body: JSON.stringify({ email })
    }),
    forgotPassword: (email) => apiRequest('/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email })
    }),
    resetPassword: (email, otp, new_password, confirm_password) => apiRequest('/reset-password', {
        method: 'POST',
        body: JSON.stringify({ email, otp, new_password, confirm_password })
    })
};

export const profile = {
    get: (email) => apiRequest(`/api/user/profile?email=${email}`),
    update: (data) => apiRequest('/api/user/profile', {
        method: 'PUT',
        body: JSON.stringify(data)
    })
};

export const history = {
    get: (email) => apiRequest(`/api/user/history?email=${email}`),
    save: (data) => apiRequest('/api/user/history', {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    deleteItem: (id) => apiRequest(`/api/user/history/${id}`, {
        method: 'DELETE'
    }),
    clear: (email) => apiRequest(`/api/user/history/clear?email=${email}`, {
        method: 'DELETE'
    })
};
