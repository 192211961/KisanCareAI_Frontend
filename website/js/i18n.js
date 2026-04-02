import TRANSLATIONS from './translations.js';

export function applyTranslations() {
    const lang = localStorage.getItem('user_lang') || 'en';
    if (lang === 'en') return;

    const dict = TRANSLATIONS[lang];
    if (!dict) return;

    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dict[key]) {
            el.textContent = dict[key];
        }
    });

    // Special case for welcome message
    const welcome = document.getElementById('display-name');
    if (welcome && dict['welcome_farmer']) {
        const name = localStorage.getItem('user_name') || 'Farmer';
        welcome.textContent = dict['welcome_farmer'].replace('{name}', name);
    }
}
