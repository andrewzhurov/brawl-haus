# brawl-haus

Welcome to the Brawl Haus repo, where the pub is being built!

Brawl Haus is a cooperative competition service, planned to have blind-typing oriented challenges as it's core.

## Mindset
All state is kept on the server side

It's being synced to clients via a websocket

The idea is pushed to extreme - even current position of a client in the app kept at server side

Clients can emit events to drive the state


Event structure is:
`[<event-id> <event-params>]`


It can be dispatched from the client side via `:tube/send` re-frame's event:
```
(re-frame/dispatch [:tube/send [:event-id {:an-event-param :a-val}]])
```

It's being handled by the received handler at server side, we can register event handler by `<event-id>` as follows:
```
(receiver
  {<event-id> (fn [tube evt]
                <handle-logic>)
   })
```

`tube` is a slight abstraction over websocket, being used to emmit back events and serve as id of a connection

`evt` is a full event we emited from server




Having all state of the world at disposal and being event-driven it looks much like a FSM, with a hell of an easy interface and reactive support for multiple clients.

It drives crazy out of me. :shivers:

And now so you.;)

## Developer mode

### Compile css:

Compile css file once.

```
lein garden once
```

Automatically recompile css file on change.

```
lein garden auto
```

CSS also gets automatically built when running `lein build` and recompiled on change with `lein dev`.

### Run application:

To launch server part:
```
lein repl
=> (reload)
```

To launch front-end part:
```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).
