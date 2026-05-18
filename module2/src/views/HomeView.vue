<template>
  <div class="page-wrapper home-page">
    <div class="container home-page__inner">

      <section class="home-hero motion-reveal" aria-labelledby="home-hero-title">
        <div class="home-hero__accent" aria-hidden="true" />
        <p class="home-eyebrow text-mono">Астрахань · Живая История</p>
        <h1 id="home-hero-title" class="home-hero-title">
          Открой город заново — через карты, маршруты и истории
        </h1>
        <p class="home-hero-lead">
          Интерактивный путеводитель по историческим объектам, авторским маршрутам
          и виртуальному музею деревянной архитектуры Астраханской области.
        </p>
        <div class="home-hero-cta">
          <router-link to="/virtual-museum" class="btn btn-primary btn-lg home-hero-cta__btn">
            Виртуальный музей →
          </router-link>
          <router-link to="/routes" class="btn btn-ghost btn-lg home-hero-cta__btn">
            Маршруты
          </router-link>
        </div>
      </section>

      <section class="home-features" aria-labelledby="home-features-title">
        <h2 id="home-features-title" class="sr-only">Возможности проекта</h2>

        <!--
          Каждая feature-card — это целиком кликабельная ссылка. Это убирает
          "фантомную интерактивность" (когда карточка реагирует на hover,
          но клик в центр ничего не делал). Внутри только семантические h3/p,
          без вложенных ссылок — структура валидна для <a>.
        -->
        <router-link
          v-for="card in featureCards"
          :key="card.to"
          :to="card.to"
          :class="['feature-card', { 'feature-card--accent': card.accent }]"
        >
          <span class="feature-card__icon" aria-hidden="true">{{ card.icon }}</span>
          <h3 class="feature-card__title">{{ card.title }}</h3>
          <p class="feature-card__desc">{{ card.desc }}</p>
          <span class="feature-card__cta" aria-hidden="true">{{ card.cta }} →</span>
        </router-link>
      </section>

      <section class="home-cta-band motion-reveal" aria-labelledby="home-cta-title">
        <div class="home-cta-band__inner">
          <h2 id="home-cta-title" class="home-cta-title">
            Хотите помочь сохранить деревянное наследие?
          </h2>
          <p class="home-cta-text">
            Деревянные дома Астрахани исчезают каждый год. Расскажите о месте,
            которого ещё нет на карте — мы рассмотрим и опубликуем.
          </p>
          <router-link to="/contact" class="btn btn-accent btn-lg">
            Связаться с нами
          </router-link>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
/**
 * HomeView — отдельная главная страница (ТЗ, раздел 4.1).
 *
 * До рефакторинга карта и музей жили в одном `MapView`. Теперь:
 *   /                → этот HomeView (лендинг с CTA на 4 раздела);
 *   /virtual-museum  → MapView (полный музей деревянного зодчества + карта);
 *   /map             → MapView (карта со всеми точками).
 */

const featureCards = [
  {
    to: '/map',
    icon: '◎',
    title: 'Карта объектов',
    desc: 'Исторические места, музеи, парки и архитектурные памятники Астрахани — с фильтром по категориям и GPS-навигацией.',
    cta: 'Открыть карту',
    accent: false,
  },
  {
    to: '/virtual-museum',
    icon: '▢',
    title: 'Виртуальный музей',
    desc: 'Деревянная архитектура Астраханской области в одном месте: фото, истории, аудио-гиды и MAX-плееры на странице каждой точки.',
    cta: 'Перейти в музей',
    accent: true,
  },
  {
    to: '/routes',
    icon: '→',
    title: 'Авторские маршруты',
    desc: 'Готовые прогулки от местных краеведов: с приоритетом, картой и сценарием для самостоятельного исследования.',
    cta: 'Смотреть маршруты',
    accent: false,
  },
  {
    to: '/shop',
    icon: '◇',
    title: 'Магазин и предзаказ',
    desc: 'Мерч с астраханскими мотивами. Положите в корзину — оставите предзаказ, а сотрудник свяжется с вами по Telegram или MAX.',
    cta: 'В магазин',
    accent: false,
  },
]
</script>

<style scoped>
.home-page {
  padding-bottom: var(--spacing-2xl);
  container-type: inline-size;
  container-name: home;
}

.home-page__inner {
  max-width: 1200px;
}

.home-hero {
  position: relative;
  padding: clamp(2rem, 6cqi, 4rem) 0 clamp(1.5rem, 4cqi, 3rem);
  max-width: 50rem;
}

.home-hero__accent {
  position: absolute;
  left: 0;
  top: 0;
  width: 3.5rem;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), transparent);
  border-radius: 2px;
}

.home-eyebrow {
  color: var(--accent);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: clamp(0.7rem, 0.4cqi + 0.6rem, 0.8rem);
  margin: 0 0 var(--spacing-sm);
}

.home-hero-title {
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 5cqi + 1rem, 3.6rem);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.025em;
  margin: 0;
  color: var(--cream, #f5ead0);
}

