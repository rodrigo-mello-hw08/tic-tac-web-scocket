# WebSocket API

Connect with STOMP at `/ws`. Send application messages to `/app/...` and subscribe to `/topic/...` destinations.

## Player registration and login

Subscribe to `/topic/players/{playerId}`. After registration, the server returns the generated numeric ID, which is used for subsequent login and player subscriptions.

- `/app/players/register`: `{ "name": "Ana", "password": "secret" }`
- `/app/players/login`: `{ "playerId": 1, "password": "secret" }`

Responses use `{ "type": "REGISTERED|LOGGED_IN|ERROR", "data": ..., "error": ... }`. Player data contains only `id` and `name`.

## Invitations and rooms

- `/app/invites/send`: `{ "fromPlayerId": 1, "toPlayerId": 2 }`
- Subscribe to `/topic/players/{id}` to receive invitations and responses.
- `/app/invites/respond`: `{ "inviteId": "...", "playerId": 2, "accepted": true }`
- On acceptance, both players receive `ROOM_JOINED`; the first `MATCH_STARTED` event is published to `/topic/rooms/{roomId}`.
- `/app/rooms/matches/next`: `{ "roomId": 1, "playerId": 1 }` starts another match after the current match has been marked complete by game logic.

Pending invitations are held in application memory. The room and its match history are stored in H2, which is in-memory and is cleared when the application stops.
