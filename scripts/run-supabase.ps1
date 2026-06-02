param(
  [Parameter(Mandatory=$true)][string]$DbUrl,
  [Parameter(Mandatory=$true)][string]$DbUser,
  [Parameter(Mandatory=$true)][string]$DbPassword
)

$env:SUPABASE_DB_URL=$DbUrl
$env:SUPABASE_DB_USER=$DbUser
$env:SUPABASE_DB_PASSWORD=$DbPassword

mvn spring-boot:run
