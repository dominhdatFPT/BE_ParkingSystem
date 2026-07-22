param(
    [string]$SourceRoot = (Join-Path $PSScriptRoot "..\src\main\java"),
    [string]$OutputFile
)

$ErrorActionPreference = "Stop"
$resolvedRoot = (Resolve-Path $SourceRoot).Path
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$files = Get-ChildItem -LiteralPath $resolvedRoot -Recurse -Filter *.java | Sort-Object FullName
$results = [System.Collections.Generic.List[object]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new()
$entityCatalog = @{}
$repositoryEntities = @{}
$repositoryVariables = @{}

function Convert-ToSnakeCase {
    param([string]$Value)
    return ([regex]::Replace($Value, '([a-z0-9])([A-Z])', '$1_$2')).ToLowerInvariant()
}

function Initialize-SourceCatalog {
    $modelRoot = Join-Path $resolvedRoot "com\swp\parking\model"
    foreach ($modelFile in Get-ChildItem -LiteralPath $modelRoot -File -Filter *.java) {
        $text = Get-Content -LiteralPath $modelFile.FullName -Raw -Encoding UTF8
        if ($text -notmatch '@Entity') { continue }

        $entityName = [System.IO.Path]::GetFileNameWithoutExtension($modelFile.Name)
        $tableMatch = [regex]::Match($text, '@Table\(name\s*=\s*"(?<name>[^"]+)"')
        $tableName = if ($tableMatch.Success) { $tableMatch.Groups["name"].Value } else { Convert-ToSnakeCase $entityName }
        $properties = @{}
        $relations = @{}
        $pendingColumn = $null
        $pendingJoin = $null
        $isRelation = $false

        foreach ($line in Get-Content -LiteralPath $modelFile.FullName -Encoding UTF8) {
            $columnMatch = [regex]::Match($line, '@Column\([^)]*name\s*=\s*"(?<name>[^"]+)"')
            if ($columnMatch.Success) { $pendingColumn = $columnMatch.Groups["name"].Value }
            $joinMatch = [regex]::Match($line, '@JoinColumn\([^)]*name\s*=\s*"(?<name>[^"]+)"')
            if ($joinMatch.Success) { $pendingJoin = $joinMatch.Groups["name"].Value }
            if ($line -match '@(?:ManyToOne|OneToOne|OneToMany|ManyToMany)') { $isRelation = $true }

            $fieldMatch = [regex]::Match($line, '^\s*private\s+(?<type>[A-Za-z0-9_<>., ?]+)\s+(?<name>[A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)')
            if (!$fieldMatch.Success) { continue }

            $property = $fieldMatch.Groups["name"].Value
            $type = ($fieldMatch.Groups["type"].Value -replace '<.*', '').Trim()
            $column = if ($pendingJoin) { $pendingJoin } elseif ($pendingColumn) { $pendingColumn } else { Convert-ToSnakeCase $property }
            $properties[$property] = $column
            if ($isRelation) { $relations[$property] = $type }
            $pendingColumn = $null
            $pendingJoin = $null
            $isRelation = $false
        }

        $entityCatalog[$entityName] = [pscustomobject]@{
            Entity = $entityName
            Table = $tableName
            Properties = $properties
            Relations = $relations
        }
    }

    $repositoryRootPath = Join-Path $resolvedRoot "com\swp\parking\repository"
    foreach ($repositoryFile in Get-ChildItem -LiteralPath $repositoryRootPath -File -Filter *Repository.java) {
        $text = Get-Content -LiteralPath $repositoryFile.FullName -Raw -Encoding UTF8
        $match = [regex]::Match($text, 'JpaRepository<\s*(?<entity>[A-Za-z0-9_]+)\s*,')
        if ($match.Success) {
            $repositoryEntities[[System.IO.Path]::GetFileNameWithoutExtension($repositoryFile.Name)] =
                $match.Groups["entity"].Value
        }
    }

    foreach ($javaFile in $files) {
        $text = Get-Content -LiteralPath $javaFile.FullName -Raw -Encoding UTF8
        foreach ($match in [regex]::Matches(
            $text,
            '(?:private\s+final|private)\s+(?<repo>[A-Za-z0-9_]+Repository)\s+(?<var>[A-Za-z_][A-Za-z0-9_]*)\s*;'
        )) {
            $repo = $match.Groups["repo"].Value
            if ($repositoryEntities.ContainsKey($repo)) {
                $repositoryVariables[$match.Groups["var"].Value] = $repositoryEntities[$repo]
            }
        }
    }
}

Initialize-SourceCatalog

function Get-LineNumber {
    param([string]$Text, [int]$Index)
    if ($Index -le 0) { return 1 }
    return ([regex]::Matches($Text.Substring(0, $Index), "`n").Count + 1)
}

function Add-Query {
    param(
        [string]$Kind,
        [string]$File,
        [int]$Line,
        [string]$Query,
        [string]$Context = ""
    )

    $clean = $Query.Trim()
    if ([string]::IsNullOrWhiteSpace($clean)) { return }
    $key = "$File|$Line|$Kind|" + ($clean -replace "\s+", " ")
    if (!$seen.Add($key)) { return }

    $results.Add([pscustomobject]@{
        Kind = $Kind
        File = $File
        Line = $Line
        Query = $clean
        Context = $Context.Trim()
    })
}

function Get-DomainName {
    param([string]$File, [string]$Query)

    $name = [System.IO.Path]::GetFileNameWithoutExtension($File)
    $name = $name -replace '(Repository|Service|Controller|Initializer)$', ''
    $labels = @{
        "Booking" = "đặt chỗ"
        "Card" = "thẻ gửi xe"
        "Customer" = "khách hàng"
        "DeviceToken" = "token thiết bị"
        "FeePackage" = "gói phí"
        "FeePackagePriceHistory" = "lịch sử giá gói phí"
        "FeeSubscription" = "đăng ký gói phí"
        "FeeSubscriptionInvoice" = "hóa đơn gói phí"
        "Notification" = "thông báo"
        "ParkingAreaSummary" = "tổng hợp khu vực đỗ xe"
        "ParkingEntry" = "xe vào bãi"
        "ParkingExit" = "xe ra bãi"
        "ParkingFloor" = "tầng đỗ xe"
        "ParkingOrder" = "phiên/đơn gửi xe"
        "ParkingSlot" = "ô đỗ xe"
        "ParkingZone" = "khu vực đỗ xe"
        "OperationsDashboard" = "dashboard vận hành"
        "SystemData" = "cấu hình, sự cố và audit hệ thống"
        "User" = "người dùng"
        "Vehicle" = "phương tiện"
        "VehicleRegistration" = "đăng ký phương tiện"
        "VehicleType" = "loại phương tiện"
        "SupportSchema" = "schema hỗ trợ"
    }
    if ($labels.ContainsKey($name)) { return $labels[$name] }

    $tableMatch = [regex]::Match(
        $Query,
        '(?is)\b(?:FROM|INTO|UPDATE|ALTER\s+TABLE|CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?)\s+([A-Za-z_][A-Za-z0-9_]*)'
    )
    if ($tableMatch.Success) {
        return "dữ liệu bảng ``$($tableMatch.Groups[1].Value)``"
    }
    return "dữ liệu nghiệp vụ"
}

function Get-QueryTypeVietnamese {
    param([object]$Item)

    $query = $Item.Query.Trim()
    $upper = $query.ToUpperInvariant()

    switch ($Item.Kind) {
        "Supabase-ready SQL equivalents" {
            return "PostgreSQL chạy trực tiếp trên Supabase; truy vấn ``SELECT`` chỉ đọc."
        }
        "Spring Data repository method" {
            if ($query -match '\bexists') { return "Spring Data Derived Query dạng ``EXISTS``; Hibernate tự sinh SQL từ tên method." }
            if ($query -match '\bcount') { return "Spring Data Derived Query dạng ``COUNT``; Hibernate tự sinh SQL từ tên method." }
            if ($query -match '\bdelete') { return "Spring Data Derived Query dạng ``DELETE``; Hibernate tự sinh SQL từ tên method." }
            return "Spring Data Derived Query dạng ``SELECT``; Hibernate tự sinh SQL từ tên method."
        }
        "Inherited JpaRepository operation" {
            if ($query -match '\.save(All)?\s*\(') { return "Thao tác ghi ``INSERT/UPDATE`` kế thừa từ ``JpaRepository``." }
            if ($query -match '\.delete') { return "Thao tác ``DELETE`` kế thừa từ ``JpaRepository``." }
            if ($query -match '\.existsById') { return "Thao tác kiểm tra tồn tại ``EXISTS`` kế thừa từ ``JpaRepository``." }
            return "Thao tác đọc ``SELECT`` kế thừa từ ``JpaRepository``."
        }
        "JPQL/native @Query" {
            if ($query -match '(?i)\b(?:FROM|JOIN|INTO|UPDATE)\s+[a-z][a-z0-9_]*\b' -and
                ($query -match '_' -or $query -match '::|LATERAL|LIMIT')) {
                return "Native SQL PostgreSQL khai báo bằng ``@Query``."
            }
            return "JPQL/HQL khai báo bằng ``@Query``; Hibernate chuyển thành SQL PostgreSQL khi chạy."
        }
        "PreparedStatement SQL" {
            return "Native SQL PostgreSQL dùng ``PreparedStatement`` và tham số ``?``."
        }
        default {
            if ($upper -match '^\s*(WITH|SELECT)') { return "Native SQL PostgreSQL dạng ``SELECT`` qua ``JdbcTemplate``; chỉ đọc." }
            if ($upper -match '^\s*INSERT') { return "Native SQL PostgreSQL dạng ``INSERT`` qua ``JdbcTemplate``." }
            if ($upper -match '^\s*UPDATE') { return "Native SQL PostgreSQL dạng ``UPDATE`` qua ``JdbcTemplate``." }
            if ($upper -match '^\s*DELETE') { return "Native SQL PostgreSQL dạng ``DELETE`` qua ``JdbcTemplate``." }
            if ($upper -match '^\s*ALTER') { return "DDL PostgreSQL dạng ``ALTER TABLE``; thay đổi schema." }
            if ($upper -match '^\s*CREATE') { return "DDL PostgreSQL dạng ``CREATE``; tạo bảng hoặc index." }
            return "Native SQL PostgreSQL được thực thi từ Java."
        }
    }
}

function Get-QueryPurposeVietnamese {
    param([object]$Item)

    $query = $Item.Query.Trim()
    $normalized = $query -replace '\s+', ' '
    $domain = Get-DomainName -File $Item.File -Query $query

    if ($Item.Kind -eq "Supabase-ready SQL equivalents") {
        if ($normalized -match 'WAITING_STAFF_APPROVAL') {
            return "Lấy danh sách đặt chỗ đang chờ nhân viên duyệt, kèm các quan hệ cần hiển thị."
        }
        if ($normalized -match 'WHERE b\.user_id') {
            return "Lấy lịch sử đặt chỗ của một người dùng, sắp xếp mới nhất trước."
        }
        if ($normalized -match 'WHERE b\.id') {
            return "Lấy chi tiết một đặt chỗ theo ``booking_id``."
        }
    }

    if ($Item.Kind -eq "Spring Data repository method") {
        $method = [regex]::Match($query, '\b(find|exists|count|delete)[A-Z][A-Za-z0-9_]*').Value
        if ($method -match '^exists') {
            return "Kiểm tra sự tồn tại của $domain theo điều kiện mã hóa trong tên method ``$method``."
        }
        if ($method -match '^count') {
            return "Đếm số bản ghi $domain theo điều kiện mã hóa trong tên method ``$method``."
        }
        if ($method -match '^delete') {
            return "Xóa $domain theo điều kiện mã hóa trong tên method ``$method``."
        }
        return "Đọc $domain theo bộ lọc và thứ tự được mã hóa trong tên method ``$method``."
    }

    if ($Item.Kind -eq "Inherited JpaRepository operation") {
        if ($query -match '\.findAll\s*\(') { return "Lấy toàn bộ $domain từ database." }
        if ($query -match '\.findById\s*\(') { return "Lấy một bản ghi $domain theo khóa chính." }
        if ($query -match '\.existsById\s*\(') { return "Kiểm tra bản ghi $domain có tồn tại theo khóa chính hay không." }
        if ($query -match '\.saveAll\s*\(') { return "Lưu nhiều bản ghi $domain; Hibernate quyết định ``INSERT`` hoặc ``UPDATE``." }
        if ($query -match '\.save\s*\(') { return "Lưu $domain; Hibernate quyết định ``INSERT`` hoặc ``UPDATE`` theo trạng thái entity." }
        if ($query -match '\.deleteById\s*\(') { return "Xóa $domain theo khóa chính." }
        if ($query -match '\.deleteAll\s*\(') { return "Xóa toàn bộ $domain trong phạm vi lời gọi." }
        return "Thực hiện thao tác CRUD với $domain thông qua ``JpaRepository``."
    }

    if ($normalized -match '(?i)^ALTER\s+TABLE') {
        return "Thay đổi cấu trúc $domain để bổ sung hoặc điều chỉnh cột phục vụ backend."
    }
    if ($normalized -match '(?i)^CREATE\s+(?:TABLE|INDEX)') {
        return "Tạo cấu trúc database cho $domain nếu chưa tồn tại."
    }
    if ($normalized -match '(?i)^INSERT') {
        return "Thêm dữ liệu $domain vào database."
    }
    if ($normalized -match '(?i)^UPDATE') {
        return "Cập nhật $domain theo điều kiện trong mệnh đề ``WHERE``."
    }
    if ($normalized -match '(?i)^DELETE') {
        return "Xóa $domain theo điều kiện trong mệnh đề ``WHERE``."
    }
    if ($normalized -match '(?i)\bCOUNT\s*\(') {
        return "Đếm hoặc tổng hợp số liệu $domain để phục vụ kiểm tra và dashboard."
    }
    if ($normalized -match '(?i)\bSUM\s*\(') {
        return "Tổng hợp giá trị $domain, thường dùng cho thống kê doanh thu hoặc chỉ số vận hành."
    }
    if ($normalized -match '(?i)WAITING_STAFF_APPROVAL') {
        return "Lấy các đặt chỗ đang chờ nhân viên duyệt cùng dữ liệu liên quan."
    }
    if ($normalized -match '(?i)vehicle_registrations' -and $normalized -match '(?i)ORDER BY.*created') {
        return "Lấy danh sách đăng ký phương tiện kèm thông tin người dùng, loại xe và gói phí để hiển thị."
    }
    if ($normalized -match '(?i)fee_package_price_history|LATERAL') {
        return "Lấy gói phí đang hoạt động và mức giá hiệu lực gần nhất trong một lần truy vấn."
    }
    if ($normalized -match '(?i)FOR UPDATE') {
        return "Đọc và khóa bản ghi $domain để tránh hai request xử lý đồng thời cùng một dữ liệu."
    }
    if ($normalized -match '(?i)\b(?:user_id|user\.id)\b') {
        return "Đọc $domain thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới."
    }
    if ($normalized -match '(?i)\bstatus\b') {
        return "Đọc $domain theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response."
    }
    if ($normalized -match '(?i)\b(?:is_active|isActive)\b') {
        return "Đọc các bản ghi $domain đang hoạt động."
    }
    if ($normalized -match '(?i)\b(?:WHERE\s+\w+\.id|WHERE\s+\w+_id)\s*=\s*[:?]') {
        return "Lấy chi tiết $domain theo mã định danh được truyền vào."
    }
    return "Đọc $domain theo các điều kiện, phép nối và thứ tự thể hiện trong câu query."
}

function Get-PrimaryEntity {
    param([object]$Item)

    $fileName = [System.IO.Path]::GetFileNameWithoutExtension($Item.File)
    if ($repositoryEntities.ContainsKey($fileName)) {
        return $repositoryEntities[$fileName]
    }

    $callMatch = [regex]::Match(
        $Item.Query,
        '\b(?<variable>[A-Za-z_][A-Za-z0-9_]*Repository|repository)\.(?:findAll|findById|existsById|save|saveAll|deleteById|deleteAll)\s*\(',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if ($callMatch.Success) {
        $variable = $callMatch.Groups["variable"].Value
        if ($repositoryVariables.ContainsKey($variable)) {
            return $repositoryVariables[$variable]
        }
        $baseName = $variable -replace '(?i)Repository$', ''
        foreach ($entityName in $entityCatalog.Keys) {
            if ($baseName -ieq $entityName -or $baseName -ieq ($entityName.Substring(0, 1).ToLower() + $entityName.Substring(1))) {
                return $entityName
            }
        }
    }

    $jpqlMatch = [regex]::Match($Item.Query, '(?i)\bFROM\s+(?<entity>[A-Z][A-Za-z0-9_]*)\s+[a-z]')
    if ($jpqlMatch.Success -and $entityCatalog.ContainsKey($jpqlMatch.Groups["entity"].Value)) {
        return $jpqlMatch.Groups["entity"].Value
    }
    return $null
}

function Get-DataAccessMetadata {
    param([object]$Item)

    $tables = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    $columns = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    $query = $Item.Query
    $primaryEntity = Get-PrimaryEntity -Item $Item

    if ($Item.Kind -in @("Spring Data repository method", "Inherited JpaRepository operation")) {
        if ($primaryEntity -and $entityCatalog.ContainsKey($primaryEntity)) {
            $meta = $entityCatalog[$primaryEntity]
            [void]$tables.Add($meta.Table)

            if ($Item.Kind -eq "Inherited JpaRepository operation") {
                if ($query -match '\.(?:findAll|save|saveAll|deleteAll)\s*\(') {
                    foreach ($column in $meta.Properties.Values) { [void]$columns.Add("$($meta.Table).$column") }
                } else {
                    $idColumn = if ($meta.Properties.ContainsKey("id")) { $meta.Properties["id"] } else { "id" }
                    [void]$columns.Add("$($meta.Table).$idColumn")
                }
            } else {
                $method = [regex]::Match($query, '\b(?:find|exists|count|delete)[A-Z][A-Za-z0-9_]*').Value
                foreach ($property in $meta.Properties.Keys) {
                    $propertyAppears = if ($property -eq "id") {
                        $method -match '(?:By|And|Or)Id(?:And|Or|OrderBy|$)'
                    } else {
                        $method -match [regex]::Escape($property)
                    }
                    if ($propertyAppears) {
                        [void]$columns.Add("$($meta.Table).$($meta.Properties[$property])")
                    }
                }
                if ($columns.Count -eq 0) {
                    foreach ($column in $meta.Properties.Values) { [void]$columns.Add("$($meta.Table).$column") }
                }
            }
        }
        return [pscustomobject]@{
            Tables = @($tables | Sort-Object)
            Columns = @($columns | Sort-Object)
        }
    }

    $isJpql = (Get-QueryTypeVietnamese -Item $Item) -like "JPQL*"
    if ($isJpql) {
        $aliases = @{}
        $fromMatch = [regex]::Match($query, '(?i)\bFROM\s+(?<entity>[A-Z][A-Za-z0-9_]*)\s+(?<alias>[a-z][A-Za-z0-9_]*)')
        if ($fromMatch.Success) {
            $entity = $fromMatch.Groups["entity"].Value
            $alias = $fromMatch.Groups["alias"].Value
            $aliases[$alias] = $entity
            if ($entityCatalog.ContainsKey($entity)) { [void]$tables.Add($entityCatalog[$entity].Table) }
        }

        foreach ($join in [regex]::Matches(
            $query,
            '(?i)\bJOIN(?:\s+FETCH)?\s+(?<parent>[a-z][A-Za-z0-9_]*)\.(?<property>[A-Za-z0-9_]+)(?:\s+(?<alias>[a-z][A-Za-z0-9_]*))?'
        )) {
            $parentAlias = $join.Groups["parent"].Value
            $property = $join.Groups["property"].Value
            if (!$aliases.ContainsKey($parentAlias)) { continue }
            $parentEntity = $aliases[$parentAlias]
            if (!$entityCatalog.ContainsKey($parentEntity)) { continue }
            $parentMeta = $entityCatalog[$parentEntity]
            if ($parentMeta.Properties.ContainsKey($property)) {
                [void]$columns.Add("$($parentMeta.Table).$($parentMeta.Properties[$property])")
            }
            if ($parentMeta.Relations.ContainsKey($property)) {
                $targetEntity = $parentMeta.Relations[$property]
                if ($entityCatalog.ContainsKey($targetEntity)) {
                    [void]$tables.Add($entityCatalog[$targetEntity].Table)
                    if ($join.Groups["alias"].Success) {
                        $aliases[$join.Groups["alias"].Value] = $targetEntity
                    }
                }
            }
        }

        foreach ($propertyRef in [regex]::Matches($query, '\b(?<alias>[a-z][A-Za-z0-9_]*)\.(?<property>[A-Za-z_][A-Za-z0-9_]*)')) {
            $alias = $propertyRef.Groups["alias"].Value
            $property = $propertyRef.Groups["property"].Value
            if (!$aliases.ContainsKey($alias)) { continue }
            $entity = $aliases[$alias]
            if (!$entityCatalog.ContainsKey($entity)) { continue }
            $meta = $entityCatalog[$entity]
            if ($meta.Properties.ContainsKey($property)) {
                [void]$columns.Add("$($meta.Table).$($meta.Properties[$property])")
            }
        }
    } else {
        $aliases = @{}
        $cteNames = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        foreach ($cteMatch in [regex]::Matches($query, '(?i)(?:\bWITH|,)\s*(?<name>[A-Za-z_][A-Za-z0-9_]*)\s+AS\s*\(')) {
            [void]$cteNames.Add($cteMatch.Groups["name"].Value)
        }
        foreach ($tableMatch in [regex]::Matches(
            $query,
            '(?i)\b(?:FROM|JOIN|UPDATE|INTO|ALTER\s+TABLE|CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?|DELETE\s+FROM)\s+(?<table>[A-Za-z_][A-Za-z0-9_]*)(?:\s+(?:AS\s+)?(?<alias>[A-Za-z_][A-Za-z0-9_]*))?'
        )) {
            $table = $tableMatch.Groups["table"].Value
            if ($table -in @("SELECT", "VALUES", "SET") -or $cteNames.Contains($table)) { continue }
            [void]$tables.Add($table)
            $alias = $tableMatch.Groups["alias"].Value
            if ($alias -and $alias -notin @("WHERE", "LEFT", "RIGHT", "INNER", "CROSS", "ON", "ORDER", "GROUP", "LIMIT", "RETURNING")) {
                $aliases[$alias] = $table
            }
            $aliases[$table] = $table
        }

        foreach ($columnRef in [regex]::Matches($query, '\b(?<alias>[A-Za-z_][A-Za-z0-9_]*)\.(?<column>[A-Za-z_][A-Za-z0-9_]*)')) {
            $alias = $columnRef.Groups["alias"].Value
            $column = $columnRef.Groups["column"].Value
            if ($aliases.ContainsKey($alias)) {
                [void]$columns.Add("$($aliases[$alias]).$column")
            }
        }

        $withoutStrings = [regex]::Replace($query, "'(?:''|[^'])*'", " ")
        foreach ($entityMeta in $entityCatalog.Values) {
            if (!$tables.Contains($entityMeta.Table)) { continue }
            foreach ($column in $entityMeta.Properties.Values) {
                if ($withoutStrings -match "(?i)\b$([regex]::Escape($column))\b") {
                    [void]$columns.Add("$($entityMeta.Table).$column")
                }
            }
        }

        $knownRawColumns = @(
            "visitor_card_id", "card_code", "display_number", "current_order_id",
            "order_id", "order_code", "entry_type", "payment_status", "payment_method",
            "calculated_fee", "checked_in_by", "checked_out_by", "checkout_confirmed_at",
            "fee_rate_id", "fee_breakdown", "first_block_minutes", "first_block_fee",
            "next_block_minutes", "next_block_fee", "daily_cap", "overnight_fee",
            "effective_from", "effective_to", "config_id", "config_data", "updated_by",
            "incident_id", "incident_type", "severity", "description", "resolution",
            "created_by", "resolved_by", "resolved_at", "reply_title", "audit_id",
            "action", "message", "recipient_user_id", "recipient_target", "published_at"
        )
        foreach ($column in $knownRawColumns) {
            if ($withoutStrings -match "(?i)\b$([regex]::Escape($column))\b") {
                [void]$columns.Add($column)
            }
        }
    }

    return [pscustomobject]@{
        Tables = @($tables | Sort-Object)
        Columns = @($columns | Sort-Object)
    }
}

$patterns = @(
    @{
        Kind = "JPQL/native @Query"
        Regex = '(?ms)@Query\(\s*(?:value\s*=\s*)?"""(?<query>.*?)""".*?\)'
    },
    @{
        Kind = "JPQL/native @Query"
        Regex = '(?m)@Query\(\s*(?:value\s*=\s*)?"(?<query>(?:\\"|[^"])*)".*?\)'
    },
    @{
        Kind = "JdbcTemplate inline SQL"
        Regex = '(?ms)jdbcTemplate\.(?:query|queryForObject|queryForList|queryForMap|update|execute)\(\s*"""(?<query>.*?)"""'
    },
    @{
        Kind = "PreparedStatement SQL"
        Regex = '(?ms)prepareStatement\(\s*"""(?<query>.*?)"""'
    },
    @{
        Kind = "SQL variable/text block"
        Regex = '(?ms)(?:String\s+sql|sql)\s*=\s*"""(?<query>.*?)"""'
    },
    @{
        Kind = "SQL returned by helper"
        Regex = '(?ms)return\s+"""(?<query>\s*(?:SELECT|WITH|INSERT|UPDATE|DELETE|ALTER|CREATE).*?)"""'
    },
    @{
        Kind = "SQL passed to helper"
        Regex = '(?ms)(?:queryLong|mergeTraffic)\([^,]*,?\s*"""(?<query>.*?)"""'
    },
    @{
        Kind = "Inline SQL string"
        Regex = '(?m)(?:jdbcTemplate\.(?:query|queryForObject|queryForList|queryForMap|update|execute)|queryLong)\(\s*"(?<query>(?:\\"|[^"])*(?:SELECT|INSERT|UPDATE|DELETE|ALTER|CREATE)(?:\\"|[^"])*)"'
    }
)

foreach ($file in $files) {
    $text = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    $relative = $file.FullName.Substring($repositoryRoot.Length).TrimStart("\", "/")

    foreach ($pattern in $patterns) {
        foreach ($match in [regex]::Matches($text, $pattern.Regex)) {
            $queryGroup = $match.Groups["query"]
            $line = Get-LineNumber -Text $text -Index $queryGroup.Index
            Add-Query -Kind $pattern.Kind -File $relative -Line $line -Query $queryGroup.Value
        }
    }

    if ($relative -like "src\main\java\com\swp\parking\repository\*.java") {
        $lines = Get-Content -LiteralPath $file.FullName -Encoding UTF8
        $signature = ""
        $signatureStart = 0

        for ($index = 0; $index -lt $lines.Count; $index++) {
            $trimmed = $lines[$index].Trim()
            if (!$signature -and $trimmed -match '^(?:List|Optional|Page|boolean|long|int|java\.util\.Optional)<*.*\s+(find|exists|count|delete)[A-Z]') {
                $signature = $trimmed
                $signatureStart = $index + 1
            } elseif ($signature) {
                $signature += " " + $trimmed
            }

            if ($signature -and $trimmed.EndsWith(";")) {
                $methodMatch = [regex]::Match($signature, '\b(find|exists|count|delete)[A-Z][A-Za-z0-9_]*')
                if ($methodMatch.Success) {
                    Add-Query `
                        -Kind "Spring Data repository method" `
                        -File $relative `
                        -Line $signatureStart `
                        -Query $signature `
                        -Context "SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity."
                }
                $signature = ""
                $signatureStart = 0
            }
        }
    }

    $lines = Get-Content -LiteralPath $file.FullName -Encoding UTF8
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $line = $lines[$index]
        foreach ($match in [regex]::Matches(
            $line,
            '\b[A-Za-z_][A-Za-z0-9_]*(?:Repository|repository)\.(?<method>findAll|findById|existsById|save|saveAll|deleteById|deleteAll)\s*\(',
            [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
        )) {
            Add-Query `
                -Kind "Inherited JpaRepository operation" `
                -File $relative `
                -Line ($index + 1) `
                -Query $line.Trim() `
                -Context "Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh."
        }
    }
}

$builder = [System.Text.StringBuilder]::new()
[void]$builder.AppendLine("# Tổng hợp toàn bộ query trong src/main/java")
[void]$builder.AppendLine()
[void]$builder.AppendLine("> Báo cáo được sinh trực tiếp từ source bởi ``scripts/export-src-queries.ps1``. Mỗi mục ghi loại query, mục đích, vị trí, bảng/entity và cột/thuộc tính liên quan.")
[void]$builder.AppendLine()
[void]$builder.AppendLine("- Tổng số mục: $($results.Count)")
[void]$builder.AppendLine("- Phạm vi quét: ``$resolvedRoot``")
[void]$builder.AppendLine("- Đây là tài liệu kiểm kê source, không phải tập lệnh migration hoặc script chạy trực tiếp trên database.")
[void]$builder.AppendLine("- Với JPQL/Spring Data/JpaRepository, SQL thực tế do Hibernate sinh từ entity mapping.")
[void]$builder.AppendLine()

$groupOrder = @(
    "JPQL/native @Query",
    "JdbcTemplate inline SQL",
    "PreparedStatement SQL",
    "SQL variable/text block",
    "SQL returned by helper",
    "SQL passed to helper",
    "Inline SQL string",
    "Spring Data repository method",
    "Inherited JpaRepository operation"
)

$groupTitles = @{
    "JPQL/native @Query" = "Query khai báo bằng @Query"
    "JdbcTemplate inline SQL" = "SQL viết trực tiếp trong JdbcTemplate"
    "PreparedStatement SQL" = "SQL dùng PreparedStatement"
    "SQL variable/text block" = "SQL lưu trong biến Java"
    "SQL returned by helper" = "SQL trả về từ hàm helper"
    "SQL passed to helper" = "SQL truyền vào hàm helper"
    "Inline SQL string" = "SQL chuỗi một dòng"
    "Spring Data repository method" = "Query Spring Data sinh từ tên method"
    "Inherited JpaRepository operation" = "Thao tác CRUD kế thừa từ JpaRepository"
}

foreach ($kind in $groupOrder) {
    $items = $results |
        Where-Object Kind -eq $kind |
        Sort-Object File, Line, Query
    if (!$items) { continue }

    [void]$builder.AppendLine("## $($groupTitles[$kind])")
    [void]$builder.AppendLine()
    foreach ($item in $items) {
        $path = $item.File.Replace("\", "/")
        [void]$builder.AppendLine("### ``$path`:$($item.Line)``")
        [void]$builder.AppendLine()
        [void]$builder.AppendLine("**Loại query:** $(Get-QueryTypeVietnamese -Item $item)")
        [void]$builder.AppendLine()
        [void]$builder.AppendLine("**Mục đích:** $(Get-QueryPurposeVietnamese -Item $item)")
        [void]$builder.AppendLine()
        [void]$builder.AppendLine("**Vị trí trong source:** ``$path``, bắt đầu tại dòng **$($item.Line)**.")
        [void]$builder.AppendLine()
        $dataAccess = Get-DataAccessMetadata -Item $item
        $tableText = if ($dataAccess.Tables.Count -gt 0) {
            ($dataAccess.Tables | ForEach-Object { "``$_``" }) -join ", "
        } else {
            "Không xác định tự động; xem entity/method tại vị trí source phía trên."
        }
        $columnText = if ($dataAccess.Columns.Count -gt 0) {
            ($dataAccess.Columns | ForEach-Object { "``$_``" }) -join ", "
        } else {
            "Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động."
        }
        [void]$builder.AppendLine("**Bảng/entity liên quan:** $tableText")
        [void]$builder.AppendLine()
        [void]$builder.AppendLine("**Cột/thuộc tính liên quan:** $columnText")
        [void]$builder.AppendLine()
        if ($item.Context) {
            [void]$builder.AppendLine("**Ghi chú:**")
            [void]$builder.AppendLine()
            [void]$builder.AppendLine($item.Context)
            [void]$builder.AppendLine()
        }
        $codeLanguage = "sql"
        if ($item.Kind -in @("Spring Data repository method", "Inherited JpaRepository operation")) {
            $codeLanguage = "java"
        } elseif ($item.Kind -eq "JPQL/native @Query" -and
                  (Get-QueryTypeVietnamese -Item $item) -like "JPQL*") {
            $codeLanguage = "jpql"
        }
        [void]$builder.AppendLine(('```' + $codeLanguage))
        [void]$builder.AppendLine($item.Query)
        [void]$builder.AppendLine('```')
        [void]$builder.AppendLine()
    }
}

$report = $builder.ToString()
if ($OutputFile) {
    $target = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputFile))
    [System.IO.File]::WriteAllText($target, $report, [System.Text.UTF8Encoding]::new($false))
    Write-Host "Đã ghi $($results.Count) mục query vào $target"
} else {
    $report
}
