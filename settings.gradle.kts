rootProject.name = "2025-externship-be-astor-dev"

include("gateway")

include("outbox")
include("outbox:worker")

include("short-url")


include("api")
include("api:url-service")
include("api:redirect-service")
include("api:stats-service")

include("domain")
include("domain:resolved-short-url")
include("domain:short-url-stats")

include("util")
include("util:object-mapper")
include("util:distributed-lock")


include("worker")
include("worker:short-url-stats-consumer")
include("worker:short-url-stats-batch")
