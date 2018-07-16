# brawl-haus

Welcome to the Brawl Haus repo, where the pub is being built!

Brawl Haus is a cooperative competition service, planned to have blind-typing oriented challenges as it's core.

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
