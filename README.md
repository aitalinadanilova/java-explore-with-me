# ExploreWithMe (EWM) — Сервис для организации событий

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

**ExploreWithMe** — это многофункциональный бэкенд-сервис, который позволяет пользователям находить компанию для совместного досуга. Пользователи могут создавать события, записываться на мероприятия других участников и оценивать качество организации.


## 🛠 Технологический стек
*   **Язык программирования:** Java 17
*   **Фреймворк:** Spring Boot 3 (Spring Data JPA, Spring Validation)
*   **База данных:** PostgreSQL (основная), H2 (тестовая)
*   **Контейнеризация:** Docker & Docker Compose
*   **Сборка:** Maven
*   **Инструментарий:** Postman, Lombok, MapStruct


##  Реализованная функциональность: Rating Events
Мною была спроектирована и реализована система рейтингов для мероприятий и их организаторов.

**Ключевые возможности:**
*   **Оценка событий:** Лайки и дизлайки могут ставить только подтвержденные участники события (статус `CONFIRMED`).
*   **Алгоритм рейтинга:** Рейтинг события рассчитывается динамически как разница между лайками и дизлайками.
*   **Рейтинг организатора:** Профиль пользователя включает в себя совокупный рейтинг, основанный на оценках всех его созданных событий. Это помогает пользователям выбирать надежных организаторов.
*   **Сортировка по популярности:** Добавлен функционал получения списка событий, отсортированных по рейтингу (от самых популярных к менее успешным).
*   **Целостность данных:** Реализована проверка на попытку повторной оценки или оценку собственного события (защита от накрутки).

---

## Запуск проекта

Для запуска приложения необходимо иметь установленные Docker и Docker Compose.
Соберите проект:
mvn clean package -DskipTests
Запустите через Docker:
docker-compose up

Сервис будет доступен по адресу http://localhost:8080.

1. **Клонируйте репозиторий:**
   ```bash
   git clone [https://github.com/aitalinadanilova/java-explore-with-me.git](https://github.com/aitalinadanilova/java-explore-with-me.git)
