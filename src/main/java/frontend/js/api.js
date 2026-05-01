// js/api.js
const BASE_URL = 'http://localhost:8080'; // Change this if your Spring Boot runs on a different port

const api = {
    request: async (endpoint, method = 'GET', body = null) => {
        const token = localStorage.getItem('jwt_token');

        const headers = {
            'Content-Type': 'application/json'
        };

        // Automatically attach token if the user is logged in
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            method,
            headers,
        };

        if (body) {
            config.body = JSON.stringify(body);
        }

        try {
            const response = await fetch(`${BASE_URL}${endpoint}`, config);

            // Handle 401 Unauthorized (Token expired/invalid)
            if (response.status === 401) {
                localStorage.clear();
                window.location.href = '/index.html';
                throw new Error("Session expired. Please log in again.");
            }

            // For endpoints that return empty bodies (like 201 Created or 204 No Content)
            const text = await response.text();
            let data = {};

            try {
                data = text ? JSON.parse(text) : {};
            } catch (e) {
                data = { message: text }; // Catches raw text errors from Spring Boot
            }

            if (!response.ok) {
                // THIS WILL SHOW YOU THE REAL ERROR
                throw new Error(data.message || data.error || `HTTP ${response.status}: Server rejected request.`);
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }
};