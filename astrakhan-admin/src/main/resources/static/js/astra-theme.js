/**
 * Тема: localStorage key совпадает с module2/module3 (astra-theme).
 * По умолчанию — светлая тема.
 */
(function () {
  var THEME_KEY = 'astra-theme'

  function applyTheme(theme) {
    var isDark = theme === 'dark'
    var root = document.documentElement
    root.classList.toggle('app-dark', isDark)
    root.classList.toggle('app-light', !isDark)
    if (document.body) {
      document.body.classList.toggle('app-dark-theme', isDark)
      document.body.classList.toggle('app-light-theme', !isDark)
    }
    var btn = document.getElementById('adminThemeToggle')
    if (btn) {
      btn.textContent = isDark ? 'Светлая тема' : 'Тёмная тема'
      btn.setAttribute('aria-label', isDark ? 'Переключить на светлую тему' : 'Переключить на тёмную тему')
    }
  }

  function getTheme() {
    try {
      var stored = localStorage.getItem(THEME_KEY)
      if (stored === 'dark' || stored === 'light') return stored
    } catch (_) {}
    return 'light'
  }

  function setTheme(theme) {
    if (theme !== 'dark' && theme !== 'light') return
    applyTheme(theme)
    try {
      localStorage.setItem(THEME_KEY, theme)
    } catch (_) {}
    try {
      window.dispatchEvent(new CustomEvent('astra:theme-changed', { detail: { theme: theme } }))
    } catch (_) {}
  }

  function init() {
    setTheme(getTheme())
    var btn = document.getElementById('adminThemeToggle')
    if (btn) {
      btn.addEventListener('click', function () {
        var cur = document.documentElement.classList.contains('app-dark') ? 'dark' : 'light'
        setTheme(cur === 'dark' ? 'light' : 'dark')
      })
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init)
  } else {
    init()
  }
})()
