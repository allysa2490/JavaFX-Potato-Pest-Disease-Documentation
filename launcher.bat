@echo off
title JavaFX Launcher

set "PATH_TO_FX=javafx-sdk-26/lib"
set "DB_LIBS=db/*"

:START

echo Compiling Java files...

javac ^
--module-path "%PATH_TO_FX%" ^
--add-modules javafx.controls,javafx.fxml ^
-cp ".;%DB_LIBS%" ^
-d . ^
Main.java *.java

echo Launching application...

java ^
--enable-native-access=javafx.graphics ^
--enable-native-access=ALL-UNNAMED ^
--module-path "%PATH_TO_FX%" ^
--add-modules javafx.controls,javafx.fxml ^
-cp ".;%DB_LIBS%" ^
Main

echo.
echo Application Closed
echo.

:CHOICE
set /p input=Run again? (Y/N): 

if /I "%input%"=="Y" goto START
if /I "%input%"=="N" goto END

echo Invalid choice. Please enter Y or N.
goto CHOICE

:END
echo Goodbye!
pause