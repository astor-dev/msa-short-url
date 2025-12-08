rootProject.name = "2025-externship-be-astor-dev"

include("gateway")

include("outbox")
include("outbox:worker")

include("short-url")
include("short-url:api:url-service")
include("short-url:api:redirect-service")

include("short-url-stats")
include("short-url-stats:batch")
include("short-url-stats:consumer")
include("short-url-stats:api:stats-service")

include("util")
include("util:object-mapper")
include("util:distributed-lock")

include("traffic-generator")