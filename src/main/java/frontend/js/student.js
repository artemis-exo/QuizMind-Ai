// js/student.js
document.addEventListener('DOMContentLoaded', () => {

    const token = localStorage.getItem('jwt_token');
    const role = localStorage.getItem('user_role');
    const name = localStorage.getItem('user_name');
    const email = localStorage.getItem('user_email');

    // Security Check
    if (!token || (role !== 'ROLE_STUDENT' && role !== 'STUDENT')) {
        window.location.href = 'index.html';
        return;
    }

    const nameDisplay = document.getElementById('studentNameDisplay');
    const rollDisplay = document.getElementById('studentRollDisplay');

    if (nameDisplay) nameDisplay.innerText = name || "Student";
    if (rollDisplay) rollDisplay.innerText = "Roll No: 53";

    // Standard Session Join
    const joinForm = document.getElementById('joinSessionForm');
    if (joinForm) {
        joinForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const sessionId = document.getElementById('joinSessionId').value.trim();
            if (sessionId) {
                window.location.href = `quiz-attempt.html?session=${sessionId}`;
            }
        });
    }
});

function logout() {
    localStorage.clear();
    window.location.href = 'index.html';
}

// ==========================================
// AI PRACTICE QUIZ (Direct Attempt Integration)
// ==========================================

async function generateAiPractice() {
    const topic = document.getElementById('aiTopic').value.trim();
    const count = document.getElementById('aiCount').value;

    if (!topic) {
        alert("Please enter a topic.");
        return;
    }

    const container = document.getElementById('aiPracticeContainer');
    const button = document.querySelector('button[onclick="generateAiPractice()"]');
    const originalBtnText = button.innerText;

    // 1. Update UI to show a clear loading state
    button.innerText = "🤖 Generating & Setting Up Quiz...";
    button.disabled = true;
    button.classList.add('opacity-75', 'cursor-not-allowed');

    container.innerHTML = `
        <div class="text-purple-600 font-medium text-center py-4">
            <p class="mb-2">Summoning AI to build your custom test...</p>
            <p class="text-sm text-gray-500">This usually takes a few seconds depending on the model.</p>
        </div>
    `;

    try {
        // 2. Call the NEW Backend API to generate AND save the session
        const response = await api.request(
            `/api/ai/generate-and-start?topic=${encodeURIComponent(topic)}&count=${count}`,
            'POST'
        );

        if (response && response.sessionId) {
            container.innerHTML = `<p class="text-green-600 text-center font-bold mb-4">Quiz Ready! Redirecting...</p>`;

            // 3. Seamlessly redirect the user to the standard attempt interface
            window.location.href = `quiz-attempt.html?session=${response.sessionId}`;
        } else {
            throw new Error("No session ID returned from the server.");
        }

    } catch (error) {
        console.error("AI Generation Error:", error);
        container.innerHTML = `<p class="text-red-500 text-center font-medium">Failed to generate: ${error.message}</p>`;
    } finally {
        // Restore button state in case of error
        button.innerText = originalBtnText;
        button.disabled = false;
        button.classList.remove('opacity-75', 'cursor-not-allowed');
    }
}