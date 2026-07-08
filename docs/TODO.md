# TODO

## Propose a PR to `com.github.parenthesin/components`

`parenthesin.helpers.state-flow.server.pedestal/parsed-response` (in
`components-0.4.3.jar`) crashes with an NPE when a response has no
`Content-Type` header (e.g. a plain 302 redirect):

```clojure
(defn- parsed-response
  [{:keys [headers body] :as request}]
  (if (string/includes? (get headers "Content-Type") "application/json")
    (assoc request :body (json/decode body true))
    request))
```

`(get headers "Content-Type")` returns `nil` for such responses, and
`string/includes?` throws instead of treating a missing header as "not
JSON". We worked around it locally by always setting `Content-Type` on our
redirect responses (`src/microservice_boilerplate/ports/http_in.clj`), but
the helper itself should guard against a `nil` Content-Type, e.g.:

```clojure
(if (some-> (get headers "Content-Type") (string/includes? "application/json"))
  ...)
```

Evaluate opening a PR upstream with this fix.
