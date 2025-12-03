rootProject.name = "2025-externship-be-astor-dev"

include("api")
include("api:gateway")
include("api:url-service")
include("api:redirect-service")

include("domain")
include("domain:outbox")
include("domain:short-url")
include("domain:resolved-short-url")

include("util")
include("util:object-mapper")
include("util:distributed-lock")

