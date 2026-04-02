const KEY_LIST = [
    'AIzaSyC_c8OjfJ4PoC7MX4eqaRB0B6RmwCCs2-U',
    'AIzaSyB3rX1f4FMSddB7lzfjwoINk2807Dtcudc',
    'AIzaSyBKTDw5N_7OFw39qkWFFrdUZrdBlWnbjzY',
    'AIzaSyBBz5tKXUu82928AFvOlixIosiiaBznGUE',
    'AIzaSyAkcJdKOkCpbPloPoez6M7Vlxhs_lnA4jo'
];

async function testKey(key) {
    const url = `https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=${key}`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                contents: [{ parts: [{ text: 'Hello' }] }]
            })
        });
        const data = await response.json();
        if (response.ok) {
            console.log(`✅ Key ${key.substring(0, 10)}...: WORKING (Response: ${data.candidates[0].content.parts[0].text.trim()})`);
        } else {
            console.log(`❌ Key ${key.substring(0, 10)}...: FAILED (${data.error?.message || 'Unknown error'})`);
        }
    } catch (e) {
        console.log(`❌ Key ${key.substring(0, 10)}...: ERROR (${e.message})`);
    }
}

async function main() {
    console.log('--- Testing Gemini API Keys ---');
    for (const key of KEY_LIST) {
        await testKey(key);
    }
}

main();
