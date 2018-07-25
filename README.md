# brawl-haus

Welcome to the Brawl Haus repo, where the pub is being built!

Brawl Haus is a cooperative competition service, planned to have blind-typing oriented challenges as it's core.

## Mindset

### State is what we care
Over years we've been struggling with accidential complexity of inter-connected mutable objects, populating front-end of our application, brought tools thought to be fancy in those times.

We've had the same problem just before that - having mutable objects of data+operations in run-time at our back-end. We've been smart enough to find a solution - decouple state out of operations and store it in non-volatile memory.

Recently we've been smart enough to apply the same principle to front-ends as well - 'flux architecture' - having one big state at the very top of our __client__ and propagating it down to the end-nodes as it mutates, driven by events, produced by end-nodes.

Just the same, and it serves us well.


Hovewer there is one more complexity to be purged out of our current systems.


### The real state is out there.

State of the server is the source of truth.

Our clients used to requset a snapshot of this state in order to allow user to operate on it. And here comes the problem - this snashot of the world gets old, and does it fast.

So our user spends some time poking around in this state: inspecting some of it, thinking, maybe filling some form; and then tries to act and - whoopsie, you can't do that! The world had changed since you've taken a look at it last time, this operation is no more valid... form has been filled by someone else by now, sorry!

Such a pleasant experience.


So, what do we need? You guessed it! Keep clients in sync with the world.

Welcome websockets! Those little channels allow bidirectional/async client-server communication. Just the tool we need to tell our clients "now the world is such and such, please inform the user"

### How it's done, in a shortie
We've good an ol' good SPA as our client, state in one place, propagating down, however the idea of "putting state to the top" was put to extreme - we keep __all the state__ our client may care at the __truly top__ of our application - **server**!
It propagates down to clients and then down to end-nodes, all in sync, clients driven!

Just the simplicity of web2.0(view being simple data representation), but with a reactive data flow and all the goodies we can do with thick and tasty SPAs at client side.



### Thesis

Server is the source of truth, *it* is our FSM - one big chunk of state, being driven by events.
It's being synced to clients via a websocket

The idea is pushed to extreme - even current position of a client in the app kept at server side

Clients can emit events to drive the state

## Enough about concepts, show me how it's done!

Server side got just two routes:
- `/` - serves SPA
- `/tube` - knock there to init a ws connection

State is kept in one big `(def public-state (atom {}))`

It has a watcher, listening to changes and pushing current value of it to connected websockets.

It's event-driven, means clients emit `events` in order to drive the state

Event is a simple vector. The structure is:
`[<event-id> <event-params>]`


Event can be dispatched from client-side via `:tube/send` re-frame's event:
```
(re-frame/dispatch [:tube/send <event>])
```

It's being handled at server-side by `event receiver`.
`event receiver` is a routing mechanism, allowing to register an `event handler` function by `<event-id>`, as follows:
```
(receiver
  {<event-id> (fn [tube event]
                <handle-logic>)
   })
```

`event` is the event we emited from client-side

`tube` is a slight abstraction over websocket, being used to emmit back events and serve as id of a connection

We emit back only one event - "now current state of the world is <VALUE>"


## Wuf? It's all there is!

Having all state of the world at disposal and being event-driven it looks much like a FSM, with a hell of an easy interface and reactive support for multiple clients.

It drives crazy out of me. :shivers:

And now so you.;)


## Developer mode

You'll need local clojure environment (see later for it) and do steps:

### Compile css:

Compile css file once.

```
lein garden once
```

Or better automatically recompile css file on change.

```
lein garden auto
```


### Run application:

To launch server-side:
```
lein repl
=> (reload)
```

To launch client-side:
```
lein figwheel
```

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

Figwheel will automatically push cljs changes to the browser.


Engineer as bold as you dare!


## Launch from docker

You'll need docker installed and one command to go:

```
$ docker run andrewzhurov/brawl-haus
```
Browse http://localhost:9090


## Local clojure environment

Requires:
- `clojure` itself https://clojure.org/guides/getting_started#_installation_on_linux
- `leiningen` project build tool https://leiningen.org/#install

## Better mindset
For wider, deeper understanding of concepts those talks I find amazing:

  * [Flux architecture](https://facebook.github.io/flux/)
  * ['Make frontend backend again'](https://youtu.be/XBfi3Q74BnE)

      History of war against accidential complexity of applications, where we are now and what the next step could be
      Thanks to @niqola for the avesome talk, it served me as source of knowledge and inspiration. Crucial.
  * More on on Clojure ecosystem and complexity it purges:
    * https://youtu.be/b-Eq4YV4uwc
    * and all you're able find in youtube for 'Rich Hicky' and 'Николай Рыжиков clojure', exceptional goodies.

---

Much thanks for your time!

If you've got inspired by the architecture approach, would love to participate in the app development or just get in touch with Clojure - contact me on the spot.

I'd love to meet likeminded people, share knowledge, help, mentor and be mentored

I practice screensharing sessions, so if you want to know more in any part (basic Clojure, SPA, how fullstack is done, more about the architecture or any other) - I'm eager to share, welcome to knock and poke me with all your questions!

Curiosity FTW ;)


- [Clojurians](https://clojurians.slack.com) @andrewzhurov
- [Hexlet](https://hexlet-ru.slack.com) @andrewzhurov
- zhurovandrew@gmail.com
- [Twitter](https://twitter.com/andrewzhurov)
