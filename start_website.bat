@echo off
cd website
echo Starting KisanMitra Website...
echo Opening: http://localhost:5500
start http://localhost:5500
python -m http.server 5500
pause