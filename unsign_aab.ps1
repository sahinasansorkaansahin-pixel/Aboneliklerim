$path = "app/build/outputs/bundle/release/Aboneliklerim.aab"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($path, "Update")
$entries = $zip.Entries | Where-Object { $_.FullName -like "META-INF/*" }
foreach ($e in $entries) {
    $e.Delete()
}
$zip.Dispose()
