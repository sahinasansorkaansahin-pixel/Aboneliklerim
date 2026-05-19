import os
import re

translations = {
    "values": {"backup_success": "Backup completed successfully!", "restore_success": "Restore completed successfully!"},
    "values-de": {"backup_success": "Sicherung erfolgreich abgeschlossen!", "restore_success": "Wiederherstellung erfolgreich abgeschlossen!"},
    "values-es": {"backup_success": "¡Copia de seguridad completada con éxito!", "restore_success": "¡Restauración completada con éxito!"},
    "values-hi": {"backup_success": "बैकअप सफलतापूर्वक पूरा हुआ!", "restore_success": "पुनर्प्राप्ति सफलतापूर्वक पूरी हुई!"},
    "values-it": {"backup_success": "Backup completato con successo!", "restore_success": "Ripristino completato con successo!"},
    "values-ja": {"backup_success": "バックアップが正常に完了しました！", "restore_success": "復元が正常に完了しました！"},
    "values-sv": {"backup_success": "Säkerhetskopieringen slutfördes framgångsrikt!", "restore_success": "Återställningen slutfördes framgångsrikt!"},
    "values-tr": {"backup_success": "Yedekleme başarıyla tamamlandı!", "restore_success": "Geri yükleme başarıyla tamamlandı!"}
}

base_path = "c:/Aboneliklerim/app/src/main/res"

for folder, trans_dict in translations.items():
    file_path = os.path.join(base_path, folder, "strings.xml")
    if not os.path.exists(file_path):
        continue
        
    print(f"Updating strings in {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for key, val in trans_dict.items():
        if f'name="{key}"' not in content:
            val_esc = val.replace("'", "\\'")
            content = content.replace('</resources>', f'    <string name="{key}">{val_esc}</string>\n</resources>')
        else:
            val_esc = val.replace("'", "\\'")
            content = re.sub(f'<string name="{key}">.*?</string>', f'<string name="{key}">{val_esc}</string>', content)
            
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
