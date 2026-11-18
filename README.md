# Spring Microservices POC: Mini Shop 🛒

## فكرة الـ POC

متجر صغير جدًا فيه حاجتين بس: **منتجات** و**أوردرات**.

- الـ **admin** يقدر يضيف ويعدّل ويمسح منتجات.
- الـ **user** يشوف المنتجات ويعمل أوردر.
- لما الـ user يعمل أوردر، الـ `order-service` بيكلّم الـ `product-service` (service-to-service call) عشان يتأكد إن المنتج موجود ويقلّل الـ stock، وبعدين يحسب السعر بعد الخصم ويحفظ الأوردر.
- الخصم والحد الأقصى للكمية جايين من **Config Server**، يعني تغيّرهم من غير ما تلمس الكود.

الفكرة بسيطة عشان التركيز يبقى على التكنولوجيات، مش على الـ business.

## الـ Architecture

```
                  ┌───────────── Kubernetes namespace: spring-poc ─────────────┐
 client ──► Ingress ──► api-gateway :8080                                      │
                  │        │  (Spring Cloud Gateway, lb://service-name)        │
                  │        ├──► product-service :8081 (x2) ──► Postgres/productdb
                  │        └──► order-service   :8082 ──Feign──► product-service
                  │                              └──────────► Postgres/orderdb │
                  │                                                            │
                  │   config-server :8888   ◄── كل الـ services بتجيب الـ config منه
                  │   discovery-server :8761 (Eureka) ◄── كل الـ services بتسجل نفسها فيه
                  └────────────────────────────────────────────────────────────┘

 Developer ─git push─► Jenkins: build ► test ► docker build/push ► bump imageTag in Git
                                                                        │
                                          ArgoCD watches Git ◄──────────┘ ─► helm render ─► kubectl apply
```

## هيكل المشروع

```
spring-poc/
├── pom.xml                  # Maven parent (Spring Boot 3.3 + Spring Cloud 2023.0)
├── config-server/           # Spring Cloud Config Server + config-repo/*.yml
├── discovery-server/        # Eureka Server
├── api-gateway/             # Spring Cloud Gateway
├── product-service/         # REST + JPA + Security
├── order-service/           # REST + JPA + Security + OpenFeign + IoC/DI demo
├── Dockerfile               # multi-stage, واحد لكل الـ modules (build-arg MODULE)
├── docker-compose.yml       # تشغيل كل حاجة locally مع Postgres
├── helm/spring-poc/         # Helm chart للـ Kubernetes
├── argocd/application.yaml  # ArgoCD Application (GitOps)
└── Jenkinsfile              # CI pipeline
```

---

## كل تكنولوجيا: هي إيه ومستخدمة فين

### 1. Spring Core (IoC & DI)
**الفكرة:** إنت مابتعملش `new` للـ objects بنفسك. الـ **IoC Container** (الـ `ApplicationContext`) هو اللي بيعمل الـ objects (اسمها **beans**) وبيحقنها (**Dependency Injection**) في اللي محتاجها.

**في الـ POC:**
- كل الـ classes بتاخد الـ dependencies بتاعتها في الـ **constructor** (constructor injection)، زي `OrderService` اللي بياخد `OrderRepository` و`ProductClient` و`DiscountPolicy` و`OrderProperties`.
- `DiscountPolicy` ده **interface** وليه implementationين: `NoDiscountPolicy` و`PercentageDiscountPolicy`. الـ `OrderService` مايعرفش هيستخدم أنهي واحد.
- `PricingConfig` فيها `@Bean` method بتقرر أنهي implementation تتعمل حسب الـ config (`discount-percent`). ده جوهر الـ IoC: الـ wiring في مكان، والـ business logic في مكان تاني.
- `@Service` و`@RestController` و`@Configuration` و`@Component` = stereotype annotations بتخلي الـ container يعمل scan ويسجل الـ beans.

📁 `order-service/.../config/PricingConfig.java`، `service/DiscountPolicy.java`، `service/OrderService.java`

### 2. Spring Boot
**الفكرة:** بيشيل عنك الـ setup: **auto-configuration** (يشوف الـ dependencies اللي عندك ويظبطها لوحده)، **starters** (dependency واحدة بتجيب كل اللي محتاجه)، **embedded Tomcat** (الـ app بيتشغل `java -jar` من غير server خارجي)، و**Actuator** للـ health checks.

