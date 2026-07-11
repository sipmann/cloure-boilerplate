# Testes de integração do login — notas de investigação

Notas do processo de investigação feito para escrever
`test/integration/microservice_boilerplate/login_test.clj`. O objetivo aqui é
documentar *por que* o teste foi escrito do jeito que foi, já que boa parte
disso não é óbvio olhando só o código do handler.

## Fluxo de login na aplicação

- `GET /login` (`ports/http_in/pages.clj`): se já existe `:user` na sessão, redireciona
  (302) para `/home`; caso contrário renderiza `authentication/login.html`.
- `POST /login`: valida `{:email s/Str :password s/Str}` (schema `LoginForm`)
  e compara com credenciais fixas (`admin@admin.com` / `admin`, hardcoded em
  `do-login`). Sucesso → 302 para `/home` + `:session {:user {...}}`. Falha →
  401 com body `"invalid email or password"`.
- A rota `/login` tem `:interceptors [(csrf/anti-forgery)]` no nível da rota
  (`routes/pages.clj`), então tanto GET quanto POST passam pelo interceptor de
  CSRF do Pedestal.

## Como o CSRF do Pedestal funciona (`io.pedestal.http.csrf`)

É um port do `ring-anti-forgery`. Pontos relevantes que não têm no código da
aplicação, só na lib:

- O token fica guardado em `(:session request)` sob a chave
  `"__anti-forgery-token"` — **não** existe cookie de double-submit aqui,
  porque a rota não passa `:cookie-token true` para `csrf/anti-forgery`.
- O token só é exposto ao client dentro do HTML renderizado
  (`templates.clj` chama `(csrf/anti-forgery-token request)`, que vira o
  hidden input `__anti-forgery-token` via `templates/render` +
  `csrf-field-html`).
- Para requests que não são GET/HEAD, o interceptor aceita o token em três
  lugares (`default-request-token`, em ordem):
  1. form param `:__anti-forgery-token` ou `"__anti-forgery-token"`
  2. header `x-csrf-token`
  3. header `x-xsrf-token`
- Se o token não bater (ou a sessão não tiver um), a resposta é `403` com
  body `"<h1>Invalid anti-forgery token</h1>"`.

**Decisão de teste:** em vez de mandar o token como campo do form (junto com
`email`/`password`), mandei via header `x-csrf-token`. Isso evita qualquer
risco de o `LoginForm` (schema fechado, só aceita `:email`/`:password`)
rejeitar a request por causa de uma chave extra no corpo do form durante a
coerção do reitit.

## Sessão: por que é preciso repassar cookie manualmente

A sessão (`ring-middlewares/session` em `router.clj`) usa o middleware padrão
do Ring (`ring.middleware.session`), com store em memória e cookie chamado
`ring-session` (default). Isso importa porque:

- `parenthesin.helpers.state-flow.server.pedestal/request!` usa
  `io.pedestal.test/response-for` por baixo dos panos, que **não mantém cookie
  jar entre chamadas** — cada `request!` é isolado. Se você não repassar o
  cookie manualmente, cada request começa uma sessão nova (e portanto um
  token CSRF novo, sem relação com o anterior).
- O header de resposta vem como `{"Set-Cookie" ("ring-session=XXX;Path=/;HttpOnly")}`
  — uma **lista** com um elemento (por causa de como
  `ring.middleware.cookies/cookies-response` usa `concat`/`update-in`, e de
  como o `io.pedestal.test` registra headers repetidos via `addHeader`).
  Por isso a extração do cookie faz `first` antes de dar `split` em `";"`.

## Detalhe sutil: sessão não muda de cookie ao logar

Ao investigar `ring.middleware.session/bare-session-response`, percebi que:

- Um login **bem-sucedido** sobrescreve o *conteúdo* da sessão (de
  `{"__anti-forgery-token" ...}` para `{:user {...}}`) mas mantém a mesma
  *chave* de sessão (o valor do cookie não muda). Por isso a resposta do
  POST de sucesso **não** vem com um novo `Set-Cookie` — o cliente continua
  usando o cookie que já tinha do GET original.
  - Consequência prática pro teste: para verificar "usuário autenticado é
    redirecionado ao acessar `/login` de novo", basta reusar o mesmo cookie
    capturado antes do login, não tem cookie novo pra extrair da resposta de
    sucesso.
- Um login **malsucedido** (401) não inclui `:session` na resposta do
  handler. Isso faz o `bare-session-response` do Ring cair no branch que
  **deleta** a sessão do store (efeito colateral real do código atual, não
  documentado em lugar nenhum). Não afeta a asserção do teste em si (só
  checamos o 401), mas é bom estar ciente: reusar aquele cookie depois de uma
  tentativa de senha errada não vai funcionar mais.

## Limitação encontrada ao rodar localmente (Windows)

`bb test` / `clj -M:test` iniciam um Postgres embarcado
(`pg-embedded-clj` + `io.zonky.test.embedded-postgres`) para os testes de
integração. Nesta máquina, a inicialização do Postgres falha com:

```
could not bind IPv6 address "::1": Permission denied
could not bind IPv4 address "127.0.0.1": Permission denied
FATAL: could not create any TCP/IP sockets
```

Isso acontece igualmente para os testes **já existentes**
(`wallet_test.clj`, `db_test.clj`) — não é algo introduzido pelo
`login_test.clj`, é uma restrição de rede/permissão do ambiente local (não
resolvida nem com sandbox desabilitado). Ou seja: não consegui rodar o
flow ponta a ponta aqui, só confirmar que o namespace **compila e carrega**
sem erro (`clojure.main -e "(require '...) "`).

Também vale notar: `deps.edn` fixa
`io.zonky.test.postgres/embedded-postgres-binaries-darwin-arm64v8` na alias
`:test`, o que sugere que o projeto foi configurado pensando em Mac — mas o
próprio `pg-embedded-clj` já resolve os binários certos pra cada SO (o log
mostra binário Windows sendo baixado/usado normalmente), então isso não foi
a causa do erro.

**Recomendação:** rodar `bb test` (ou `clj -M:test`) num ambiente onde o
Postgres embarcado já sobe (Mac, Linux, WSL, ou Windows sem essa restrição de
firewall/permissão) para validar o flow completo.