.home-hero-lead {
  margin: var(--spacing-lg) 0 0;
  color: var(--gray-400);
  font-size: clamp(0.95rem, 0.5cqi + 0.85rem, 1.1rem);
  line-height: 1.65;
  max-width: 46ch;
}

.home-hero-cta {
  margin-top: clamp(1.25rem, 3cqi, 2rem);
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.home-hero-cta__btn {
  min-height: 48px;
  padding-inline: clamp(1rem, 2cqi, 1.5rem);
}

.home-features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  gap: clamp(1rem, 2.5cqi, 1.75rem);
  margin-top: clamp(2rem, 5cqi, 3rem);
}

/* Карточка — целиком кликабельная ссылка, без вложенных интерактивных элементов */
.feature-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: clamp(1.1rem, 2.5cqi, 1.75rem);
  border: 1px solid rgba(212, 184, 150, 0.16);
  border-radius: var(--radius-md, 14px);
  background: var(--surface-card, linear-gradient(160deg, rgba(42, 35, 30, 0.45) 0%, rgba(20, 16, 13, 0.65) 100%));
  box-shadow: var(--shadow-card, 0 12px 28px rgba(0, 0, 0, 0.18));
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
}

.feature-card:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 3px;
}

.feature-card--accent {
  background: var(--surface-card-accent, linear-gradient(165deg, rgba(200, 169, 110, 0.16) 0%, rgba(20, 16, 13, 0.7) 100%));
  border-color: rgba(200, 169, 110, 0.45);
}

.feature-card__icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  color: var(--accent);
  border: 1px solid rgba(200, 169, 110, 0.35);
  border-radius: 50%;
  background: var(--surface-card-icon, rgba(20, 16, 13, 0.6));
}

.feature-card__title {
  font-family: var(--font-display);
  font-size: clamp(1.1rem, 1.5cqi + 0.85rem, 1.3rem);
  margin: 0;
  color: var(--cream, #f5ead0);
}

.feature-card__desc {
  margin: 0;
  color: var(--gray-400);
  font-size: 0.9rem;
  line-height: 1.6;
  flex: 1;
}

.feature-card__cta {
  align-self: flex-start;
  margin-top: var(--spacing-sm);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
}

.feature-card:hover .feature-card__cta {
  color: var(--accent-dark, var(--accent));
}

.home-cta-band {
  margin-top: clamp(2.5rem, 6cqi, 4rem);
  padding: clamp(1.5rem, 4cqi, 2.5rem);
  border-radius: var(--radius-md);
  border: 1px solid rgba(212, 184, 150, 0.22);
  background:
    radial-gradient(circle at top right, rgba(200, 169, 110, 0.18), transparent 60%),
    linear-gradient(180deg, rgba(42, 35, 30, 0.7) 0%, rgba(20, 16, 13, 0.85) 100%);
  box-shadow: var(--shadow-card);
}

.home-cta-band__inner {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  align-items: flex-start;
  max-width: 56ch;
}

.home-cta-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.4rem, 2.5cqi + 0.9rem, 2rem);
  color: var(--cream, #f5ead0);
  line-height: 1.15;
}

.home-cta-text {
  margin: 0;
  color: var(--gray-400);
  font-size: clamp(0.875rem, 0.4cqi + 0.85rem, 1rem);
  line-height: 1.65;
}

/* Mobile-first рассеивание hero на узких экранах */
@container home (max-width: 540px) {
  .home-hero-cta__btn {
    width: 100%;
    justify-content: center;
  }
}

/**
 * Light-тема: переопределяем хардкод-rgba на светлые поверхности,
 * чтобы текст (var(--cream) = #1a1512 в light) был контрастен на фоне карточек.
 * Vue scoped CSS добавит [data-v-xxxx] только к последнему селектору
 * (.feature-card / .home-cta-band) — родительский :root.app-light остается глобальным.
 */
:root.app-light .feature-card {
  background: linear-gradient(160deg, #ffffff 0%, #faf7f3 100%);
  border-color: rgba(0, 0, 0, 0.08);
  color: var(--paper);
}

:root.app-light .feature-card--accent {
  background: linear-gradient(165deg, rgba(184, 148, 94, 0.12) 0%, #fffaf2 100%);
  border-color: rgba(184, 148, 94, 0.35);
}

:root.app-light .feature-card__icon {
  background: rgba(184, 148, 94, 0.1);
  border-color: rgba(184, 148, 94, 0.35);
}

:root.app-light .home-cta-band {
  background:
    radial-gradient(circle at top right, rgba(184, 148, 94, 0.12), transparent 60%),
    linear-gradient(180deg, #ffffff 0%, #faf7f3 100%);
  border-color: rgba(0, 0, 0, 0.08);
}

@media (prefers-reduced-motion: reduce) {
  .feature-card {
    transition: none;
  }
  .feature-card:hover {
    transform: none;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
</style>