**في الـ POC:**
- كل service عنده class فيه `@SpringBootApplication` و`main`.
- الـ starters في كل `pom.xml`: `spring-boot-starter-web`، `-data-jpa`، `-security`، `-actuator`، `-validation`.
- الـ Actuator بيدّي `/actuator/health/liveness` و`/readiness`، والـ Kubernetes probes بتستخدمهم.
- `CommandLineRunner` في `DataSeeder` بيحط بيانات تجريبية أول ما الـ app يقوم.

### 3. Spring Configuration
**الفكرة:** الـ config بره الكود (YAML/properties/env vars) وبتتربط بـ Java بشكل type-safe.

**في الـ POC:**
- **Java config:** `@Configuration` + `@Bean` (`SecurityConfig`، `PricingConfig`، `DataSeeder`).
- **`@ConfigurationProperties`:** `OrderProperties` (`poc.order.*`) و`SecurityProperties` (`poc.security.users`) متربطين بـ records، ومعاهم `@Validated` (لو حطيت discount 150 الـ app مش هيقوم).
- **Placeholders + env vars:** `${DB_URL:jdbc:h2:mem:productdb}` يعني استخدم الـ env var لو موجود، وإلا H2. نفس الكود يشتغل locally على H2 وفي Kubernetes على Postgres.
- **Externalized config:** كل الـ config موجود في `config-server/src/main/resources/config-repo/` (شوف رقم 7).

### 4. Spring Web / REST (MVC)
**الفكرة:** `@RestController` بيستقبل HTTP requests ويرجع JSON.

**في الـ POC:**
- `ProductController` و`OrderController` فيهم `@GetMapping` و`@PostMapping` و`@PutMapping` و`@DeleteMapping` و`@PathVariable` و`@RequestParam` و`@RequestBody`.
- **DTOs كـ Java records** (`ProductRequest`، `OrderResponse`...) عشان الـ API مايبقاش مربوط بالـ entity.
- **Validation:** `@Valid` + `@NotBlank` و`@Min` و`@DecimalMin`.
- **Error handling:** `GlobalExceptionHandler` (`@RestControllerAdvice`) بيرجع errors بصيغة `ProblemDetail` (RFC 7807).

| Method | Path | مين يقدر |
|---|---|---|
| GET | `/api/products` (`?inStock=true`) | USER |
| GET | `/api/products/{id}` | USER |
| POST / PUT / DELETE | `/api/products[/{id}]` | ADMIN |
| POST | `/api/products/{id}/decrease-stock` | USER (بيستخدمه order-service) |
| POST | `/api/orders` | USER |
| GET | `/api/orders/my`، `/api/orders/my/summary` | أي user مسجل |
| GET | `/api/orders` | ADMIN |

### 5. Spring Data JPA
**الفكرة:** بتكتب **interface** بس، وSpring بيعمل الـ implementation والـ SQL.

**في الـ POC:**
- `Product` و`Order` = `@Entity`، و`Product` فيه `@Version` للـ optimistic locking (لو اتنين اشتروا نفس المنتج في نفس اللحظة).
- `ProductRepository extends JpaRepository`: فيه `save` و`findAll` و`findById`... جاهزين، ومعاهم **derived queries** زي `findByNameIgnoreCase` و`findByStockGreaterThan`.
- `OrderRepository`: فيه derived query (`findByUsernameOrderByCreatedAtDesc`) و**JPQL** بـ `@Query` (`totalSpentBy`).
- `@Transactional` في الـ services. والـ update بيشتغل بالـ **dirty checking** من غير `save`.
- **H2** in-memory locally، و**PostgreSQL** في docker-compose وKubernetes (نفس الكود، الفرق env vars بس).

### 6. Spring Security
**الفكرة:** authentication (إنت مين؟) وauthorization (مسموحلك بإيه؟).

**في الـ POC:**
- `SecurityConfig`: `SecurityFilterChain` فيه **HTTP Basic** و**stateless** (من غير session) وCSRF مقفول لأنه REST API.
- الـ users جايين من الـ config (`poc.security.users`) والـ passwords متشفرة بـ **BCrypt**. في Kubernetes الـ passwords جاية من **Secret**.
- **Method security:** `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` على الـ endpoints.
- الـ `/actuator/health` مفتوح عشان الـ Kubernetes probes.
- الـ order-service بيعدّي الـ `Authorization` header للـ product-service عن طريق Feign `RequestInterceptor` (`FeignAuthForwardingConfig`).
- Users: `admin / admin123` (ADMIN, USER) و`user / user123` (USER).

