rootProject.name = "2025-externship-be-astor-dev"

include("api")
include("api:gateway")
include("api:url-service")
include("api:redirect-service")

include("domain")
include("domain:short-url")

include("util")