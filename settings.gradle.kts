rootProject.name = "2025-externship-be-astor-dev"

include("gateway")

include("outbox")
include("outbox:worker")

include("short-url")
include("short-url:api:url-service")
include("short-url:api:redirect-service")

include("short-url-stats")
include("short-url-stats:batch")

include("api")
include("api:stats-service")

include("domain")
include("domain:resolved-short-url")

include("util")
include("util:object-mapper")
include("util:distributed-lock")


include("worker")
include("worker:short-url-stats-consumer")
