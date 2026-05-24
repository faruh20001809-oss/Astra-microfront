/** Стабильные фикстуры для моков Java API (module2). */

export const MOCK_POIS = [
  {
    id: 1,
    name: 'Дом Тетюшинова',
    category: 'архитектура',
    status: 'PUBLISHED',
    latitude: 46.3497,
    longitude: 48.0408,
    year: 1890,
    shortDescription: 'Образец деревянного зодчества конца XIX века.',
    address: 'ул. Советская, 15, Астрахань',
    style: 'деревянное зодчество',
  },
  {
    id: 2,
    name: 'Усадьба купца Варгина',
    category: 'архитектура',
    status: 'PUBLISHED',
    latitude: 46.3453,
    longitude: 48.0331,
    year: 1912,
    shortDescription: 'Купеческий особняк с резным декором.',
    address: 'ул. Кирова, 8, Астрахань',
  },
  {
    id: 3,
    name: 'Дом с виноградной лозой',
    category: 'музеи',
    status: 'PUBLISHED',
    latitude: 46.3512,
    longitude: 48.0452,
    year: 1901,
    shortDescription: 'Фасад с характерным орнаментом.',
  },
]

export const MOCK_ROUTES = [
  {
    id: 1,
    title: 'Кремль и его окрестности',
    name: 'Кремль и его окрестности',
    price: 0,
    published: true,
    category: 'история',
    coverImage: null,
    description: 'Пешая прогулка по историческому центру.',
    thematicDescription: 'Исторический центр Астрахани — сердце города с Кремлём и соборами.',
    videoUrls: [],
    audioUrls: [],
    duration: '90 мин',
    distance: '2.5 км',
    isPaid: false,
    pois: [1, 2],
    stops: [
      {
        poiId: 1,
        name: 'Дом Тетюшинова',
        description: 'Деревянное зодчество',
        thematicDescription: 'На этом маршруте дом рассматриваем как въездную точку в купеческий квартал.',
        videoUrls: [],
        audioUrls: [],
      },
      {
        poiId: 2,
        name: 'Усадьба Варгина',
        description: 'Купеческий особняк',
        thematicDescription: 'Здесь акцент на резном декоре и торговых связях Астрахани.',
        videoUrls: [],
        audioUrls: [],
      },
    ],
    durationMinutes: 90,
  },
  {
    id: 2,
    title: 'Купеческая Астрахань',
    name: 'Купеческая Астрахань',
    price: 490,
    published: true,
    category: 'архитектура',
    coverImage: null,
    description: 'Маршрут по купеческим особнякам.',
    thematicDescription: 'Купеческие особняки и торговые ряды XIX века.',
    videoUrls: [],
    audioUrls: [],
    duration: '120 мин',
    distance: '4 км',
    isPaid: true,
    paid: true,
    pois: [2, 3],
    stops: [{ name: 'Усадьба Варгина', description: 'Резной декор' }],
    durationMinutes: 120,
  },
]

export const MOCK_PRODUCTS = [
  {
    id: 1,
    name: 'Футболка «Астрахань»',
    price: 1290,
    category: 'одежда',
    image: null,
    description: 'Хлопок, унисекс.',
  },
  {
    id: 2,
    name: 'Кружка «Живая история»',
    price: 890,
    category: 'сувениры',
    image: null,
    description: 'Керамика 350 мл.',
  },
]

export function javaSuccess<T>(data: T) {
  return { status: 'success', data }
}
