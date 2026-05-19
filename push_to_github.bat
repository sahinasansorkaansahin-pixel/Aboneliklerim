@echo off
echo ==========================================
echo  Aboneliklerim GitHub Push Yardimcisi
echo ==========================================
echo.
echo [1/3] Remote repository (uzak sunucu) adresi tanımlanıyor...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/sahinasansorkaansahin-pixel/Aboneliklerim.git
git branch -M main

echo [2/3] Kodlar GitHub'a push ediliyor...
echo (Eğer giriş yapmadıysanız tarayıcı üzerinden giriş ekranı gelecektir)
git push -u origin main

echo.
echo [3/3] İşlem tamamlandı!
echo.
pause