> في production الأحسن تستخدم **OAuth2/JWT** (Keycloak مثلًا) و`spring-boot-starter-oauth2-resource-server`. اخترنا Basic هنا عشان الـ POC يفضل بسيط.

### 7. Spring Cloud / Microservices
| Component | دوره | فين |
|---|---|---|
| **Config Server** | كل الـ configs في مكان واحد. الـ services بتجيبها وقت الـ startup (`spring.config.import: configserver:`). `application.yml` مشترك و`<service-name>.yml` لكل service. | `config-server/` |
| **Eureka (Service Discovery)** | كل service بيسجل نفسه باسمه والـ IP بتاعه، فالـ services بتكلم بعض بالاسم مش بالـ IP. | `discovery-server/` |
| **API Gateway** | entry point واحد. بيعمل route لـ `/api/products/**` و`/api/orders/**` بـ `lb://service-name` (load balancing). ومعاه `GlobalFilter` بيحط `X-Correlation-Id`. | `api-gateway/` + `config-repo/api-gateway.yml` |
| **OpenFeign + LoadBalancer** | `ProductClient` مجرد interface، وSpring بيعمل الـ HTTP client. `name = "product-service"` بيتحل من Eureka، ولو فيه replicas كتير الطلبات بتتوزع عليهم. | `order-service/.../client/ProductClient.java` |

> ملحوظة: في Kubernetes ممكن تستغنى عن Eureka وتستخدم الـ K8s Services (`spring-cloud-kubernetes`). سيبنا Eureka عشان يبان الـ Spring Cloud pattern.

### 8. Docker
- `Dockerfile` واحد **multi-stage** لكل الـ modules:
  1. **build stage** (`maven:3.9-eclipse-temurin-21`) بيعمل build للـ module المطلوب بس (`-pl MODULE -am`) وبيعمل cache للـ dependencies في layer لوحدها.
  2. **runtime stage** (`eclipse-temurin:21-jre-alpine`) فيه JRE بس، وبيشتغل **non-root**، وبيستخدم **Spring Boot layered jar** (الـ dependencies في layer منفصلة، فالـ rebuild بيبقى أسرع والـ push أصغر).
- `docker-compose.yml` بيشغّل الـ 5 services ومعاهم Postgres وhealthchecks و`depends_on`.

### 9. Kubernetes
الـ objects اللي الـ chart بيعملها:
- **Deployment + Service** لكل microservice (`product-service` عنده **replicas: 2** عشان يبان الـ load balancing).
- **initContainer** بيستنى الـ config-server والـ eureka يقوموا.
- **startup/liveness/readiness probes** على الـ Actuator.
- **resources** (requests/limits) و`MaxRAMPercentage=75` عشان الـ JVM تحترم الـ container memory.
- **Secret** للـ passwords، و**StatefulSet + PVC** للـ Postgres، و**ConfigMap** فيه init script بيعمل الـ databases.
- **Ingress** بيوجّه على الـ api-gateway.

### 10. Helm Charts
`helm/spring-poc/` فيه chart واحد. `templates/services.yaml` بيعمل **loop** (`range`) على `.Values.services`، يعني إضافة microservice جديد = سطرين في `values.yaml` من غير template جديد. كل حاجة قابلة للتغيير من `values.yaml`: الـ registry والـ tag والـ replicas والـ ports والـ resources والـ passwords والـ ingress host.

### 11. Jenkins (CI)
`Jenkinsfile` (declarative pipeline):
1. **Build & Unit Test:** `mvn clean verify` وبينشر نتايج JUnit.
2. **Docker Build:** بيعمل build للـ 5 images **بالتوازي** (`parallel`) بـ tag = `BUILD_NUMBER-gitsha`.
3. **Docker Push** (على `main` بس): login بـ credentials من Jenkins Credentials Store.
4. **Update Helm values (GitOps):** بيعدّل `global.imageTag` في `values.yaml` وبيعمل commit وpush للـ Git.

Jenkins **مابيعملش deploy** بنفسه ومابيحتاجش kubeconfig. ده شغل ArgoCD.

> لازم تعرّف في Jenkins: tools باسم `maven-3.9` و`jdk-21`، وcredentials باسم `docker-registry-creds` و`git-creds`. وعشان الـ commit بتاع Jenkins مايعملش build تاني، فعّل الـ plugin بتاع `[skip ci]` أو اعمل exclude للـ user `jenkins`.

