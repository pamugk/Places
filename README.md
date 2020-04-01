# Places
Андроид-приложение для помечивания мест на карте... нуу, метками.

## Функционал
1. Работа с активностями и Google-картами:
   - [x] Google-карта с возможностью проставления меток на ней;
   - [x] Создание фото, его привязывание к определённой точке и просмотр;
   - [x] Сохранение состояния приложения.
2. Работа с БД:
   - [x] Сохранение точек в БД (а именно, Realm).
3. Работа с местоположением:
   - [x] Выдача текущего местоположения на карте;
   - [x] Оповещение средствами Android при приближении к одной из точек.

## Хранение данных
Как уже было упомянуто, точки сохраняются в БД Realm. Для работы с БД проделано следующее:
* Добавлен класс точки MarkedMarker, наследующий от RealmObject;
* Добавлена инициализация БД при старте приложения;
* Определено поведение Realm при необходимости миграции (в классе RealmUtility);
* Добавлено получение Realm-соединения в onCreate MapsActivity;
* Все действия на получение/добавление/удаление/изменение_фото точек пущены через БД.

Всё это работает вот так:
1. Программа стартует и запускает инициализацию Realm, в ходе которой собирается информация обо всех классах, наследующих от RealmObject - именно объекты таких классов будут храниться в БД;
2. Создаётся MapsActivity (т.к. она назначена главной), вызывается её onCreate, в котором получается соединение к Realm. Конфигурация Realm формируется в getDefaultConfig класса getDefaultConfig, где указывается текущая версия схемы Realm и задано прямое указание на удаление Realm в случае, когда необходима миграция (т.е. когда были добавлены/удалены классы для хранения в Realm, либо когда были изменены поля уже имеющихся классов - по аналогии с тем, как если бы в реляционных БД понадобилось добавление/удаление таблиц либо изменнеие набора столбцов каких-нибудь таблиц);
3. Как только Google-карты заканчивают инициализацию, на них проставляются по координатам все хранящиеся в БД точки;
4. При добавлении/удалении точки и изменении фотографии для точки запросы для соответствующих изменений направляются в БД.

## Местоположения
### Получение данных о местоположении
* При создании MapsActivity производятся следующие действия:
  - Созаётся FusedLocationProviderClient, используемый для контроля местоположения
  - Создаётся и настраивается запрос на получение текущего местоположения. Настройки запроса таковы: выполнять каждые 20 секунд, данные о местоположении получать с максимально возможной точностью.
  - Создаётся функция обратного вызова для результатов запроса о местоположении. В этой функции по формуле длины ортодромии (реализованной по мотивам [специального случая формулы Винсенте](https://en.wikipedia.org/wiki/Great-circle_distance#Computational_formulas)) вычисляется расстояние между текущим местоположением и всеми точками в БД; если хотя бы одно из расстояний меньше 1 км., выдаётся оповещение (подробности по оповещениям в разделе "Оповещения");
* По завершении инициализации Google-карт при наличии разрешения на доступ к местоположению включается функционал отображения местоположения непосредственно на картах. Если разрешения нет, средствами класса PermissionUtils (взятого из примера Google по работе с местоположением) у пользователя запрашивается нужное разрешение, и уже потом включается функционал отслеживания местоположения.
* В onResume MapsActivity через FusedLocationProviderClient для созданного запроса  с указанным выше обратным вызовом начинается получение информации о текущем местоположении.
* В onPause получение информации о текущем местоположении прекращается, что было сделано для экономии ресурсов устройства, где запущено приложение.

### Оповещения
При запуске приложения создаётся канал для оповещений с именем, описанием и идентификатором, которые помогут однозначно его выделить среду других каналов; также идентификатор канала используется непосредственно при отправке оповещений.

С учётом специфики реализации отправки оповещений, получилось приложениие а-ля радар-детектор/счётчик_Гейгера/другая_надоедливая_пищалка - оповещения будут появляться каждые 20 секунд до тех пор, пока пользователь не окажется на расстоянии более 1 км. ото всех отмеченных на карте точек (что нисколько не противоречит ТЗ).
