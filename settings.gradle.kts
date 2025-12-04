rootProject.name = "2025-externship-be-astor-dev"

include("api")
include("api:gateway")
include("api:url-service")
include("api:redirect-service")
include("api:stats-service")

include("domain")
include("domain:outbox")
include("domain:short-url")
include("domain:resolved-short-url")
include("domain:short-url-stats")

include("util")
include("util:object-mapper")
include("util:distributed-lock")


include("worker")
include("worker:outbox-polling-publisher")
include("worker:short-url-stats-processor")
include("worker:short-url-stats-batch")