### 12. ArgoCD (CD / GitOps)
`argocd/application.yaml`:
- بيراقب `helm/spring-poc` في الـ Git repo (branch `main`).
- `automated` + `prune` + `selfHeal`: أي تغيير في Git بيتطبق لوحده، وأي تعديل يدوي بـ `kubectl` بيترجع زي ما هو في Git. يعني **Git = source of truth**.
- `CreateNamespace=true` بيعمل الـ namespace `spring-poc`.

**الـ flow كله:** `git push` ← Jenkins يعمل build وtest وimages وpush ← يغيّر الـ tag في Git ← ArgoCD يشوف التغيير ← `helm template` + apply ← Kubernetes يعمل rolling update.

---

## إزاي تشغّله

### أ) Locally من غير Docker (Java 21 + Maven)
```bash
mvn clean package
java -jar config-server/target/app.jar &
java -jar discovery-server/target/app.jar &
# استنى لحد ما الاتنين يقوموا (حوالي 15 ثانية)
java -jar product-service/target/app.jar &
java -jar order-service/target/app.jar &
java -jar api-gateway/target/app.jar &
```
Eureka dashboard: http://localhost:8761

### ب) Docker Compose
```bash
docker compose up --build
```

### ج) Kubernetes + Helm (minikube مثلًا)
```bash
minikube start --cpus 4 --memory 6g
minikube addons enable ingress
eval $(minikube docker-env)                     # الـ images تتبني جوه minikube
for m in config-server discovery-server api-gateway product-service order-service; do
  docker build --build-arg MODULE=$m -t spring-poc/$m:1.0.0 .
done
helm upgrade --install spring-poc helm/spring-poc -n spring-poc --create-namespace \
  --set global.imageRegistry=spring-poc
kubectl -n spring-poc get pods -w
kubectl -n spring-poc port-forward svc/api-gateway 8080:8080
```

### د) ArgoCD
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
# عدّل repoURL في argocd/application.yaml لـ الـ repo بتاعك، وبعدين:
kubectl apply -f argocd/application.yaml
kubectl -n argocd port-forward svc/argocd-server 8443:443   # UI على https://localhost:8443
```

### جرّب الـ API (عن طريق الـ Gateway)
```bash
curl -i localhost:8080/api/products                          # 401 من غير login
curl -u user:user123 localhost:8080/api/products             # list
curl -u user:user123 -X POST localhost:8080/api/products \
     -H 'Content-Type: application/json' -d '{"name":"Monitor","price":5000,"stock":5}'   # 403
curl -u admin:admin123 -X POST localhost:8080/api/products \
     -H 'Content-Type: application/json' -d '{"name":"Monitor","price":5000,"stock":5}'   # 201
curl -u user:user123 -X POST localhost:8080/api/orders \
     -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'               # 201، خصم 10%
curl -u user:user123 localhost:8080/api/products/1           # الـ stock قل 2
curl -u user:user123 localhost:8080/api/orders/my/summary
```

مثال response للأوردر (Laptop بـ 25000 × 2 بعد خصم 10%):
```json
{"id":1,"username":"user","productId":1,"productName":"Laptop","quantity":2,
 "totalPrice":45000.00,"discountPolicy":"PERCENTAGE_10","status":"CREATED"}
```

### Tests
```bash
mvn test
```
- `ProductApiTest`: الـ security (401/403) والـ validation (400) والـ stock (409).
- `OrderApiTest`: الـ IoC بيحقن `PercentageDiscountPolicy` من الـ config، والخصم بيتحسب صح، والـ limit جاي من الـ config. الـ `ProductClient` معمول له `@MockBean`.

---

## حاجات مقصود إنها بسيطة (ولو عايز تكبّرها)
- **Security:** HTTP Basic + in-memory users. الأحسن OAuth2/JWT مع Keycloak.
- **Consistency:** لو تقليل الـ stock نجح وحفظ الأوردر فشل، مفيش rollback بين الـ services. الحل الحقيقي **Saga** أو **Outbox** مع Kafka.
- **Resilience:** ممكن تضيف **Resilience4j** (circuit breaker وretry) على الـ Feign client.
- **Config:** الـ Config Server شغال `native` (من الـ classpath). في production خليه `git` backend.
- **Secrets:** الـ passwords في `values.yaml` للتجربة بس. استخدم **Sealed Secrets** أو **External Secrets** مع ArgoCD.
- **Observability:** Micrometer Tracing + Zipkin، وPrometheus + Grafana.
